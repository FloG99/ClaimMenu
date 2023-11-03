package de.floskater99.mapmenu;

import de.floskater99.mapmenu.mapMenus.claim.ClaimMenu;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftChatMessage;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;


public class MapMenuAPI {
    public static final WeakHashMap<HumanEntity, MapMenu> playersWithOpenMenu = new WeakHashMap<>();
    public static final WeakHashMap<Player, Location> playersLastLocation = new WeakHashMap<>();
    public static final WeakHashMap<MapMenu, Integer> mapMenuHash = new WeakHashMap<>();
    public static final int defaultSlot = 4;
    private static int taskID = -1;


    public static void openMenu(Player player, Class<? extends MapMenu> mapMenuClass) {
        MapMenu oldMenu = playersWithOpenMenu.get(player);

        MapMenu mapMenu = createMapMenu(player, mapMenuClass);
        if (mapMenu == null) {
            return;
        }

        playersWithOpenMenu.put(player, mapMenu);
        playersLastLocation.put(player, player.getLocation());

        showMap(player);

        if (oldMenu == null) {
            resetPitch(player, 69f);

            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.CREATIVE);
            }
        } else {
            mapMenu.cursorPositionX = oldMenu.cursorPositionX;
            mapMenu.cursorPositionY = oldMenu.cursorPositionY;
            mapMenu.removedItem = oldMenu.removedItem;
        }

        refreshMapMenuForPlayer(player);

        if (oldMenu == null) {
            startScheduler();
        }
    }

    private static MapMenu createMapMenu(Player player, Class<? extends MapMenu> mapMenuClass) {
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();

        MapView mapView = Bukkit.getMap(0);
        if (mapView == null) {
            mapView = Bukkit.createMap(player.getWorld());
        }
        for (org.bukkit.map.MapRenderer renderer : mapView.getRenderers()) {
            mapView.removeRenderer(renderer);
        }
        mapView.setCenterX(player.getLocation().getBlockX());
        mapView.setCenterZ(player.getLocation().getBlockZ());
        mapView.setScale(MapView.Scale.CLOSEST);
        mapView.setLocked(true);

        assert mapMeta != null;
        mapMeta.setMapView(mapView);
        mapItem.setItemMeta(mapMeta);

        MapMenu mapMenu = null;
        try {
            mapMenu = (MapMenu) mapMenuClass.getConstructors()[0].newInstance(player, mapItem);
            mapMeta.setDisplayName(mapMenu.getMapDisplayName());
            mapMenu.onLoad();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return mapMenu;
    }

    public static void closeMenu(Player player, boolean showOldItem) {
        if (showOldItem) showOldItem(player);

        player.setGameMode(playersWithOpenMenu.get(player).gameModeBefore);
        player.getInventory().setHeldItemSlot(playersWithOpenMenu.get(player).selectedSlotBefore);

        playersWithOpenMenu.remove(player);
        if (playersWithOpenMenu.isEmpty()) stopScheduler();
    }


    private static void displayItem(Player player, ItemStack item) {
        player.getInventory().setItem(defaultSlot, item);
    }

    public static void showMap(Player player) {
        player.getInventory().setHeldItemSlot(defaultSlot);
        displayItem(player, playersWithOpenMenu.get(player).map);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("Press Q to close").color(ChatColor.GREEN).create());
    }

    public static void showOldItem(Player player) {
        displayItem(player, playersWithOpenMenu.get(player).removedItem);
    }


    private static void startScheduler() {
        if (taskID != -1) return;

        int counter = 0;
        int worldBackgroundFPS = 1;

        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.instance, new Runnable() {
            long iteration = 0;

            @Override
            public void run() {
                for (Map.Entry<HumanEntity, MapMenu> entry : playersWithOpenMenu.entrySet()) {
                    Player player = (Player) entry.getKey();
                    if (Bukkit.getServer().getOnlinePlayers().contains(player)) {
                        MapMenu menu = playersWithOpenMenu.get(player);
                        Location before = playersLastLocation.get(player);
                        Location now = player.getLocation();

                        boolean updateMap = false;
                        boolean updateCursor = false;

                        if (menu.doRenderBase() && (before.getBlockX() != now.getBlockX() || before.getBlockZ() != now.getBlockZ()) && iteration % worldBackgroundFPS == 0) {
                            menu.canvas.refreshBase(player);
                            updateMap = true;
                        }

                        if (before.getPitch() != now.getPitch() || before.getYaw() != now.getYaw()) {
                            float actualYawChange = now.getYaw() - before.getYaw();
                            if (Math.abs(actualYawChange) >= 180)
                                actualYawChange = Math.signum(-actualYawChange) * (360 - Math.abs(actualYawChange));
                            float yawChange = Math.max(-22.5f, Math.min(22.5f, actualYawChange)) + 67.5f;

                            if (now.getPitch() < 45f) {
                                resetPitch(player, 45f);
                            }

                            menu.addCursorX(rotationToMapDistance(yawChange, -128, 127));
                            menu.cursorPositionY = rotationToMapDistance(now.getPitch(), -119, 127);

                            menu.window.hoverUpdate(menu.getCursorMapPosition()[0], menu.getCursorMapPosition()[1]);
                            updateCursor = true;

                            playersLastLocation.put(player, now);
                        }

                        Integer hashCode = menu.window.hashCode();
                        if (!hashCode.equals(mapMenuHash.get(menu))) {
                            mapMenuHash.put(menu, hashCode);
                            updateMap = true;
                        }

                        if (menu.canvas.needsRerender) {
                            updateMap = true;
                            menu.canvas.needsRerender = false;
                        }

                        if (updateMap || updateCursor) {
                            if (updateMap) {
                                menu.render();
                            }

                            if (updateCursor) {
                                menu.mouseMove();
                            }

                            updateMap(player, menu);
                        }
                    }
                }
                iteration++;
            }
        }, 5L, 1L);
    }

    private static void stopScheduler() {
        if (taskID == -1) return;

        Bukkit.getScheduler().cancelTask(taskID);
        taskID = -1;
    }

    public static void updateMap(Player player, MapMenu menu) {
        MapDecoration cursor = new MapDecoration(MapDecoration.Type.byIcon(menu.defaultCursor.getRawType()), menu.cursorPositionX, menu.cursorPositionY, menu.defaultCursor.getDirection(), CraftChatMessage.fromStringOrNull(menu.defaultCursor.getCaption()));
        List<MapDecoration> combinedList = new ArrayList<>(menu.canvas.mapDecorations.values().stream().filter(Objects::nonNull).toList());
        combinedList.add(cursor);

        MapItemSavedData.MapPatch mapPatch = new MapItemSavedData.MapPatch(0, 0, 128, 128, menu.canvas.buffer);

        Packet<ClientGamePacketListener> mapPacket = new ClientboundMapItemDataPacket(((MapMeta) menu.map.getItemMeta()).getMapId(), ((MapMeta) menu.map.getItemMeta()).getMapView().getScale().getValue(), false, combinedList, mapPatch);
        sendPacket(player, mapPacket);
    }

    public static void sendPacket(Player player, Packet<?> packet) {
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    private static void resetPitch(Player player, float pitch) {
        ClientboundPlayerPositionPacket posPacket = new ClientboundPlayerPositionPacket(0, 0, 0, 0, pitch, Set.of(
            RelativeMovement.X,
            RelativeMovement.Y,
            RelativeMovement.Z,
            RelativeMovement.Y_ROT), Integer.MAX_VALUE);

        sendPacket(player, posPacket);
    }

    private static byte rotationToMapDistance(float value, int minOut, int maxOut) {
        if (value < 45 || value > 90) value = Math.max(45, Math.min(90, value));
        float in_min = 45;
        float in_max = 90;
        return (byte) (Math.round((value - in_min) * (maxOut - minOut) / (in_max - in_min) + minOut));
    }

    public static void leftClickEvent(Player player) {
        if (!playersWithOpenMenu.containsKey(player)) return;

        playersWithOpenMenu.get(player).leftClick();
    }

    public static void rightClickEvent(Player player) {
        if (!playersWithOpenMenu.containsKey(player)) return;

        playersWithOpenMenu.get(player).rightClick();
    }

    public static void scrollEvent(Player player, int direction) {
        if (!playersWithOpenMenu.containsKey(player)) return;

        playersWithOpenMenu.get(player).scroll(direction);

    }

    public static void refreshMapMenuForPlayer(OfflinePlayer player) {
        if (!player.isOnline()) {
            return;
        }

        MapMenu invitedPlayerMapMenu = MapMenuAPI.playersWithOpenMenu.get(player.getPlayer());
        if (invitedPlayerMapMenu != null) {
            invitedPlayerMapMenu.render();
            MapMenuAPI.updateMap(player.getPlayer(), invitedPlayerMapMenu);
        }
    }

    public static void liveUpdateMenu(OfflinePlayer player, Class<? extends MapMenu> mapMenuType, String key) {
        if (player == null || !player.isOnline()) {
            return;
        }

        MapMenu mapMenu = MapMenuAPI.playersWithOpenMenu.get(player.getPlayer());
        if (mapMenu == null || !mapMenu.getClass().isAssignableFrom(mapMenuType)) {
            return;
        }

        mapMenu.liveUpdate(key);
        MapMenuAPI.updateMap(player.getPlayer(), mapMenu);
    }
}
