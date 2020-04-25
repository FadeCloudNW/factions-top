package net.novucs.ftop.newdelayedspawners;

import net.novucs.ftop.entity.BlockPos;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;

import java.util.Objects;

public class NewDelayedSpawner {

	private final BlockPos location;
	private final EntityType spawnerType;

	public NewDelayedSpawner(BlockPos location, EntityType spawnerType) {
		this.location = location;
		this.spawnerType = spawnerType;
	}

	public BlockPos getLocation() {
		return location;
	}

	public EntityType getSpawnerType() {
		return spawnerType;
	}

	public static NewDelayedSpawner of(CreatureSpawner spawner) {
		return new NewDelayedSpawner(BlockPos.of(spawner.getBlock()), spawner.getSpawnedType());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NewDelayedSpawner that = (NewDelayedSpawner) o;
		return location.equals(that.location) &&
				spawnerType == that.spawnerType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(location);
	}

	@Override
	public String toString() {
		return "NewDelayedSpawner{" +
				"location=" + location +
				", spawnerType=" + spawnerType +
				'}';
	}

}
