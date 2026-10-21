package net.runelite.client.plugins.fracturedarchive.bork;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;
import net.runelite.client.util.Text;

/**
 * Bork. Second encounter, paired with Ezekiel. Bork fights with the Smorks.
 */
@Slf4j
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

	/** The player Bork last called out in chat, or null. */
	private Player calledTarget;

	/** Bork while stunned, and the ticks left (0 = not stunned). */
	private NPC stunnedBork;
	private int stunTicks;

	/** How long the cannon stuns Bork. */
	private static final int STUN_TICKS = 16;

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

	/**
	 * The player each Smork has picked for its next fire bomb. Kept until that Smork fires the
	 * bomb, even if it stops attacking them in between.
	 */
	private final Map<NPC, Player> smorkTargets = new HashMap<>();

	// Fight logging, to work out the stomp after the stun
	private final Set<Projectile> loggedProjectiles = Collections.newSetFromMap(new java.util.WeakHashMap<>());
	private final Set<Long> loggedSpotAnims = new HashSet<>();
	private WorldPoint lastBorkTile;

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
		smorkTargets.clear();
		fires.clear();
		clearStun();
		calledTarget = null;
		lastBorkTile = null;
		loggedSpotAnims.clear();
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
		smorkTargets.remove(event.getNpc());
	}

	/** Each Smork's picked target, until its fire bomb comes out. */
	public Map<NPC, Player> getSmorkTargets()
	{
		return Collections.unmodifiableMap(smorkTargets);
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
			debug("bork anim " + animation + " at " + tile(npc) + " facing " + npc.getOrientation()
				+ " interacting " + name(npc.getInteracting()) + (stunTicks > 0 ? " (stun " + stunTicks + " left)" : ""));
			if (STUN_ANIMATIONS.contains(animation) || ATTACK_ANIMATIONS.contains(animation))
			{
				logPlayers();
			}
			if (STUN_ANIMATIONS.contains(animation))
			{
				// The stun can play several animations in a row; only the first starts the counter
				if (stunTicks == 0)
				{
					stunnedBork = npc;
					stunTicks = STUN_TICKS;
				}
			}
			else if (ATTACK_ANIMATIONS.contains(animation))
			{
				clearStun();
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
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		Actor actor = event.getActor();
		if (actor instanceof NPC && ((NPC) actor).getId() == NpcID.TFA_BORK)
		{
			debug("bork says \"" + event.getOverheadText() + "\"");
			checkCalledTarget(event.getOverheadText());
		}
	}

	@Override
	public void onChatMessage(ChatMessage event)
	{
		// Bork's call-out can also come through as NPC chat
		if (event.getType() == ChatMessageType.NPC_SAY || event.getType() == ChatMessageType.GAMEMESSAGE)
		{
			String name = event.getName();
			// Bork himself, not a Smork
			String lower = name == null ? "" : Text.removeTags(name).toLowerCase();
			if (lower.contains("bork") && !lower.contains("smork"))
			{
				checkCalledTarget(event.getMessage());
			}
		}
	}

	/** Bork names who he's going for; pick out whichever player in the area he named. */
	private void checkCalledTarget(String text)
	{
		WorldView wv = client.getTopLevelWorldView();
		if (text == null || wv == null)
		{
			return;
		}

		String said = Text.standardize(Text.removeTags(text));
		Player best = null;
		for (Player player : wv.players())
		{
			String name = player.getName();
			if (name == null)
			{
				continue;
			}
			String standardized = Text.standardize(name);
			// Longest match wins, so "Bob" doesn't beat "Bob the Great"
			if (!standardized.isEmpty() && said.contains(standardized)
				&& (best == null || standardized.length() > Text.standardize(best.getName()).length()))
			{
				best = player;
			}
		}
		if (best != null)
		{
			calledTarget = best;
		}
	}

	@Override
	public Player getTarget()
	{
		if (calledTarget != null && isActive())
		{
			return calledTarget;
		}
		return super.getTarget();
	}

	@Override
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		int id = projectile.getId();
		if (isActive() && loggedProjectiles.add(projectile) && id != SMORK_MAGIC_SHOT && id != SMORK_RANGED_SHOT)
		{
			debug("projectile " + id + " from " + name(projectile.getSourceActor()) + " to "
				+ (projectile.getTargetActor() != null ? name(projectile.getTargetActor())
				: projectile.getTargetPoint() != null ? projectile.getTargetPoint().getX() + "," + projectile.getTargetPoint().getY() : "?")
				+ ", " + (projectile.getRemainingCycles() + 29) / 30 + " ticks");
		}

		if (id == SMORK_FIRE_BOMB)
		{
			// The bomb is out: that Smork's pick is done. Only on the bomb's first move, so the
			// Smork's next pick isn't cleared while this bomb is still in the air.
			if (fireBombs.add(projectile) && projectile.getSourceActor() instanceof NPC)
			{
				smorkTargets.remove((NPC) projectile.getSourceActor());
			}
		}
		else if ((id == SMORK_MAGIC_SHOT || id == SMORK_RANGED_SHOT) && projectile.getSourceActor() instanceof NPC)
		{
			NPC source = (NPC) projectile.getSourceActor();
			if (source.getId() == NpcID.TFA_SMORK)
			{
				smorkStyles.put(source, id == SMORK_MAGIC_SHOT ? ShotStyle.MAGIC : ShotStyle.RANGED);
				// The player it's shooting at, if it hasn't picked one yet
				if (projectile.getTargetActor() instanceof Player)
				{
					smorkTargets.putIfAbsent(source, (Player) projectile.getTargetActor());
				}
			}
		}
	}

	@Override
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		GraphicsObject g = event.getGraphicsObject();
		if (isActive() && g.getId() != SMORK_FIRE)
		{
			WorldPoint wp = WorldPoint.fromLocal(client, g.getLocation());
			debug("gfx " + g.getId() + " at " + wp.getX() + "," + wp.getY());
		}
		if (event.getGraphicsObject().getId() == SMORK_FIRE)
		{
			fires.add(event.getGraphicsObject());
		}
	}

	@Override
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (isActive())
		{
			WorldPoint wp = event.getGameObject().getWorldLocation();
			debug("object " + event.getGameObject().getId() + " at " + wp.getX() + "," + wp.getY());
		}
	}

	@Override
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		if (isActive())
		{
			WorldPoint wp = event.getGroundObject().getWorldLocation();
			debug("ground object " + event.getGroundObject().getId() + " at " + wp.getX() + "," + wp.getY());
		}
	}

	@Override
	public void onGameTick(GameTick event)
	{
		// Where Bork moves to, and his spot animations (the stomp may show on him first)
		if (boss != null)
		{
			WorldPoint at = boss.getWorldLocation();
			if (!at.equals(lastBorkTile))
			{
				debug("bork moved to " + tile(boss) + " facing " + boss.getOrientation());
				lastBorkTile = at;
			}
			for (net.runelite.api.ActorSpotAnim spotAnim : boss.getSpotAnims())
			{
				if (loggedSpotAnims.add(spotAnim.getId() * 100000L + client.getTickCount() / 4))
				{
					debug("bork spotanim " + spotAnim.getId());
				}
			}
		}

		// Bombs that have landed, and fire that has burnt out
		fireBombs.removeIf(p -> p.getRemainingCycles() <= 0);
		fires.removeIf(GraphicsObject::finished);

		// A Smork that's attacking someone picks them, if it hasn't picked anyone yet
		WorldView wv = client.getTopLevelWorldView();
		if (wv != null)
		{
			for (NPC npc : wv.npcs())
			{
				if (npc.getId() == NpcID.TFA_SMORK && !npc.isDead() && npc.getInteracting() instanceof Player)
				{
					smorkTargets.putIfAbsent(npc, (Player) npc.getInteracting());
				}
			}
		}
		smorkTargets.keySet().removeIf(NPC::isDead);

		if (stunTicks > 0 && --stunTicks == 0)
		{
			clearStun();
		}
	}

	/** Every player's tile, to see where the stomp goes compared to the team. */
	private void logPlayers()
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return;
		}
		StringBuilder sb = new StringBuilder("players:");
		for (Player player : wv.players())
		{
			WorldPoint wp = player.getWorldLocation();
			sb.append(' ').append(player.getName()).append('@').append(wp.getX()).append(',').append(wp.getY());
		}
		debug(sb.toString());
	}

	private static String tile(Actor actor)
	{
		WorldPoint wp = actor.getWorldLocation();
		return wp.getX() + "," + wp.getY();
	}

	private static String name(Actor actor)
	{
		if (actor == null)
		{
			return "nobody";
		}
		return (actor instanceof NPC ? "npc " + ((NPC) actor).getId() : actor.getName()) + "@" + tile(actor);
	}

	private void debug(String message)
	{
		log.info("[Bork t{}] {}", client.getTickCount(), message);
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
