package de.floskater99.mapmenu.mapMenus.claim;

import com.google.common.base.Objects;
import de.floskater99.mapmenu.Main;
import de.floskater99.mapmenu.MapMenu;
import org.bukkit.OfflinePlayer;
import org.bukkit.map.MapPalette;

import java.awt.*;
import java.util.Set;
import java.util.UUID;

public class Team {
    public UUID owner;
    public Set<UUID> members;
    public UUID id;
    public String teamName;
    public Color teamColor;
    public byte teamMapColor;
    public int additionalChunks;

    public Team(UUID id, UUID owner, Set<UUID> members, String teamName, Color teamColor, int additionalChunks) {
        this.owner = owner;
        this.members = members;
        this.id = id;
        this.teamName = teamName;
        this.additionalChunks = additionalChunks;
        setTeamColor(teamColor);
    }

    public void setTeamColor(Color teamColor) {
        this.teamColor = teamColor;
        this.teamMapColor = teamColor == null ? (byte) 0 : MapPalette.matchColor(teamColor);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public void setOwner(UUID uuid) {
        owner = uuid;
        TeamController.updateTeam(this);
    }

    public int getMaxClaims() {
        int defaultCount = Main.instance.getConfig().getInt("teams.chunksDefault");
        int perPlayerCount = Main.instance.getConfig().getInt("teams.chunksPerPlayer");
        return defaultCount + members.size() * perPlayerCount + additionalChunks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return Objects.equal(id, team.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(owner, teamName, teamColor);
    }
}
