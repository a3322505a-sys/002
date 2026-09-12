package org.kalinisa.diatronome.Cores;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.*;
import android.os.*;
import org.kalinisa.diatronome.Tools.*;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Current TuneBeat playback engine. PCM sample positions, not callback timers, determine sound. */
public final class TuneBeatCore extends BaseCore {
    public static final int HANDLER_MSG_TICK=1,HANDLER_MSG_PLAY=2,HANDLER_MSG_CONFIG=4,HANDLER_MSG_ERROR=5;
    private static final TuneBeatCore INSTANCE=new TuneBeatCore();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->new Thread(r,"TuneBeat-playback"));
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile Session current;
    private volatile BeatConfig audible=BeatConfig.DEFAULT,requested=BeatConfig.DEFAULT;
    private volatile int bpm=60,currentTick=-1;
    private SharedPreferences prefs;
    private Runnable changed;
    private static final class Session {
        volatile boolean cancelled;AudioTrack track;final BeatSequence sequence;
        Session(BeatConfig c,int b){sequence=new BeatSequence(c,b);}
    }
    private TuneBeatCore(){}
    public static TuneBeatCore getInstance(){return INSTANCE;}
    public synchronized void configure(Context c){
        if(prefs!=null)return;prefs=c.getApplicationContext().getSharedPreferences("tunebeat",Context.MODE_PRIVATE);
        try{bpm=prefs.getInt("bpm",60);}catch(ClassCastException ignored){}if(bpm<30||bpm>240)bpm=60;
        try{requested=audible=BeatConfig.of(prefs.getInt("meter_n",4),prefs.getInt("meter_d",4),prefs.getInt("subdivision",1));}catch(ClassCastException ignored){requested=audible=BeatConfig.DEFAULT;}
    }
    public void setChangeListener(Runnable r){changed=r;}
    private void changed(){sendMessage(HANDLER_MSG_CONFIG,0,0);if(changed!=null)changed.run();}
    private void save(){if(prefs!=null)prefs.edit().putInt("meter_n",audible.numerator).putInt("meter_d",audible.denominator).putInt("subdivision",audible.subdivision).apply();}
    public BeatConfig getConfig(){return audible;}
    public BeatConfig getRequestedConfig(){return requested;}
    public int getTempoBpm(){return bpm;}
    public int getCurrentTick(){return currentTick;}
    public boolean getIsPlaying(){return current!=null;}
    public synchronized void setTempoBpm(int value){bpm=Math.max(30,Math.min(240,value));if(current!=null)current.sequence.request(requested,bpm);changed();}
    public synchronized void requestConfig(BeatConfig config){
        requested=config;
        if(current==null){audible=config;save();}else current.sequence.request(config,bpm);
        changed();
    }
    public synchronized void play(){
        if(current!=null)return;Session s=new Session(requested,bpm);current=s;currentTick=-1;worker.execute(()->stream(s));sendMessage(HANDLER_MSG_PLAY,1,0);
    }
    public synchronized void stop(){
        Session s=current;current=null;
        if(s!=null){s.cancelled=true;synchronized(s){if(s.track!=null)try{s.track.pause();s.track.flush();}catch(IllegalStateException ignored){}}}
        currentTick=-1;audible=requested;save();changed();sendMessage(HANDLER_MSG_PLAY,0,0);
    }
    private void stream(Session s){
        AudioTrack track=null;
        try{
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            int min=AudioTrack.getMinBufferSize(BeatSequence.RATE,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(min<=0)throw new IllegalStateException("No audio output");
            synchronized(s){
                if(s.cancelled)return;
                track=new AudioTrack(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                    new AudioFormat.Builder().setSampleRate(BeatSequence.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build(),Math.max(min,2048),AudioTrack.MODE_STREAM,AudioManager.AUDIO_SESSION_ID_GENERATE);
                s.track=track;if(track.getState()!=AudioTrack.STATE_INITIALIZED)throw new IllegalStateException("No audio output");track.play();
            }
            BeatRenderer renderer=new BeatRenderer(s.sequence);ArrayDeque<BeatSequence.Event> events=new ArrayDeque<>();short[] pcm=new short[256];
            while(!s.cancelled){
                renderer.render(pcm,events);int offset=0;
                while(offset<pcm.length&&!s.cancelled){int n=track.write(pcm,offset,pcm.length-offset);if(n<=0)throw new IllegalStateException("Audio write failed");offset+=n;}
                long head=((long)track.getPlaybackHeadPosition())&0xffffffffL;
                // Deliver only events the audio device has consumed; generation alone is not a visible beat.
                BeatSequence.Event latest=null;while(!events.isEmpty()&&events.peekFirst().frame<head)latest=events.removeFirst();
                if(latest!=null){final BeatSequence.Event e=latest;main.post(()->{
                    if(current!=s||s.cancelled)return;
                    if(!audible.equals(e.config)){audible=e.config;save();changed();}
                    currentTick=e.tick;sendMessage(HANDLER_MSG_TICK,e);});}
            }
        }catch(RuntimeException failure){if(!s.cancelled)main.post(()->{if(current==s){stop();sendMessage(HANDLER_MSG_ERROR,0,0);}});}
        finally{synchronized(s){if(track!=null){try{track.pause();track.flush();}catch(IllegalStateException ignored){}track.release();}s.track=null;}}
    }
}
