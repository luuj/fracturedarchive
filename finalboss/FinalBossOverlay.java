package net.runelite.client.plugins.fracturedarchive.finalboss;

import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class FinalBossOverlay extends EncounterOverlay
{
	@Inject
	private FinalBoss encounter;

	@Inject
	protected FinalBossOverlay(FracturedArchiveConfig config)
	{
		super(config);
	}

	@Override
	protected void renderEncounter(Graphics2D graphics)
	{
		if (!encounter.isActive())
		{
			return;
		}

		// Draw this encounter's overlays here.
	}
}
