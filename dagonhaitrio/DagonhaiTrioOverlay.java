package net.runelite.client.plugins.fracturedarchive.dagonhaitrio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

@Singleton
public class DagonhaiTrioOverlay extends EncounterOverlay
{
	private static final Color PORTAL = new Color(170, 0, 255);
	private static final Color CLOSE_SPOT = Color.GREEN;
	private static final Color FIRE = new Color(255, 115, 0);
	private static final Color WATER = new Color(0, 120, 255);
	private static final Color AIR = Color.WHITE;
	private static final Color EARTH = Color.GREEN;
	private static final Color ZAMORAK = Color.RED;
	private static final Color BANDOS = new Color(160, 120, 60);
	private static final Color SARADOMIN = new Color(80, 160, 255);
	private static final Color SHOCKWAVE = new Color(160, 120, 60);
	private static final Color HEAL = Color.YELLOW;
	private static final Color HURT = new Color(0, 120, 255);

	@Inject
	private DagonhaiTrio encounter;

	@Inject
	protected DagonhaiTrioOverlay(FracturedArchiveConfig config)
	{
		super(config);
	}

	@Override
	protected void renderEncounter(Graphics2D graphics)
	{
		if (!encounter.isFightActive())
		{
			return;
		}

		int elderTicks = encounter.getElderTicks();
		NPC elder = encounter.getBoss();
		if (config.dagonhaiElderCounter() && elderTicks > 0 && elder != null)
		{
			// The elder's attack counter: cyan magic, green ranged
			String text = Integer.toString(elderTicks);
			Point point = elder.getCanvasTextLocation(graphics, text, config.textZ());
			if (point != null)
			{
				renderText(graphics, point, text, encounter.isElderMagic() ? Color.CYAN : Color.GREEN);
			}
		}

		if (config.dagonhaiPortals())
		{
			// The imp portals in the corners
			java.util.List<NPC> portals = encounter.getPortals();
			for (NPC portal : portals)
			{
				outline(graphics, portal, size(portal), PORTAL);
			}

			// Where to stand to close them all at once: tiles next to every open rift (diagonals
			// count). Only when there's more than one, since any tile beside a lone rift would do.
			if (portals.size() > 1)
			{
				net.runelite.api.coords.WorldPoint first = portals.get(0).getWorldLocation();
				for (int dx = -1; dx <= 1; dx++)
				{
					for (int dy = -1; dy <= 1; dy++)
					{
						net.runelite.api.coords.WorldPoint tile = first.dx(dx).dy(dy);
						boolean all = true;
						for (NPC portal : portals)
						{
							net.runelite.api.coords.WorldPoint p = portal.getWorldLocation();
							int d = Math.max(Math.abs(p.getX() - tile.getX()), Math.abs(p.getY() - tile.getY()));
							if (d != 1 || p.getPlane() != tile.getPlane())
							{
								all = false;
								break;
							}
						}
						if (all)
						{
							renderTile(graphics, tile, CLOSE_SPOT, new Color(0, 255, 0, 60), config.borderWidth());
						}
					}
				}
			}
		}

		if (config.dagonhaiShockwave())
		{
			// The Bandos shockwave: a ring that spreads a tile a tick from the cauldron (or whoever
			// tanked it). All its tiles are queued at once, each to go off when the ring gets there;
			// fill the ring where it is now and outline where it'll be next tick.
			net.runelite.api.WorldView wv = client.getTopLevelWorldView();
			if (wv != null)
			{
				int cycle = client.getGameCycle();
				for (net.runelite.api.GraphicsObject g : wv.getGraphicsObjects())
				{
					int id = g.getId();
					if (id < net.runelite.api.gameval.SpotanimID.VFX_DAGONHAI_CAULDRON_ATTACK_BANDOS_AOE_ATTACK_01
						|| id > net.runelite.api.gameval.SpotanimID.VFX_DAGONHAI_CAULDRON_ATTACK_BANDOS_AOE_ATTACK_04
						|| g.finished())
					{
						continue;
					}
					int ticks = (g.getStartCycle() - cycle + 29) / 30;
					if (ticks > 1 || ticks < 0)
					{
						continue;
					}
					Polygon poly = Perspective.getCanvasTilePoly(client, g.getLocation());
					if (ticks == 0)
					{
						renderPoly(graphics, poly, SHOCKWAVE, new Color(SHOCKWAVE.getRed(), SHOCKWAVE.getGreen(), SHOCKWAVE.getBlue(), 90), config.borderWidth());
					}
					else
					{
						renderPoly(graphics, poly, SHOCKWAVE, null, config.borderWidth());
					}
				}
			}
		}

		if (config.dagonhaiTomePath())
		{
			// The tome's path from the monk to the cauldron (the gusts on the floor): stand on it to
			// block the tome. In the god's color while we know which god it is.
			Color c = Color.WHITE;
			for (DagonhaiTrio.God god : encounter.getIncantations().values())
			{
				c = godColor(god);
			}
			net.runelite.api.WorldView wv = client.getTopLevelWorldView();
			if (wv != null)
			{
				for (net.runelite.api.GraphicsObject g : wv.getGraphicsObjects())
				{
					int id = g.getId();
					if (g.finished()
						|| id < net.runelite.api.gameval.SpotanimID.VFX_DAGONHAI_THROW_BOOK_FLOOR_ZAMORAK_NORTH_01
						|| id > net.runelite.api.gameval.SpotanimID.VFX_DAGONHAI_THROW_BOOK_FLOOR_BANDOS_NORTH_WEST_01)
					{
						continue;
					}
					renderPoly(graphics, Perspective.getCanvasTilePoly(client, g.getLocation()), c,
						new Color(c.getRed(), c.getGreen(), c.getBlue(), 50), config.borderWidth());
				}
			}
		}

		if (config.dagonhaiFlames())
		{
			// Zamorak: the flame glyphs on the floor, from when they appear until they erupt (3 to 5
			// ticks later)
			net.runelite.api.WorldView wv = client.getTopLevelWorldView();
			if (wv != null)
			{
				for (net.runelite.api.GraphicsObject g : wv.getGraphicsObjects())
				{
					int id = g.getId();
					if (g.finished()
						|| id < net.runelite.api.gameval.SpotanimID.VFX_DAGONHAI_CAULDRON_ATTACK_ZAMORAK_AOE_IDLE_00
						|| id > net.runelite.api.gameval.SpotanimID.VFX_DAGONHAI_CAULDRON_ATTACK_ZAMORAK_AOE_ATTACK_01)
					{
						continue;
					}
					renderPoly(graphics, Perspective.getCanvasTilePoly(client, g.getLocation()), ZAMORAK,
						new Color(255, 0, 0, 60), config.borderWidth());
				}
			}
		}

		if (config.dagonhaiLightning())
		{
			// Saradomin: the lightning strike tiles, from their telegraph until the strike (about 4
			// ticks for blue, 5 for yellow). Yellow heals, blue hurts.
			net.runelite.api.WorldView wv = client.getTopLevelWorldView();
			if (wv != null)
			{
				for (net.runelite.api.GraphicsObject g : wv.getGraphicsObjects())
				{
					if (g.finished())
					{
						continue;
					}
					Color c;
					switch (g.getId())
					{
						case net.runelite.api.gameval.SpotanimID.TFA_DAGONHAI_SARADOMIN_HEAL_TELEGRAPH_150:
						case net.runelite.api.gameval.SpotanimID.TFA_DAGONHAI_SARADOMIN_HEAL_TELEGRAPH_180:
						case net.runelite.api.gameval.SpotanimID.TFA_DAGONHAI_SARADOMIN_HEAL_LIGHTNING:
							c = HEAL;
							break;
						case net.runelite.api.gameval.SpotanimID.TFA_DAGONHAI_SARADOMIN_DAMAGE_TELEGRAPH_150:
						case net.runelite.api.gameval.SpotanimID.TFA_DAGONHAI_SARADOMIN_DAMAGE_LIGHTNING:
							c = HURT;
							break;
						default:
							continue;
					}
					renderPoly(graphics, Perspective.getCanvasTilePoly(client, g.getLocation()), c,
						new Color(c.getRed(), c.getGreen(), c.getBlue(), 60), config.borderWidth());
				}
			}
		}

		if (config.dagonhaiElements())
		{
			// A wizard channelling an element: its tiles in that element's color, with the element's
			// name (size 12, like "Tank")
			for (Map.Entry<NPC, DagonhaiTrio.Element> e : encounter.getChannels().entrySet())
			{
				Color c = elementColor(e.getValue());
				outline(graphics, e.getKey(), size(e.getKey()), c);
				label(graphics, e.getKey(), elementName(e.getValue()), c);
			}
		}

		if (config.dagonhaiIncantations())
		{
			// "What have we here?": an incantation's coming from this monk
			for (NPC monk : encounter.getIncantationWarnings())
			{
				outline(graphics, monk, size(monk), Color.WHITE);
				label(graphics, monk, "Incantation", Color.WHITE);
			}

			// The wizard calling on a god: its tile, with the god's name (size 12, like "Tank")
			for (Map.Entry<NPC, DagonhaiTrio.God> e : encounter.getIncantations().entrySet())
			{
				NPC wizard = e.getKey();
				Color c = godColor(e.getValue());
				outline(graphics, wizard, size(wizard), c);
				label(graphics, wizard, godName(e.getValue()), c);
			}
		}
	}

