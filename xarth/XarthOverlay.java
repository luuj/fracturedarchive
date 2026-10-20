package net.runelite.client.plugins.fracturedarchive.xarth;

import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.NPC;
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
		NPC xarth = encounter.getBoss();
		if (xarth == null)
		{
			return;
		}

		if (config.xarthAttackCounter() && encounter.getAttackTicks() > 0)
		{
			String text = Integer.toString(encounter.getAttackTicks());
			renderText(graphics, xarth.getCanvasTextLocation(graphics, text, config.textZ()), text, encounter.getAttackColor());
		}
	}
}
