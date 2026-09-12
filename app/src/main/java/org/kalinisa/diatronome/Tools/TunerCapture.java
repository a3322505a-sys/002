package org.kalinisa.diatronome.Tools;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/** Each capture session exclusively owns recorder, detector and overlapping PCM window. */
public final class TunerCapture {
    public interface Listener { void pitch(GuitarPitchDetector.Result result); void error(); }
    private static final int RATE=44100, SAMPLES=4096;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->new Thread(r,"TuneBeat-microphone"));
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Listener listener;
    private volatile Session current;
    private boolean closed;
    private static class Session { volatile boolean cancelled; AudioRecord recorder; }
    public TunerCapture(Listener listener){this.listener=listener;}
    public synchronized void start(){
        if(closed||current!=null)return;
        Session s=new Session();current=s;worker.execute(()->capture(s));
    }
    public synchronized void stop(){
        Session s=current;current=null;if(s==null)return;
        synchronized(s){s.cancelled=true;if(s.recorder!=null)try{s.recorder.stop();}catch(IllegalStateException ignored){}}
    }
    public synchronized void close(){stop();closed=true;worker.shutdownNow();main.removeCallbacksAndMessages(null);}
    private void publish(Session s,GuitarPitchDetector.Result result){main.post(()->{if(current==s&&!s.cancelled)listener.pitch(result);});}
    private void capture(Session s){
        AudioRecord recorder=null;
        try{
            int min=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(min<=0)throw new IllegalStateException("Audio input unavailable");
            synchronized(s){
                if(s.cancelled)return;
                recorder=new AudioRecord(MediaRecorder.AudioSource.MIC,RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(SAMPLES*2,min));
                s.recorder=recorder;
                if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("Recorder uninitialized");
                recorder.startRecording();
            }
            GuitarPitchDetector detector=new GuitarPitchDetector();
            short[] pcm=new short[SAMPLES];int offset=0;
            while(!s.cancelled&&!Thread.currentThread().isInterrupted()){
                while(offset<SAMPLES&&!s.cancelled){int n=recorder.read(pcm,offset,SAMPLES-offset);if(n<=0)throw new IllegalStateException("Capture stopped");offset+=n;}
                if(s.cancelled)break;
                publish(s,detector.detect(pcm));
                System.arraycopy(pcm,GuitarPitchDetector.HOP,pcm,0,SAMPLES-GuitarPitchDetector.HOP);
                offset=SAMPLES-GuitarPitchDetector.HOP;
            }
        }catch(RuntimeException failure){
            if(!s.cancelled)main.post(()->{if(current==s){stop();listener.error();}});
        }finally{
            synchronized(s){if(recorder!=null){try{recorder.stop();}catch(IllegalStateException ignored){}recorder.release();}s.recorder=null;}
        }
    }
}
