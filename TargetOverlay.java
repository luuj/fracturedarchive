package net.runelite.client.plugins.fracturedarchive;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Player;
import net.runelite.client.plugins.fracturedarchive.archiveguardian.ArchiveGuardian;
import net.runelite.client.plugins.fracturedarchive.bork.Bork;
import net.runelite.client.plugins.fracturedarchive.dagonhaitrio.DagonhaiTrio;
import net.runelite.client.plugins.fracturedarchive.dreadnoth.Dreadnoth;
import net.runelite.client.plugins.fracturedarchive.ezekiel.Ezekiel;
import net.runelite.client.plugins.fracturedarchive.finalboss.FinalBoss;
import net.runelite.client.plugins.fracturedarchive.necromancer.Necromancer;
import net.runelite.client.plugins.fracturedarchive.xarth.Xarth;

/**
 * Highlights the true tile of the player each boss is going after (the tank), in pink with
 * "Tank" on it.
 */
@Singleton
class TargetOverlay extends EncounterOverlay
{
	/** Boss targets pink. */
	private static final Color BOSS_TARGET_COLOR = new Color(255, 105, 180);

	private final List<Encounter> encounters;

	@Inject
	TargetOverlay(FracturedArchiveConfig config, ArchiveGuardian guardian, Bork bork, Ezekiel ezekiel,
		Dreadnoth dreadnoth, Necromancer necromancer, Xarth xarth, DagonhaiTrio dagonhaiTrio, FinalBoss finalBoss)
	{
		super(config);
		encounters = List.of(guardian, bork, ezekiel, dreadnoth, necromancer, xarth, dagonhaiTrio, finalBoss);
	}

	@Override
	protected void renderEncounter(Graphics2D graphics)
	{
		if (!config.highlightTargets())
		{
			return;
		}

		for (Encounter encounter : encounters)
		{
			Player tank = encounter.getTarget();
			highlight(graphics, tank, BOSS_TARGET_COLOR);
			label(graphics, tank, "Tank", BOSS_TARGET_COLOR);
		}
	}

	/** A word on the player's true tile, at size 12. */
	private void label(Graphics2D graphics, Player player, String text, Color color)
	{
		if (player == null)
		{
			return;
		}
		net.runelite.api.coords.LocalPoint lp = net.runelite.api.coords.LocalPoint.fromWorld(client, player.getWorldLocation());
		if (lp == null)
		{
			return;
		}
		java.awt.Font font = graphics.getFont();
		graphics.setFont(font.deriveFont(12f));
		net.runelite.api.Point point = net.runelite.api.Perspective.getCanvasTextLocation(client, graphics, lp, text, 0);
		if (point != null)
		{
			renderText(graphics, point, text, color);
		}
		graphics.setFont(font);
	}

	/** The true (server-side) tile, not where the player model is drawn mid-walk. */
	private void highlight(Graphics2D graphics, Player player, Color color)
	{
		if (player != null)
		{
			renderTile(graphics, player.getWorldLocation(), color,
				new Color(color.getRed(), color.getGreen(), color.getBlue(), 40), config.borderWidth());
		}
	}
}
