package com.lbdevz.lbmuhafiz.listeners;

import com.lbdevz.lbmuhafiz.LBMuhafiz;
import com.lbdevz.lbmuhafiz.models.MuhafizModel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class MuhafizDeathListener implements Listener {

    private final LBMuhafiz plugin;
    private final Random random = new Random();

    public MuhafizDeathListener(LBMuhafiz plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;

        if (!plugin.getMuhafizManager().isMuhafiz(living.getUniqueId())) {
            return;
        }

        MuhafizModel model = plugin.getMuhafizManager().getMuhafizByUUID(living.getUniqueId());
        if (model == null) return;

        // Hasar hesaplandıktan hemen sonra can barını güncelle (1 tick sonra)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (living.isValid() && !living.isDead()) {
                    plugin.getMuhafizManager().updateHealthName(living, model.getDisplayName(), living.getHealth(), model.getMaxHealth());
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (!plugin.getMuhafizManager().isMuhafiz(entity.getUniqueId())) {
            return;
        }

        // Vanilla eşya düşüşlerini temizle
        event.getDrops().clear();

        MuhafizModel model = plugin.getMuhafizManager().getMuhafizByUUID(entity.getUniqueId());
        Player killer = entity.getKiller();

        if (model != null && killer != null) {
            // EXP Ödülü
            if (model.getRewardExp() > 0) {
                event.setDroppedExp(model.getRewardExp());
            }

            // Config'deki Echo Shard Şansı (% Oranı)
            double rollShard = random.nextDouble() * 100.0;
            if (rollShard <= model.getEchoShardChance()) {
                event.getDrops().add(new ItemStack(Material.ECHO_SHARD, 1));
            }

            // Config'deki Komut Ödülü Şansı (% Oranı)
            double rollCommand = random.nextDouble() * 100.0;
            if (rollCommand <= model.getCommandChance()) {
                if (model.getRewardCommands() != null) {
                    for (String cmd : model.getRewardCommands()) {
                        String formattedCmd = cmd.replace("%player%", killer.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
                    }
                }
            }
        }

        // Respawn sayacını başlat ve aktif listeden kaldır
        plugin.getMuhafizManager().handleDeath(entity.getUniqueId());
    }
}