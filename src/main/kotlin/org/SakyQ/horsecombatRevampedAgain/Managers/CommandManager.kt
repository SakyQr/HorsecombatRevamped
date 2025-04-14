package org.SakyQ.horsecombatRevampedAgain.managers

import org.SakyQ.horsecombatRevampedAgain.Commands.GiveLanceCommand
import org.SakyQ.horsecombatRevampedAgain.Commands.GiveLanceTabCompleter
import org.SakyQ.horsecombatRevampedAgain.Commands.HorseCombatTabCompleter
import org.SakyQ.horsecombatRevampedAgain.Commands.TownyBypassCommand
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
        // Register existing commands
        plugin.getCommand("givelance")?.setExecutor(giveLanceCommand)
        plugin.getCommand("givelance")?.tabCompleter = giveLanceTabCompleter

        // Make sure HorseCombat tab completer works
        plugin.getCommand("horsecombat")?.tabCompleter = horseCombatTabCompleter

        // Register new commands
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

        // Log command registration for debug
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("HorseCombat commands registered!")
            plugin.logger.info("Tab completers registered: givelance, horsecombat, hcbypass")
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

            "checkregion" -> {
                if (sender !is Player) {
                    sender.sendMessage("${ChatColor.RED}This command can only be used by players.")
                    return true
                }

                val horseSpawnListener = plugin.listenerManager.getHorseSpawnListener()
                val region = horseSpawnListener.findMatchingRegion(sender.location)

                if (region != null) {
                    sender.sendMessage("${ChatColor.GREEN}You are in the ${region.name} horse region!")
                    sender.sendMessage("${ChatColor.GRAY}Horse Type: ${ChatColor.YELLOW}${region.style?.name ?: "None"}")
                    sender.sendMessage("${ChatColor.GRAY}Horse Color: ${ChatColor.YELLOW}${region.color?.name ?: "None"}")
                } else {
                    sender.sendMessage("${ChatColor.RED}You are not in any horse region.")
                }
                return true
            }


            "help" -> {
                sender.sendMessage("${ChatColor.GOLD}HorseCombat Commands:")
                sender.sendMessage("${ChatColor.GRAY}/horsecombat reload ${ChatColor.WHITE}- Reload the plugin configuration")
                sender.sendMessage("${ChatColor.GRAY}/horsecombat debug ${ChatColor.WHITE}- Toggle debug mode")
                sender.sendMessage("${ChatColor.GRAY}/horsecombat checkregion ${ChatColor.WHITE}- Check which horse region you're in")
                sender.sendMessage("${ChatColor.GRAY}/horsecombat listregs ${ChatColor.WHITE}- List all horse regions")
                sender.sendMessage("${ChatColor.GRAY}/horseegg <type> ${ChatColor.WHITE}- Get a horse spawn egg")
                sender.sendMessage("${ChatColor.GRAY}/givelance [player] [type] ${ChatColor.WHITE}- Give a lance to a player")
                sender.sendMessage("${ChatColor.GRAY}/hcbypass [toggle|on|off|status] ${ChatColor.WHITE}- Toggle Towny bypass")
                sender.sendMessage("${ChatColor.GRAY}/hcadmin ${ChatColor.WHITE}- Open the admin GUI")
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