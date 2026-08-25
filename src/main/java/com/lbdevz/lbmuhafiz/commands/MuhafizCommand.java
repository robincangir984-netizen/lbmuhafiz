package com.lbdevz.lbmuhafiz.commands;

import com.lbdevz.lbmuhafiz.LBMuhafiz;
import com.lbdevz.lbmuhafiz.models.MuhafizModel;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuhafizCommand implements CommandExecutor {

    private final LBMuhafiz plugin;

    public MuhafizCommand(LBMuhafiz plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&a[LB-Muhafız] ");

        if (!sender.hasPermission("lbmuhafiz.admin")) {
            sender.sendMessage(color(prefix + plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(color("&e/lbmuhafiz reload &7- Konfigürasyonu yeniler."));
            sender.sendMessage(color("&e/lbmuhafiz setloc <id> &7- Bulunduğunuz konumu muhafıza atar."));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getMuhafizManager().loadMuhafizlar();
            sender.sendMessage(color(prefix + plugin.getConfig().getString("messages.reload")));
            return true;
        }

        if (args[0].equalsIgnoreCase("setloc")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Bu komutu sadece oyuncular kullanabilir!");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(color(prefix + "&cKullanım: /lbmuhafiz setloc <muhafiz_id>"));
                return true;
            }

            String id = args[1];
            MuhafizModel model = plugin.getMuhafizManager().getMuhafizModel(id);

            if (model == null) {
                player.sendMessage(color(prefix + plugin.getConfig().getString("messages.muhafiz-not-found")));
                return true;
            }

            Location loc = player.getLocation();

            String path = "muhafizlar." + id + ".location.";
            plugin.getConfig().set(path + "world", loc.getWorld().getName());
            plugin.getConfig().set(path + "x", loc.getX());
            plugin.getConfig().set(path + "y", loc.getY());
            plugin.getConfig().set(path + "z", loc.getZ());
            plugin.getConfig().set(path + "yaw", loc.getYaw());
            plugin.getConfig().set(path + "pitch", loc.getPitch());
            plugin.saveConfig();

            plugin.getMuhafizManager().loadMuhafizlar();

            String msg = plugin.getConfig().getString("messages.loc-set", "&aKonum ayarlandı!").replace("%id%", id);
            player.sendMessage(color(prefix + msg));
            return true;
        }

        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}