package me.zwoosks.pumpkinshooter;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
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
    private static List<FallingBlock> fallingBlocks = new ArrayList<>();
    private final double blocksTimerAboveItem;
    private final boolean hologramsActive;
    private final boolean tracersActive;
    private final int tracerTicksInterval;
    private int delaySeconds;

    public ListenerClass(PumpkinShooter plugin) {
        this.plugin = plugin;
        this.delaySeconds = plugin.getConfig().getInt("settings.useDelayInSeconds");
        this.ticksInterval = plugin.getConfig().getDouble("settings.timerIntervalTicks");
        this.blocksTimerAboveItem = plugin.getConfig().getDouble("settings.blocksTimerAboveItem");
        this.hologramsActive = plugin.getConfig().getBoolean("settings.holograms");
        this.tracersActive = plugin.getConfig().getBoolean("settings.tracers");
        this.tracerTicksInterval = plugin.getConfig().getInt("settings.tracersTicks");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void antiGlitchDuplication(ItemSpawnEvent e) {
        if(e.getEntity().getItemStack().getType() == Material.CARVED_PUMPKIN) {
            ItemStack is = e.getEntity().getItemStack();
            PersistentDataContainer pdc = is.getItemMeta().getPersistentDataContainer();
            boolean containsPickable = false;
            if(pdc != null) containsPickable = pdc.has(new NamespacedKey(plugin, "pickablePumpkin"), PersistentDataType.INTEGER);
            e.setCancelled(!containsPickable);
        }
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
                    FallingBlock fb = player.getWorld().spawnFallingBlock(player.getEyeLocation(), Material.CARVED_PUMPKIN, (byte) 0);
                    fb.setVelocity(player.getLocation().getDirection().multiply(plugin.getConfig().getDouble("settings.vectorMultiplier")));
                    sendMessage(player, "messages.throw");
                    if(tracersActive) {
                        double ofX, ofY, ofZ;
                        ofX = plugin.getConfig().getDouble("settings.particleOffsetX");
                        ofY = plugin.getConfig().getDouble("settings.particleOffsetY");
                        ofZ = plugin.getConfig().getDouble("settings.particleOffsetZ");
                        int particleCount = plugin.getConfig().getInt("settings.particleCount");
                        int size = plugin.getConfig().getInt("settings.particleSize");
                        int r, g, b;
                        r = plugin.getConfig().getInt("settings.particleR");
                        g = plugin.getConfig().getInt("settings.particleG");
                        b = plugin.getConfig().getInt("settings.particleB");
                        fallingBlocks.add(fb);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if(fb.isDead()) {
                                    fallingBlocks.remove(fb);
                                    cancel();
                                }
                                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(r, g, b), size);
                                fb.getWorld().spawnParticle(Particle.REDSTONE, fb.getLocation(), 2, ofX, ofY, ofZ, dustOptions);
                            }
                        }.runTaskTimer(plugin, 0L, tracerTicksInterval);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPumpkinLand(EntityChangeBlockEvent e) {
        if(e.getTo() == Material.CARVED_PUMPKIN && e.getEntity() instanceof FallingBlock) {
            e.setCancelled(true);
            ItemStack tempPumpkin = new ItemStack(Material.CARVED_PUMPKIN);
            ItemMeta meta = tempPumpkin.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(new NamespacedKey(plugin, "pickablePumpkin"), PersistentDataType.INTEGER, 1);
            tempPumpkin.setItemMeta(meta);
            Location loc = e.getBlock().getLocation();
            Item it = loc.getWorld().dropItem(loc, tempPumpkin);
            if(hologramsActive) {
                Location holoLocation = null;
                List<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc, 2, 2, 2).stream().toList();
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
                            Location itemLocation = it.getLocation();
                            itemLocation.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, itemLocation, 3);
                            itemLocation.getWorld().playSound(itemLocation, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
                            it.remove();
                            cancel();
                        }
                        List<String> parsedLines = new ArrayList<String>();
                        for(String s : rawLines) {
                            parsedLines.add(s.replace("%seconds%", String.valueOf(localDuration/20)));
                        }
                        DHAPI.setHologramLines(hologram, parsedLines);
                        DHAPI.moveHologram(hologram, it.getLocation().add(0, blocksTimerAboveItem, 0));
                        durationList.put(holoName, localDuration - ticksInterval);
                    }
                }.runTaskTimer(plugin, 0L, (long) ticksInterval);
            } else {
                BukkitScheduler scheduler = plugin.getServer().getScheduler();
                scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Location itemLocation = it.getLocation();
                        itemLocation.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, itemLocation, 3);
                        itemLocation.getWorld().playSound(itemLocation, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
                        it.remove();
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

    @EventHandler(priority = EventPriority.HIGHEST)
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