	/** A word on the NPC's tiles, at size 12. */
	private void label(Graphics2D graphics, NPC npc, String text, Color color)
	{
		LocalPoint lp = npc.getLocalLocation();
		if (lp == null)
		{
			return;
		}
		java.awt.Font font = graphics.getFont();
		graphics.setFont(font.deriveFont(12f));
		Point point = Perspective.getCanvasTextLocation(client, graphics, lp, text, 0);
		if (point != null)
		{
			renderText(graphics, point, text, color);
		}
		graphics.setFont(font);
	}

	private static String elementName(DagonhaiTrio.Element element)
	{
		switch (element)
		{
			case FIRE:
				return "Fire";
			case WATER:
				return "Water";
			case AIR:
				return "Air";
			default:
				return "Earth";
		}
	}

	private static int size(NPC npc)
	{
		NPCComposition comp = npc.getComposition();
		return comp != null ? comp.getSize() : 1;
	}

	/** An area of the given size centered on the NPC. */
	private void outline(Graphics2D graphics, NPC npc, int size, Color color)
	{
		LocalPoint lp = npc.getLocalLocation();
		if (lp == null)
		{
			return;
		}
		Polygon poly = Perspective.getCanvasTileAreaPoly(client, lp, size);
		renderPoly(graphics, poly, color, new Color(color.getRed(), color.getGreen(), color.getBlue(), 40), config.borderWidth());
	}

	private static Color elementColor(DagonhaiTrio.Element element)
	{
		switch (element)
		{
			case FIRE:
				return FIRE;
			case WATER:
				return WATER;
			case AIR:
				return AIR;
			default:
				return EARTH;
		}
	}

	private static Color godColor(DagonhaiTrio.God god)
	{
		switch (god)
		{
			case ZAMORAK:
				return ZAMORAK;
			case BANDOS:
				return BANDOS;
			default:
				return SARADOMIN;
		}
	}

	private static String godName(DagonhaiTrio.God god)
	{
		switch (god)
		{
			case ZAMORAK:
				return "Zamorak";
			case BANDOS:
				return "Bandos";
			default:
				return "Saradomin";
		}
	}
}
