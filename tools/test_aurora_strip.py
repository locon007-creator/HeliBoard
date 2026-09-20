#!/usr/bin/env python3
"""Source-level Aurora acceptance checks; device behavior requires separate testing."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'


class AuroraLayoutTests(unittest.TestCase):
    def test_prediction_strip_occupies_available_width(self):
        layout = ET.parse(ROOT / 'app/src/main/res/layout/suggestions_strip.xml').getroot()
        strip = next(node for node in layout.iter() if node.get(ANDROID + 'id') == '@+id/suggestions_strip')
        self.assertEqual('0dp', strip.get(ANDROID + 'layout_width'))
        self.assertEqual('1', strip.get(ANDROID + 'layout_weight'))
        self.assertIsNone(strip.get(ANDROID + 'maxWidth'))

    def test_two_equal_prediction_slots(self):
        values = ET.parse(ROOT / 'app/src/main/res/values/config-common.xml').getroot()
        named = {node.get('name'): node.text.strip() for node in values if node.get('name')}
        self.assertEqual('2', named['config_suggestions_count_in_strip'])
        self.assertEqual('50%', named['config_center_suggestion_percentile'])

    def test_permanent_number_row_enabled_in_defaults(self):
        defaults = (ROOT / 'app/src/main/java/helium314/keyboard/latin/settings/Defaults.kt').read_text()
        self.assertRegex(defaults, r'const val PREF_SHOW_NUMBER_ROW\s*=\s*true\b')

    def test_english_and_latin_american_spanish_are_enabled_on_startup(self):
        method = ET.parse(ROOT / 'app/src/main/res/xml/method.xml').getroot()
        available = {node.get(ANDROID + 'languageTag') for node in method if node.tag == 'subtype'}
        self.assertIn('en-US', available)
        self.assertIn('es-419', available)
        self.assertTrue((ROOT / 'app/src/main/assets/dicts/main_en-US.dict').is_file())
        self.assertTrue((ROOT / 'app/src/main/assets/dicts/main_es.dict').is_file())
        app = (ROOT / 'app/src/main/java/helium314/keyboard/latin/App.kt').read_text()
        self.assertIn('configureAuroraLanguages()', app)
        self.assertIn('SubtypeSettings.addEnabledSubtype', app)
        self.assertIn('es-419', app)

    def test_both_dictionaries_can_predict_without_language_switching(self):
        app = (ROOT / 'app/src/main/java/helium314/keyboard/latin/App.kt').read_text()
        engine = (ROOT / 'app/src/main/java/helium314/keyboard/latin/DictionaryFacilitatorImpl.kt').read_text()
        self.assertIn('ExtraValue.SECONDARY_LOCALES', app)
        self.assertIn('SubtypeUtilsAdditional.changeAdditionalSubtype', app)
        self.assertIn('"aurora_languages_initialized_v2"', app)
        self.assertIn('getSecondaryLocales(selectedSubtype.extraValue)', engine)
        self.assertIn('"es-419"', app)
        self.assertIn('"en-US"', app)

    def test_learning_user_words_enabled_only_if_not_previously_set(self):
        app = (ROOT / 'app/src/main/java/helium314/keyboard/latin/App.kt').read_text()
        engine = (ROOT / 'app/src/main/java/helium314/keyboard/latin/DictionaryFacilitatorImpl.kt').read_text()
        self.assertIn('configureAuroraLearning()', app)
        self.assertIn('preferences.contains(Settings.PREF_ADD_TO_PERSONAL_DICTIONARY)', app)
        self.assertIn('putBoolean(Settings.PREF_ADD_TO_PERSONAL_DICTIONARY, true)', app)
        self.assertIn('sv.mAddToPersonalDictionary', engine)
        self.assertIn('UserHistoryDictionary.addToDictionary', engine)

    def test_aurora_colorful_rounded_theme_defaults_preserve_existing_settings(self):
        app = (ROOT / 'app/src/main/java/helium314/keyboard/latin/App.kt').read_text()
        themes = (ROOT / 'app/src/main/java/helium314/keyboard/keyboard/KeyboardTheme.kt').read_text()
        self.assertIn('configureAuroraAppearance()', app)
        self.assertIn('KeyboardTheme.STYLE_ROUNDED', app)
        self.assertIn('KeyboardTheme.THEME_INDIGO', app)
        self.assertIn('KeyboardTheme.THEME_OCEAN', app)
        self.assertIn('preferences.contains(Settings.PREF_THEME_STYLE)', app)
        self.assertIn('THEME_OCEAN -> DefaultColors(', themes)
        self.assertIn('THEME_INDIGO -> DefaultColors(', themes)


if __name__ == '__main__':
    unittest.main(verbosity=2)
