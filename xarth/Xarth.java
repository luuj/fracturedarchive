package net.runelite.client.plugins.fracturedarchive.xarth;

import java.awt.Color;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
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
	@Getter
	private int attackTicks;
	private int attackAnimation = -1;

	@Inject
	private XarthOverlay overlay;

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
				// volley: show nothing until the first fireball starts its count
				attackTicks = 0;
				return;
			}
			// +1 so the first number shown is the full attack speed
			attackTicks = ATTACK_SPEED + 1;
		}
	}

	@Override
	public void onGameTick(GameTick event)
	{
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

	Set<net.runelite.api.Projectile> getFireballs()
	{
		return java.util.Collections.unmodifiableSet(fireballs);
	}

	@Override
	public void onProjectileMoved(net.runelite.api.events.ProjectileMoved event)
	{
		net.runelite.api.Projectile p = event.getProjectile();
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
				// The corpse volley: count down from the "Corpse volley ticks" setting from the first fireball (his repeated volley
				// animations are ignored until it's over). +1 like the other counts, for the tick
				// that's taken off before it's first shown.
				volleyEndTick = client.getTickCount() + VOLLEY_TICKS;
				attackTicks = config.xarthVolleyTicks() + 1;
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
		if (isActive())
		{
			debug("npc " + npc.getId() + " (" + npc.getName() + ") at " + tile(npc.getWorldLocation()));
		}
	}

	/** Logging is off (kept as a no-op so it's easy to turn back on). */
	private void debug(String message)
	{
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
