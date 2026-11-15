package net.runelite.client.plugins.fracturedarchive;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * "Use Divine Potion": a flashing red box above the chat box (like the surge reminder) the moment
 * a room's boss hits 0 hp, so you can drink before your stats are restored at the end of the room.
 * Every boss but the Archive Guardian and Ocellus. Where a room has more than one (Bork and his
 * Smorks, the Dagon'hai Trio), all of them have to be at 0 hp. Shows for SHOW_TICKS, whatever you
 * do (drinking, leaving the room).
 */
@Singleton
public class DivinePanel extends OverlayPanel
{
	/** Each room's bosses: all that are there have to be dead. */
	private static final List<Set<Integer>> ROOMS = List.of(
		Set.of(NpcID.TFA_BORK, NpcID.TFA_SMORK),
		Set.of(NpcID.TFA_EZEKIEL),
		Set.of(NpcID.TFA_JELLY_BOSS),
		Set.of(NpcID.TFA_MOIA),
		Set.of(NpcID.TFA_XARTH),
		Set.of(NpcID.TFA_DAGONHAI_LEADER, NpcID.TFA_DAGONHAI_LEADER_UNSHIELDED,
			NpcID.TFA_DAGONHAI_MINION_1, NpcID.TFA_DAGONHAI_MINION_1_UNSHIELDED,
			NpcID.TFA_DAGONHAI_MINION_2, NpcID.TFA_DAGONHAI_MINION_2_UNSHIELDED));

	/** How long it shows: 10 ticks (6 seconds), whatever you do. */
	private static final int SHOW_TICKS = 10;
	private static final Color FLASH_BACKGROUND = new Color(200, 0, 0, 220);

	@Inject
	private Client client;

	private final FracturedArchiveConfig config;

	/** Rooms whose reminder has gone off (until their bosses are gone or back above 0 hp). */
	private final Set<Integer> fired = new HashSet<>();
	/** Ticks in a row each room's bosses have all been at 0 hp: it has to hold for DEAD_TICKS (a one-tick blip doesn't count). */
	private static final int DEAD_TICKS = 2;
	private final int[] deadTicks = new int[ROOMS.size()];
	/** Shown until this tick (-1 = not shown). */
	private int showUntil = -1;

	@Inject
	DivinePanel(FracturedArchiveConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.BOTTOM_LEFT);
		panelComponent.setPreferredSize(new Dimension(150, 0));
	}

	/** Called every game tick by the plugin. */
	void onGameTick()
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return;
		}
		int tick = client.getTickCount();
		if (showUntil >= 0 && tick > showUntil)
		{
			showUntil = -1;
		}
		for (int i = 0; i < ROOMS.size(); i++)
		{
			Set<Integer> ids = ROOMS.get(i);
			boolean any = false;
			boolean allDead = true;
			for (NPC npc : wv.npcs())
			{
				if (ids.contains(npc.getId()))
				{
					any = true;
					if (!isDead(npc))
					{
						allDead = false;
					}
				}
			}
			if (!any || !allDead)
			{
				// Left the room, or a boss is back above 0 hp: it can go off again
				fired.remove(i);
				deadTicks[i] = 0;
			}
			else if (++deadTicks[i] >= DEAD_TICKS && fired.add(i))
			{
				showUntil = tick + SHOW_TICKS - 1;
			}
		}
	}

	private int ezekielDiedTick = -100;

	/** Ezekiel's death animation (passed on by Ezekiel, from the animation event). */
	public void ezekielDied()
	{
		int tick = client.getTickCount();
		if (tick - ezekielDiedTick > 30)
		{
			showUntil = tick + SHOW_TICKS - 1;
		}
		ezekielDiedTick = tick;
	}

	/** At 0 hp (his health bar empty), or already dying. */
	private static boolean isDead(NPC npc)
	{
		if (npc.getId() == NpcID.TFA_EZEKIEL)
		{
			// Ezekiel's bar empties mid-fight: he's handled by his death animation instead (see
			// ezekielDied)
			return false;
		}
		return npc.isDead() || (npc.getHealthScale() > 0 && npc.getHealthRatio() == 0);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.divineReminder() || showUntil < 0)
		{
			return null;
		}
		boolean flashOn = (System.currentTimeMillis() / 500) % 2 == 0;
		panelComponent.setBackgroundColor(flashOn ? FLASH_BACKGROUND : ComponentConstants.STANDARD_BACKGROUND_COLOR);
		panelComponent.getChildren().add(TitleComponent.builder().text("Use Divine Potion")
			.color(flashOn ? Color.WHITE : Color.RED).build());
		return super.render(graphics);
	}
}
