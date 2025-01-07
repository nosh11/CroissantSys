package com.github.sheauoian.croissantsys

import com.github.sheauoian.croissantsys.command.MenuCmd
import com.github.sheauoian.croissantsys.command.StatusCmd
import com.github.sheauoian.croissantsys.command.argument.CMobArgument
import com.github.sheauoian.croissantsys.command.argument.CStoreArgument
import com.github.sheauoian.croissantsys.command.argument.EDataArgument
import com.github.sheauoian.croissantsys.command.argument.UDataOnlineArgument
import com.github.sheauoian.croissantsys.command.argument.WarpPointArgument
import com.github.sheauoian.croissantsys.command.context.UDataContextProvider
import com.github.sheauoian.croissantsys.command.op.CMobCmd
import com.github.sheauoian.croissantsys.command.op.CStoreCmd
import com.github.sheauoian.croissantsys.command.op.UserFlagCmd
import com.github.sheauoian.croissantsys.command.op.WarpPointSettingCmd
import com.github.sheauoian.croissantsys.command.op.WearingCmd
import com.github.sheauoian.croissantsys.command.op.equipment.EquipmentCommand
import com.github.sheauoian.croissantsys.discord.RabbitBot
import com.github.sheauoian.croissantsys.listener.ElevatorListener
import com.github.sheauoian.croissantsys.mining.CToolCmd
import com.github.sheauoian.croissantsys.mining.MiningListener
import com.github.sheauoian.croissantsys.user.listener.PlayerJoinListener
import com.github.sheauoian.croissantsys.pve.DamageListener
import com.github.sheauoian.croissantsys.pve.equipment.data.EDataManager
import com.github.sheauoian.croissantsys.pve.equipment.data.EquipmentData
import com.github.sheauoian.croissantsys.pve.equipment.listener.EquipmentStoringListener
import com.github.sheauoian.croissantsys.pve.equipment.weapon.WeaponListener
import com.github.sheauoian.croissantsys.pve.mob.CMob
import com.github.sheauoian.croissantsys.pve.mob.CMobListener
import com.github.sheauoian.croissantsys.store.CStore
import com.github.sheauoian.croissantsys.store.CStoreManager
import com.github.sheauoian.croissantsys.store.trait.CStoreTrait
import com.github.sheauoian.croissantsys.user.UserDataManager
import com.github.sheauoian.croissantsys.user.UserRunnable
import com.github.sheauoian.croissantsys.user.listener.SkillListener
import com.github.sheauoian.croissantsys.user.online.UserDataOnline
import com.github.sheauoian.croissantsys.world.listener.HologramListener
import com.github.sheauoian.croissantsys.world.warppoint.WarpPoint
import com.github.sheauoian.croissantsys.world.warppoint.WarpPointManager
import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.adventure.LiteAdventureExtension
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.trait.TraitInfo
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class CroissantSys: JavaPlugin() {
    companion object {
        lateinit var instance: CroissantSys
        fun broadcast(message: String) {
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(message))
        }
    }

    private var liteCommands: LiteCommands<CommandSender>? = null
    var rabbitDiscordBot: RabbitBot? = null

    override fun onEnable() {
        instance = this
        saveDefaultConfig()

        liteCommandSetup()
        eventSetup()

        EDataManager.instance.reload()
        UserRunnable().runTaskTimer(this, 5, 2)
        WarpPointManager.reload()
        CStoreManager.instance.reload()
        CMob.CMobManager.reload()

        val citizens = server.pluginManager.getPlugin("Citizens")
        if (citizens == null)
            logger.log(Level.SEVERE, "Citizens 2.0 not found")
        else if (!citizens.isEnabled)
            logger.log(Level.SEVERE, "Citizens 2.0 not enabled")
        else {
            try {
                CitizensAPI.getTraitFactory().registerTrait(
                    TraitInfo.create(CStoreTrait::class.java)
                )
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "an error occur while registering CStoreTrait")
                e.printStackTrace()
            }
        }

        discordSetup()
    }

    override fun onDisable() {
        saveConfig()
        UserDataManager.instance.saveAll()
        EDataManager.instance.saveAll()
        DbDriver.close()
    }

    private fun discordSetup() {
        val token = config.getString("discord_token")
        val guild = config.getString("discord_guild")
        if (token != null && guild != null) rabbitDiscordBot = RabbitBot(token, guild)
    }

    private fun liteCommandSetup() {
        liteCommands = LiteBukkitFactory
            .builder("sys", this)
            .extension(LiteAdventureExtension()) { config ->
                config.miniMessage(true)
            }
            .commands(
                EquipmentCommand(),
                StatusCmd(),
                MenuCmd(),
                WarpPointSettingCmd(),
                WearingCmd(),
                UserFlagCmd(),
                CStoreCmd(),
                CToolCmd(),
                CMobCmd()
            )
            .argument(
                EquipmentData::class.java, EDataArgument()
            )
            .argument(
                WarpPoint::class.java, WarpPointArgument()
            )
            .argument(
                UserDataOnline::class.java, UDataOnlineArgument()
            )
            .argument(
                CStore::class.java, CStoreArgument()
            )
            .argument(
                CMob::class.java, CMobArgument()
            )
            .context(
                UserDataOnline::class.java, UDataContextProvider()
            )
            .build()
    }

    private fun eventSetup() {
        val manager = Bukkit.getPluginManager()
        manager.registerEvents(PlayerJoinListener(), this)
        manager.registerEvents(DamageListener(), this)
        manager.registerEvents(WeaponListener(), this)
        manager.registerEvents(HologramListener(), this)
        manager.registerEvents(EquipmentStoringListener(), this)
        manager.registerEvents(SkillListener(), this)

        manager.registerEvents(ElevatorListener(), this)
        manager.registerEvents(MiningListener(), this)
        manager.registerEvents(CMobListener(), this)
    }


    fun getSpawnPoint(): Location {
        config.getLocation("initial_spawn_point")?.let {
            return it
        }
        val world = Bukkit.getWorld("world")
        if (world == null) {
            logger.log(Level.SEVERE, "World is not loaded!")
            return Bukkit.getWorlds()[0].spawnLocation
        }
        return world.spawnLocation
    }

    fun changeSpawnPoint(location: Location) {
        config.set("initial_spawn_point", location)
        saveConfig()
    }

    var initialSpawnPoint: Location
        get() {
            return getSpawnPoint()
        }
        set(location) {
            changeSpawnPoint(location)
        }
}