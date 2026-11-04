package net.runelite.client.plugins.fracturedarchive.ezekiel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.ParamID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * A reminder box above the chat box (like Moia's cough box): on the standard spellbook, when your
 * autocast isn't the surge for the room you're in: Wind Surge for Ezekiel, Water Surge for Xarth,
 * Earth Surge for Ocellus. It goes by the autocast last set on your harmonised nightmare staff
 * (remembered while you wield something else), so it shows whatever weapon you're holding. Nothing
 * until the staff has been wielded once since the client started. (One box for all three; it lives here
 * because Ezekiel was first.)
 */
@Singleton
public class AutocastPanel extends OverlayPanel
{
	/** Autocast slot (varbit) -> the spell's "item", which carries its name. */
	private static final int AUTOCAST_SPELLS_ENUM = 1986;
	/** The boss rooms' map regions (behind the instance). */
	private static final int EZEKIEL_REGION = 16214;
	private static final int XARTH_REGION = 16726;
	private static final int OCELLUS_REGION = 16472;

	private static final Color FLASH_BACKGROUND = new Color(200, 0, 0, 220);
	/** VarbitID.SPELLBOOK: 0 = standard. */
	private static final int STANDARD_SPELLBOOK = 0;

	@Inject
	private Ezekiel ezekiel;


	@Inject
	private Client client;

	private final FracturedArchiveConfig config;

	/** The room's fight has started ("Challenge started"): no reminder until you leave the room, even after the boss dies. */
	private boolean fightStarted;

	/** Chat, passed on by Ezekiel (which gets every message). */
	void onChatMessage(String message)
	{
		if (message == null)
		{
			return;
		}
		// Stays hidden after the boss dies too, until you leave the room (see render)
		if (message.contains("Challenge started:"))
		{
			fightStarted = true;
		}
	}

	/** The autocast last seen on the harmonised staff ("None" if none), or null if not seen yet. */
	private String harmAutocast;

	@Inject
	AutocastPanel(FracturedArchiveConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.BOTTOM_LEFT);
		panelComponent.setPreferredSize(new Dimension(150, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (harmStaffEquipped())
		{
			// Remember the staff's autocast for when you swap to another weapon
			String spell = autocastSpell();
			harmAutocast = spell == null ? "None" : spell;
		}

		String wanted = wantedSpell();
		if (wanted == null)
		{
			// Out of the rooms: the next fight hasn't started
			fightStarted = false;
		}
		String spell = harmAutocast;
		if (wanted == null || fightStarted || spell == null || client.getVarbitValue(VarbitID.SPELLBOOK) != STANDARD_SPELLBOOK
			|| wanted.equalsIgnoreCase(spell))
		{
			return null;
		}

		// Flash: red background for half a second, normal for half a second
		boolean flashOn = (System.currentTimeMillis() / 500) % 2 == 0;
		panelComponent.setBackgroundColor(flashOn ? FLASH_BACKGROUND
			: net.runelite.client.ui.overlay.components.ComponentConstants.STANDARD_BACKGROUND_COLOR);
		panelComponent.getChildren().add(TitleComponent.builder().text("Autocast " + wanted)
			.color(flashOn ? Color.WHITE : Color.RED).build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Staff:")
			.right(spell)
			.rightColor(flashOn ? Color.WHITE : Color.RED)
			.build());
		return super.render(graphics);
	}

	/** Whether the weapon you're wielding is a harmonised nightmare staff. */
	private boolean harmStaffEquipped()
	{
		net.runelite.api.ItemContainer worn = client.getItemContainer(net.runelite.api.gameval.InventoryID.WORN);
		if (worn == null)
		{
			return false;
		}
		net.runelite.api.Item weapon = worn.getItem(net.runelite.api.EquipmentInventorySlot.WEAPON.getSlotIdx());
		return weapon != null && weapon.getId() > 0
			&& "Harmonised nightmare staff".equalsIgnoreCase(client.getItemDefinition(weapon.getId()).getName());
	}

	/** The surge for the room you're in (if its reminder is on), or null. */
	private String wantedSpell()
	{
		// Which boss room you're in, by the real map region behind the instance (the same every
		// raid, from the room logs). You wait in these rooms before the fight starts; the bosses
		// themselves only load in just before (Ocellus as a different NPC until the fight).
		net.runelite.api.Player me = client.getLocalPlayer();
		if (me == null || client.getTopLevelWorldView() == null || !client.getTopLevelWorldView().isInstance())
		{
			return null;
		}
		int region = net.runelite.api.coords.WorldPoint.fromLocalInstance(client, me.getLocalLocation()).getRegionID();
		if (config.ezekielAutocastReminder() && region == EZEKIEL_REGION)
		{
			return "Wind Surge";
		}
		if (config.xarthAutocastReminder() && region == XARTH_REGION)
		{
			return "Water Surge";
		}
		if (config.ocellusAutocastReminder() && region == OCELLUS_REGION)
		{
			return "Earth Surge";
		}
		return null;
	}

	/** The name of the spell you're set to autocast, or null if none. */
	private String autocastSpell()
	{
		int slot = client.getVarbitValue(VarbitID.AUTOCAST_SPELL);
		if (slot <= 0)
		{
			return null;
		}
		int spellItem = client.getEnum(AUTOCAST_SPELLS_ENUM).getIntValue(slot);
		if (spellItem <= 0)
		{
			return null;
		}
		String name = client.getItemDefinition(spellItem).getStringValue(ParamID.SPELL_NAME);
		return name == null || name.isEmpty() ? null : name;
	}
}
