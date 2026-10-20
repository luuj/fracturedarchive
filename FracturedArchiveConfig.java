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
		name = "Boss targets",
		description = "Highlight the true tile of the player each boss (and each Smork) is going after",
		position = 0,
		closedByDefault = true
	)
	String targetSection = "targetSection";

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

	// Boss targets

	@ConfigItem(
		keyName = "highlightTargets",
		name = "Highlight targets",
		description = "Highlight the true tile of the player each boss is going after",
		position = 0,
		section = targetSection
	)
	default boolean highlightTargets()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "targetColor",
		name = "Boss target color",
		description = "Tile color of the player a boss is going after",
		position = 1,
		section = targetSection
	)
	default Color targetColor()
	{
		return Color.RED;
	}

	@Alpha
	@ConfigItem(
		keyName = "smorkTargetColor",
		name = "Smork target color",
		description = "Tile color of the player each Smork has picked for its fire bomb, until the bomb comes out",
		position = 2,
		section = targetSection
	)
	default Color smorkTargetColor()
	{
		return Color.BLUE;
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
		name = "Beam color",
		description = "Counter color for the beam special",
		position = 12,
		section = archiveguardianSection
	)
	default Color guardianSpecialColor()
	{
		return Color.YELLOW;
	}

	@Alpha
	@ConfigItem(
		keyName = "guardianMushroomAttackColor",
		name = "Mushroom attack color",
		description = "Counter color when the special turns out to be the mushroom attack (mushrooms appear right after it)",
		position = 16,
		section = archiveguardianSection
	)
	default Color guardianMushroomAttackColor()
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
		description = "Mark the straight line (north, south, east or west) each sword will fly along, up to the first wall",
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

	@ConfigItem(
		keyName = "ezekielLaneTicks",
		name = "Show lane ticks",
		description = "Only show a sword's lane this many ticks before it goes off",
		position = 3,
		section = ezekielSection
	)
	@Range(min = 1, max = 30)
	default int ezekielLaneTicks()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "ezekielSwordTimer",
		name = "Sword timer",
		description = "Count down the ticks above each sword until it goes off",
		position = 4,
		section = ezekielSection
	)
	default boolean ezekielSwordTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ezekielSwordTicks",
		name = "Sword fuse ticks",
		description = "Ticks from a sword appearing until it goes off",
		position = 5,
		section = ezekielSection
	)
	@Range(min = 1, max = 100)
	default int ezekielSwordTicks()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "ezekielAttackCounter",
		name = "Attack counter",
		description = "Count the ticks above Ezekiel after his melee, magic and special attacks",
		position = 6,
		section = ezekielSection
	)
	default boolean ezekielAttackCounter()
	{
		return true;
	}

	@Range(min = 1, max = 20)
	@ConfigItem(
		keyName = "ezekielAttackSpeed",
		name = "Attack speed",
		description = "Ticks between Ezekiel's attacks",
		position = 7,
		section = ezekielSection
	)
	default int ezekielAttackSpeed()
	{
		return 4;
	}

	@Alpha
	@ConfigItem(
		keyName = "ezekielMeleeColor",
		name = "Melee color",
		description = "Counter color after a melee attack",
		position = 8,
		section = ezekielSection
	)
	default Color ezekielMeleeColor()
	{
		return Color.RED;
	}

	@Alpha
	@ConfigItem(
		keyName = "ezekielMagicColor",
		name = "Magic color",
		description = "Counter color after a magic attack",
		position = 9,
		section = ezekielSection
	)
	default Color ezekielMagicColor()
	{
		return Color.CYAN;
	}

	@Alpha
	@ConfigItem(
		keyName = "ezekielSpecialColor",
		name = "Special color",
		description = "Counter color after the star special",
		position = 10,
		section = ezekielSection
	)
	default Color ezekielSpecialColor()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
		keyName = "ezekielStarCracks",
		name = "Highlight star cracks",
		description = "Mark the tiles of the special's star cracks until they explode",
		position = 11,
		section = ezekielSection
	)
	default boolean ezekielStarCracks()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ezekielStarBorder",
		name = "Star border color",
		description = "Outline color of the star crack tiles",
		position = 12,
		section = ezekielSection
	)
	default Color ezekielStarBorder()
	{
		return new Color(255, 60, 60, 220);
	}

	@Alpha
	@ConfigItem(
		keyName = "ezekielStarFill",
		name = "Star fill color",
		description = "Fill color of the star crack tiles",
		position = 13,
		section = ezekielSection
	)
	default Color ezekielStarFill()
	{
		return new Color(255, 60, 60, 50);
	}

	@ConfigItem(
		keyName = "ezekielFinale",
		name = "Finale tracker",
		description = "Count the ticks since the finale started above Ezekiel, and mark the finale sword telegraphs",
		position = 14,
		section = ezekielSection
	)
	default boolean ezekielFinale()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ezekielFinaleColor",
		name = "Finale color",
		description = "Color of the finale counter and sword telegraph tiles",
		position = 15,
		section = ezekielSection
	)
	default Color ezekielFinaleColor()
	{
		return Color.MAGENTA;
	}

	@ConfigItem(
		keyName = "ezekielDebug",
		name = "Debug log",
		description = "Post Ezekiel's animations, graphics, NPCs and objects to the chat box with tick numbers, to find the finale's safe tiles",
		position = 16,
		section = ezekielSection
	)
	default boolean ezekielDebug()
	{
		return false;
	}

	// Dreadnoth

	@ConfigItem(
		keyName = "dreadnothSplatter",
		name = "Highlight blood splatter",
		description = "Mark the blood splatter tiles Dreadnoth leaves on the ground",
		position = 0,
		section = dreadnothSection
	)
	default boolean dreadnothSplatter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dreadnothIncoming",
		name = "Show incoming splatter",
		description = "Mark where Dreadnoth's ranged blobs will land while they're in the air",
		position = 1,
		section = dreadnothSection
	)
	default boolean dreadnothIncoming()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "dreadnothSplatterBorder",
		name = "Border color",
		description = "Outline color of the blood splatter tiles",
		position = 2,
		section = dreadnothSection
	)
	default Color dreadnothSplatterBorder()
	{
		return new Color(180, 0, 0, 220);
	}

	@Alpha
	@ConfigItem(
		keyName = "dreadnothSplatterFill",
		name = "Fill color",
		description = "Fill color of the blood splatter tiles",
		position = 3,
		section = dreadnothSection
	)
	default Color dreadnothSplatterFill()
	{
		return new Color(180, 0, 0, 60);
	}

	@Alpha
	@ConfigItem(
		keyName = "dreadnothIncomingColor",
		name = "Incoming color",
		description = "Outline color of where a ranged blob will land",
		position = 4,
		section = dreadnothSection
	)
	default Color dreadnothIncomingColor()
	{
		return new Color(255, 120, 0, 220);
	}

	@ConfigItem(
		keyName = "dreadnothIncomingTicks",
		name = "Incoming splatter ticks",
		description = "Count down the ticks on the tile until each ranged blob lands",
		position = 8,
		section = dreadnothSection
	)
	default boolean dreadnothIncomingTicks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dreadnothJumpCounter",
		name = "Jump counter",
		description = "Count the ticks above Dreadnoth from when he jumps until he lands",
		position = 5,
		section = dreadnothSection
	)
	default boolean dreadnothJumpCounter()
	{
		return true;
	}

	@Range(min = 1, max = 20)
	@ConfigItem(
		keyName = "dreadnothJumpTicks",
		name = "Jump ticks",
		description = "Ticks from Dreadnoth jumping until he lands",
		position = 6,
		section = dreadnothSection
	)
	default int dreadnothJumpTicks()
	{
		return 4;
	}

	@Alpha
	@ConfigItem(
		keyName = "dreadnothJumpColor",
		name = "Jump color",
		description = "Color of the jump counter",
		position = 7,
		section = dreadnothSection
	)
	default Color dreadnothJumpColor()
	{
		return Color.ORANGE;
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
