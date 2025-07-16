package org.SakyQ.horsecombatRevampedAgain.managers

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.SmithingRecipe
import org.bukkit.inventory.SmithingTransformRecipe

class RecipeManager(private val plugin: HorsecombatRevampedAgain) {

    fun registerRecipes() {
        registerLanceRecipe()
        registerFancyLanceRecipe()

        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Registered crafting recipes for lances")
        }
    }

    private fun registerLanceRecipe() {
        // Create the lance item
        val lance = createLance()

        // Create recipe key
        val key = NamespacedKey(plugin, "lance_of_momentum")

        // Create the shaped recipe
        val recipe = ShapedRecipe(key, lance)

        // Define the shape
        recipe.shape("  I", " S ", "S  ")

        // Define the ingredients
        recipe.setIngredient('I', Material.IRON_INGOT)
        recipe.setIngredient('S', Material.STICK)

        // Register the recipe
        try {
            Bukkit.addRecipe(recipe)

            // Add alternative recipe for left-handed crafting
            val leftKey = NamespacedKey(plugin, "lance_of_momentum_left")
            val leftRecipe = ShapedRecipe(leftKey, lance)
            leftRecipe.shape("I  ", " S ", "  S")
            leftRecipe.setIngredient('I', Material.IRON_INGOT)
            leftRecipe.setIngredient('S', Material.STICK)
            Bukkit.addRecipe(leftRecipe)

            plugin.logger.info("Registered crafting recipe for Lance of Momentum")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to register lance recipe: ${e.message}")
        }
    }

    private fun registerFancyLanceRecipe() {
        // Create a fancy variant of the lance
        val fancyLance = createFancyLance()

        // Create recipe key
        val key = NamespacedKey(plugin, "fancy_lance_of_momentum")

        // For 1.16+ servers, use SmithingRecipe
        if (isModernMinecraft()) {
            try {
                // Create base ingredient
                val baseItem = RecipeChoice.MaterialChoice(Material.STICK)

                // Create addition ingredient
                val additionItem = RecipeChoice.MaterialChoice(Material.GOLD_INGOT, Material.DIAMOND)

                // Create template item (for 1.20+)
                val templateItem = if (supportsSmithingTemplates()) {
                    RecipeChoice.MaterialChoice(Material.IRON_INGOT)
                } else {
                    null
                }

                // Create the recipe based on Minecraft version
                if (supportsSmithingTemplates()) {
                    // For 1.20+
                    val smithingRecipe = SmithingTransformRecipe(
                        key,
                        fancyLance,
                        templateItem!!,
                        baseItem,
                        additionItem
                    )
                    Bukkit.addRecipe(smithingRecipe)
                } else {
                    // For 1.16-1.19
                    try {
                        val legacyRecipeClass = Class.forName("org.bukkit.inventory.SmithingRecipe")
                        val constructor = legacyRecipeClass.getConstructor(
                            NamespacedKey::class.java,
                            ItemStack::class.java,
                            RecipeChoice::class.java,
                            RecipeChoice::class.java
                        )
                        val legacyRecipe = constructor.newInstance(key, fancyLance, baseItem, additionItem)
                        Bukkit.addRecipe(legacyRecipe as org.bukkit.inventory.Recipe)
                    } catch (e: Exception) {
                        plugin.logger.warning("Failed to create legacy smithing recipe: ${e.message}")
                        createFallbackFancyLanceRecipe()
                    }
                }

                plugin.logger.info("Registered smithing recipe for Fancy Lance of Momentum")
            } catch (e: Exception) {
                plugin.logger.warning("Failed to register fancy lance smithing recipe: ${e.message}")
                createFallbackFancyLanceRecipe()
            }
        } else {
            createFallbackFancyLanceRecipe()
        }
    }

    private fun createFallbackFancyLanceRecipe() {
        // Create a fallback crafting recipe if smithing doesn't work
        val fancyLance = createFancyLance()
        val key = NamespacedKey(plugin, "fancy_lance_crafting")

        // Create shaped recipe
        val recipe = ShapedRecipe(key, fancyLance)
        recipe.shape("GDG", "DSD", "GDG")
        recipe.setIngredient('G', Material.GOLD_INGOT)
        recipe.setIngredient('D', Material.DIAMOND)
        recipe.setIngredient('S', Material.STICK)

        try {
            Bukkit.addRecipe(recipe)
            plugin.logger.info("Registered fallback crafting recipe for Fancy Lance of Momentum")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to register fallback fancy lance recipe: ${e.message}")
        }
    }

    private fun createLance(): ItemStack {
        val lance = ItemStack(Material.STICK)
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

    private fun createFancyLance(): ItemStack {
        val lance = ItemStack(Material.STICK)
        val meta = lance.itemMeta ?: return lance

        val name = plugin.config.getString("lance.fancyDisplayName") ?: "§e§lFancy Lance of Momentum"
        meta.setDisplayName(name)

        val loreDefault = listOf(
            "§6A beautifully crafted lance with gold and diamond accents",
            "§7Use this lance to knock players off their horses!",
            "§7Deals slightly more damage than a regular lance."
        )
        val lore = plugin.config.getStringList("lance.fancyLore")
        meta.lore = if (lore.isEmpty()) loreDefault else lore

        val customModelData = plugin.config.getInt("lance.fancyCustomModelData", 12346)
        meta.setCustomModelData(customModelData)

        // Add additional item flags if available
        try {
            val hideAttributesMethod = meta.javaClass.getMethod("addItemFlags", Array<org.bukkit.inventory.ItemFlag>::class.java)
            val itemFlagClass = Class.forName("org.bukkit.inventory.ItemFlag")
            val hideAttributesValue = itemFlagClass.getMethod("valueOf", String::class.java).invoke(null, "HIDE_ATTRIBUTES")
            hideAttributesMethod.invoke(meta, arrayOf(hideAttributesValue))
        } catch (e: Exception) {
            // Ignore if not supported
        }

        lance.itemMeta = meta

        return lance
    }

    // Helper method to check if we're on modern Minecraft (1.16+)
    private fun isModernMinecraft(): Boolean {
        try {
            // Check if SmithingRecipe class exists
            Class.forName("org.bukkit.inventory.SmithingRecipe")
            return true
        } catch (e: ClassNotFoundException) {
            return false
        }
    }

    // Helper method to check if we support smithing templates (1.20+)
    private fun supportsSmithingTemplates(): Boolean {
        try {
            // Check if SmithingTransformRecipe class exists
            Class.forName("org.bukkit.inventory.SmithingTransformRecipe")
            return true
        } catch (e: ClassNotFoundException) {
            return false
        }
    }
}