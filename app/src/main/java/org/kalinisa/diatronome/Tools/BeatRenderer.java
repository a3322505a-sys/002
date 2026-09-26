package org.kalinisa.diatronome.Tools;
import java.util.ArrayDeque;

/** One continuous mono PCM stream; never pause/flush between ticks. */
public final class BeatRenderer {
    private final BeatSequence sequence;
    private BeatSequence.Event sounding;
    private long nextFrame;
    private long frame;
    private final RhythmScore rhythm;
    private int beatCount, rhythmBar, rhythmBeat, nextNoteIndex;
    private long beatStart, nextNoteFrame=Long.MAX_VALUE;
    private double beatFrames;
    public BeatRenderer(BeatSequence s){this(s,null);}
    public BeatRenderer(BeatSequence s,RhythmScore score){sequence=s;rhythm=score;}
    private void scheduleNote() {
        if(rhythm==null){nextNoteFrame=Long.MAX_VALUE;return;}
        int lower=rhythmBeat*RhythmScore.UNITS_PER_BEAT;
        while(nextNoteIndex<rhythm.size()){
            RhythmScore.Note n=rhythm.note(nextNoteIndex);
            if(n.bar>rhythmBar||n.bar==rhythmBar&&n.startUnit>=lower+RhythmScore.UNITS_PER_BEAT)break;
            if(n.bar==rhythmBar&&n.startUnit>=lower){
                nextNoteFrame=beatStart+Math.round((n.startUnit-lower)*beatFrames/RhythmScore.UNITS_PER_BEAT);
                return;
            }
            nextNoteIndex++;
        }
        nextNoteFrame=Long.MAX_VALUE;
    }
    public void render(short[] pcm,ArrayDeque<BeatSequence.Event> events){
        for(int i=0;i<pcm.length;i++,frame++){
            if(frame>=nextFrame){
                sounding=sequence.next();events.addLast(sounding);nextFrame=sequence.nextFrame();
                if(rhythm!=null){
                    int beat=beatCount++%(rhythm.bars()*4);
                    rhythmBar=beat/4;rhythmBeat=beat%4;
                    if(beat==0)nextNoteIndex=0;
                    beatStart=sounding.frame;beatFrames=BeatSequence.RATE*60.0/sounding.bpm;
                    scheduleNote();
                }
            }
            while(frame>=nextNoteFrame){
                events.addLast(new BeatSequence.Event(nextNoteFrame,sounding.tick,sounding.bpm,sounding.config,nextNoteIndex));
                nextNoteIndex++;scheduleNote();
            }
            long age=sounding==null?Long.MAX_VALUE:frame-sounding.frame;
            if(age<1058){
                int accent=sounding.config.accent(sounding.tick);
                double hz=accent==4?880:accent==3?440:660;
                double amp=accent==4?.90:accent==3?.72:.40;
                double envelope=Math.min(1,age/66.0)*Math.pow(1-age/1058.0,2);
                pcm[i]=(short)(32767*amp*envelope*Math.sin(2*Math.PI*hz*age/BeatSequence.RATE));
            }else pcm[i]=0;
        }
    }
}
