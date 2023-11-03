package de.floskater99.mapmenu.commands;

import de.floskater99.mapmenu.mapMenus.claim.TeamController;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AdminCommands implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player) || !sender.isOp()) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("refresh")) {
            if (args.length == 1) {
                String subCommand = args[0].toLowerCase();

                if (subCommand.equals("teams")) {
                    sender.sendMessage("Refreshing teams...");
                    TeamController.initializeTeams();
                    return true;
                } else if (subCommand.equals("claimedchunks")) {
                    sender.sendMessage("Refreshing claimed chunks...");
                    TeamController.initializeClaimedChunks();
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.isOp()) {
            // Disable tab completion for non-OP players
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            // Add tab completions for subcommands
            if ("teams".startsWith(input)) {
                completions.add("teams");
            }
            if ("claimedchunks".startsWith(input)) {
                completions.add("claimedchunks");
            }
        }

        return completions;
    }
}
