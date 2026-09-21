#!/usr/bin/env bash
# Called as ONE script by android-emulator-runner. Its `script:` input runs each line
# in a separate POSIX sh process, so multiline shell state must live here.
set -euo pipefail
mkdir -p audit
stage='starting emulator smoke checks'
trap 'status=$?; if ((status != 0)); then printf "FAILED (exit %s): %s\n" "$status" "$stage" | tee -a audit/result.txt; adb logcat -d -v time -s AndroidRuntime:E ActivityManager:E ActivityTaskManager:E > audit/android-crashes.txt 2>&1 || true; adb shell dumpsys input_method > audit/input-method-dump.txt 2>&1 || true; fi' EXIT

stage='locating freshly built APK'
apk=$(find app/build/outputs/apk/debug -maxdepth 1 -name '*.apk' -print -quit)
test -n "$apk"
printf 'APK: %s\n' "$apk" | tee audit/apk-path.txt
sha256sum "$apk" > audit/apk-sha256.txt

stage='installing APK on Android emulator'
adb install -r "$apk" | tee audit/install.txt
PACKAGE='helium314.keyboard.auroranext'
stage='verifying installed package'
adb shell pm path "$PACKAGE" | tee audit/package.txt
grep -F 'package:' audit/package.txt
adb shell dumpsys package "$PACKAGE" > audit/package-dump.txt

stage='launching Aurora Settings activity'
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p "$PACKAGE" | tee audit/resolved-launcher.txt
adb logcat -c
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p "$PACKAGE" | tee audit/start-settings.txt
sleep 5
adb logcat -d -v time -s AndroidRuntime:E ActivityManager:E ActivityTaskManager:E > audit/android-crashes.txt
adb shell dumpsys activity activities > audit/activity-dump.txt
adb shell ps -A > audit/processes.txt
adb exec-out screencap -p > audit/settings-home.png
adb shell pidof "$PACKAGE" > audit/settings-process.txt || true
if ! grep -Eq '[0-9]+' audit/settings-process.txt; then
  echo 'Aurora Settings process not running after launch.' | tee audit/result.txt
  exit 1
fi

stage='capturing Settings UI'
adb shell uiautomator dump /sdcard/aurora-window.xml || true
adb pull /sdcard/aurora-window.xml audit/settings-hierarchy.xml || true
adb shell input swipe 500 1700 500 550 500 || true
sleep 2
adb exec-out screencap -p > audit/settings-scrolled.png

stage='verifying keyboard service registration and enabling IME'
adb shell ime list -s | tee audit/ime-services.txt
IME=$(grep -F "$PACKAGE/" audit/ime-services.txt | head -n 1 | tr -d '\r')
test -n "$IME"
adb shell ime enable "$IME" | tee audit/ime-enable.txt
adb shell ime set "$IME" | tee audit/ime-set.txt
adb shell dumpsys input_method > audit/input-method-dump.txt
grep -F "$PACKAGE" audit/input-method-dump.txt
grep -F "$IME" audit/input-method-dump.txt

stage='checking Android crash logs'
adb logcat -d -v brief -s AndroidRuntime:E > audit/android-crashes.txt
if grep -F -A 40 'FATAL EXCEPTION' audit/android-crashes.txt | grep -F "$PACKAGE"; then
  echo 'Aurora crashed during the emulator smoke audit' | tee audit/result.txt
  exit 1
fi
printf '%s\n' 'PASS: emulator APK install, Settings launch, and IME registration/selection. Actual typing, predictions and Samsung device behavior are NOT verified by this smoke test.' | tee audit/result.txt
