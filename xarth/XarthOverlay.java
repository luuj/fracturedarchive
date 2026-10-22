package net.runelite.client.plugins.fracturedarchive.xarth;

import java.awt.Color;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class XarthOverlay extends EncounterOverlay
{
	private static final Color FIREBALL_BORDER = new Color(255, 120, 0, 220);
	private static final Color FIREBALL_FILL = new Color(255, 120, 0, 50);

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
		if (config.xarthFireballs())
		{
			// Where each fireball lands (that's where a corpse spawns), with the ticks until it does
			for (Projectile fireball : encounter.getFireballs())
			{
				LocalPoint lp = LocalPoint.fromWorld(client, fireball.getTargetPoint());
				int cycles = fireball.getRemainingCycles();
				if (lp == null || cycles <= 0)
				{
					continue;
				}
				renderPoly(graphics, Perspective.getCanvasTilePoly(client, lp), FIREBALL_BORDER, FIREBALL_FILL, config.borderWidth());
				String text = Integer.toString((cycles + 29) / 30);
				renderText(graphics, Perspective.getCanvasTextLocation(client, graphics, lp, text, 0), text, FIREBALL_BORDER);
			}
		}

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
