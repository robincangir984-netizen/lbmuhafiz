package com.lbdevz.lbmuhafiz.managers;

import com.lbdevz.lbmuhafiz.LBMuhafiz;
import com.lbdevz.lbmuhafiz.models.MuhafizModel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class MuhafizManager {

    private final LBMuhafiz plugin;
    private final Map<String, MuhafizModel> muhafizModels = new HashMap<>();
    private final Map<UUID, String> activeMuhafizs = new HashMap<>();
    private final Set<String> dyingMuhafizs = new HashSet<>();
    private final NamespacedKey muhafizKey;
    private BukkitTask distanceCheckTask;

    public MuhafizManager(LBMuhafiz plugin) {
        this.plugin = plugin;
        this.muhafizKey = new NamespacedKey(plugin, "muhafiz_id");
        startDistanceChecker();
    }

    public void loadMuhafizlar() {
        removeAllActive();
        muhafizModels.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("muhafizlar");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String path = "muhafizlar." + key + ".";
            String displayName = plugin.getConfig().getString(path + "display-name", "&cMuhafız");
            String typeStr = plugin.getConfig().getString(path + "entity-type", "ZOMBIE");
            
            EntityType type;
            try {
                type = EntityType.valueOf(typeStr.toUpperCase());
            } catch (Exception e) {
                type = EntityType.ZOMBIE;
            }

            double health = plugin.getConfig().getDouble(path + "health", 100.0);
            int delay = plugin.getConfig().getInt(path + "respawn-delay-seconds", 300);

            List<String> commands = plugin.getConfig().getStringList(path + "rewards.commands");
            int exp = plugin.getConfig().getInt(path + "rewards.experience", 0);
            
            double echoShardChance = plugin.getConfig().getDouble(path + "rewards.echo-shard-chance", 50.0);
            double commandChance = plugin.getConfig().getDouble(path + "rewards.command-chance", 20.0);

            String worldName = plugin.getConfig().getString(path + "location.world", "world");
            World world = Bukkit.getWorld(worldName);

            double x = plugin.getConfig().getDouble(path + "location.x");
            double y = plugin.getConfig().getDouble(path + "location.y");
            double z = plugin.getConfig().getDouble(path + "location.z");
            float yaw = (float) plugin.getConfig().getDouble(path + "location.yaw");
            float pitch = (float) plugin.getConfig().getDouble(path + "location.pitch");

            Location loc = (world != null) ? new Location(world, x, y, z, yaw, pitch) : null;

            MuhafizModel model = new MuhafizModel(key, displayName, type, health, delay, commands, exp, echoShardChance, commandChance, loc);
            muhafizModels.put(key, model);

            if (loc != null) {
                spawnMuhafiz(model);
            }
        }
    }

    public void spawnMuhafiz(MuhafizModel model) {
        if (model.getSpawnLocation() == null || model.getSpawnLocation().getWorld() == null) return;

        Location spawnLoc = model.getSpawnLocation();
        World world = spawnLoc.getWorld();

        if (!spawnLoc.getChunk().isLoaded()) {
            spawnLoc.getChunk().load();
        }

        // Eski muhafız varsa haritadan zorla temizle
        killExistingEntitiesOfModel(model.getId());

        Entity entity = world.spawnEntity(spawnLoc, model.getEntityType());
        if (entity instanceof LivingEntity living) {
            AttributeInstance maxHealthAttr = living.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(model.getMaxHealth());
            }

            living.setHealth(model.getMaxHealth());
            living.setRemoveWhenFarAway(false);

            living.getPersistentDataContainer().set(muhafizKey, PersistentDataType.STRING, model.getId());

            activeMuhafizs.put(living.getUniqueId(), model.getId());
            dyingMuhafizs.remove(model.getId());
            updateHealthName(living, model.getDisplayName(), living.getHealth(), model.getMaxHealth());
        }
    }

    private void startDistanceChecker() {
        if (distanceCheckTask != null && !distanceCheckTask.isCancelled()) {
            distanceCheckTask.cancel();
        }

        distanceCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (MuhafizModel model : muhafizModels.values()) {
                    String modelId = model.getId();
                    if (dyingMuhafizs.contains(modelId)) continue;

                    Location setLoc = model.getSpawnLocation();
                    if (setLoc == null || setLoc.getWorld() == null) continue;

                    // Chunk yüklü değilse yükle ki mob silinebilsin
                    if (!setLoc.getChunk().isLoaded()) {
                        setLoc.getChunk().load();
                    }

                    // Dünya üzerindeki bu modele ait muhafızları tara
                    for (Entity entity : setLoc.getWorld().getEntities()) {
                        if (!(entity instanceof LivingEntity living)) continue;

                        String entityModelId = living.getPersistentDataContainer().get(muhafizKey, PersistentDataType.STRING);
                        if (entityModelId == null || !entityModelId.equals(modelId)) continue;

                        Location mobLoc = living.getLocation();

                        // 1. Muhafız setloc'tan 8 blok uzaklaştı mı? (8^2 = 64)
                        boolean isMobFar = !mobLoc.getWorld().equals(setLoc.getWorld()) || mobLoc.distanceSquared(setLoc) > 64.0;

                        // 2. Muhafızın hedefi setloc'tan 8 blok uzaklaştı mı?
                        boolean isTargetFar = false;
                        if (living instanceof Mob mob && mob.getTarget() != null) {
                            Location targetLoc = mob.getTarget().getLocation();
                            if (!targetLoc.getWorld().equals(setLoc.getWorld()) || targetLoc.distanceSquared(setLoc) > 64.0) {
                                isTargetFar = true;
                            }
                        }

                        if (isMobFar || isTargetFar) {
                            dyingMuhafizs.add(modelId);
                            activeMuhafizs.remove(living.getUniqueId());

                            // Mob'u kaldır
                            living.remove();

                            // 5 saniye (100 tick) sonra doğur
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    spawnMuhafiz(model);
                                }
                            }.runTaskLater(plugin, 100L);
                            break;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    private void killExistingEntitiesOfModel(String modelId) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                String id = entity.getPersistentDataContainer().get(muhafizKey, PersistentDataType.STRING);
                if (modelId.equals(id)) {
                    entity.remove();
                }
            }
        }
        activeMuhafizs.values().removeIf(id -> id.equals(modelId));
    }

    public void updateHealthName(LivingEntity entity, String baseName, double currentHealth, double maxHealth) {
        String healthBar = getHealthBar(currentHealth, maxHealth);
        String formattedName = ChatColor.translateAlternateColorCodes('&', baseName + " &7[" + healthBar + "&7]");
        entity.setCustomName(formattedName);
        entity.setCustomNameVisible(true);
    }

    private String getHealthBar(double currentHealth, double maxHealth) {
        int totalBlocks = 10;
        int greenBlocks = (int) Math.ceil((currentHealth / maxHealth) * totalBlocks);
        if (greenBlocks < 0) greenBlocks = 0;
        if (greenBlocks > totalBlocks) greenBlocks = totalBlocks;

        int redBlocks = totalBlocks - greenBlocks;

        StringBuilder bar = new StringBuilder("&a");
        for (int i = 0; i < greenBlocks; i++) {
            bar.append("█");
        }
        bar.append("&c");
        for (int i = 0; i < redBlocks; i++) {
            bar.append("█");
        }

        return bar.toString();
    }

    public void handleDeath(UUID entityUUID) {
        String muhafizId = activeMuhafizs.remove(entityUUID);
        if (muhafizId == null || dyingMuhafizs.contains(muhafizId)) return;

        MuhafizModel model = muhafizModels.get(muhafizId);
        if (model == null) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnMuhafiz(model);
            }
        }.runTaskLater(plugin, model.getRespawnDelaySeconds() * 20L);
    }

    public boolean isMuhafiz(UUID entityUUID) {
        return activeMuhafizs.containsKey(entityUUID);
    }

    public MuhafizModel getMuhafizByUUID(UUID entityUUID) {
        String id = activeMuhafizs.get(entityUUID);
        return id != null ? muhafizModels.get(id) : null;
    }

    public MuhafizModel getMuhafizModel(String id) {
        return muhafizModels.get(id);
    }

    public Set<String> getMuhafizIds() {
        return muhafizModels.keySet();
    }

    public void removeAllActive() {
        if (distanceCheckTask != null) {
            distanceCheckTask.cancel();
        }

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(muhafizKey, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }

        activeMuhafizs.clear();
        dyingMuhafizs.clear();
    }
}