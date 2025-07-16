package org.SakyQ.horsecombatRevampedAgain.managers

import org.SakyQ.horsecombatRevampedAgain.Commands.*
import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.SakyQ.horsecombatRevampedAgain.gui.AdminGUI
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandManager(private val plugin: HorsecombatRevampedAgain) {

    private val giveLanceCommand = GiveLanceCommand(plugin)
    private val giveLanceTabCompleter = GiveLanceTabCompleter()
    private val horseCombatTabCompleter = HorseCombatTabCompleter()
    private val townyBypassCommand = TownyBypassCommand(plugin)
    private val adminGUI = AdminGUI(plugin)

    fun registerCommands() {
        // Register lance commands
        plugin.getCommand("givelance")?.setExecutor(giveLanceCommand)
        plugin.getCommand("givelance")?.tabCompleter = giveLanceTabCompleter

        // Register main command tab completer
        plugin.getCommand("horsecombat")?.tabCompleter = horseCombatTabCompleter

        // Register Towny bypass commands
        plugin.getCommand("hcbypass")?.setExecutor(townyBypassCommand)
        plugin.getCommand("hcbypass")?.tabCompleter = townyBypassCommand

        // Register admin GUI command
        plugin.getCommand("hcadmin")?.setExecutor { sender, _, _, _ ->
            if (sender !is Player) {
                sender.sendMessage("${ChatColor.RED}This command can only be used by players.")
                return@setExecutor true
            }

            if (!sender.hasPermission("horsecombat.admin.gui")) {
                sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                return@setExecutor true
            }

            adminGUI.openAdminGui(sender)
            true
        }

        // Register event listener for GUI
        plugin.server.pluginManager.registerEvents(adminGUI, plugin)

        if (plugin.isDebugEnabled()) {
            plugin.logger.info("HorseCombat commands registered!")
            plugin.logger.info("Registered commands: givelance, horsecombat, hcbypass, hcadmin")
        }
    }

    fun handleHorseCombatCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.GOLD}HorseCombat ${ChatColor.GRAY}- ${ChatColor.WHITE}Version ${plugin.description.version}")
            sender.sendMessage("${ChatColor.GRAY}Use /horsecombat help for a list of commands")
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission("horsecombat.admin.reload")) {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to reload the plugin.")
                    return true
                }
                plugin.reloadPlugin()
                sender.sendMessage("${ChatColor.GREEN}HorseCombat configuration reloaded!")
                return true
            }

            "debug" -> {
                if (!sender.hasPermission("horsecombat.debug")) {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to toggle debug mode.")
                    return true
                }

                plugin.toggleDebugMode()
                val status = if (plugin.isDebugEnabled()) "enabled" else "disabled"
                sender.sendMessage("${ChatColor.GREEN}Debug mode is now $status!")
                return true
            }

            "help" -> {
                sender.sendMessage("${ChatColor.GOLD}HorseCombat Commands:")
                sender.sendMessage("${ChatColor.GRAY}/horsecombat reload ${ChatColor.WHITE}- Reload the plugin configuration")
                sender.sendMessage("${ChatColor.GRAY}/horsecombat debug ${ChatColor.WHITE}- Toggle debug mode")
                sender.sendMessage("${ChatColor.GRAY}/givelance [player] ${ChatColor.WHITE}- Give a lance to a player")

                if (sender.hasPermission("horsecombat.admin.townybypass")) {
                    sender.sendMessage("${ChatColor.GRAY}/hcbypass [toggle|on|off|status] ${ChatColor.WHITE}- Toggle Towny bypass")
                }

                if (sender.hasPermission("horsecombat.admin.gui")) {
                    sender.sendMessage("${ChatColor.GRAY}/hcadmin ${ChatColor.WHITE}- Open the admin GUI")
                }
                return true
            }

            "gui", "admin" -> {
                if (sender !is Player) {
                    sender.sendMessage("${ChatColor.RED}This command can only be used by players.")
                    return true
                }

                if (!sender.hasPermission("horsecombat.admin.gui")) {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                    return true
                }

                adminGUI.openAdminGui(sender as Player)
                return true
            }

            else -> {
                sender.sendMessage("${ChatColor.RED}Unknown command. Use /horsecombat help for a list of commands.")
                return true
            }
        }
    }

    // Getter for townyBypassCommand so it can be accessed by the GUI
    fun getTownyBypassCommand(): TownyBypassCommand {
        return townyBypassCommand
    }
}