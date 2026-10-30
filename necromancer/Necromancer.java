package net.runelite.client.plugins.fracturedarchive.necromancer;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.fracturedarchive.FightLogger;
import net.runelite.client.plugins.fracturedarchive.Encounter;
import net.runelite.client.plugins.fracturedarchive.EncounterOverlay;
import net.runelite.client.plugins.fracturedarchive.FracturedArchiveConfig;

/**
 * Necromancer. Third encounter, paired with Dreadnoth. Name not yet revealed.
 */
@Singleton
public class Necromancer extends Encounter
{
	// The necromancer is Moia (TFA_MOIA) in the game data. Related: TFA_MOIA_MINION
	private static final Set<Integer> BOSS_IDS = Set.of(NpcID.TFA_MOIA);
	// TODO: map region ID isn't in the game data; read it in game (Developer Tools > Location) if needed.
	static final int REGION_ID = -1;

	@Inject
	private NecromancerOverlay overlay;

	/** Not mapped yet: log the whole fight (every NPC of it) to work its attacks out later. */
	private static final Set<Integer> LOG_IDS = Set.of(NpcID.TFA_MOIA, NpcID.TFA_MOIA_MINION);
	private FightLogger logger;

	@Inject
	protected Necromancer(FracturedArchiveConfig config)
	{
		super(config);
	}

	@Override
	public void init()
	{
		logger = new FightLogger(client, "Necromancer", LOG_IDS);
	}

	@Override
	protected Set<Integer> bossIds()
	{
		return BOSS_IDS;
	}

	@Override
	protected EncounterOverlay overlay()
	{
		return overlay;
	}

	/** Her slow magic projectile (lands 6 ticks after it's thrown) and the fast ranged one. */
	static final int MAGIC_PROJECTILE = 4213;
	static final int RANGED_PROJECTILE = 4214;
	/** The cough (smoke cloud) projectile. */
	static final int COUGH_PROJECTILE = 4218;
	/**
	 * The cough: whoever it hits coughs every 8 ticks (the first 3 ticks after it's thrown), and it
	 * spreads to whoever's next to them. Each one keeps coughing until a cough hits them for 5, so
	 * how long it lasts varies. The timer over each is the ticks until their next cough.
	 */
	static final int COUGH_GAP = 8;
	private static final int FIRST_COUGH_TICKS = 3;
	/** At most 4 coughs each (it can end sooner, on a cough that hits for 5). */
	static final int MAX_COUGHS = 4;

	private final Set<net.runelite.api.Projectile> magicProjectiles = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
	private final Set<net.runelite.api.Projectile> seen = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
	/** Everyone coughing and the tick of their last (or, for a fresh hit, an imagined) cough. */
	private final java.util.Map<net.runelite.api.Actor, Integer> lastCough = new java.util.WeakHashMap<>();
	/** The tick each one's cough ended, so the *Cough* on that same tick doesn't restart it. */
	private final java.util.Map<net.runelite.api.Actor, Integer> coughEnded = new java.util.WeakHashMap<>();
	/** How many times each has coughed so far. */
	private final java.util.Map<net.runelite.api.Actor, Integer> coughCount = new java.util.WeakHashMap<>();
	/** When each caught it, and when each was last reinfected. */
	private final java.util.Map<net.runelite.api.Actor, Integer> infectedTick = new java.util.WeakHashMap<>();
	private final java.util.Map<net.runelite.api.Actor, Integer> resetTick = new java.util.WeakHashMap<>();

	Set<net.runelite.api.Projectile> getMagicProjectiles()
	{
		magicProjectiles.removeIf(p -> p.getRemainingCycles() <= 0);
		return magicProjectiles;
	}

	/** Everyone coughing, with how many coughs they have left. */
	java.util.Map<net.runelite.api.Actor, String> getCoughLabels()
	{
		java.util.Map<net.runelite.api.Actor, String> labels = new java.util.HashMap<>();
		for (java.util.Map.Entry<net.runelite.api.Actor, Integer> e : getCoughTicks().entrySet())
		{
			labels.put(e.getKey(), Integer.toString(MAX_COUGHS - coughCount.getOrDefault(e.getKey(), 0)));
		}
		return labels;
	}

