package net.runelite.client.plugins.fracturedarchive.necromancer;

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
 * Necromancer. Third encounter, paired with Dreadnoth. Name not yet revealed.
 */
@Singleton
public class Necromancer extends Encounter
{
	// The necromancer is Moia (TFA_MOIA) in the game data. Related: TFA_MOIA_MINION
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_MOIA);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	@Inject
	private NecromancerOverlay overlay;

	/** Not mapped yet: log the whole fight (every NPC of it) to work its attacks out later. */
	private static final Set<Integer> LOG_IDS = Set.of(NpcID.TFA_MOIA, NpcID.TFA_MOIA_MINION);
	private FightLogger logger;

	@Inject
	protected Necromancer(FracturedArchiveConfig config)
	{
		super(config);
	}

	@Override
	public void init()
	{
		logger = new FightLogger(client, "Necromancer", LOG_IDS);
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
