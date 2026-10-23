package net.runelite.client.plugins.fracturedarchive.dreadnoth;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class DreadnothOverlay extends EncounterOverlay
{
	private static final int CYCLES_PER_TICK = 30;

	/** Colors (the colors you had saved). */
	private static final Color SPLATTER_BORDER = new Color(244, 23, 165);
	private static final Color SPLATTER_FILL = new Color(180, 0, 0, 0);
	private static final Color INCOMING_COLOR = new Color(31, 236, 101);
	private static final Color JUMP_COLOR = new Color(255, 200, 0);
	private static final Color COOLDOWN_COLOR = Color.CYAN;
	private static final Color RANGED_COLOR = Color.GREEN;

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
				renderPoly(graphics, poly, SPLATTER_BORDER, SPLATTER_FILL, config.borderWidth());
			}
		}

		if (config.dreadnothRangedProjectiles())
		{
			for (Projectile shot : encounter.getRangedShots())
			{
				renderShot(graphics, shot);
			}
		}

		NPC dreadnoth = encounter.getBoss();
		if (config.dreadnothJumpCounter() && dreadnoth != null && encounter.getJumpTicks() > 0)
		{
			String text = Integer.toString(encounter.getJumpTicks());
			renderText(graphics, dreadnoth.getCanvasTextLocation(graphics, text, config.textZ()), text, JUMP_COLOR);
		}
		else if (config.dreadnothJumpCounter() && dreadnoth != null && encounter.getCooldownTicks() > 0)
		{
			// The jump cooldown after a fast jump, in cyan
			String text = Integer.toString(encounter.getCooldownTicks());
			renderText(graphics, dreadnoth.getCanvasTextLocation(graphics, text, config.textZ()), text, COOLDOWN_COLOR);
		}

		if (config.dreadnothIncoming())
		{
			for (Map.Entry<Projectile, LocalPoint> entry : encounter.getIncoming().entrySet())
			{
				LocalPoint target = entry.getValue();
				renderPoly(graphics, Perspective.getCanvasTilePoly(client, target), INCOMING_COLOR, null, config.borderWidth());

				int cycles = entry.getKey().getRemainingCycles();
				if (config.dreadnothIncomingTicks() && cycles > 0)
				{
					// 30 client cycles (20 ms each) per game tick, rounded up
					String text = Integer.toString((cycles + CYCLES_PER_TICK - 1) / CYCLES_PER_TICK);
					renderText(graphics, Perspective.getCanvasTextLocation(client, graphics, target, text, 0), text, INCOMING_COLOR);
				}
			}
		}
	}

	/**
	 * Like Projectile Ticks: only when it's aimed at you, the ticks until it hits drawn on the
	 * projectile itself.
	 */
	private void renderShot(Graphics2D graphics, Projectile shot)
	{
		int cycles = shot.getRemainingCycles();
		if (cycles <= 0 || shot.getTargetActor() != client.getLocalPlayer())
		{
			return;
		}

		Point p = Perspective.localToCanvas(client, (int) shot.getX(), (int) shot.getY(), (int) shot.getZ());
		if (p != null)
		{
			renderText(graphics, p, Integer.toString(cycles / CYCLES_PER_TICK), RANGED_COLOR);
		}
	}
}
