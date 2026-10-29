package net.runelite.client.plugins.fracturedarchive.necromancer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class NecromancerOverlay extends EncounterOverlay
{
	private static final Color BINDER_BORDER = new Color(255, 0, 0, 150);
	private static final Color BINDER_FILL = new Color(255, 0, 0, 25);
	private static final Color COUGH_COLOR = new Color(170, 0, 255);

	@Inject
	private Necromancer encounter;

	@Inject
	protected NecromancerOverlay(FracturedArchiveConfig config)
	{
		super(config);
	}

	@Override
	protected void renderEncounter(Graphics2D graphics)
	{
		if (!encounter.isFightActive())
		{
			return;
		}

		if (config.moiaBinders())
		{
			// The 3x3 around each undead binder
			for (NPC binder : encounter.getBinders())
			{
				LocalPoint lp = binder.getLocalLocation();
				if (lp != null)
				{
					Polygon area = Perspective.getCanvasTileAreaPoly(client, lp, 3);
					renderPoly(graphics, area, BINDER_BORDER, BINDER_FILL, config.borderWidth());
				}
			}
		}

		if (config.moiaMagicCounter())
		{
			// Like Projectile Ticks: her magic projectile aimed at you, with the ticks until it hits
			for (Projectile p : encounter.getMagicProjectiles())
			{
				int cycles = p.getRemainingCycles();
				if (cycles <= 0 || p.getTargetActor() != client.getLocalPlayer())
				{
					continue;
				}
				Point point = Perspective.localToCanvas(client, (int) p.getX(), (int) p.getY(), (int) p.getZ());
				if (point != null)
				{
					renderText(graphics, point, Integer.toString((cycles + 29) / 30), Color.CYAN);
				}
			}
		}

		if (config.moiaCough())
		{
			// Over everyone coughing: ticks until their next cough : coughs left (size 20)
			java.awt.Font font = graphics.getFont();
			graphics.setFont(font.deriveFont(20f));
			for (java.util.Map.Entry<Actor, String> e : encounter.getCoughLabels().entrySet())
			{
				Point point = e.getKey().getCanvasTextLocation(graphics, e.getValue(), config.textZ());
				if (point != null)
				{
					renderText(graphics, point, e.getValue(), COUGH_COLOR);
				}
			}
			graphics.setFont(font);
		}
	}
}
