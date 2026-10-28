package com.andoni.signosdaily

/**
 * Store-screenshot mode, started from adb on debug builds:
 * `am start -n com.andoni.signosdaily/.MainActivity --es demo_tab today`.
 * It loads sample data and hides ads and pop-up dialogs.
 */
object DemoMode {
    @Volatile var active = false
}
