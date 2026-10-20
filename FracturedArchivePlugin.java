package net.runelite.client.plugins.fracturedarchive;

import com.google.inject.Provides;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
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
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.plugins.fracturedarchive.archiveguardian.ArchiveGuardian;
import net.runelite.client.plugins.fracturedarchive.bork.Bork;
import net.runelite.client.plugins.fracturedarchive.dagonhaitrio.DagonhaiTrio;
import net.runelite.client.plugins.fracturedarchive.dreadnoth.Dreadnoth;
import net.runelite.client.plugins.fracturedarchive.ezekiel.Ezekiel;
import net.runelite.client.plugins.fracturedarchive.finalboss.FinalBoss;
import net.runelite.client.plugins.fracturedarchive.necromancer.Necromancer;
import net.runelite.client.plugins.fracturedarchive.xarth.Xarth;

@PluginDescriptor(
	name = "<html><font color=#b82584>[J] Fractured Archive",
	description = "All-in-one plugin for the Fractured Archive",
	tags = {"raids", "raid", "fractured", "archive", "guardian", "bork", "ezekiel", "dreadnoth", "necromancer", "xarth", "dagonhai"},
	enabledByDefault = false
)
@Slf4j
public class FracturedArchivePlugin extends Plugin
{
	static final String CONFIG_GROUP = "fracturedarchive";

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TargetOverlay targetOverlay;

	@Inject
	private ArchiveGuardian archiveGuardian;

	@Inject
	private Bork bork;

	@Inject
	private Ezekiel ezekiel;

	@Inject
	private Dreadnoth dreadnoth;

	@Inject
	private Necromancer necromancer;

	@Inject
	private Xarth xarth;

	@Inject
	private DagonhaiTrio dagonhaiTrio;

	@Inject
	private FinalBoss finalBoss;

	/**
	 * Every encounter, in raid order. Each run is the Archive Guardian, one boss from each of the
	 * three pairs (Bork/Ezekiel, Dreadnoth/Necromancer, Xarth/Dagon'hai Trio), then the final boss.
	 */
	private List<Encounter> encounters;

	@Provides
	FracturedArchiveConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FracturedArchiveConfig.class);
	}

	@Override
	protected void startUp()
	{
		if (encounters == null)
		{
			encounters = List.of(archiveGuardian, bork, ezekiel, dreadnoth, necromancer, xarth, dagonhaiTrio, finalBoss);
			encounters.forEach(Encounter::init);
		}

		encounters.forEach(Encounter::load);
		overlayManager.add(targetOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(targetOverlay);
		if (encounters != null)
		{
			encounters.forEach(Encounter::unload);
		}
	}

	// Game events are received here once and handed to every encounter. Each encounter
	// ignores anything that isn't its own, so a new handler only needs adding in Encounter
	// and below.

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		encounters.forEach(e -> e.onNpcSpawned(event));
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		encounters.forEach(e -> e.onNpcDespawned(event));
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		encounters.forEach(e -> e.onNpcChanged(event));
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		encounters.forEach(e -> e.onGameTick(event));
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		encounters.forEach(e -> e.onAnimationChanged(event));
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		encounters.forEach(e -> e.onProjectileMoved(event));
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		encounters.forEach(e -> e.onGraphicsObjectCreated(event));
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		encounters.forEach(e -> e.onGameObjectSpawned(event));
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		encounters.forEach(e -> e.onGameObjectDespawned(event));
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		encounters.forEach(e -> e.onGroundObjectSpawned(event));
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		encounters.forEach(e -> e.onGroundObjectDespawned(event));
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		encounters.forEach(e -> e.onVarbitChanged(event));
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		encounters.forEach(e -> e.onGameStateChanged(event));
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		encounters.forEach(e -> e.onOverheadTextChanged(event));
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		encounters.forEach(e -> e.onChatMessage(event));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CONFIG_GROUP.equals(event.getGroup()))
		{
			encounters.forEach(e -> e.onConfigChanged(event));
		}
	}
}
