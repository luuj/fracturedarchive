package net.runelite.client.plugins.fracturedarchive.dreadnoth;

import java.awt.Graphics2D;
import java.awt.Polygon;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class DreadnothOverlay extends EncounterOverlay
{
	@Inject
	private Dreadnoth encounter;

	@Inject
	protected DreadnothOverlay(FracturedArchiveConfig config)
	{
		super(config);
	}

	@Override
	protected void renderEncounter(Graphics2D graphics)
	{
		// Drawn whether or not Dreadnoth himself has been picked up yet
		if (config.dreadnothSplatter())
		{
			for (TileObject object : encounter.getSplatter())
			{
				Polygon poly;
				if (object instanceof GameObject)
				{
					GameObject g = (GameObject) object;
					poly = Perspective.getCanvasTileAreaPoly(client, g.getLocalLocation(), Math.max(g.sizeX(), g.sizeY()));
				}
				else
				{
					poly = Perspective.getCanvasTilePoly(client, object.getLocalLocation());
				}
				renderPoly(graphics, poly, config.dreadnothSplatterBorder(), config.dreadnothSplatterFill(), config.borderWidth());
			}
		}

		NPC dreadnoth = encounter.getBoss();
		if (config.dreadnothJumpCounter() && dreadnoth != null && encounter.getJumpTicks() > 0)
		{
			String text = Integer.toString(encounter.getJumpTicks());
			renderText(graphics, dreadnoth.getCanvasTextLocation(graphics, text, config.textZ()), text, config.dreadnothJumpColor());
		}

		if (config.dreadnothIncoming())
		{
			for (LocalPoint target : encounter.getIncoming())
			{
				renderPoly(graphics, Perspective.getCanvasTilePoly(client, target), config.dreadnothIncomingColor(), null, config.borderWidth());
			}
		}
	}
}
