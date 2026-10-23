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
import net.runelite.api.coords.LocalPoint;
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

	/**
	 * Stomps: 4 of them, 3 ticks apart. The first lands 3 ticks after he starts the stomp
	 * (14549), the next 3 after that, and so on. 14550 and 14551 come 1 tick before a stomp lands,
	 * so they keep the count in step if the start was missed.
	 */
	private static final Set<Integer> STOMP_ANIMATIONS = Set.of(
		AnimationID.NPC_BORK_ENRAGED_STOMP_ATTACK01,
		AnimationID.NPC_BORK_ENRAGED_STOMP_ATTACK_CYCLE_AND_OUT01);
	private static final int STOMP_GAP = 3;
	private static final int STOMP_COUNT = 4;
	/** The tick the next stomp lands (-1 = none coming), and how many are still to come. */
	private int nextStompTick = -1;
	private int stompsLeft;

	/** Ticks until the next stomp lands (0 = none coming). */
	int getStompTicks()
	{
		if (nextStompTick < 0)
		{
			return 0;
		}
		// Worked out from the tick number straight away, without waiting for the game tick event
		// (which comes at the end of the tick): otherwise each stomp tick shows nothing until then
		int remaining = nextStompTick - client.getTickCount();
		int left = stompsLeft;
		while (remaining <= 0 && left > 1)
		{
			remaining += STOMP_GAP;
			left--;
		}
		return Math.max(0, remaining);
	}

	/**
	 * Stomp safe spot. After every stun Bork stomps 4 times (starting about 17 ticks after the
	 * stun), hitting the whole room except the "shadow" behind the Smork, as seen from Bork.
	 * He doesn't move from the stun to the stomp, so it's known as soon as he's stunned.
	 */
	private final Set<WorldPoint> stompSafeTiles = new HashSet<>();
	private int stompSafeUntil = -1;

	Set<WorldPoint> getStompSafeTiles()
	{
		return Collections.unmodifiableSet(stompSafeTiles);
	}

	private void updateStompSafeTiles()
	{
		stompSafeTiles.clear();
		WorldView wv = client.getTopLevelWorldView();
		if (boss == null || wv == null || (stunTicks <= 0 && client.getTickCount() > stompSafeUntil))
		{
			return;
		}

		java.util.List<WorldPoint> smorks = new java.util.ArrayList<>();
		for (NPC npc : wv.npcs())
		{
			if (npc.getId() == NpcID.TFA_SMORK && !npc.isDead())
			{
				smorks.add(npc.getWorldLocation());
			}
		}
		if (smorks.isEmpty())
		{
			return;
		}

		// Bork stomps from the corner at the middle of his 4x4
		WorldPoint b = boss.getWorldLocation();
		double ox = b.getX() + 2;
		double oy = b.getY() + 2;
		net.runelite.api.CollisionData[] maps = wv.getCollisionMaps();
		int[][] flags = maps != null ? maps[wv.getPlane()].getFlags() : null;

		// Only the ring of tiles right around each Smork: where to stand to get back to Bork
		for (WorldPoint smork : smorks)
		{
			for (int x = smork.getX() - 1; x <= smork.getX() + 3; x++)
			{
				for (int y = smork.getY() - 1; y <= smork.getY() + 3; y++)
				{
					WorldPoint tile = new WorldPoint(x, y, b.getPlane());
					if (!isSmorkTile(smorks, x, y) && !isBlocked(flags, tile) && inShadow(ox, oy, x, y, smork))
					{
						stompSafeTiles.add(tile);
					}
				}
			}
		}
	}

	private static boolean isSmorkTile(java.util.List<WorldPoint> smorks, int x, int y)
	{
		for (WorldPoint s : smorks)
		{
			if (x >= s.getX() && x < s.getX() + 3 && y >= s.getY() && y < s.getY() + 3)
			{
				return true;
			}
		}
		return false;
	}

	private boolean isBlocked(int[][] flags, WorldPoint tile)
	{
		LocalPoint lp = LocalPoint.fromWorld(client, tile);
		if (lp == null || flags == null)
		{
			return lp == null;
		}
		int sx = lp.getSceneX();
		int sy = lp.getSceneY();
		if (sx < 0 || sy < 0 || sx >= flags.length || sy >= flags[sx].length)
		{
			return true;
		}
		return (flags[sx][sy] & net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0;
	}

	/**
	 * Safe if at least 13 of 100 points spread across the tile are hidden behind the Smork's
	 * middle from where Bork stomps. Fitted to 53 logged stomps (316 safe tiles next to the
	 * Smork): no hit tile reached more than 11, and only 4 safe ones came in under 13.
	 */
	private static boolean inShadow(double ox, double oy, int x, int y, WorldPoint smork)
	{
		double x0 = smork.getX() + 0.5;
		double y0 = smork.getY() + 0.5;
		double x1 = smork.getX() + 2.5;
		double y1 = smork.getY() + 2.5;
		int hidden = 0;
		for (int i = 0; i < 10; i++)
		{
			for (int j = 0; j < 10; j++)
			{
				if (segmentHitsBox(ox, oy, x + i / 10.0 + 0.05, y + j / 10.0 + 0.05, x0, y0, x1, y1))
				{
					hidden++;
				}
			}
		}
		return hidden >= 13;
	}

	/** Whether the segment from (ax, ay) to (bx, by) passes through the box. */
	private static boolean segmentHitsBox(double ax, double ay, double bx, double by,
		double x0, double y0, double x1, double y1)
	{
		double tMin = 0;
		double tMax = 1;
		double[] d = {bx - ax, by - ay};
		double[] a = {ax, ay};
		double[] lo = {x0, y0};
		double[] hi = {x1, y1};
		for (int i = 0; i < 2; i++)
		{
			if (Math.abs(d[i]) < 1e-9)
			{
				if (a[i] < lo[i] || a[i] > hi[i])
				{
					return false;
				}
			}
			else
			{
				double t1 = (lo[i] - a[i]) / d[i];
				double t2 = (hi[i] - a[i]) / d[i];
				tMin = Math.max(tMin, Math.min(t1, t2));
				tMax = Math.min(tMax, Math.max(t1, t2));
				if (tMin > tMax)
				{
					return false;
				}
			}
		}
		return true;
	}

	/** How long the cannon stuns Bork. */
	private static final int STUN_TICKS = 16;

	// Smork projectiles and the fire left on the ground
	private static final int SMORK_MAGIC_SHOT = SpotanimID.VFX_SMORK_MAGE_ATTACK_PROJANIM01;
	private static final int SMORK_RANGED_SHOT = SpotanimID.VFX_SMORK_RANGE_ATTACK_PROJANIM01;
	private static final int SMORK_FIRE_BOMB = SpotanimID.VFX_SMORK_NAPALM_FIRE_PROJANIM01;
	private static final int SMORK_FIRE = SpotanimID.VFX_SMORK_NAPALM_FIRE_SPOTANIM01;
	/** The big cannonball a Smork fires at the ground about 20 ticks after each fire bomb. */
	private static final int SMORK_CANNONBALL = SpotanimID.CANNONBALL_TRAVEL_BIG;

	enum ShotStyle
	{
		MAGIC,
		RANGED
	}

	/** The style of each Smork's last shot. */
	private final Map<NPC, ShotStyle> smorkStyles = new HashMap<>();

	/** Fire bombs in the air. */
	private final Set<Projectile> fireBombs = new HashSet<>();

	/** Smorks' normal magic and ranged shots in the air. */
	private final Set<Projectile> smorkProjectiles = new HashSet<>();

	Set<Projectile> getSmorkProjectiles()
	{
		return Collections.unmodifiableSet(smorkProjectiles);
	}

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
		smorkProjectiles.clear();
		smorkTargets.clear();
		fires.clear();
		clearStun();
		calledTarget = null;
		lastBorkTile = null;
		stompSafeTiles.clear();
		stompSafeUntil = -1;
		nextStompTick = -1;
		stompsLeft = 0;
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
			if (STOMP_ANIMATIONS.contains(animation))
			{
				// A stomp lands next tick: 14550 is the first of the four, 14551 the third
				nextStompTick = client.getTickCount() + 1;
				stompsLeft = animation == AnimationID.NPC_BORK_ENRAGED_STOMP_ATTACK01 ? STOMP_COUNT : STOMP_COUNT - 2;
				stompSafeUntil = client.getTickCount() + 12;
			}
			else if (animation == AnimationID.NPC_BORK_ENRAGED_STOMP_ATTACK_IN01
				|| animation == AnimationID.NPC_BORK_IDLE_TO_ENRAGED_STOMP_ATTACK_IN01)
			{
				nextStompTick = client.getTickCount() + STOMP_GAP;
				stompsLeft = STOMP_COUNT;
				stompSafeUntil = client.getTickCount() + 14;
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
		if (isActive() && loggedProjectiles.add(projectile))
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
		else if (id == SMORK_CANNONBALL && projectile.getSourceActor() instanceof NPC
			&& ((NPC) projectile.getSourceActor()).getId() == NpcID.TFA_SMORK)
		{
			// Marked like the fire bomb: where it lands, and the ticks until it does
			fireBombs.add(projectile);
		}
		else if (id == SMORK_MAGIC_SHOT || id == SMORK_RANGED_SHOT)
		{
			// Smork volleys hit every player at once and come with no source actor: credit the
			// Smork nearest where they start
			if (smorkProjectiles.add(projectile))
			{
				NPC source = projectile.getSourceActor() instanceof NPC ? (NPC) projectile.getSourceActor() : nearestSmork(projectile);
				if (source != null && source.getId() == NpcID.TFA_SMORK)
				{
					smorkStyles.put(source, id == SMORK_MAGIC_SHOT ? ShotStyle.MAGIC : ShotStyle.RANGED);
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
		updateStompSafeTiles();

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
		smorkProjectiles.removeIf(p -> p.getRemainingCycles() <= 0);
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

		if (nextStompTick >= 0 && client.getTickCount() >= nextStompTick)
		{
			// That one landed; on to the next, until all four are done
			if (--stompsLeft > 0)
			{
				nextStompTick += STOMP_GAP;
			}
			else
			{
				nextStompTick = -1;
			}
		}

		if (stunTicks > 0 && --stunTicks == 0)
		{
			clearStun();
		}
	}

	/** The Smork closest to where a projectile started, or null. */
	private NPC nearestSmork(Projectile projectile)
	{
		WorldView wv = client.getTopLevelWorldView();
		WorldPoint from = projectile.getSourcePoint();
		if (wv == null || from == null)
		{
			return null;
		}
		NPC best = null;
		int bestDistance = Integer.MAX_VALUE;
		for (NPC npc : wv.npcs())
		{
			if (npc.getId() == NpcID.TFA_SMORK)
			{
				int d = npc.getWorldLocation().distanceTo(from);
				if (d < bestDistance)
				{
					best = npc;
					bestDistance = d;
				}
			}
		}
		return best;
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
