package org.kalinisa.diatronome.Cores;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import androidx.core.app.NotificationCompat;
import org.kalinisa.diatronome.MainActivity;
import org.kalinisa.diatronome.R;

/** Owns background playback and audio focus. Process recreation never resumes sound. */
public class MetronomePlaybackService extends Service {
    public static final String PLAY="tunebeat.PLAY", STOP="tunebeat.STOP";
    private static final String CHANNEL="tunebeat_metronome";
    private AudioManager audio;
    private AudioFocusRequest focus;
    private boolean receiverRegistered;
    private final AudioManager.OnAudioFocusChangeListener focusChange=change->{if(change<0){MetronomeCore.getInstance().stop();stopSelf();}};
    private final BroadcastReceiver noisy=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){MetronomeCore.getInstance().stop();stopSelf();}};
    public static void configure(Context c,MetronomeCore core){
        if(core.getTempoBpm()>0)return;
        core.setRefPitch(440);core.setWaveFormAccent("SINE");core.setWaveFormMain("SINE");core.setWaveFormSubdivision("SINE");
        core.setPitchAccentSkb(5);core.setPitchMainSkb(3);core.setPitchSubdivisionSkb(3);
        core.setBeatsConfig(new int[]{4,3,3,3});
        int bpm=60;try{bpm=c.getSharedPreferences("tunebeat",MODE_PRIVATE).getInt("bpm",60);}catch(ClassCastException ignored){}
        if(bpm<30||bpm>240)bpm=60;core.setTempoBpm(bpm);
    }
    @Override public void onCreate(){super.onCreate();audio=(AudioManager)getSystemService(AUDIO_SERVICE);}
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null||!PLAY.equals(intent.getAction())){MetronomeCore.getInstance().stop();stopSelf();return START_NOT_STICKY;}
        MetronomeCore core=MetronomeCore.getInstance();configure(this,core);
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"节拍器播放",NotificationManager.IMPORTANCE_LOW));
        int immutable=Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0;
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|immutable);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,MetronomePlaybackService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|immutable);
        Notification n=new NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_launcher).setContentTitle("节拍器正在播放")
            .setContentText("4/4 · 每拍一下").setContentIntent(open).setOngoing(true).setSilent(true).addAction(0,"停止",stop).build();
        startForeground(1686,n);
        if(core.getIsPlaying())return START_NOT_STICKY;
        int granted;
        if(Build.VERSION.SDK_INT>=26){
            focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener(focusChange).build();
            granted=audio.requestAudioFocus(focus);
        }else granted=audio.requestAudioFocus(focusChange,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
        if(granted!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){stopSelf();return START_NOT_STICKY;}
        if(!receiverRegistered){
            androidx.core.content.ContextCompat.registerReceiver(this,noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);receiverRegistered=true;
        }
        core.play();return START_NOT_STICKY;
    }
    @Override public void onDestroy(){
        MetronomeCore.getInstance().stop();
        if(receiverRegistered)unregisterReceiver(noisy);
        if(audio!=null){if(Build.VERSION.SDK_INT>=26&&focus!=null)audio.abandonAudioFocusRequest(focus);else audio.abandonAudioFocus(focusChange);}
        super.onDestroy();
    }
}
