// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.internal

import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.utils.RecapitalizeMode
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test the actual modifier state machine, rather than a displayed shift icon. */
@RunWith(RobolectricTestRunner::class)
class AuroraShiftStateTest {
    @Test fun singleTapShiftChangesToUppercaseLayoutAndStaysUntilCharacter() {
        val actions = FakeActions()
        val keyboard = KeyboardState(actions)

        keyboard.onPressKey(KeyCode.SHIFT, 1, 0, null)
        assertEquals(ShiftMode.MANUAL, actions.lastShift)
        keyboard.onReleaseKey(KeyCode.SHIFT, false, 0, null)
        assertEquals(ShiftMode.MANUAL, actions.lastShift)
    }

    @Test fun doubleTapLocksCapsAndAnotherTapUnlocks() {
        val actions = FakeActions()
        val keyboard = KeyboardState(actions)

        keyboard.onPressKey(KeyCode.SHIFT, 1, 0, null)
        keyboard.onReleaseKey(KeyCode.SHIFT, false, 0, null)
        keyboard.onPressKey(KeyCode.SHIFT, 1, 0, null)
        assertEquals(ShiftMode.LOCKED, actions.lastShift)
        keyboard.onReleaseKey(KeyCode.SHIFT, false, 0, null)

        keyboard.onPressKey(KeyCode.SHIFT, 1, 0, null)
        assertEquals(ShiftMode.UNSHIFT, actions.lastShift)
        keyboard.onReleaseKey(KeyCode.SHIFT, false, 0, null)
    }

    private class FakeActions : KeyboardState.SwitchActions {
        var lastShift: ShiftMode = ShiftMode.UNSHIFT
        private var doubleTapTimerActive = false

        override fun setAlphabetKeyboard(shiftMode: ShiftMode) { lastShift = shiftMode }
        override fun setEmojiKeyboard() = Unit
        override fun setClipboardKeyboard() = Unit
        override fun setNumpadKeyboard() = Unit
        override fun setDpadKeyboard() = Unit
        override fun setSymbolsKeyboard() = Unit
        override fun setSymbolsShiftedKeyboard() = Unit
        override fun startDoubleTapShiftKeyTimer() { doubleTapTimerActive = true }
        override fun popDoubleTapShiftKeyTimer(): Boolean = doubleTapTimerActive.also { doubleTapTimerActive = false }
        override fun cancelDoubleTapShiftKeyTimer() { doubleTapTimerActive = false }
        override fun setOneHandedModeEnabled(enabled: Boolean) = Unit
        override fun switchOneHandedMode() = Unit
        override fun setFloatingKeyboardEnabled(enabled: Boolean) = Unit
    }
}
