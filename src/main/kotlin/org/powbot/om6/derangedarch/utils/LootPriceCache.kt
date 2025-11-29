package org.powbot.om6.derangedarch.utils

import org.powbot.api.rt4.GrandExchange

/**
 * Caches item prices on script start to avoid repeated GE API calls during looting.
 */
object LootPriceCache {

    private val priceCache = mutableMapOf<Int, Int>()

    /**
     * Item IDs from Deranged Archaeologist drop table to cache.
     * Only includes items worth potentially looting.
     */
    private val ITEMS_TO_CACHE = setOf(
        // Unique
        21480,  // Steel ring

        // Weapons and armour
        2503,   // Black d'hide body
        1319,   // Rune 2h sword
        1333,   // Rune sword

        // Runes and ammunition
        4698,   // Mud rune
        868,    // Rune knife
        11212,  // Dragon arrow
        2,      // Cannonball

        // Seeds
        5295,   // Ranarr seed
        5300,   // Snapdragon seed
        5304,   // Torstol seed
        5313,   // Watermelon seed
        5313,   // Willow seed
        21480,  // Mahogany seed
        5292,   // Maple seed
        5280,   // Teak seed
        5315,   // Yew seed
        5288,   // Papaya tree seed
        5316,   // Magic seed
        5289,   // Palm tree seed
        5317,   // Spirit seed
        22877,  // Dragonfruit tree seed
        22856,  // Celastrus seed
        22859,  // Redwood tree seed

        // Materials
        261,    // Grimy dwarf weed
        239,    // White berries
        9431,   // Runite limbs
        1747,   // Black dragonhide
        444,    // Gold ore
        1617,   // Uncut diamond
        21973,  // Onyx bolt tips

        // Consumables
        2297,   // Anchovy pizza
        10925,  // Prayer potion(3)
        6705,   // Potato with cheese
        385,    // Shark

        // Other
        989,    // Crystal key
        532,    // Long bone

        // Rare drop table highlights
        1621,   // Uncut dragonstone
        1624,   // Uncut sapphire
        1622,   // Uncut emerald
        1619,   // Uncut ruby
        2363,   // Runite bar
        1319,   // Rune 2h sword (RDT)
        1373,   // Rune battleaxe
        1201,   // Rune kiteshield
        1149,   // Dragon med helm
        1247,   // Rune spear
        2366,   // Shield left half
        1237,   // Dragon spear

        // Fossils (always loot)
        13446,  // Unidentified small fossil
        13448,  // Unidentified medium fossil
        13450,  // Unidentified large fossil
        13452,  // Unidentified rare fossil

        // Numulite (always loot)
        13445   // Numulite
    )

    /**
     * Initialize the cache by fetching current GE prices.
     * Should be called once on script start.
     */
    fun initialize() {
        priceCache.clear()
        ITEMS_TO_CACHE.forEach { itemId ->
            val price = GrandExchange.getItemPrice(itemId) ?: 0
            priceCache[itemId] = price
        }
    }

    /**
     * Get cached price for an item.
     * Returns 0 if item not in cache.
     */
    fun getPrice(itemId: Int): Int {
        return priceCache[itemId] ?: 0
    }

    /**
     * Check if an item is in the cache.
     */
    fun isCached(itemId: Int): Boolean {
        return priceCache.containsKey(itemId)
    }
}