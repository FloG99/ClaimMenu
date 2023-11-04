package de.floskater99.mapmenu.mapMenus.claim;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import de.floskater99.mapmenu.MapMenu;
import de.floskater99.mapmenu.MapMenuAPI;
import de.floskater99.mapmenu.gui.*;
import de.floskater99.mapmenu.gui.Frame;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MinecraftFont;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;


public class ClaimMenu extends MapMenu {
    private int claimRange = 0; // Add to config if needed
    private final HashBasedTable<Integer, Integer, Team> claimedChunks;
    private final List<Pair<Integer, Integer>> blockedChunks;
    private final Map<Team, Integer> claimedChunkCounts = TeamController.claimedChunkCounts;
    private Team team;
    private final List<Team> teams;
    private static BufferedImage settingsBackground;
    private static BufferedImage settingsHoverBackground;
    private static BufferedImage switchTeamIcon;
    private static BufferedImage switchTeamHoverIcon;
    private int lookDir = -1; // 0 - 7, 45 degree intervals
    private int oldHighlightXKey;
    private int oldHighlightZKey;
    private Element claimAmount;

    static {
        try {
            settingsBackground = ImageIO.read(Objects.requireNonNull(ClaimMenu.class.getResourceAsStream("/images/Team.png")));
            settingsHoverBackground = ImageIO.read(Objects.requireNonNull(ClaimMenu.class.getResourceAsStream("/images/TeamHover.png")));
            switchTeamIcon = ImageIO.read(Objects.requireNonNull(ClaimMenu.class.getResourceAsStream("/images/Switch1.png")));
            switchTeamHoverIcon = ImageIO.read(Objects.requireNonNull(ClaimMenu.class.getResourceAsStream("/images/Switch1Hover.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ClaimMenu(Player player, ItemStack map) {
        super(player, map);

        Collection<Team> allTeams = TeamController.teams.values();
        teams = allTeams.stream()
                .filter(team -> team.members.contains(player.getUniqueId()))
                .sorted(Comparator.comparing(team -> team.owner.equals(player.getUniqueId()) ? 1 : 0))
                .toList();

        this.team = !teams.isEmpty() ? teams.get(0) : null;
        String teamUUID = PlayerDataController.get(player, "claimmenu:claimteam");
        if (teamUUID != null) {
            Team team = TeamController.teams.get(UUID.fromString(teamUUID));
            if (teams.contains(team)) {
                this.team = team;
            }
        }

        blockedChunks = TeamController.blockedChunks.computeIfAbsent(player.getWorld().getName(), k -> List.of());
        claimedChunks = TeamController.claimedChunks.computeIfAbsent(player.getWorld().getName(), k -> HashBasedTable.create());
        if (team != null) {
            claimedChunkCounts.putIfAbsent(team, 0);
        }
    }

    @Override
    protected void onLoad() {
        if (team != null) {
            Frame teamFrame = new Frame(0, 112, 128, 16);
            Element background = new ElementBuilder(0, 0, 128, 16)
                .setBackgroundColor(new Color(0, 0, 0, 190))
                .onClick(() -> {}) // Disable clicking on things in the background
                .onHoverStart(() -> {}) // Disable hover events on things in the background
                .build();
            teamFrame.addElement(background);
            
            int claimedChunkCount = claimedChunkCounts.get(team);
            claimAmount = new ElementBuilder(128 - 52, 0, 51, 16)
                .setText(claimedChunkCount + "/" + team.getMaxClaims(), new Color(255, 255, 255), TextAlignment.CENTER_RIGHT)
                .build();
            teamFrame.addElement(claimAmount);

            Element teamElement = new ElementBuilder(2, 0, 126, 16)
                .setText(team.teamName, new Color(255, 255, 255), TextAlignment.CENTER_LEFT)
                .build();
            teamFrame.addElement(teamElement);

            if (teams.size() > 1) {
                int maxTeamnameWidth = teams.stream().mapToInt(team -> MinecraftFont.Font.getWidth(team.teamName)).max().orElse(0);
                Element switchTeam = new ElementBuilder(Math.min(6 + maxTeamnameWidth, 128 - MinecraftFont.Font.getWidth(claimedChunkCount + "/" + team.getMaxClaims()) - 12), 3, 9, 9)
                    .setBackgroundImage(switchTeamIcon, false)
                    .onClick(() -> {
                        team = teams.stream().filter(t -> !t.equals(team)).findAny().orElse(null);
                        
                        PlayerDataController.put(player, "claimmenu:claimteam", team.id.toString());
                        claimedChunkCounts.putIfAbsent(team, 0);
                        teamElement.setStyleProperty(ElementProperty.TEXT, team.teamName);
                        this.refreshClaimAmount();
                    })
                    .build();
                switchTeam.setHoverProperty(ElementProperty.BACKGROUND_IMAGE, switchTeamHoverIcon);
                teamFrame.addElement(switchTeam);
            }
            
            window.showFrame(teamFrame);
        }
        
        Element settingsButton = new ElementBuilder(128 - 15, 0, 15, 15)
            .setBackgroundImage(settingsBackground, false)
            .build();
        settingsButton.hoverProperties.put(ElementProperty.BACKGROUND_IMAGE, settingsHoverBackground);
        settingsButton.onClick(() -> {
            MapMenuAPI.openMenu(player, ClaimMenuSettings.class);
        });
        window.addElement(settingsButton);

        canvas.mapDecorations.put("playerMarker", getPlayerMarker(getLookDir()));
    }

    @Override
    protected void onLeftClick(byte x, byte y) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("Use right-click.").color(ChatColor.RED).create());
    }

    @Override
    protected void onRightClick(byte x, byte y) {
        int xKey = Math.floorDiv(this.canvas.baseCoord.left + x, 16);
        int zKey = Math.floorDiv(this.canvas.baseCoord.right + y, 16);

        if (team == null) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("You need to be in a team to claim chunks.").color(ChatColor.RED).create());
            MapMenuAPI.openMenu(player, ClaimMenuSettings.class);
            return;
        }

        if (player.getWorld().getEnvironment() == World.Environment.THE_END && xzMinDistance(player.getLocation(), new Location(player.getWorld(), 0, 0, 0)) < 176) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("You can not claim the main end island.").color(ChatColor.RED).create());
            return;
        }

        if (isChunkBlocked(xKey, zKey)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("This chunk cannot be claimed.").color(ChatColor.RED).create());
            return;
        }

        if (isChunkOwnedByAnotherTeam(xKey, zKey)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("This chunk has already been claimed.").color(ChatColor.RED).create());
            return;
        }

        if (claimRange > 0 && !isClaimableChunk(xKey, zKey)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("Too close to another claim.").color(ChatColor.RED).create());
            return;
        }

        if (claimedChunks.contains(xKey, zKey)) {
            unclaimChunk(xKey, zKey);
        } else {
            claimChunk(xKey, zKey);
        }

        this.canvas.needsRerender = true;
    }

    private boolean isChunkOwnedByAnotherTeam(int xKey, int zKey) {
        Team chunkOwner = claimedChunks.get(xKey, zKey);
        return chunkOwner != null && !chunkOwner.equals(team);
    }

    private boolean isChunkBlocked(int xKey, int zKey) {
        return blockedChunks.contains(new ImmutablePair<>(xKey, zKey));
    }

    private int xzMinDistance(Location a, Location b) {
        return Math.abs(Math.min(b.getBlockX() - a.getBlockX(), b.getBlockZ() - a.getBlockZ()));
    }

    private boolean isClaimableChunk(int xKey, int zKey) {
        for (int claimX = xKey - claimRange; claimX <= xKey + claimRange; claimX++) {
            for (int claimZ = zKey - claimRange; claimZ <= zKey + claimRange; claimZ++) {
                if (isChunkOwnedByAnotherTeam(claimX, claimZ)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void unclaimChunk(int xKey, int zKey) {
        TeamController.unclaimChunkWrapper(player.getWorld(), xKey, zKey, team);
    }

    private void claimChunk(int xKey, int zKey) {
        int claimedChunkCount = claimedChunkCounts.get(team);
        if (claimedChunkCount + 1 > team.getMaxClaims()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("You cannot claim more chunks.").color(ChatColor.RED).create());
            return;
        }

        TeamController.claimChunkWrapper(player.getWorld(), xKey, zKey, team, false);
    }

    @Override
    public void liveUpdate(String key) {
        if ("claimCount".equals(key)) {
            refreshClaimAmount();
        }
    }

    private void refreshClaimAmount() {
        int claimedChunkCount = claimedChunkCounts.get(team);
        claimAmount.setStyleProperty(ElementProperty.TEXT, claimedChunkCount + "/" + team.getMaxClaims());
    }

    @Override
    protected void onMouseMove(int x, int y) {
        refreshHoverText(x, y);
        refreshPlayerMarker();
        refreshChunkHighlight(x, y);
    }

    private void refreshHoverText(int x, int z) {
        int xKey = Math.floorDiv(this.canvas.baseCoord.left + x, 16);
        int zKey = Math.floorDiv(this.canvas.baseCoord.right + z, 16);
        Team chunkOwner = claimedChunks.get(xKey, zKey);


        if (chunkOwner != null) {
            this.canvas.mapDecorations.put("hoverText", new MapDecoration(MapDecoration.Type.PLAYER_OFF_LIMITS, (byte) (this.cursorPositionX + 1), this.cursorPositionY, (byte) 0, CraftChatMessage.fromStringOrNull(chunkOwner.teamName)));
        } else {
            this.canvas.mapDecorations.put("hoverText", null);
        }
    }

    private int getLookDir() {
        return (int) ((player.getLocation().getYaw() + 540 + 22.5) % 360) / 45;
    }

    private MapDecoration getPlayerMarker(int lookDir) {
        int[] markerXMapping = new int[]{0, 1, 0, -1, -2, -3, -2, -1};
        int[] markerYMapping = new int[]{0, 1, 2, 3, 2, 1, 0, -1};

        return new MapDecoration(MapDecoration.Type.PLAYER_OFF_LIMITS, (byte) (1 + markerXMapping[lookDir]), (byte) (markerYMapping[lookDir]), (byte) (lookDir * 2), null);
    }

    private void refreshPlayerMarker() {
        int lookDir = getLookDir();
        if (lookDir != this.lookDir) {
            this.lookDir = lookDir;

            canvas.mapDecorations.put("playerMarker", getPlayerMarker(lookDir));
        }
    }

    private void refreshChunkHighlight(int x, int z) {
        int xCoord = this.canvas.baseCoord.left;
        int zCoord = this.canvas.baseCoord.right;
        int xKey = Math.floorDiv(xCoord + x, 16);
        int zKey = Math.floorDiv(zCoord + z, 16);

        if (oldHighlightXKey != xKey || oldHighlightZKey != zKey) {
            oldHighlightXKey = xKey;
            oldHighlightZKey = zKey;

            this.canvas.needsRerender = true;
        }
    }

    @Override
    protected void onRender() {
        int xCoord = this.canvas.baseCoord.left;
        int zCoord = this.canvas.baseCoord.right;

        for (Table.Cell<Integer, Integer, Team> claimedChunk : claimedChunks.cellSet()) {
            Integer chunkX = claimedChunk.getRowKey();
            Integer chunkZ = claimedChunk.getColumnKey();
            assert chunkX != null;
            assert chunkZ != null;

            // Skip chunks that don't need to be rendered
            if (chunkX * 16 > xCoord + 128 || (chunkX + 1) * 16 < xCoord || chunkZ * 16 > zCoord + 128 || (chunkZ + 1) * 16 < zCoord) {
                continue;
            }

            Team chunkOwner = claimedChunk.getValue();
            assert chunkOwner != null;
            int x, z;

            // Fill
            for (z = chunkZ * 16; z < (chunkZ + 1) * 16; z++) {
                for (x = chunkX * 16; x < (chunkX + 1) * 16; x++) {
                    this.canvas.shiftBasePixelColor(x - xCoord, z - zCoord, -40, -40, -40);
                }
            }

            // Right border
            if (!chunkOwner.equals(claimedChunks.get(chunkX + 1, chunkZ))) {
                x = (chunkX + 1) * 16 - 1;
                for (z = chunkZ * 16; z < (chunkZ + 1) * 16; z++) {
                    this.canvas.setPixel(x - xCoord, z - zCoord, chunkOwner.teamMapColor);
                }
            }


            // Left border
            if (!chunkOwner.equals(claimedChunks.get(chunkX - 1, chunkZ))) {
                x = chunkX * 16;
                for (z = chunkZ * 16; z < (chunkZ + 1) * 16; z++) {
                    this.canvas.setPixel(x - xCoord, z - zCoord, chunkOwner.teamMapColor);
                }
            }

            // Top border
            if (!chunkOwner.equals(claimedChunks.get(chunkX, chunkZ - 1))) {
                z = chunkZ * 16;
                for (x = chunkX * 16; x < (chunkX + 1) * 16; x++) {
                    this.canvas.setPixel(x - xCoord, z - zCoord, chunkOwner.teamMapColor);
                }
            }

            // Bottom border
            if (!chunkOwner.equals(claimedChunks.get(chunkX, chunkZ + 1))) {
                z = (chunkZ + 1) * 16 - 1;
                for (x = chunkX * 16; x < (chunkX + 1) * 16; x++) {
                    this.canvas.setPixel(x - xCoord, z - zCoord, chunkOwner.teamMapColor);
                }
            }
        }

        byte[] cursorPos = this.getCursorMapPosition();
        this.refreshHoverText(cursorPos[0], cursorPos[1]);

        int xKey = Math.floorDiv(xCoord + cursorPos[0], 16);
        int zKey = Math.floorDiv(zCoord + cursorPos[1], 16);

        if (!claimedChunks.contains(xKey, zKey) && !window.isObjectAt(cursorPos[0], cursorPos[1])) {
            for (int z = zKey * 16; z < (zKey + 1) * 16; z++) {
                for (int x = xKey * 16; x < (xKey + 1) * 16; x++) {
                    this.canvas.shiftBasePixelColor(x - xCoord, z - zCoord, 40, 40, 40);
                }
            }
        }

        if (team != null) {
            refreshClaimAmount();
        }
    }

    @Override
    protected void onScroll(int direction) {}

    @Override
    public boolean doRenderBase() {
        return true;
    }

    @Override
    public String getMapDisplayName() {
        return "§f- §6Claim Chunks§f -";
    }
}