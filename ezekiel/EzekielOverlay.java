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
import net.runelite.api.GraphicsObject;
import net.runelite.api.Constants;
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

	/** Finale swords' warnings before they drop. Not confirmed in game yet. */
	private static final Set<Integer> FINALE_TELEGRAPHS = Set.of(
		SpotanimID.VFX_EZEKIEL_SWORD_TELEGRAPH01,
		SpotanimID.VFX_EZEKIEL_SWORD_TELEGRAPH02,
		SpotanimID.VFX_EZEKIEL_SWORD_TELEGRAPH03,
		SpotanimID.VFX_EZEKIEL_SWORD_TELEGRAPH04);

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

		if (config.ezekielStarCracks() || config.ezekielFinale())
		{
			for (GraphicsObject g : wv.getGraphicsObjects())
			{
				int id = g.getId();
				if (config.ezekielStarCracks() && STAR_GRAPHICS.contains(id))
				{
					renderPoly(graphics, Perspective.getCanvasTilePoly(client, g.getLocation()),
						config.ezekielStarBorder(), config.ezekielStarFill(), config.borderWidth());
				}
				else if (config.ezekielFinale() && FINALE_TELEGRAPHS.contains(id))
				{
					Color c = config.ezekielFinaleColor();
					renderPoly(graphics, Perspective.getCanvasTilePoly(client, g.getLocation()),
						c, new Color(c.getRed(), c.getGreen(), c.getBlue(), 50), config.borderWidth());
				}
			}
		}

		NPC ezekiel = encounter.getBoss();
		if (ezekiel == null)
		{
			return;
		}

		int finaleTicks = encounter.getFinaleTicks();
		if (config.ezekielFinale() && finaleTicks >= 0)
		{
			String text = "Finale " + finaleTicks;
			renderText(graphics, ezekiel.getCanvasTextLocation(graphics, text, config.textZ()), text, config.ezekielFinaleColor());
		}
		else if (config.ezekielAttackCounter() && encounter.getAttackTicks() > 0)
		{
			String text = Integer.toString(encounter.getAttackTicks());
			renderText(graphics, ezekiel.getCanvasTextLocation(graphics, text, config.textZ()), text, encounter.getAttackColor());
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
