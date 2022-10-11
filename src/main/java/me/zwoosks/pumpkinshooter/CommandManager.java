package me.zwoosks.pumpkinshooter;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager implements CommandExecutor {

    private PumpkinShooter plugin;

    public CommandManager(PumpkinShooter plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) return false;
        if(args.length < 1) {
            sendMessage(sender, "messages.help");
        } else{
            if(args[0].equalsIgnoreCase("give")) {
                if(sender.hasPermission("pumpkinshooter.give")) {
                    if(args.length == 1) {
                        Player player = (Player) sender;
                        givePumpkinShooter(player);
                        sendMessage(sender, "messages.givenMessage", "%playerName%", player.getName());
                    } else if(args.length == 2) {
                        Player givenPlayer = plugin.getServer().getPlayerExact(args[1]);
                        if (givenPlayer == null) {
                            sendMessage(sender, "messages.notOnline", "%givenName%", args[1]);
                        } else {
                            givePumpkinShooter(givenPlayer);
                            sendMessage(sender, "messages.givenMessage", "%playerName%", givenPlayer.getName());
                        }
                    }
                } else {
                    sendMessage(sender, "messages.noPerm", "%node%", "pumpkinshooter.give");
                }
            } else {
                sendMessage(sender, "messages.help");
            }
        }
        return true;
    }

    private void givePumpkinShooter(Player player) {
        Material material = Material.getMaterial(plugin.getConfig().getString("settings.item-id"));
        ItemStack is = new ItemStack(material, 1);
        ItemMeta meta = is.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        // value 1 means it is a custom pumpkin shooter
        pdc.set(new NamespacedKey(plugin, "isShooter"), PersistentDataType.INTEGER, 1);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("settings.displayName")));
        List<String> rawItemLore = plugin.getConfig().getStringList("settings.lore");
        List<String> itemLore = new ArrayList<String>();
        for(String s : rawItemLore) {
            itemLore.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        meta.setLore(itemLore);
        is.setItemMeta(meta);
        player.getInventory().addItem(is);
    }

    private void sendMessage(CommandSender sender, String key) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(key)));
    }

    private void sendMessage(CommandSender sender, String key, String toReplace, String replacement) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(key).replace(toReplace, replacement)));
    }

}
