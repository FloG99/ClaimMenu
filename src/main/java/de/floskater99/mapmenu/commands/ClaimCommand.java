package de.floskater99.mapmenu.commands;

import de.floskater99.mapmenu.MapMenuAPI;
import de.floskater99.mapmenu.mapMenus.claim.ClaimMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClaimCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return false;

        if (cmd.getName().equalsIgnoreCase("claim")) {
            if (MapMenuAPI.playersWithOpenMenu.containsKey(player)) {
                MapMenuAPI.closeMenu(player, true);
            } else {
                MapMenuAPI.openMenu(player, ClaimMenu.class);
            }
        }

        return false;
    }
}
