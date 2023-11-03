package de.floskater99.mapmenu

import de.floskater99.mapmenu.mapMenus.claim.Team
import de.floskater99.mapmenu.mapMenus.claim.TeamController
import org.bukkit.Location
import java.util.*


class MapMenuWrapper {


    fun getChunks(team: Team): Any {
        return TeamController.claimedChunks.entries.filter { chunkEntry -> chunkEntry.value == team }
            .map { it.key }
    }


    fun getOwner(team: Team): UUID {
        return team.owner
    }

    fun getTeam(location: Location): Team {
        return TeamController.claimedChunks[location.world!!.name]!!.get(location.chunk.x, location.chunk.z)!!
    }


    companion object {

        @JvmField
        val instance = MapMenuWrapper()
    }


}