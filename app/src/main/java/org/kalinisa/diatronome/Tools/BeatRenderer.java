package org.kalinisa.diatronome.Tools;
import java.util.ArrayDeque;

/** One continuous mono PCM stream; never pause/flush between ticks. */
public final class BeatRenderer {
    private final BeatSequence sequence;
    private BeatSequence.Event sounding;
    private long nextFrame;
    private long frame;
    public BeatRenderer(BeatSequence s){sequence=s;}
    public void render(short[] pcm,ArrayDeque<BeatSequence.Event> events){
        for(int i=0;i<pcm.length;i++,frame++){
            if(frame>=nextFrame){sounding=sequence.next();events.addLast(sounding);nextFrame=sequence.nextFrame();}
            long age=sounding==null?Long.MAX_VALUE:frame-sounding.frame;
            if(age<1058){
                int accent=sounding.config.accent(sounding.tick);
                double hz=accent==4?880:accent==3?440:660;
                double amp=accent==4?.72:accent==3?.48:.24;
                double envelope=Math.min(1,age/66.0)*Math.pow(1-age/1058.0,2);
                pcm[i]=(short)(32767*amp*envelope*Math.sin(2*Math.PI*hz*age/BeatSequence.RATE));
            }else pcm[i]=0;
        }
    }
}
