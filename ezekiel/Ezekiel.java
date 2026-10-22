package net.runelite.client.plugins.fracturedarchive.ezekiel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

/**
 * Ezekiel. Second encounter, paired with Bork. A justiciar of Saradomin.
 */
@Slf4j
@Singleton
public class Ezekiel extends Encounter
{
	// Related: TFA_EZEKIEL_SWORD_NOOP, TFA_EZEKIEL_DEATH_SWORD
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_EZEKIEL);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	/** The swords that fly across the room. The _NOOP version is a sword that's finished. */
	private static final int SWORD_ID = NpcID.TFA_EZEKIEL_SWORD;

	/** Ticks from a sword appearing until it goes off. */
	private static final int SWORD_TICKS = 15;

	private final Map<NPC, SwordTrack> swords = new HashMap<>();

	/** Attack counter: ticks remaining (0 = none) and the attack that started it. */
	@Getter
	private int attackTicks;
	@Getter
	private int attackAnimation = -1;

	/**
	 * His rhythm: two basic attacks, then the star special. Counts 1, 2 on the basic attacks and 3
	 * on the star, then goes away once the star's attack is done.
	 */
	private int starCount;
	private int starShownUntil = -1;
	private int lastAttackTick = -1;

	/**
	 * Between stars he does two melees, a pause (9+ ticks), then two more melees and the star.
	 * A pause this long starts the count over, so only the pair right before the star shows 1, 2.
	 */
	private static final int STAR_COUNT_RESET_GAP = 9;

	/** Colors (the colors you had saved). */
	static final java.awt.Color MELEE_COLOR = java.awt.Color.RED;
	static final java.awt.Color MAGIC_COLOR = java.awt.Color.CYAN;
	static final java.awt.Color SPECIAL_COLOR = java.awt.Color.YELLOW;

	/** Ticks between Ezekiel's attacks. */
	private static final int ATTACK_SPEED = 5;

	/** Tick the finale started, or -1 when it isn't happening. */
	private int finaleStartTick = -1;

	@Inject
	private EzekielOverlay overlay;

	@Inject
	protected Ezekiel(FracturedArchiveConfig config)
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
		swords.clear();
		roomCenter = null;
		attackTicks = 0;
		attackAnimation = -1;
		finaleStartTick = -1;
		starCount = 0;
		starShownUntil = -1;
		lastAttackTick = -1;
	}

	Map<NPC, SwordTrack> getSwords()
	{
		return Collections.unmodifiableMap(swords);
	}

	// Swords are tracked whether or not Ezekiel has been picked up yet.

	@Override
	public void onNpcSpawned(NpcSpawned event)
	{
		super.onNpcSpawned(event);
		NPC npc = event.getNpc();
		if (npc.getId() == SWORD_ID)
		{
			swords.put(npc, new SwordTrack(client.getTickCount()));
		}
	}

	@Override
	public void onNpcChanged(NpcChanged event)
	{
		super.onNpcChanged(event);
		NPC npc = event.getNpc();
		if (npc.getId() == SWORD_ID)
		{
			swords.putIfAbsent(npc, new SwordTrack(client.getTickCount()));
		}
		else
		{
			// e.g. turned into the finished (_NOOP) sword
			swords.remove(npc);
		}
	}

	@Override
	public void onNpcDespawned(NpcDespawned event)
	{
		super.onNpcDespawned(event);
		swords.remove(event.getNpc());
	}

	// Finale logging: everything that happens during the finale goes to the log file, to find
	// what marks the sword the lightning misses

	private boolean inFinale()
	{
		return finaleStartTick >= 0;
	}

	private void debug(String message)
	{
		log.info("[Ezekiel t{} finale+{}] {}", client.getTickCount(), getFinaleTicks(), message);
	}

	private static String tile(net.runelite.api.coords.WorldPoint p)
	{
		return p == null ? "?" : p.getX() + "," + p.getY();
	}

	private static String who(net.runelite.api.Actor actor)
	{
		if (actor == null)
		{
			return "nobody";
		}
		return (actor instanceof NPC ? "npc " + ((NPC) actor).getId() + " (" + actor.getName() + ")" : actor.getName())
			+ "@" + tile(actor.getWorldLocation());
	}

	/** Effects already showing on each actor, so each is logged once when it appears and ends. */
	private final Map<net.runelite.api.Actor, Set<Integer>> shownSpotAnims = new java.util.WeakHashMap<>();
	private final Set<net.runelite.api.Projectile> loggedProjectiles = Collections.newSetFromMap(new java.util.WeakHashMap<>());

	private void recordFinale()
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return;
		}
		java.util.List<net.runelite.api.Actor> actors = new java.util.ArrayList<>();
		wv.npcs().forEach(actors::add);
		wv.players().forEach(actors::add);
		// Where you are each tick: with the hits below, that shows which tiles were safe
		if (client.getLocalPlayer() != null)
		{
			debug("you at " + tile(client.getLocalPlayer().getWorldLocation()));
		}

		// Every finale sword's effects each tick, with their timing, to catch effects being replayed
		StringBuilder swordsLine = new StringBuilder();
		for (NPC npc : wv.npcs())
		{
			if (npc.getId() == NpcID.TFA_EZEKIEL_FINALE_VFX)
			{
				swordsLine.append(" | ").append(tile(npc.getWorldLocation())).append(':');
				for (net.runelite.api.ActorSpotAnim sa : npc.getSpotAnims())
				{
					swordsLine.append(' ').append(sa.getId()).append(" f").append(sa.getFrame())
						.append(" start").append(sa.getStartCycle() - client.getGameCycle());
				}
				swordsLine.append(" anim ").append(npc.getAnimation());
			}
		}
		if (swordsLine.length() > 0)
		{
			debug("swords" + swordsLine);
		}

		for (net.runelite.api.Actor actor : actors)
		{
			Set<Integer> now = new java.util.HashSet<>();
			for (net.runelite.api.ActorSpotAnim spotAnim : actor.getSpotAnims())
			{
				now.add(spotAnim.getId());
			}
			Set<Integer> before = shownSpotAnims.getOrDefault(actor, Set.of());
			for (int id : now)
			{
				if (!before.contains(id))
				{
					debug("spotanim " + id + " on " + who(actor));
				}
			}
			for (int id : before)
			{
				if (!now.contains(id))
				{
					debug("spotanim " + id + " ended on " + who(actor));

				}
			}
			if (now.isEmpty())
			{
				shownSpotAnims.remove(actor);
			}
			else
			{
				shownSpotAnims.put(actor, now);
			}
		}
	}

	private void logFinaleStart()
	{
		StringBuilder sb = new StringBuilder("finale start: ezekiel " + (boss != null ? who(boss) + " facing " + boss.getOrientation() : "?"));
		for (NPC sword : swords.keySet())
		{
			sb.append(" | sword ").append(who(sword)).append(" facing ").append(sword.getOrientation());
		}
		debug(sb.toString());
		shownSpotAnims.clear();
	}

	@Override
	public void onProjectileMoved(net.runelite.api.events.ProjectileMoved event)
	{
		net.runelite.api.Projectile p = event.getProjectile();
		if (inFinale() && loggedProjectiles.add(p))
		{
			debug("projectile " + p.getId() + " from " + who(p.getSourceActor()) + " " + tile(p.getSourcePoint())
				+ " to " + (p.getTargetActor() != null ? who(p.getTargetActor()) : tile(p.getTargetPoint()))
				+ ", " + (p.getRemainingCycles() + 29) / 30 + " ticks");
		}
	}

	@Override
	public void onHitsplatApplied(net.runelite.api.events.HitsplatApplied event)
	{
		if (inFinale() && event.getActor() == client.getLocalPlayer())
		{
			debug("you were hit for " + event.getHitsplat().getAmount() + " (type " + event.getHitsplat().getHitsplatType()
				+ ") at " + tile(event.getActor().getWorldLocation()));
		}
	}

	@Override
	public void onGraphicsObjectCreated(net.runelite.api.events.GraphicsObjectCreated event)
	{
		if (inFinale())
		{
			net.runelite.api.GraphicsObject g = event.getGraphicsObject();
			// Delay before it starts, in client cycles (30 per tick): the strikes may be queued up
			debug("gfx " + g.getId() + " at " + tile(net.runelite.api.coords.WorldPoint.fromLocal(client, g.getLocation()))
				+ " starts in " + (g.getStartCycle() - client.getGameCycle()) + " cycles");
		}
	}

	@Override
	public void onGameObjectSpawned(net.runelite.api.events.GameObjectSpawned event)
	{
		if (inFinale())
		{
			debug("object " + event.getGameObject().getId() + " at " + tile(event.getGameObject().getWorldLocation()));
		}
	}

	@Override
	public void onGroundObjectSpawned(net.runelite.api.events.GroundObjectSpawned event)
	{
		if (inFinale())
		{
			debug("ground object " + event.getGroundObject().getId() + " at " + tile(event.getGroundObject().getWorldLocation()));
		}
	}

	@Override
	public void onGameTick(GameTick event)
	{
		if (inFinale())
		{
			recordFinale();
		}
		if (attackTicks > 0 && --attackTicks == 0)
		{
			attackAnimation = -1;
		}

		// Lock each sword's direction the first tick after it appears. After that it's never
		// changed, because a sword turns to face whoever hits it.
		for (Map.Entry<NPC, SwordTrack> entry : swords.entrySet())
		{
			SwordTrack track = entry.getValue();
			if (!track.hasDirection())
			{
				int[] dir = pickDirection(entry.getKey());
				track.setDirection(dir[0], dir[1]);
			}
		}
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
		if (inFinale())
		{
			debug("anim " + animation + " on " + who(npc) + " facing " + npc.getOrientation());
		}

		if (!bossIds().contains(npc.getId()))
		{
			return;
		}

		switch (animation)
		{
			case AnimationID.NPC_EZEKIEL_MELEE01:
			case AnimationID.NPC_EZEKIEL_MAGIC01:
			case AnimationID.NPC_EZEKIEL_SPECIAL01:
				if (animation == AnimationID.NPC_EZEKIEL_SPECIAL01)
				{
					// The star: show 3 until its attack is done
					starCount = 3;
					starShownUntil = client.getTickCount() + ATTACK_SPEED;
				}
				else
				{
					boolean pause = lastAttackTick >= 0 && client.getTickCount() - lastAttackTick >= STAR_COUNT_RESET_GAP;
					starCount = starCount >= 3 || pause ? 1 : starCount + 1;
					starShownUntil = -1;
				}
				lastAttackTick = client.getTickCount();
				attackAnimation = animation;
				// +1 so the first number shown is the full attack speed
				attackTicks = ATTACK_SPEED + 1;
				break;
			case AnimationID.NPC_EZEKIEL_FINALE01:
				starCount = 0;
				starShownUntil = -1;
				// No attack counter during the finale
				finaleStartTick = client.getTickCount();
				logFinaleStart();
				attackTicks = 0;
				attackAnimation = -1;
				break;
			case AnimationID.NPC_EZEKIEL_FINALE01_VFX:
			case AnimationID.NPC_EZEKIEL_FINALE_LOOP01:
				if (finaleStartTick < 0)
				{
					finaleStartTick = client.getTickCount();
					logFinaleStart();
				}
				break;
			case AnimationID.NPC_EZEKIEL_FINALE_DEATH01:
			case AnimationID.NPC_EZEKIEL_DEATH01:
				if (inFinale())
				{
					debug("finale over (" + animation + ")");
				}
				finaleStartTick = -1;
				shownSpotAnims.clear();
				break;
			default:
				break;
		}
	}

	/** Basic attacks since his last star (1, 2, then 3 on the star), or 0 when hidden. */
	int getStarCount()
	{
		if (starShownUntil >= 0 && client.getTickCount() >= starShownUntil)
		{
			starCount = 0;
			starShownUntil = -1;
		}
		return starCount;
	}

	/** Ticks since the finale started, or -1 when it isn't happening. */
	int getFinaleTicks()
	{
		return finaleStartTick < 0 ? -1 : client.getTickCount() - finaleStartTick;
	}

	java.awt.Color getAttackColor()
	{
		switch (attackAnimation)
		{
			case AnimationID.NPC_EZEKIEL_MELEE01:
				return MELEE_COLOR;
			case AnimationID.NPC_EZEKIEL_MAGIC01:
				return MAGIC_COLOR;
			default:
				return SPECIAL_COLOR;
		}
	}

	@Override
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			swords.clear();
			roomCenter = null;
		}
	}

	/** Ticks until the sword goes off (0 or less once it has). */
	int ticksUntilLaunch(SwordTrack track)
	{
		return SWORD_TICKS - (client.getTickCount() - track.getSpawnTick());
	}

	/** Center of the room in scene tiles, worked out once per fight from the open floor. */
	private int[] roomCenter;

	/**
	 * The straight direction the sword will fly: straight across to the opposite side of the
	 * room. A sword on the north side goes south, on the east side goes west, and so on.
	 */
	private int[] pickDirection(NPC sword)
	{
		LocalPoint start = sword.getLocalLocation();
		WorldView wv = client.getTopLevelWorldView();
		if (start == null || wv == null || wv.getCollisionMaps() == null)
		{
			return cardinal(sword.getOrientation());
		}

		int[][] flags = wv.getCollisionMaps()[wv.getPlane()].getFlags();
		int sx = start.getSceneX();
		int sy = start.getSceneY();
		if (roomCenter == null)
		{
			roomCenter = findRoomCenter(flags, sx, sy);
		}
		if (roomCenter == null)
		{
			return cardinal(sword.getOrientation());
		}

		int offX = sx - roomCenter[0];
		int offY = sy - roomCenter[1];
		if (Math.abs(offX) == Math.abs(offY))
		{
			// Exactly on a diagonal: fall back to the way it's facing
			return cardinal(sword.getOrientation());
		}
		return Math.abs(offY) > Math.abs(offX)
			? new int[]{0, -Integer.signum(offY)}  // north side goes south, south side goes north
			: new int[]{-Integer.signum(offX), 0}; // east side goes west, west side goes east
	}

	private static final int ROOM_RADIUS = 30;

	/**
	 * Fills the open floor next to the sword (within ROOM_RADIUS tiles) and returns the middle of
	 * what it covers, or null if the sword isn't next to any floor.
	 */
	private static int[] findRoomCenter(int[][] flags, int sx, int sy)
	{
		int[] open = null;
		// Swords sit in the wall, so find the nearest open tile first
		for (int r = 0; r <= 4 && open == null; r++)
		{
			for (int x = sx - r; x <= sx + r && open == null; x++)
			{
				for (int y = sy - r; y <= sy + r; y++)
				{
					if (isOpen(flags, x, y))
					{
						open = new int[]{x, y};
						break;
					}
				}
			}
		}
		if (open == null)
		{
			return null;
		}

		boolean[][] seen = new boolean[Constants.SCENE_SIZE][Constants.SCENE_SIZE];
		java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
		queue.add(open);
		seen[open[0]][open[1]] = true;
		int minX = open[0], maxX = open[0], minY = open[1], maxY = open[1];
		while (!queue.isEmpty())
		{
			int[] t = queue.poll();
			minX = Math.min(minX, t[0]);
			maxX = Math.max(maxX, t[0]);
			minY = Math.min(minY, t[1]);
			maxY = Math.max(maxY, t[1]);
			int[][] next = {{t[0] + 1, t[1]}, {t[0] - 1, t[1]}, {t[0], t[1] + 1}, {t[0], t[1] - 1}};
			for (int[] n : next)
			{
				if (Math.abs(n[0] - sx) > ROOM_RADIUS || Math.abs(n[1] - sy) > ROOM_RADIUS
					|| !isOpen(flags, n[0], n[1]) || seen[n[0]][n[1]])
				{
					continue;
				}
				seen[n[0]][n[1]] = true;
				queue.add(n);
			}
		}
		return new int[]{(minX + maxX) / 2, (minY + maxY) / 2};
	}

	private static boolean isOpen(int[][] flags, int x, int y)
	{
		return x >= 0 && y >= 0 && x < Constants.SCENE_SIZE && y < Constants.SCENE_SIZE
			&& (flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
	}

	/**
	 * Turns an orientation (0 = south, 512 = west, 1024 = north, 1536 = east) into the nearest of
	 * the 4 straight directions. Swords never go diagonally.
	 */
	static int[] cardinal(int orientation)
	{
		int quarter = Math.floorMod(Math.round(orientation / 512f), 4);
		switch (quarter)
		{
			case 0:
				return new int[]{0, -1}; // south
			case 1:
				return new int[]{-1, 0}; // west
			case 2:
				return new int[]{0, 1}; // north
			default:
				return new int[]{1, 0}; // east
		}
	}

	/** When a sword appeared and the direction it will fly, locked in once. */
	static final class SwordTrack
	{
		private final int spawnTick;
		private int dx;
		private int dy;
		private boolean hasDirection;

		SwordTrack(int spawnTick)
		{
			this.spawnTick = spawnTick;
		}

		void setDirection(int dx, int dy)
		{
			this.dx = dx;
			this.dy = dy;
			hasDirection = true;
		}

		boolean hasDirection()
		{
			return hasDirection;
		}

		int getSpawnTick()
		{
			return spawnTick;
		}

		int getDx()
		{
			return dx;
		}

		int getDy()
		{
			return dy;
		}
	}
}
