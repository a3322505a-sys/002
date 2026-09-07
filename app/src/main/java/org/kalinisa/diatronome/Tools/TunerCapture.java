package org.kalinisa.diatronome.Tools;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.kalinisa.diatronome.Cores.SoundAnalyzer.SoundAnalyzerEnvelop;

/** Reuses upstream pitch detection; each capture session exclusively owns its recorder. */
public final class TunerCapture {
    public interface Listener { void pitch(double hz); void error(); }
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
    private void publish(Session s,double hz){main.post(()->{if(current==s&&!s.cancelled)listener.pitch(hz);});}
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
            SoundAnalyzerEnvelop detector=new SoundAnalyzerEnvelop(RATE);
            short[] pcm=new short[SAMPLES];double[] signal=new double[SAMPLES],recent=new double[3];int count=0,pos=0;
            while(!s.cancelled&&!Thread.currentThread().isInterrupted()){
                int offset=0;
                while(offset<SAMPLES&&!s.cancelled){int n=recorder.read(pcm,offset,SAMPLES-offset);if(n<=0)throw new IllegalStateException("Capture stopped");offset+=n;}
                if(s.cancelled)break;
                double power=0;for(int i=0;i<SAMPLES;i++){signal[i]=pcm[i]/32768.0;power+=signal[i]*signal[i];}
                if(Math.sqrt(power/SAMPLES)<0.012){count=0;publish(s,0);continue;}
                double hz=detector.getPitch(signal);
                if(hz<50||hz>1500||Double.isNaN(hz)||Double.isInfinite(hz)){count=0;publish(s,0);continue;}
                recent[pos++%3]=hz;count=Math.min(3,count+1);
                if(count==3){double[] sorted=recent.clone();Arrays.sort(sorted);publish(s,sorted[1]);}
            }
        }catch(RuntimeException failure){
            if(!s.cancelled)main.post(()->{if(current==s){stop();listener.error();}});
        }finally{
            synchronized(s){if(recorder!=null){try{recorder.stop();}catch(IllegalStateException ignored){}recorder.release();}s.recorder=null;}
        }
    }
}
