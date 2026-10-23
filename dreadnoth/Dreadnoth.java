package net.runelite.client.plugins.fracturedarchive.dreadnoth;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.OverheadTextChanged;
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
	/** Dreadnoth's blood (thrown at the ground, leaves the splatter) and his ranged attack. */
	private static final Set<Integer> BLOB_PROJECTILES = Set.of(
		SpotanimID.VFX_DREADNOTH_RANGED_PROJ01,
		SpotanimID.VFX_DREADNOTH_RANGED_PROJ02);

	/** Splatter on the ground, by the tile it's on. */
	private final Map<LocalPoint, TileObject> splatter = new HashMap<>();
	/** Blood in the air and where each will land. */
	private final Map<Projectile, LocalPoint> incoming = new HashMap<>();
	/** His ranged attacks in the air (aimed at a player, so no floor marking). */
	private final Set<Projectile> rangedShots = new HashSet<>();

	private static final Set<Integer> JUMP_ANIMATIONS = Set.of(
		AnimationID.DREADNOTH_JUMP01,
		AnimationID.DREADNOTH_JUMP02);

	/** Jump length: 6 ticks at the start, 4 once he's said "glub glub". */
	private static final int JUMP_TICKS = 6;
	private static final int FAST_JUMP_TICKS = 4;
	private boolean glubbed;
	/** The jump in progress is a fast one (it gets a cooldown after). */
	private boolean fastJump;
	/** After a fast jump lands: the cooldown before he can jump again (0 = none). */
	private static final int JUMP_COOLDOWN_TICKS = 4;
	@lombok.Getter
	private int cooldownTicks;

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
		rangedShots.clear();
		jumpTicks = 0;
		glubbed = false;
		fastJump = false;
		cooldownTicks = 0;
	}

	@Override
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		// The start of a jump. Mid-air, the second jump animation doesn't restart it; but back to
		// back jumps start on the tick the last one lands (when 1 is still showing), so those do.
		if (bossIds().contains(npc.getId()) && JUMP_ANIMATIONS.contains(npc.getAnimation()) && jumpTicks <= 1)
		{
			boss = npc;
			// +1 so the first number shown is the full count
			jumpTicks = (glubbed ? FAST_JUMP_TICKS : JUMP_TICKS) + 1;
			fastJump = glubbed;
			cooldownTicks = 0;
		}
	}

	@Override
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (event.getActor() instanceof NPC && bossIds().contains(((NPC) event.getActor()).getId()))
		{
			checkGlub(event.getOverheadText());
		}
	}

	@Override
	public void onChatMessage(ChatMessage event)
	{
		// The shout can also come through as NPC chat
		if (event.getType() == ChatMessageType.NPC_SAY || event.getType() == ChatMessageType.GAMEMESSAGE)
		{
			String name = event.getName() == null ? "" : event.getName().toLowerCase();
			if (name.isEmpty() || name.contains("dreadnoth"))
			{
				checkGlub(event.getMessage());
			}
		}
	}

	private void checkGlub(String text)
	{
		if (text != null && text.toLowerCase().contains("glub glub"))
		{
			glubbed = true;
		}
	}

	Collection<TileObject> getSplatter()
	{
		return Collections.unmodifiableCollection(splatter.values());
	}

	Set<Projectile> getRangedShots()
	{
		return Collections.unmodifiableSet(rangedShots);
	}

	/** Blood in the air, each with where it will land. */
	Map<Projectile, LocalPoint> getIncoming()
	{
		return Collections.unmodifiableMap(incoming);
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
		if (!BLOB_PROJECTILES.contains(projectile.getId()))
		{
			return;
		}

		// The blood is thrown at the ground; his ranged attack is aimed at a player
		if (projectile.getTargetActor() != null)
		{
			rangedShots.add(projectile);
		}
		else
		{
			incoming.put(projectile, event.getPosition());
		}
	}

	@Override
	public void onGameTick(GameTick event)
	{
		if (cooldownTicks > 0)
		{
			cooldownTicks--;
		}
		if (jumpTicks > 0 && --jumpTicks == 0 && fastJump)
		{
			// In the fast phase his jump has a 4 tick cooldown after he lands. Set during the game
			// tick, so it's shown as is (no +1).
			cooldownTicks = JUMP_COOLDOWN_TICKS;
		}

		// Below 25% hp he jumps fast too, as if he'd said "glub glub"
		if (!glubbed && boss != null && boss.getHealthScale() > 0 && boss.getHealthRatio() >= 0
			&& boss.getHealthRatio() * 4 < boss.getHealthScale())
		{
			glubbed = true;
		}

		// Forget blobs once they've landed
		incoming.keySet().removeIf(p -> p.getRemainingCycles() <= 0);
		rangedShots.removeIf(p -> p.getRemainingCycles() <= 0);
	}

	@Override
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			splatter.clear();
			incoming.clear();
		rangedShots.clear();
		}
	}
}
