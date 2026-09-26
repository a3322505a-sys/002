package org.kalinisa.diatronome.Tools;

/** Sample clock: fractional frames retained across events. Configuration changes occur at bar boundaries. */
public final class BeatSequence {
    public static final int RATE=44100;
    public static final class Event {
        public final long frame; public final int tick,bpm,rhythmIndex; public final BeatConfig config;
        Event(long f,int t,int b,BeatConfig c){this(f,t,b,c,-1);}
        Event(long f,int t,int b,BeatConfig c,int rhythmIndex){frame=f;tick=t;bpm=b;config=c;this.rhythmIndex=rhythmIndex;}
    }
    private BeatConfig active,requested;
    private int tempo,requestedTempo,tick;
    private double frame;
    public BeatSequence(BeatConfig c,int bpm){active=requested=c;tempo=requestedTempo=bpm;}
    public synchronized void request(BeatConfig c,int bpm){requested=c;requestedTempo=Math.max(30,Math.min(240,bpm));}
    public synchronized long nextFrame(){return Math.round(frame);}
    public synchronized Event next(){
        if(tick==0)active=requested;
        if(tick%active.subdivision==0)tempo=requestedTempo;
        Event e=new Event(Math.round(frame),tick,tempo,active);
        frame+=RATE*60.0/(tempo*active.subdivision);
        tick=(tick+1)%active.ticks();return e;
    }
}
