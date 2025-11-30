package org.powbot.om6.salvagesorter.config

/**
 * Constants used throughout the SalvageSorter script.
 */
object Constants {
    // ========================================
    // CAMERA SETTINGS
    // ========================================

    /** Target zoom level for the camera (0-100 scale where 100 is fully zoomed out). */
    const val TARGET_ZOOM_LEVEL: Int = 100

    // ========================================
    // MESSAGE CONSTANTS
    // ========================================

    const val CARGO_MESSAGE = "Your crewmate on the salvaging hook cannot salvage as the cargo hold is full."
    const val HARVESTER_MESSAGE = "Your crystal extractor has harvested"
    const val SALVAGE_COMPLETE_MESSAGE = "You salvage all you can"
    const val SALVAGE_SUCCESS_MESSAGE = "You cast out"
    const val HOOK_CAST_MESSAGE_1 = "You cast out your salvaging hook"
    const val HOOK_CAST_MESSAGE_2 = "You start operating"

    // ========================================
    // ITEM IDS
    // ========================================

    const val COINS_ID = 995

    // ========================================
    // WIDGET CONSTANTS
    // ========================================

    const val ROOT_CARGO_WIDGET = 943
    const val COMPONENT_CARGO_SPACE = 5
    const val COMPONENT_DEPOSIT_SALVAGE = 14
    const val COMPONENT_WITHDRAW = 10
    const val INDEX_FIRST_SLOT = 0

    const val COMPONENT_CLOSE = 1
    const val INDEX_CLOSE = 11

    const val ROOT_SAILINGTAB = 601
    const val COMPONENT_SAILINGTAB = 93

    const val ROOT_ASSIGN_WIDGET = 937
    const val COMPONENT_ASSIGN_WIDGET = 25
    const val INDEX_ASSIGN_CANNON = 31
    const val INDEX_ASSIGN_SLOT1 = 43
    const val INDEX_ASSIGN_SLOT2 = 47

    const val COMPONENT_ASSIGN_WIDGETCONFIRM = 20
    const val INDEX_ASSIGN_UNASSIGN = 0
    const val INDEX_ASSIGNCONFIRM_SLOT1 = 1
    const val INDEX_ASSIGNCONFIRM_SLOT2 = 2

    // ========================================
    // ASSIGNMENT SLEEP TIMINGS
    // ========================================

    const val ASSIGNMENT_MAIN_WAIT_MIN = 1200
    const val ASSIGNMENT_MAIN_WAIT_MAX = 1800
    const val ASSIGNMENT_INV_OPEN_MIN = 600
    const val ASSIGNMENT_INV_OPEN_MAX = 900

    // ========================================
    // WALK SLEEP TIMINGS
    // ========================================

    const val WALK_WAIT_MIN = 1800
    const val WALK_WAIT_MAX = 2400

    // ========================================
    // HOOK SLEEP TIMINGS
    // ========================================

    const val HOOK_MAIN_WAIT_MIN = 900
    const val HOOK_MAIN_WAIT_MAX = 1200
    const val HOOK_TAB_CLOSE_MIN = 600
    const val HOOK_TAB_CLOSE_MAX = 1200
    const val HOOK_TAB_OPEN_MIN = 200
    const val HOOK_TAB_OPEN_MAX = 400
    const val HOOK_WAIT_LOOP_MIN = 1000
    const val HOOK_WAIT_LOOP_MAX = 3000

    // ========================================
    // DEPOSIT SLEEP TIMINGS
    // ========================================

    const val DEPOSIT_PRE_WAIT_MIN = 700
    const val DEPOSIT_PRE_WAIT_MAX = 1100
    const val DEPOSIT_BETWEEN_TAPS_MIN = 1200
    const val DEPOSIT_BETWEEN_TAPS_MAX = 1800

    // ========================================
    // CARGO WITHDRAW SLEEP TIMINGS
    // ========================================

    const val CARGO_MAIN_WAIT_MIN = 600
    const val CARGO_MAIN_WAIT_MAX = 900
    const val CARGO_TAP1_WAIT_MIN = 1800
    const val CARGO_TAP1_WAIT_MAX = 2400
    const val CARGO_TAP4_WAIT_MIN = 1800
    const val CARGO_TAP4_WAIT_MAX = 2400

    // ========================================
    // SORT SLEEP TIMINGS
    // ========================================

