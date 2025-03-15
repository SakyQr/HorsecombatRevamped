package org.SakyQ.horsecombatRevampedAgain.Commands

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class GiveLanceCommand(private val plugin: HorsecombatRevampedAgain) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Check if sender has permission
        if (!sender.hasPermission("horsecombat.givelance")) {
            sender.sendMessage("§cYou do not have permission to use this command.")
            return true
        }

        // If no arguments, give lance to the sender (if they're a player)
        if (args.isEmpty()) {
            if (sender !is Player) {
                sender.sendMessage("§cUsage: /givelance <player>")
                return true
            }

            giveLance(sender, sender)
            return true
        }

        // If arguments exist, try to find the target player
        val targetName = args[0]
        val targetPlayer = Bukkit.getPlayer(targetName)

        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer not found: $targetName")
            return true
        }

        // Give lance to the target player
        giveLance(sender, targetPlayer)
        return true
    }

    private fun giveLance(sender: CommandSender, target: Player) {
        val lance = createLance()

        // Check if inventory is full
        if (target.inventory.firstEmpty() == -1) {
            sender.sendMessage("§c${target.name}'s inventory is full!")
            return
        }

        // Add item to target's inventory
        target.inventory.addItem(lance)

        // Get success message from config
        val successMessage = plugin.config.getString("messages.lanceReceived") ?: "§aYou have received a Lance of Momentum!"

        // Send message to sender and target
        if (sender != target) {
            sender.sendMessage("§aYou gave a Lance of Momentum to ${target.name}!")
        }

        target.sendMessage(successMessage)
    }

    private fun createLance(): ItemStack {
        val lance = ItemStack(Material.STICK) // Use a stick as the base item
        val meta = lance.itemMeta ?: return lance

        val name = plugin.config.getString("lance.displayName") ?: "§bLance of Momentum"
        meta.setDisplayName(name)

        val loreDefault = listOf("§7Use this lance to knock players off their horses!")
        val lore = plugin.config.getStringList("lance.lore")
        meta.lore = if (lore.isEmpty()) loreDefault else lore

        val customModelData = plugin.config.getInt("lance.customModelData", 12345)
        meta.setCustomModelData(customModelData)

        lance.itemMeta = meta

        return lance
    }
}