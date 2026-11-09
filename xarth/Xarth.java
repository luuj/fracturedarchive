package net.runelite.client.plugins.fracturedarchive.xarth;

import java.awt.Color;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.NPC;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

/**
 * Xarth. Fourth encounter, paired with the Dagon'hai Trio. A Tormented Demon.
 */
@Singleton
public class Xarth extends Encounter
{
	// Related: TFA_XARTH_RUBBLE_NPC, TFA_XARTH_CORPSE, TFA_XARTH_CORPSE_DOUSED
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_XARTH);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	/** Ticks between Xarth's attacks. */
	private static final int ATTACK_SPEED = 4;

	/** His special attacks; they share one color. */
	private static final Set<Integer> SPECIAL_ATTACKS = Set.of(
		AnimationID.XARTH_ATTACK_FLAME_BLAST_01,
		AnimationID.XARTH_ATTACK_ROOT_01,
		AnimationID.XARTH_ATTACK_AOE_01,
		AnimationID.XARTH_ATTACK_SUMMON_01);

	/** Attack counter: ticks remaining (0 = none) and the attack that started it. */
	private int attackTicks;
	private int attackAnimation = -1;
	/**
	 * The tick the corpse volley's first fireball was seen (-1 = none). Its count is worked out
	 * from the tick number: the fireball can show up before or after the game tick event within
	 * a tick, so counting it down in onGameTick sometimes started it one higher.
	 */
	private int volleyStartTick = -1;

	int getAttackTicks()
	{
		if (volleyStartTick >= 0)
		{
			if (client.getTickCount() < volleyStartTick)
			{
				// The volley animation's tick: the count starts on the next one
				return 0;
			}
			int left = config.xarthVolleyTicks() - (client.getTickCount() - volleyStartTick);
			if (left > 0)
			{
				return left;
			}
			volleyStartTick = -1;
		}
		return attackTicks;
	}

	@Inject
	private XarthOverlay overlay;

	@Inject
	private net.runelite.client.ui.overlay.infobox.InfoBoxManager infoBoxManager;

	@Inject
	private net.runelite.client.game.ItemManager itemManager;

	@Inject
	private net.runelite.client.plugins.fracturedarchive.FracturedArchivePlugin plugin;

	/** Fire rune, for the burn info box's icon. */
	private static final int BURN_ICON_ITEM = 554;
	private BurnInfoBox burnInfoBox;

	@Override
	public void load()
	{
		super.load();
		burnInfoBox = new BurnInfoBox(itemManager.getImage(BURN_ICON_ITEM), plugin, this, config);
		infoBoxManager.addInfoBox(burnInfoBox);
	}

	@Override
	public void unload()
	{
		infoBoxManager.removeInfoBox(burnInfoBox);
		super.unload();
		clearBurn();
		pendingBurn = 0;
	}

	/*
	 * Burn on you (hitsplat 74), worked out from the logs: the burns all share one timer. Each
	 * hitsplat is the stacks (up to 5) and they hit every 4 ticks. Every one of his magic attacks
	 * that lands on you (even a 0) adds a stack, hits the tick after, and resets the timer to 6
	 * hits. So what's left is stacks x hits left, if no more land.
	 */
	private static final int BURN_HITSPLAT = 74;
	private static final int BURN_HITS = 6;
	/** No burn hit for this long: it's gone. */
	private static final int BURN_TIMEOUT_TICKS = 6;
	private int burnStacks;
	private int burnHitsLeft;
	/** One of his magic attacks landed on you since the last burn hit (resets the timer). */
	private boolean burnRefresh;
	/** Burn damage on you this tick, handled at the game tick. */
	private int pendingBurn;
	private int lastBurnTick = -1;
	/** His magic attacks at you in the air, with the tick each lands. */
	private final java.util.Map<net.runelite.api.Projectile, Integer> myFireballs = new java.util.IdentityHashMap<>();
	private final Set<net.runelite.api.Projectile> seenFireballs = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

	/** Burn damage still to come if no more burns land (0 = not burning). */
	int getBurnRemaining()
	{
		return burnStacks * burnHitsLeft;
	}

	private void clearBurn()
	{
		burnStacks = 0;
		burnHitsLeft = 0;
		burnRefresh = false;
		lastBurnTick = -1;
		myFireballs.clear();
	}

	@Override
	public void onHitsplatApplied(net.runelite.api.events.HitsplatApplied event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}
		net.runelite.api.Hitsplat h = event.getHitsplat();
		if (h.getHitsplatType() == BURN_HITSPLAT)
		{
			pendingBurn += h.getAmount();
		}
		burnLog("hitsplat " + h.getAmount() + " type " + h.getHitsplatType() + (h.isMine() ? " (mine)" : ""));
	}

	@Override
	public void onChatMessage(net.runelite.api.events.ChatMessage event)
	{
		String msg = event.getMessage();
		if (msg != null && msg.startsWith("Challenge complete: Xarth"))
		{
			// Your burn goes out when he dies
			if (burnStacks > 0)
			{
				burnLog("Xarth died, burn cleared (" + getBurnRemaining() + " was shown)");
			}
			clearBurn();
		}
		if (msg != null && (msg.contains("alight") || msg.toLowerCase().contains("burn")
			|| msg.toLowerCase().contains("extinguish") || msg.startsWith("Challenge")))
		{
			burnLog("chat " + msg);
		}
	}

	private void updateBurn()
	{
		int tick = client.getTickCount();
		// His magic attacks landing on you this tick
		java.util.Iterator<Integer> it = myFireballs.values().iterator();
		while (it.hasNext())
		{
			if (it.next() <= tick)
			{
				it.remove();
				burnRefresh = true;
				burnLog("magic attack landed on you (burn refresh)");
			}
		}

		if (pendingBurn > 0)
		{
			int stacks = pendingBurn;
			pendingBurn = 0;
			if (lastBurnTick >= 0 && tick - lastBurnTick > BURN_TIMEOUT_TICKS)
			{
				burnStacks = 0;
			}
			if (burnRefresh || stacks > burnStacks || burnHitsLeft <= 0)
			{
				// A new burn: this is its first hit
				burnHitsLeft = BURN_HITS - 1;
			}
			else
			{
				burnHitsLeft--;
			}
			burnRefresh = false;
			burnStacks = stacks;
			lastBurnTick = tick;
			burnLog("burn " + stacks + " -> " + burnHitsLeft + " hits left, " + getBurnRemaining() + " damage left");
		}
		else if (burnStacks > 0 && lastBurnTick >= 0 && tick - lastBurnTick > BURN_TIMEOUT_TICKS)
		{
			burnLog("burn ended, " + getBurnRemaining() + " damage was still shown");
			clearBurn();
		}
		else if (burnStacks > 0 && burnHitsLeft <= 0 && tick - lastBurnTick >= 4)
		{
			// Its last hit was the one before: done
			clearBurn();
		}
	}

	/** Logging is off (kept as a no-op so it's easy to turn back on). */
	private void burnLog(String message)
	{
	}

	@Inject
	protected Xarth(FracturedArchiveConfig config)
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
		attackTicks = 0;
		attackAnimation = -1;
		volleyStartTick = -1;
		fireballs.clear();
		lastAttackTick = -1;
		volleyEndTick = -1;
		rangedPhase = false;
	}

	@Override
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!(event.getActor() instanceof NPC) || !bossIds().contains(((NPC) event.getActor()).getId()))
		{
			return;
		}

		int animation = event.getActor().getAnimation();
		debug("anim " + animation);
		if (animation == AnimationID.XARTH_ATTACK_MELEE_01
			|| animation == AnimationID.XARTH_ATTACK_MAGIC_01
			|| animation == AnimationID.XARTH_ATTACK_RANGED_01
			|| SPECIAL_ATTACKS.contains(animation))
		{
			if (animation == AnimationID.XARTH_ATTACK_MAGIC_01 && client.getTickCount() < volleyEndTick)
			{
				// Part of the fireball volley already being counted
				return;
			}
			if (animation == AnimationID.XARTH_ATTACK_RANGED_01)
			{
				rangedPhase = true;
			}
			attackAnimation = animation;
			lastAttackTick = client.getTickCount();
			if (animation == AnimationID.XARTH_ATTACK_MAGIC_01 && rangedPhase)
			{
				// Once his ranged attacks have started, his magic animation is only ever the corpse
				// volley. Count it from the animation, starting the tick after: his first fireball
				// comes 1 or 2 ticks after it, so counting from the fireball was off by one at times.
				attackTicks = 0;
				volleyStartTick = client.getTickCount() + 1;
				volleyEndTick = client.getTickCount() + VOLLEY_TICKS;
				return;
			}
			// +1 so the first number shown is the full attack speed
			attackTicks = ATTACK_SPEED + 1;
			volleyStartTick = -1;
		}
	}

	@Override
	public void onGameTick(GameTick event)
	{
		updateBurn();
		if (attackTicks > 0 && --attackTicks == 0)
		{
			attackAnimation = -1;
		}
		fireballs.removeIf(p -> p.getRemainingCycles() <= 0);
	}

	/**
	 * Xarth's fireballs (projectile 4034, from his 14984 attack) thrown at a tile: each spawns a
	 * corpse there as it lands, 2 ticks later.
	 */
	private static final int FIREBALL = 4034;
	/** How long after the first fireball to ignore his repeated volley animations. */
	private static final int VOLLEY_TICKS = 7;

	private int lastAttackTick = -1;
	private int volleyEndTick = -1;
	/** His ranged attacks have started: from then on he doesn't use magic on players. */
	private boolean rangedPhase;
	private final Set<net.runelite.api.Projectile> fireballs = new java.util.HashSet<>();
	private final Set<net.runelite.api.Projectile> logged = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

	/**
	 * Corpses walking to Xarth (not doused ones), with the ticks until each reaches him: they stand
	 * still for 2 ticks after spawning, then walk a tile a tick, so it's the tiles left until it's
	 * next to him plus whatever's left of that wait. Worked out live, so it holds still while a
	 * corpse is held in place.
	 */
	private static final int CORPSE_WAIT_TICKS = 2;
	private final java.util.Map<NPC, Integer> corpseSpawnTicks = new java.util.WeakHashMap<>();

	java.util.Map<NPC, Integer> getCorpseTicks()
	{
		java.util.Map<NPC, Integer> corpses = new java.util.HashMap<>();
		net.runelite.api.WorldView wv = client.getTopLevelWorldView();
		if (boss == null || wv == null)
		{
			return corpses;
		}
		net.runelite.api.coords.WorldPoint sw = boss.getWorldLocation();
		int size = boss.getComposition() != null ? boss.getComposition().getSize() : 1;
		for (NPC npc : wv.npcs())
		{
			if (npc.getId() != net.runelite.api.gameval.NpcID.TFA_XARTH_CORPSE || npc.isDead())
			{
				continue;
			}
			net.runelite.api.coords.WorldPoint p = npc.getWorldLocation();
			int dx = Math.max(0, Math.max(sw.getX() - p.getX(), p.getX() - (sw.getX() + size - 1)));
			int dy = Math.max(0, Math.max(sw.getY() - p.getY(), p.getY() - (sw.getY() + size - 1)));
			Integer spawned = corpseSpawnTicks.get(npc);
			int wait = spawned == null ? 0 : Math.max(0, CORPSE_WAIT_TICKS - (client.getTickCount() - spawned));
			int ticks = Math.max(dx, dy) - 1 + wait;
			// Down to 0 (the tick it reaches him)
			if (ticks >= 0)
			{
				corpses.put(npc, ticks);
			}
		}
		return corpses;
	}

	Set<net.runelite.api.Projectile> getFireballs()
	{
		return java.util.Collections.unmodifiableSet(fireballs);
	}

	@Override
	public void onProjectileMoved(net.runelite.api.events.ProjectileMoved event)
	{
		net.runelite.api.Projectile p = event.getProjectile();
		if (p.getId() == FIREBALL && p.getTargetActor() == client.getLocalPlayer() && seenFireballs.add(p))
		{
			// His magic attack at you: lands in this many ticks (30 client cycles a tick)
			myFireballs.put(p, client.getTickCount() + (p.getRemainingCycles() + 29) / 30);
		}
		if (!isActive() || !logged.add(p))
		{
			return;
		}
		debug("projectile " + p.getId() + " from " + who(p.getSourceActor()) + " " + tile(p.getSourcePoint())
			+ " to " + (p.getTargetActor() != null ? who(p.getTargetActor()) : tile(p.getTargetPoint()))
			+ ", " + (p.getRemainingCycles() + 29) / 30 + " ticks");
		// 4034 is also his normal magic attack at a player; the corpse ones are aimed at a tile
		if (p.getId() == FIREBALL && p.getTargetActor() == null && p.getTargetPoint() != null)
		{
			fireballs.add(p);
			if (client.getTickCount() >= volleyEndTick && lastAttackTick >= 0)
			{
				// The corpse volley: count down from the "Corpse volley ticks" setting from the first
				// fireball (his repeated volley animations are ignored until it's over)
				volleyEndTick = client.getTickCount() + VOLLEY_TICKS;
				attackTicks = 0;
				volleyStartTick = client.getTickCount();
			}
		}
	}

	@Override
	public void onGraphicsObjectCreated(net.runelite.api.events.GraphicsObjectCreated event)
	{
		if (isActive())
		{
			net.runelite.api.GraphicsObject g = event.getGraphicsObject();
			debug("gfx " + g.getId() + " at " + tile(net.runelite.api.coords.WorldPoint.fromLocal(client, g.getLocation())));
		}
	}

	@Override
	public void onNpcSpawned(net.runelite.api.events.NpcSpawned event)
	{
		super.onNpcSpawned(event);
		NPC npc = event.getNpc();
		if (npc.getId() == net.runelite.api.gameval.NpcID.TFA_XARTH_CORPSE)
		{
			corpseSpawnTicks.put(npc, client.getTickCount());
		}
		if (isActive())
		{
			debug("npc " + npc.getId() + " (" + npc.getName() + ") at " + tile(npc.getWorldLocation()));
		}
	}

	/** Fight events, logged with the burn logger (to line burns up with his attacks). */
	private void debug(String message)
	{
		burnLog(message);
	}

	private static String tile(net.runelite.api.coords.WorldPoint p)
	{
		return p == null ? "?" : p.getX() + "," + p.getY();
	}

	private static String who(net.runelite.api.Actor a)
	{
		if (a == null)
		{
			return "nobody";
		}
		return (a instanceof NPC ? "npc " + ((NPC) a).getId() : a.getName()) + "@" + tile(a.getWorldLocation());
	}

	Color getAttackColor()
	{
		switch (attackAnimation)
		{
			case AnimationID.XARTH_ATTACK_MELEE_01:
				return Color.RED;
			case AnimationID.XARTH_ATTACK_MAGIC_01:
				return Color.CYAN;
			case AnimationID.XARTH_ATTACK_RANGED_01:
				return Color.GREEN;
			default:
				return new Color(255, 115, 0);
		}
	}
}
