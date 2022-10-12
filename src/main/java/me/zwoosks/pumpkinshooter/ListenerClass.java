package me.zwoosks.pumpkinshooter;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

public class ListenerClass implements Listener {

    private PumpkinShooter plugin;
    private static HashMap<UUID, Long> dates = new HashMap<>();
    private final double blocksTimerAboveItem;
    private final boolean hologramsActive;
    private int delaySeconds;

    public ListenerClass(PumpkinShooter plugin) {
        this.plugin = plugin;
        this.delaySeconds = plugin.getConfig().getInt("settings.useDelayInSeconds");
        this.ticksInterval = plugin.getConfig().getDouble("settings.timerIntervalTicks");
        this.blocksTimerAboveItem = plugin.getConfig().getDouble("settings.blocksTimerAboveItem");
        this.hologramsActive = plugin.getConfig().getBoolean("settings.holograms");
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
            e.setCancelled(true);
            ItemStack tempPumpkin = new ItemStack(Material.CARVED_PUMPKIN);
            ItemMeta meta = tempPumpkin.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(new NamespacedKey(plugin, "pickablePumpkin"), PersistentDataType.INTEGER, 1);
            tempPumpkin.setItemMeta(meta);
            Location loc = e.getBlock().getLocation();
            loc.getWorld().dropItem(loc, tempPumpkin);
            if(hologramsActive) {
                Location holoLocation = null;
                List<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc, 10, 10, 10).stream().toList();
                for(Entity entity : nearbyEntities) {
                    if(entity instanceof Item) {
                        holoLocation = entity.getLocation();
                    }
                }
                Random random = new Random();
                String holoName = e.getEntity().getName() + random.nextInt();
                durationList.put(holoName, 20*plugin.getConfig().getDouble("settings.secondsBeforePumpkinExplosion"));
                holoLocation.setY(holoLocation.getY() + blocksTimerAboveItem);
                List<String> rawLines = plugin.getConfig().getStringList("settings.hologramLines");
                Hologram hologram = DHAPI.createHologram(holoName, holoLocation);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        double localDuration = durationList.get(holoName);
                        if(localDuration <= 0) {
                            DHAPI.removeHologram(holoName);
                            durationList.remove(holoName);
                            List<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc, 10, 10, 10).stream().toList();
                            for(Entity entity : nearbyEntities) {
                                if(entity instanceof Item) {
                                    Location entityLoc = entity.getLocation();
                                    entityLoc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, entityLoc, 3);
                                    entityLoc.getWorld().playSound(entityLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
                                    entity.remove();
                                }
                            }
                            cancel();
                        }
                        List<String> parsedLines = new ArrayList<String>();
                        for(String s : rawLines) {
                            parsedLines.add(s.replace("%seconds%", String.valueOf(localDuration/20)));
                        }
                        DHAPI.setHologramLines(hologram, parsedLines);
                        durationList.put(holoName, localDuration - ticksInterval);
                    }
                }.runTaskTimer(plugin, 0L, (long) ticksInterval);
            } else {
                BukkitScheduler scheduler = plugin.getServer().getScheduler();
                scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        List<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc, 10, 10, 10).stream().toList();
                        for(Entity entity : nearbyEntities) {
                            if(entity instanceof Item) {
                                Location entityLoc = entity.getLocation();
                                entityLoc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, entityLoc, 3);
                                entityLoc.getWorld().playSound(entityLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
                                entity.remove();
                            }
                        }
                    }
                }, plugin.getConfig().getInt("settings.secondsBeforePumpkinExplosion")*20L);
            }
        }
    }

    @EventHandler
    public void onEntityPickupPumpkin(EntityPickupItemEvent e) { /* For players */
        ItemStack is = e.getItem().getItemStack();
        PersistentDataContainer pdc = is.getItemMeta().getPersistentDataContainer();
        boolean cannotPick = false;
        if(pdc != null) cannotPick = pdc.has(new NamespacedKey(plugin, "pickablePumpkin"), PersistentDataType.INTEGER);
        e.setCancelled(cannotPick);
    }

    @EventHandler
    public void onInventoryPickUpPumpkin(InventoryPickupItemEvent e) { /* For hoppers */
        ItemStack is = e.getItem().getItemStack();
        PersistentDataContainer pdc = is.getItemMeta().getPersistentDataContainer();
        boolean cannotPick = false;
        if(pdc != null) cannotPick = pdc.has(new NamespacedKey(plugin, "pickablePumpkin"), PersistentDataType.INTEGER);
        e.setCancelled(cannotPick);
    }

    @EventHandler
    public void onPumpkinSpawn(EntityDropItemEvent e) {
        Entity entity = e.getEntity();
        ItemStack is = e.getItemDrop().getItemStack();
        if(is.getType() == Material.CARVED_PUMPKIN) {
            ItemMeta im = is.getItemMeta();
            PersistentDataContainer pdc = im.getPersistentDataContainer();
            pdc.set(new NamespacedKey(plugin, "pickablePumpkin"), PersistentDataType.INTEGER, 1);
            is.setItemMeta(im);
            e.getItemDrop().setItemStack(is);
            if(hologramsActive) {
                Random random = new Random();
                String holoName = e.getEntity().getName() + random.nextInt();
                durationList.put(holoName, 20*plugin.getConfig().getDouble("settings.secondsBeforePumpkinExplosion"));
                Location holoLocation = e.getItemDrop().getLocation();
                holoLocation.setY(holoLocation.getY() + blocksTimerAboveItem);
                List<String> rawLines = plugin.getConfig().getStringList("settings.hologramLines");
                Hologram hologram = DHAPI.createHologram(holoName, holoLocation);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        double localDuration = durationList.get(holoName);
                        if(localDuration <= 0) {
                            DHAPI.removeHologram(holoName);
                            durationList.remove(holoName);
                            Location itemLoc = e.getItemDrop().getLocation();
                            itemLoc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, itemLoc, 3);
                            itemLoc.getWorld().playSound(itemLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
                            e.getItemDrop().remove();
                            cancel();
                        }
                        List<String> parsedLines = new ArrayList<String>();
                        for(String s : rawLines) {
                            parsedLines.add(s.replace("%seconds%", String.valueOf(localDuration/20)));
                        }
                        DHAPI.setHologramLines(hologram, parsedLines);
                        durationList.put(holoName, localDuration - ticksInterval);
                    }
                }.runTaskTimer(plugin, 0L, (long) ticksInterval);
            } else {
                BukkitScheduler scheduler = plugin.getServer().getScheduler();
                scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Location location = e.getItemDrop().getLocation();
                        e.getItemDrop().remove();
                        location.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, location, 3);
                        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
                    }
                }, plugin.getConfig().getInt("settings.secondsBeforePumpkinExplosion")*20L);
            }
        }
    }

    private HashMap<String, Double> durationList = new HashMap<String, Double>();
    private final double ticksInterval;

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
