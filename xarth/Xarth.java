package net.runelite.client.plugins.fracturedarchive.xarth;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
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
		// Clear this encounter's own state here.
	}
}
