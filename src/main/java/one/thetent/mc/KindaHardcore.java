package one.thetent.mc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class KindaHardcore extends JavaPlugin implements Listener {
    private final Random random = new Random();
    private int teleportXPositive;
    private int teleportXNegative;
    private int teleportZPositive;
    private int teleportZNegative;
    private boolean khcToggleEnabled;
    private boolean khcToggleAdminOnly;
    private final Set<Player> teleportDisabled = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        teleportXPositive = getConfig().getInt("teleport-x-positive", 1000);
        teleportXNegative = getConfig().getInt("teleport-x-negative", -1000);
        teleportZPositive = getConfig().getInt("teleport-z-positive", 1000);
        teleportZNegative = getConfig().getInt("teleport-z-negative", -1000);
        khcToggleEnabled = getConfig().getBoolean("khc-toggle-enabled", true);
        khcToggleAdminOnly = getConfig().getBoolean("khc-toggle-admin-only", true);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("RandomTeleport enabled with teleport ranges: X+ " + teleportXPositive + ", X- " + teleportXNegative + ", Z+ " + teleportZPositive + ", Z- " + teleportZNegative);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!teleportDisabled.contains(player)) {
            Location bedSpawn = player.getBedSpawnLocation();
            if (bedSpawn != null) {
                player.sendMessage(ChatColor.GREEN + "Your bed spawn location:\n" + formatLocation(bedSpawn, ChatColor.GREEN));
            } else {
                player.sendMessage(ChatColor.RED + "You do not have a bed spawn set.");
            }
            Bukkit.getScheduler().runTaskLater(this, () -> teleportPlayer(player, bedSpawn), 5L);
        }
    }

    private void teleportPlayer(Player player, Location bedSpawn) {
        World world = player.getWorld();
        Location safeLocation = findSafeLocation(world);
        if (safeLocation != null) {
            player.teleport(safeLocation);
            player.sendMessage(ChatColor.RED + "You have been randomly teleported to:\n" + formatLocation(safeLocation, ChatColor.RED));
            
            if (bedSpawn != null) {
                double distance = safeLocation.distance(bedSpawn);
                player.sendMessage(ChatColor.YELLOW + "Distance to bed spawn: " + String.format("%.2f", distance) + " blocks");
            }
        } else {
            player.sendMessage(ChatColor.RED + "No safe location found! Staying at spawn.");
            player.teleport(world.getSpawnLocation());
        }
    }

    private Location findSafeLocation(World world) {
        for (int attempts = 0; attempts < 50; attempts++) { // Try 50 times to find a safe spot
            int x = random.nextInt(teleportXPositive - teleportXNegative + 1) + teleportXNegative;
            int z = random.nextInt(teleportZPositive - teleportZNegative + 1) + teleportZNegative;
            Location highest = world.getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);
            Material blockType = highest.subtract(0, 1, 0).getBlock().getType();
            
            if (isSafeBlock(blockType)) {
                return highest;
            }
        }
        return null; // If no safe location is found
    }

    private boolean isSafeBlock(Material block) {
        return !(block == Material.WATER || block == Material.LAVA || block == Material.AIR);
    }
    
    private String formatLocation(Location loc, ChatColor color) {
        return color + "X: " + loc.getBlockX() + "\n" +
               color + "Y: " + loc.getBlockY() + "\n" +
               color + "Z: " + loc.getBlockZ();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("khctoggle")) {
            if (!khcToggleEnabled) {
                sender.sendMessage(ChatColor.RED + "This command is disabled.");
                return true;
            }
            if (khcToggleAdminOnly) {
                if (!(sender instanceof Player) || !sender.isOp()) {
                    sender.sendMessage(ChatColor.RED + "You must be an operator to use this command.");
                    return true;
                }
            }
            if (args.length == 1) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    if (teleportDisabled.contains(target)) {
                        teleportDisabled.remove(target);
                        sender.sendMessage(ChatColor.GREEN + target.getName() + " now has auto teleport ENABLED.");
                    } else {
                        teleportDisabled.add(target);
                        sender.sendMessage(ChatColor.RED + target.getName() + " now has auto teleport DISABLED.");
                    }
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /khctoggle <player>");
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("khcreload")) {
            if (sender.isOp()) {
                reloadConfig();
                teleportXPositive = getConfig().getInt("teleport-x-positive", 1000);
                teleportXNegative = getConfig().getInt("teleport-x-negative", -1000);
                teleportZPositive = getConfig().getInt("teleport-z-positive", 1000);
                teleportZNegative = getConfig().getInt("teleport-z-negative", -1000);
                khcToggleEnabled = getConfig().getBoolean("khc-toggle-enabled", true);
                khcToggleAdminOnly = getConfig().getBoolean("khc-toggle-admin-only", true);
                sender.sendMessage(ChatColor.GREEN + "KHC config reloaded successfully!");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "You must be an operator to use this command.");
                return true;
            }
        }
        return false;
    }
}
