package net.runelite.client.plugins.fracturedarchive.ezekiel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
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
		strikes.clear();
		strikeArea.clear();
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

	private boolean inFinale()
	{
		return finaleStartTick >= 0;
	}

	/** The finale's lightning strikes on the 3x3 in front of him (impact graphics). */
	private static final Set<Integer> STRIKE_GRAPHICS = Set.of(
		net.runelite.api.gameval.SpotanimID.VFX_EZEKIEL_FINALE_IMPACT_LARGE01,
		net.runelite.api.gameval.SpotanimID.VFX_EZEKIEL_FINALE_IMPACT_MEDIUM01,
		net.runelite.api.gameval.SpotanimID.VFX_EZEKIEL_FINALE_IMPACT_SMALL01);
	/** Queued strikes: the tick each lands, and its tiles. */
	private final java.util.TreeMap<Integer, Set<net.runelite.api.coords.WorldPoint>> strikes = new java.util.TreeMap<>();
	/** Every tile struck this round (the 3x3), and the tick the round's strikes were queued. */
	private final Set<net.runelite.api.coords.WorldPoint> strikeArea = new java.util.HashSet<>();
	private int strikeAreaTick = -100;

	/**
	 * This round's order, as back-row tile -> 1, 2 or 3. Each strike leaves some of the 3x3 safe:
	 * a side column (stand in that back corner) or the whole back row (stand in its middle).
	 * Shown until the round's last strike lands.
	 */
	java.util.Map<net.runelite.api.coords.WorldPoint, Integer> getStrikeOrder()
	{
		java.util.SortedMap<Integer, Set<net.runelite.api.coords.WorldPoint>> round = strikes.tailMap(strikeAreaTick);
		if (round.isEmpty() || round.lastKey() < client.getTickCount() || strikeArea.isEmpty())
		{
			return java.util.Map.of();
		}

		// The back row: the struck tiles farthest from Ezekiel
		if (boss == null)
		{
			return java.util.Map.of();
		}
		net.runelite.api.coords.WorldPoint ez = boss.getWorldLocation();
		int[] dir = cardinal(boss.getOrientation());
		int far = Integer.MIN_VALUE;
		for (net.runelite.api.coords.WorldPoint wp : strikeArea)
		{
			far = Math.max(far, (wp.getX() - ez.getX()) * dir[0] + (wp.getY() - ez.getY()) * dir[1]);
		}

		java.util.Map<net.runelite.api.coords.WorldPoint, Integer> order = new java.util.HashMap<>();
		int step = 1;
		for (Set<net.runelite.api.coords.WorldPoint> hit : round.values())
		{
			java.util.List<net.runelite.api.coords.WorldPoint> safeBack = new java.util.ArrayList<>();
			for (net.runelite.api.coords.WorldPoint wp : strikeArea)
			{
				int along = (wp.getX() - ez.getX()) * dir[0] + (wp.getY() - ez.getY()) * dir[1];
				if (along == far && !hit.contains(wp))
				{
					safeBack.add(wp);
				}
			}
			net.runelite.api.coords.WorldPoint target = null;
			if (safeBack.size() == 1)
			{
				target = safeBack.get(0);
			}
			else if (!safeBack.isEmpty())
			{
				// The whole back row is safe: its middle tile
				int sx = 0;
				int sy = 0;
				for (net.runelite.api.coords.WorldPoint wp : safeBack)
				{
					sx += wp.getX();
					sy += wp.getY();
				}
				target = new net.runelite.api.coords.WorldPoint(Math.round((float) sx / safeBack.size()),
					Math.round((float) sy / safeBack.size()), ez.getPlane());
			}
			if (target != null)
			{
				order.putIfAbsent(target, step);
			}
			step++;
		}
		return order;
	}

	@Override
	public void onGraphicsObjectCreated(net.runelite.api.events.GraphicsObjectCreated event)
	{
		if (inFinale())
		{
			net.runelite.api.GraphicsObject g = event.getGraphicsObject();
			int delay = g.getStartCycle() - client.getGameCycle();
			if (STRIKE_GRAPHICS.contains(g.getId()) && delay >= 60)
			{
				// Each round queues its three strikes at once, about 4, 5 and 6 ticks ahead, in a
				// random order: group the tiles by the tick each lands on
				if (client.getTickCount() != strikeAreaTick)
				{
					// A new round
					strikeArea.clear();
					strikeAreaTick = client.getTickCount();
				}
				int lands = client.getTickCount() + delay / 30;
				net.runelite.api.coords.WorldPoint wp = net.runelite.api.coords.WorldPoint.fromLocal(client, g.getLocation());
				strikes.computeIfAbsent(lands, k -> new java.util.HashSet<>()).add(wp);
				strikeArea.add(wp);
			}
		}
	}

	@Override
	public void onGameTick(GameTick event)
	{
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
				attackTicks = 0;
				attackAnimation = -1;
				break;
			case AnimationID.NPC_EZEKIEL_FINALE01_VFX:
			case AnimationID.NPC_EZEKIEL_FINALE_LOOP01:
				if (finaleStartTick < 0)
				{
					finaleStartTick = client.getTickCount();
					}
				break;
			case AnimationID.NPC_EZEKIEL_FINALE_DEATH01:
			case AnimationID.NPC_EZEKIEL_DEATH01:
				finaleStartTick = -1;
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
