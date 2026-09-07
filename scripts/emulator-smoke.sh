#!/usr/bin/env bash
set -euo pipefail
mkdir -p verification
trap 'adb logcat -d > verification/logcat.txt' EXIT
adb install /tmp/diatronome.apk
adb install -g app/build/outputs/apk/debug/app-debug.apk
adb shell pm path org.kalinisa.diatronome
adb shell pm path io.github.a3322505a.tunebeat
gradle --no-daemon :app:connectedDebugAndroidTest
adb pull /sdcard/Android/data/io.github.a3322505a.tunebeat/files/screens verification/screens
# Exercise release code with a temporary CI certificate. The delivered APK is
# signed offline with the persistent private key, which is never sent to CI.
gradle --no-daemon --console=plain :app:signingReport > verification/signing-report.txt
smoke_keystore=$(awk '/^Store: / && $2 != "null" {sub(/^Store: /, ""); print; exit}' verification/signing-report.txt)
test -f "$smoke_keystore"
"$ANDROID_HOME/build-tools/36.1.0/apksigner" sign --ks "$smoke_keystore" --ks-pass pass:android --key-pass pass:android --out verification/smoke-release.apk dist/*-unsigned.apk
adb install -r verification/smoke-release.apk
adb shell pm revoke io.github.a3322505a.tunebeat android.permission.RECORD_AUDIO
adb shell am force-stop io.github.a3322505a.tunebeat
adb shell am start -W -n io.github.a3322505a.tunebeat/org.kalinisa.diatronome.MainActivity | tee verification/release-launch.txt
sleep 2
adb shell pidof io.github.a3322505a.tunebeat
adb shell screencap -p /sdcard/p0.png
adb pull /sdcard/p0.png verification/p0.png
adb shell am force-stop io.github.a3322505a.tunebeat
adb shell am start -W -n org.kalinisa.diatronome/.MainActivity | tee verification/original-launch.txt
sleep 2
adb shell pidof org.kalinisa.diatronome
