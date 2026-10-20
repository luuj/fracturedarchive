package net.runelite.client.plugins.fracturedarchive.finalboss;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
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
		MELEE, PROJECTILE, SPECIAL
	}

	/** Attacks that start the counter, by style. Timings aren't confirmed yet. */
	private static final Map<Integer, AttackStyle> ATTACKS = Map.ofEntries(
		// Phase 1
		Map.entry(AnimationID.NPC_OCELLUS_PHASE1_BASIC_PROJECTILE_START, AttackStyle.PROJECTILE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE1_RAILGUN, AttackStyle.SPECIAL),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE1_CHARGE, AttackStyle.SPECIAL),
		// Phase 3
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_BASIC, AttackStyle.MELEE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_BASIC_SILENT, AttackStyle.MELEE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_HYBRID, AttackStyle.MELEE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_PRAYER, AttackStyle.MELEE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_SLASH, AttackStyle.MELEE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_SLASH_SILENT, AttackStyle.MELEE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_MELEE_SUPERIOR_SLASH, AttackStyle.SPECIAL),
		// Named "walk" in the game data, but it starts a 10-tick count in game
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_WALK, AttackStyle.SPECIAL),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_BASIC_PROJECTILE_START, AttackStyle.PROJECTILE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_BASIC_PROJECTILE_ALL, AttackStyle.PROJECTILE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_BASIC_PROJECTILE_NOLOOP, AttackStyle.PROJECTILE),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_SOUL_FRAGMENT_START, AttackStyle.SPECIAL),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_UNLEASH_IN, AttackStyle.SPECIAL),
		Map.entry(AnimationID.NPC_OCELLUS_QUADRANT_DESTROY_IN, AttackStyle.SPECIAL),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_DEFLECT_IN, AttackStyle.SPECIAL),
		Map.entry(AnimationID.NPC_OCELLUS_PHASE3_DEFLECT_IN_SILENT, AttackStyle.SPECIAL));

	/** Attacks whose length has been confirmed in game, in ticks. */
	private static final Map<Integer, Integer> KNOWN_TICKS = Map.of(
		AnimationID.NPC_OCELLUS_PHASE3_MELEE_BASIC, 3,
		AnimationID.NPC_OCELLUS_PHASE3_MELEE_SLASH, 4,
		AnimationID.NPC_OCELLUS_PHASE3_WALK, 10);

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

	@Getter
	private int attackTicks;
	private AttackStyle attackStyle;

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
		attackTicks = 0;
		attackStyle = null;
	}

	/** Phase 2's orange ("safe") portal graphics. */
	static final Set<Integer> ORANGE_PORTAL_GRAPHICS = Set.of(
		net.runelite.api.gameval.SpotanimID.VFX_OCELLUS_P2_SAFE_PORTAL_SPAWN,
		net.runelite.api.gameval.SpotanimID.VFX_OCELLUS_P2_SAFE_PORTAL_IDLE);

	/**
	 * Where the orange portals are. They can show up as a graphic on the floor, a graphic on an
	 * NPC, or (phase 3) as the portal NPC type picked in the settings.
	 */
	java.util.List<LocalPoint> getOrangePortalPoints()
	{
		java.util.List<LocalPoint> out = new java.util.ArrayList<>();
		net.runelite.api.WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return out;
		}
		for (net.runelite.api.GraphicsObject g : wv.getGraphicsObjects())
		{
			if (ORANGE_PORTAL_GRAPHICS.contains(g.getId()))
			{
				out.add(g.getLocation());
			}
		}
		for (NPC npc : wv.npcs())
		{
			boolean orange = npc.getId() == config.ocellusOrangePortal().npcId;
			for (net.runelite.api.ActorSpotAnim a : npc.getSpotAnims())
			{
				orange |= ORANGE_PORTAL_GRAPHICS.contains(a.getId());
			}
			if (orange && !npc.isDead() && npc.getLocalLocation() != null)
			{
				out.add(npc.getLocalLocation());
			}
		}
		return out;
	}

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

	Color getAttackColor()
	{
		if (attackStyle == AttackStyle.MELEE)
		{
			return config.ocellusMeleeColor();
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

		AttackStyle style = ATTACKS.get(animation);
		if (style != null)
		{
			attackStyle = style;
			// +1 so the first number shown is the full count. Attacks with a confirmed length use
			// it; the rest use the "Attack speed" setting.
			attackTicks = KNOWN_TICKS.getOrDefault(animation, config.ocellusAttackSpeed()) + 1;
		}
	}

	@Override
	public void onGameTick(GameTick event)
	{
		if (attackTicks > 0 && --attackTicks == 0)
		{
			attackStyle = null;
		}
	}

	@Override
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
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
			debug("projectile " + p.getId() + " to " + (p.getTargetActor() != null ? p.getTargetActor().getName() : tile(p.getTargetPoint()))
				+ ", " + (p.getRemainingCycles() + 29) / 30 + " ticks");
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
		if (config.ocellusDebug())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", line, null);
		}
	}

	private static String tile(WorldPoint p)
	{
		return p == null ? "?" : p.getX() + "," + p.getY();
	}
}
