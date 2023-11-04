package de.floskater99.mapmenu.mapMenus.claim;

import com.google.common.collect.HashBasedTable;
import de.floskater99.mapmenu.MapMenuAPI;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class TeamController {
    public static final Map<UUID, Team> teams = new HashMap<>();
    public static final Map<String, HashBasedTable<Integer, Integer, Team>> claimedChunks = new HashMap<>();
    public static final Map<String, List<Pair<Integer, Integer>>> blockedChunks = new HashMap<>();
    public static final Map<Team, Integer> claimedChunkCounts = new HashMap<>();
    public static final Map<UUID, Set<Team>> invites = new HashMap<>();
    
    public static Set<Team> getInvites(UUID uuid) {
        return TeamController.invites.computeIfAbsent(uuid, k -> new LinkedHashSet<>());
    }

    public static void initializeTeams() {
        teams.clear();

        try {
            Statement statement = Database.getInstance().getConnection().createStatement();

            String selectTeamsQuery = "SELECT * FROM teams";
            ResultSet teamsResultSet = statement.executeQuery(selectTeamsQuery);

            while (teamsResultSet.next()) {
                if ("blocked".equals(teamsResultSet.getString("id"))) {
                    continue;
                }
                UUID teamId = UUID.fromString(teamsResultSet.getString("id"));
                UUID owner = UUID.fromString(teamsResultSet.getString("owner"));
                String teamName = teamsResultSet.getString("name");
                String colorHex = teamsResultSet.getString("color");
                int additionalChunks = teamsResultSet.getInt("additionalchunks");
                Color teamColor = hexToColor(colorHex);
                Team team = new Team(teamId, owner, new HashSet<>(), teamName, teamColor, additionalChunks);
                teams.put(teamId, team);
            }

            // Fetch all team members at once, for performance reasons
            String selectMembersQuery = "SELECT teamid, userid FROM teammembers";
            ResultSet membersResultSet = statement.executeQuery(selectMembersQuery);

            while (membersResultSet.next()) {
                UUID teamId = UUID.fromString(membersResultSet.getString("teamid"));
                if (teams.containsKey(teamId)) {
                    teams.get(teamId).members.add(UUID.fromString(membersResultSet.getString("userid")));
                }
            }

            teamsResultSet.close();
            membersResultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void initializeClaimedChunks() {
        blockedChunks.clear();
        claimedChunks.clear();
        claimedChunkCounts.clear();

        try {
            Connection connection = Database.getInstance().getConnection();
            Statement statement = connection.createStatement();

            String selectClaimedChunksQuery = "SELECT * FROM claimedchunks";
            ResultSet chunksResultSet = statement.executeQuery(selectClaimedChunksQuery);

            while (chunksResultSet.next()) {
                String world = chunksResultSet.getString("world");
                int chunkX = chunksResultSet.getInt("chunkx");
                int chunkZ = chunksResultSet.getInt("chunkz");
                String teamID = chunksResultSet.getString("teamid");

                if ("blocked".equals(teamID)) {
                    blockedChunks.computeIfAbsent(world, k -> new ArrayList<>()).add(new ImmutablePair<>(chunkX, chunkZ));
                } else {
                    UUID teamId = UUID.fromString(teamID);

                    if (teams.containsKey(teamId)) {
                        Team team = teams.get(teamId);
                        claimedChunks.computeIfAbsent(world, k -> HashBasedTable.create()).put(chunkX, chunkZ, team);
                        claimedChunkCounts.merge(team, 1, Integer::sum);
                    }
                }
            }

            chunksResultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addTeam(Team team) {
        teams.put(team.id, team);

        try {
            Connection connection = Database.getInstance().getConnection();
            connection.setAutoCommit(false);

            // Insert team into teams table
            String insertTeamQuery = "INSERT INTO teams (id, name, color, owner) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertTeamQuery)) {
                pstmt.setString(1, team.id.toString());
                pstmt.setString(2, team.teamName);
                pstmt.setString(3, colorToHex(team.teamColor));
                pstmt.setString(4, team.owner.toString());
                pstmt.executeUpdate();
            }

            // Insert team owner into teammembers table
            String insertOwnerQuery = "INSERT INTO teammembers (teamid, userid) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertOwnerQuery)) {
                pstmt.setString(1, team.id.toString());
                pstmt.setString(2, team.owner.toString());
                pstmt.executeUpdate();
            }

            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTeam(Team team) {
        try {
            Connection connection = Database.getInstance().getConnection();

            // Update team in teams table
            String updateTeamQuery = "UPDATE teams SET name=?, color=?, owner=? WHERE id=?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateTeamQuery)) {
                pstmt.setString(1, team.teamName);
                pstmt.setString(2, colorToHex(team.teamColor));
                pstmt.setString(3, team.owner.toString());
                pstmt.setString(4, team.id.toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteTeamWrapper(Team team) {
        TeamController.deleteTeam(team);
        TeamController.invites.forEach((player, invites) -> {
            if (invites.contains(team)) {
                invites.remove(team);
                MapMenuAPI.liveUpdateMenu(Bukkit.getPlayer(player), ClaimMenuSettings.class, "invites");
            }
        });
        team.members.forEach(member -> MapMenuAPI.liveUpdateMenu(Bukkit.getPlayer(member), ClaimMenuSettings.class, "kicked"));
        TeamController.claimedChunks.values().forEach(worldClaimedChunks -> worldClaimedChunks.cellSet().removeIf(cell -> team.equals(cell.getValue())));
    }

    private static void deleteTeam(Team team) {
        teams.remove(team.id);

        try {
            Connection connection = Database.getInstance().getConnection();

            // Delete the team from the teams table
            // Because TeamMembers and ClaimedChunks are connected to the teamid via foreign key and have ON DELETE CASCADE, this is all we need to do.
            String deleteTeamQuery = "DELETE FROM teams WHERE id=?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteTeamQuery)) {
                pstmt.setString(1, team.id.toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean addMemberToTeamWrapper(Team team, UUID targetPlayer) {
        if (team.members.contains(targetPlayer)) {
            return false;
        }

        Set<Team> invites = TeamController.invites.get(targetPlayer);
        if (invites != null) {
            invites.remove(team);
        }
        team.addMember(targetPlayer);
        TeamController.addMemberToTeam(team, targetPlayer);
        team.members.stream().filter(member -> !member.equals(targetPlayer)).forEach(member -> MapMenuAPI.liveUpdateMenu(Bukkit.getPlayer(member), ClaimMenuSettings.class, "teamMembers"));
        MapMenuAPI.liveUpdateMenu(Bukkit.getPlayer(targetPlayer), ClaimMenuSettings.class, "added");

        return true;
    }

    public static void addMemberToTeam(Team team, UUID uuid) {
        try {
            Connection connection = Database.getInstance().getConnection();

            String insertMemberQuery = "INSERT INTO teammembers (teamid, userid) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertMemberQuery)) {
                pstmt.setString(1, team.id.toString());
                pstmt.setString(2, uuid.toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean removeMemberFromTeamWrapper(Team team, UUID targetPlayer) {
        if (!team.members.contains(targetPlayer)) {
            return false;
        }

        team.removeMember(targetPlayer);
        TeamController.removeMemberFromTeam(team, targetPlayer);
        team.members.forEach(member -> MapMenuAPI.liveUpdateMenu(Bukkit.getPlayer(member), ClaimMenuSettings.class, "teamMembers"));
        MapMenuAPI.liveUpdateMenu(Bukkit.getPlayer(targetPlayer), ClaimMenuSettings.class, "kicked");

        return true;
    }

    public static void removeMemberFromTeam(Team team, UUID uuid) {
        try {
            Connection connection = Database.getInstance().getConnection();

            String removeMemberQuery = "DELETE FROM teammembers WHERE teamid = ? AND userid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(removeMemberQuery)) {
                pstmt.setString(1, team.id.toString());
                pstmt.setString(2, uuid.toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String colorToHex(Color color) {
        return String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Color hexToColor(String hex) {
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color format: " + hex);
        }

        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);

        return new Color(r, g, b);
    }

    public static String claimChunkWrapper(World world, int x, int z, Team team, boolean allowOverwrite) {
        Team chunkTeam = claimedChunks.get(world.getName()).get(x, z);
        if (chunkTeam == team) {
            return "same";
        } else if (chunkTeam != null && !allowOverwrite) {
            return "other";
        }

        claimedChunks.get(world.getName()).put(x, z, team);
        claimedChunkCounts.merge(team, 1, Integer::sum);
        TeamController.claimChunk(world, x, z, team.id.toString());
        team.members.forEach(member -> MapMenuAPI.liveUpdateMenu(Bukkit.getPlayer(member), ClaimMenu.class, "claimCount"));
        return null;
    }

    public static void claimChunk(World world, int x, int z, String teamId) {
        try {
            Connection connection = Database.getInstance().getConnection();

            String insertChunkQuery = "INSERT INTO claimedchunks (world, chunkx, chunkz, teamid) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertChunkQuery)) {
                pstmt.setString(1, world.getName());
                pstmt.setInt(2, x);
                pstmt.setInt(3, z);
                pstmt.setString(4, teamId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String unclaimChunkWrapper(World world, int x, int z, Team team) {
        Team chunkTeam = claimedChunks.get(world.getName()).get(x, z);
        if (chunkTeam == null) {
            return "unclaimed";
        } else if (!team.equals(chunkTeam)) {
            return "other";
        }

        claimedChunks.get(world.getName()).remove(x, z);
        claimedChunkCounts.merge(team, -1, Integer::sum);
        TeamController.unclaimChunk(world, x, z);
        team.members.forEach(member -> MapMenuAPI.liveUpdateMenu(Bukkit.getPlayer(member), ClaimMenu.class, "claimCount"));
        return null;
    }

    public static void unclaimChunk(World world, int x, int z) {
        try {
            Connection connection = Database.getInstance().getConnection();

            String deleteChunkQuery = "DELETE FROM claimedchunks WHERE world = ? AND chunkx = ? AND chunkz = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteChunkQuery)) {
                pstmt.setString(1, world.getName());
                pstmt.setInt(2, x);
                pstmt.setInt(3, z);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addAdditionalChunksToTeam(Team team, int count) {
        try {
            Connection connection = Database.getInstance().getConnection();

            String deleteChunkQuery = "UPDATE teams SET additionalchunks = additionalchunks + ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteChunkQuery)) {
                pstmt.setInt(1, count);
                pstmt.setString(2, team.id.toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void blockChunk(Location location) {
        int x = location.getChunk().getX();
        int z = location.getChunk().getZ();

        TeamController.blockedChunks.computeIfAbsent(location.getWorld().getName(), k -> new ArrayList<>()).add(new ImmutablePair<>(x, z));
        TeamController.claimChunk(location.getWorld(), x, z, "blocked");
    }

    public static void unblockChunk(Location location) {
        int x = location.getChunk().getX();
        int z = location.getChunk().getZ();

        TeamController.blockedChunks.computeIfAbsent(location.getWorld().getName(), k -> new ArrayList<>()).remove(new ImmutablePair<>(x, z));
        TeamController.unclaimChunk(location.getWorld(), location.getChunk().getX(), location.getChunk().getZ());
    }
}
