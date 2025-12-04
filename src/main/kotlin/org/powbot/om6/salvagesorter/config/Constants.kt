package org.powbot.om6.salvagesorter.config

/**
 * Constants used throughout the SalvageSorter script.
 */
object Constants {

    // ========================================
    // CARGO HOLD COORDINATES (Withdraw)
    // ========================================

    /** X coordinate for opening cargo hold interface at sorting location */
    const val CARGO_OPEN_X = 573
    /** Y coordinate for opening cargo hold interface at sorting location */
    const val CARGO_OPEN_Y = 161

    const val CARGO_OPEN_MENUOPTION = "Open"

    /** X coordinate for walking back to sorting position after withdrawing */
    const val CARGO_WALKBACK_X = 573
    /** Y coordinate for walking back to sorting position after withdrawing */
    const val CARGO_WALKBACK_Y = 482

    const val CARGO_WALKBACK_MENUOPTION = "Sort-salvage"
    // ========================================
    // SALVAGE HOOK COORDINATES
    // ========================================

    /** X coordinate for tapping the salvage hook to deploy it */
    const val HOOK_DEPLOY_X = 379
    /** Y coordinate for tapping the salvage hook to deploy it */
    const val HOOK_DEPLOY_Y = 218

    const val HOOK_DEPLOY_MENUOPTION = "Deploy"

    /** X coordinate for opening cargo hold at hook location (for depositing) */
    const val HOOK_CARGO_OPEN_X = 544
    /** Y coordinate for opening cargo hold at hook location (for depositing) */
    const val HOOK_CARGO_OPEN_Y = 280

    const val HOOK_CARGO_MENUOPTION = "Open"

    /** X coordinate for walking to hook location from sorting */
    const val HOOK_WALK_TO_X = 383
    /** Y coordinate for walking to hook location from sorting */
    const val HOOK_WALK_TO_Y = 188

    const val HOP_X = 763 //Skiff
    const val HOP_Y = 95 //Skiff
/*    const val HOP_X = 791
    const val HOP_Y = 63*/
    const val INITEXTRACTORX = 314
    const val INITEXTRACTORY = 238

    const val hopEXTRACTORX = 306
    const val hopEXTRACTORY = 377

    const val EXTRACTOR_SORT_X = 313
    const val EXTRACTOR_SORT_Y = 238
    const val EXTRACTOR_genX = 327
    const val EXTRACTOR_genY = 283
    const val EXTRACTOR_HOOK_X = 301
    const val EXTRACTOR_HOOK_Y = 375
    // ========================================
    // SORTING COORDINATES
    // ========================================

    /** X coordinate for tapping the sort button on salvage */
    const val SORT_BUTTON_X = 574
    /** Y coordinate for tapping the sort button on salvage */
    const val SORT_BUTTON_Y = 359

    const val SORT_BUTTON_MENUOPTION = "Sort-salvage"

    /** Tolerance for sort button X coordinate (for click validation) */
    const val SORT_BUTTON_TOLERANCE_X = 5
    /** Tolerance for sort button Y coordinate (for click validation) */
    const val SORT_BUTTON_TOLERANCE_Y = 5

    /** X coordinate for walking to sorting location from hook */
    const val SORT_WALK_TO_X = 579
    /** Y coordinate for walking to sorting location from hook */
    const val SORT_WALK_TO_Y = 490


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
    const val COMPONENT_CARGO_SPACE = 4
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
    const val INDEX_ASSIGNCONFIRM_SLOT1 = 1
    const val INDEX_ASSIGNCONFIRM_SLOT2 = 2


    const val COMPONENT_ASSIGN_WIDGETBACKBUTTON = 29
    const val INDEX_ASSIGNCONFIRM_BACKBUTTON = 1


    // ========================================
    // ASSIGNMENT SLEEP TIMINGS
    // ========================================

    const val ASSIGNMENT_INV_OPEN_MIN = 100
    const val ASSIGNMENT_INV_OPEN_MAX = 200

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
    const val SORT_MAIN_CHECK_INTERVAL = 600
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

    // ========================================
    // WIDGET INTERACTION SLEEP TIMINGS
    // ========================================

    /** Standard sleep between widget clicks/interactions */
    const val WIDGET_INTERACTION_MIN = 600
    const val WIDGET_INTERACTION_MAX = 900

}
