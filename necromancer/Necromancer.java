package net.runelite.client.plugins.fracturedarchive.necromancer;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

/**
 * Necromancer. Third encounter, paired with Dreadnoth. Name not yet revealed.
 */
@Singleton
public class Necromancer extends Encounter
{
	// The necromancer is Moia (TFA_MOIA) in the game data. Related: TFA_MOIA_MINION
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_MOIA);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	@Inject
	private NecromancerOverlay overlay;

	@Inject
	protected Necromancer(FracturedArchiveConfig config)
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
