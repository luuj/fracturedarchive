package net.runelite.client.plugins.fracturedarchive.ezekiel;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Area;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Constants;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class EzekielOverlay extends EncounterOverlay
{
	/** Longest lane drawn, in tiles, in case a sword's path never hits a wall. */
	private static final int MAX_LANE_LENGTH = 30;
	private static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_FULL;

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
		if (config.ezekielSwordLanes())
		{
			WorldView wv = client.getTopLevelWorldView();
			if (wv != null)
			{
				for (Map.Entry<NPC, Ezekiel.SwordTrack> entry : encounter.getSwords().entrySet())
				{
					renderLane(graphics, wv, entry.getKey(), entry.getValue());
				}
			}
		}

		if (!encounter.isActive())
		{
			return;
		}

		// Other Ezekiel overlays go here.
	}

	private void renderLane(Graphics2D graphics, WorldView wv, NPC sword, Ezekiel.SwordTrack track)
	{
		LocalPoint start = sword.getLocalLocation();
		if (start == null)
		{
			return;
		}

		int dx;
		int dy;
		if (track.hasMoved())
		{
			dx = track.getDx();
			dy = track.getDy();
		}
		else
		{
			int[] dir = facing(sword.getOrientation());
			dx = dir[0];
			dy = dir[1];
		}
		if (dx == 0 && dy == 0)
		{
			return;
		}

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

	/**
	 * Turns an orientation (0 = south, 512 = west, 1024 = north, 1536 = east) into a step of
	 * -1, 0 or 1 on each axis, snapped to the nearest of the 8 directions.
	 */
	private static int[] facing(int orientation)
	{
		double angle = 2 * Math.PI * orientation / 2048.0;
		double x = -Math.sin(angle);
		double y = -Math.cos(angle);
		final double threshold = Math.sin(Math.PI / 8); // 22.5 degrees
		return new int[]{snap(x, threshold), snap(y, threshold)};
	}

	private static int snap(double v, double threshold)
	{
		return v > threshold ? 1 : v < -threshold ? -1 : 0;
	}
}
