package net.runelite.client.plugins.fracturedarchive.ezekiel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Area;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Constants;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class EzekielOverlay extends EncounterOverlay
{
	/** Longest lane drawn, in tiles, in case a sword's path never hits a wall. */
	private static final int MAX_LANE_LENGTH = 30;
	private static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_FULL;

	/** A sword's lane only shows for its last 5 ticks. */
	private static final int LANE_TICKS = 5;

	/** The special's star: the crack, then the marker, before it explodes. */
	private static final Set<Integer> STAR_GRAPHICS = Set.of(
		SpotanimID.EZEKIEL_SPECIAL_CRACK_VFX01,
		SpotanimID.VFX_EZEKIEL_STAR_EXPLOSION_SPAWN01,
		SpotanimID.VFX_EZEKIEL_STAR_EXPLOSION_MARKER01);

	/** Colors (the colors you had saved). */
	private static final Color SWORD_BORDER = new Color(255, 115, 0, 200);
	private static final Color SWORD_FILL = new Color(255, 115, 0, 15);
	private static final Color STAR_BORDER = new Color(255, 115, 0, 220);
	private static final Color STAR_FILL = new Color(255, 115, 0, 50);
	private static final Color DODGE_COLOR = new Color(255, 255, 255, 180);

	@Inject
	private Ezekiel encounter;

	@Inject
	protected EzekielOverlay(FracturedArchiveConfig config)
	{
		super(config);
	}

	@Override
	protected void renderEncounter(Graphics2D graphics)
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return;
		}

		// Each sword keeps the order it's given, so the 2nd and 3rd keep theirs after the 1st goes
		for (Map.Entry<NPC, Integer> e : swordOrder().entrySet())
		{
			fixedOrder.putIfAbsent(e.getKey(), e.getValue());
		}
		fixedOrder.keySet().retainAll(encounter.getSwords().keySet());
		for (Map.Entry<NPC, Ezekiel.SwordTrack> entry : encounter.getSwords().entrySet())
		{
			NPC sword = entry.getKey();
			Ezekiel.SwordTrack track = entry.getValue();
			Integer step = fixedOrder.get(sword);
			// In a set of three on one side they go a tick apart: 15, 16, 17
			int ticks = encounter.ticksUntilLaunch(track) + (step != null ? step - 1 : 0);
			if (ticks <= 0)
			{
				// Already gone off
				continue;
			}

			boolean showLane = ticks <= LANE_TICKS;
			if (config.ezekielSwordLanes() && showLane && track.hasDirection())
			{
				renderLane(graphics, wv, sword, track);
			}

			if (config.ezekielSwordTimer())
			{
				String text = Integer.toString(ticks);
				Point point = sword.getCanvasTextLocation(graphics, text, config.textZ());
				renderText(graphics, point, text, showLane ? SWORD_BORDER : Color.WHITE);
			}

			// Which goes 1st, 2nd, 3rd in a set on one side, under the countdown
			if (config.ezekielSwordTimer() && step != null)
			{
				Color c = step == 1 ? Color.GREEN : step == 2 ? Color.YELLOW : Color.RED;
				renderStackedText(graphics, sword, Integer.toString(step), c, 20f);
			}
		}

		if (config.ezekielStarCracks())
		{
			for (GraphicsObject g : wv.getGraphicsObjects())
			{
				int id = g.getId();
				if (STAR_GRAPHICS.contains(id))
				{
					renderPoly(graphics, Perspective.getCanvasTilePoly(client, g.getLocation()),
						STAR_BORDER, STAR_FILL, config.borderWidth());
				}
			}
		}

		NPC ezekiel = encounter.getBoss();
		if (ezekiel == null)
		{
			return;
		}

		if (config.ezekielFinaleFront() && encounter.getFinaleTicks() >= 0)
		{
			renderFinaleFront(graphics, ezekiel);
		}

		int star = encounter.getStarCount();
		if (config.ezekielStarCounter() && star > 0)
		{
			// Stacked directly under the tick counter (like Theatre), in a smaller font
			renderStackedText(graphics, ezekiel, Integer.toString(star), star >= 3 ? Ezekiel.SPECIAL_COLOR : Ezekiel.MELEE_COLOR, 20f);
		}

		if (config.ezekielAttackCounter() && encounter.getAttackTicks() > 0)
		{
			String text = Integer.toString(encounter.getAttackTicks());
			renderText(graphics, ezekiel.getCanvasTextLocation(graphics, text, config.textZ()), text, encounter.getAttackColor());
		}
	}

	/**
	 * Swords on the same side that went down together go off one after another: flying east or
	 * west, the northern one first, then the middle, then the southern; flying north or south,
	 * the western one first, then the middle, then the eastern.
	 */
	private final Map<NPC, Integer> fixedOrder = new java.util.HashMap<>();

	private Map<NPC, Integer> swordOrder()
	{
		// Group by direction, then split where the drops are more than 2 ticks apart
		Map<String, java.util.List<NPC>> byDirection = new java.util.HashMap<>();
		for (Map.Entry<NPC, Ezekiel.SwordTrack> entry : encounter.getSwords().entrySet())
		{
			Ezekiel.SwordTrack track = entry.getValue();
			if (!track.hasDirection() || encounter.ticksUntilLaunch(track) <= 0 || entry.getKey().getWorldLocation() == null)
			{
				continue;
			}
			byDirection.computeIfAbsent(track.getDx() + "," + track.getDy(), k -> new java.util.ArrayList<>()).add(entry.getKey());
		}
		java.util.List<java.util.List<NPC>> setList = new java.util.ArrayList<>();
		for (java.util.List<NPC> group : byDirection.values())
		{
			group.sort(java.util.Comparator.comparingInt(n -> encounter.getSwords().get(n).getSpawnTick()));
			java.util.List<NPC> current = null;
			int last = Integer.MIN_VALUE;
			for (NPC n : group)
			{
				int spawn = encounter.getSwords().get(n).getSpawnTick();
				if (current == null || spawn - last > 2)
				{
					current = new java.util.ArrayList<>();
					setList.add(current);
				}
				current.add(n);
				last = spawn;
			}
		}
		Map<String, java.util.List<NPC>> sets = new java.util.HashMap<>();
		for (int i = 0; i < setList.size(); i++)
		{
			sets.put(Integer.toString(i), setList.get(i));
		}

		Map<NPC, Integer> order = new java.util.HashMap<>();
		for (java.util.List<NPC> set : sets.values())
		{
			if (set.size() < 3)
			{
				// Only sets of three on one side
				continue;
			}
			Ezekiel.SwordTrack track = encounter.getSwords().get(set.get(0));
			if (track.getDx() != 0)
			{
				// Flying east or west: north first
				set.sort((a, b) -> Integer.compare(b.getWorldLocation().getY(), a.getWorldLocation().getY()));
			}
			else
			{
				// Flying north or south: west first
				set.sort((a, b) -> Integer.compare(a.getWorldLocation().getX(), b.getWorldLocation().getX()));
			}
			for (int i = 0; i < set.size(); i++)
			{
				order.put(set.get(i), i + 1);
			}
		}
		return order;
	}

	/**
	 * During the finale: the far row of the 3x3 in front of Ezekiel, which is where to dodge to.
	 * "In front" is the way he's facing, rounded to north, south, east or west.
	 */
	private void renderFinaleFront(Graphics2D graphics, NPC ezekiel)
	{
		LocalPoint center = ezekiel.getLocalLocation();
		if (center == null)
		{
			return;
		}

		NPCComposition comp = ezekiel.getTransformedComposition();
		int size = comp != null ? Math.max(1, comp.getSize()) : 3;
		int half = (size - 1) / 2;
		int[] dir = Ezekiel.cardinal(ezekiel.getOrientation());
		int dx = dir[0];
		int dy = dir[1];
		int tile = Perspective.LOCAL_TILE_SIZE;

		// The far row, across the way he's facing
		int toLastRow = half + size;
		LocalPoint rowMiddle = center.plus(dx * toLastRow * tile, dy * toLastRow * tile);
		Color safe = DODGE_COLOR;
		Color safeFill = new Color(safe.getRed(), safe.getGreen(), safe.getBlue(), 110);
		java.util.Map<net.runelite.api.coords.WorldPoint, Integer> order = encounter.getStrikeOrder();
		for (int i = -half; i <= size - 1 - half; i++)
		{
			LocalPoint lp = rowMiddle.plus(dy * i * tile, dx * i * tile);
			renderPoly(graphics, Perspective.getCanvasTilePoly(client, lp), safe, safeFill, config.borderWidth());

			// Once this round's strikes are known: which back-row tile to stand on 1st, 2nd, 3rd
			Integer step = order.get(net.runelite.api.coords.WorldPoint.fromLocal(client, lp));
			if (step != null)
			{
				// Size 20
				java.awt.Font font = graphics.getFont();
				graphics.setFont(font.deriveFont(20f));
				String text = Integer.toString(step);
				renderText(graphics, Perspective.getCanvasTextLocation(client, graphics, lp, text, 0), text,
					step == 1 ? Color.GREEN : step == 2 ? Color.YELLOW : Color.RED);
				graphics.setFont(font);
			}
		}
	}

	private void renderLane(Graphics2D graphics, WorldView wv, NPC sword, Ezekiel.SwordTrack track)
	{
		LocalPoint start = sword.getLocalLocation();
		if (start == null)
		{
			return;
		}

		int dx = track.getDx();
		int dy = track.getDy();

		NPCComposition composition = sword.getTransformedComposition();
		int size = composition != null ? Math.max(1, composition.getSize()) : 1;

		CollisionData[] maps = wv.getCollisionMaps();
		int[][] flags = maps != null ? maps[wv.getPlane()].getFlags() : null;

		// Step tile by tile from the sword until it hits a wall or the edge of the scene. Swords can
		// start inside a wall at the edge of the room, so walls are only a stop once the lane has
		// reached open floor.
		Area lane = new Area();
		LocalPoint step = start;
		boolean reachedFloor = false;
		for (int i = 0; i <= MAX_LANE_LENGTH; i++)
		{
			if (i > 0)
			{
				step = step.plus(dx * Perspective.LOCAL_TILE_SIZE, dy * Perspective.LOCAL_TILE_SIZE);
			}

			boolean blocked = isBlocked(flags, step);
			if (blocked && reachedFloor || isOutsideScene(step))
			{
				break;
			}
			reachedFloor |= !blocked;

			Polygon poly = Perspective.getCanvasTileAreaPoly(client, step, size);
			if (poly != null)
			{
				lane.add(new Area(poly));
			}
		}

		if (!lane.isEmpty())
		{
			graphics.setStroke(new BasicStroke(config.borderWidth()));
			graphics.setColor(SWORD_FILL);
			graphics.fill(lane);
			graphics.setColor(SWORD_BORDER);
			graphics.draw(lane);
		}
	}

	private static boolean isOutsideScene(LocalPoint lp)
	{
		int x = lp.getSceneX();
		int y = lp.getSceneY();
		return x < 0 || y < 0 || x >= Constants.SCENE_SIZE || y >= Constants.SCENE_SIZE;
	}

	private static boolean isBlocked(int[][] flags, LocalPoint lp)
	{
		return !isOutsideScene(lp) && flags != null && (flags[lp.getSceneX()][lp.getSceneY()] & BLOCKED) != 0;
	}
}
