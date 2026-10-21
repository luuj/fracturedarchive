package net.runelite.client.plugins.fracturedarchive.archiveguardian;

import java.awt.Color;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class ArchiveGuardianOverlay extends EncounterOverlay
{
	/** Tiles marked on each side of the mushroom. */
	private static final int MUSHROOM_RADIUS = 1;

	/** Mushroom area colors (the colors you had saved). */
	private static final Color MUSHROOM_BORDER = new Color(255, 0, 0, 100);
	private static final Color MUSHROOM_FILL = new Color(255, 0, 0, 17);

	@Inject
	private ArchiveGuardian encounter;

	@Inject
	protected ArchiveGuardianOverlay(FracturedArchiveConfig config)
	{
		super(config);
	}

	@Override
	protected void renderEncounter(Graphics2D graphics)
	{
		if (config.guardianMushrooms())
		{
			for (GameObject mushroom : encounter.getMushrooms())
			{
				// The mushroom's footprint plus 1 tile on every side (3x3 for a 1x1 mushroom)
				int size = Math.max(mushroom.sizeX(), mushroom.sizeY()) + 2 * MUSHROOM_RADIUS;
				renderPoly(graphics, Perspective.getCanvasTileAreaPoly(client, mushroom.getLocalLocation(), size),
					MUSHROOM_BORDER, MUSHROOM_FILL, config.borderWidth());
			}
		}

		NPC guardian = encounter.getCounterNpc();
		if (config.guardianAttackTicks() && guardian != null)
		{
			String text = Integer.toString(encounter.getTicksRemaining());
			renderText(graphics, guardian.getCanvasTextLocation(graphics, text, config.textZ()), text, encounter.getCounterColor());
		}

		if (!encounter.isActive())
		{
			return;
		}

		// Other Archive Guardian overlays go here.
	}
}
