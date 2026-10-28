#!/usr/bin/env bash
# Called by the screenshots workflow once the emulator is running.
set -euo pipefail

PKG=com.andoni.signosdaily
adb install -r app/build/outputs/apk/github/debug/app-github-debug.apk
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS || true

# Gesture navigation and a clean status bar.
adb shell cmd overlay enable com.android.internal.systemui.navbar.gestural || true
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
demo enter
demo clock -e hhmm 0941
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4
demo network -e mobile show -e datatype none -e level 4
demo notifications -e visible false
sleep 3

# Taps the centre of the first on-screen element whose text matches $1.
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null
  adb pull /sdcard/ui.xml ui.xml >/dev/null
  local xy
  xy=$(python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
for node in ET.parse("ui.xml").iter("node"):
    if node.get("text") == sys.argv[1]:
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  [ -n "$xy" ] && adb shell input tap $xy
}

open_tab() {
  adb shell am start -S -W -n "$PKG/.MainActivity" --es demo_tab "$1" >/dev/null
  # Leaves time for the sign videos to download and start playing.
  sleep 12
}

shot() { adb exec-out screencap -p > "shots/$1.png"; }

mkdir -p shots
for tab in today library progress settings; do
  open_tab "$tab"
  shot "$tab"
done
open_tab quiz
shot quiz_intro
tap_text "Empezar repaso" && sleep 8 && shot quiz

adb shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null
ls -l shots
