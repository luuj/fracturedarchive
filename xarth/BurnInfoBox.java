package net.runelite.client.plugins.fracturedarchive.xarth;

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;
import net.runelite.client.ui.overlay.infobox.InfoBox;

/**
 * Xarth's burn on you as an info box (like Spawn Counter): the burn damage still to come if no
 * more burns land. Only shown while you're burning.
 */
class BurnInfoBox extends InfoBox
{
	private static final Color BURN_COLOR = new Color(255, 115, 0);

	private final Xarth encounter;
	private final FracturedArchiveConfig config;

	BurnInfoBox(BufferedImage image, Plugin plugin, Xarth encounter, FracturedArchiveConfig config)
	{
		super(image, plugin);
		this.encounter = encounter;
		this.config = config;
	}

	@Override
	public boolean render()
	{
		return config.xarthBurnRemaining() && encounter.getBurnRemaining() > 0;
	}

	@Override
	public String getText()
	{
		return Integer.toString(encounter.getBurnRemaining());
	}

	@Override
	public Color getTextColor()
	{
		return BURN_COLOR;
	}

	@Override
	public String getTooltip()
	{
		return "Burn damage left: " + encounter.getBurnRemaining();
	}
}
