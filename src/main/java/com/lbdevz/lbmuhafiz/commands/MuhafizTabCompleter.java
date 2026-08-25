package com.lbdevz.lbmuhafiz.commands;

import com.lbdevz.lbmuhafiz.LBMuhafiz;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MuhafizTabCompleter implements TabCompleter {

    private final LBMuhafiz plugin;

    public MuhafizTabCompleter(LBMuhafiz plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lbmuhafiz.admin")) return List.of();

        if (args.length == 1) {
            return Arrays.asList("reload", "setloc");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setloc")) {
            return new ArrayList<>(plugin.getMuhafizManager().getMuhafizIds());
        }

        return List.of();
    }
}