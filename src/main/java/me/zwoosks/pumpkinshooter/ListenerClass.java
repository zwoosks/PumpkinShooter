package me.zwoosks.pumpkinshooter;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

public class ListenerClass implements Listener {

    private static HashMap<UUID, Long> dates = new HashMap<>();

    private PumpkinShooter plugin;
    private int delaySeconds;

    public ListenerClass(PumpkinShooter plugin) {
        this.plugin = plugin;
        this.delaySeconds = plugin.getConfig().getInt("settings.useDelayInSeconds");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if(e.getAction() == Action.RIGHT_CLICK_AIR) {
            Player player = e.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if(itemInHand != null) {
                String configName = plugin.getConfig().getString("settings.item-id");
                String itemNameType = itemInHand.getType().toString();
                if(ShooterAPI.isShooter(itemInHand, configName, plugin)) {
                    // Custom Item!
                    if((dates.get(player.getUniqueId()) != null)
                            && ((dates.get(player.getUniqueId()) + delaySeconds*1000) > System.currentTimeMillis())) {
                        sendMessage(player, "messages.wait", "%waitTime%", Integer.toString(remainingDelay(player.getUniqueId())));
                        return;
                    }
                    dates.put(player.getUniqueId(), System.currentTimeMillis());
                    FallingBlock fb = player.getWorld().spawnFallingBlock(player.getLocation(), Material.CARVED_PUMPKIN, (byte) 0);
                    fb.setVelocity(player.getLocation().getDirection().multiply(plugin.getConfig().getDouble("settings.vectorMultiplier")));
                    sendMessage(player, "messages.throw");
                }
            }
        }
    }

    @EventHandler
    public void onPumpkinLand(EntityChangeBlockEvent e) {
        if(e.getTo() == Material.CARVED_PUMPKIN) {
            BukkitScheduler scheduler = plugin.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    Location location = e.getBlock().getLocation();
                    Block block = location.getBlock();
                    block.setType(Material.AIR);
                    location.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, location, 3);
                    location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
                }
            }, plugin.getConfig().getInt("settings.secondsBeforePumpkinExplosion")*20L);
        }
    }

    private int remainingDelay(UUID uuid) {
        int remaining = (int) (System.currentTimeMillis() - dates.get(uuid)) / 1000;
        return delaySeconds - remaining;
    }

    private void sendMessage(Player player, String key) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(key)));
    }

    private void sendMessage(Player player, String key, String toReplace, String replacement) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(key).replace(toReplace, replacement)));
    }

}
