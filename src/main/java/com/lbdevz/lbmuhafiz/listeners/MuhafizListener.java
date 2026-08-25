package com.lbdevz.lbmuhafiz.listeners;

import com.lbdevz.lbmuhafiz.LBMuhafiz;
import com.lbdevz.lbmuhafiz.models.MuhafizModel;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public class MuhafizListener implements Listener {

    private final LBMuhafiz plugin;

    public MuhafizListener(LBMuhafiz plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMuhafizTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() == null) return;

        if (plugin.getMuhafizManager().isMuhafiz(event.getEntity().getUniqueId())) {
            MuhafizModel model = plugin.getMuhafizManager().getMuhafizByUUID(event.getEntity().getUniqueId());
            if (model != null && model.getSpawnLocation() != null) {
                Location spawnLoc = model.getSpawnLocation();
                Location targetLoc = event.getTarget().getLocation();

                // Hedef oyuncu doğma noktasından 8 bloktan uzaktaysa hedef almayı iptal et
                if (!targetLoc.getWorld().equals(spawnLoc.getWorld()) || targetLoc.distanceSquared(spawnLoc) > 64.0) {
                    event.setCancelled(true);
                }
            }
        }
    }
}