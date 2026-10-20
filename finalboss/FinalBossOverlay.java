package net.runelite.client.plugins.fracturedarchive.finalboss;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class FinalBossOverlay extends EncounterOverlay
{
	private static final int CYCLES_PER_TICK = 30;

	/** Floor graphics that hit where they are: phase 1 charge cracks and explosions, phase 2 AoE, bombs, debris. */
	private static final Set<Integer> DANGER_GRAPHICS = Set.of(
		SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_01_START, SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_01_START_90,
		SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_01_START_180, SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_01_START_270,
		SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_01, SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_01_END,
		SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_01_90, SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_01_END_90,
		SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_01_180, SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_01_END_180,
		SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_01_270, SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_01_END_270,
		SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_02_START, SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_02_START_90,
		SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_02_START_180, SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_02_START_270,
		SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_02, SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_02_END,
		SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_02_90, SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_02_END_90,
		SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_02_180, SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_02_END_180,
		SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_02_270, SpotanimID.VFX_OCELLUS_P1_FLOOR_CRACK_02_END_270,
		SpotanimID.VFX_OCELLUS_P1_EXPLODE_FLOOR, SpotanimID.VFX_OCELLUS_P1_EXPLODE_FLOOR_90,
		SpotanimID.VFX_OCELLUS_P1_EXPLODE_FLOOR_180, SpotanimID.VFX_OCELLUS_P1_EXPLODE_FLOOR_270,
		SpotanimID.VFX_OCELLUS_P2_AOE_DODGE,
		SpotanimID.VFX_OCELLUS_QUADRANT_BOMB_SPAWN_01, SpotanimID.VFX_OCELLUS_QUADRANT_BOMB_DROP_01,
		SpotanimID.VFX_OCELLUS_QUADRANT_BOMB_IDLE_01,
		SpotanimID.VFX_OCELLUS_FALLING_DEBRIS);

	/** Ocellus's projectiles. */
	private static final Set<Integer> PROJECTILES = Set.of(
		SpotanimID.VFX_OCELLUS_WATER_ATTACK01_PROJANIM,
		SpotanimID.VFX_OCELLUS_FIRE_ATTACK01_PROJANIM,
		SpotanimID.VFX_OCELLUS_MAGIC_PROJANIM01,
		SpotanimID.VFX_OCELLUS_MAGIC_PROJANIM02,
		SpotanimID.VFX_OCELLUS_PHASE1_CHARGE_EXPEND_PROJECTILE,
		SpotanimID.VFX_OCELLUS_PORTAL_PROJECTILE,
		SpotanimID.TFA_OCELLUS_SF_PROJ);

	@Inject
	private FinalBoss encounter;

	@Inject
	protected FinalBossOverlay(FracturedArchiveConfig config)
	{
		super(config);
	}

	@Override
	protected void renderEncounter(Graphics2D graphics)
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null || !encounter.isActive())
		{
			return;
		}

		if (config.ocellusGroundMarkers())
		{
			Color danger = config.ocellusDangerColor();
			Color dangerFill = alpha(danger, 50);

			for (TileObject o : encounter.getDangerObjects())
			{
				renderPoly(graphics, footprint(o), danger, dangerFill, config.borderWidth());
			}

			for (GraphicsObject g : wv.getGraphicsObjects())
			{
				if (DANGER_GRAPHICS.contains(g.getId()))
				{
					renderPoly(graphics, Perspective.getCanvasTilePoly(client, g.getLocation()), danger, dangerFill, config.borderWidth());
				}
			}
		}

		if (config.ocellusArtifacts())
		{
			Color c = config.ocellusArtifactColor();
			for (GameObject o : encounter.getArtifacts())
			{
				renderPoly(graphics, footprint(o), c, alpha(c, 40), config.borderWidth());
			}
		}

		if (config.ocellusProjectiles())
		{
			for (Projectile p : client.getProjectiles())
			{
				if (PROJECTILES.contains(p.getId()) && p.getRemainingCycles() > 0)
				{
					renderProjectile(graphics, p);
				}
			}
		}

		if (config.ocellusPortalCrossing())
		{
			renderPortalCrossing(graphics);
		}

		NPC ocellus = encounter.getBoss();
		if (config.ocellusAttackCounter() && ocellus != null && encounter.getAttackTicks() > 0)
		{
			String text = Integer.toString(encounter.getAttackTicks());
			renderText(graphics, ocellus.getCanvasTextLocation(graphics, text, config.textZ()), text, encounter.getAttackColor());
		}
	}

	/**
	 * A projectile thrown at the ground gets its landing tile marked with the ticks until it
	 * lands. One aimed at a player only shows if it's aimed at you, as a tick count on the
	 * projectile itself (like Projectile Ticks).
	 */
	private void renderProjectile(Graphics2D graphics, Projectile p)
	{
		Color c = p.getId() == SpotanimID.VFX_OCELLUS_WATER_ATTACK01_PROJANIM ? config.ocellusWaterColor()
			: p.getId() == SpotanimID.VFX_OCELLUS_FIRE_ATTACK01_PROJANIM ? config.ocellusFireColor()
			: config.ocellusProjectileColor();
		String ticks = Integer.toString((p.getRemainingCycles() + CYCLES_PER_TICK - 1) / CYCLES_PER_TICK);

		if (p.getTargetActor() == null)
		{
			WorldPoint landing = p.getTargetPoint();
			LocalPoint lp = landing != null ? LocalPoint.fromWorld(client, landing) : p.getTarget();
			if (lp != null)
			{
				renderPoly(graphics, Perspective.getCanvasTilePoly(client, lp), c, alpha(c, 50), config.borderWidth());
				renderText(graphics, Perspective.getCanvasTextLocation(client, graphics, lp, ticks, 0), ticks, c);
			}
			return;
		}

		// Aimed at a player: like Projectile Ticks, only when it's you, the ticks drawn on the
		// projectile itself
		if (p.getTargetActor() != client.getLocalPlayer())
		{
			return;
		}
		Point pt = Perspective.localToCanvas(client, (int) p.getX(), (int) p.getY(), (int) p.getZ());
		if (pt != null)
		{
			renderText(graphics, pt, Integer.toString(p.getRemainingCycles() / CYCLES_PER_TICK), c);
		}
	}

	private Polygon footprint(TileObject o)
	{
		if (o instanceof GameObject)
		{
			GameObject g = (GameObject) o;
			return Perspective.getCanvasTileAreaPoly(client, g.getLocalLocation(), Math.max(g.sizeX(), g.sizeY()));
		}
		return Perspective.getCanvasTilePoly(client, o.getLocalLocation());
	}

	private static Color alpha(Color c, int a)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
	}

	/**
	 * Each orange portal sits on the arena's edge and fires straight across: one on the west or
	 * east wall along its row, one on the north or south wall along its column. Where a row meets
	 * a column is a 2x2. Which wall a portal is on comes from where it is relative to the middle
	 * of the arena (worked out from the open floor). Only the crossing nearest you is drawn.
	 */
	private void renderPortalCrossing(Graphics2D graphics)
	{
		java.util.List<LocalPoint> orange = encounter.getOrangePortalPoints();
		net.runelite.api.Player me = client.getLocalPlayer();
		if (orange.size() < 2 || me == null || me.getLocalLocation() == null)
		{
			arenaCenter = null;
			return;
		}

		LocalPoint mine = me.getLocalLocation();
		if (arenaCenter == null)
		{
			arenaCenter = findArenaCenter(mine);
			if (arenaCenter == null)
			{
				return;
			}
		}

		java.util.List<LocalPoint> rows = new java.util.ArrayList<>();
		java.util.List<LocalPoint> columns = new java.util.ArrayList<>();
		for (LocalPoint p : orange)
		{
			int dx = Math.abs(p.getX() - arenaCenter.getX());
			int dy = Math.abs(p.getY() - arenaCenter.getY());
			(dx > dy ? rows : columns).add(p);
		}

		LocalPoint best = null;
		long bestDist = Long.MAX_VALUE;
		for (LocalPoint column : columns)
		{
			for (LocalPoint row : rows)
			{
				LocalPoint crossing = new LocalPoint(column.getX(), row.getY(), mine.getWorldView());
				long d = dist2(crossing, mine);
				if (d < bestDist)
				{
					bestDist = d;
					best = crossing;
				}
			}
		}

		if (best != null)
		{
			Color c = config.ocellusPortalCrossingColor();
			renderPoly(graphics, Perspective.getCanvasTileAreaPoly(client, best, 2), c, alpha(c, 60), config.borderWidth());
		}
	}

	/** Middle of the arena, from the open floor around you; reset when the portals go. */
	private LocalPoint arenaCenter;

	private LocalPoint findArenaCenter(LocalPoint from)
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null || wv.getCollisionMaps() == null)
		{
			return null;
		}
		int[][] flags = wv.getCollisionMaps()[wv.getPlane()].getFlags();
		int sx = from.getSceneX();
		int sy = from.getSceneY();
		int n = net.runelite.api.Constants.SCENE_SIZE;
		if (sx < 0 || sy < 0 || sx >= n || sy >= n)
		{
			return null;
		}

		boolean[][] seen = new boolean[n][n];
		java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
		queue.add(new int[]{sx, sy});
		seen[sx][sy] = true;
		int minX = sx;
		int maxX = sx;
		int minY = sy;
		int maxY = sy;
		while (!queue.isEmpty())
		{
			int[] t = queue.poll();
			minX = Math.min(minX, t[0]);
			maxX = Math.max(maxX, t[0]);
			minY = Math.min(minY, t[1]);
			maxY = Math.max(maxY, t[1]);
			int[][] next = {{t[0] + 1, t[1]}, {t[0] - 1, t[1]}, {t[0], t[1] + 1}, {t[0], t[1] - 1}};
			for (int[] q : next)
			{
				if (q[0] < 0 || q[1] < 0 || q[0] >= n || q[1] >= n || seen[q[0]][q[1]]
					|| Math.abs(q[0] - sx) > 40 || Math.abs(q[1] - sy) > 40
					|| (flags[q[0]][q[1]] & net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0)
				{
					continue;
				}
				seen[q[0]][q[1]] = true;
				queue.add(q);
			}
		}
		int tile = Perspective.LOCAL_TILE_SIZE;
		// Scene tiles back to local units, relative to where you are
		int cx = from.getX() + (((minX + maxX) / 2) - sx) * tile;
		int cy = from.getY() + (((minY + maxY) / 2) - sy) * tile;
		return new LocalPoint(cx, cy, from.getWorldView());
	}

	private static long dist2(LocalPoint a, LocalPoint b)
	{
		long dx = a.getX() - b.getX();
		long dy = a.getY() - b.getY();
		return dx * dx + dy * dy;
	}
}
