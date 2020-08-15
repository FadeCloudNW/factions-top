package net.novucs.ftop.delayedspawners;

import com.google.common.collect.ImmutableMap;
import net.novucs.ftop.FactionsTopPlugin;
import net.novucs.ftop.PluginService;
import net.novucs.ftop.RecalculateReason;
import net.novucs.ftop.WorthType;
import org.bukkit.Bukkit;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DelayedSpawnerService implements PluginService {

	private final FactionsTopPlugin plugin;

	// Maps a delayed spawner to the time it was placed
	private final Map<DelayedSpawner, Long> activationMap = new ConcurrentHashMap<>();

	// Maps the time elapsed (as sort of a step function) to how much value
	private final TreeMap<Long, Double> incrementalWorthMap = new TreeMap<>();

	public DelayedSpawnerService(FactionsTopPlugin plugin) {
		this.plugin = plugin;
		this.incrementalWorthMap.put(0L, 0.0);
		this.incrementalWorthMap.put(TimeUnit.HOURS.toMillis(12), 0.15);
		this.incrementalWorthMap.put(TimeUnit.HOURS.toMillis(24), 0.5);
		this.incrementalWorthMap.put(TimeUnit.HOURS.toMillis(36), 0.8);
		this.incrementalWorthMap.put(TimeUnit.HOURS.toMillis(48), 1.0);
	}

	@Override
	public void initialize() {
		SpawnerActivationTask activationTask = new SpawnerActivationTask(this);
		DelayedSpawnerPersistence persistence = new DelayedSpawnerPersistence(this);
		activationMap.putAll(persistence.load());

		Bukkit.getPluginManager().registerEvents(new DelayedSpawnerListener(this), getPlugin());
		Bukkit.getScheduler().runTaskTimerAsynchronously(getPlugin(), activationTask, 20, 20);
		Bukkit.getScheduler().runTaskTimerAsynchronously(getPlugin(), persistence, 20 * 15, 20 * 15);
	}

	public double getInstantWorth(CreatureSpawner spawner) {
		return getWorthTier(DelayedSpawner.of(spawner)).getValue()
				* getSpawnerWorth(spawner.getSpawnedType());
	}

	void addIncrementalValue(DelayedSpawner spawner,
	                         Map.Entry<Long, Double> currentWorth,
	                         Map.Entry<Long, Double> previousWorth) {
		double worth = getSpawnerWorth(spawner.getSpawnerType());
		double increment = (currentWorth.getValue() * worth) - (previousWorth.getValue() * worth);

		plugin.getWorthManager().add(spawner.getChunk().getChunk(plugin.getServer()), RecalculateReason.DELAYED_SPAWNER,
				WorthType.SPAWNER, increment, ImmutableMap.of(), ImmutableMap.of());
	}

	private Map.Entry<Long, Double> getWorthTier(DelayedSpawner spawner) {
		return Optional.ofNullable(activationMap.get(spawner))
				.map(placed -> System.currentTimeMillis() - placed)
				.map(incrementalWorthMap::floorEntry)
				.orElse(incrementalWorthMap.lastEntry());
	}

	private double getSpawnerWorth(EntityType entityType) {
		return plugin.getSettings().getSpawnerPrice(entityType);
	}

	public void add(CreatureSpawner spawner) {
		DelayedSpawner delayedSpawner = DelayedSpawner.of(spawner);
		activationMap.put(delayedSpawner, System.currentTimeMillis());
	}

	public void remove(CreatureSpawner spawner) {
		DelayedSpawner delayedSpawner = DelayedSpawner.of(spawner);
		activationMap.remove(delayedSpawner);
	}

	public double getPotentialWorth(String factionId) {
		try {
			return plugin.getWorthManager().getWorth(factionId).getSpawners().entrySet().stream()
					.mapToDouble(entry -> plugin.getSettings().getSpawnerPrice(entry.getKey()) * entry.getValue())
					.sum();
		} catch (Exception ex) {
			getPlugin().getLogger().severe("Shit went down when trying to get potential worth...");
			ex.printStackTrace();
			return -1;
		}
	}

	public FactionsTopPlugin getPlugin() {
		return plugin;
	}

	public Map<DelayedSpawner, Long> getActivationMap() {
		return activationMap;
	}

	public TreeMap<Long, Double> getIncrementalWorthMap() {
		return incrementalWorthMap;
	}

	@Override
	public void terminate() {

	}
}
