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
		name = "Moia",
		description = "Third encounter, paired with Dreadnoth: Moia",
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
		name = "Ocellus",
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
		position = 1,
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
		position = 2,
		section = generalSection
	)
	default int borderWidth()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "divineReminder",
		name = "Divine potion reminder",
		description = "A flashing \"Use Divine Potion\" box above the chat box for 10 ticks when a room's boss hits 0 hp (every boss but the Archive Guardian and Ocellus; all of Bork and his Smorks, all of the Dagon'hai Trio)",
		position = 5,
		section = generalSection
	)
	default boolean divineReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bossHp",
		name = "Boss hp",
		description = "A small hp over every boss, like Opponent Information: \"HP: 2940/3000 (98%)\" in green, or \"Shield: 540/550 (99%)\" in teal while its shield is up (Archive Guardian, Ocellus phase 1). Red within 5% above a phase threshold (10% for the Archive Guardian)",
		position = 3,
		section = generalSection
	)
	default boolean bossHp()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightTargets",
		name = "Highlight boss targets",
		description = "Highlight the true tile of the player each boss (and each Smork) is going after",
		position = 4,
		section = generalSection
	)
	default boolean highlightTargets()
	{
		return true;
	}

	// Archive Guardian

	@ConfigItem(
		keyName = "guardianAttackTicks",
		name = "Attack counter",
		description = "Count down the ticks after the Guardian's ranged attack, special attack and landing",
		position = 0,
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
		position = 1,
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
		position = 2,
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
		position = 0,
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
		position = 1,
		section = borkSection
	)
	default boolean borkStompCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "borkStompSafeSpot",
		name = "Highlight stomp safe spot",
		description = "From the stun until his 4 stomps finish, highlight the safe tiles right next to the Smork (green)",
		position = 5,
		section = borkSection
	)
	default boolean borkStompSafeSpot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "smorkProjectiles",
		name = "Projectile ticks",
		description = "Show the ticks until each Smork magic (cyan) or ranged (green) shot aimed at you hits, on the shot itself",
		position = 4,
		section = borkSection
	)
	default boolean smorkProjectiles()
	{
		return true;
	}

	@ConfigItem(
		keyName = "smorkStyleColors",
		name = "Color Smork counter by style",
		description = "Color each Smork's shot counter by its last shot: magic or ranged (red once a bomb is due)",
		position = 3,
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
		position = 6,
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
		position = 3,
		section = ezekielSection
	)
	default boolean ezekielSwordLanes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ezekielSwordCrossing",
		name = "Highlight four-sword crossing",
		description = "With four swords up, outline where all four of their lanes cross (orange, like the lanes)",
		position = 4,
		section = ezekielSection
	)
	default boolean ezekielSwordCrossing()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ezekielSwordTimer",
		name = "Sword counter",
		description = "Count down the ticks above each sword until it goes off",
		position = 2,
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
		position = 0,
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
		position = 1,
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
		position = 5,
		section = ezekielSection
	)
	default boolean ezekielStarCracks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ezekielFinaleFront",
		name = "Highlight finale dodge row",
		description = "During the finale, mark the far row of the 3x3 in front of Ezekiel (where to dodge to)",
		position = 6,
		section = ezekielSection
	)
	default boolean ezekielFinaleFront()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ezekielAutocastReminder",
		name = "Wind Surge reminder",
		description = "In Ezekiel's room on the standard spellbook: a box above the chat box when your harmonised nightmare staff's autocast (the last one set) isn't Wind Surge",
		position = 7,
		section = ezekielSection
	)
	default boolean ezekielAutocastReminder()
	{
		return true;
	}

	// Dreadnoth

	@ConfigItem(
		keyName = "dreadnothSplatter",
		name = "Highlight blood splatter",
		description = "Mark the blood splatter tiles Dreadnoth leaves on the ground",
		position = 5,
		section = dreadnothSection
	)
	default boolean dreadnothSplatter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dreadnothIncoming",
		name = "Highlight incoming blood",
		description = "Mark where Dreadnoth's blood will land while it's in the air",
		position = 4,
		section = dreadnothSection
	)
	default boolean dreadnothIncoming()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dreadnothIncomingTicks",
		name = "Incoming blood counter",
		description = "Count down the ticks on the tile until his blood lands",
		position = 2,
		section = dreadnothSection
	)
	default boolean dreadnothIncomingTicks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dreadnothRangedProjectiles",
		name = "Projectile ticks",
		description = "Show the ticks on Dreadnoth's ranged attack while it's aimed at you, like Projectile Ticks",
		position = 3,
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
		position = 0,
		section = dreadnothSection
	)
	default boolean dreadnothJumpCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dreadnothMoveTimer",
		name = "Movement counter",
		description = "Before 25% hp: a small count under Dreadnoth from each step he takes, until his next one",
		position = 1,
		section = dreadnothSection
	)
	default boolean dreadnothMoveTimer()
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
		name = "Highlight fireballs",
		description = "Mark where each of Xarth's fireballs will land (where a corpse spawns), with the ticks until it does (orange)",
		position = 3,
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
		position = 1,
		section = xarthSection
	)
	default int xarthVolleyTicks()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "xarthCorpseCountdown",
		name = "Corpse counter",
		description = "Over each corpse walking to Xarth, the ticks until it reaches him (cyan)",
		position = 2,
		section = xarthSection
	)
	default boolean xarthCorpseCountdown()
	{
		return true;
	}

	@ConfigItem(
		keyName = "xarthAutocastReminder",
		name = "Water Surge reminder",
		description = "In Xarth's room on the standard spellbook: a box above the chat box when your harmonised nightmare staff's autocast (the last one set) isn't Water Surge",
		position = 4,
		section = xarthSection
	)
	default boolean xarthAutocastReminder()
	{
		return true;
	}

	// Dagon'hai Trio

	@ConfigItem(
		keyName = "dagonhaiPortals",
		name = "Highlight imp portals",
		description = "Highlight the imp portals in the corners (purple), and the tile next to all of them to close them at once (green)",
		position = 1,
		section = dagonhaitrioSection
	)
	default boolean dagonhaiPortals()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dagonhaiElements",
		name = "Highlight elemental channels",
		description = "Highlight a wizard channelling an element, with its name: blue water, orange fire, white air, green earth",
		position = 2,
		section = dagonhaitrioSection
	)
	default boolean dagonhaiElements()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dagonhaiIncantations",
		name = "Highlight incantations",
		description = "Highlight the monk about to call on a god (\"Incantation\" at \"What have we here?\"), then which god: Zamorak, Bandos or Saradomin",
		position = 3,
		section = dagonhaitrioSection
	)
	default boolean dagonhaiIncantations()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dagonhaiShockwave",
		name = "Highlight Bandos shockwave",
		description = "The Bandos shockwave ring as it spreads: filled where it is now, outlined where it'll be next tick (brown)",
		position = 4,
		section = dagonhaitrioSection
	)
	default boolean dagonhaiShockwave()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dagonhaiLightning",
		name = "Highlight Saradomin lightning",
		description = "Saradomin's lightning strike tiles until they strike: yellow heals, blue hurts",
		position = 5,
		section = dagonhaitrioSection
	)
	default boolean dagonhaiLightning()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dagonhaiFlames",
		name = "Highlight Zamorak flames",
		description = "Zamorak's flame glyphs on the floor until they erupt (red)",
		position = 6,
		section = dagonhaitrioSection
	)
	default boolean dagonhaiFlames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dagonhaiTomePath",
		name = "Highlight tome path",
		description = "The path a god tome takes to the cauldron, where to stand to block it (in the god's color)",
		position = 7,
		section = dagonhaitrioSection
	)
	default boolean dagonhaiTomePath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dagonhaiElderCounter",
		name = "Elder attack counter",
		description = "Over the elder, the ticks until his attack lands (cyan magic, green ranged)",
		position = 0,
		section = dagonhaitrioSection
	)
	default boolean dagonhaiElderCounter()
	{
		return true;
	}

	// Necromancer (Moia)

	@ConfigItem(
		keyName = "moiaMagicCounter",
		name = "Projectile ticks",
		description = "On Moia's slow magic projectile aimed at you, the ticks until it hits (cyan)",
		position = 3,
		section = necromancerSection
	)
	default boolean moiaMagicCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "moiaCough",
		name = "Cough tracker",
		description = "Highlight everyone coughing (yellow tiles), and list the players with how many coughs they have left, from 4, in a box above the chat box",
		position = 6,
		section = necromancerSection
	)
	default boolean moiaCough()
	{
		return true;
	}

	@ConfigItem(
		keyName = "moiaBinders",
		name = "Highlight undead binders",
		description = "Highlight each undead binder's tile: white, or green for the one on the centre spot of their grid",
		position = 4,
		section = necromancerSection
	)
	default boolean moiaBinders()
	{
		return true;
	}

	@ConfigItem(
		keyName = "moiaEnrage",
		name = "Enrage charge counter",
		description = "Over Moia during her enrage, the ticks until each charge finishes (orange)",
		position = 1,
		section = necromancerSection
	)
	default boolean moiaEnrage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "moiaEnrageAttacks",
		name = "Enrage attack counter",
		description = "In the enrage: ticks until her next attack over Moia (every 5 ticks, green), with the enrage charge count smaller under it",
		position = 2,
		section = necromancerSection
	)
	default boolean moiaEnrageAttacks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "moiaNextSpecial",
		name = "Next special",
		description = "Under Moia, her next special before she enrages: Magic or Cough (they go magic, magic, cough)",
		position = 0,
		section = necromancerSection
	)
	default boolean moiaNextSpecial()
	{
		return true;
	}

	@ConfigItem(
		keyName = "moiaBinderSpots",
		name = "Highlight binder spots",
		description = "Mark the 9 spots the undead binders stand on, numbered in the order they walk them (until she enrages)",
		position = 5,
		section = necromancerSection
	)
	default boolean moiaBinderSpots()
	{
		return true;
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
		name = "Combo counter",
		description = "Below the tick counter in phase 3: which of the two combos before his special (1 or 2), blue for the prayer turn-off or orange with Untether/Insolence",
		position = 1,
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
		position = 5,
		section = finalbossSection
	)
	default boolean ocellusObliterationCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusTeleportCounter",
		name = "Teleport counter",
		description = "Phase 1: when he starts teleporting to the middle, count down 5 ticks to him appearing there (in purple). Not when he goes there to transform into phase 2",
		position = 3,
		section = finalbossSection
	)
	default boolean ocellusTeleportCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusDeflectCounter",
		name = "Deflect counter",
		description = "Phase 4: count down the deflect under his attack counter (green = ranged, red = melee)",
		position = 2,
		section = finalbossSection
	)
	default boolean ocellusDeflectCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusVoidChain",
		name = "Void chain counter",
		description = "Phase 4: when you get the void chain warning, count down to each of its 6 explosions over your character (purple)",
		position = 7,
		section = finalbossSection
	)
	default boolean ocellusVoidChain()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusProjectiles",
		name = "Projectile ticks",
		description = "Show the ticks until Ocellus's projectiles land: on the projectile if it's aimed at you (like Projectile Ticks), on the landing tile if it's thrown at the ground",
		position = 8,
		section = finalbossSection
	)
	default boolean ocellusProjectiles()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusGroundMarkers",
		name = "Highlight dangerous floor",
		description = "Mark dangerous floor (charge cracks, explosions, AoEs, bombs, debris, void puddles)",
		position = 12,
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
		position = 13,
		section = finalbossSection
	)
	default boolean ocellusArtifacts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusPortalCrossing",
		name = "Highlight portal crossing",
		description = "Phase 2: highlight the 2x2 where the orange portals' lanes cross as soon as the portals fire, and the safe portal tile the game puts on the floor",
		position = 10,
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
		position = 14,
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
		position = 15,
		section = finalbossSection
	)
	default boolean ocellusVoidPortals()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusQuadrantSpawns",
		name = "Highlight quadrant landing spots",
		description = "Phase 3: outline where Ocellus lands in each quadrant (white)",
		position = 11,
		section = finalbossSection
	)
	default boolean ocellusQuadrantSpawns()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusStartQuadrant",
		name = "Highlight start quadrant",
		description = "Mark where the fight starts: his landing spot in the quadrant diagonally opposite the Bandosian Censer, with Start on it, until the portal phase",
		position = 9,
		section = finalbossSection
	)
	default boolean ocellusStartQuadrant()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusAutocastReminder",
		name = "Earth Surge reminder",
		description = "Against Ocellus on the standard spellbook: a box above the chat box when your harmonised nightmare staff's autocast (the last one set) isn't Earth Surge",
		position = 18,
		section = finalbossSection
	)
	default boolean ocellusAutocastReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusSpecialQueue",
		name = "Special queue",
		description = "Phase 3: his next two specials in a box above the chat box, red whenever the prayer turn-off is next",
		position = 17,
		section = finalbossSection
	)
	default boolean ocellusSpecialQueue()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusVoidDropWarning",
		name = "Void drop counter",
		description = "After each quadrant move: the ticks until the void explodes (purple). It follows you from 18 ticks after the line and drops 2 ticks after you stop: from 21 ticks after the line, the count shows on your tile once you stop",
		position = 6,
		section = finalbossSection
	)
	default boolean ocellusVoidDropWarning()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ocellusVoidVine",
		name = "Highlight trapped players",
		description = "Highlight the tile of a player trapped by a Void Vine",
		position = 16,
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
