// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.JniUtils
import helium314.keyboard.latin.utils.NextScreenIcon
import helium314.keyboard.latin.utils.SubtypeLocaleUtils.displayName
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.latin.utils.previewDark
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.initPreview
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.preferences.PreferenceCategory
import helium314.keyboard.settings.screens.gesturedata.END_DATE_EPOCH_MILLIS
import helium314.keyboard.settings.screens.gesturedata.TWO_WEEKS_IN_MILLIS

/** A task-first home for all existing Aurora settings; no preference screen is removed. */
@Composable
fun MainSettingsScreen(
    onClickAbout: () -> Unit,
    onClickTextCorrection: () -> Unit,
    onClickPreferences: () -> Unit,
    onClickToolbar: () -> Unit,
    onClickGestureTyping: () -> Unit,
    onClickDataGathering: () -> Unit,
    onClickAdvanced: () -> Unit,
    onClickAppearance: () -> Unit,
    onClickLanguage: () -> Unit,
    onClickLayouts: () -> Unit,
    onClickDictionaries: () -> Unit,
    onClickBack: () -> Unit,
) {
    var showMore by rememberSaveable { mutableStateOf(false) }
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.ime_settings),
        settings = emptyList(),
    ) {
        val enabledSubtypes = SubtypeSettings.getEnabledSubtypes(true)
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) { innerPadding ->
            Column(Modifier.padding(innerPadding).verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.aurora_settings_intro),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                PreferenceCategory(stringResource(R.string.aurora_settings_typing))
                Preference(
                    name = stringResource(R.string.language_and_layouts_title),
                    description = enabledSubtypes.joinToString(", ") { it.displayName() }
                        .ifBlank { stringResource(R.string.aurora_settings_languages_description) },
                    onClick = onClickLanguage,
                    icon = R.drawable.ic_settings_languages,
                ) { NextScreenIcon() }
                Preference(
                    name = stringResource(R.string.settings_screen_correction),
                    description = stringResource(R.string.aurora_settings_correction_description),
                    onClick = onClickTextCorrection,
                    icon = R.drawable.ic_settings_correction,
                ) { NextScreenIcon() }
                Preference(
                    name = stringResource(R.string.settings_screen_preferences),
                    description = stringResource(R.string.aurora_settings_preferences_description),
                    onClick = onClickPreferences,
                    icon = R.drawable.ic_settings_preferences,
                ) { NextScreenIcon() }
                Preference(
                    name = stringResource(R.string.dictionary_settings_category),
                    description = stringResource(R.string.aurora_settings_dictionary_description),
                    onClick = onClickDictionaries,
                    icon = R.drawable.ic_dictionary,
                ) { NextScreenIcon() }

                PreferenceCategory(stringResource(R.string.aurora_settings_personalize))
                Preference(
                    name = stringResource(R.string.settings_screen_appearance),
                    description = stringResource(R.string.aurora_settings_appearance_description),
                    onClick = onClickAppearance,
                    icon = R.drawable.ic_settings_appearance,
                ) { NextScreenIcon() }
                Preference(
                    name = stringResource(R.string.settings_screen_toolbar),
                    description = stringResource(R.string.aurora_settings_toolbar_description),
                    onClick = onClickToolbar,
                    icon = R.drawable.ic_settings_toolbar,
                ) { NextScreenIcon() }

                PreferenceCategory(stringResource(R.string.aurora_settings_more))
                Preference(
                    name = stringResource(R.string.aurora_settings_more),
                    description = stringResource(R.string.aurora_settings_more_description),
                    onClick = { showMore = !showMore },
                    icon = R.drawable.ic_settings_advanced,
                ) { Text(if (showMore) "−" else "+") }
                AnimatedVisibility(visible = showMore) {
                    Column {
                        Preference(
                            name = stringResource(R.string.settings_screen_secondary_layouts),
                            onClick = onClickLayouts,
                            icon = R.drawable.ic_settings_layout,
                        ) { NextScreenIcon() }
                        if (JniUtils.sHaveGestureLib) {
                            Preference(
                                name = stringResource(R.string.settings_screen_gesture),
                                onClick = onClickGestureTyping,
                                icon = R.drawable.ic_settings_gesture,
                            ) { NextScreenIcon() }
                        }
                        if (JniUtils.sHaveGestureLib && System.currentTimeMillis() < END_DATE_EPOCH_MILLIS + TWO_WEEKS_IN_MILLIS) {
                            Preference(
                                name = stringResource(R.string.gesture_data_screen),
                                onClick = onClickDataGathering,
                                icon = R.drawable.ic_settings_gesture,
                            ) { NextScreenIcon() }
                        }
                        Preference(
                            name = stringResource(R.string.settings_screen_advanced),
                            onClick = onClickAdvanced,
                            icon = R.drawable.ic_settings_advanced,
                        ) { NextScreenIcon() }
                        Preference(
                            name = stringResource(R.string.settings_screen_about),
                            onClick = onClickAbout,
                            icon = R.drawable.ic_settings_about,
                        ) { NextScreenIcon() }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewScreen() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            MainSettingsScreen({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
    }
}
