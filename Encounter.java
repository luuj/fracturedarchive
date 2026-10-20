package net.runelite.client.plugins.fracturedarchive;

import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Base class for one boss encounter, like Theatre's Room. It tracks whether the boss is
 * spawned and adds/removes the encounter's overlay. Subclasses fill in their NPC IDs and
 * override only the event handlers they need.
 */
public abstract class Encounter
{
	protected final FracturedArchiveConfig config;

	@Inject
	protected Client client;

	@Inject
	protected OverlayManager overlayManager;

	/** The boss NPC while it's spawned, or null. */
	@Getter
	protected NPC boss;

	protected Encounter(FracturedArchiveConfig config)
	{
		this.config = config;
	}

	/** NPC IDs of the boss (every form/phase). Empty until the raid's IDs are known. */
	protected abstract Set<Integer> bossIds();

	/** The overlay for this encounter, or null if it doesn't draw anything. */
	protected abstract EncounterOverlay overlay();

	/** Called once, the first time the plugin starts. */
	public void init()
	{
	}

	/** Called every time the plugin starts. */
	public void load()
	{
		if (overlay() != null)
		{
			overlayManager.add(overlay());
		}
	}

	/** Called every time the plugin stops. */
	public void unload()
	{
		if (overlay() != null)
		{
			overlayManager.remove(overlay());
		}
		reset();
	}

	/** Clears all state for this encounter. Subclasses should call super.reset(). */
	protected void reset()
	{
		boss = null;
	}

	public boolean isActive()
	{
		return boss != null;
	}

	/** The player the boss is going after: by default whoever it's attacking. */
	public Player getTarget()
	{
		if (boss != null && !boss.isDead() && boss.getInteracting() instanceof Player)
		{
			return (Player) boss.getInteracting();
		}
		return null;
	}

	protected boolean inRegion(int regionId)
	{
		WorldView wv = client.getTopLevelWorldView();
		return wv != null && ArrayUtils.contains(wv.getMapRegions(), regionId);
	}

	// Event handlers, called by the plugin. Overrides of onNpcSpawned/onNpcDespawned should
	// call super so boss tracking keeps working.

	public void onNpcSpawned(NpcSpawned event)
	{
		if (bossIds().contains(event.getNpc().getId()))
		{
			boss = event.getNpc();
		}
	}

	public void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc() == boss)
		{
			reset();
		}
	}

	/**
	 * Bosses change form mid-fight (shielded/unshielded, phases) by changing the existing NPC.
	 * Start tracking when an NPC turns into one of the boss forms, and stop when the boss turns
	 * into something else (for example its dead form). Overrides should call super.
	 */
	public void onNpcChanged(NpcChanged event)
	{
		NPC npc = event.getNpc();
		if (bossIds().contains(npc.getId()))
		{
			boss = npc;
		}
		else if (npc == boss)
		{
			reset();
		}
	}

	public void onGameTick(GameTick event)
	{
	}

	public void onAnimationChanged(AnimationChanged event)
	{
	}

	public void onProjectileMoved(ProjectileMoved event)
	{
	}

	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
	}

	public void onGameObjectSpawned(GameObjectSpawned event)
	{
	}

	public void onGameObjectDespawned(GameObjectDespawned event)
	{
	}

	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
	}

	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
	}

	public void onVarbitChanged(VarbitChanged event)
	{
	}

	public void onGameStateChanged(GameStateChanged event)
	{
	}

	public void onConfigChanged(ConfigChanged event)
	{
	}

	public void onOverheadTextChanged(OverheadTextChanged event)
	{
	}

	public void onChatMessage(ChatMessage event)
	{
	}
}
