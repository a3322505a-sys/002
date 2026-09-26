package org.kalinisa.diatronome.Tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** A 4/4 rhythm score. Twelve units per quarter accommodate eighths and triplets exactly. */
public final class RhythmScore {
    public static final int UNITS_PER_BEAT = 12;
    public static final int UNITS_PER_BAR = 4 * UNITS_PER_BEAT;

    public enum Kind {
        QUARTER(12, false), EIGHTH(6, false), HALF(24, false),
        DOTTED_QUARTER(18, false), TRIPLET_EIGHTH(4, false),
        QUARTER_REST(12, true), EIGHTH_REST(6, true);
        public final int units;
        public final boolean rest;
        Kind(int units, boolean rest) { this.units = units; this.rest = rest; }
    }

    public static final class Note {
        public final Kind kind;
        public final int bar, startUnit;
        Note(Kind kind, int bar, int startUnit) {
            this.kind = kind; this.bar = bar; this.startUnit = startUnit;
        }
    }

    private final List<Note> notes;
    private final int bars;
    private RhythmScore(List<Note> notes, int bars) {
        this.notes = Collections.unmodifiableList(notes); this.bars = bars;
    }
    public int bars() { return bars; }
    public int size() { return notes.size(); }
    public Note note(int index) { return notes.get(index); }

    /** Entry point for an externally supplied 4/4 score; each bar must be complete. */
    public static RhythmScore of(List<List<Kind>> bars) {
        if (bars == null || bars.isEmpty()) throw new IllegalArgumentException("Empty score");
        List<Note> notes = new ArrayList<>();
        for (int bar = 0; bar < bars.size(); bar++) {
            List<Kind> kinds = bars.get(bar);
            if (kinds == null) throw new IllegalArgumentException("Empty bar");
            int position = 0;
            for (Kind kind : kinds) {
                if (kind == null || position + kind.units > UNITS_PER_BAR)
                    throw new IllegalArgumentException("Invalid bar " + bar);
                notes.add(new Note(kind, bar, position));
                position += kind.units;
            }
            if (position != UNITS_PER_BAR) throw new IllegalArgumentException("Incomplete bar " + bar);
        }
        return new RhythmScore(notes, bars.size());
    }

    /** Twelve bars made from musical two-beat cells, with all initial note types represented. */
    public static RhythmScore randomPractice() {
        Kind Q = Kind.QUARTER, E = Kind.EIGHTH, T = Kind.TRIPLET_EIGHTH;
        Kind[][] cells = {
            {Q, Q}, {E, E, Q}, {Q, E, E}, {E, E, E, E},
            {Kind.HALF}, {Kind.DOTTED_QUARTER, E},
            {Kind.QUARTER_REST, Q}, {Kind.EIGHTH_REST, E, Q},
            {T, T, T, Q}, {Q, T, T, T}, {T, T, T, T, T, T}
        };
        Random random = new Random();
        List<Kind[]> chosen = new ArrayList<>();
        // Mandatory examples make a generated page useful even if random choices repeat.
        for (int i = 3; i <= 9; i++) chosen.add(cells[i]);
        while (chosen.size() < 24) chosen.add(cells[random.nextInt(cells.length)]);
        Collections.shuffle(chosen, random);
        List<List<Kind>> bars = new ArrayList<>();
        for (int bar = 0; bar < 12; bar++) {
            List<Kind> kinds = new ArrayList<>();
            Collections.addAll(kinds, chosen.get(bar * 2));
            Collections.addAll(kinds, chosen.get(bar * 2 + 1));
            bars.add(kinds);
        }
        return of(bars);
    }
}
