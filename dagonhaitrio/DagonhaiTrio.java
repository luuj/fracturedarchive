package net.runelite.client.plugins.fracturedarchive.dagonhaitrio;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.fracturedarchive.FightLogger;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

/**
 * Dagon'hai Trio. Fourth encounter, paired with Xarth.
 */
@Singleton
public class DagonhaiTrio extends Encounter
{
	// Tracks the leader. Related: TFA_DAGONHAI_MINION_1/2 (+ _UNSHIELDED), TFA_DAGONHAI_FOCUS, TFA_DAGONHAI_DEMON, TFA_DAGONHAI_RIFT
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_DAGONHAI_LEADER, NpcID.TFA_DAGONHAI_LEADER_UNSHIELDED);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	@Inject
	private DagonhaiTrioOverlay overlay;

	/** Not mapped yet: log the whole fight (every NPC of it) to work its attacks out later. */
	private static final Set<Integer> LOG_IDS = Set.of(NpcID.TFA_DAGONHAI_LEADER, NpcID.TFA_DAGONHAI_LEADER_UNSHIELDED,
		NpcID.TFA_DAGONHAI_MINION_1, NpcID.TFA_DAGONHAI_MINION_1_UNSHIELDED, NpcID.TFA_DAGONHAI_MINION_2,
		NpcID.TFA_DAGONHAI_MINION_2_UNSHIELDED, NpcID.TFA_DAGONHAI_FOCUS, NpcID.TFA_DAGONHAI_DEMON, NpcID.TFA_DAGONHAI_RIFT);
	private FightLogger logger;

	@Inject
	protected DagonhaiTrio(FracturedArchiveConfig config)
	{
		super(config);
	}

	@Override
	public void init()
	{
		logger = new FightLogger(client, "Dagonhai", LOG_IDS);
	}

	/** The three wizards (the elder and the two monks, shielded or not). */
	static final Set<Integer> WIZARD_IDS = Set.of(NpcID.TFA_DAGONHAI_LEADER, NpcID.TFA_DAGONHAI_LEADER_UNSHIELDED,
		NpcID.TFA_DAGONHAI_MINION_1, NpcID.TFA_DAGONHAI_MINION_1_UNSHIELDED, NpcID.TFA_DAGONHAI_MINION_2,
		NpcID.TFA_DAGONHAI_MINION_2_UNSHIELDED);

	enum Element
	{
		FIRE, WATER, AIR, EARTH
	}

	enum God
	{
		ZAMORAK, BANDOS, SARADOMIN
	}

	/**
	 * What each wizard is doing and since when. They're told by what they yell; the highlight goes
	 * when the wizard is stunned ("Argh!"), the tome is blocked ("Curse you!"), it says something
	 * else, or when it would have finished: a channel completes 18 ticks after the yell, and a
	 * tome is blocked 4 ticks after it (8 allowed, for one that isn't blocked).
	 */
	private static final int CHANNEL_TICKS = 18;
	private static final int INCANTATION_TICKS = 8;
	/** "What have we here?": an incantation's coming (the god is called 3 ticks later). */
	private static final int WARNING_TICKS = 5;
	private final java.util.Map<net.runelite.api.NPC, Integer> warnings = new java.util.HashMap<>();

	java.util.Set<net.runelite.api.NPC> getIncantationWarnings()
	{
		int tick = client.getTickCount();
		warnings.entrySet().removeIf(e -> tick - e.getValue() >= WARNING_TICKS || e.getKey().isDead());
		return warnings.keySet();
	}
	private final java.util.Map<net.runelite.api.NPC, Element> channels = new java.util.HashMap<>();
	private final java.util.Map<net.runelite.api.NPC, God> incantations = new java.util.HashMap<>();
	private final java.util.Map<net.runelite.api.NPC, Integer> startTicks = new java.util.HashMap<>();

	java.util.Map<net.runelite.api.NPC, Element> getChannels()
	{
		expire();
		return channels;
	}

	java.util.Map<net.runelite.api.NPC, God> getIncantations()
	{
		expire();
		return incantations;
	}

	private void expire()
	{
		int tick = client.getTickCount();
		startTicks.entrySet().removeIf(e ->
		{
			int limit = incantations.containsKey(e.getKey()) ? INCANTATION_TICKS : CHANNEL_TICKS;
			if (tick - e.getValue() >= limit || e.getKey().isDead())
			{
				channels.remove(e.getKey());
				incantations.remove(e.getKey());
				return true;
			}
			return false;
		});
	}

	/** The imp portals in the corners. */
	java.util.List<net.runelite.api.NPC> getPortals()
	{
		java.util.List<net.runelite.api.NPC> portals = new java.util.ArrayList<>();
		net.runelite.api.WorldView wv = client.getTopLevelWorldView();
		if (wv != null)
		{
			for (net.runelite.api.NPC npc : wv.npcs())
			{
				if (npc.getId() == NpcID.TFA_DAGONHAI_RIFT && !npc.isDead())
				{
					portals.add(npc);
				}
			}
		}
		return portals;
	}

	/**
	 * The elder's attacks: ranged (14593) or magic (14597) every 5 ticks, hitting 5 ticks after the
	 * animation (thrown 2 ticks in, 3 ticks to land).
	 */
	private static final int ELDER_ATTACK_TICKS = 5;
	private int elderAttackTick = -1;
	private boolean elderMagic;

	/** Ticks until the elder's attack lands (0 = none). */
	int getElderTicks()
	{
		if (elderAttackTick < 0)
		{
			return 0;
		}
		return Math.max(0, ELDER_ATTACK_TICKS - (client.getTickCount() - elderAttackTick));
	}

	boolean isElderMagic()
	{
		return elderMagic;
	}

	/** In the fight: the elder's up, or the fight logger still sees the room's NPCs. */
	boolean isFightActive()
	{
		return isActive() || logger.isActive();
	}

	private void handleYell(net.runelite.api.NPC npc, String text)
	{
		Element element = null;
		God god = null;
		String t = text.toLowerCase();
		if (t.contains("feed the flames"))
		{
			element = Element.FIRE;
		}
		else if (t.contains("guide the flow"))
		{
			element = Element.WATER;
		}
		else if (t.contains("focus the winds"))
		{
			element = Element.AIR;
		}
		else if (t.contains("strengthen the bond"))
		{
			element = Element.EARTH;
		}
		else if (t.contains("zamorak fuels") || t.contains("zamorak empowers"))
		{
			god = God.ZAMORAK;
		}
		else if (t.contains("bandosian strength"))
		{
			god = God.BANDOS;
		}
		else if (t.contains("saradominist zeal"))
		{
			god = God.SARADOMIN;
		}

		if (element != null)
		{
			incantations.remove(npc);
			channels.put(npc, element);
			startTicks.put(npc, client.getTickCount());
		}
		else if (god != null)
		{
			warnings.remove(npc);
			channels.remove(npc);
			incantations.put(npc, god);
			startTicks.put(npc, client.getTickCount());
		}
		else if (t.contains("what have we here"))
		{
			warnings.put(npc, client.getTickCount());
		}
		else if (t.contains("argh") || t.contains("curse you"))
		{
			// Stunned by the matching element, or the tome was blocked
			channels.remove(npc);
			incantations.remove(npc);
			startTicks.remove(npc);
		}
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
	}

	@Override
	public void unload()
	{
		super.unload();
		logger.reset();
		channels.clear();
		incantations.clear();
		startTicks.clear();
		warnings.clear();
	}

	@Override
	public void onGameTick(GameTick event)
	{
		logger.onGameTick();
	}

	@Override
	public void onNpcSpawned(NpcSpawned event)
	{
		super.onNpcSpawned(event);
		logger.onNpcSpawned(event);
	}

	@Override
	public void onNpcDespawned(NpcDespawned event)
	{
		super.onNpcDespawned(event);
		logger.onNpcDespawned(event);
	}

	@Override
	public void onNpcChanged(NpcChanged event)
	{
		super.onNpcChanged(event);
		logger.onNpcChanged(event);
	}

	@Override
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor() instanceof net.runelite.api.NPC
			&& ((net.runelite.api.NPC) event.getActor()).getId() == NpcID.TFA_DAGONHAI_LEADER)
		{
			int anim = event.getActor().getAnimation();
			if (anim == net.runelite.api.gameval.AnimationID.NPC_DAGONHAI_ATTACK_RANGED_01
				|| anim == net.runelite.api.gameval.AnimationID.NPC_DAGONHAI_ATTACK_MAGIC_01)
			{
				elderAttackTick = client.getTickCount();
				elderMagic = anim == net.runelite.api.gameval.AnimationID.NPC_DAGONHAI_ATTACK_MAGIC_01;
			}
		}
		logger.onAnimationChanged(event);
	}

	@Override
	public void onProjectileMoved(ProjectileMoved event)
	{
		logger.onProjectileMoved(event);
	}

	@Override
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		logger.onGraphicsObjectCreated(event);
	}

	@Override
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		logger.onGameObjectSpawned(event);
	}

	@Override
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		logger.onGameObjectDespawned(event);
	}

	@Override
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		logger.onGroundObjectSpawned(event);
	}

	@Override
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		logger.onGroundObjectDespawned(event);
	}

	@Override
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (event.getActor() instanceof net.runelite.api.NPC
			&& WIZARD_IDS.contains(((net.runelite.api.NPC) event.getActor()).getId()) && event.getOverheadText() != null)
		{
			handleYell((net.runelite.api.NPC) event.getActor(), event.getOverheadText());
		}
		logger.onOverheadTextChanged(event);
	}

	@Override
	public void onChatMessage(ChatMessage event)
	{
		logger.onChatMessage(event);
	}

	@Override
	public void onVarbitChanged(VarbitChanged event)
	{
		logger.onVarbitChanged(event);
	}

	@Override
	public void onHitsplatApplied(net.runelite.api.events.HitsplatApplied event)
	{
		logger.onHitsplatApplied(event);
	}
}
