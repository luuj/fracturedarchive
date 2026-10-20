package net.runelite.client.plugins.fracturedarchive.bork;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class BorkOverlay extends EncounterOverlay
{
	/** The bomb comes after 5 or 6 normal shots, so warn from the 5th. */
	private static final int BOMB_WARNING_SHOTS = 5;

	@Inject
	private Bork encounter;

	@Inject
	protected BorkOverlay(FracturedArchiveConfig config)
	{
		super(config);
	}

	@Override
	protected void renderEncounter(Graphics2D graphics)
	{
		if (config.smorkAttackCounter())
		{
			for (Map.Entry<NPC, Integer> entry : encounter.getSmorkShots().entrySet())
			{
				NPC smork = entry.getKey();
				if (smork.isDead())
				{
					continue;
				}

				int shots = entry.getValue();
				String text = Integer.toString(shots);
				// Red at 5 or more, since the bomb can come after the 5th or 6th shot; otherwise the style of its last shot
				Color color = Color.WHITE;
				Bork.ShotStyle style = encounter.getSmorkStyle(smork);
				if (shots >= BOMB_WARNING_SHOTS)
				{
					color = Color.RED;
				}
				else if (config.smorkStyleColors() && style != null)
				{
					color = style == Bork.ShotStyle.MAGIC ? config.smorkMagicColor() : config.smorkRangedColor();
				}
				renderText(graphics, smork.getCanvasTextLocation(graphics, text, config.textZ()), text, color);
			}
		}

		if (config.smorkFireBombs())
		{
			for (Projectile bomb : encounter.getFireBombs())
			{
				// Where it will land, fixed when it's thrown, plus the ticks until it gets there
				WorldPoint landing = bomb.getTargetPoint();
				LocalPoint lp = landing != null ? LocalPoint.fromWorld(client, landing) : bomb.getTarget();
				renderArea(graphics, lp);
				int cycles = bomb.getRemainingCycles();
				if (lp != null && cycles > 0)
				{
					// 30 client cycles (20 ms each) per game tick, rounded up
					String text = Integer.toString((cycles + 29) / 30);
					renderText(graphics, Perspective.getCanvasTextLocation(client, graphics, lp, text, 0), text, config.smorkFireBorder());
				}
			}
			for (GraphicsObject fire : encounter.getFires())
			{
				renderArea(graphics, fire.getLocation());
			}
		}

		// Only one counter above Bork at a time: the stun while he's stunned, otherwise his attack
		NPC bork = encounter.getStunnedBork();
		if (config.borkStunCounter() && bork != null)
		{
			String text = Integer.toString(encounter.getStunTicks());
			renderText(graphics, bork.getCanvasTextLocation(graphics, text, config.textZ()), text, config.borkStunColor());
		}
		else if (config.borkAttackCounter() && (bork = encounter.getAttackingBork()) != null)
		{
			String text = Integer.toString(encounter.getAttackTicks());
			renderText(graphics, bork.getCanvasTextLocation(graphics, text, config.textZ()), text, config.borkAttackColor());
		}

		if (!encounter.isActive())
		{
			return;
		}

		// Other Bork overlays go here.
	}

	private void renderArea(Graphics2D graphics, LocalPoint center)
	{
		if (center != null)
		{
			renderPoly(graphics, Perspective.getCanvasTilePoly(client, center),
				config.smorkFireBorder(), config.smorkFireFill(), config.borderWidth());
		}
	}
}
