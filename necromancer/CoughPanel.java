package net.runelite.client.plugins.fracturedarchive.necromancer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Moia's cough as a box above the chat box (like Spell Reminder): one line per person coughing,
 * "name: coughs left". You first, then everyone else; binders aren't listed. Hidden when nobody's
 * coughing. It can be dragged elsewhere like any other overlay (alt + drag).
 */
@Singleton
public class CoughPanel extends OverlayPanel
{
	private static final Color COUGH_COLOR = Color.YELLOW;

	// Field-injected like NecromancerOverlay (Necromancer injects this panel too)
	@Inject
	private Necromancer encounter;

	@Inject
	private Client client;

	private final FracturedArchiveConfig config;

	@Inject
	CoughPanel(FracturedArchiveConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.BOTTOM_LEFT);
		panelComponent.setPreferredSize(new Dimension(150, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.moiaCough() || !encounter.isFightActive())
		{
			return null;
		}

		List<Map.Entry<Actor, String>> lines = new ArrayList<>();
		for (Map.Entry<Actor, String> e : encounter.getCoughLabels().entrySet())
		{
			if (!(e.getKey() instanceof NPC) && e.getKey().getName() != null)
			{
				lines.add(e);
			}
		}
		if (lines.isEmpty())
		{
			return null;
		}
		// You first, then by name
		lines.sort((a, b) -> a.getKey() == client.getLocalPlayer() ? -1 : b.getKey() == client.getLocalPlayer() ? 1
			: a.getKey().getName().compareToIgnoreCase(b.getKey().getName()));

		panelComponent.getChildren().add(TitleComponent.builder().text("Cough").color(COUGH_COLOR).build());
		for (Map.Entry<Actor, String> e : lines)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(e.getKey().getName().replace(' ', ' '))
				.right(e.getValue())
				.rightColor(COUGH_COLOR)
				.build());
		}
		return super.render(graphics);
	}
}
