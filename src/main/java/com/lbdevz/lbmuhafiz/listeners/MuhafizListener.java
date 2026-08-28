package com.lbdevz.lbmuhafiz.listeners;

import com.lbdevz.lbmuhafiz.LBMuhafiz;
import com.lbdevz.lbmuhafiz.models.MuhafizModel;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public class MuhafizListener implements Listener {

    private final LBMuhafiz plugin;

    public MuhafizListener(LBMuhafiz plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMuhafizTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() == null) return;

        if (plugin.getMuhafizManager().isMuhafiz(event.getEntity().getUniqueId())) {
            MuhafizModel model = plugin.getMuhafizManager().getMuhafizByUUID(event.getEntity().getUniqueId());
            if (model != null && model.getSpawnLocation() != null) {
                Location setLoc = model.getSpawnLocation();
                Location targetLoc = event.getTarget().getLocation();

                // Hedef oyuncu setloc konumundan 15 blok uzağa çıktığı an hedef almayı iptal et
                if (!targetLoc.getWorld().equals(setLoc.getWorld()) || targetLoc.distanceSquared(setLoc) > 225.0) {
                    event.setCancelled(true);
                    if (event.getEntity() instanceof Mob mob) {
                        mob.setTarget(null);
                    }
                }
            }
        }
    }

    // Muhafizin verdigi hasar: config'deki "damage" degeri uygulanir
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMuhafizAttack(EntityDamageByEntityEvent event) {
        LivingEntity attacker = null;

        if (event.getDamager() instanceof LivingEntity le) {
            attacker = le;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity le) {
            attacker = le;
        }

        if (attacker == null) return;

        MuhafizModel model = plugin.getMuhafizManager().getMuhafizByUUID(attacker.getUniqueId());
        if (model == null) return;

        // 0 veya negatifse vanilla hasar korunur
        if (model.getDamage() > 0) {
            event.setDamage(model.getDamage());
        }
    }
}