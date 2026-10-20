package net.runelite.client.plugins.fracturedarchive.finalboss;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

/**
 * Final boss. Always the last encounter. Identity not yet revealed.
 */
@Singleton
public class FinalBoss extends Encounter
{
	// The final boss is Ocellus (TFA_OCELLUS_P1-P3) in the game data. Related: TFA_OCELLUS_P3_BOMBPORTAL_BASIC/HEAL/BUFF, TFA_OCELLUS_P3_VOID_VINE
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_OCELLUS_P1, NpcID.TFA_OCELLUS_P2, NpcID.TFA_OCELLUS_P3);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	@Inject
	private FinalBossOverlay overlay;

	@Inject
	protected FinalBoss(FracturedArchiveConfig config)
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
		// Clear this encounter's own state here.
	}
}
