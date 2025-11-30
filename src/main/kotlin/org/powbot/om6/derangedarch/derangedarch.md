# Deranged Archaeologist Magic Killer

**Version:** 2.4.2  
**Author:** 0m6  
**Category:** Combat  
**Status:** BETA

## Overview

Automated script for killing the Deranged Archaeologist boss in Old School RuneScape using magic combat. Features intelligent task-based execution, emergency safety systems, and smart looting with price-based filtering.

## Prerequisites

### Required Items
- **Ring of Dueling** (any charges) - Must be in inventory to start
- **Digsite Pendant** (any charges)
- **Axe** (any type)
- **Food** (e.g., Sharks)
- **Prayer Potions**
- **Emergency Teleport** (Ardougne cloak, Ectophial, or Teleport to house)
- **Runes** (for combat spells)

### Recommended Stats
- Magic: 75+ recommended
- Prayer: 43+ (Protect from Missiles)
- Hitpoints: 70+

## Configuration

### Equipment Setup
Define your combat gear via the Equipment configuration:
```
Default: Mystic hat, God cape, Amulet of glory, Mystic robe top, 
         Mystic robe bottom, Rune boots, Ring of dueling, 
         Staff of the dead
```

### Inventory Setup
Configure your full inventory including:
- Axe (for chopping trunk obstacle)
- Food (configurable type and amount)
- Prayer potions
- Ring of dueling
- Digsite pendant
- Runes for combat
- Emergency teleport item

### Script Settings

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| **Food Name** | String | "Shark" | Food item to eat |
| **Always Loot** | String | "Shark,Numulite" | Items to loot regardless of value (comma-separated) |
| **Minimum Loot Value** | Integer | 1000 | Don't loot items worth less than this (GP) |
| **Eat At %** | Integer | 65 | Health percentage to eat at |
| **Emergency Teleport HP %** | Integer | 25 | HP threshold for emergency escape |
| **Emergency Teleport Item** | Selection | "Ardougne cloak" | Options: Teleport to house, Ectophial, Ardougne cloak |

## Features

### Core Systems

#### Task-Based Execution
The script uses a priority-based task system that executes tasks in order:

1. **EmergencyTeleportTask** - Instant escape at low HP
2. **DeactivatePrayerTask** - Conserve prayer when safe
3. **GoToBankTask** - Return to bank when needed
4. **EquipItemsTask** - Maintain proper gear
5. **BankTask** - Restock supplies
6. **DrinkFromPoolTask** - Restore stats at Ferox Enclave
7. **TravelToBossTask** - Navigate to boss location
8. **DodgeSpecialTask** - Avoid special attacks
9. **PrayerTask** - Manage prayer points and protection
10. **PoisonTask** - Handle poison status
11. **EatTask** - Maintain health
12. **FightTask** - Attack the boss
13. **FixPitchTask** - Adjust camera angle
14. **LootTask** - Collect valuable drops
15. **RepositionTask** - Maintain optimal positioning

#### Smart Looting System
- **Price Cache:** Pre-loads item values for instant loot decisions
- **Whitelist:** Always loot configured items (e.g., food, Numulite)
- **Value Threshold:** Ignores items below minimum value
- **Inventory Management:** Automatically drops low-value items when full

#### Combat Management
- **Auto-Prayer:** Activates Protect from Missiles during combat
- **Prayer Conservation:** Deactivates prayer when not fighting
- **Health Monitoring:** Eats food at configured HP threshold
- **Poison Handling:** Drinks antipoison when poisoned

#### Special Attack Avoidance
Intelligently dodges the boss's book throw special attack:
- Detects projectile (ID: 1260)
- Identifies attack via chat message: "Learn to Read!"
- Calculates safe dodge positions
- Moves perpendicular to projectile trajectory
- Maintains minimum safe distance

#### Safety Features
- **Emergency Teleport:** Automatic escape at critical HP
- **Banking System:** Returns to Ferox Enclave for supplies
- **Stat Restoration:** Uses Pool of Refreshment for full restore
- **Equipment Validation:** Ensures proper gear before combat
- **Inventory Verification:** Checks required items before trips

## Travel Route

### To Boss Location
1. Use Digsite Pendant → Fossil Island
2. Use Magic Mushtree → Verdant Valley
3. Walk to and chop Thick Vine
4. Walk to and chop Decaying Trunk
5. Arrive at boss spawn area

### Return to Bank
1. Use Ring of Dueling → Ferox Enclave
2. Bank at Ferox Enclave bank chest
3. Drink from Pool of Refreshment (if needed)

## Key Locations

