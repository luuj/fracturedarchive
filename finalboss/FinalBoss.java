package net.runelite.client.plugins.fracturedarchive.finalboss;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

/**
 * Final boss: Ocellus (TFA_OCELLUS_P1-P3 in the game data), in three phases.
 * Related: TFA_OCELLUS_P3_BOMBPORTAL_BASIC/HEAL/BUFF, TFA_OCELLUS_P3_VOID_VINE
 */
@Slf4j
@Singleton
public class FinalBoss extends Encounter
{
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_OCELLUS_P1, NpcID.TFA_OCELLUS_P2, NpcID.TFA_OCELLUS_P3);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	enum AttackStyle
	{
		MELEE, PROJECTILE, SPECIAL, PRAYER
	}

	/** Attacks that start the counter, by style. Timings aren't confirmed yet. */
	private static final Map<Integer, AttackStyle> ATTACKS = Map.ofEntries(
		// Phase 1
		Map.entry(AnimationID.NPC_OCELLUS_PHASE1_BASIC_PROJECTILE_START, AttackStyle.PROJECTILE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE1_RAILGUN, AttackStyle.SPECIAL),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE1_CHARGE, AttackStyle.SPECIAL),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE1_EXPEND, AttackStyle.SPECIAL),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE1_EXPLODE, AttackStyle.SPECIAL),
		// Phase 3
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_BASIC, AttackStyle.MELEE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_BASIC_SILENT, AttackStyle.MELEE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_HYBRID, AttackStyle.MELEE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_PRAYER, AttackStyle.MELEE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_SLASH, AttackStyle.MELEE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_SLASH_SILENT, AttackStyle.MELEE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_SUPERIOR_SLASH, AttackStyle.SPECIAL),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_BASIC_PROJECTILE_START, AttackStyle.PROJECTILE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_BASIC_PROJECTILE_ALL, AttackStyle.PROJECTILE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_BASIC_PROJECTILE_NOLOOP, AttackStyle.PROJECTILE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_SOUL_FRAGMENT_START, AttackStyle.SPECIAL),
		// Unleash has its own counter (see onGraphicsObjectCreated)
		Map.entry(AnimationID.NPC_OCELLUS_QUADRANT_DESTROY_IN, AttackStyle.SPECIAL),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_DEFLECT_IN, AttackStyle.SPECIAL),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_DEFLECT_IN_SILENT, AttackStyle.SPECIAL),
		// The special that turns off prayer
		// The loop and out parts don't restart it (see onAnimationChanged)
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_PRAYER_IN, AttackStyle.PRAYER));

	/** Length for attacks that haven't been timed yet. */
	private static final int DEFAULT_ATTACK_TICKS = 5;

	/** Attacks whose length has been confirmed in game, in ticks. */
	private static final Map<Integer, Integer> KNOWN_TICKS = Map.of(
		AnimationID.NPC_OCELLUS_PHASE3_MELEE_BASIC, 3,
		AnimationID.NPC_OCELLUS_PHASE3_MELEE_SLASH, 4,
		// The special that turns off prayer: it hits 5 ticks after he starts it
		AnimationID.NPC_OCELLUS_PHASE3_PRAYER_IN, 5);

	/**
	 * Later in phase 3 he holds the prayer special (14870, played as the first hit lands) and a
	 * second hit comes 3 ticks after the first.
	 */
	private static final int PRAYER_SECOND_HIT = 3;

	/** Void degen bombs and puddles on the floor: stand off these. */
	private static final Set<Integer> DANGER_OBJECTS = Set.of(
		ObjectID.TFA_OCELLUS_VOID_DEGEN_BOMB,
		ObjectID.TFA_OCELLUS_VOID_DEGEN_AOE,
		ObjectID.TFA_OCELLUS_VOID_DEGEN_AOE_INSOLENCE_1,
		ObjectID.TFA_OCELLUS_VOID_DEGEN_AOE_INSOLENCE_4,
		ObjectID.TFA_OCELLUS_VOID_DEGEN_AOE_INSOLENCE_7);

	/** The artifacts while they're usable (each has an _INACTIVE version). */
	private static final Set<Integer> ACTIVE_ARTIFACTS = Set.of(
		ObjectID.TFA_OCELLUS_ARTIFACT_DAMAGE_MELEE,
		ObjectID.TFA_OCELLUS_ARTIFACT_DAMAGE_RANGED,
		ObjectID.TFA_OCELLUS_ARTIFACT_SPEC,
		ObjectID.TFA_OCELLUS_ARTIFACT_HEAL);

	/** Phase 3's portal NPCs, by type. */
	public enum PortalType
	{
		BASIC(NpcID.TFA_OCELLUS_P3_BOMBPORTAL_BASIC),
		HEAL(NpcID.TFA_OCELLUS_P3_BOMBPORTAL_HEAL),
		BUFF(NpcID.TFA_OCELLUS_P3_BOMBPORTAL_BUFF);

		public final int npcId;

		PortalType(int npcId)
		{
			this.npcId = npcId;
		}
	}

	private final Set<NPC> portals = new java.util.HashSet<>();

	/**
	 * Unleash: four waves of floor AoEs 4 ticks apart, then he falls back down (the floor-wide
	 * hit) 12 ticks after the fourth wave. The counter runs from the fourth wave to his landing.
	 */
	private static final int ARENA_HIT_AFTER_LAST_WAVE = 12;
	private static final int UNLEASH_WAVES = 4;
	private boolean unleashing;
	private int unleashWaves;
	private int lastWaveTick = -1;
	private int arenaHitTick = -1;

	/**
	 * Phase 3 rhythm: two basic attacks (melee or projectile), then the sweep (slash). Counts 1, 2
	 * on the basic attacks and 3 on the sweep, then goes away when the sweep is done.
	 */
	private int sweepCount;
	private int sweepShownUntil = -1;

	/** The current attack: when it started and how long it lasts (null style = no counter). */
	private AttackStyle attackStyle;
	private int attackStartTick;
	private int attackLength;

	private final Map<LocalPoint, TileObject> dangerObjects = new HashMap<>();
	private final Map<LocalPoint, GameObject> artifacts = new HashMap<>();
	private final Set<Projectile> loggedProjectiles = Collections.newSetFromMap(new java.util.WeakHashMap<>());

	@Inject
	private FinalBossOverlay overlay;

	@Inject
	protected FinalBoss(FracturedArchiveConfig config)
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
		attackStyle = null;
		clearUnleash();
		sweepCount = 0;
		sweepShownUntil = -1;
	}

	/** The "safe portal" graphic the game puts where the orange portals' lines cross. */
	static final Set<Integer> ORANGE_PORTAL_GRAPHICS = Set.of(
		net.runelite.api.gameval.SpotanimID.VFX_OCELLUS_P2_SAFE_PORTAL_SPAWN,
		net.runelite.api.gameval.SpotanimID.VFX_OCELLUS_P2_SAFE_PORTAL_IDLE);

	@Override
	public void onNpcSpawned(net.runelite.api.events.NpcSpawned event)
	{
		super.onNpcSpawned(event);
		trackPortal(event.getNpc());
	}

	@Override
	public void onNpcChanged(net.runelite.api.events.NpcChanged event)
	{
		super.onNpcChanged(event);
		portals.remove(event.getNpc());
		trackPortal(event.getNpc());
	}

	@Override
	public void onNpcDespawned(net.runelite.api.events.NpcDespawned event)
	{
		super.onNpcDespawned(event);
		portals.remove(event.getNpc());
	}

	private void trackPortal(NPC npc)
	{
		if (isActive())
		{
			StringBuilder spots = new StringBuilder();
			for (net.runelite.api.ActorSpotAnim a : npc.getSpotAnims())
			{
				spots.append(' ').append(a.getId());
			}
			debug("npc " + npc.getId() + " (" + npc.getName() + ") at " + tile(npc.getWorldLocation())
				+ " anim " + npc.getAnimation() + " spotanims" + spots);
		}
		for (PortalType type : PortalType.values())
		{
			if (npc.getId() == type.npcId)
			{
				portals.add(npc);
				debug("portal " + type + " (" + npc.getId() + ") at " + tile(npc.getWorldLocation()) + " facing " + npc.getOrientation());
			}
		}
	}

	Collection<TileObject> getDangerObjects()
	{
		return Collections.unmodifiableCollection(dangerObjects.values());
	}

	Collection<GameObject> getArtifacts()
	{
		return Collections.unmodifiableCollection(artifacts.values());
	}

	private void clearUnleash()
	{
		unleashing = false;
		unleashWaves = 0;
		lastWaveTick = -1;
		arenaHitTick = -1;
	}

	/** Ticks until Unleash's arena-wide hit, counted from its fourth wave (0 = none). */
	int getUnleashTicks()
	{
		if (arenaHitTick < 0)
		{
			return 0;
		}
		return Math.max(0, arenaHitTick - client.getTickCount());
	}

	/** Basic attacks since the last sweep (1, 2, then 3 on the sweep), or 0 when hidden. */
	int getSweepCount()
	{
		if (sweepShownUntil >= 0 && client.getTickCount() >= sweepShownUntil)
		{
			sweepCount = 0;
			sweepShownUntil = -1;
		}
		return sweepCount;
	}

	/** Last animation seen on each animated piece of scenery, to record only changes. */
	private final Map<Long, Integer> sceneryAnimations = new HashMap<>();

	/**
	 * Records (to the log file) any wall or floor scenery near you that starts a new animation
	 * during the fight. Used to find which portals turn orange in phase 2.
	 */
	private void recordSceneryAnimations()
	{
		if (!isActive() || boss.getId() != NpcID.TFA_OCELLUS_P2)
		{
			sceneryAnimations.clear();
			return;
		}
		net.runelite.api.WorldView wv = client.getTopLevelWorldView();
		net.runelite.api.Player me = client.getLocalPlayer();
		if (wv == null || me == null || me.getLocalLocation() == null)
		{
			return;
		}
		net.runelite.api.Tile[][] tiles = wv.getScene().getTiles()[wv.getPlane()];
		int cx = me.getLocalLocation().getSceneX();
		int cy = me.getLocalLocation().getSceneY();
		for (int x = Math.max(0, cx - 20); x <= Math.min(tiles.length - 1, cx + 20); x++)
		{
			for (int y = Math.max(0, cy - 20); y <= Math.min(tiles[x].length - 1, cy + 20); y++)
			{
				net.runelite.api.Tile tile = tiles[x][y];
				if (tile == null)
				{
					continue;
				}
				for (GameObject o : tile.getGameObjects())
				{
					if (o != null)
					{
						recordAnimation(o, o.getRenderable(), "object");
					}
				}
				net.runelite.api.WallObject wall = tile.getWallObject();
				if (wall != null)
				{
					recordAnimation(wall, wall.getRenderable1(), "wall");
					recordAnimation(wall, wall.getRenderable2(), "wall2");
				}
				net.runelite.api.DecorativeObject deco = tile.getDecorativeObject();
				if (deco != null)
				{
					recordAnimation(deco, deco.getRenderable(), "deco");
				}
			}
		}
	}

	private void recordAnimation(TileObject o, net.runelite.api.Renderable r, String kind)
	{
		if (!(r instanceof net.runelite.api.DynamicObject))
		{
			return;
		}
		net.runelite.api.Animation a = ((net.runelite.api.DynamicObject) r).getAnimation();
		int anim = a == null ? -1 : a.getId();
		long key = o.getHash() * 3 + (kind.equals("wall2") ? 1 : 0);
		Integer before = sceneryAnimations.put(key, anim);
		if (before == null || before != anim)
		{
			debug(kind + " " + o.getId() + " anim " + anim + " at " + tile(o.getWorldLocation()));
		}
	}

	/** Ticks left on the counter (0 = none). */
	int getAttackTicks()
	{
		if (attackStyle == null)
		{
			return 0;
		}
		if (attackStartTick < 0)
		{
			return attackLength;
		}
		return Math.max(0, attackLength - (client.getTickCount() - attackStartTick));
	}

	Color getAttackColor()
	{
		if (attackStyle == AttackStyle.MELEE)
		{
			return config.ocellusMeleeColor();
		}
		if (attackStyle == AttackStyle.PRAYER)
		{
			return config.ocellusPrayerColor();
		}
		return attackStyle == AttackStyle.PROJECTILE ? config.ocellusProjectileColor() : config.ocellusSpecialColor();
	}

	@Override
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!(event.getActor() instanceof NPC) || !bossIds().contains(((NPC) event.getActor()).getId()))
		{
			return;
		}

		int animation = event.getActor().getAnimation();
		debug("anim " + animation);

		if (animation == AnimationID.NPC_OCELLUS_PHASE3_UNLEASH_OUT || animation == AnimationID.NPC_OCELLUS_PHASE3_UNLEASH_OUT_90F)
		{
			// He's landed
			clearUnleash();
		}

		if (animation == AnimationID.NPC_OCELLUS_PHASE3_UNLEASH_IN)
		{
			clearUnleash();
			unleashing = true;
			attackStyle = null;
		}

		if (animation == AnimationID.NPC_OCELLUS_PHASE3_PRAYER_LOOP)
		{
			// He's holding it (later in phase 3): the first hit lands now, and a second hit comes
			// 3 ticks later. Start a fresh countdown to that second hit.
			attackStyle = AttackStyle.PRAYER;
			attackStartTick = -1;
			attackLength = PRAYER_SECOND_HIT;
		}

		switch (animation)
		{
			case AnimationID.NPC_OCELLUS_PHASE3_MELEE_BASIC:
			case AnimationID.NPC_OCELLUS_PHASE3_BASIC_PROJECTILE_ALL:
			case AnimationID.NPC_OCELLUS_PHASE3_BASIC_PROJECTILE_START:
			case AnimationID.NPC_OCELLUS_PHASE3_BASIC_PROJECTILE_NOLOOP:
				sweepCount = sweepCount >= 3 ? 1 : sweepCount + 1;
				sweepShownUntil = -1;
				break;
			case AnimationID.NPC_OCELLUS_PHASE3_MELEE_SLASH:
				// The sweep: show 3 for its 4 ticks, then go away
				sweepCount = 3;
				sweepShownUntil = client.getTickCount() + 4;
				break;
			case AnimationID.NPC_OCELLUS_PHASE3_MELEE_SUPERIOR_SLASH:
			case AnimationID.NPC_OCELLUS_PHASE3_PRAYER_IN:
			case AnimationID.NPC_OCELLUS_PHASE3_UNLEASH_IN:
			case AnimationID.NPC_OCELLUS_QUADRANT_DESTROY_IN:
				// A special breaks the rhythm; start again from the next basic attack
				sweepCount = 0;
				sweepShownUntil = -1;
				break;
			default:
				break;
		}

		AttackStyle style = ATTACKS.get(animation);
		if (style != null)
		{
			// The count starts on the game tick after the animation is seen, so the first number
			// shown is always the full length, whichever point in the tick the animation arrives. Attacks with a confirmed length use
			// it; the rest use DEFAULT_ATTACK_TICKS.
			attackStyle = style;
			// Pinned to the next game tick (see onGameTick)
			attackStartTick = -1;
			attackLength = KNOWN_TICKS.getOrDefault(animation, DEFAULT_ATTACK_TICKS);
		}
	}

	@Override
	public void onGameTick(GameTick event)
	{
		recordSceneryAnimations();

		if (attackStyle != null && attackStartTick < 0)
		{
			attackStartTick = client.getTickCount();
		}
		if (attackStyle != null && getAttackTicks() <= 0)
		{
			attackStyle = null;
		}
		if (arenaHitTick >= 0 && client.getTickCount() > arenaHitTick + 2)
		{
			// Missed his landing; don't leave the counter hanging
			clearUnleash();
		}
	}

	@Override
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		int id = event.getGraphicsObject().getId();
		int tick = client.getTickCount();
		if (unleashing && id == net.runelite.api.gameval.SpotanimID.VFX_OCELLUS_P2_AOE_DODGE && tick != lastWaveTick)
		{
			// Each wave drops many tiles on the same tick: count the tick once
			lastWaveTick = tick;
			if (++unleashWaves == UNLEASH_WAVES)
			{
				arenaHitTick = tick + ARENA_HIT_AFTER_LAST_WAVE;
			}
		}

		if (isActive())
		{
			debug("gfx " + event.getGraphicsObject().getId() + " at " + tile(WorldPoint.fromLocal(client, event.getGraphicsObject().getLocation())));
		}
	}

	@Override
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile p = event.getProjectile();
		if (isActive() && loggedProjectiles.add(p))
		{
			debug("projectile " + p.getId() + " from " + tile(p.getSourcePoint())
				+ (p.getSourceActor() != null ? " (" + p.getSourceActor().getName() + ")" : "")
				+ " to " + (p.getTargetActor() != null ? p.getTargetActor().getName() : tile(p.getTargetPoint()))
				+ ", " + (p.getRemainingCycles() + 29) / 30 + " ticks");
		}
	}

	@Override
	public void onOverheadTextChanged(net.runelite.api.events.OverheadTextChanged event)
	{
		// Record what Ocellus (or anything near him) says, e.g. "insolence" or "untether"
		if (isActive() && event.getActor() != null)
		{
			debug("says (" + event.getActor().getName() + "): " + event.getOverheadText());
		}
	}

	@Override
	public void onChatMessage(net.runelite.api.events.ChatMessage event)
	{
		if (isActive() && (event.getType() == net.runelite.api.ChatMessageType.NPC_SAY
			|| event.getType() == net.runelite.api.ChatMessageType.GAMEMESSAGE))
		{
			debug("chat " + event.getType() + " (" + event.getName() + "): " + event.getMessage());
		}
	}

	@Override
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject o = event.getGameObject();
		added(o);
		if (ACTIVE_ARTIFACTS.contains(o.getId()))
		{
			artifacts.put(o.getLocalLocation(), o);
		}
	}

	@Override
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject o = event.getGameObject();
		removed(o);
		artifacts.remove(o.getLocalLocation(), o);
	}

	@Override
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		added(event.getGroundObject());
	}

	@Override
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		removed(event.getGroundObject());
	}

	private void added(TileObject o)
	{
		if (DANGER_OBJECTS.contains(o.getId()))
		{
			dangerObjects.put(o.getLocalLocation(), o);
		}
		if (isActive())
		{
			debug("object " + o.getId() + " at " + tile(o.getWorldLocation()));
		}
	}

	private void removed(TileObject o)
	{
		dangerObjects.remove(o.getLocalLocation(), o);
	}

	@Override
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			dangerObjects.clear();
			artifacts.clear();
			portals.clear();
		}
	}

	private void debug(String message)
	{
		String line = "[Ocellus t" + client.getTickCount() + "] " + message;
		log.info("{}", line);
	}

	private static String tile(WorldPoint p)
	{
		return p == null ? "?" : p.getX() + "," + p.getY();
	}
}
