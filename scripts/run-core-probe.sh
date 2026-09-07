#!/usr/bin/env bash
set -euo pipefail
probe_tmp=$(mktemp -d)
trap 'rm -rf "$probe_tmp"' EXIT
mkdir -p "$probe_tmp/android/util" "$probe_tmp/classes" verification
# Only the timer's warning logger is Android-specific. This shim does not model audio.
cat > "$probe_tmp/android/util/Log.java" <<'JAVA'
package android.util;
public class Log {
    public static int w(String tag, String message) {
        System.err.println(tag + ": " + message);
        return 0;
    }
}
JAVA
javac -d "$probe_tmp/classes" "$probe_tmp/android/util/Log.java" \
  app/src/main/java/org/kalinisa/diatronome/Cores/AccuracyTimer.java \
  app/src/main/java/org/kalinisa/diatronome/Cores/SoundAnalyzer/*.java scripts/P0Probe.java
java -cp "$probe_tmp/classes" P0Probe | tee verification/core-probe.txt
