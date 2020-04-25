package net.novucs.ftop.newdelayedspawners;

import net.novucs.ftop.entity.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class DelayedActivationTask implements Runnable {

	private final NewDelayedSpawners spawners;

	public DelayedActivationTask(NewDelayedSpawners spawners) {
		this.spawners = spawners;
	}

	@Override
	public void run() {
		TreeMap<Long, Double> incrementalWorth = spawners.getIncrementalWorthMap();
		Iterator<Map.Entry<NewDelayedSpawner, Long>> it = spawners.getActivationMap().entrySet().iterator();

		while(it.hasNext()) {
			Map.Entry<NewDelayedSpawner, Long> entry = it.next();
			long elapsed = System.currentTimeMillis() - entry.getValue();

			if (elapsed < 10000)
				continue;

			Map.Entry<Long, Double> currentWorthMultiplier = incrementalWorth.floorEntry(elapsed);

			if (!currentWorthMultiplier.getKey().equals(incrementalWorth.floorKey(elapsed - 1000))) {
				BlockPos location = entry.getKey().getLocation();
				World world = Bukkit.getWorld(location.getWorld());
				Runnable runnable = () -> spawners.updateWorth(entry.getKey(), currentWorthMultiplier);

				System.out.println(location.toString() + " new multi: " + currentWorthMultiplier.getValue());

				if (world.isChunkLoaded(location.getX() / 16, location.getZ() / 16)) runnable.run();
				else Bukkit.getScheduler().runTask(spawners.getPlugin(), runnable);

				if (elapsed > spawners.getLastIncrementEntry().getKey())
					it.remove();
			}
		}
	}

}
