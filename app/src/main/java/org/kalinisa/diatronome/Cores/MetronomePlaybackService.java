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
    private final AudioManager.OnAudioFocusChangeListener focusChange=change->{if(change<0){TuneBeatCore.getInstance().stop();stopSelf();}};
    private final BroadcastReceiver noisy=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){TuneBeatCore.getInstance().stop();stopSelf();}};
    public static void configure(Context c,TuneBeatCore core){core.configure(c);}
    private boolean foreground;
    private Notification notification(){
        int immutable=Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0;
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|immutable);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,MetronomePlaybackService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|immutable);
        TuneBeatCore core=TuneBeatCore.getInstance();
        return new NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_launcher).setContentTitle("节拍器正在播放")
            .setContentText(core.getTempoBpm()+" BPM · "+core.getConfig().meter()+" · "+core.getConfig().detail()).setContentIntent(open).setOngoing(true).setSilent(true).addAction(0,"停止",stop).build();
    }
    @Override public void onCreate(){super.onCreate();audio=(AudioManager)getSystemService(AUDIO_SERVICE);TuneBeatCore.getInstance().setChangeListener(()->{if(foreground){if(!TuneBeatCore.getInstance().getIsPlaying()){stopSelf();return;}((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(1686,notification());}});}
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null||!PLAY.equals(intent.getAction())){TuneBeatCore.getInstance().stop();stopSelf();return START_NOT_STICKY;}
        TuneBeatCore core=TuneBeatCore.getInstance();configure(this,core);
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"节拍器播放",NotificationManager.IMPORTANCE_LOW));
        startForeground(1686,notification());foreground=true;
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
        foreground=false;TuneBeatCore.getInstance().setChangeListener(null);TuneBeatCore.getInstance().stop();
        if(receiverRegistered)unregisterReceiver(noisy);
        if(audio!=null){if(Build.VERSION.SDK_INT>=26&&focus!=null)audio.abandonAudioFocusRequest(focus);else audio.abandonAudioFocus(focusChange);}
        super.onDestroy();
    }
}
