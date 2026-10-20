package net.runelite.client.plugins.fracturedarchive.ezekiel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
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

	private final Map<NPC, SwordTrack> swords = new HashMap<>();

	/** Attack counter: ticks remaining (0 = none) and the attack that started it. */
	@Getter
	private int attackTicks;
	@Getter
	private int attackAnimation = -1;

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
		if (config.ezekielDebug() && (isActive() || !swords.isEmpty()))
		{
			debug("npc " + npc.getId() + " (" + npc.getName() + ") at " + tile(npc.getWorldLocation()));
		}
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

		if (config.ezekielDebug() && (bossIds().contains(npc.getId()) || npc.getId() == NpcID.TFA_EZEKIEL_FINALE_VFX))
		{
			debug("anim " + animation + " on " + npc.getName() + " (" + npc.getId() + ") at " + tile(npc.getWorldLocation()));
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
				attackAnimation = animation;
				// +1 so the first number shown is the full attack speed
				attackTicks = config.ezekielAttackSpeed() + 1;
				break;
			case AnimationID.NPC_EZEKIEL_FINALE01:
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

	@Override
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		if (config.ezekielDebug() && (isActive() || !swords.isEmpty()))
		{
			GraphicsObject g = event.getGraphicsObject();
			debug("gfx " + g.getId() + " at " + tile(WorldPoint.fromLocal(client, g.getLocation())));
		}
	}

	@Override
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (config.ezekielDebug() && isActive())
		{
			GameObject o = event.getGameObject();
			debug("object " + o.getId() + " at " + tile(o.getWorldLocation()));
		}
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
				return config.ezekielMeleeColor();
			case AnimationID.NPC_EZEKIEL_MAGIC01:
				return config.ezekielMagicColor();
			default:
				return config.ezekielSpecialColor();
		}
	}

	private void debug(String message)
	{
		String finale = finaleStartTick >= 0 ? " finale+" + getFinaleTicks() : "";
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[Ezekiel t" + client.getTickCount() + finale + "] " + message, null);
	}

	private static String tile(WorldPoint p)
	{
		return p == null ? "?" : p.getX() + "," + p.getY();
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
		return config.ezekielSwordTicks() - (client.getTickCount() - track.getSpawnTick());
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
