package net.runelite.client.plugins.fracturedarchive;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FontType;
import net.runelite.client.config.Range;

@ConfigGroup(FracturedArchivePlugin.CONFIG_GROUP)
public interface FracturedArchiveConfig extends Config
{
	@ConfigSection(
		name = "General",
		description = "Settings shared by every encounter",
		position = 0
	)
	String generalSection = "generalSection";

	@ConfigSection(
		name = "Archive Guardian",
		description = "Always the first encounter",
		position = 1,
		closedByDefault = true
	)
	String archiveguardianSection = "archiveguardianSection";

	@ConfigSection(
		name = "Bork",
		description = "Second encounter, paired with Ezekiel. Bork fights with the Smorks",
		position = 2,
		closedByDefault = true
	)
	String borkSection = "borkSection";

	@ConfigSection(
		name = "Ezekiel",
		description = "Second encounter, paired with Bork. A justiciar of Saradomin",
		position = 3,
		closedByDefault = true
	)
	String ezekielSection = "ezekielSection";

	@ConfigSection(
		name = "Dreadnoth",
		description = "Third encounter, paired with the Necromancer. Dreadnoth the Jellied",
		position = 4,
		closedByDefault = true
	)
	String dreadnothSection = "dreadnothSection";

	@ConfigSection(
		name = "Necromancer",
		description = "Third encounter, paired with Dreadnoth. Name not yet revealed",
		position = 5,
		closedByDefault = true
	)
	String necromancerSection = "necromancerSection";

	@ConfigSection(
		name = "Xarth",
		description = "Fourth encounter, paired with the Dagon'hai Trio. A Tormented Demon",
		position = 6,
		closedByDefault = true
	)
	String xarthSection = "xarthSection";

	@ConfigSection(
		name = "Dagon'hai Trio",
		description = "Fourth encounter, paired with Xarth",
		position = 7,
		closedByDefault = true
	)
	String dagonhaitrioSection = "dagonhaitrioSection";

	@ConfigSection(
		name = "Final boss",
		description = "Always the last encounter. Identity not yet revealed",
		position = 8,
		closedByDefault = true
	)
	String finalbossSection = "finalbossSection";

	@ConfigItem(
		keyName = "font",
		name = "Font",
		description = "Font used for all text overlays",
		position = 0,
		section = generalSection
	)
	default FontType font()
	{
		return FontType.BOLD.withSize(16);
	}

	@Range(min = 0, max = 300)
	@ConfigItem(
		keyName = "textZ",
		name = "Text height",
		description = "How high above a boss its tick counter is drawn",
		position = 2,
		section = generalSection
	)
	default int textZ()
	{
		return 50;
	}

	@Range(min = 1, max = 5)
	@ConfigItem(
		keyName = "borderWidth",
		name = "Border width",
		description = "Thickness of tile outlines",
		position = 1,
		section = generalSection
	)
	default int borderWidth()
	{
		return 1;
	}

	// Archive Guardian

