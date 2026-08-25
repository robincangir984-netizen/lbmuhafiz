package com.lbdevz.lbmuhafiz;

import com.lbdevz.lbmuhafiz.commands.MuhafizCommand;
import com.lbdevz.lbmuhafiz.commands.MuhafizTabCompleter;
import com.lbdevz.lbmuhafiz.listeners.MuhafizDeathListener;
import com.lbdevz.lbmuhafiz.managers.MuhafizManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LBMuhafiz extends JavaPlugin {

    private MuhafizManager muhafizManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.muhafizManager = new MuhafizManager(this);
        this.muhafizManager.loadMuhafizlar();

        if (getCommand("lbmuhafiz") != null) {
            getCommand("lbmuhafiz").setExecutor(new MuhafizCommand(this));
            getCommand("lbmuhafiz").setTabCompleter(new MuhafizTabCompleter(this));
        }

        getServer().getPluginManager().registerEvents(new MuhafizDeathListener(this), this);

        getLogger().info("LBMuhafiz başarıyla yüklendi!");
    }

    @Override
    public void onDisable() {
        if (muhafizManager != null) {
            muhafizManager.removeAllActive();
        }
    }

    public MuhafizManager getMuhafizManager() {
        return muhafizManager;
    }
}