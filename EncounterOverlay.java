package net.runelite.client.plugins.fracturedarchive;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Base class for an encounter's overlay, like Theatre's RoomOverlay. It sets the shared font
 * and provides the same tile and text drawing as the other [J] plugins (fill, then border,
 * then text with OverlayUtil's 1px shadow). Subclasses implement renderEncounter().
 */
public abstract class EncounterOverlay extends Overlay
{
	protected final FracturedArchiveConfig config;

	@Inject
	protected Client client;

	protected EncounterOverlay(FracturedArchiveConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public final Dimension render(Graphics2D graphics)
	{
		// Set the font before any text is measured or positioned, so every overlay uses the same one.
		graphics.setFont(config.font().getFont());
		renderEncounter(graphics);
		return null;
	}

	protected abstract void renderEncounter(Graphics2D graphics);

	/** Fills and outlines a single tile. */
	protected void renderTile(Graphics2D graphics, WorldPoint point, Color border, Color fill, int borderWidth)
	{
		if (point == null)
		{
			return;
		}

		LocalPoint lp = LocalPoint.fromWorld(client, point);
		if (lp != null)
		{
			renderPoly(graphics, Perspective.getCanvasTilePoly(client, lp), border, fill, borderWidth);
		}
	}

	/** Fills and outlines a polygon. Either color can be null to skip that part. */
	protected void renderPoly(Graphics2D graphics, Polygon poly, Color border, Color fill, int borderWidth)
	{
		if (poly == null)
		{
			return;
		}

		if (fill != null)
		{
			graphics.setColor(fill);
			graphics.fill(poly);
		}

		if (border != null)
		{
			graphics.setColor(border);
			graphics.setStroke(new BasicStroke(borderWidth));
			graphics.draw(poly);
		}
	}

	/**
	 * Draws a small count stacked directly under an actor's tick counter (like Theatre), at the
	 * given font size, centered under where the counter sits.
	 */
	protected void renderStackedText(Graphics2D graphics, net.runelite.api.Actor actor, String text, Color color, float size)
	{
		Point anchor = actor.getCanvasTextLocation(graphics, text, config.textZ());
		if (anchor == null)
		{
			return;
		}

		java.awt.Font mainFont = graphics.getFont();
		int mainWidth = graphics.getFontMetrics().stringWidth(text);
		int mainHeight = graphics.getFontMetrics().getHeight();
		graphics.setFont(mainFont.deriveFont(size));
		int smallWidth = graphics.getFontMetrics().stringWidth(text);
		renderText(graphics, new Point(anchor.getX() + (mainWidth - smallWidth) / 2, anchor.getY() + mainHeight), text, color);
		graphics.setFont(mainFont);
	}

	/** Draws text at a canvas point with the standard 1px shadow. */
	protected void renderText(Graphics2D graphics, Point canvasPoint, String text, Color color)
	{
		if (canvasPoint != null)
		{
			OverlayUtil.renderTextLocation(graphics, canvasPoint, text, color);
		}
	}
}
