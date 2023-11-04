package de.floskater99.mapmenu.listeners

import de.floskater99.mapmenu.Main
import de.floskater99.mapmenu.MapMenuAPI
import de.floskater99.mapmenu.mapMenus.claim.Team
import de.floskater99.mapmenu.mapMenus.claim.TeamController
import org.apache.commons.lang3.StringUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.EnderChest
import org.bukkit.block.data.Directional
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.vehicle.VehicleCreateEvent
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import java.util.*


class ClaimListener : Listener {
    private var tntTeam: String? = null
    private val causedByTeamKey = NamespacedKey(Main.instance, "causedByTeam")
    
    private fun canAccessChunk(location: Location, player: Player): Boolean {
        val claimTeam = TeamController.claimedChunks[location.world!!.name]?.get(location.chunk.x, location.chunk.z)
        return claimTeam == null || claimTeam.members.contains(player.uniqueId) || player.isOp
    }

    private fun getClaimTeam(location: Location): Team? {
        return TeamController.claimedChunks[location.world!!.name]?.get(location.chunk.x, location.chunk.z)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!canAccessChunk(event.block.location, event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (!canAccessChunk(event.block.location, event.player)) {
            event.isCancelled = true
        }
    }
    
    @EventHandler
    fun onBlockMultiPlace(event: BlockMultiPlaceEvent) {
        if (event.replacedBlockStates.any { location -> !canAccessChunk(location.block.location, event.player) }) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockExplode(event: BlockExplodeEvent) {
        val explodingBlockTeam = getClaimTeam(event.block.location)
        val affectedBlocks = event.blockList()
        val forbiddenAffectedBlocks = affectedBlocks.filter {
            val blockTeam = getClaimTeam(it.location)
            println(blockTeam)
            blockTeam != null && blockTeam != explodingBlockTeam
        }.map { it.location }

        // Remove claimed blocks from the list of affected blocks
        affectedBlocks.removeIf { forbiddenAffectedBlocks.contains(it.location) }
    }
    
    @EventHandler
    fun onBlockDispenseAtEdge(event: BlockDispenseEvent) {
        val claimTeamFrom = getClaimTeam(event.block.location)
        val dispenserData = event.block.blockData as Directional
        val blockInFront = event.block.getRelative(dispenserData.facing)
        val claimTeamTo = getClaimTeam(blockInFront.location)
        if (claimTeamTo != null && claimTeamFrom != claimTeamTo) {
            event.isCancelled = true
            tntTeam = null
            return
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockDispense(event: BlockDispenseEvent) {
        if (event.item.type == Material.TNT || event.item.type == Material.TNT_MINECART) {
            val claimTeam = getClaimTeam(event.block.location)
            tntTeam = claimTeam?.id?.toString() ?: ""
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun onTNTPrime(event: TNTPrimeEvent) {
        val primingBlock = event.primingBlock
        val playerID = if (event.primingEntity is Player) (event.primingEntity as Player).uniqueId else null

        val primeTeams =
            if (primingBlock != null) listOf(getClaimTeam(primingBlock.location))
            else TeamController.teams.values.filter { team -> team.members.contains(playerID) }
        
        tntTeam = primeTeams.joinToString(";") { primeTeam -> primeTeam?.id?.toString() ?: "" }
    }

    // Is guaranteed to be called after BlockDispenseEvent/TNTPrimeEvent if TNT is dispensed, because events are called in order (BlockDispenseEvent/TNTPrimeEvent -> EntitySpawnEvent)
    @EventHandler
    fun onCreatureSpawn(event: EntitySpawnEvent) {
        if (tntTeam != null) {
            event.entity.persistentDataContainer.set(causedByTeamKey, PersistentDataType.STRING, tntTeam!!)

            tntTeam = null
        }
    }
    
    @EventHandler
    fun onTNTMinecartPlace(event: VehicleCreateEvent) {
        if (tntTeam != null) {
            event.vehicle.persistentDataContainer.set(causedByTeamKey, PersistentDataType.STRING, tntTeam!!)
        } else if (event.vehicle.type == EntityType.MINECART_TNT) {
            val claimTeam = getClaimTeam(event.vehicle.location)
            event.vehicle.persistentDataContainer.set(causedByTeamKey, PersistentDataType.STRING, claimTeam?.id?.toString() ?: "");
        }
    }
    
    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (event.cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || event.cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            if (getClaimTeam(entity.location) != null) {
                event.isCancelled = true
            }
        }

        if (event.cause == EntityDamageEvent.DamageCause.FALL) {
            val team = getClaimTeam(entity.location);
            if (team != null && "Spawn" == team.teamName) {
                event.isCancelled = true
            }
        }
    }
    
    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        val entity = event.entity
        if (event.entityType == EntityType.PRIMED_TNT || event.entityType == EntityType.MINECART_TNT) {
            val explosionCauseTeams =
                if (entity is TNTPrimed && entity.source is Player)
                    TeamController.teams.values.filter { team -> team.members.contains(entity.source!!.uniqueId) }
                else {
                    val str = entity.persistentDataContainer[causedByTeamKey, PersistentDataType.STRING]
                    if (StringUtils.isEmpty(str)) null else str?.split(";")?.map { teamUUID -> TeamController.teams[UUID.fromString(teamUUID)] } 
                }

            val affectedBlocks = event.blockList()
            val forbiddenAffectedBlocks = affectedBlocks.filter {
                val blockTeam = getClaimTeam(it.location)
                blockTeam != null && (explosionCauseTeams == null || !explosionCauseTeams.contains(blockTeam))
            }.map { it.location }

            // Remove claimed blocks from the list of affected blocks
            affectedBlocks.removeIf { forbiddenAffectedBlocks.contains(it.location) }
        } else if (event.entityType === EntityType.CREEPER || event.entityType == EntityType.ENDER_CRYSTAL) {
            val affectedBlocks = event.blockList()
            val forbiddenAffectedBlocks = affectedBlocks.filter {
                val blockTeam = getClaimTeam(it.location)
                blockTeam != null
            }.map { it.location }

            // Remove claimed blocks from the list of affected blocks
            affectedBlocks.removeIf { forbiddenAffectedBlocks.contains(it.location) }
        }
    }

    @EventHandler
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        val pistonClaimTeam = getClaimTeam(event.block.location)
        val direction = event.direction

        if (event.blocks.any {
            val targetLocationTeam = getClaimTeam(it.location.add(Vector(direction.modX, direction.modY, direction.modZ)))
            targetLocationTeam != null && targetLocationTeam != pistonClaimTeam
        }) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        val pistonClaimTeam = getClaimTeam(event.block.location)
        
        if (event.blocks.any { getClaimTeam(it.location) != null && getClaimTeam(it.location) != pistonClaimTeam }) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        var damager = event.damager
        val entity = event.entity

        if (damager is Projectile) {
            val damagerProj = damager
            if (damagerProj.shooter is Player) {
                damager = damagerProj.shooter as Player
            }
        }

        if (damager is Player) {
            if (!canAccessChunk(entity.location, damager)) {
                event.isCancelled = true
            }
        }
    }
    
    @EventHandler
    fun onVehileDestroy(event: VehicleDestroyEvent) {
        val attacker = event.attacker

        if (attacker !is Player || !canAccessChunk(event.vehicle.location, attacker)) {
            event.isCancelled = true
            return;
        }
    }
    
    @EventHandler
    fun onFireSpread(event: BlockSpreadEvent) {
        val claimTeamSource = getClaimTeam(event.source.location)
        val claimTeamTarget = getClaimTeam(event.block.location)
        if (claimTeamTarget != null && claimTeamTarget != claimTeamSource) {
            event.isCancelled = true
        }
    }
    
    @EventHandler
    fun onWaterLavaFlow(event: BlockFromToEvent) {
        val claimTeamSource = getClaimTeam(event.block.location)
        val claimTeamTarget = getClaimTeam(event.toBlock.location)
        if (claimTeamTarget != null && claimTeamTarget != claimTeamSource) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onVehicleMove(event: VehicleMoveEvent) {
        if (!event.vehicle.passengers.filterIsInstance<Player>().any { player -> canAccessChunk(player.location, player) }) {
            event.vehicle.teleport(event.from)
        }
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (!canAccessChunk(event.rightClicked.location, event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val clickedBlock = event.clickedBlock ?: return

        if (event.clickedBlock != null && event.clickedBlock !is EnderChest) {
            if (!canAccessChunk(clickedBlock.location, event.player)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (MapMenuAPI.playersWithOpenMenu.containsKey(event.player) && event.player.inventory.heldItemSlot == MapMenuAPI.defaultSlot) return // Handled in Listeners.java

        if (!canAccessChunk(event.itemDrop.location, event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onChunkEnter(event: PlayerMoveEvent) {
        val to = event.to ?: return

        val player = event.player
        val fromChunkTeam = getClaimTeam(event.from)
        val toChunkTeam = getClaimTeam(to)

        if (toChunkTeam != null && toChunkTeam != fromChunkTeam) {
            player.sendTitle(net.md_5.bungee.api.ChatColor.of(toChunkTeam.teamColor).toString() + toChunkTeam.teamName, "§3Entering claimed area", 5, 40, 5)
        } else if (fromChunkTeam != null && toChunkTeam == null) {
            player.sendTitle("§2Wilderness", "§3Leaving claimed area", 5, 40, 5)
        }
    }
    
    
    

    
    
    

    
}
