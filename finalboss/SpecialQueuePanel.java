package net.runelite.client.plugins.fracturedarchive.finalboss;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Ocellus's next two specials in phase 3, in a box above the chat box (like Moia's cough box).
 * The box turns red whenever the prayer turn-off is next (don't push him to the next quadrant yet). Hidden in the last quadrant,
 * where he doesn't do specials.
 */
@Singleton
public class SpecialQueuePanel extends OverlayPanel
{
	private static final Color WARNING_BACKGROUND = new Color(150, 0, 0, 200);

	// Field-injected like the overlay (FinalBoss injects this panel too)
	@Inject
	private FinalBoss encounter;

	private final FracturedArchiveConfig config;

	@Inject
	SpecialQueuePanel(FracturedArchiveConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.BOTTOM_LEFT);
		panelComponent.setPreferredSize(new Dimension(130, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.ocellusSpecialQueue() || encounter.getBoss() == null
			|| encounter.getBoss().getId() != NpcID.TFA_OCELLUS_P3 || encounter.isLastQuadrant())
		{
			return null;
		}

		String orange = encounter.getNextOrangeSpecial();
		boolean prayerFirst = encounter.isPrayerNext();
		String first = prayerFirst ? "Prayer" : orange;
		String second = prayerFirst ? orange : "Prayer";
		// Red whenever the prayer turn-off is next: pushing him to the next quadrant now means
		// Prayer then Insolence there (the orange after a move is always Insolence)
		boolean warning = prayerFirst;

		panelComponent.setBackgroundColor(warning ? WARNING_BACKGROUND : net.runelite.client.ui.overlay.components.ComponentConstants.STANDARD_BACKGROUND_COLOR);
		panelComponent.getChildren().add(TitleComponent.builder().text("Specials").color(Color.WHITE).build());
		panelComponent.getChildren().add(line("Next:", first));
		panelComponent.getChildren().add(line("Then:", second));
		return super.render(graphics);
	}

	private static LineComponent line(String left, String special)
	{
		return LineComponent.builder()
			.left(left)
			.right(special)
			.rightColor("Prayer".equals(special) ? FinalBoss.PRAYER_COLOR : FinalBoss.SPECIAL_COLOR)
			.build();
	}
}
