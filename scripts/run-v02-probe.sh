#!/usr/bin/env bash
set -euo pipefail
probe_tmp=$(mktemp -d)
trap 'rm -rf "$probe_tmp"' EXIT
mkdir -p verification
java com.sun.tools.javac.Main -d "$probe_tmp" \
 app/src/main/java/org/kalinisa/diatronome/Tools/{BeatConfig,BeatSequence,BeatRenderer,RhythmScore,RhythmGenerator}.java \
 scripts/V02Probe.java
java -cp "$probe_tmp" V02Probe | tee verification/v02-probe.txt
