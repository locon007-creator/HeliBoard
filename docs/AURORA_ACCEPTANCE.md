# Aurora Next — acceptance and release gate

A successful APK compilation is not acceptance. Never label a build complete without verified functionality. Do not replace missing evidence with a version bump or a UI-only change.

| Requirement | Source or CI evidence | Actual phone verification | Status |
|---|---|---|---|
| Permanent number row | `Defaults.PREF_SHOW_NUMBER_ROW` and structural test | Verify across text fields, languages and theme changes | Source checked; device pending |
| Exactly two wide suggestion choices | `suggestions_strip.xml`, `config-common.xml`, CI patch to `SuggestionStripLayoutHelper.java`, structural tests | Type unfamiliar and familiar English/Spanish words; verify two readable tappable alternatives | Source/CI checked; device pending |
| No duplicate English box above predictions | Check suggestion-strip toolbar and any language indicator; language remains on spacebar | Inspect suggestions and toolbar on active keyboard | Not yet verified |
| Shift and Caps Lock | `KeyboardState.kt` handles press/release and lock transitions | Single Shift, double-tap Caps Lock, unlock, auto-cap, both languages and switching fields | Not yet functionally verified |
| English and Spanish prediction together | Offline `main_en-US.dict` and `main_es.dict`, linked secondary locales, source checks | Mixed English/Spanish sentence with suggestions **without switching** | Source checked; device pending |
| Learn personal words | Preferences opt in on new install; history and dictionary engine present | Repeated novel term, suggestion after restart, option to turn off/delete history | Source checked; device pending |
| Clipboard history | Default preference and existing clipboard manager | Copy, paste, pin, delete, retention and restart | Source checked; device pending |
| Number row, multicolor themes and customization | Source structural checks, existing theme editor | Portrait and landscape; theme changing; no clipped keys | Source checked; visual pending |
| Professional Settings | Organized home: Typing and languages / Appearance and tools / collapsed More; English and Spanish labels | Scroll, discover options, navigate, Back, persistence and accessibility at 360–430 dp | Compiled; interactive and visual pending |
| Correct installation and update | Separate package ID prevents conflict with older Aurora | Verify signature continuity and in-place upgrades | BLOCKED: CI debug signing is not guaranteed stable |

## Current automatic test gate

`.github/workflows/build-debug-apk.yml` runs seven structural checks and the full `:app:testDebugUnitTest` suite before packaging. Keep failures blocking APK publication; upload diagnostic reports even on failure. Java 21 is required by Robolectric/Android SDK 36. The initial complete run on 2026-09-20 executed 178 unit tests: 173 passed, five failed. Failures: legacy single-language assumption (test revised, rerun required), two broken third-party documentation URLs, previously documented Hangul editing issue, previously documented invalid emoji-modifier edge case. Check subsequent CI runs for accurate current totals.

## Release requirements

1. Fix or explicitly and transparently resolve failing tests; do not silently skip failures.
2. Verify all requested Aurora behavior on a real Android phone or an instrumented emulator. Robolectric, source checks and a green APK build are not substitutes for this.
3. Capture and examine Settings and keyboard screenshots for portrait clipping, typography, interaction size and accessibility; compare with requirements.
4. Resolve stable signing before describing later APKs as in-place upgrades.
5. Deliver only the tested APK matching the reported GitHub commit, with pass/fail evidence and an explicit list of anything still unverified.
