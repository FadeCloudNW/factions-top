package net.novucs.ftop.delayedspawners;

import net.novucs.ftop.entity.ChunkPos;
import net.novucs.ftop.hook.FactionsHook;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.*;

public class SpawnerActivationTask implements Runnable{

	private final DelayedSpawnerService spawnerService;
	private final TreeMap<Long, Double> worthTiers;
	private final Set<String> ignoredFactions;
	private final FactionsHook factionsHook;

	public SpawnerActivationTask(DelayedSpawnerService spawnerService) {
		this.spawnerService = spawnerService;
		this.worthTiers = spawnerService.getIncrementalWorthMap();
		this.ignoredFactions = new HashSet<>(spawnerService.getPlugin().getSettings().getIgnoredFactionIds());
		this.factionsHook = spawnerService.getPlugin().getFactionsHook();
	}

	@Override
	public void run() {
		Iterator<Map.Entry<DelayedSpawner, Long>> it =
				spawnerService.getActivationMap().entrySet().iterator();

		while(it.hasNext()) {
			Map.Entry<DelayedSpawner, Long> spawnerEntry = it.next();

			if (ignoredFactions.contains(factionsHook.getFactionAt(spawnerEntry.getKey().getChunk()))) {
				it.remove();
				continue;
			}

			long timeElapsed = since(spawnerEntry.getValue());

			Map.Entry<Long, Double> currentWorthTier = worthTiers.floorEntry(timeElapsed);
			Map.Entry<Long, Double> prevWorthTier    = worthTiers.floorEntry(timeElapsed - 1_000);

			if (currentWorthTier.equals(prevWorthTier) || currentWorthTier.equals(worthTiers.firstEntry()))
				continue;

			DelayedSpawner spawner = spawnerEntry.getKey();
			ChunkPos chunkPos = spawner.getChunk();
			World world = Bukkit.getWorld(chunkPos.getWorld());

			world.getChunkAtAsync(chunkPos.getX(), chunkPos.getZ(), chunk ->
					spawnerService.addIncrementalValue(spawner, currentWorthTier, prevWorthTier));

			if (currentWorthTier.equals(worthTiers.lastEntry()))
				it.remove();
		}
	}

	private long since(long time) {
		return System.currentTimeMillis() - time;
	}

}