    const val SORT_PRE_TAP_MIN = 500
    const val SORT_PRE_TAP_MAX = 800
    const val SORT_TAB_OPEN_MIN = 200
    const val SORT_TAB_OPEN_MAX = 400
    const val SORT_TAB_CLOSE_MIN = 600
    const val SORT_TAB_CLOSE_MAX = 1200
    const val SORT_SUCCESS_WAIT_MIN = 1200
    const val SORT_SUCCESS_WAIT_MAX = 2400
    const val SORT_CHECK_INTERVAL = 2400
    const val SORT_INITIAL_WAIT = 7200L
    const val SORT_RETAP_MIN = 500
    const val SORT_RETAP_MAX = 800
    const val SORT_MAIN_CHECK_INTERVAL = 1800
    const val SORT_POST_INTERRUPT_WAIT = 600

    // ========================================
    // CLEANUP SLEEP TIMINGS
    // ========================================

    const val CLEANUP_ALCH_MIN = 3000
    const val CLEANUP_ALCH_MAX = 3600
    const val CLEANUP_DROP_MIN = 200
    const val CLEANUP_DROP_MAX = 300

    // ========================================
    // WALK TO SORT SLEEP TIMINGS
    // ========================================

    const val WALKTOSORT_CAMERA_MIN = 600
    const val WALKTOSORT_CAMERA_MAX = 1200
    const val WALKTOSORT_WALK_MIN = 1800
    const val WALKTOSORT_WALK_MAX = 2400

    // ========================================
    // CARGO TAP COORDINATES
    // ========================================

    const val CARGO_TAP_1_X = 584
    const val CARGO_TAP_1_Y = 148
    const val CARGO_TAP_2_X = 143
    const val CARGO_TAP_2_Y = 237
    const val CARGO_TAP_3_X = 571
    const val CARGO_TAP_3_Y = 159
    const val CARGO_TAP_4_X = 432
    const val CARGO_TAP_4_Y = 490

    // ========================================
    // ASSIGNMENT TAP COORDINATES
    // ========================================

    const val ASSIGN_BOTH_1_X = 818  // OPEN TAB
    const val ASSIGN_BOTH_1_Y = 394
    const val ASSIGN_BOTH_2_X = 747  // FIRST SLOT
    const val ASSIGN_BOTH_2_Y = 435
    const val ASSIGN_BOTH_3_X = 690  // SIAD
    const val ASSIGN_BOTH_3_Y = 403
    const val ASSIGN_BOTH_4_X = 747  // SECOND SLOT
    const val ASSIGN_BOTH_4_Y = 469
    const val ASSIGN_BOTH_5_X = 684  // GHOST
    const val ASSIGN_BOTH_5_Y = 370
    const val ASSIGN_CANNON_X = 748  // CANNON SELECT
    const val ASSIGN_CANNON_Y = 401
    const val ASSIGN_BOTH_SCROLL_X = 773  // SCROLL (CLICK X3)
    const val ASSIGN_BOTH_SCROLL_Y = 477

    // ========================================
    // SORT TAP COORDINATES
    // ========================================

    const val SORT_BUTTON_X = 574
    const val SORT_BUTTON_Y = 359
    const val SORT_BUTTON_TOLERANCE_X = 10
    const val SORT_BUTTON_TOLERANCE_Y = 10

    // ========================================
    // HOOK/SALVAGE TAP COORDINATES
    // ========================================

    const val HOOK_SALVAGE_1_X = 525 // TAP HOOK
    const val HOOK_SALVAGE_1_Y = 406 // TAP HOOK
    const val HOOK_SALVAGE_2_X = 337 // TAP CARGO
    const val HOOK_SALVAGE_2_Y = 350 // TAP CARGO
    const val HOOK_SALVAGE_3_X = 551
    const val HOOK_SALVAGE_3_Y = 308
    const val HOOK_SALVAGE_4_X = 570
    const val HOOK_SALVAGE_4_Y = 165
    const val HOOK_SALVAGE_6_X = 791 // TAP WALK TO HOOK
    const val HOOK_SALVAGE_6_Y = 63 // TAP WALK TO HOOK

    // ========================================
    // WALK TO SORT TAP COORDINATES
    // ========================================

    const val WALKTOSORT_TAP_X = 580
    const val WALKTOSORT_TAP_Y = 482
}
