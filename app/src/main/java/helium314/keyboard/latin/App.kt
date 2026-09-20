// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
package helium314.keyboard.latin

import android.app.Application
import android.os.Build
import helium314.keyboard.keyboard.emoji.SupportedEmojis
import helium314.keyboard.latin.define.DebugFlags
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.FoldableUtils
import helium314.keyboard.latin.utils.LayoutUtilsCustom
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.SubtypeSettings
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

    /** Enable the agreed languages once, without overriding later user choices. */
    private fun configureAuroraLanguages() {
        val preferences = prefs()
        val initializedKey = "aurora_languages_initialized_v1"
        if (preferences.getBoolean(initializedKey, false)) return

        val english = SubtypeSettings.getResourceSubtypesForLocale(Locale.US).firstOrNull()
        val latinAmericanSpanish = SubtypeSettings.getResourceSubtypesForLocale(
            Locale.forLanguageTag("es-419")
        ).firstOrNull()
        if (english == null || latinAmericanSpanish == null) {
            Log.w("Aurora", "English or Latin American Spanish subtype unavailable; leaving settings intact")
            return
        }

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
