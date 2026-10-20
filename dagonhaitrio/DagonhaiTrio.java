package net.runelite.client.plugins.fracturedarchive.dagonhaitrio;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

/**
 * Dagon'hai Trio. Fourth encounter, paired with Xarth.
 */
@Singleton
public class DagonhaiTrio extends Encounter
{
	// Tracks the leader. Related: TFA_DAGONHAI_MINION_1/2 (+ _UNSHIELDED), TFA_DAGONHAI_FOCUS, TFA_DAGONHAI_DEMON, TFA_DAGONHAI_RIFT
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_DAGONHAI_LEADER, NpcID.TFA_DAGONHAI_LEADER_UNSHIELDED);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	@Inject
	private DagonhaiTrioOverlay overlay;

	@Inject
	protected DagonhaiTrio(FracturedArchiveConfig config)
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