	/** Everyone coughing, with the ticks until their next cough. */
	java.util.Map<net.runelite.api.Actor, Integer> getCoughTicks()
	{
		int tick = client.getTickCount();
		// Forget anyone who's missed a cough (gone, or it ended without us seeing the 5)
		lastCough.values().removeIf(last -> tick - last > COUGH_GAP + 2);
		java.util.Map<net.runelite.api.Actor, Integer> ticks = new java.util.HashMap<>();
		for (java.util.Map.Entry<net.runelite.api.Actor, Integer> e : lastCough.entrySet())
		{
			// +1: each cough shows up at the very end of its tick, so the first number shown is 8
			int next = COUGH_GAP + 1 - (tick - e.getValue());
			if (next > 0)
			{
				ticks.put(e.getKey(), next);
			}
		}
		return ticks;
	}

	/**
	 * Enrage (33%): she charges ("Useless corpses... Give me your strength!", then "Again!" for
	 * each next one), and each charge lasts 2 ticks less than the one before: 41, 39, 37...
	 */
	private static final int FIRST_CHARGE_TICKS = 41;
	/** Her teleport into the magic attack, and the cough throw. */
	private static final int MAGIC_SPECIAL_ANIM = 14788;
	private static final int COUGH_SPECIAL_ANIM = 14786;
	private static final int CHARGE_STEP = 2;
	private int chargeStartTick = -1;
	private int chargeLength;
	private int charges;

	/**
	 * Before the enrage her specials go magic, magic, cough, and repeat: count them to know which
	 * is next. Hidden once she enrages.
	 */
	private int specials;
	private boolean enraged;

	/** Her next special: true for the cough, false for magic (null once she's enraged). */
	Boolean isCoughNext()
	{
		return enraged ? null : specials % 3 == 2;
	}

	/** Where Moia starts: the binders' grid of spots is east of here, centred 8 east and 1 north. */
	private net.runelite.api.coords.WorldPoint moiaStart;

	/** The middle spot of the binders' grid. */
	net.runelite.api.coords.WorldPoint getBinderGridCenter()
	{
		return moiaStart == null ? null : moiaStart.dx(8).dy(1);
	}

	/** Ticks until her enrage charge finishes (0 = not charging). */
	int getChargeTicks()
	{
		if (chargeStartTick < 0)
		{
			return 0;
		}
		return Math.max(0, chargeLength - (client.getTickCount() - chargeStartTick));
	}

	/** The undead binders still standing. */
	java.util.List<net.runelite.api.NPC> getBinders()
	{
		java.util.List<net.runelite.api.NPC> binders = new java.util.ArrayList<>();
		net.runelite.api.WorldView wv = client.getTopLevelWorldView();
		if (wv != null)
		{
			for (net.runelite.api.NPC npc : wv.npcs())
			{
				if (npc.getId() == NpcID.TFA_MOIA_MINION && !npc.isDead())
				{
					binders.add(npc);
				}
			}
		}
		return binders;
	}

	@Override
	protected void reset()
	{
		super.reset();
		// Moia vanishes for about 10 ticks when she enrages (at 33%), which resets the encounter:
		// keep the coughs going through that (anyone who's stopped coughing drops off by itself)
		magicProjectiles.clear();
	}

	@Override
	public void unload()
	{
		super.unload();
		logger.reset();
		lastCough.clear();
		coughEnded.clear();
		coughCount.clear();
		infectedTick.clear();
		resetTick.clear();
		chargeStartTick = -1;
		charges = 0;
		specials = 0;
		enraged = false;
		moiaStart = null;
	}

	/** In the fight: Moia's up, or she's vanished mid-fight to enrage. */
	boolean isFightActive()
	{
		return isActive() || logger.isActive();
	}

	@Override
	public void onGameTick(GameTick event)
	{
		logger.onGameTick();
		if (!logger.isActive())
		{
			// Out of the room: start the next fight fresh
			moiaStart = null;
			enraged = false;
			specials = 0;
			charges = 0;
			chargeStartTick = -1;
		}
	}

	@Override
	public void onNpcSpawned(NpcSpawned event)
	{
		super.onNpcSpawned(event);
		logger.onNpcSpawned(event);
		net.runelite.api.NPC npc = event.getNpc();
		if (npc.getId() == NpcID.TFA_MOIA && moiaStart == null)
		{
			// Where she starts (she reappears somewhere else when she enrages)
			moiaStart = npc.getWorldLocation();
		}

	}

	@Override
	public void onNpcDespawned(NpcDespawned event)
	{
		super.onNpcDespawned(event);
		logger.onNpcDespawned(event);
	}

	@Override
	public void onNpcChanged(NpcChanged event)
	{
		super.onNpcChanged(event);
		logger.onNpcChanged(event);
	}

