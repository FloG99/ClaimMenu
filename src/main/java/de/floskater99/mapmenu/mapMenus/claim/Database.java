package de.floskater99.mapmenu.mapMenus.claim;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import de.floskater99.mapmenu.Main;
import org.bukkit.configuration.file.FileConfiguration;

public class Database {

    private static Database instance;
    private Connection connection;

    private Database() {
    }

    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public void initializeConnection(FileConfiguration config) throws Exception {
        String jdbcUrl = config.getString("database.url");
        String username = config.getString("database.username");
        String password = config.getString("database.password");

        Class.forName("org.mariadb.jdbc.Driver");
        connection = DriverManager.getConnection(jdbcUrl, username, password);
    }

    public Connection getConnection() {
        FileConfiguration config = Main.getInstance().getConfig();

        String jdbcUrl = config.getString("database.url");
        String username = config.getString("database.username");
        String password = config.getString("database.password");

        if (connection == null) {
            throw new IllegalStateException("Connection not initialized");
        }

        try {
            if (connection.isClosed()) {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
            }
        } catch (SQLException e) {
            try {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }

        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
