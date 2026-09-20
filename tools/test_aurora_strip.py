#!/usr/bin/env python3
"""Source-level regression tests for the Aurora keyboard strip.

These checks do not replace Android-device interaction tests.
"""
from pathlib import Path
import re
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


if __name__ == '__main__':
    unittest.main(verbosity=2)
