package com.lbdevz.lbmuhafiz.listeners;

import com.lbdevz.lbmuhafiz.LBMuhafiz;
import com.lbdevz.lbmuhafiz.models.MuhafizModel;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
}