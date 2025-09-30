package io.elyvara.vtp;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.UUID;

public class VerticalTeleport extends JavaPlugin implements Listener {
	private final HashMap<UUID, Long> cooldowns = new HashMap<>();
	private final long cooldownDuration = 125; // 0.125 second in milliseconds

	@Override
	public void onEnable() {
		saveDefaultConfig();
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		final Material elevatorMaterial = Material.getMaterial(getConfig().getString("material-type"));
		Block clickedBlock = event.getClickedBlock();
		Player player = event.getPlayer();
		UUID playerUUID = player.getUniqueId();
		long currentTime = System.currentTimeMillis();
		Location playerLoc = player.getLocation();

		// Check if left or right click
		if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			return;
		}

		// Check if clicked block is null
		if (clickedBlock == null) {
			return;
		}

		// Check if clicked block is elevatorMaterial
		if (!clickedBlock.getType().equals(elevatorMaterial)) {
			return;
		}

		// Check if clicked block is below player
		if (playerLoc.toCenterLocation().getX() == clickedBlock.getLocation().toCenterLocation().getX()
				&& playerLoc.toCenterLocation().getZ() == clickedBlock.getLocation().toCenterLocation().getZ()
				&& playerLoc.toCenterLocation().getY() > clickedBlock.getLocation().toCenterLocation().getY()) {
			final int worldFloorY = -64;
			final int worldCeilingY = 320;
			World world = player.getWorld();
			Location destination = player.getLocation().clone();
			destination.setX(clickedBlock.getX() + 0.5);
			destination.setZ(clickedBlock.getZ() + 0.5);

			// Should safely cancel the event at this point
			event.setCancelled(true);

			if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
				// If left click --> check downward for elevator
				for (double destY = clickedBlock.getY() - 2; destY > worldFloorY; destY--) {
					destination.setY(destY);

					if (isSafeElevator(destination, player)) {
						if (cooldowns.containsKey(playerUUID)) {
							long lastInteractionTime = cooldowns.get(playerUUID);
							if (currentTime < lastInteractionTime + cooldownDuration) {
								// Player still on cooldown
								return;
							}
						}
						if (!player.teleport(destination)) {
							player.sendMessage(Component.text("Teleport failed for unknown reason."));
						}
						else {
							// Set cooldown after successful vtp
							cooldowns.put(playerUUID, currentTime);
						}
						// Stop searching
						break;
					}
				}
			}
			else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
				// If right click --> check upward for elevator
				for (double destY = clickedBlock.getY() + 4; destY < worldCeilingY; destY++) {
					destination.setY(destY);

					if (isSafeElevator(destination, player)) {
						if (cooldowns.containsKey(playerUUID)) {
							long lastInteractionTime = cooldowns.get(playerUUID);
							if (currentTime < lastInteractionTime + cooldownDuration) {
								// Player still on cooldown
								return;
							}
						}
						if (!player.teleport(destination)) {
							player.sendMessage(Component.text("Teleport failed for unknown reason."));
						}
						else {
							// Set cooldown after successful vtp
							cooldowns.put(playerUUID, currentTime);
						}
						// Stop searching
						break;
					}
				}
			}
		}
		else {
			return;
		}
	}

	public boolean isSafeElevator(Location loc, Player player) {
		Block legDest = loc.getBlock();
		Block headDest = legDest.getRelative(BlockFace.UP);
		final Material elevatorMaterial = Material.getMaterial(getConfig().getString("material-type"));
		Block potentialElevator = legDest.getRelative(BlockFace.DOWN);

		if (!potentialElevator.getType().equals(elevatorMaterial)) {
			// Not an elevator
			return false;
		}

		if (legDest.isPassable() && headDest.isPassable() 
				&& !legDest.getType().equals(Material.LAVA) 
				&& !headDest.getType().equals(Material.LAVA)) {
			// Safe elevator at loc
			return true;
		}
		else {
			// Not safe, notify player
			player.sendMessage(Component.text("Destination elevator blocked or unsafe."));
			return false;
		}
	}
}
