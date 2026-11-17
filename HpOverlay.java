package net.runelite.client.plugins.fracturedarchive;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * A small labeled hp over every boss (size 12, on the line above its tick counter), like
 * Opponent Information: the amount left, the full amount and the share, for whatever is taking
 * damage right now. "Shield: 540/550 (99%)" in teal while its shield is up (the Archive Guardian
 * until it's broken, and Ocellus for all of phase 1), "HP: 2940/3000 (98%)" in green otherwise.
 * Red while it's within 5% (10% for the Guardian) above a phase threshold (see PHASES), back to its
 * color once past it. The Guardian's shield stays red at 0% until he turns into his hp form, and
 * Ocellus in phase 3 goes back to normal as soon as he says Obliteration/Run for that threshold.
 *
 * Where the number comes from, best first:
 * - The game's boss hp bar (the HPBAR_HUD varbits): exact, and it follows the pool being hit
 *   (from the logs: the Guardian's reads x/550 while shielded and x/1100 once it's down).
 * - The boss's own health bar over its head, for a boss the game's bar isn't showing (one of
 *   several Smorks). Only known while that bar is up, so the last value is kept. Not used when
 *   the game's bar is there: over the Guardian it can still be showing his hp after the shield
 *   comes back.
 * Worked out once a tick; drawing only reads the results.
 */
@Singleton
class HpOverlay extends EncounterOverlay
{
	private static final Color HP_COLOR = Color.GREEN;
	private static final Color SHIELD_COLOR = new Color(0, 190, 170);
	/** Within this many % above a phase threshold (10 for the Archive Guardian). */
	private static final double NEAR_PHASE = 5;
	private static final double GUARDIAN_NEAR_PHASE = 10;
	private static final Color NEAR_PHASE_COLOR = Color.RED;

	/**
	 * The % each form changes phase at (of the number shown: the Guardian's shield and then his hp,
	 * Ocellus's shield in phase 1). Red within 5% above one (10% for the Guardian), back to normal once past it.
	 */
	private static final Map<Integer, double[]> PHASES = Map.of(
		NpcID.TFA_GUARDIAN, new double[]{0},
		NpcID.TFA_GUARDIAN_UNSHIELDED, new double[]{33},
		NpcID.TFA_EZEKIEL, new double[]{33},
		NpcID.TFA_MOIA, new double[]{33},
		NpcID.TFA_JELLY_BOSS, new double[]{85, 65, 45, 25},
		NpcID.TFA_OCELLUS_P1, new double[]{66, 25},
		NpcID.TFA_OCELLUS_P3, new double[]{75, 50, 25});
	private static final float SIZE = 12f;

	/** Every boss form that gets a number, mapped to its boss (forms of one boss share a number). */
	private static final Map<Integer, Integer> BOSS_OF = new HashMap<>();
	/** The forms with a shield up: the Archive Guardian (until it's broken) and Ocellus in phase 1. */
	private static final Set<Integer> SHIELDED_IDS = Set.of(NpcID.TFA_GUARDIAN, NpcID.TFA_OCELLUS_P1);

	static
	{
		boss(NpcID.TFA_GUARDIAN, NpcID.TFA_GUARDIAN_UNSHIELDED);
		boss(NpcID.TFA_BORK);
		boss(NpcID.TFA_SMORK);
		boss(NpcID.TFA_EZEKIEL);
		boss(NpcID.TFA_JELLY_BOSS);
		boss(NpcID.TFA_MOIA);
		boss(NpcID.TFA_XARTH);
		boss(NpcID.TFA_DAGONHAI_LEADER, NpcID.TFA_DAGONHAI_LEADER_UNSHIELDED);
		boss(NpcID.TFA_DAGONHAI_MINION_1, NpcID.TFA_DAGONHAI_MINION_1_UNSHIELDED);
		boss(NpcID.TFA_DAGONHAI_MINION_2, NpcID.TFA_DAGONHAI_MINION_2_UNSHIELDED);
		boss(NpcID.TFA_OCELLUS_P1, NpcID.TFA_OCELLUS_P2, NpcID.TFA_OCELLUS_P3);
	}

	private static void boss(int... forms)
	{
		for (int form : forms)
		{
			BOSS_OF.put(form, forms[0]);
		}
	}

	/** One boss's number: -1 = not known (nothing drawn). */
	private static final class Hp
	{
		private int form = -1;
		/** The share left of what's taking damage, and whether that's the shield. */
		private int value = -1;
		/** The exact amount left and its full amount, when the game's boss hp bar gives them (-1 = not known). */
		private int current = -1;
		private int max = -1;
		private boolean shield;
		/** Within 5% (10% for the Guardian) above one of its phase thresholds. */
		private boolean nearPhase;
		/** Ocellus phase 3: thresholds he's done his Obliteration for (no more red for those). */
		private final java.util.Set<Double> done = new java.util.HashSet<>();
		/** Drawn text, remade only when the number changes. */
		private String text = "";
	}

	private final Map<NPC, Hp> bosses = new LinkedHashMap<>();
	/** The small font, remade only when the main font changes. */
	private Font smallFont;
	private Font smallFontOf;

	@Inject
	private net.runelite.client.plugins.fracturedarchive.finalboss.FinalBoss finalBoss;

	@Inject
	HpOverlay(FracturedArchiveConfig config)
	{
		super(config);
	}

