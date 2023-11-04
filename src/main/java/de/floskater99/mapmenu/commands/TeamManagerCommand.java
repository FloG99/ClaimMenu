package de.floskater99.mapmenu.commands;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import de.floskater99.mapmenu.MapMenuAPI;
import de.floskater99.mapmenu.mapMenus.claim.ClaimMenu;
import de.floskater99.mapmenu.mapMenus.claim.Team;
import de.floskater99.mapmenu.mapMenus.claim.TeamController;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TeamManagerCommand implements CommandExecutor, TabCompleter {
    // teammanager create <teamname>
    // teammanager rename <teamname> <new teamname>
    // teammanager delete <teamname>
    // teammanager setowner <teamname> <player>
    // teammanager removeplayer <teamname> <player>
    // teammanager addplayer <teamname> <player>
    // teammanager addchunk <teamname> [chunkx] [chunkz]
    // teammanager overwritechunk <teamname> [chunkx] [chunkz]
    // teammanager removechunk <teamname> [chunkx] [chunkz]
    // teammanager list <teamname>
    // teammanager giftchunks <teamname> <count>

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("Usage: /teammanager <subcommand> <teamname> [arguments]");
            return true;
        }
        
        args = mergeQuotedWords(args);

        String subcommand = args[0];
        String teamName = args[1];
        
        Team team = getTeamByNameOrID(teamName);

        if (team == null && !subcommand.equalsIgnoreCase("create")) {
            player.sendMessage("Could not find a team with ID or name \"" + teamName + "\"");
            return true;
        }

        switch (subcommand.toLowerCase()) {
            case "create" -> createTeam(args, teamName, player);
            case "delete" -> deleteTeam(args, team, player);
            case "rename" -> renameTeam(args, team, player);
            case "setowner" -> setTeamOwner(args, team, player);
            case "removeplayer" -> removePlayer(args, team, player);
            case "addplayer" -> addPlayer(args, team, player);
            case "addchunk" -> addChunk(args, team, player);
            case "overwritechunk" -> overwriteChunk(args, team, player);
            case "removechunk" -> removeChunk(args, team, player);
            case "list" -> listTeam(team, player);
            case "giftchunks" -> giftChunks(args, team, player);
            case "listchunks" -> player.sendMessage();
            default ->
                player.sendMessage("Invalid subcommand. Usage: /teammanager <subcommand> <teamname> [arguments]");
        }

        return false;
    }

    private Team getTeamByNameOrID(String nameOrID) {
        return TeamController.teams.values().stream().filter(team_ -> team_.teamName.equals(nameOrID) || team_.id.toString().equals(nameOrID)).findAny().orElse(null);
    }

    private void createTeam(String[] args, String teamName, Player player) {
        if (args.length != 2) {
            player.sendMessage("Usage: /teammanager create <teamname>");
            return;
        }

        Team team = new Team(UUID.randomUUID(), player.getUniqueId(), Sets.newHashSet(player.getUniqueId()), teamName, new Color(255, 242, 0), 0);

        TeamController.addTeam(team);
        player.sendMessage("Created team '" + team.teamName + "'");
    }

    private void deleteTeam(String[] args, Team team, Player player) {
        if (args.length != 2) {
            player.sendMessage("Usage: /teammanager delete <teamname>");
            return;
        }

        TeamController.deleteTeamWrapper(team);
        player.sendMessage("Deleted team: " + team.teamName);
    }

    private void renameTeam(String[] args, Team team, Player player) {
        if (args.length != 3) {
            player.sendMessage("Usage: /teammanager rename <teamname> <new teamname>");
            return;
        }

        String oldTeamName = team.teamName;
        team.teamName = args[2];

        TeamController.updateTeam(team);
        player.sendMessage("Renamed team '" + oldTeamName + "' to '" + team.teamName + "'");
    }

    private void setTeamOwner(String[] args, Team team, Player player) {
        if (args.length != 3) {
            player.sendMessage("Usage: /teammanager setowner <teamname> <player>");
            return;
        }

        String newOwnerName = args[2];
        OfflinePlayer newOwner;
        if (newOwnerName.contains("-")) { // UUID
            newOwner = Bukkit.getOfflinePlayer(UUID.fromString(newOwnerName));
        } else { // Name
            newOwner = Bukkit.getOfflinePlayer(newOwnerName);
        }

        if (!newOwner.hasPlayedBefore()) {
            player.sendMessage("Player not found.");
            return;
        }

        team.owner = newOwner.getUniqueId();

        TeamController.updateTeam(team);
        player.sendMessage("Made " + newOwner.getName() + " the new team owner.");
    }

    private void removePlayer(String[] args, Team team, Player player) {
        if (args.length != 3) {
            player.sendMessage("Usage: /teammanager removeplayer <teamname> <player>");
            return;
        }

        String playerName = args[2];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
        if (!targetPlayer.hasPlayedBefore()) {
            player.sendMessage("Player " + playerName + " not found");
            return;
        }

        boolean success = TeamController.removeMemberFromTeamWrapper(team, targetPlayer.getUniqueId());
        if (success) {
            player.sendMessage("Removed player " + targetPlayer.getName() + " from team " + team.teamName);
        } else {
            player.sendMessage("Player " + targetPlayer.getName() + " isn't a member of that team");
        }
    }

    private void addPlayer(String[] args, Team team, Player player) {
        if (args.length != 3) {
            player.sendMessage("Usage: /teammanager addplayer <teamname> <player>");
            return;
        }

        String playerName = args[2];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
        if (!targetPlayer.hasPlayedBefore()) {
            player.sendMessage("Player " + playerName + " not found");
            return;
        }

        boolean success = TeamController.addMemberToTeamWrapper(team, targetPlayer.getUniqueId());
        if (success) {
            player.sendMessage("Added player " + targetPlayer.getName() + " to team " + team.teamName);
        } else {
            player.sendMessage("Could not add player " + targetPlayer.getName() + " to team " + team.teamName);
        }
    }

    private void addChunk(String[] args, Team team, Player player) {
        if (args.length != 2 && args.length != 4) {
            player.sendMessage("Usage: /teammanager addchunk <teamname> [chunkx] [chunkz]");
            return;
        }

        int chunkX, chunkZ;

        if (args.length == 2) {
            chunkX = player.getLocation().getChunk().getX();
            chunkZ = player.getLocation().getChunk().getZ();
        } else {
            try {
                chunkX = Integer.parseInt(args[2]);
                chunkZ = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
                player.sendMessage("Invalid chunk coordinates. Usage: /teammanager addchunk <teamname> [chunkx] [chunkz]");
                return;
            }
        }

        String error = TeamController.claimChunkWrapper(player.getWorld(), chunkX, chunkZ, team, false);

        switch (Strings.nullToEmpty(error)) {
            case "same" -> player.sendMessage("Failed to claim chunk. Chunk is already claimed by that team.");
            case "other" ->
                player.sendMessage("Failed to claim chunk. Chunk is already claimed by another team. Use /teammanager overwritechunk <teamname> [chunkx] [chunkz] instead.");
            default ->
                player.sendMessage("Added chunk claim at [X " + chunkX + ", Z " + chunkZ + "] to team " + team.teamName);
        }
    }
    
    private void overwriteChunk(String[] args, Team team, Player player) {
        if (args.length != 2 && args.length != 4) {
            player.sendMessage("Usage: /teammanager addchunk <teamname> [chunkx] [chunkz]");
            return;
        }

        int chunkX, chunkZ;

        if (args.length == 2) {
            chunkX = player.getLocation().getChunk().getX();
            chunkZ = player.getLocation().getChunk().getZ();
        } else {
            try {
                chunkX = Integer.parseInt(args[2]);
                chunkZ = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
                player.sendMessage("Invalid chunk coordinates. Usage: /teammanager addchunk <teamname> [chunkx] [chunkz]");
                return;
            }
        }

        TeamController.claimChunkWrapper(player.getWorld(), chunkX, chunkZ, team, true);

        player.sendMessage("Changed chunk claim at [X " + chunkX + ", Z " + chunkZ + "] to team " + team.teamName);
    }

    private void removeChunk(String[] args, Team team, Player player) {
        if (args.length != 2 && args.length != 4) {
            player.sendMessage("Usage: /teammanager removechunk <teamname> [chunkx] [chunkz]");
            return;
        }

        int chunkX, chunkZ;

        if (args.length == 2) {
            chunkX = player.getLocation().getChunk().getX();
            chunkZ = player.getLocation().getChunk().getZ();
        } else {
            try {
                chunkX = Integer.parseInt(args[2]);
                chunkZ = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
                player.sendMessage("Invalid chunk coordinates. Usage: /teammanager addchunk <teamname> <chunkx> <chunkz>");
                return;
            }
        }

        String error = TeamController.unclaimChunkWrapper(player.getWorld(), chunkX, chunkZ, team);

        switch (Strings.nullToEmpty(error)) {
            case "unclaimed" -> player.sendMessage("Failed to unclaim chunk. Chunk is already unclaimed.");
            case "other" -> player.sendMessage("Failed to unclaim chunk. Chunk isn't claimed by that team.");
            default ->
                player.sendMessage("Removed chunk claim at [X " + chunkX + ", Z " + chunkZ + "] from team " + team.teamName);
        }
    }

    private void giftChunks(String[] args, Team team, Player player) {
        if (args.length != 3 && args.length != 2) {
            player.sendMessage("Usage: /teammanager giftchunks <teamname> [chunk count]");
            return;
        }

        if (args.length == 2) {
            player.sendMessage("Team " + team.teamName + " has " + team.additionalChunks + " gifted chunks.");
            return;
        }

        try {
            int count = Integer.parseInt(args[2]);
            
            TeamController.addAdditionalChunksToTeam(team, count);
            team.additionalChunks += count;
            team.members.forEach(member -> MapMenuAPI.liveUpdateMenu(Bukkit.getPlayer(member), ClaimMenu.class, "claimCount"));
            
            player.sendMessage("Gifted " + count + " additional chunks to team " + team.teamName + ". Team now has " + team.additionalChunks + " additional chunks.");
        } catch (NumberFormatException ignored) {
            player.sendMessage("Failed to gift chunks. The specified count is not a number.");
        }
    }

    private void listTeam(Team team, Player player) {
        player.sendMessage("ID: " + team.id); // TODO: Click to copy
        player.sendMessage("Owner: " + Bukkit.getOfflinePlayer(team.owner).getName());
        player.sendMessage("Members: " + team.members.stream().map(member -> Bukkit.getOfflinePlayer(member).getName()).collect(Collectors.joining(", ")));
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] rawArgs) {
        if (!(sender instanceof Player)) {
            return List.of();
        }

        String[] args = mergeQuotedWords(rawArgs);

        if (args.length == 1) {
            List<String> subCommands = List.of("create", "delete", "rename", "setowner", "addplayer", "removeplayer", "addchunk", "overwritechunk", "removechunk", "list", "giftchunks");
            return subCommands.stream().filter(subCommand -> subCommand.startsWith(args[0])).toList();
        }

        if (args.length == 2 && !"create".equals(args[0])) {
            return TeamController.teams.values().stream().map(team -> team.teamName.contains(" ") ? "\"" + team.teamName + "\"" : team.teamName).filter(name -> name.startsWith(args[1])).toList();
        }

        if (args.length == 3) {
            if ("addplayer".equals(args[0])) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.startsWith(args[2])).toList();
            }
            if ("removeplayer".equals(args[0]) || "setowner".equals(args[0])) {
                Team team = getTeamByNameOrID(args[1]);
                if (team != null) {
                    return team.members.stream().map(Bukkit::getOfflinePlayer).map(OfflinePlayer::getName).filter(name -> name != null && name.startsWith(args[2])).toList();
                }
            }
        }

        return List.of();
    }
    
    public static String[] mergeQuotedWords(String[] inputArray) {
        if (inputArray == null || inputArray.length == 0) {
            return inputArray;
        }

        List<String> mergedList = new ArrayList<>();
        StringBuilder mergedWord = new StringBuilder();

        for (String element : inputArray) {
            if (element.startsWith("\"")) {
                mergedWord.append(element.substring(1));
            } else if (element.endsWith("\"")) {
                mergedWord.append(" ").append(element.substring(0, element.length() - 1)); // Remove the ending quote
                mergedList.add(mergedWord.toString());
                mergedWord.setLength(0);
            } else if (!mergedWord.isEmpty()) {
                mergedWord.append(" ").append(element);
            } else {
                mergedList.add(element);
            }
        }

        return mergedList.toArray(new String[0]);
    }
}