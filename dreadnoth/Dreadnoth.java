package net.runelite.client.plugins.fracturedarchive.dreadnoth;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

/**
 * Dreadnoth. Third encounter, paired with the Necromancer. Dreadnoth the Jellied.
 */
@Singleton
public class Dreadnoth extends Encounter
{
	// Dreadnoth the Jellied is TFA_JELLY_BOSS in the game data. Related: TFA_JELLY_MINION
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_JELLY_BOSS);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	@Inject
	private DreadnothOverlay overlay;

	@Inject
	protected Dreadnoth(FracturedArchiveConfig config)
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
