package de.floskater99.mapmenu;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.floskater99.mapmenu.commands.AdminCommands;
import de.floskater99.mapmenu.commands.ClaimCommand;
import de.floskater99.mapmenu.commands.TeamManagerCommand;
import de.floskater99.mapmenu.listeners.Listeners;
import de.floskater99.mapmenu.listeners.ClaimListener;
import de.floskater99.mapmenu.mapMenus.claim.Database;
import de.floskater99.mapmenu.mapMenus.claim.PlayerDataController;
import de.floskater99.mapmenu.mapMenus.claim.TeamController;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.HandlerList;

import java.sql.Statement;
import java.util.Map;
import java.util.Objects;

public class Main extends JavaPlugin {
    public static Main instance;
    private Database database;


    public static Main getInstance() {
        return instance;
    }

    @Override

    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        instance = this;

        FileConfiguration config = getConfig();

        try {
            database = Database.getInstance();
            database.initializeConnection(config);
            initDatabase();

        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("An error occurred during database initialization!");
            setEnabled(false);
            return;
        }

        TeamController.initializeTeams();
        TeamController.initializeClaimedChunks();
        PlayerDataController.initializePlayerData();

        Bukkit.getPluginManager().registerEvents(new Listeners(), this);
        Bukkit.getPluginManager().registerEvents(new ClaimListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDataController(), this);


        Objects.requireNonNull(getCommand("claim")).setExecutor(new ClaimCommand());
        AdminCommands adminCommands = new AdminCommands();
        Objects.requireNonNull(getCommand("refresh")).setExecutor(adminCommands);
        Objects.requireNonNull(getCommand("refresh")).setTabCompleter(adminCommands);
        Objects.requireNonNull(getCommand("refresh")).setPermission("bukkit.command.op");
        TeamManagerCommand teamManagerCommand = new TeamManagerCommand();
        Objects.requireNonNull(getCommand("teammanager")).setExecutor(teamManagerCommand);
        Objects.requireNonNull(getCommand("teammanager")).setTabCompleter(teamManagerCommand);
        Objects.requireNonNull(getCommand("teammanager")).setPermission("bukkit.command.op");

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this,
            ListenerPriority.NORMAL, PacketType.Play.Server.MAP) {
            @Override
            public void onPacketSending(PacketEvent event) {
                ClientboundMapItemDataPacket mapPacket = (ClientboundMapItemDataPacket) event.getPacket().getHandle();
                if (mapPacket.getMapId() == 0 && mapPacket.isLocked()) {
                    event.setCancelled(true);
                }
            }
        });
    }

    public void initDatabase() throws Exception {
        Statement statement = database.getConnection().createStatement();

        String createTableTeams = "CREATE TABLE IF NOT EXISTS teams (" +
            "id varchar(36) PRIMARY KEY NOT NULL," +
            "name varchar(20) NOT NULL," +
            "color varchar(6) NOT NULL," +
            "owner varchar(36) NOT NULL," +
            "additionalchunks INTEGER DEFAULT 0" +
            ");";

        String createTableTeamMembers = "CREATE TABLE IF NOT EXISTS teammembers (" +
            "teamid varchar(36) NOT NULL," +
            "userid varchar(36) NOT NULL," +
            "CONSTRAINT teammembers_PK PRIMARY KEY (teamid,userid)," +
            "CONSTRAINT teammembers_FK FOREIGN KEY (teamid) REFERENCES teams(id) ON DELETE CASCADE ON UPDATE CASCADE" +
            ");";

        String createTableClaimedChunks = "CREATE TABLE IF NOT EXISTS claimedchunks (" +
            "world varchar(255) NOT NULL," +
            "chunkx INTEGER NOT NULL," +
            "chunkz INTEGER NOT NULL," +
            "teamid varchar(36) NOT NULL," +
            "CONSTRAINT claimedchunks_PK PRIMARY KEY (world,chunkx,chunkz)," +
            "CONSTRAINT claimedchunks_FK FOREIGN KEY (teamid) REFERENCES teams(id) ON DELETE CASCADE ON UPDATE CASCADE" +
            ");";
        
        String createTableUserData = "CREATE TABLE IF NOT EXISTS userdata (" +
            "userid varchar(36) NOT NULL," +
            "`key` varchar(100) NOT NULL," +
            "value LONGTEXT NULL," +
            "CONSTRAINT userdata_PK PRIMARY KEY (userid,`key`)" +
            ");";
        
        String createTableTeamAlliances = "CREATE TABLE IF NOT EXISTS teamalliances (" +
            "team1 varchar(36) NOT NULL," +
            "team2 varchar(36) NOT NULL," +
            "CONSTRAINT teamalliances_PK PRIMARY KEY (team1,team2)" +
            ");";
        
        if (statement.executeUpdate(createTableTeams) > 0) {
            getLogger().info("Generated missing table TEAMS");
        }
            
        if (statement.executeUpdate(createTableTeamMembers) > 0) {
            getLogger().info("Generated missing table TEAMMEMBERS");
        }
            
        if (statement.executeUpdate(createTableClaimedChunks) > 0) {
            getLogger().info("Generated missing table CLAIMEDCHUNKS");
        }
        
        if (statement.executeUpdate(createTableUserData) > 0) {
            getLogger().info("Generated missing table USERDATA");
        }
        
        if (statement.executeUpdate(createTableTeamAlliances) > 0) {
            getLogger().info("Generated missing table TEAMALLIANCES");
        }

        statement.close();
    }

    @Override
    public void onDisable() {
        for (Map.Entry<HumanEntity, MapMenu> entry : MapMenuAPI.playersWithOpenMenu.entrySet()) {
            MapMenuAPI.showOldItem((Player) entry.getKey());
        }

        HandlerList.unregisterAll(this);

        ProtocolLibrary.getProtocolManager().removePacketListeners(this);

        database.closeConnection();
    }
}