| Location | Tile | Purpose |
|----------|------|---------|
| Boss Trigger | (3683, 3707, 0) | Boss spawn area |
| Ferox Bank | (3135, 3631, 0) | Banking location |
| Ferox Pool | (3128-3130, 3634-3637) | Stat restoration |
| Reposition Tile | (3688, 3705, 0) | Optimal combat position |

### Dodge Tiles
Pre-calculated safe positions for special attack:
- (3683, 3703, 0)
- (3687, 3706, 0)
- (3683, 3710, 0)
- (3678, 3706, 0)

## Constants & Thresholds

### Combat
- **Boss NPC ID:** 7806
- **Protected Prayer:** Protect from Missiles
- **Low Prayer:** 30 points
- **Critical Prayer:** 10 points
- **Min Run Energy:** 40%

### Camera
- **Target Pitch:** 92°
- **Valid Range:** 85-99°

### Distances
- **Fight Area:** 8 tiles
- **Boss Close Distance:** 2 tiles
- **Dodge Distance:** 5+ tiles

## Paint Display

The overlay shows:
- **Current Task:** Active task being executed
- **Magic XP:** Real-time XP tracking with XP/hr
- **Loot GP:** Total gold collected
- **GP/hr:** Profit rate calculation

## Item Management

### Ring of Dueling
- IDs: 2552-2566 (even numbers)
- Keeps rings with charges (2552-2564)
- Destroys fully depleted rings (2566)

### Digsite Pendant
- IDs: 11190-11194
- Keeps pendants with charges (11191-11194)
- Destroys depleted pendants (11190)

### Prayer Potions
- Recognizes all doses (1-4)
- Auto-drinks when prayer is low
- Keeps empty vials (ID: 229) for banking

## Break System

The script can safely break when:
- Player is near Ferox Enclave bank (< 10 tiles)
- Stats are fully restored (HP and Prayer)

## Troubleshooting

### Common Issues

**Script won't start:**
- Ensure Ring of Dueling is in inventory
- Verify all required items are configured
- Check that equipment setup is valid

**Not attacking boss:**
- Verify prayer points available
- Check if repositioning to combat area
- Ensure boss is spawned (walk near trigger tile)

**Constantly banking:**
- Check food name matches inventory
- Verify prayer potion recognition
- Ensure minimum quantities are met

**Not dodging special attack:**
- Script detects via projectile and chat
- Ensure camera pitch is in valid range (85-99°)
- Check player is within dodge area

**Emergency teleport not working:**
- Verify teleport item is in inventory
- Check item name matches configuration
- Ensure teleport has charges/uses

## Performance Tips

1. **Maximize GP/hr:**
   - Increase loot value threshold
   - Minimize always-loot list
   - Use higher-tier food for longer trips

2. **Improve Survivability:**
   - Lower eat threshold (e.g., 70-75%)
   - Higher emergency teleport threshold (e.g., 30-35%)
   - Bring more food, fewer prayer potions

3. **Extend Trips:**
   - Use better prayer bonus gear
   - Bring more prayer potions
   - Consider using prayer-restoring items

## File Structure

```
derangedarch/
├── DerangedArchaeologistMagicKiller.kt  # Main script class
├── Constants.kt                          # Configuration constants
├── start.kt                              # Script entry point
├── tasks/
│   ├── Task.kt                          # Base task interface
│   ├── BankTask.kt                      # Banking logic
│   ├── DeactivatePrayer.kt              # Prayer management
│   ├── DodgeSpecialTask.kt              # Special attack avoidance
│   ├── DrinkFromPoolTask.kt             # Stat restoration
│   ├── EatTask.kt                       # Food consumption
│   ├── EmergencyTeleportTask.kt         # Emergency escape
│   ├── EquipItemsTask.kt                # Gear management
│   ├── FightTask.kt                     # Combat execution
│   ├── FixPitchTask.kt                  # Camera adjustment
│   ├── GoToBankTask.kt                  # Bank navigation
│   ├── LootTask.kt                      # Item collection
│   ├── PoisonTask.kt                    # Poison handling
│   ├── PrayerTask.kt                    # Prayer activation
│   ├── RepositionTask.kt                # Position optimization
│   └── TravelToBossTask.kt              # Boss area navigation
└── utils/
    ├── LootPriceCache.kt                # Item price database
    └── ScriptUtils.kt                   # Utility functions
```

## Version History

**v2.4.2** (Current)
- BETA release
- Core task system implementation
- Smart loot filtering
- Emergency safety systems
- Special attack dodge mechanics

---

**Note:** This script is in BETA. Report issues or suggestions to the author (0m6).
