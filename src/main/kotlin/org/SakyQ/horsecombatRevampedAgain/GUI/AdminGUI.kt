package org.SakyQ.horsecombatRevampedAgain.gui

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class AdminGUI(private val plugin: HorsecombatRevampedAgain) : Listener {

    private val guiTitle = "${ChatColor.DARK_AQUA}HorseCombat Admin Panel"
    private val activeGuis = HashSet<Player>()

    // Open the admin GUI for a player
    fun openAdminGui(player: Player) {
        if (!player.hasPermission("horsecombat.admin.gui")) {
            player.sendMessage("${ChatColor.RED}You don't have permission to access the admin GUI.")
            return
        }

        val inventory = Bukkit.createInventory(null, 27, guiTitle)

        // Fill with glass panes for decoration
        val backgroundItem = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        val backgroundMeta = backgroundItem.itemMeta
        backgroundMeta?.setDisplayName(" ")
        backgroundItem.itemMeta = backgroundMeta

        for (i in 0 until inventory.size) {
            inventory.setItem(i, backgroundItem)
        }

        // Towny Integration Status Item
        val townyEnabled = plugin.shouldRespectTowny()
        val townyItem = ItemStack(if (townyEnabled) Material.EMERALD_BLOCK else Material.REDSTONE_BLOCK)
        val townyMeta = townyItem.itemMeta
        townyMeta?.setDisplayName("${ChatColor.GOLD}Towny Integration")

        val townyLore = ArrayList<String>()
        townyLore.add("${ChatColor.GRAY}Status: ${if (townyEnabled) "${ChatColor.GREEN}Enabled" else "${ChatColor.RED}Disabled"}")
        if (townyEnabled && player.hasPermission("horsecombat.admin.townybypass")) {
            val townyBypassCommand = plugin.commandManager.getTownyBypassCommand()
            val bypassStatus = if (townyBypassCommand?.hasBypass(player) == true)
                "${ChatColor.GREEN}Enabled" else "${ChatColor.RED}Disabled"
            townyLore.add("${ChatColor.GRAY}Your Bypass: $bypassStatus")
            townyLore.add("${ChatColor.YELLOW}Click to toggle bypass")
        }
        townyMeta?.lore = townyLore
        townyItem.itemMeta = townyMeta
        inventory.setItem(10, townyItem)

        // Plugin Settings Item
        val settingsItem = ItemStack(Material.COMPARATOR)
        val settingsMeta = settingsItem.itemMeta
        settingsMeta?.setDisplayName("${ChatColor.GOLD}Plugin Settings")
        val settingsLore = ArrayList<String>()
        settingsLore.add("${ChatColor.GRAY}Click to view and edit")
        settingsLore.add("${ChatColor.GRAY}plugin configuration")
        settingsMeta?.lore = settingsLore
        settingsItem.itemMeta = settingsMeta
        inventory.setItem(12, settingsItem)

        // Reload Plugin Item
        val reloadItem = ItemStack(Material.CLOCK)
        val reloadMeta = reloadItem.itemMeta
        reloadMeta?.setDisplayName("${ChatColor.GOLD}Reload Plugin")
        val reloadLore = ArrayList<String>()
        reloadLore.add("${ChatColor.GRAY}Click to reload the plugin")
        reloadLore.add("${ChatColor.GRAY}configuration from disk")
        reloadMeta?.lore = reloadLore
        reloadItem.itemMeta = reloadMeta
        inventory.setItem(14, reloadItem)

        // Player Management Item
        val playerItem = ItemStack(Material.PLAYER_HEAD)
        val playerMeta = playerItem.itemMeta
        playerMeta?.setDisplayName("${ChatColor.GOLD}Player Management")
        val playerLore = ArrayList<String>()
        playerLore.add("${ChatColor.GRAY}Manage player permissions")
        playerLore.add("${ChatColor.GRAY}and settings")
        playerMeta?.lore = playerLore
        playerItem.itemMeta = playerMeta
        inventory.setItem(16, playerItem)

        // Close Button
        val closeItem = ItemStack(Material.BARRIER)
        val closeMeta = closeItem.itemMeta
        closeMeta?.setDisplayName("${ChatColor.RED}Close")
        closeItem.itemMeta = closeMeta
        inventory.setItem(26, closeItem)

        player.openInventory(inventory)
        activeGuis.add(player)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.view.title != guiTitle || !activeGuis.contains(player)) return

        event.isCancelled = true

        if (event.currentItem == null) return

        when (event.slot) {
            10 -> { // Towny Integration
                if (plugin.shouldRespectTowny() &&
                    player.hasPermission("horsecombat.admin.townybypass")) {
                    player.performCommand("hcbypass toggle")
                    // Refresh GUI after a small delay
                    Bukkit.getScheduler().runTaskLater(plugin, Runnable { openAdminGui(player) }, 2L)
                }
            }
            12 -> { // Plugin Settings
                // Close this GUI and open settings GUI
                activeGuis.remove(player)
                player.closeInventory()
                player.sendMessage("${ChatColor.YELLOW}Settings GUI will be implemented in a future update.")
            }
            14 -> { // Reload Plugin
                if (player.hasPermission("horsecombat.admin.reload")) {
                    player.closeInventory()
                    activeGuis.remove(player)
                    plugin.reloadPlugin()
                    player.sendMessage("${ChatColor.GREEN}HorseCombat plugin has been reloaded!")
                } else {
                    player.sendMessage("${ChatColor.RED}You don't have permission to reload the plugin.")
                }
            }
            16 -> { // Player Management
                player.sendMessage("${ChatColor.YELLOW}Player management will be implemented in a future update.")
            }
            26 -> { // Close Button
                activeGuis.remove(player)
                player.closeInventory()
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        if (event.view.title == guiTitle) {
            activeGuis.remove(player)
        }
    }
}