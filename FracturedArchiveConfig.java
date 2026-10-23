package net.runelite.client.plugins.fracturedarchive;

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

	@ConfigItem(
		keyName = "borkStompCounter",
		name = "Stomp counter",
		description = "Count down to each of Bork's 4 stomps, 3 ticks apart (in red)",
		position = 2,
		section = borkSection
	)
	default boolean borkStompCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "borkStompSafeSpot",
		name = "Stomp safe spot",
		description = "From the stun until his 4 stomps finish, highlight the safe tiles right next to the Smork (green)",
		position = 2,
		section = borkSection
	)
	default boolean borkStompSafeSpot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "smorkProjectiles",
		name = "Smork projectiles",
		description = "Show the ticks until each Smork magic (cyan) or ranged (green) shot aimed at you hits, on the shot itself",
		position = 2,
		section = borkSection
	)
	default boolean smorkProjectiles()
	{
		return true;
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

	@ConfigItem(
		keyName = "smorkFireBombs",
		name = "Highlight fire bombs",
		description = "Mark where each fire bomb and big cannonball will land while it's in the air, then the fire while it burns",
		position = 11,
		section = borkSection
	)
	default boolean smorkFireBombs()
	{
		return true;
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

	@ConfigItem(
		keyName = "ezekielStarCounter",
		name = "Star counter",
		description = "Below the tick counter, count Ezekiel's basic attacks 1, 2, then 3 on the star special, then hide",
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

	@ConfigItem(
		keyName = "xarthFireballs",
		name = "Fireballs",
		description = "Mark where each of Xarth's fireballs will land (where a corpse spawns), with the ticks until it does (orange)",
		position = 1,
		section = xarthSection
	)
	default boolean xarthFireballs()
	{
		return true;
	}

	@Range(min = 1, max = 20)
	@ConfigItem(
		keyName = "xarthVolleyTicks",
		name = "Corpse volley ticks",
		description = "What the attack counter starts at when Xarth throws his first corpse fireball (more corpses in bigger parties take longer)",
		position = 2,
		section = xarthSection
	)
	default int xarthVolleyTicks()
	{
		return 6;
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

	@ConfigItem(
		keyName = "ocellusSweepCounter",
		name = "Sweep counter",
		description = "Below the tick counter, count Ocellus's basic attacks in phase 3: 1, 2, then 3 on the sweep, then hide",
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
		keyName = "ocellusObliterationCounter",
		name = "Obliteration counter",
		description = "When he says Obliteration or Run, count down 15 ticks (in purple)",
		position = 4,
		section = finalbossSection
	)
	default boolean ocellusObliterationCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusDeflectCounter",
		name = "Deflect counter",
		description = "Phase 4: count down the deflect under his attack counter (green = ranged, red = melee)",
		position = 4,
		section = finalbossSection
	)
	default boolean ocellusDeflectCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusVoidChain",
		name = "Void chain",
		description = "Phase 4: when you get the void chain warning, count down to each of its 6 explosions over your character (teal)",
		position = 4,
		section = finalbossSection
	)
	default boolean ocellusVoidChain()
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

	@ConfigItem(
		keyName = "ocellusPortalCrossing",
		name = "Portal crossing",
		description = "Phase 2: highlight the 2x2 where the orange portals' lanes cross as soon as the portals fire, and the safe portal tile the game puts on the floor",
		position = 13,
		section = finalbossSection
	)
	default boolean ocellusPortalCrossing()
	{
		return true;
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
