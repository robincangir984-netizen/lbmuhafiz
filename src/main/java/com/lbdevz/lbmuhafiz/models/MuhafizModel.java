package com.lbdevz.lbmuhafiz.models;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.List;

public class MuhafizModel {

    private final String id;
    private final String displayName;
    private final EntityType entityType;
    private final double maxHealth;
    private final int respawnDelaySeconds;
    private final List<String> rewardCommands;
    private final int rewardExp;
    private final double echoShardChance;
    private final double commandChance;
    private Location spawnLocation;

    public MuhafizModel(String id, String displayName, EntityType entityType, double maxHealth, 
                        int respawnDelaySeconds, List<String> rewardCommands, int rewardExp, 
                        double echoShardChance, double commandChance, Location spawnLocation) {
        this.id = id;
        this.displayName = displayName;
        this.entityType = entityType;
        this.maxHealth = maxHealth;
        this.respawnDelaySeconds = respawnDelaySeconds;
        this.rewardCommands = rewardCommands;
        this.rewardExp = rewardExp;
        this.echoShardChance = echoShardChance;
        this.commandChance = commandChance;
        this.spawnLocation = spawnLocation;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public EntityType getEntityType() { return entityType; }
    public double getMaxHealth() { return maxHealth; }
    public int getRespawnDelaySeconds() { return respawnDelaySeconds; }
    public List<String> getRewardCommands() { return rewardCommands; }
    public int getRewardExp() { return rewardExp; }
    public double getEchoShardChance() { return echoShardChance; }
    public double getCommandChance() { return commandChance; }
    public Location getSpawnLocation() { return spawnLocation; }
    public void setSpawnLocation(Location spawnLocation) { this.spawnLocation = spawnLocation; }
}