package de.floskater99.mapmenu.listeners;

import de.floskater99.mapmenu.Main;
import de.floskater99.mapmenu.MapMenuAPI;
import de.floskater99.mapmenu.mapMenus.claim.ClaimMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;

public class Listeners implements Listener {
    @EventHandler
    public void onScroll(PlayerItemHeldEvent e) {
        if (!MapMenuAPI.playersWithOpenMenu.containsKey(e.getPlayer())) return;

        //- : left, + : right
        MapMenuAPI.scrollEvent(e.getPlayer(), e.getNewSlot() - e.getPreviousSlot());
        if (e.getNewSlot() != MapMenuAPI.defaultSlot) {
            e.setCancelled(true);
            Bukkit.getScheduler().runTaskLater(Main.instance, () -> {
                e.getPlayer().getInventory().setHeldItemSlot(MapMenuAPI.defaultSlot);
            }, 0);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) { //Fires when opening Creative inventory, as well as InventoryCreativeEvent
        if (!MapMenuAPI.playersWithOpenMenu.containsKey(e.getWhoClicked())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!MapMenuAPI.playersWithOpenMenu.containsKey(e.getPlayer())) return;

        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            MapMenuAPI.leftClickEvent(e.getPlayer());
            e.setCancelled(true);
        } else if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK){
            if (e.getHand() == EquipmentSlot.HAND) MapMenuAPI.rightClickEvent(e.getPlayer());
            e.setCancelled(true);
        }
    }



    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!MapMenuAPI.playersWithOpenMenu.containsKey(e.getEntity())) return;

        e.getDrops().remove(MapMenuAPI.playersWithOpenMenu.get(e.getEntity()).map);
        e.getDrops().add(MapMenuAPI.playersWithOpenMenu.get(e.getEntity()).removedItem);
        MapMenuAPI.closeMenu(e.getEntity(), false);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        if (!MapMenuAPI.playersWithOpenMenu.containsKey(e.getPlayer())) return;

        MapMenuAPI.closeMenu(e.getPlayer(), true);
    }

    @EventHandler
    public void onThrowItem(PlayerDropItemEvent e) {
        if (!MapMenuAPI.playersWithOpenMenu.containsKey(e.getPlayer())) return;

        if (e.getPlayer().getInventory().getHeldItemSlot() == MapMenuAPI.defaultSlot) {
            e.getItemDrop().remove();
            MapMenuAPI.closeMenu(e.getPlayer(), true);
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemSwap(PlayerSwapHandItemsEvent e) {
        if (MapMenuAPI.playersWithOpenMenu.containsKey(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent e) {
        if (!MapMenuAPI.playersWithOpenMenu.containsKey(e.getPlayer())) return;

        // Refresh the map to display the new world
        if (MapMenuAPI.playersWithOpenMenu.get(e.getPlayer()).getClass().equals(ClaimMenu.class)) {
            MapMenuAPI.openMenu(e.getPlayer(), ClaimMenu.class);
        }
    }

    @EventHandler
    public void onInteractAtItemFrame(PlayerInteractEntityEvent e) {
        if (!MapMenuAPI.playersWithOpenMenu.containsKey(e.getPlayer())) return;

        EntityType type = e.getRightClicked().getType();

        if (type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME) {
            e.setCancelled(true);
        }
    }
}
