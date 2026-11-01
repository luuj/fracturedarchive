package net.runelite.client.plugins.fracturedarchive.necromancer;

import java.awt.Color;
import java.awt.Graphics2D;
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
	private static final Color BINDER_BORDER = new Color(255, 255, 255, 180);
	private static final Color BINDER_FILL = new Color(255, 255, 255, 25);
	private static final Color SPOT_COLOR = new Color(200, 200, 200, 160);
	private static final Color CENTER_BORDER = new Color(0, 255, 0, 150);
	private static final Color CENTER_FILL = new Color(0, 255, 0, 25);
	private static final Color COUGH_COLOR = Color.YELLOW;
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
			// Each binder's own tile: white, or green for the one on the centre spot of their grid
			// (when three are in a row down the middle)
			net.runelite.api.coords.WorldPoint center = encounter.getBinderGridCenter();
			for (NPC binder : encounter.getBinders())
			{
				net.runelite.api.coords.WorldPoint wp = binder.getWorldLocation();
				boolean middle = center != null && center.equals(wp);
				renderTile(graphics, wp, middle ? CENTER_BORDER : BINDER_BORDER, middle ? CENTER_FILL : BINDER_FILL,
					config.borderWidth());
			}
		}

		if (config.moiaBinderSpots())
		{
			// The spots binders stand on, numbered in the order they walk them (size 12)
			java.util.List<net.runelite.api.coords.WorldPoint> spots = encounter.getBinderSpots();
			java.awt.Font font = graphics.getFont();
			graphics.setFont(font.deriveFont(12f));
			for (int i = 0; i < spots.size(); i++)
			{
				LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), spots.get(i));
				if (lp == null)
				{
					continue;
				}
				renderPoly(graphics, Perspective.getCanvasTilePoly(client, lp), SPOT_COLOR, null, 1);
				String text = Integer.toString(i + 1);
				Point point = Perspective.getCanvasTextLocation(client, graphics, lp, text, 0);
				if (point != null)
				{
					renderText(graphics, point, text, SPOT_COLOR);
				}
			}
			graphics.setFont(font);
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
			// Everyone coughing: their tile, so you can keep away from them (the coughs left are in
			// the box above the chat box)
			for (Actor actor : encounter.getCoughLabels().keySet())
			{
				renderTile(graphics, actor.getWorldLocation(), COUGH_COLOR,
					new Color(COUGH_COLOR.getRed(), COUGH_COLOR.getGreen(), COUGH_COLOR.getBlue(), 50), config.borderWidth());
			}
		}
	}
}
