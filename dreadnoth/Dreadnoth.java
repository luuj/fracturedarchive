package net.runelite.client.plugins.fracturedarchive.dreadnoth;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

/**
 * Dreadnoth. Third encounter, paired with the Necromancer. Dreadnoth the Jellied.
 */
@Singleton
public class Dreadnoth extends Encounter
{
	// Dreadnoth the Jellied is TFA_JELLY_BOSS in the game data. Related: TFA_JELLY_MINION
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_JELLY_BOSS);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	/** The blood splatter left on the ground. */
	private static final int SPLATTER_ID = ObjectID.TFA_JELLY_BOSS_DAMAGE_TILE;
	/** Dreadnoth's ranged blobs, which leave the splatter where they land. */
	private static final Set<Integer> BLOB_PROJECTILES = Set.of(
		SpotanimID.VFX_DREADNOTH_RANGED_PROJ01,
		SpotanimID.VFX_DREADNOTH_RANGED_PROJ02);

	/** Splatter on the ground, by the tile it's on. */
	private final Map<LocalPoint, TileObject> splatter = new HashMap<>();
	/** Blobs in the air and where each will land. */
	private final Map<Projectile, LocalPoint> incoming = new HashMap<>();

	private static final Set<Integer> JUMP_ANIMATIONS = Set.of(
		AnimationID.DREADNOTH_JUMP01,
		AnimationID.DREADNOTH_JUMP02);

	/** Ticks until Dreadnoth lands from a jump (0 = not jumping). */
	@Getter
	private int jumpTicks;

	@Inject
	private DreadnothOverlay overlay;

	@Inject
	protected Dreadnoth(FracturedArchiveConfig config)
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
		splatter.clear();
		incoming.clear();
		jumpTicks = 0;
	}

	@Override
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		// Only the start of a jump, so the second jump animation doesn't restart it mid-air
		if (bossIds().contains(npc.getId()) && JUMP_ANIMATIONS.contains(npc.getAnimation()) && jumpTicks == 0)
		{
			boss = npc;
			// +1 so the first number shown is the full count
			jumpTicks = config.dreadnothJumpTicks() + 1;
		}
	}

	Collection<TileObject> getSplatter()
	{
		return Collections.unmodifiableCollection(splatter.values());
	}

	Collection<LocalPoint> getIncoming()
	{
		return Collections.unmodifiableCollection(incoming.values());
	}

	// Splatter can be either kind of object, so watch both.

	@Override
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		added(event.getGameObject());
	}

	@Override
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		removed(event.getGameObject());
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

	private void added(TileObject object)
	{
		if (object.getId() == SPLATTER_ID)
		{
			splatter.put(object.getLocalLocation(), object);
		}
	}

	private void removed(TileObject object)
	{
		if (object.getId() == SPLATTER_ID)
		{
			splatter.remove(object.getLocalLocation(), object);
		}
	}

	@Override
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		if (BLOB_PROJECTILES.contains(projectile.getId()))
		{
			incoming.put(projectile, event.getPosition());
		}
	}

	@Override
	public void onGameTick(GameTick event)
	{
		if (jumpTicks > 0)
		{
			jumpTicks--;
		}

		// Forget blobs once they've landed
		incoming.keySet().removeIf(p -> p.getRemainingCycles() <= 0);
	}

	@Override
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			splatter.clear();
			incoming.clear();
		}
	}
}
