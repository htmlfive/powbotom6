# 0m6 PowBot Scripts Collection

A collection of automation scripts for Old School RuneScape using the PowBot API.

---

## Table of Contents

1. [Salvage Sorter](#salvage-sorter)
2. [Deranged Archaeologist](#deranged-archaeologist)
3. [Pest Control](#pest-control)
4. [Stall Thiever](#stall-thiever)
5. [Moonlight Moth Catcher](#moonlight-moth-catcher)
6. [Varrock Museum](#varrock-museum)
7. [WebWalker](#webwalker)

---

## Salvage Sorter

**Version:** 1.3.0  
**Category:** Other  
**Description:** Automates salvage sorting, loot cleanup, cargo withdrawal, and crystal extractor taps for the Sailing skill's shipwreck salvaging activity.

### Features
- Automatic salvage sorting at the sorting station
- Cargo hold deposit and withdrawal management
- Crystal extractor tapping on timer
- High Alchemy support for valuable drops
- Power Salvage Mode for lower-level players without sorting station access
- Configurable camera directions for tap locations

### Configuration Options
| Option | Description | Default |
|--------|-------------|---------|
| Power Salvage Mode | Skips sorting, drops salvage when full | `false` |
| Enable Extractor | Auto-tap crystal extractor every ~64s | `true` |
| Salvage Item Name | Target salvage type | `Opulent salvage` |
| Start Sorting | Begin in sorting mode | `false` |
| Tap-to-drop | Enable tap-to-drop for items | `true` |
| Max Cargo Space | Maximum cargo capacity | `160` |
| Extractor Tap Direction | Camera direction for extractor | `North` |
| Drop Salvage Direction | Camera direction for sorting | `North` |

### Supported Salvage Types
- Small salvage, Fishy salvage, Barracuda salvage
- Large salvage, Plundered salvage, Martial salvage
- Fremennik salvage, Opulent salvage

---

## Deranged Archaeologist

**Version:** 2.4.2  
**Category:** Combat  
**Description:** Automated boss killer for the Deranged Archaeologist using magic. Handles banking at Ferox Enclave, equipment management, and emergency teleports.

### Features
- Full gear and inventory management
- Automatic prayer flicking
- Special attack dodging
- Intelligent looting with price-based filtering
- Emergency teleport system
- Pool of Refreshment support at Ferox Enclave
- Poison management

### Requirements
- Ring of Dueling in inventory to start
- Digsite Pendant for travel
- Emergency teleport item configured

### Configuration Options
| Option | Description | Default |
|--------|-------------|---------|
| Required Equipment | Gear setup via equipment selector | Pre-configured |
| Required Inventory | Full inventory setup | Pre-configured |
| Food Name | Food item to eat | `Shark` |
| Always Loot | Items to always pick up | `Shark,Numulite` |
| Minimum Loot Value | GP threshold for looting | `1000` |
| Eat At % | Health percentage to eat | `65` |
| Emergency Teleport HP % | HP % to emergency teleport | `25` |
| Emergency Teleport Item | Teleport item to use | `Ardougne cloak` |

### Supported Emergency Teleports
- Teleport to house (tablet)
- Ectophial
- Ardougne cloak

---

## Pest Control

**Version:** 1.0.0  
**Category:** Minigame  
**Description:** Plays the Pest Control minigame automatically. Supports all boat tiers and multiple activity modes.

### Features
- Automatic boat boarding and game participation
- Portal attacking with intelligent targeting
- Knight defense mode
- Mix mode for alternating activities
- Prayer support (overhead and offensive)
- Success rate tracking
- Zeal percentage monitoring

### Requirements
- Start geared up and ready to play
- Be at the Pest Control island

### Configuration Options
| Option | Description | Default |
|--------|-------------|---------|
| Boat Type | Difficulty tier | `Hard` |
| Activity | Combat role | `Attack Portal` |
| Overhead Prayer | Defensive prayer | `None` |
| Offensive Prayer | Damage prayer | `None` |

### Activity Modes
- **Defend Knight:** Protects the Void Knight from pests
- **Attack Portal:** Focuses on destroying portals
- **Mix:** Alternates between both activities randomly

### Prayer Options
- **Overhead:** None, Protect from Magic/Melee/Missiles, Redemption
- **Offensive:** None, Eagle Eye, Mystic Might, Rigour

---

## Stall Thiever

**Version:** (See Constants)  
**Category:** Thieving  
**Description:** Automated stall thieving with banking support, world hopping, and item management.

### Features
- Configurable stall targeting
- Automatic banking when inventory is full
- World hopping when players are nearby
- Item dropping for unwanted loot
- Camera pitch management

### Configuration Options
| Option | Description | Default |
|--------|-------------|---------|
| Stall Target | Target stall selection | Configurable |
| Enable Hopping | Hop when players nearby | `true` |
| Target Items | Items to bank | Configurable |
| Drop Items | Items to always drop | Configurable |
| Thieving Tile | Location to stand | Configurable |
| Bank Tile | Banking location | Configurable |

---

## Moonlight Moth Catcher

**Version:** 1.0.7  
**Category:** Hunter  
**Description:** Catches Moonlight Moths in the Cam Torum area with automatic banking and world hopping.

### Features
- Automatic moth catching
- Banking for butterfly jars
- World hopping when players detected
- Stair navigation (up/down)
- Running energy management

### Requirements
- Butterfly jars in bank
- Start near the moth catching area

### Behavior
- Automatically navigates to moth location
- Avoids moths below Y-axis 9437
- Hops to random members world when players nearby
- Banks caught moths and withdraws new jars

---

## Varrock Museum

**Version:** 1.0.0  
**Category:** Minigame  
**Description:** Automates the Varrock Museum specimen cleaning activity for Hunter and Slayer XP.

### Features
- Automatic specimen collection from rocks
- Find cleaning at specimen table
- Storage crate depositing
- Antique lamp usage with configurable skill
- Unwanted item dropping

### Requirements
- **Inventory:** Specimen Brush, Rock Pick, Trowel
- **Equipment:** Leather boots, Leather gloves
- **Location:** Start in the museum dig site area
- **Dialogue:** Set deposit dialogue to "Don't ask again"

### Configuration Options
| Option | Description | Default |
|--------|-------------|---------|
| Spam Click Take | Fast specimen collection | `false` |
| Lamp Skill | Skill for XP lamps | Configurable |
| Drop List | Items to drop | `Bowl,Pot` |

### Supported Lamp Skills
Attack, Strength, Ranged, Magic, Defense, Hitpoints, Prayer, Agility, Herblore, Thieving, Crafting, Runecrafting, Slayer, Farming, Mining, Smithing, Fishing, Cooking, Firemaking, Woodcutting, Fletching, Construction, Hunter

---

## WebWalker

**Version:** (See Constants)  
**Category:** Other  
**Description:** Utility script that walks to any specified location using the PowBot web walker.

### Features
- Predefined location presets
- Custom coordinate input support
- Automatic walkable tile detection
- Distance tracking
- Real-time position display

### Configuration Options
| Option | Description | Default |
|--------|-------------|---------|
| Target Location | Predefined or custom | Configurable |
| Custom Coordinates | X,Y,Z format | `3208,3220,2` |

### Predefined Locations
- **Banks:** Al Kharid, Ardougne South, Catherby, Draynor, Edgeville, Falador East/West, Gnome Stronghold, Grand Exchange, Lumbridge, Motherlode Mine, Pollnivneach, Seers' Village, Shilo Village, TzHaar, Varlamore City, Varrock East/West, Yanille, Zeah Arceuus
- **Other:** Fremennik Docks, Custom coordinates

---

## Project Structure

```
org.powbot.om6/
├── derangedarch/           # Deranged Archaeologist script
│   ├── tasks/              # Combat and utility tasks
│   └── utils/              # Price cache and utilities
├── moths/                  # Moonlight Moth Catcher
├── pestcontrol/            # Pest Control minigame
│   ├── data/               # Game data (portals, boats, etc.)
│   ├── helpers/            # NPC and movement helpers
│   └── task/               # Game tasks
├── salvagesorter/          # Sailing Salvage Sorter
│   ├── config/             # Constants and configuration
│   └── tasks/              # Sorting and salvaging tasks
├── stalls/                 # Stall Thiever
│   └── tasks/              # Thieving tasks
├── varrockmuseum/          # Varrock Museum cleaner
└── webwalker/              # WebWalker utility
```

---

## Author

**0m6**

---

## Notes

- All scripts use a task-based architecture for modularity
- Configuration is handled via PowBot's ScriptConfiguration system
- Paint overlays provide real-time status information
- Most scripts support break handling via `canBreak()` override