	/** Called every game tick by the plugin. */
	void onGameTick()
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null || !wv.isInstance() || !config.bossHp())
		{
			// The raid is an instance: nothing to do anywhere else
			bosses.clear();
			return;
		}

		// The bosses here now, and how many there are of each (the boss hp bar only speaks for
		// a boss there's one of, not for one of several Smorks)
		Map<Integer, Integer> count = new HashMap<>();
		Map<NPC, Hp> here = new LinkedHashMap<>();
		for (NPC npc : wv.npcs())
		{
			Integer boss = BOSS_OF.get(npc.getId());
			if (boss != null)
			{
				count.merge(boss, 1, Integer::sum);
				Hp hp = bosses.get(npc);
				here.put(npc, hp != null ? hp : new Hp());
			}
		}
		bosses.clear();
		bosses.putAll(here);
		if (bosses.isEmpty())
		{
			return;
		}

		int hudNpc = client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
		int hudHp = client.getVarbitValue(VarbitID.HPBAR_HUD_HP);
		int hudBase = client.getVarbitValue(VarbitID.HPBAR_HUD_BASEHP);
		Integer hudBoss = BOSS_OF.get(hudNpc);

		for (Map.Entry<NPC, Hp> entry : bosses.entrySet())
		{
			NPC npc = entry.getKey();
			Hp h = entry.getValue();
			int id = npc.getId();
			int boss = BOSS_OF.get(id);
			boolean shielded = SHIELDED_IDS.contains(id);
			if (id != h.form)
			{
				// A new form: the old number was for something else (a shield, or the last phase)
				h.form = id;
				h.value = -1;
				h.current = -1;
				h.max = -1;
				h.done.clear();
			}

			// The bar over its head, when it's showing
			int scale = npc.getHealthScale();
			int ratio = npc.getHealthRatio();
			int bar = scale > 0 && ratio >= 0 ? percent(ratio, scale) : -1;

			int value = h.value;
			int current = h.current;
			int max = h.max;
			if (hudBoss != null && hudBoss == boss && hudBase > 0 && count.get(boss) == 1)
			{
				// Exact: like Opponent Information, "540/550 (99%)"
				current = Math.max(0, hudHp);
				max = hudBase;
				value = percent(current, max);
			}
			else if (bar >= 0)
			{
				// Only a share is known
				current = -1;
				max = -1;
				value = bar;
			}

			if (value != h.value || current != h.current || max != h.max || shielded != h.shield)
			{
				h.value = value;
				h.current = current;
				h.max = max;
				h.shield = shielded;
				String label = shielded ? "Shield: " : "HP: ";
				if (value < 0)
				{
					h.text = "";
				}
				else if (max > 0)
				{
					h.text = label + current + "/" + max + " (" + value + "%)";
				}
				else
				{
					h.text = label + value + "%";
				}
			}

			double exact = max > 0 ? current * 100.0 / max : value;
			if (id == NpcID.TFA_OCELLUS_P3 && exact >= 0 && finalBoss.getObliterationTicks() > 0)
			{
				// He's said Obliteration/Run: the threshold he's at is done, back to normal now
				for (double phase : PHASES.get(id))
				{
					if (exact - phase <= NEAR_PHASE && exact > phase - NEAR_PHASE)
					{
						h.done.add(phase);
					}
				}
			}
			h.nearPhase = nearPhase(id, exact, h.done);
		}
	}

	/** Whether a % is just above (within NEAR_PHASE of) one of its phase thresholds. */
	private static boolean nearPhase(int form, double percent, java.util.Set<Double> done)
	{
		double[] phases = PHASES.get(form);
		if (phases == null || percent < 0)
		{
			return false;
		}
		double near = form == NpcID.TFA_GUARDIAN || form == NpcID.TFA_GUARDIAN_UNSHIELDED ? GUARDIAN_NEAR_PHASE : NEAR_PHASE;
		for (double phase : phases)
		{
			if (done.contains(phase))
			{
				continue;
			}
			// A shield breaking at 0% stays red at 0 (until he changes into his hp form)
			boolean past = phase == 0 ? percent < 0 : percent <= phase;
			if (!past && percent - phase <= near)
			{
				return true;
			}
		}
		return false;
	}

	/** A share as a whole percent: rounded up, so it only reads 0 when it is 0. */
	private static int percent(int value, int max)
	{
		if (value <= 0)
		{
			return 0;
		}
		return Math.min(100, (value * 100 + max - 1) / max);
	}

	@Override
	protected void renderEncounter(Graphics2D graphics)
	{
		if (bosses.isEmpty() || !config.bossHp())
		{
			return;
		}

		Font main = graphics.getFont();
		int lineHeight = graphics.getFontMetrics().getHeight();
		if (smallFont == null || smallFontOf != main)
		{
			smallFont = main.deriveFont(SIZE);
			smallFontOf = main;
		}
		graphics.setFont(smallFont);
		for (Map.Entry<NPC, Hp> entry : bosses.entrySet())
		{
			NPC npc = entry.getKey();
			Hp h = entry.getValue();
			if (npc.isDead() || h.text.isEmpty())
			{
				continue;
			}
			// Centered over the boss, one line above where its tick counter goes
			Point anchor = npc.getCanvasTextLocation(graphics, h.text, config.textZ());
			if (anchor != null)
			{
				renderText(graphics, new Point(anchor.getX(), anchor.getY() - lineHeight), h.text, h.nearPhase ? NEAR_PHASE_COLOR : h.shield ? SHIELD_COLOR : HP_COLOR);
			}
		}
		graphics.setFont(main);
	}
}
