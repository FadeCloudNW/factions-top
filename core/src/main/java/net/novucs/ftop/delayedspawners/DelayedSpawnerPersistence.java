package net.novucs.ftop.delayedspawners;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class DelayedSpawnerPersistence implements Runnable {

	private final DelayedSpawnerService spawnerService;

	public DelayedSpawnerPersistence(DelayedSpawnerService spawnerService) {
		this.spawnerService = spawnerService;
	}

	@Override
	public void run() {
		Map<DelayedSpawner, Long> map = ImmutableMap.copyOf(spawnerService.getActivationMap());

		if (map.isEmpty()) {
			getFile().delete();
			return;
		}

		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeInt(map.size());

		map.forEach((spawner, placedTimestamp) -> {
			out.writeLong(placedTimestamp);
			out.write(spawner.serialize());
		});

		try(FileOutputStream fos = new FileOutputStream(getFile())) {
			fos.write(out.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Map<DelayedSpawner, Long> load() {
		Map<DelayedSpawner, Long> map = new HashMap<>();
		File file = getFile();

		if (!file.exists())
			return map;

		try(FileInputStream fis = new FileInputStream(getFile())) {
			ByteArrayDataInput input = ByteStreams.newDataInput(ByteStreams.toByteArray(fis));
			int spawners = input.readInt();

			for (int i = 0; i < spawners; i++) {
				long placedTimestamp = input.readLong();
				DelayedSpawner spawner = DelayedSpawner.deserialize(input);
				map.put(spawner, placedTimestamp);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return map;
	}

	private File getFile() {
		return new File(spawnerService.getPlugin().getDataFolder(), "spawners.spnr");
	}

}
