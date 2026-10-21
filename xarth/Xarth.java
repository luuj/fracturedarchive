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
	}

	@Override
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!(event.getActor() instanceof NPC) || !bossIds().contains(((NPC) event.getActor()).getId()))
		{
			return;
		}

		int animation = event.getActor().getAnimation();
		if (animation == AnimationID.XARTH_ATTACK_MELEE_01
			|| animation == AnimationID.XARTH_ATTACK_MAGIC_01
			|| animation == AnimationID.XARTH_ATTACK_RANGED_01
			|| SPECIAL_ATTACKS.contains(animation))
		{
			attackAnimation = animation;
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
				return Color.YELLOW;
		}
	}
}
