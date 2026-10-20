package net.runelite.client.plugins.fracturedarchive.ezekiel;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.ChatMessageType;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Constants;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Projectile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

/**
 * Ezekiel. Second encounter, paired with Bork. A justiciar of Saradomin.
 */
@Slf4j
@Singleton
public class Ezekiel extends Encounter
{
	// Related: TFA_EZEKIEL_SWORD_NOOP, TFA_EZEKIEL_DEATH_SWORD
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_EZEKIEL);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	/** The swords that fly across the room. The _NOOP version is a sword that's finished. */
	private static final int SWORD_ID = NpcID.TFA_EZEKIEL_SWORD;

	private final Map<NPC, SwordTrack> swords = new HashMap<>();

	/** Attack counter: ticks remaining (0 = none) and the attack that started it. */
	@Getter
	private int attackTicks;
	@Getter
	private int attackAnimation = -1;

	/** Ticks between Ezekiel's attacks. */
	private static final int ATTACK_SPEED = 5;

	/** Tick the finale started, or -1 when it isn't happening. */
	private int finaleStartTick = -1;

	/**
	 * The finale's swords, and whether each has been marked by lightning. The one lightning
	 * doesn't hit is the safe one.
	 */
	private final Map<NPC, Boolean> finaleSwords = new HashMap<>();

	/**
	 * The safe swords in the order they came up: each time lightning has hit every sword but one,
	 * the one it missed is the next safe sword (1, 2, 3...).
	 */
	private final java.util.List<NPC> safeOrder = new java.util.ArrayList<>();
	private int lastRoundTick = -1;
	/** Swords that had a lightning spotanim last tick, so a lingering one only counts once. */
	private final Set<NPC> lightningLastTick = new HashSet<>();
	/** Lightning projectiles already counted. */
	private final Set<Projectile> countedProjectiles = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
	/** Spotanims already logged on each finale sword, so the debug log only shows new ones. */
	private final Map<NPC, Set<Integer>> loggedSwordSpotAnims = new HashMap<>();

	private static final Set<Integer> FINALE_SWORD_ANIMATIONS = Set.of(
		AnimationID.VFX_EZEKIEL_FINALE_SWORD_DROP01, AnimationID.VFX_EZEKIEL_FINALE_SWORD_IDLE01,
		AnimationID.VFX_EZEKIEL_FINALE_SWORD_DROP02, AnimationID.VFX_EZEKIEL_FINALE_SWORD_IDLE02,
		AnimationID.VFX_EZEKIEL_FINALE_SWORD_DROP03, AnimationID.VFX_EZEKIEL_FINALE_SWORD_IDLE03,
		AnimationID.VFX_EZEKIEL_FINALE_SWORD_DROP04, AnimationID.VFX_EZEKIEL_FINALE_SWORD_IDLE04);

	/** A finale sword's own warning animation: taken to mean lightning is about to hit it. */
	private static final Set<Integer> FINALE_SWORD_TELEGRAPHS = Set.of(
		AnimationID.VFX_EZEKIEL_FINALE_SWORD_TELEGRAPH01,
		AnimationID.VFX_EZEKIEL_FINALE_SWORD_TELEGRAPH02,
		AnimationID.VFX_EZEKIEL_FINALE_SWORD_TELEGRAPH03,
		AnimationID.VFX_EZEKIEL_FINALE_SWORD_TELEGRAPH04);

	private static final Set<Integer> FINALE_SWORD_DESPAWNS = Set.of(
		AnimationID.VFX_EZEKIEL_FINALE_SWORD_DESPAWN01,
		AnimationID.VFX_EZEKIEL_FINALE_SWORD_DESPAWN02,
		AnimationID.VFX_EZEKIEL_FINALE_SWORD_DESPAWN03,
		AnimationID.VFX_EZEKIEL_FINALE_SWORD_DESPAWN04);

	/** Graphics that land on a sword when lightning marks or hits it. Not confirmed in game yet. */
	private static final Set<Integer> LIGHTNING_GRAPHICS = Set.of(
		SpotanimID.VFX_EZEKIEL_SWORD_TELEGRAPH01,
		SpotanimID.VFX_EZEKIEL_SWORD_TELEGRAPH02,
		SpotanimID.VFX_EZEKIEL_SWORD_TELEGRAPH03,
		SpotanimID.VFX_EZEKIEL_SWORD_TELEGRAPH04,
		SpotanimID.VFX_EZEKIEL_FINALE_IMPACT_LARGE01,
		SpotanimID.VFX_EZEKIEL_FINALE_IMPACT_MEDIUM01,
		SpotanimID.VFX_EZEKIEL_FINALE_IMPACT_SMALL01);

	@Inject
	private EzekielOverlay overlay;

	@Inject
	protected Ezekiel(FracturedArchiveConfig config)
	{
		super(config);
	}

	@Override
	protected Set<Integer> bossIds()
	{
		return BOSS_IDS;
	}

	@Override
	protected EncounterOverlay overlay()
	{
		return overlay;
	}

	@Override
	protected void reset()
	{
		super.reset();
		swords.clear();
		roomCenter = null;
		attackTicks = 0;
		attackAnimation = -1;
		finaleStartTick = -1;
		finaleSwords.clear();
		loggedSwordSpotAnims.clear();
		clearSafeOrder();
	}

	Map<NPC, SwordTrack> getSwords()
	{
		return Collections.unmodifiableMap(swords);
	}

	// Swords are tracked whether or not Ezekiel has been picked up yet.

	@Override
	public void onNpcSpawned(NpcSpawned event)
	{
		super.onNpcSpawned(event);
		NPC npc = event.getNpc();
		if (isActive() || !swords.isEmpty())
		{
			debug("npc " + npc.getId() + " (" + npc.getName() + ") at " + tile(npc.getWorldLocation()));
		}
		if (npc.getId() == SWORD_ID)
		{
			swords.put(npc, new SwordTrack(client.getTickCount()));
		}
	}

	@Override
	public void onNpcChanged(NpcChanged event)
	{
		super.onNpcChanged(event);
		NPC npc = event.getNpc();
		if (npc.getId() == SWORD_ID)
		{
			swords.putIfAbsent(npc, new SwordTrack(client.getTickCount()));
		}
		else
		{
			// e.g. turned into the finished (_NOOP) sword
			swords.remove(npc);
		}
	}

	@Override
	public void onNpcDespawned(NpcDespawned event)
	{
		super.onNpcDespawned(event);
		swords.remove(event.getNpc());
		finaleSwords.remove(event.getNpc());
		lightningLastTick.remove(event.getNpc());
	}

	@Override
	public void onGameTick(GameTick event)
	{
		checkSwordSpotAnims();

		if (attackTicks > 0 && --attackTicks == 0)
		{
			attackAnimation = -1;
		}

		// Lock each sword's direction the first tick after it appears. After that it's never
		// changed, because a sword turns to face whoever hits it.
		for (Map.Entry<NPC, SwordTrack> entry : swords.entrySet())
		{
			SwordTrack track = entry.getValue();
			if (!track.hasDirection())
			{
				int[] dir = pickDirection(entry.getKey());
				track.setDirection(dir[0], dir[1]);
			}
		}
	}

	@Override
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		int animation = npc.getAnimation();

		// Ezekiel, and any NPC during the finale (so the swords' animations are recorded too)
		if (bossIds().contains(npc.getId()) || npc.getId() == NpcID.TFA_EZEKIEL_FINALE_VFX || finaleStartTick >= 0)
		{
			debug("anim " + animation + " on " + npc.getName() + " (" + npc.getId() + ") at " + tile(npc.getWorldLocation()));
		}

		if (FINALE_SWORD_ANIMATIONS.contains(animation))
		{
			finaleSwords.putIfAbsent(npc, false);
		}
		else if (FINALE_SWORD_TELEGRAPHS.contains(animation))
		{
			finaleSwords.putIfAbsent(npc, false);
			strike(npc);
		}
		else if (FINALE_SWORD_DESPAWNS.contains(animation))
		{
			finaleSwords.remove(npc);
		}

		if (!bossIds().contains(npc.getId()))
		{
			return;
		}

		switch (animation)
		{
			case AnimationID.NPC_EZEKIEL_MELEE01:
			case AnimationID.NPC_EZEKIEL_MAGIC01:
			case AnimationID.NPC_EZEKIEL_SPECIAL01:
				attackAnimation = animation;
				// +1 so the first number shown is the full attack speed
				attackTicks = ATTACK_SPEED + 1;
				break;
			case AnimationID.NPC_EZEKIEL_FINALE01:
				finaleStartTick = client.getTickCount();
				clearSafeOrder();
				attackTicks = 0;
				attackAnimation = -1;
				break;
			case AnimationID.NPC_EZEKIEL_FINALE01_VFX:
			case AnimationID.NPC_EZEKIEL_FINALE_LOOP01:
				if (finaleStartTick < 0)
				{
					finaleStartTick = client.getTickCount();
				}
				break;
			case AnimationID.NPC_EZEKIEL_FINALE_DEATH01:
			case AnimationID.NPC_EZEKIEL_DEATH01:
				finaleStartTick = -1;
				finaleSwords.clear();
			loggedSwordSpotAnims.clear();
			clearSafeOrder();
		loggedSwordSpotAnims.clear();
				break;
			default:
				break;
		}
	}

	@Override
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		GraphicsObject lightning = event.getGraphicsObject();
		// During the finale, a new graphic on a sword's tiles is taken as lightning hitting it
		// (except the star special's own graphics)
		if ((finaleStartTick >= 0 || LIGHTNING_GRAPHICS.contains(lightning.getId()))
			&& !STAR_GRAPHIC_IDS.contains(lightning.getId()))
		{
			NPC sword = swordAt(lightning.getLocation());
			if (sword != null)
			{
				debug("gfx " + lightning.getId() + " on finale sword at " + tile(sword.getWorldLocation()));
				strike(sword);
			}
		}

		if (isActive() || !swords.isEmpty())
		{
			GraphicsObject g = event.getGraphicsObject();
			debug("gfx " + g.getId() + " at " + tile(WorldPoint.fromLocal(client, g.getLocation())));
		}
	}

	@Override
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (isActive())
		{
			GameObject o = event.getGameObject();
			debug("object " + o.getId() + " at " + tile(o.getWorldLocation()));
		}
	}

	/**
	 * Lightning can show as a spotanim played on the sword itself. Mark the sword if it has one
	 * of the lightning graphics, and log any new spotanim when the debug log is on.
	 */
	private void checkSwordSpotAnims()
	{
		Set<NPC> hasLightning = new HashSet<>();
		for (NPC sword : new java.util.ArrayList<>(finaleSwords.keySet()))
		{
			for (ActorSpotAnim spotAnim : sword.getSpotAnims())
			{
				int id = spotAnim.getId();
				if (LIGHTNING_GRAPHICS.contains(id))
				{
					hasLightning.add(sword);
					if (!lightningLastTick.contains(sword))
					{
						strike(sword);
					}
				}
				if (loggedSwordSpotAnims.computeIfAbsent(sword, k -> new HashSet<>()).add(id))
				{
					debug("spotanim " + id + " on finale sword " + sword.getId() + " at " + tile(sword.getWorldLocation()));
				}
			}
		}
		lightningLastTick.clear();
		lightningLastTick.addAll(hasLightning);
	}

	@Override
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		Actor target = projectile.getTargetActor();
		// Lightning fired at a finale sword
		if (target instanceof NPC && finaleSwords.containsKey(target))
		{
			if (countedProjectiles.add(projectile))
			{
				debug("projectile " + projectile.getId() + " at finale sword " + ((NPC) target).getId() + " at " + tile(target.getWorldLocation()));
				strike((NPC) target);
			}
		}
		else if (finaleStartTick >= 0 && loggedProjectiles.add(projectile))
		{
			debug("projectile " + projectile.getId() + " to " + tile(projectile.getTargetPoint()));
			// A projectile thrown at the ground on a sword's tiles counts as lightning too
			WorldPoint landing = projectile.getTargetPoint();
			NPC sword = landing != null ? swordAt(LocalPoint.fromWorld(client, landing)) : null;
			if (sword != null && countedProjectiles.add(projectile))
			{
				strike(sword);
			}
		}
	}

	/** Finale projectiles already logged. */
	private final Set<Projectile> loggedProjectiles = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

	/** The star special's graphics, which aren't lightning. */
	private static final Set<Integer> STAR_GRAPHIC_IDS = Set.of(
		SpotanimID.EZEKIEL_SPECIAL_CRACK_VFX01,
		SpotanimID.VFX_EZEKIEL_STAR_EXPLOSION_SPAWN01,
		SpotanimID.VFX_EZEKIEL_STAR_EXPLOSION_MARKER01,
		SpotanimID.VFX_EZEKIEL_STAR_EXPLOSION01);

	/** The finale sword whose tiles (plus 1 tile around) cover this point, or null. */
	private NPC swordAt(LocalPoint at)
	{
		if (at == null)
		{
			return null;
		}
		for (NPC sword : finaleSwords.keySet())
		{
			LocalPoint lp = sword.getLocalLocation();
			NPCComposition c = sword.getTransformedComposition();
			int reach = (c != null ? c.getSize() : 1) * Perspective.LOCAL_TILE_SIZE / 2 + Perspective.LOCAL_TILE_SIZE;
			if (lp != null && Math.abs(lp.getX() - at.getX()) <= reach && Math.abs(lp.getY() - at.getY()) <= reach)
			{
				return sword;
			}
		}
		return null;
	}

	/**
	 * Lightning hit a finale sword. Once it has hit every sword but one this round, the one it
	 * missed is the next safe sword, and a new round starts.
	 */
	private void strike(NPC sword)
	{
		if (!finaleSwords.containsKey(sword) || finaleSwords.get(sword))
		{
			return;
		}
		finaleSwords.put(sword, true);

		long hit = finaleSwords.values().stream().filter(b -> b).count();
		int tick = client.getTickCount();
		if (finaleSwords.size() >= 2 && hit == finaleSwords.size() - 1 && tick != lastRoundTick)
		{
			for (Map.Entry<NPC, Boolean> entry : finaleSwords.entrySet())
			{
				if (!entry.getValue())
				{
					safeOrder.add(entry.getKey());
					debug("safe sword " + safeOrder.size() + " at " + tile(entry.getKey().getWorldLocation()));
				}
				entry.setValue(false);
			}
			lastRoundTick = tick;
		}
	}

	private void clearSafeOrder()
	{
		safeOrder.clear();
		lastRoundTick = -1;
		lightningLastTick.clear();
		for (Map.Entry<NPC, Boolean> entry : finaleSwords.entrySet())
		{
			entry.setValue(false);
		}
	}

	/** The safe swords, in order (a sword can come up more than once). */
	java.util.List<NPC> getSafeOrder()
	{
		return Collections.unmodifiableList(safeOrder);
	}

	/** The finale's swords, each with whether lightning has marked it. */
	Map<NPC, Boolean> getFinaleSwords()
	{
		return Collections.unmodifiableMap(finaleSwords);
	}

	/** Ticks since the finale started, or -1 when it isn't happening. */
	int getFinaleTicks()
	{
		return finaleStartTick < 0 ? -1 : client.getTickCount() - finaleStartTick;
	}

	java.awt.Color getAttackColor()
	{
		switch (attackAnimation)
		{
			case AnimationID.NPC_EZEKIEL_MELEE01:
				return config.ezekielMeleeColor();
			case AnimationID.NPC_EZEKIEL_MAGIC01:
				return config.ezekielMagicColor();
			default:
				return config.ezekielSpecialColor();
		}
	}

	private void debug(String message)
	{
		String finale = finaleStartTick >= 0 ? " finale+" + getFinaleTicks() : "";
		String line = "[Ezekiel t" + client.getTickCount() + finale + "] " + message;
		log.info("{}", line);
		if (config.ezekielDebug())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", line, null);
		}
	}

	private static String tile(WorldPoint p)
	{
		return p == null ? "?" : p.getX() + "," + p.getY();
	}

	@Override
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			swords.clear();
			roomCenter = null;
			finaleSwords.clear();
			loggedSwordSpotAnims.clear();
			clearSafeOrder();
		loggedSwordSpotAnims.clear();
		}
	}

	/** Ticks until the sword goes off (0 or less once it has). */
	int ticksUntilLaunch(SwordTrack track)
	{
		return config.ezekielSwordTicks() - (client.getTickCount() - track.getSpawnTick());
	}

	/** Center of the room in scene tiles, worked out once per fight from the open floor. */
	private int[] roomCenter;

	/**
	 * The straight direction the sword will fly: straight across to the opposite side of the
	 * room. A sword on the north side goes south, on the east side goes west, and so on.
	 */
	private int[] pickDirection(NPC sword)
	{
		LocalPoint start = sword.getLocalLocation();
		WorldView wv = client.getTopLevelWorldView();
		if (start == null || wv == null || wv.getCollisionMaps() == null)
		{
			return cardinal(sword.getOrientation());
		}

		int[][] flags = wv.getCollisionMaps()[wv.getPlane()].getFlags();
		int sx = start.getSceneX();
		int sy = start.getSceneY();
		if (roomCenter == null)
		{
			roomCenter = findRoomCenter(flags, sx, sy);
		}
		if (roomCenter == null)
		{
			return cardinal(sword.getOrientation());
		}

		int offX = sx - roomCenter[0];
		int offY = sy - roomCenter[1];
		if (Math.abs(offX) == Math.abs(offY))
		{
			// Exactly on a diagonal: fall back to the way it's facing
			return cardinal(sword.getOrientation());
		}
		return Math.abs(offY) > Math.abs(offX)
			? new int[]{0, -Integer.signum(offY)}  // north side goes south, south side goes north
			: new int[]{-Integer.signum(offX), 0}; // east side goes west, west side goes east
	}

	private static final int ROOM_RADIUS = 30;

	/**
	 * Fills the open floor next to the sword (within ROOM_RADIUS tiles) and returns the middle of
	 * what it covers, or null if the sword isn't next to any floor.
	 */
	private static int[] findRoomCenter(int[][] flags, int sx, int sy)
	{
		int[] open = null;
		// Swords sit in the wall, so find the nearest open tile first
		for (int r = 0; r <= 4 && open == null; r++)
		{
			for (int x = sx - r; x <= sx + r && open == null; x++)
			{
				for (int y = sy - r; y <= sy + r; y++)
				{
					if (isOpen(flags, x, y))
					{
						open = new int[]{x, y};
						break;
					}
				}
			}
		}
		if (open == null)
		{
			return null;
		}

		boolean[][] seen = new boolean[Constants.SCENE_SIZE][Constants.SCENE_SIZE];
		java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
		queue.add(open);
		seen[open[0]][open[1]] = true;
		int minX = open[0], maxX = open[0], minY = open[1], maxY = open[1];
		while (!queue.isEmpty())
		{
			int[] t = queue.poll();
			minX = Math.min(minX, t[0]);
			maxX = Math.max(maxX, t[0]);
			minY = Math.min(minY, t[1]);
			maxY = Math.max(maxY, t[1]);
			int[][] next = {{t[0] + 1, t[1]}, {t[0] - 1, t[1]}, {t[0], t[1] + 1}, {t[0], t[1] - 1}};
			for (int[] n : next)
			{
				if (Math.abs(n[0] - sx) > ROOM_RADIUS || Math.abs(n[1] - sy) > ROOM_RADIUS
					|| !isOpen(flags, n[0], n[1]) || seen[n[0]][n[1]])
				{
					continue;
				}
				seen[n[0]][n[1]] = true;
				queue.add(n);
			}
		}
		return new int[]{(minX + maxX) / 2, (minY + maxY) / 2};
	}

	private static boolean isOpen(int[][] flags, int x, int y)
	{
		return x >= 0 && y >= 0 && x < Constants.SCENE_SIZE && y < Constants.SCENE_SIZE
			&& (flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
	}

	/**
	 * Turns an orientation (0 = south, 512 = west, 1024 = north, 1536 = east) into the nearest of
	 * the 4 straight directions. Swords never go diagonally.
	 */
	static int[] cardinal(int orientation)
	{
		int quarter = Math.floorMod(Math.round(orientation / 512f), 4);
		switch (quarter)
		{
			case 0:
				return new int[]{0, -1}; // south
			case 1:
				return new int[]{-1, 0}; // west
			case 2:
				return new int[]{0, 1}; // north
			default:
				return new int[]{1, 0}; // east
		}
	}

	/** When a sword appeared and the direction it will fly, locked in once. */
	static final class SwordTrack
	{
		private final int spawnTick;
		private int dx;
		private int dy;
		private boolean hasDirection;

		SwordTrack(int spawnTick)
		{
			this.spawnTick = spawnTick;
		}

		void setDirection(int dx, int dy)
		{
			this.dx = dx;
			this.dy = dy;
			hasDirection = true;
		}

		boolean hasDirection()
		{
			return hasDirection;
		}

		int getSpawnTick()
		{
			return spawnTick;
		}

		int getDx()
		{
			return dx;
		}

		int getDy()
		{
			return dy;
		}
	}
}
