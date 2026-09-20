// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
package helium314.keyboard.latin

import android.app.Application
import android.os.Build
import helium314.keyboard.keyboard.emoji.SupportedEmojis
import helium314.keyboard.latin.common.Constants.Subtype.ExtraValue
import helium314.keyboard.latin.define.DebugFlags
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.SettingsSubtype.Companion.toSettingsSubtype
import helium314.keyboard.latin.utils.FoldableUtils
import helium314.keyboard.latin.utils.LayoutUtilsCustom
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.SubtypeUtilsAdditional
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.upgradeToolbarPrefs
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugFlags.init(this)
        FoldableUtils.init(this)
        Settings.init(this)
        SubtypeSettings.init(this)

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch { // do some uncritical work in background for faster startup
            SupportedEmojis.load(this@App)
            LayoutUtilsCustom.removeMissingLayouts(this@App)
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            @Suppress("DEPRECATION")
            Log.i(
                "startup", "Starting ${applicationInfo.processName} version ${packageInfo.versionName} (${
                    packageInfo.versionCode
                }) on Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
            )
        }

        RichInputMethodManager.init(this)
        checkVersionUpgrade(this)
        configureAuroraLanguages()
        if (BuildConfig.DEBUG) // do this on every debug apk start because we may work on adding a new toolbar key
            upgradeToolbarPrefs(prefs())
        transferOldPinnedClips(this) // todo: remove in a few months, maybe end 2026
        app = this
        Defaults.initDynamicDefaults(this)
    }

    /** Initialize agreed languages once, then configure the secondary dictionaries once.
     * Upgrading an existing Aurora installation must not reactivate languages the user disabled.
     */
    private fun configureAuroraLanguages() {
        val preferences = prefs()
        val initializedKey = "aurora_languages_initialized_v1"
        val bilingualKey = "aurora_languages_initialized_v2"
        if (preferences.getBoolean(bilingualKey, false)) return

        val english = SubtypeSettings.getResourceSubtypesForLocale(Locale.US).firstOrNull()
        val latinAmericanSpanish = SubtypeSettings.getResourceSubtypesForLocale(
            Locale.forLanguageTag("es-419")
        ).firstOrNull()
        if (english == null || latinAmericanSpanish == null) {
            Log.w("Aurora", "English or Latin American Spanish subtype unavailable; leaving settings intact")
            return
        }

        if (!preferences.getBoolean(initializedKey, false)) {
            val enabled = SubtypeSettings.getEnabledSubtypes(false)
            if (english !in enabled) SubtypeSettings.addEnabledSubtype(preferences, english)
            if (latinAmericanSpanish !in enabled) {
                SubtypeSettings.addEnabledSubtype(preferences, latinAmericanSpanish)
            }
            if (!preferences.contains(Settings.PREF_SELECTED_SUBTYPE)) {
                SubtypeSettings.setSelectedSubtype(preferences, english)
            }
            preferences.edit().putBoolean(initializedKey, true).apply()
        }

        // Merely enabling two languages makes users switch keyboards. SecondaryLocales is
        // the existing dictionary engine's actual mechanism for simultaneous suggestions.
        fun addSecondaryLanguage(mainTag: String, secondaryTag: String) {
            val subtype = SubtypeSettings.getEnabledSubtypes(false)
                .firstOrNull { it.locale().toLanguageTag() == mainTag } ?: return
            val original = subtype.toSettingsSubtype()
            val configured = original.getExtraValueOf(ExtraValue.SECONDARY_LOCALES)
                ?.split(":").orEmpty().filter { it.isNotBlank() }
            if (secondaryTag in configured) return
            val updated = original.with(
                ExtraValue.SECONDARY_LOCALES,
                (configured + secondaryTag).distinct().joinToString(":")
            )
            SubtypeUtilsAdditional.changeAdditionalSubtype(original, updated, this)
        }
        addSecondaryLanguage("en-US", "es-419")
        addSecondaryLanguage("es-419", "en-US")
        preferences.edit().putBoolean(bilingualKey, true).apply()
    }

    companion object {
        // used so JniUtils can access application once
        private var app: App? = null
        fun getApp(): App? {
            val application = app
            app = null
            return application
        }
    }
}
