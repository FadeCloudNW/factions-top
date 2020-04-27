package net.novucs.ftop.delayedspawners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.novucs.ftop.entity.BlockPos;
import net.novucs.ftop.entity.ChunkPos;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;

import java.util.Objects;

public class DelayedSpawner {

	private final BlockPos location;
	private final EntityType spawnerType;

	public static DelayedSpawner of(CreatureSpawner spawner) {
		return new DelayedSpawner(BlockPos.of(spawner.getBlock()), spawner.getSpawnedType());
	}

	public DelayedSpawner(BlockPos location, EntityType spawnerType) {
		this.location = location;
		this.spawnerType = spawnerType;
	}

	public BlockPos getLocation() {
		return location;
	}

	public EntityType getSpawnerType() {
		return spawnerType;
	}

	public ChunkPos getChunk() {
		return ChunkPos.of(getLocation().getWorld(), getLocation().getX() >> 4, getLocation().getZ() >> 4);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DelayedSpawner that = (DelayedSpawner) o;
		return location.equals(that.location);
	}

	@Override
	public int hashCode() {
		return Objects.hash(location);
	}

	@SuppressWarnings("UnstableApiUsage")
	public byte[] serialize() {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF(spawnerType.name());
		out.writeUTF(location.getWorld());
		out.writeInt(location.getX());
		out.writeInt(location.getY());
		out.writeInt(location.getZ());
		return out.toByteArray();
	}

	public static DelayedSpawner deserialize(ByteArrayDataInput in) {
		EntityType spawnerType = EntityType.valueOf(in.readUTF());
		String world = in.readUTF();
		int x = in.readInt();
		int y = in.readInt();
		int z = in.readInt();
		return new DelayedSpawner(BlockPos.of(world, x, y, z), spawnerType);
	}

}
