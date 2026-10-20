package net.runelite.client.plugins.fracturedarchive.xarth;

import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class XarthOverlay extends EncounterOverlay
{
	@Inject
	private Xarth encounter;

	@Inject
	protected XarthOverlay(FracturedArchiveConfig config)
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
