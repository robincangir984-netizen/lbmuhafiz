package com.lbdevz.lbmuhafiz.listeners;

import com.lbdevz.lbmuhafiz.LBMuhafiz;
import com.lbdevz.lbmuhafiz.models.MuhafizModel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;

        if (!plugin.getMuhafizManager().isMuhafiz(living.getUniqueId())) {
            return;
        }

        MuhafizModel model = plugin.getMuhafizManager().getMuhafizByUUID(living.getUniqueId());
        if (model == null) return;

        // SANAL CAN SISTEMI: Config'deki can 1024'u asabilir (orn. 5500).
        // Vanilla can en fazla 1024 oldugu icin hasari plugin takip eder.
        double currentHealth = plugin.getMuhafizManager().getVirtualHealth(living.getUniqueId());
        if (currentHealth <= 0) currentHealth = model.getMaxHealth();

        double finalDamage = event.getFinalDamage();
        double newHealth = currentHealth - finalDamage;

        if (newHealth <= 0) {
            // Sanal can bitti -> gercek olum
            plugin.getMuhafizManager().removeVirtualHealth(living.getUniqueId());
            event.setDamage(1000000.0);
            return;
        }

        plugin.getMuhafizManager().setVirtualHealth(living.getUniqueId(), newHealth);
        plugin.getMuhafizManager().updateHealthName(living, model.getDisplayName(), newHealth, model.getMaxHealth());

        AttributeInstance maxAttr = living.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        final double vanillaMax = (maxAttr != null) ? maxAttr.getBaseValue() : 1024.0;

        // Bu vurus vanilla cani bitirmesin: gerekiyorsa hasar uygulanmadan once cani yukselt
        if (living.getHealth() <= finalDamage + 1.0) {
            double safe = finalDamage + 2.0;
            if (safe > vanillaMax) safe = vanillaMax;
            living.setHealth(safe);
        }

        // Vanilla hasar UYGULANIR: ittirme, kirmizi yanip sonme, ses ve hasar sayisi dogal calisir.
        // Olmemesi icin 1 tick sonra vanilla can, sanal can oranina geri senkronlanir.
        final double virtualNow = newHealth;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!living.isValid() || living.isDead()) return;

                double target = vanillaMax * (virtualNow / model.getMaxHealth());
                if (target < 1.0) target = 1.0;
                if (target > vanillaMax) target = vanillaMax;

                living.setHealth(target);
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