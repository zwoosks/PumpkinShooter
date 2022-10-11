package me.zwoosks.pumpkinshooter;

import org.bukkit.plugin.java.JavaPlugin;

public final class PumpkinShooter extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.getServer().getPluginManager().registerEvents(new ListenerClass(this), this);
        this.getCommand("pumpkinShooter").setExecutor(new CommandManager(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
