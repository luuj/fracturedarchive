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
 * Highlights the true tile of the player each boss is going after (red by default), and of the
 * player each Smork has picked for its fire bomb (blue by default), until the bomb comes out.
 */
@Singleton
class TargetOverlay extends EncounterOverlay
{
	/** Boss targets red, Smork targets blue. */
	private static final Color BOSS_TARGET_COLOR = Color.RED;
	private static final Color SMORK_TARGET_COLOR = Color.BLUE;

	private final List<Encounter> encounters;
	private final Bork bork;

	@Inject
	TargetOverlay(FracturedArchiveConfig config, ArchiveGuardian guardian, Bork bork, Ezekiel ezekiel,
		Dreadnoth dreadnoth, Necromancer necromancer, Xarth xarth, DagonhaiTrio dagonhaiTrio, FinalBoss finalBoss)
	{
		super(config);
		this.bork = bork;
		encounters = List.of(guardian, bork, ezekiel, dreadnoth, necromancer, xarth, dagonhaiTrio, finalBoss);
	}

	@Override
	protected void renderEncounter(Graphics2D graphics)
	{
		if (!config.highlightTargets())
		{
			return;
		}

		// Smorks first, so a player both Bork and a Smork are on shows the boss's color.
		// Each Smork's pick stays highlighted until its fire bomb comes out.
		for (Player player : bork.getSmorkTargets().values())
		{
			highlight(graphics, player, SMORK_TARGET_COLOR);
		}

		for (Encounter encounter : encounters)
		{
			highlight(graphics, encounter.getTarget(), BOSS_TARGET_COLOR);
		}
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
