package org.SakyQ.horsecombatRevampedAgain.Commands

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType
import org.bukkit.entity.Horse

class HorseEggCommand(private val plugin: HorsecombatRevampedAgain) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Only players can use this command
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}This command can only be used by players.")
            return true
        }

        // Check permission
        if (!sender.hasPermission("horsecombat.horseegg")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return true
        }

        // Need at least one argument (horse type)
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}Usage: /horseegg <type>")
            sender.sendMessage("${ChatColor.GRAY}Available types: ${getHorseTypeList()}")
            return true
        }

        val horseType = args[0].lowercase()

        // Check if inventory is full
        if (sender.inventory.firstEmpty() == -1) {
            sender.sendMessage("${ChatColor.RED}Your inventory is full!")
            return true
        }

        // Give the horse egg
        val success = giveHorseEgg(sender, horseType)

        if (success) {
            sender.sendMessage("${ChatColor.GREEN}You received a ${formatHorseType(horseType)} Horse Spawn Egg!")
        } else {
            sender.sendMessage("${ChatColor.RED}Invalid horse type: $horseType")
            sender.sendMessage("${ChatColor.GRAY}Available types: ${getHorseTypeList()}")
        }

        return true
    }

    private fun giveHorseEgg(player: Player, horseType: String): Boolean {
        // Get horse color and style from type
        val (color, style) = parseHorseType(horseType) ?: return false

        // Create spawn egg
        val egg = ItemStack(Material.HORSE_SPAWN_EGG)
        val meta = egg.itemMeta ?: return false

        // Set name and lore
        meta.setDisplayName("${ChatColor.GOLD}${formatHorseType(horseType)} Horse")

        val lore = mutableListOf<String>()
        lore.add("${ChatColor.GRAY}Right-click to spawn a custom horse")
        lore.add("${ChatColor.GRAY}Color: ${ChatColor.WHITE}${formatColor(color)}")
        if (style != null) {
            lore.add("${ChatColor.GRAY}Style: ${ChatColor.WHITE}${formatStyle(style)}")
        }

        meta.lore = lore

        // Store horse type in NBT
        val dataKey = "horsecombat_type"
        meta.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, dataKey),
            org.bukkit.persistence.PersistentDataType.STRING,
            horseType
        )

        egg.itemMeta = meta
        player.inventory.addItem(egg)

        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Gave horse egg of type $horseType to ${player.name}")
        }

        return true
    }

    private fun parseHorseType(type: String): Pair<Horse.Color, Horse.Style?>? {
        return when (type.lowercase()) {
            "white_horse" -> Pair(Horse.Color.WHITE, null)
            "black_horse" -> Pair(Horse.Color.BLACK, null)
            "brown_horse" -> Pair(Horse.Color.BROWN, null)
            "chestnut_horse" -> Pair(Horse.Color.CHESTNUT, null)
            "creamy_horse" -> Pair(Horse.Color.CREAMY, null)
            "dark_brown_horse" -> Pair(Horse.Color.DARK_BROWN, null)
            "gray_horse" -> Pair(Horse.Color.GRAY, null)
            "mountain_horse" -> Pair(Horse.Color.DARK_BROWN, Horse.Style.WHITE_DOTS)
            "desert_horse" -> Pair(Horse.Color.CREAMY, Horse.Style.WHITE)
            "forest_horse" -> Pair(Horse.Color.BROWN, Horse.Style.BLACK_DOTS)
            "arctic_horse" -> Pair(Horse.Color.WHITE, Horse.Style.WHITE_DOTS)
            "spotted_horse" -> Pair(Horse.Color.WHITE, Horse.Style.BLACK_DOTS)
            "black_dots_horse" -> Pair(Horse.Color.GRAY, Horse.Style.BLACK_DOTS)
            "white_dots_horse" -> Pair(Horse.Color.BLACK, Horse.Style.WHITE_DOTS)
            "white_stripe_horse" -> Pair(Horse.Color.DARK_BROWN, Horse.Style.WHITE)
            else -> null
        }
    }

    private fun getHorseTypeList(): String {
        return "white_horse, black_horse, brown_horse, chestnut_horse, creamy_horse, dark_brown_horse, gray_horse, " +
                "mountain_horse, desert_horse, forest_horse, arctic_horse, spotted_horse, black_dots_horse, white_dots_horse, " +
                "black_stripe_horse, white_stripe_horse"
    }

    private fun formatHorseType(type: String): String {
        return type.split("_")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    private fun formatColor(color: Horse.Color): String {
        return color.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")
    }

    private fun formatStyle(style: Horse.Style): String {
        return style.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")
    }
}