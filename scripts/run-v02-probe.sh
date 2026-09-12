#!/usr/bin/env bash
set -euo pipefail
probe_tmp=$(mktemp -d)
trap 'rm -rf "$probe_tmp"' EXIT
mkdir -p verification
# YIN stays benchmark-only; production uses one MPM implementation with an explicitly lower cutoff.
java com.sun.tools.javac.Main -d "$probe_tmp" \
 scripts/reference/be/tarsos/dsp/pitch/*.java \
 app/src/main/java/org/kalinisa/diatronome/Cores/SoundAnalyzer/ISoundAnalyzer.java \
 app/src/main/java/org/kalinisa/diatronome/Cores/SoundAnalyzer/SoundAnalyzerEnvelop.java \
 scripts/PitchComparison.java
java -cp "$probe_tmp" PitchComparison "$@" | tee verification/pitch-comparison.csv
java com.sun.tools.javac.Main -cp "$probe_tmp" -d "$probe_tmp" \
 app/src/main/java/be/tarsos/dsp/pitch/*.java \
 app/src/main/java/org/kalinisa/diatronome/Tools/{BeatConfig,BeatSequence,BeatRenderer,TuningMath,GuitarPitchDetector,TuningTracker}.java \
 scripts/V02Probe.java
java -cp "$probe_tmp" V02Probe "$@" | tee verification/v02-probe.txt