	@Override
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor() instanceof net.runelite.api.NPC
			&& ((net.runelite.api.NPC) event.getActor()).getId() == NpcID.TFA_MOIA)
		{
			int anim = event.getActor().getAnimation();
			if (anim == MAGIC_SPECIAL_ANIM || anim == COUGH_SPECIAL_ANIM)
			{
				// Keep in step even if one was missed: a cough is always the 3rd
				specials = anim == COUGH_SPECIAL_ANIM ? 0 : specials + 1;
			}
		}
		if (event.getActor() instanceof net.runelite.api.NPC
			&& ((net.runelite.api.NPC) event.getActor()).getId() == NpcID.TFA_MOIA
			&& event.getActor().getAnimation() == net.runelite.api.gameval.AnimationID.MOIA_CHANT01)
		{
			chargeLength = FIRST_CHARGE_TICKS - CHARGE_STEP * charges;
			charges++;
			chargeStartTick = client.getTickCount();
		}
		logger.onAnimationChanged(event);
	}

	@Override
	public void onProjectileMoved(ProjectileMoved event)
	{
		net.runelite.api.Projectile p = event.getProjectile();
		if (seen.add(p))
		{
			if (p.getId() == MAGIC_PROJECTILE)
			{
				magicProjectiles.add(p);
			}
			else if (p.getId() == COUGH_PROJECTILE && p.getTargetActor() != null)
			{
				// First cough 3 ticks after it's thrown
				lastCough.put(p.getTargetActor(), client.getTickCount() + FIRST_COUGH_TICKS - COUGH_GAP);
				coughCount.put(p.getTargetActor(), 0);
				infectedTick.put(p.getTargetActor(), client.getTickCount());
			}
		}
		logger.onProjectileMoved(event);
	}

	@Override
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		logger.onGraphicsObjectCreated(event);
	}

	@Override
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		logger.onGameObjectSpawned(event);
	}

	@Override
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		logger.onGameObjectDespawned(event);
	}

	@Override
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		logger.onGroundObjectSpawned(event);
	}

	@Override
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		logger.onGroundObjectDespawned(event);
	}

	@Override
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (event.getActor() instanceof net.runelite.api.NPC
			&& ((net.runelite.api.NPC) event.getActor()).getId() == NpcID.TFA_MOIA && event.getOverheadText() != null)
		{
			String said = event.getOverheadText();
			if (said.startsWith("Cease this futile"))
			{
				// Enraging: the charges start from the first again, and no more specials
				charges = 0;
				chargeStartTick = -1;
				enraged = true;
			}
			else if (said.startsWith("Hmph"))
			{
				// Dead
				chargeStartTick = -1;
			}
		}

		// Each cough (including anyone it's spread to, binders too)
		if ("*Cough*".equals(event.getOverheadText()) && event.getActor() != null
			&& !Integer.valueOf(client.getTickCount()).equals(coughEnded.get(event.getActor())))
		{
			int tick = client.getTickCount();
			net.runelite.api.Actor actor = event.getActor();
			if (!lastCough.containsKey(actor))
			{
				coughCount.put(actor, 0);
				infectedTick.put(actor, tick);
			}

			// A cough reinfects anyone already coughing next to it (a binder that caught it from
			// you gives it straight back): their 4 start again after this tick
			net.runelite.api.coords.WorldPoint at = actor.getWorldLocation();
			for (net.runelite.api.Actor other : new java.util.ArrayList<>(lastCough.keySet()))
			{
				if (other == actor || tick == infectedTick.getOrDefault(other, -1))
				{
					continue;
				}
				net.runelite.api.coords.WorldPoint p = other.getWorldLocation();
				if (at != null && p != null && p.getPlane() == at.getPlane()
					&& Math.max(Math.abs(p.getX() - at.getX()), Math.abs(p.getY() - at.getY())) <= 1)
				{
					coughCount.put(other, 0);
					resetTick.put(other, tick);
				}
			}

			int count = tick == resetTick.getOrDefault(actor, -1) ? 0 : coughCount.merge(actor, 1, Integer::sum);
			if (count >= MAX_COUGHS)
			{
				// That was the last one
				lastCough.remove(actor);
				coughCount.remove(actor);
				coughEnded.put(actor, tick);
			}
			else
			{
				lastCough.put(actor, tick);
			}
		}
		logger.onOverheadTextChanged(event);
	}

	@Override
	public void onChatMessage(ChatMessage event)
	{
		logger.onChatMessage(event);
	}

	@Override
	public void onVarbitChanged(VarbitChanged event)
	{
		logger.onVarbitChanged(event);
	}

	@Override
	public void onHitsplatApplied(net.runelite.api.events.HitsplatApplied event)
	{
		logger.onHitsplatApplied(event);
	}
}
