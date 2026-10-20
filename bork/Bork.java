package net.runelite.client.plugins.fracturedarchive.bork;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
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
 * Bork. Second encounter, paired with Ezekiel. Bork fights with the Smorks.
 */
@Singleton
public class Bork extends Encounter
{
	// Related: TFA_SMORK
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_BORK);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	private static final int SMORK_SHOT = AnimationID.NPC_SMORK_CANNON_ATTACK01;
	private static final int SMORK_BOMB = AnimationID.NPC_SMORK_BIG_CANNON_ATTACK01;

	// Bork's stun animations, from the cannon. Any of them starts the counter.
	private static final Set<Integer> STUN_ANIMATIONS = Set.of(
		AnimationID.BORK_STUNNED,
		AnimationID.NPC_BORK_STUN01,
		AnimationID.NPC_BORK_12_CYCLE_STUN01,
		AnimationID.NPC_BORK_14_CYCLE_STUN01);

	// Bork attacking again means the stun is over, even if the counter hasn't run out
	private static final Set<Integer> ATTACK_ANIMATIONS = Set.of(
		AnimationID.NPC_BORK_GONG_MELEE01,
		AnimationID.NPC_BORK_ENRAGED_GONG_ATTACK_IN01,
		AnimationID.NPC_BORK_ENRAGED_GONG_ATTACK01,
		AnimationID.NPC_BORK_ENRAGED_STOMP_ATTACK_IN01,
		AnimationID.NPC_BORK_ENRAGED_STOMP_ATTACK01,
		AnimationID.NPC_BORK_IDLE_TO_ENRAGED_STOMP_ATTACK_IN01,
		AnimationID.NPC_BORK_IDLE_TO_ENRAGED_GONG_ATTACK_IN01);

	// The belly drum
	private static final int BELLY_DRUM = AnimationID.NPC_BORK_GONG_MELEE01;

	/** Bork after a belly drum, and the ticks until the next one (0 = no counter). */
	private NPC attackingBork;
	private int attackTicks;

	/** Bork while stunned, and the ticks left (0 = not stunned). */
	private NPC stunnedBork;
	private int stunTicks;

	// Smork projectiles and the fire left on the ground
	private static final int SMORK_MAGIC_SHOT = SpotanimID.VFX_SMORK_MAGE_ATTACK_PROJANIM01;
	private static final int SMORK_RANGED_SHOT = SpotanimID.VFX_SMORK_RANGE_ATTACK_PROJANIM01;
	private static final int SMORK_FIRE_BOMB = SpotanimID.VFX_SMORK_NAPALM_FIRE_PROJANIM01;
	private static final int SMORK_FIRE = SpotanimID.VFX_SMORK_NAPALM_FIRE_SPOTANIM01;

	enum ShotStyle
	{
		MAGIC,
		RANGED
	}

	/** The style of each Smork's last shot. */
	private final Map<NPC, ShotStyle> smorkStyles = new HashMap<>();

	/** Fire bombs in the air. */
	private final Set<Projectile> fireBombs = new HashSet<>();

	/** Fire burning on the ground. */
	private final Set<GraphicsObject> fires = new HashSet<>();

	/** Normal shots each Smork has fired since its last bomb. */
	private final Map<NPC, Integer> smorkShots = new HashMap<>();

	@Inject
	private BorkOverlay overlay;

	@Inject
	protected Bork(FracturedArchiveConfig config)
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
		smorkShots.clear();
		smorkStyles.clear();
		fireBombs.clear();
		fires.clear();
		clearStun();
		clearAttack();
	}

	NPC getAttackingBork()
	{
		return attackTicks > 0 ? attackingBork : null;
	}

	int getAttackTicks()
	{
		return attackTicks;
	}

	NPC getStunnedBork()
	{
		return stunTicks > 0 ? stunnedBork : null;
	}

	int getStunTicks()
	{
		return stunTicks;
	}

	ShotStyle getSmorkStyle(NPC smork)
	{
		return smorkStyles.get(smork);
	}

	Set<Projectile> getFireBombs()
	{
		return Collections.unmodifiableSet(fireBombs);
	}

	Set<GraphicsObject> getFires()
	{
		return Collections.unmodifiableSet(fires);
	}

	Map<NPC, Integer> getSmorkShots()
	{
		return Collections.unmodifiableMap(smorkShots);
	}

	// Smorks are tracked whether or not Bork has been picked up yet.

	@Override
	public void onNpcSpawned(NpcSpawned event)
	{
		super.onNpcSpawned(event);
		if (event.getNpc().getId() == NpcID.TFA_SMORK)
		{
			smorkShots.put(event.getNpc(), 0);
		}
	}

	@Override
	public void onNpcDespawned(NpcDespawned event)
	{
		super.onNpcDespawned(event);
		smorkShots.remove(event.getNpc());
		smorkStyles.remove(event.getNpc());
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

		if (npc.getId() == NpcID.TFA_BORK)
		{
			if (STUN_ANIMATIONS.contains(animation))
			{
				// The stun can play several animations in a row; only the first starts the counter
				if (stunTicks == 0)
				{
					stunnedBork = npc;
					stunTicks = config.borkStunTicks();
				}
			}
			else if (ATTACK_ANIMATIONS.contains(animation))
			{
				clearStun();
			}

			if (animation == BELLY_DRUM)
			{
				attackingBork = npc;
				attackTicks = config.borkAttackTicks();
			}
			else if (STUN_ANIMATIONS.contains(animation))
			{
				// No attacks while stunned
				clearAttack();
			}
			return;
		}

		if (npc.getId() != NpcID.TFA_SMORK)
		{
			return;
		}

		if (animation == SMORK_SHOT)
		{
			smorkShots.merge(npc, 1, Integer::sum);
		}
		else if (animation == SMORK_BOMB)
		{
			smorkShots.put(npc, 0);
		}
	}

	@Override
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		int id = projectile.getId();

		if (id == SMORK_FIRE_BOMB)
		{
			fireBombs.add(projectile);
		}
		else if ((id == SMORK_MAGIC_SHOT || id == SMORK_RANGED_SHOT) && projectile.getSourceActor() instanceof NPC)
		{
			NPC source = (NPC) projectile.getSourceActor();
			if (source.getId() == NpcID.TFA_SMORK)
			{
				smorkStyles.put(source, id == SMORK_MAGIC_SHOT ? ShotStyle.MAGIC : ShotStyle.RANGED);
			}
		}
	}

	@Override
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		if (event.getGraphicsObject().getId() == SMORK_FIRE)
		{
			fires.add(event.getGraphicsObject());
		}
	}

	@Override
	public void onGameTick(GameTick event)
	{
		// Bombs that have landed, and fire that has burnt out
		fireBombs.removeIf(p -> p.getRemainingCycles() <= 0);
		fires.removeIf(GraphicsObject::finished);

		if (stunTicks > 0 && --stunTicks == 0)
		{
			clearStun();
		}

		if (attackTicks > 0 && --attackTicks == 0)
		{
			clearAttack();
		}
	}

	private void clearAttack()
	{
		attackingBork = null;
		attackTicks = 0;
	}

	private void clearStun()
	{
		stunnedBork = null;
		stunTicks = 0;
	}

	@Override
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			smorkShots.clear();
			smorkStyles.clear();
			fireBombs.clear();
			fires.clear();
		}
	}
}