	@ConfigItem(
		keyName = "guardianAttackTicks",
		name = "Attack tick counter",
		description = "Count down the ticks after the Guardian's ranged attack, special attack and landing",
		position = 10,
		section = archiveguardianSection
	)
	default boolean guardianAttackTicks()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "guardianRangedColor",
		name = "Ranged color",
		description = "Counter color after a ranged attack",
		position = 11,
		section = archiveguardianSection
	)
	default Color guardianRangedColor()
	{
		return Color.GREEN;
	}

	@Alpha
	@ConfigItem(
		keyName = "guardianSpecialColor",
		name = "Special color",
		description = "Counter color after a special attack",
		position = 12,
		section = archiveguardianSection
	)
	default Color guardianSpecialColor()
	{
		return Color.MAGENTA;
	}

	@Alpha
	@ConfigItem(
		keyName = "guardianGroundColor",
		name = "Grounded color",
		description = "Counter color while the Guardian is on the ground",
		position = 13,
		section = archiveguardianSection
	)
	default Color guardianGroundColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		keyName = "guardianMushrooms",
		name = "Highlight mushrooms",
		description = "Mark the mushroom and 1 tile around it from when it spawns until it starts to despawn",
		position = 0,
		section = archiveguardianSection
	)
	default boolean guardianMushrooms()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "guardianMushroomBorder",
		name = "Border color",
		description = "Outline color of the mushroom area",
		position = 1,
		section = archiveguardianSection
	)
	default Color guardianMushroomBorder()
	{
		return new Color(255, 0, 0, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "guardianMushroomFill",
		name = "Fill color",
		description = "Fill color of the mushroom area",
		position = 2,
		section = archiveguardianSection
	)
	default Color guardianMushroomFill()
	{
		return new Color(255, 0, 0, 40);
	}

	// Bork

	@ConfigItem(
		keyName = "smorkAttackCounter",
		name = "Smork attack counter",
		description = "Count each Smork's normal cannon shots since its last bomb (big cannon shot)",
		position = 0,
		section = borkSection
	)
	default boolean smorkAttackCounter()
	{
		return true;
	}

	@Range(min = 1, max = 20)
	@ConfigItem(
		keyName = "smorkAttacksPerBomb",
		name = "Shots per bomb",
		description = "How many normal shots a Smork fires before a bomb. The count turns red when it reaches this",
		position = 1,
		section = borkSection
	)
	default int smorkAttacksPerBomb()
	{
		return 6;
	}

	@ConfigItem(
		keyName = "borkStunCounter",
		name = "Stun counter",
		description = "Count down while Bork is stunned by the cannon",
		position = 2,
		section = borkSection
	)
	default boolean borkStunCounter()
	{
		return true;
	}

	@Range(min = 1, max = 30)
	@ConfigItem(
		keyName = "borkStunTicks",
		name = "Stun length",
		description = "How many ticks Bork stays stunned",
		position = 3,
		section = borkSection
	)
	default int borkStunTicks()
	{
		return 12;
	}

	@Alpha
	@ConfigItem(
		keyName = "borkStunColor",
		name = "Stun color",
		description = "Color of the stun counter",
		position = 4,
		section = borkSection
	)
	default Color borkStunColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "borkAttackCounter",
		name = "Attack counter",
		description = "Count down to Bork's next belly drum (gong) attack",
		position = 5,
		section = borkSection
	)
	default boolean borkAttackCounter()
	{
		return true;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "borkAttackTicks",
		name = "Attack speed",
		description = "Ticks between Bork's belly drum attacks",
		position = 6,
		section = borkSection
	)
	default int borkAttackTicks()
	{
		return 3;
	}

	@Alpha
	@ConfigItem(
		keyName = "borkAttackColor",
		name = "Attack color",
		description = "Color of the attack counter",
		position = 7,
		section = borkSection
	)
	default Color borkAttackColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "smorkStyleColors",
		name = "Color shots by style",
		description = "Color each Smork's shot counter by its last shot: magic or ranged (red once a bomb is due)",
		position = 8,
		section = borkSection
	)
	default boolean smorkStyleColors()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "smorkMagicColor",
		name = "Magic color",
		description = "Shot counter color after a magic shot",
		position = 9,
		section = borkSection
	)
	default Color smorkMagicColor()
	{
		return Color.CYAN;
	}

	@Alpha
	@ConfigItem(
		keyName = "smorkRangedColor",
		name = "Ranged color",
		description = "Shot counter color after a ranged shot",
		position = 10,
		section = borkSection
	)
	default Color smorkRangedColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "smorkFireBombs",
		name = "Highlight fire bombs",
		description = "Mark where each fire bomb will land while it's in the air, then the fire while it burns",
		position = 11,
		section = borkSection
	)
	default boolean smorkFireBombs()
	{
		return true;
	}

	@Range(min = 1, max = 7)
	@ConfigItem(
		keyName = "smorkFireSize",
		name = "Fire area size",
		description = "Width of the marked area in tiles (1 = just the tile, 3 = 3x3)",
		position = 12,
		section = borkSection
	)
	default int smorkFireSize()
	{
		return 1;
	}

	@Alpha
	@ConfigItem(
		keyName = "smorkFireBorder",
		name = "Fire border color",
		description = "Outline color of the fire bomb area",
		position = 13,
		section = borkSection
	)
	default Color smorkFireBorder()
	{
		return new Color(255, 100, 0, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "smorkFireFill",
		name = "Fire fill color",
		description = "Fill color of the fire bomb area",
		position = 14,
		section = borkSection
	)
	default Color smorkFireFill()
	{
		return new Color(255, 100, 0, 50);
	}

	// Ezekiel

	@ConfigItem(
		keyName = "ezekielSwordLanes",
		name = "Highlight sword lanes",
		description = "Mark the straight line each summoned sword will travel along, up to the first wall",
		position = 0,
		section = ezekielSection
	)
	default boolean ezekielSwordLanes()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ezekielSwordBorder",
		name = "Border color",
		description = "Outline color of the sword lanes",
		position = 1,
		section = ezekielSection
	)
	default Color ezekielSwordBorder()
	{
		return new Color(255, 200, 0, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "ezekielSwordFill",
		name = "Fill color",
		description = "Fill color of the sword lanes",
		position = 2,
		section = ezekielSection
	)
	default Color ezekielSwordFill()
	{
		return new Color(255, 200, 0, 40);
	}

	// Each encounter's settings go in its section above, for example:
	//
	// @ConfigItem(
	// 	keyName = "borkSmorkHighlight",
	// 	name = "Smork highlight",
	// 	description = "Highlights the Smorks",
	// 	position = 0,
	// 	section = borkSection
	// )
	// default boolean borkSmorkHighlight()
	// {
	// 	return true;
	// }
}
