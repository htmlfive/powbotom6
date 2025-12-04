package org.powbot.om6.varrockmuseum

import org.powbot.api.rt4.walking.model.Skill

object VarrockMuseumConstants {
    // Object names
    const val DIG_SITE_SPECIMEN_ROCKS = "Dig Site specimen rocks"
    const val SPECIMEN_TABLE = "Specimen table"
    const val STORAGE_CRATE = "Storage crate"
    
    // Item names
    const val UNCLEANED_FIND = "Uncleaned find"
    const val ANTIQUE_LAMP = "Antique lamp"
    
    // Clean find categories that get added to crate
    val CLEAN_FINDS = listOf(
        "Pottery",
        "Jewellery",
        "Old chipped vase",
        "Arrowheads"
    )
    
    // Actions
    const val ACTION_TAKE = "Take"
    const val ACTION_CLEAN = "Clean"
    const val ACTION_ADD_FINDS = "Add finds"
    const val ACTION_RUB = "Rub"
    
    // Dialogue text
    const val DIALOGUE_PLACE_ALL = "Place all your finds, and don't ask this again."
    
    // Default configuration values
    const val DEFAULT_SPAM_CLICK = false
    const val DEFAULT_LAMP_SKILL = "Slayer"
    
    // Widget IDs
    const val LAMP_SKILL_WIDGET = 240
    
    // Skill name to Skill enum mapping for paint tracking
    val SKILL_NAME_TO_ENUM = mapOf(
        "Attack" to Skill.Attack,
        "Strength" to Skill.Strength,
        "Ranged" to Skill.Ranged,
        "Magic" to Skill.Magic,
        "Defense" to Skill.Defence,
        "Sailing" to Skill.Overall, //Change to Sailing when fixed
        "Hitpoints" to Skill.Hitpoints,
        "Prayer" to Skill.Prayer,
        "Agility" to Skill.Agility,
        "Herblore" to Skill.Herblore,
        "Thieving" to Skill.Thieving,
        "Crafting" to Skill.Crafting,
        "Runecrafting" to Skill.Runecrafting,
        "Slayer" to Skill.Slayer,
        "Farming" to Skill.Farming,
        "Mining" to Skill.Mining,
        "Smithing" to Skill.Smithing,
        "Fishing" to Skill.Fishing,
        "Cooking" to Skill.Cooking,
        "Firemaking" to Skill.Firemaking,
        "Woodcutting" to Skill.Woodcutting,
        "Fletching" to Skill.Fletching,
        "Construction" to Skill.Construction,
        "Hunter" to Skill.Hunter
    )
    
    // Default drop list string (for configuration)
    const val DEFAULT_DROP_LIST_STRING = "Broken arrow,Broken glass,Iron dagger,Uncut jade,Bones,Big bones,Bowl,Bronze limbs,Coal,Copper ore,Iron arrowtips,Iron bolts,Iron dart,Iron knife,Iron ore,Mithril ore,Pot,Tin ore,Uncut opal,Wooden stock"
    
    // Default drop list (configurable)
    val DEFAULT_DROP_LIST = listOf(
        "Broken arrow",
        "Broken glass",
        "Iron dagger",
        "Uncut jade",
        "Bones",
        "Big bones",
        "Bowl",
        "Bronze limbs",
        "Coal",
        "Copper ore",
        "Iron arrowtips",
        "Iron bolts",
        "Iron dagger",
        "Iron dart",
        "Iron knife",
        "Iron ore",
        "Mithril ore",
        "Pot",
        "Tin ore",
        "Uncut jade",
        "Uncut opal",
        "Wooden stock"
    )
}
