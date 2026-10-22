package net.runelite.client.plugins.fracturedarchive.archiveguardian;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

/**
 * Archive Guardian. Always the first encounter.
 */
@Singleton
public class ArchiveGuardian extends Encounter
{
	// Related: TFA_GUARDIAN_BATTERY_INACTIVE/ACTIVE/DESTROYED, ARCHIVE_GUARDIAN_TENTACLE, TFA_GUARDIAN_DEAD
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_GUARDIAN_START, NpcID.TFA_GUARDIAN, NpcID.TFA_GUARDIAN_UNSHIELDED);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	// A mushroom goes SPAWN -> ACTIVE -> DESPAWN. It's marked from SPAWN until DESPAWN starts.
	private static final Set<Integer> DANGEROUS_MUSHROOMS = Set.of(ObjectID.TFA_GUARDIAN_MUSHROOM_SPAWN, ObjectID.TFA_GUARDIAN_MUSHROOM_ACTIVE);

	/**
	 * Ticks counted after each animation; the same timings and counting as your Boss Ticks entry.
	 * In game the special attack plays NPC_ARCHIVE_GUARDIAN_ENRAGED_PHASE01, not the animation
	 * named SPECIAL_ATTACK01, so both are treated as the special.
	 */
	private final Map<Integer, Integer> attackTicks = Map.of(
		AnimationID.NPC_ARCHIVE_GUARDIAN_RANGE_ATTACK01, 5,
		AnimationID.NPC_ARCHIVE_GUARDIAN_MELEE_ATTACK01, 5,
		// The special's length is BEAM_TICKS or MUSHROOM_ATTACK_TICKS (see startCounter)
		AnimationID.NPC_ARCHIVE_GUARDIAN_SPECIAL_ATTACK01, 0,
		// Despite its name, this is the animation the special attack actually plays in game
		AnimationID.NPC_ARCHIVE_GUARDIAN_ENRAGED_PHASE01, 0,
		AnimationID.NPC_ARCHIVE_GUARDIAN_IDLE_TO_GROUND01, 40
	);

	/** Counter colors (the colors you had saved). */
	private static final Color RANGED_COLOR = Color.GREEN;
	private static final Color BEAM_COLOR = new Color(255, 115, 0);
	private static final Color GROUND_COLOR = Color.RED;
	private static final Color MELEE_COLOR = Color.RED;
	private static final Color MUSHROOM_ATTACK_COLOR = Color.MAGENTA;

	private final Map<Integer, Color> attackColors = Map.of(
		AnimationID.NPC_ARCHIVE_GUARDIAN_RANGE_ATTACK01, RANGED_COLOR,
		AnimationID.NPC_ARCHIVE_GUARDIAN_MELEE_ATTACK01, MELEE_COLOR,
		AnimationID.NPC_ARCHIVE_GUARDIAN_SPECIAL_ATTACK01, BEAM_COLOR,
		AnimationID.NPC_ARCHIVE_GUARDIAN_ENRAGED_PHASE01, BEAM_COLOR,
		AnimationID.NPC_ARCHIVE_GUARDIAN_IDLE_TO_GROUND01, GROUND_COLOR
	);

	/**
	 * The beam and the mushroom attack share one animation. If mushrooms spawn within this many
	 * ticks of it, the counter switches to the mushroom attack.
	 */
	private static final int MUSHROOM_ATTACK_WINDOW = 2;

	/** Ticks from the special animation until the beam or the mushroom attack hits. */
	private static final int BEAM_TICKS = 10;
	private static final int MUSHROOM_ATTACK_TICKS = 5;

	/** The Guardian the counter is drawn on, and its ticks remaining (0 = no counter). */
	private NPC counterNpc;
	private int ticksRemaining;
	private int counterAnimation = -1;
	/** Tick the special started, and whether it has turned out to be the mushroom attack. */
	private int specialStartTick = -1;
	private boolean mushroomAttack;

	/** Mushrooms currently up, by tile, so a state change on the same tile replaces the old one. */
	private final Map<WorldPoint, GameObject> mushrooms = new HashMap<>();

	@Inject
	private ArchiveGuardianOverlay overlay;

	@Inject
	protected ArchiveGuardian(FracturedArchiveConfig config)
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
		mushrooms.clear();
		clearCounter();
	}

	NPC getCounterNpc()
	{
		return ticksRemaining > 0 ? counterNpc : null;
	}

	int getTicksRemaining()
	{
		return ticksRemaining;
	}

	Color getCounterColor()
	{
		if (mushroomAttack)
		{
			return MUSHROOM_ATTACK_COLOR;
		}

		Color color = attackColors.get(counterAnimation);
		return color != null ? color : Color.WHITE;
	}

	@Override
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor() instanceof NPC)
		{
			startCounter((NPC) event.getActor());
		}
	}

	@Override
	public void onNpcChanged(NpcChanged event)
	{
		super.onNpcChanged(event);
		// An attack animation can start at the same moment the Guardian changes form
		startCounter(event.getNpc());
	}

	@Override
	public void onGameTick(GameTick event)
	{
		if (ticksRemaining > 0 && --ticksRemaining == 0)
		{
			clearCounter();
		}
	}

	private void startCounter(NPC npc)
	{
		if (!bossIds().contains(npc.getId()))
		{
			return;
		}

		int animation = npc.getAnimation();
		Integer ticks = attackTicks.get(animation);
		if (ticks == null)
		{
			return;
		}

		boolean special = animation == AnimationID.NPC_ARCHIVE_GUARDIAN_SPECIAL_ATTACK01
			|| animation == AnimationID.NPC_ARCHIVE_GUARDIAN_ENRAGED_PHASE01;
		// Starts as the beam; switches to the mushroom attack if mushrooms appear (see onGameObjectSpawned)
		mushroomAttack = false;
		specialStartTick = special ? client.getTickCount() : -1;
		if (special)
		{
			ticks = BEAM_TICKS;
		}

		// +1 and counted down each tick, the same as Boss Ticks, so your timings carry over
		counterNpc = npc;
		counterAnimation = animation;
		ticksRemaining = ticks + 1;
	}

	/** Mushrooms just after the special mean it was the mushroom attack, not the beam. */
	private void checkMushroomAttack()
	{
		if (mushroomAttack || specialStartTick < 0 || ticksRemaining <= 0)
		{
			return;
		}

		int elapsed = client.getTickCount() - specialStartTick;
		if (elapsed <= MUSHROOM_ATTACK_WINDOW)
		{
			mushroomAttack = true;
			// Same +1 counting as the other counters, less the ticks already gone
			ticksRemaining = Math.max(1, MUSHROOM_ATTACK_TICKS + 1 - elapsed);
		}
	}

	private void clearCounter()
	{
		counterNpc = null;
		ticksRemaining = 0;
		counterAnimation = -1;
		specialStartTick = -1;
		mushroomAttack = false;
	}

	Collection<GameObject> getMushrooms()
	{
		return Collections.unmodifiableCollection(mushrooms.values());
	}

	// Mushrooms are tracked whether or not the boss has been picked up yet, since they
	// only appear in this room anyway.

	@Override
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		WorldPoint tile = object.getWorldLocation();
		if (DANGEROUS_MUSHROOMS.contains(object.getId()))
		{
			mushrooms.put(tile, object);
			checkMushroomAttack();
		}
		else if (object.getId() == ObjectID.TFA_GUARDIAN_MUSHROOM_DESPAWN)
		{
			mushrooms.remove(tile);
		}
	}

	@Override
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject object = event.getGameObject();
		// Only remove it if it's the one we're tracking, not an older state being replaced
		mushrooms.remove(object.getWorldLocation(), object);
	}

	@Override
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			mushrooms.clear();
		}
	}
}
