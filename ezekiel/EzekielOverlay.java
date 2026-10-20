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

	/** The special's star: the crack, then the marker, before it explodes. */
	private static final Set<Integer> STAR_GRAPHICS = Set.of(
		SpotanimID.EZEKIEL_SPECIAL_CRACK_VFX01,
		SpotanimID.VFX_EZEKIEL_STAR_EXPLOSION_SPAWN01,
		SpotanimID.VFX_EZEKIEL_STAR_EXPLOSION_MARKER01);

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

		for (Map.Entry<NPC, Ezekiel.SwordTrack> entry : encounter.getSwords().entrySet())
		{
			NPC sword = entry.getKey();
			Ezekiel.SwordTrack track = entry.getValue();
			int ticks = encounter.ticksUntilLaunch(track);
			if (ticks <= 0)
			{
				// Already gone off
				continue;
			}

			boolean showLane = ticks <= config.ezekielLaneTicks();
			if (config.ezekielSwordLanes() && showLane && track.hasDirection())
			{
				renderLane(graphics, wv, sword, track);
			}

			if (config.ezekielSwordTimer())
			{
				String text = Integer.toString(ticks);
				Point point = sword.getCanvasTextLocation(graphics, text, config.textZ());
				renderText(graphics, point, text, showLane ? config.ezekielSwordBorder() : Color.WHITE);
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
						config.ezekielStarBorder(), config.ezekielStarFill(), config.borderWidth());
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

		if (config.ezekielAttackCounter() && encounter.getAttackTicks() > 0)
		{
			String text = Integer.toString(encounter.getAttackTicks());
			renderText(graphics, ezekiel.getCanvasTextLocation(graphics, text, config.textZ()), text, encounter.getAttackColor());
		}
	}

	/**
	 * During the finale: the far row of the 3x3 in front of Ezekiel, in the safe color, which is
	 * where to dodge to. "In front" is the way he's facing, rounded to
	 * north, south, east or west.
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

		// Its far row, across the way he's facing. Each tile lines up with one of the finale swords
		// (left, middle, right as seen along the row), and gets that sword's safe numbers.
		int toLastRow = half + size;
		LocalPoint rowMiddle = center.plus(dx * toLastRow * tile, dy * toLastRow * tile);
		Map<Integer, String> numbers = config.ezekielFinale() ? rowNumbers(center, dx, dy, half, size) : Map.of();
		Color safe = config.ezekielFinaleSafeColor();
		Color safeFill = new Color(safe.getRed(), safe.getGreen(), safe.getBlue(), 70);
		for (int i = -half; i <= size - 1 - half; i++)
		{
			// Perpendicular to the way he's facing
			LocalPoint lp = rowMiddle.plus(dy * i * tile, dx * i * tile);
			renderPoly(graphics, Perspective.getCanvasTilePoly(client, lp), safe, safeFill, config.borderWidth());
			String text = numbers.get(i);
			if (text != null)
			{
				renderText(graphics, Perspective.getCanvasTextLocation(client, graphics, lp, text, 0), text, Color.WHITE);
			}
		}
	}

	/**
	 * Which dodge-row tile each safe number goes on. The finale swords are lined up along the row
	 * (sorted by where they sit across Ezekiel's facing), so the leftmost sword is the leftmost
	 * tile and so on. With a different number of swords than tiles, each sword goes on the left,
	 * middle or right tile by which side of Ezekiel it's on.
	 */
	private Map<Integer, String> rowNumbers(LocalPoint center, int dx, int dy, int half, int size)
	{
		java.util.List<NPC> order = encounter.getSafeOrder();
		if (order.isEmpty())
		{
			return Map.of();
		}

		java.util.List<NPC> swords = new java.util.ArrayList<>(encounter.getFinaleSwords().keySet());
		java.util.function.ToIntFunction<NPC> across = n ->
		{
			LocalPoint lp = n.getLocalLocation();
			return lp == null ? 0 : (lp.getX() - center.getX()) * dy + (lp.getY() - center.getY()) * dx;
		};
		swords.sort(java.util.Comparator.comparingInt(across));

		Map<NPC, Integer> tileFor = new java.util.HashMap<>();
		for (int k = 0; k < swords.size(); k++)
		{
			NPC sword = swords.get(k);
			int slot;
			if (swords.size() == size)
			{
				slot = k - half;
			}
			else
			{
				int t = across.applyAsInt(sword);
				slot = Math.abs(t) < Perspective.LOCAL_TILE_SIZE ? 0 : Integer.signum(t) * half;
			}
			tileFor.put(sword, slot);
		}

		Map<Integer, String> numbers = new java.util.HashMap<>();
		for (int k = 0; k < order.size(); k++)
		{
			Integer slot = tileFor.get(order.get(k));
			if (slot != null)
			{
				numbers.merge(slot, Integer.toString(k + 1), (x, y) -> x + " " + y);
			}
		}
		return numbers;
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
			graphics.setColor(config.ezekielSwordFill());
			graphics.fill(lane);
			graphics.setColor(config.ezekielSwordBorder());
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
