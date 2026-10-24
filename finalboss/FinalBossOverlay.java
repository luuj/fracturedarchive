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

	/** Ocellus's projectiles (not the portal projectiles fired at the walls in phase 2). */
	private static final Set<Integer> PROJECTILES = Set.of(
		SpotanimID.VFX_OCELLUS_WATER_ATTACK01_PROJANIM,
		SpotanimID.VFX_OCELLUS_FIRE_ATTACK01_PROJANIM,
		SpotanimID.VFX_OCELLUS_MAGIC_PROJANIM01,
		SpotanimID.VFX_OCELLUS_MAGIC_PROJANIM02,
		SpotanimID.VFX_OCELLUS_PHASE1_CHARGE_EXPEND_PROJECTILE,
		SpotanimID.TFA_OCELLUS_SF_PROJ);

	@Inject
	private FinalBoss encounter;

	/** His phase 1 water and fire projectiles. */
	private static final Color WATER_COLOR = Color.CYAN;
	private static final Color FIRE_COLOR = Color.GREEN;

	/** Void bomb warnings and when each was first seen (they land 2 ticks later). */
	private static final int BOMB_WARNING_TICKS = 2;
	private final java.util.Map<String, Integer> bombWarnings = new java.util.HashMap<>();


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
			Color danger = FinalBoss.DANGER_COLOR;
			Color dangerFill = alpha(danger, 50);

			for (TileObject o : encounter.getDangerObjects())
			{
				renderPoly(graphics, footprint(o), danger, dangerFill, config.borderWidth());
			}

			java.util.Set<String> warnings = new java.util.HashSet<>();
			for (GraphicsObject g : wv.getGraphicsObjects())
			{
				if (DANGER_GRAPHICS.contains(g.getId()))
				{
					renderPoly(graphics, Perspective.getCanvasTilePoly(client, g.getLocation()), danger, dangerFill, config.borderWidth());
				}
				if (g.getId() == SpotanimID.VFX_OCELLUS_QUADRANT_BOMB_IDLE_01 && g.getLocation() != null)
				{
					// Void bomb warning: lands 2 ticks after it first appears
					LocalPoint lp = g.getLocation();
					String key = lp.getX() + "," + lp.getY();
					warnings.add(key);
					int start = bombWarnings.computeIfAbsent(key, k -> client.getTickCount());
					int left = BOMB_WARNING_TICKS - (client.getTickCount() - start);
					if (left > 0)
					{
						String text = Integer.toString(left);
						renderText(graphics, Perspective.getCanvasTextLocation(client, graphics, lp, text, 0), text, danger);
					}
				}
			}
			bombWarnings.keySet().retainAll(warnings);
		}

		if (config.ocellusArtifacts())
		{
			Color c = FinalBoss.ARTIFACT_COLOR;
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
		if (config.ocellusVoidPortals() || config.ocellusVoidVine())
		{
			renderVoid(graphics, wv);
		}

		if (config.ocellusSoulFragments())
		{
			renderSoulFragments(graphics, wv);
		}

		if (config.ocellusVoidChain())
		{
			renderVoidChain(graphics);
		}

		int sweep = encounter.getSweepCount();
		if (config.ocellusSweepCounter() && ocellus != null && sweep > 0)
		{
			// Stacked directly under the tick counter (like Theatre), in a smaller font
			renderStackedText(graphics, ocellus, Integer.toString(sweep), sweep >= 3 ? FinalBoss.SPECIAL_COLOR : FinalBoss.MELEE_COLOR, 20f);
		}

		int deflect = encounter.getDeflectTicks();
		if (config.ocellusDeflectCounter() && ocellus != null && deflect > 0)
		{
			// Small, under the attack counter (like the sweep count): green = ranged deflect, red = melee
			renderStackedText(graphics, ocellus, Integer.toString(deflect), encounter.getDeflectColor(), 20f);
		}

		int obliteration = encounter.getObliterationTicks();
		int unleash = encounter.getUnleashTicks();
		if (config.ocellusObliterationCounter() && ocellus != null && obliteration > 0)
		{
			String text = Integer.toString(obliteration);
			renderText(graphics, ocellus.getCanvasTextLocation(graphics, text, config.textZ()), text, FinalBoss.OBLITERATION_COLOR);
		}
		else if (config.ocellusUnleashCounter() && ocellus != null && unleash > 0)
		{
			String text = Integer.toString(unleash);
			renderText(graphics, ocellus.getCanvasTextLocation(graphics, text, config.textZ()), text, FinalBoss.SPECIAL_COLOR);
		}
		else if (config.ocellusAttackCounter() && ocellus != null && encounter.getAttackTicks() > 0)
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
		Color c = p.getId() == SpotanimID.VFX_OCELLUS_WATER_ATTACK01_PROJANIM ? WATER_COLOR
			: p.getId() == SpotanimID.VFX_OCELLUS_FIRE_ATTACK01_PROJANIM ? FIRE_COLOR
			: FinalBoss.PROJECTILE_COLOR;
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

	/** Soul fragment graphics while they're spawning or sitting on the floor (not despawning). */
	private static final Set<Integer> SOUL_FRAGMENTS = Set.of(
		SpotanimID.VFX_OCELLUS_SOUL_FRAGMENT_SPAWN_6_TICKS,
		SpotanimID.VFX_OCELLUS_SOUL_FRAGMENT_SPAWN_12_TICKS,
		SpotanimID.VFX_OCELLUS_SOUL_FRAGMENT_IDLE);

	/** When each spawning fragment was first seen, so its countdown runs from there. */
	private final java.util.Map<String, Integer> fragmentStarts = new java.util.HashMap<>();

	/**
	 * The soul fragments (the orbs) on their tiles. They can be a floor graphic or a graphic on an
	 * invisible NPC, so both are checked. While one is spawning it counts down the ticks its spawn
	 * takes (the game names them 6 and 12 ticks).
	 */
	private void renderSoulFragments(Graphics2D graphics, WorldView wv)
	{
		Color c = FinalBoss.SOUL_FRAGMENT_COLOR;
		Color fill = alpha(c, 70);
		java.util.Set<String> seen = new java.util.HashSet<>();

		for (GraphicsObject g : wv.getGraphicsObjects())
		{
			if (SOUL_FRAGMENTS.contains(g.getId()) && !g.finished() && g.getLocation() != null)
			{
				LocalPoint lp = g.getLocation();
				renderPoly(graphics, Perspective.getCanvasTilePoly(client, lp), c, fill, config.borderWidth());
				String key = "g" + g.getId() + ":" + lp.getX() + "," + lp.getY();
				seen.add(key);
				renderFragmentTicks(graphics, key, g.getId(), lp, c);
			}
		}
		for (NPC npc : wv.npcs())
		{
			if (npc == encounter.getBoss() || npc.getLocalLocation() == null)
			{
				continue;
			}
			for (net.runelite.api.ActorSpotAnim a : npc.getSpotAnims())
			{
				if (SOUL_FRAGMENTS.contains(a.getId()))
				{
					renderTile(graphics, npc.getWorldLocation(), c, fill, config.borderWidth());
					String key = "n" + a.getId() + ":" + npc.getIndex();
					seen.add(key);
					renderFragmentTicks(graphics, key, a.getId(), npc.getLocalLocation(), c);
					break;
				}
			}
		}
		fragmentStarts.keySet().retainAll(seen);
	}

	private void renderFragmentTicks(Graphics2D graphics, String key, int id, LocalPoint lp, Color c)
	{
		int length = id == SpotanimID.VFX_OCELLUS_SOUL_FRAGMENT_SPAWN_6_TICKS ? 6
			: id == SpotanimID.VFX_OCELLUS_SOUL_FRAGMENT_SPAWN_12_TICKS ? 12
			: 0;
		if (length == 0)
		{
			// Already landed (idle): no countdown
			return;
		}
		int start = fragmentStarts.computeIfAbsent(key, k -> client.getTickCount());
		int left = length - (client.getTickCount() - start);
		if (left > 0)
		{
			String text = Integer.toString(left);
			renderText(graphics, Perspective.getCanvasTextLocation(client, graphics, lp, text, 0), text, c);
		}
	}

	/**
	 * Phase 3's void portals, each in its own color, and the tile of any player a Void Vine has
	 * trapped (the vine sits on the trapped player).
	 */
	private void renderVoid(Graphics2D graphics, WorldView wv)
	{
		for (NPC npc : wv.npcs())
		{
			if (npc.isDead())
			{
				continue;
			}

			Color c;
			switch (npc.getId())
			{
				case net.runelite.api.gameval.NpcID.TFA_OCELLUS_P3_BOMBPORTAL_HEAL:
					c = FinalBoss.SIPHON_COLOR;
					break;
				case net.runelite.api.gameval.NpcID.TFA_OCELLUS_P3_BOMBPORTAL_BUFF:
					c = FinalBoss.AUGMENTER_COLOR;
					break;
				case net.runelite.api.gameval.NpcID.TFA_OCELLUS_P3_BOMBPORTAL_BASIC:
					c = FinalBoss.RIFT_COLOR;
					break;
				case net.runelite.api.gameval.NpcID.TFA_OCELLUS_P3_VOID_VINE:
					if (config.ocellusVoidVine())
					{
						renderTrapped(graphics, wv, npc);
					}
					continue;
				default:
					continue;
			}

			if (config.ocellusVoidPortals())
			{
				net.runelite.api.NPCComposition comp = npc.getTransformedComposition();
				int size = comp != null ? Math.max(1, comp.getSize()) : 1;
				renderPoly(graphics, Perspective.getCanvasTileAreaPoly(client, npc.getLocalLocation(), size), c, alpha(c, 50), config.borderWidth());
			}
		}
	}

	/** The true tile of the player the vine has trapped, or the vine's own tile if no one's on it. */
	/** Void chain: the ticks until the next explosion, over your character (when you're warned). */
	private void renderVoidChain(Graphics2D graphics)
	{
		net.runelite.api.Player me = client.getLocalPlayer();
		int ticks = encounter.getChainTicks();
		if (me == null || ticks <= 0 || !encounter.getChainTargets().contains(me))
		{
			return;
		}
		String text = Integer.toString(ticks);
		// Size 20, one line below the usual over-head spot so other over-head counters (like a
		// weapon tick counter) don't cover it
		java.awt.Font font = graphics.getFont();
		graphics.setFont(font.deriveFont(20f));
		Point p = me.getCanvasTextLocation(graphics, text, config.textZ());
		if (p != null)
		{
			// A thick black outline: the "2" shows on the tick each teal explosion goes off right
			// beside you, and plain text disappears into it
			int x = p.getX();
			int y = p.getY() + graphics.getFontMetrics().getHeight();
			graphics.setColor(Color.BLACK);
			for (int dx = -2; dx <= 2; dx++)
			{
				for (int dy = -2; dy <= 2; dy++)
				{
					if (dx != 0 || dy != 0)
					{
						graphics.drawString(text, x + dx, y + dy);
					}
				}
			}
			graphics.setColor(FinalBoss.VOID_CHAIN_COLOR);
			graphics.drawString(text, x, y);
		}
		graphics.setFont(font);
	}

	private void renderTrapped(Graphics2D graphics, WorldView wv, NPC vine)
	{
		WorldPoint at = vine.getWorldLocation();
		if (at == null)
		{
			return;
		}

		WorldPoint tile = at;
		int best = Integer.MAX_VALUE;
		for (net.runelite.api.Player player : wv.players())
		{
			WorldPoint p = player.getWorldLocation();
			int d = p == null ? Integer.MAX_VALUE : p.distanceTo(at);
			if (d <= 1 && d < best)
			{
				best = d;
				tile = p;
			}
		}

		Color c = FinalBoss.TRAPPED_COLOR;
		renderTile(graphics, tile, c, alpha(c, 80), config.borderWidth());
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
	 * The "safe portal" the game puts on the floor after the portals finish: just its own tile.
	 * One shows per quadrant; only the one nearest you is drawn. (The 2x2 where the orange
	 * portals' lines cross, shown as soon as they spawn, still needs the portals found first.)
	 */
	private void renderPortalCrossing(Graphics2D graphics)
	{
		// The 2x2 where the orange (silent) portals' lanes cross, as soon as the portals fire
		// The next one, early: dimmer and in blue
		WorldPoint next = encounter.getNextCrossing();
		LocalPoint nextSw = next != null ? LocalPoint.fromWorld(client, next) : null;
		if (nextSw != null)
		{
			Color c = FinalBoss.NEXT_CROSSING_COLOR;
			LocalPoint middle = nextSw.plus(Perspective.LOCAL_TILE_SIZE / 2, Perspective.LOCAL_TILE_SIZE / 2);
			renderPoly(graphics, Perspective.getCanvasTileAreaPoly(client, middle, 2), alpha(c, 230), alpha(c, 90), config.borderWidth());
		}

		for (WorldPoint crossing : encounter.getOrangeCrossings())
		{
			LocalPoint sw = LocalPoint.fromWorld(client, crossing);
			if (sw != null)
			{
				Color c = FinalBoss.PORTAL_CROSSING_COLOR;
				LocalPoint middle = sw.plus(Perspective.LOCAL_TILE_SIZE / 2, Perspective.LOCAL_TILE_SIZE / 2);
				renderPoly(graphics, Perspective.getCanvasTileAreaPoly(client, middle, 2), c, alpha(c, 60), config.borderWidth());
			}
		}

		net.runelite.api.Player me = client.getLocalPlayer();
		WorldView wv = client.getTopLevelWorldView();
		if (me == null || me.getLocalLocation() == null || wv == null)
		{
			return;
		}

		LocalPoint best = null;
		long bestDist = Long.MAX_VALUE;
		for (GraphicsObject g : wv.getGraphicsObjects())
		{
			if (!FinalBoss.ORANGE_PORTAL_GRAPHICS.contains(g.getId()) || g.getLocation() == null)
			{
				continue;
			}
			long d = dist2(g.getLocation(), me.getLocalLocation());
			if (d < bestDist)
			{
				bestDist = d;
				best = g.getLocation();
			}
		}

		if (best != null)
		{
			Color c = FinalBoss.PORTAL_CROSSING_COLOR;
			renderPoly(graphics, Perspective.getCanvasTilePoly(client, best), c, alpha(c, 60), config.borderWidth());
		}
	}

	private static long dist2(LocalPoint a, LocalPoint b)
	{
		long dx = a.getX() - b.getX();
		long dy = a.getY() - b.getY();
		return dx * dx + dy * dy;
	}
}
