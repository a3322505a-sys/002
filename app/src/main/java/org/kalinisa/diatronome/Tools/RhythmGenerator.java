package org.kalinisa.diatronome.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Two-beat vocabulary and three connected four-bar phrases. */
public final class RhythmGenerator {
    public enum Difficulty { BASIC, INTERMEDIATE, FULL }
    public enum Theme { MIXED, DOTTED, TRIPLET }
    public static final class Options {
        public static final Options DEFAULT = new Options(Difficulty.FULL, Theme.MIXED);
        public final Difficulty difficulty;
        public final Theme theme;
        public Options(Difficulty difficulty, Theme theme) {
            if (difficulty == null || theme == null) throw new IllegalArgumentException("Missing option");
            this.theme = theme;
            this.difficulty = theme == Theme.TRIPLET ? Difficulty.FULL
                : theme == Theme.DOTTED && difficulty == Difficulty.BASIC ? Difficulty.INTERMEDIATE : difficulty;
        }
    }
    private static final RhythmScore.Kind Q = RhythmScore.Kind.QUARTER;
    private static final RhythmScore.Kind E = RhythmScore.Kind.EIGHTH;
    private static final RhythmScore.Kind T = RhythmScore.Kind.TRIPLET_EIGHTH;
    private static final class Cell {
        final String name; final RhythmScore.Kind[] notes; final Theme feature; final int weight;
        Cell(String name, Theme feature, int weight, RhythmScore.Kind... notes) {
            this.name=name;this.feature=feature;this.weight=weight;this.notes=notes;
        }
    }
    private static final Cell[] CELLS = {
        new Cell("quarters",Theme.MIXED,5,Q,Q),
        new Cell("eighths-first",Theme.MIXED,4,E,E,Q),
        new Cell("eighths-last",Theme.MIXED,4,Q,E,E),
        new Cell("four-eighths",Theme.MIXED,2,E,E,E,E),
        new Cell("half",Theme.MIXED,2,RhythmScore.Kind.HALF),
        new Cell("dotted",Theme.DOTTED,4,RhythmScore.Kind.DOTTED_QUARTER,E),
        new Cell("quarter-rest",Theme.MIXED,2,RhythmScore.Kind.QUARTER_REST,Q),
        new Cell("eighth-rest",Theme.MIXED,2,RhythmScore.Kind.EIGHTH_REST,E,Q),
        new Cell("triplet-first",Theme.TRIPLET,3,T,T,T,Q),
        new Cell("triplet-last",Theme.TRIPLET,3,Q,T,T,T),
        new Cell("two-triplets",Theme.TRIPLET,1,T,T,T,T,T,T)
    };
    private RhythmGenerator() {}
    public static RhythmScore generate(Options options) { return generate(options,new Random()); }
    public static RhythmScore generate(Options options,long seed) { return generate(options,new Random(seed)); }
    public static RhythmScore generate(Options options,Random random) {
        if(options==null||random==null)throw new IllegalArgumentException("Missing generator input");
        // A recurring first half at bars 1, 5 and 9; optionally also at bar 3.
        Cell motif=pickBase(random,true), calm=CELLS[random.nextBoolean()?0:4];
        Cell[][] bars=new Cell[12][2];
        boolean fourth=random.nextBoolean();
        for(int phrase=0;phrase<3;phrase++){
            int start=phrase*4;
            bars[start][0]=motif;
            bars[start][1]=pick(random,options,false);
            bars[start+1][0]=differentBase(random,motif);
            bars[start+1][1]=bars[start][1];
            bars[start+2][0]=(phrase==0&&fourth)?motif:differentBase(random,motif);
            bars[start+2][1]=pick(random,options,false);
            bars[start+3][0]=differentBase(random,motif);
            bars[start+3][1]=calm;
        }
        // The shared motif appears across phrases three or four times (A and A' share it).
        // A' retains the first half within a phrase; other repeats reinforce that motif.
        // Limit consecutive dense/featured cells and give targeted practice two cells per phrase.
        for(int phrase=0;phrase<3;phrase++){
            int from=phrase*4;
            if(options.theme!=Theme.MIXED){
                bars[from][1]=pickFeature(random,options.theme);
                bars[from+2][1]=pickFeature(random,options.theme);
                bars[from+1][1]=bars[from][1];
            }
        }
        bars[11][1]=calm;
        for(int i=1,streak=difficult(bars[0][0])?1:0;i<24;i++){
            int bar=i/2,half=i%2;
            if(difficult(bars[bar][half]))streak++;else streak=0;
            int limit=options.theme==Theme.MIXED?2:3;
            if(streak>limit){bars[bar][half]=CELLS[0];streak=0;}
        }
        List<List<RhythmScore.Kind>> result=new ArrayList<>();
        for(Cell[] pair:bars){
            List<RhythmScore.Kind> notes=new ArrayList<>();
            notes.addAll(Arrays.asList(pair[0].notes));notes.addAll(Arrays.asList(pair[1].notes));result.add(notes);
        }
        return RhythmScore.of(result);
    }
    private static boolean difficult(Cell c){return c.feature!=Theme.MIXED||c==CELLS[3];}
    private static Cell differentBase(Random r,Cell other){
        Cell c=pickBase(r,false);
        return c==other?CELLS[other==CELLS[0]?1:0]:c;
    }
    private static Cell pickBase(Random r,boolean motif){
        int[] choices=motif?new int[]{0,1,2}:new int[]{0,1,2,3,4,6,7};
        return CELLS[choices[r.nextInt(choices.length)]];
    }
    private static Cell pickFeature(Random r,Theme theme){
        if(theme==Theme.DOTTED)return CELLS[5];
        return CELLS[8+r.nextInt(3)];
    }
    private static Cell pick(Random r,Options o,boolean ending){
        if(ending)return CELLS[r.nextBoolean()?0:4];
        if(o.theme!=Theme.MIXED && r.nextInt(100)<50)return pickFeature(r,o.theme);
        if(o.theme==Theme.MIXED && o.difficulty!=Difficulty.BASIC && r.nextInt(100)<35)
            return o.difficulty==Difficulty.INTERMEDIATE?CELLS[5]:r.nextBoolean()?CELLS[5]:pickFeature(r,Theme.TRIPLET);
        // In a focused exercise the accompaniment remains simple.
        if(o.theme!=Theme.MIXED)return pickBase(r,false);
        return pickBase(r,false);
    }
}
