package net.novucs.ftop.newdelayedspawners;

import com.google.common.collect.ImmutableMap;
import net.novucs.ftop.FactionsTopPlugin;
import net.novucs.ftop.PluginService;
import net.novucs.ftop.RecalculateReason;
import net.novucs.ftop.WorthType;
import net.novucs.ftop.entity.ChunkPos;
import net.novucs.ftop.hook.event.FactionClaimEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NewDelayedSpawners implements PluginService, Listener {

	private final FactionsTopPlugin plugin;

	// Maps a delayed spawner to the time it was placed
	private final Map<NewDelayedSpawner, Long> activationMap = new ConcurrentHashMap<>();

	// Maps the time elapsed (as sort of a step function) to how much value
	private final TreeMap<Long, Double> incrementalWorthMap = new TreeMap<>();

	// The entry of representing when the spawner has its full value
	private final Map.Entry<Long, Double> lastEntry;

	public NewDelayedSpawners(FactionsTopPlugin plugin) {
		this.plugin = plugin;
		this.incrementalWorthMap.put(0L, 0D);
//		this.incrementalWorthMap.put(TimeUnit.HOURS.toMillis(12), 0.15);
//		this.incrementalWorthMap.put(TimeUnit.HOURS.toMillis(24), 0.5);
//		this.incrementalWorthMap.put(TimeUnit.HOURS.toMillis(36), 0.8);
//		this.incrementalWorthMap.put(TimeUnit.HOURS.toMillis(48), 1.0);

		this.incrementalWorthMap.put(TimeUnit.SECONDS.toMillis(11), 0.15);
		this.incrementalWorthMap.put(TimeUnit.SECONDS.toMillis(15), 0.5);
		this.incrementalWorthMap.put(TimeUnit.SECONDS.toMillis(20), 0.8);
		this.incrementalWorthMap.put(TimeUnit.SECONDS.toMillis(25), 1.0);

		this.lastEntry = incrementalWorthMap.lastEntry();
	}

	@Override
	public void initialize() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new DelayedActivationTask(this),
				20, 20);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on(FactionClaimEvent event) {
		boolean claiming = !plugin.getSettings().getIgnoredFactionIds().contains(event.getFactionId());

		NewDelayedSpawners spawners = plugin.getNewDelayedSpawners();
		event.getClaims().asMap().forEach((oldFacId, claims) -> {
			claims.forEach(claim -> {
				if (claiming) {
					World world = Bukkit.getWorld(claim.getWorld());
					world.getChunkAtAsync(claim.getX(), claim.getZ(), chunk -> {
						plugin.getWorthManager().getChunks().get(claim).setSpawners(new HashMap<>());
						plugin.getWorthManager().set(claim, WorthType.SPAWNER, 0);

						Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
							Arrays.stream(chunk.getTileEntities())
									.filter(state -> state instanceof CreatureSpawner)
									.peek(System.out::println)
									.forEach(state -> spawners.addSpawner((CreatureSpawner) state, true));
						});
					});
				}
			});
		});
	}

	public double getInstantaneousWorth(CreatureSpawner spawner) {
		NewDelayedSpawner delayedSpawner = NewDelayedSpawner.of(spawner);
		Long timestampPlaced = activationMap.get(delayedSpawner);
		double spawnerPrice = plugin.getSettings().getSpawnerPrice(spawner.getSpawnedType());

		if (timestampPlaced == null)
			return spawnerPrice;

		long timeSincePlaced = System.currentTimeMillis() - timestampPlaced;
		return incrementalWorthMap.floorEntry(timeSincePlaced).getValue() * spawnerPrice;
	}

	void updateWorth(NewDelayedSpawner spawner, Map.Entry<Long, Double> valueTier) {
		Chunk chunk = spawner.getLocation().getBlock(plugin.getServer()).getChunk();
		double price = plugin.getSettings().getSpawnerPrice(spawner.getSpawnerType());
		double previousPrice = Optional
				.ofNullable(incrementalWorthMap.lowerEntry(valueTier.getKey()))
				.map(Map.Entry::getValue)
				.orElse(0D) * price;
		double newPrice = price * valueTier.getValue();

		plugin.getWorthManager().add(chunk, RecalculateReason.DELAYED_SPAWNER, WorthType.SPAWNER,
				newPrice - previousPrice, ImmutableMap.of(), ImmutableMap.of());
	}

	public void removeSpawner(CreatureSpawner spawner) {
		activationMap.remove(NewDelayedSpawner.of(spawner));
	}

	public void addSpawner(CreatureSpawner spawner) {
		addSpawner(spawner, false);
	}

	public void addSpawner(CreatureSpawner spawner, boolean addSpawners) {
		String faction = plugin.getFactionsHook().getFactionAt(ChunkPos.of(spawner.getChunk()));
		if (plugin.getSettings().getIgnoredFactionIds().contains(faction))
			return;

		activationMap.put(NewDelayedSpawner.of(spawner), System.currentTimeMillis());

		if (addSpawners) {
			plugin.getWorthManager().add(spawner.getChunk(), RecalculateReason.DELAYED_SPAWNER, WorthType.SPAWNER,
					getInstantaneousWorth(spawner), ImmutableMap.of(), ImmutableMap.of(spawner.getSpawnedType(), 1));
		}
	}

	public double getPotentialWorth(String factionId) {
		return plugin.getWorthManager().getWorth(factionId).getSpawners().entrySet().stream()
				.mapToDouble(entry -> plugin.getSettings().getSpawnerPrice(entry.getKey()) * entry.getValue())
				.sum();
	}

	public FactionsTopPlugin getPlugin() {
		return plugin;
	}

	public TreeMap<Long, Double> getIncrementalWorthMap() {
		return incrementalWorthMap;
	}

	public Map<NewDelayedSpawner, Long> getActivationMap() {
		return activationMap;
	}

	public Map.Entry<Long, Double> getLastIncrementEntry() {
		return lastEntry;
	}

	@Override
	public void terminate() {

	}

}
