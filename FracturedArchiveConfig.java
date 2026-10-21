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
		keyName = "ezekielAttackCounter",
		name = "Attack counter",
		description = "Count the 5 ticks above Ezekiel after his melee, magic and special attacks",
		position = 6,
		section = ezekielSection
	)
	default boolean ezekielAttackCounter()
	{
		return true;
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
		keyName = "ezekielStarCounter",
		name = "Star counter",
		description = "Under Ezekiel, count his basic attacks 1, 2, then 3 on the star special, then hide",
		position = 10,
		section = ezekielSection
	)
	default boolean ezekielStarCounter()
	{
		return true;
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
		keyName = "ezekielFinaleFront",
		name = "Finale dodge row",
		description = "During the finale, mark the far row of the 3x3 in front of Ezekiel (where to dodge to)",
		position = 14,
		section = ezekielSection
	)
	default boolean ezekielFinaleFront()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ezekielFinaleSafeColor",
		name = "Finale dodge color",
		description = "Color of the finale dodge row",
		position = 15,
		section = ezekielSection
	)
	default Color ezekielFinaleSafeColor()
	{
		return Color.GREEN;
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
		name = "Show incoming blood",
		description = "Mark where Dreadnoth's blood will land while it's in the air",
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
		description = "Outline color of where his blood will land",
		position = 4,
		section = dreadnothSection
	)
	default Color dreadnothIncomingColor()
	{
		return new Color(255, 120, 0, 220);
	}

	@ConfigItem(
		keyName = "dreadnothIncomingTicks",
		name = "Incoming blood ticks",
		description = "Count down the ticks on the tile until his blood lands",
		position = 8,
		section = dreadnothSection
	)
	default boolean dreadnothIncomingTicks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dreadnothRangedProjectiles",
		name = "Mark ranged projectiles",
		description = "Show the ticks on Dreadnoth's ranged attack while it's aimed at you, like Projectile Ticks",
		position = 9,
		section = dreadnothSection
	)
	default boolean dreadnothRangedProjectiles()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "dreadnothRangedColor",
		name = "Ranged color",
		description = "Color of the ranged projectile tick count",
		position = 10,
		section = dreadnothSection
	)
	default Color dreadnothRangedColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "dreadnothJumpCounter",
		name = "Jump counter",
		description = "Count the ticks above Dreadnoth from when he jumps until he lands (6, or 4 after he says \"glub glub\")",
		position = 5,
		section = dreadnothSection
	)
	default boolean dreadnothJumpCounter()
	{
		return true;
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

	// Xarth

	@ConfigItem(
		keyName = "xarthAttackCounter",
		name = "Attack counter",
		description = "Count the 4 ticks above Xarth after each attack",
		position = 0,
		section = xarthSection
	)
	default boolean xarthAttackCounter()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "xarthMeleeColor",
		name = "Melee color",
		description = "Counter color after a melee attack",
		position = 1,
		section = xarthSection
	)
	default Color xarthMeleeColor()
	{
		return Color.RED;
	}

	@Alpha
	@ConfigItem(
		keyName = "xarthMagicColor",
		name = "Magic color",
		description = "Counter color after a magic attack",
		position = 2,
		section = xarthSection
	)
	default Color xarthMagicColor()
	{
		return Color.CYAN;
	}

	@Alpha
	@ConfigItem(
		keyName = "xarthRangedColor",
		name = "Ranged color",
		description = "Counter color after a ranged attack",
		position = 3,
		section = xarthSection
	)
	default Color xarthRangedColor()
	{
		return Color.GREEN;
	}

	@Alpha
	@ConfigItem(
		keyName = "xarthSpecialColor",
		name = "Special color",
		description = "Counter color after a flame blast, root, AoE or summon",
		position = 4,
		section = xarthSection
	)
	default Color xarthSpecialColor()
	{
		return Color.YELLOW;
	}

	// Final boss (Ocellus)

	@ConfigItem(
		keyName = "ocellusAttackCounter",
		name = "Attack counter",
		description = "Count the ticks above Ocellus after each attack (5 for attacks not timed yet)",
		position = 0,
		section = finalbossSection
	)
	default boolean ocellusAttackCounter()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusMeleeColor",
		name = "Melee color",
		description = "Counter color after a melee attack",
		position = 2,
		section = finalbossSection
	)
	default Color ocellusMeleeColor()
	{
		return Color.RED;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusProjectileColor",
		name = "Projectile color",
		description = "Counter color after a projectile attack, and the color of his other projectiles",
		position = 3,
		section = finalbossSection
	)
	default Color ocellusProjectileColor()
	{
		return Color.MAGENTA;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusSpecialColor",
		name = "Special color",
		description = "Counter color after a special attack (railgun, charge, unleash, soul fragment and so on)",
		position = 4,
		section = finalbossSection
	)
	default Color ocellusSpecialColor()
	{
		return Color.YELLOW;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusPrayerColor",
		name = "Prayer special color",
		description = "Counter color after the special that turns off prayer",
		position = 4,
		section = finalbossSection
	)
	default Color ocellusPrayerColor()
	{
		return new Color(80, 160, 255);
	}

	@ConfigItem(
		keyName = "ocellusSweepCounter",
		name = "Sweep counter",
		description = "Under Ocellus in phase 3, count his basic attacks 1, 2, then 3 on the sweep, then hide",
		position = 4,
		section = finalbossSection
	)
	default boolean ocellusSweepCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusUnleashCounter",
		name = "Unleash counter",
		description = "After Unleash's fourth floor AoE wave, count down the 12 ticks until he falls back down (in the special color)",
		position = 4,
		section = finalbossSection
	)
	default boolean ocellusUnleashCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusProjectiles",
		name = "Mark projectiles",
		description = "Show the ticks until Ocellus's projectiles land: on the projectile if it's aimed at you (like Projectile Ticks), on the landing tile if it's thrown at the ground",
		position = 5,
		section = finalbossSection
	)
	default boolean ocellusProjectiles()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusWaterColor",
		name = "Water attack color",
		description = "Color of his water projectile",
		position = 6,
		section = finalbossSection
	)
	default Color ocellusWaterColor()
	{
		return Color.CYAN;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusFireColor",
		name = "Fire attack color",
		description = "Color of his fire projectile",
		position = 7,
		section = finalbossSection
	)
	default Color ocellusFireColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "ocellusGroundMarkers",
		name = "Ground markers",
		description = "Mark dangerous floor (charge cracks, explosions, AoEs, bombs, debris, void puddles)",
		position = 8,
		section = finalbossSection
	)
	default boolean ocellusGroundMarkers()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusDangerColor",
		name = "Danger color",
		description = "Color of dangerous floor",
		position = 9,
		section = finalbossSection
	)
	default Color ocellusDangerColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		keyName = "ocellusArtifacts",
		name = "Highlight artifacts",
		description = "Outline the artifacts (damage, spec, heal) while they can be used",
		position = 11,
		section = finalbossSection
	)
	default boolean ocellusArtifacts()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusArtifactColor",
		name = "Artifact color",
		description = "Outline color of usable artifacts",
		position = 12,
		section = finalbossSection
	)
	default Color ocellusArtifactColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "ocellusPortalCrossing",
		name = "Portal crossing",
		description = "Highlight the safe portal tile the game puts on the floor, nearest you",
		position = 13,
		section = finalbossSection
	)
	default boolean ocellusPortalCrossing()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusPortalCrossingColor",
		name = "Portal crossing color",
		description = "Color of the portal crossing square",
		position = 13,
		section = finalbossSection
	)
	default Color ocellusPortalCrossingColor()
	{
		return Color.ORANGE;
	}

	@ConfigItem(
		keyName = "ocellusSoulFragments",
		name = "Highlight soul fragments",
		description = "Highlight the tile of each soul fragment (orb) while it's spawning or on the floor, with a countdown while it spawns (6 or 12 ticks)",
		position = 13,
		section = finalbossSection
	)
	default boolean ocellusSoulFragments()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusSoulFragmentColor",
		name = "Soul fragment color",
		description = "Color of soul fragment tiles",
		position = 13,
		section = finalbossSection
	)
	default Color ocellusSoulFragmentColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "ocellusVoidPortals",
		name = "Highlight void portals",
		description = "Highlight phase 3's Void Siphon, Void Augmenter and Void Rift, each in its own color",
		position = 13,
		section = finalbossSection
	)
	default boolean ocellusVoidPortals()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusSiphonColor",
		name = "Void Siphon color",
		description = "Color of the Void Siphon (heals Ocellus)",
		position = 13,
		section = finalbossSection
	)
	default Color ocellusSiphonColor()
	{
		return Color.RED;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusAugmenterColor",
		name = "Void Augmenter color",
		description = "Color of the Void Augmenter (buffs Ocellus)",
		position = 13,
		section = finalbossSection
	)
	default Color ocellusAugmenterColor()
	{
		return Color.ORANGE;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusRiftColor",
		name = "Void Rift color",
		description = "Color of the Void Rift",
		position = 13,
		section = finalbossSection
	)
	default Color ocellusRiftColor()
	{
		return Color.MAGENTA;
	}

	@ConfigItem(
		keyName = "ocellusVoidVine",
		name = "Highlight trapped players",
		description = "Highlight the tile of a player trapped by a Void Vine",
		position = 13,
		section = finalbossSection
	)
	default boolean ocellusVoidVine()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ocellusTrappedColor",
		name = "Trapped player color",
		description = "Color of a trapped player's tile",
		position = 13,
		section = finalbossSection
	)
	default Color ocellusTrappedColor()
	{
		return new Color(0, 255, 0);
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
