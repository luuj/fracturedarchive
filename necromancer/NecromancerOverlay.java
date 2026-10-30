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
	private static final Color CENTER_BORDER = new Color(0, 255, 0, 150);
	private static final Color CENTER_FILL = new Color(0, 255, 0, 25);
	private static final Color COUGH_COLOR = new Color(170, 0, 255);
	private static final Color ENRAGE_COLOR = new Color(255, 115, 0);

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
			// The 3x3 around each undead binder: green for one on the centre spot of their grid
			// (only when three are in a row down the middle), red otherwise
			net.runelite.api.coords.WorldPoint center = encounter.getBinderGridCenter();
			for (NPC binder : encounter.getBinders())
			{
				LocalPoint lp = binder.getLocalLocation();
				if (lp != null)
				{
					boolean middle = center != null && center.equals(binder.getWorldLocation());
					Polygon area = Perspective.getCanvasTileAreaPoly(client, lp, 3);
					renderPoly(graphics, area, middle ? CENTER_BORDER : BINDER_BORDER, middle ? CENTER_FILL : BINDER_FILL, config.borderWidth());
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

		Boolean coughNext = encounter.isCoughNext();
		NPC boss = encounter.getBoss();
		if (config.moiaNextSpecial() && coughNext != null && boss != null)
		{
			// Her next special, under where her counters go (size 12): magic (cyan) or cough (purple)
			renderStackedText(graphics, boss, coughNext ? "Cough" : "Magic", coughNext ? COUGH_COLOR : Color.CYAN, 12f);
		}

		int charge = encounter.getChargeTicks();
		NPC moia = encounter.getBoss();
		if (config.moiaEnrage() && charge > 0 && moia != null)
		{
			// Over Moia: ticks until her enrage charge finishes (kill the binders before it does)
			String text = Integer.toString(charge);
			Point point = moia.getCanvasTextLocation(graphics, text, config.textZ());
			if (point != null)
			{
				renderText(graphics, point, text, ENRAGE_COLOR);
			}
		}

		if (config.moiaCough())
		{
			// Over everyone coughing: how many coughs they have left (size 20)
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
