package me.zwoosks.pumpkinshooter;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class ShooterAPI {

    public static boolean isShooter(ItemStack is, String itemType, PumpkinShooter plugin) {
        ItemMeta im = is.getItemMeta();
        if(is.getType().toString().equalsIgnoreCase(itemType) && im != null) {
            PersistentDataContainer persistentData = im.getPersistentDataContainer();
            if(persistentData != null) {
                if(persistentData.has(new NamespacedKey(plugin, "isShooter"), PersistentDataType.INTEGER)) {
                    int isShooterInt = persistentData.get(new NamespacedKey(plugin, "isShooter"), PersistentDataType.INTEGER);
                    return (isShooterInt == 1);
                }
            }
        }
        return false;
    }

}
