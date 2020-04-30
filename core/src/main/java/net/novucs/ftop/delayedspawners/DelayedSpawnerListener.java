package net.novucs.ftop.delayedspawners;

import net.novucs.ftop.FactionsTopPlugin;
import net.novucs.ftop.WorthType;
import net.novucs.ftop.entity.ChunkPos;
import net.novucs.ftop.hook.event.FactionClaimEvent;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class DelayedSpawnerListener implements Listener {

	private final DelayedSpawnerService spawnerService;
	private final FactionsTopPlugin plugin;

	public DelayedSpawnerListener(DelayedSpawnerService spawnerService) {
		this.spawnerService = spawnerService;
		this.plugin = spawnerService.getPlugin();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on(FactionClaimEvent event) {
		if (event.isClaiming()) {
			List<ChunkPos> updateChunkPos = new ArrayList<>();

			event.getClaims().asMap().forEach((oldId, chunks) -> {
				if (oldId.equals(event.getFactionId()))
					return;

				updateChunkPos.addAll(chunks);
			});

			Bukkit.getScheduler().runTaskLater(plugin, () -> updateChunkPos
					.forEach(chunkPos -> Bukkit.getWorld(chunkPos.getWorld())
					.getChunkAtAsync(chunkPos.getX(), chunkPos.getZ(), claimCallback(chunkPos))), 1);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock().getType() != Material.MOB_SPAWNER)
			return;

		if (event.getItem() != null && event.getItem().getType() != Material.AIR)
			return;

		NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.US);
		double worth = spawnerService.getInstantWorth((CreatureSpawner) event.getClickedBlock().getState());
		event.getPlayer().sendMessage(format(String.format("&e&l[!] &eThis spawner is worth %s", fmt.format(worth))));

		DelayedSpawner spawner = DelayedSpawner.of((CreatureSpawner) event.getClickedBlock().getState());
		Optional.ofNullable(spawnerService.getActivationMap().get(spawner)).ifPresent(placed -> {
			long timeFullVal = placed + spawnerService.getIncrementalWorthMap().lastKey();
			long timeUntilFull = timeFullVal - System.currentTimeMillis();
			String duration = DurationFormatUtils.formatDurationWords(timeUntilFull, true, true);
			duration = duration.replace(" ", "");
			duration = duration.replace("days", "d ").replace("day", "d ");
			duration = duration.replace("hours", "h ").replace("hour", "h ");
			duration = duration.replace("minutes", "m ").replace("minute", "m ");
			duration = duration.replace("seconds", "s").replace("second", "s");
			duration = duration.trim();
			event.getPlayer().sendMessage(format(String.format("&7It will reach full value in &n%s&r&7!", duration)));
		});

	}

	private World.ChunkLoadCallback claimCallback(final ChunkPos chunkPos) {
		return chunk -> {
			Map<EntityType, Integer> spawners = Arrays.stream(chunk.getTileEntities())
					.filter(state -> state instanceof CreatureSpawner)
					.map(CreatureSpawner.class::cast)
					.peek(spawnerService::add)
					.collect(Collectors.groupingBy(CreatureSpawner::getSpawnedType, counting()));

			plugin.getWorthManager().set(chunkPos, WorthType.SPAWNER, 0);
			plugin.getWorthManager().setSpawners(chunkPos, spawners);
		};
	}

	public static <T> Collector<T, ?, Integer> counting() {
		return Collectors.reducing(0, e -> 1, Integer::sum);
	}

	private String format(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}

}
