package helium314.keyboard

import helium314.keyboard.keyboard.KeyboardElement
import helium314.keyboard.keyboard.KeyboardLayoutSet
import helium314.keyboard.keyboard.internal.KeyboardParams
import helium314.keyboard.keyboard.internal.keyboard_parser.LocaleKeyboardInfos
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.common.LocaleUtils.constructLocale
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.SettingsSubtype.Companion.toSettingsSubtype
import helium314.keyboard.latin.utils.LayoutType
import helium314.keyboard.latin.utils.POPUP_KEYS_LAYOUT
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.SubtypeUtilsAdditional
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [
    ShadowInputMethodManager2::class
])
class SubtypeTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)
    private val params = KeyboardParams()

    init {
        ShadowLog.setupLogging()
        ShadowLog.stream = System.out
        params.mId = KeyboardLayoutSet.getFakeKeyboardId(KeyboardElement.ALPHABET)
        params.mPopupKeyOrder.add(POPUP_KEYS_LAYOUT)
        LocaleKeyboardInfos.addLocaleKeyTextsToParams(latinIME, params, LocaleKeyboardInfos.POPUP_KEYS_NORMAL)
    }

    @Test fun emptyAdditionalSubtypesResultsInEmptyList() {
        val prefs = latinIME.prefs()
        prefs.edit().putString(Settings.PREF_ADDITIONAL_SUBTYPES, "").apply()
        assertTrue(SubtypeSettings.getAdditionalSubtypes().isEmpty())
        val from = SubtypeSettings.getResourceSubtypesForLocale("es".constructLocale()).first()
        SubtypeUtilsAdditional.changeAdditionalSubtype(from.toSettingsSubtype(), from.toSettingsSubtype(), latinIME)
        assertEquals(emptyList(), SubtypeSettings.getAdditionalSubtypes().map { it.toSettingsSubtype() })
    }

    @Test fun subtypeStaysEnabledOnEdits() {
        val prefs = latinIME.prefs()
        prefs.edit().putString(Settings.PREF_ADDITIONAL_SUBTYPES, "").apply()
        val from = SubtypeSettings.getResourceSubtypesForLocale("es".constructLocale()).first()
        SubtypeSettings.addEnabledSubtype(prefs, from)
        val before = SubtypeSettings.getEnabledSubtypes(false).map { it.toSettingsSubtype() }
        assertTrue(from.toSettingsSubtype() in before)

        val to = from.toSettingsSubtype().withLayout(LayoutType.SYMBOLS, "symbols_arabic")
        SubtypeUtilsAdditional.changeAdditionalSubtype(from.toSettingsSubtype(), to, latinIME)
        val enabledAfterEdit = SubtypeSettings.getEnabledSubtypes(false).map { it.toSettingsSubtype() }
        assertTrue(to in enabledAfterEdit, "Edited Spanish subtype must stay enabled")
        assertEquals(before.size, enabledAfterEdit.size, "Editing Spanish must not remove or duplicate other languages")
        assertEquals(before.filterNot { it == from.toSettingsSubtype() }.toSet(), enabledAfterEdit.filterNot { it == to }.toSet())

        val toNew = to.withoutLayout(LayoutType.SYMBOLS)
        assertEquals(from.toSettingsSubtype(), toNew)
        SubtypeUtilsAdditional.changeAdditionalSubtype(to, toNew, latinIME)
        assertEquals(emptyList(), SubtypeSettings.getAdditionalSubtypes().map { it.toSettingsSubtype() })
        val enabledRestored = SubtypeSettings.getEnabledSubtypes(false).map { it.toSettingsSubtype() }
        assertTrue(from.toSettingsSubtype() in enabledRestored)
        assertEquals(before.toSet(), enabledRestored.toSet(), "Restoring Spanish must preserve both enabled languages")
    }
}
