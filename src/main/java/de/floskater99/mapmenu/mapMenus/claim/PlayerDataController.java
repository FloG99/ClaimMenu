package de.floskater99.mapmenu.mapMenus.claim;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerDataController implements Listener {
    private static final Map<UUID, Map<String, String>> data = new HashMap<>();

    public static String get(Player player, String key) {
        UUID uuid = player.getUniqueId();
        if (!data.containsKey(uuid)) {
            return null;
        }

        return data.get(uuid).get(key);
    }
    
    public static void put(Player player, String key, String value) {
        put(player.getUniqueId(), key, value);
    }

    public static void put(UUID uuid, String key, String value) {
        data.computeIfAbsent(uuid, x -> new HashMap<>()).put(key, value);

        try {
            Connection connection = Database.getInstance().getConnection();

            String query = "UPDATE userdata SET value=? WHERE userid=? AND `key`=?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, value);
                pstmt.setString(2, uuid.toString());
                pstmt.setString(3, key);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void loadDataForPlayers(Collection<? extends Player> players) {
        if (players.isEmpty()) {
            return;
        }

        try {
            Statement statement = Database.getInstance().getConnection().createStatement();

            String playerList = players.stream().map(p -> "'" + p.getUniqueId() + "'").collect(Collectors.joining(", "));
            String query = "SELECT * FROM userdata WHERE userid in (" + playerList + ")";
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                UUID playerUUID = UUID.fromString(resultSet.getString("userid"));
                String key = resultSet.getString("key");
                String value = resultSet.getString("value");
                put(playerUUID, key, value);
            }

            resultSet.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static void initializePlayerData() {
        loadDataForPlayers(Bukkit.getOnlinePlayers());
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        loadDataForPlayers(List.of(e.getPlayer()));
    }
    
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        data.remove(uuid);
    }
}