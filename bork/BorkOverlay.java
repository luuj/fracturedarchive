package net.runelite.client.plugins.fracturedarchive.bork;

import java.awt.Color;
import java.awt.Graphics2D;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Perspective;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.NPC;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class BorkOverlay extends EncounterOverlay
{
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
			int perBomb = config.smorkAttacksPerBomb();
			for (Map.Entry<NPC, Integer> entry : encounter.getSmorkShots().entrySet())
			{
				NPC smork = entry.getKey();
				if (smork.isDead())
				{
					continue;
				}

				int shots = entry.getValue();
				String text = Integer.toString(shots);
				// Red once a bomb is due, otherwise the style of its last shot
				Color color = Color.WHITE;
				Bork.ShotStyle style = encounter.getSmorkStyle(smork);
				if (shots >= perBomb)
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
			int size = config.smorkFireSize();
			for (Projectile bomb : encounter.getFireBombs())
			{
				renderArea(graphics, bomb.getTarget(), size);
			}
			for (GraphicsObject fire : encounter.getFires())
			{
				renderArea(graphics, fire.getLocation(), size);
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

	private void renderArea(Graphics2D graphics, LocalPoint center, int size)
	{
		if (center != null)
		{
			renderPoly(graphics, Perspective.getCanvasTileAreaPoly(client, center, size),
				config.smorkFireBorder(), config.smorkFireFill(), config.borderWidth());
		}
	}
}
