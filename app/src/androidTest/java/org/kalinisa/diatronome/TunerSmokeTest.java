package org.kalinisa.diatronome;
import android.app.Instrumentation;
import android.content.Intent;
import android.widget.TextView;
import android.os.ParcelFileDescriptor;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kalinisa.diatronome.Cores.TuneBeatCore;
import org.kalinisa.diatronome.Tools.TuningMath;
import java.io.InputStream;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class TunerSmokeTest {
    private void shell(Instrumentation ins,String cmd)throws Exception{
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(ins.getUiAutomation().executeShellCommand(cmd))){while(in.read()!=-1){}}
    }
    @Test public void stringsSilenceAndSwitching()throws Exception{
        Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        ins.getTargetContext().getSharedPreferences("tunebeat",0).edit().clear().commit();
        MainActivity a=(MainActivity)ins.startActivitySync(new Intent(ins.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));ins.waitForIdleSync();
        try{
            ins.runOnMainSync(()->a.findViewById(R.id.play_pause).performClick());Thread.sleep(700);
            ins.runOnMainSync(()->a.findViewById(R.id.tab_tuner).performClick());Thread.sleep(700);
            assertFalse(TuneBeatCore.getInstance().getIsPlaying());
            int[] ids={R.id.string_1,R.id.string_2,R.id.string_3,R.id.string_4,R.id.string_5,R.id.string_6};
            for(int i=0;i<6;i++){
                final int string=i;
                ins.runOnMainSync(()->{
                    a.findViewById(ids[string]).performClick();
                    assertTrue(((TextView)a.findViewById(R.id.target_note)).getText().toString().contains(TuningMath.NOTES[string]));
                    for(int n=0;n<6;n++)a.renderPitch(TuningMath.frequency(string)*Math.pow(2,20/1200.0));assertEquals("偏高 · 调低",((TextView)a.findViewById(R.id.tuner_status)).getText().toString());
                    a.renderPitch(0);assertEquals("拨动琴弦",((TextView)a.findViewById(R.id.tuner_status)).getText().toString());
                });
            }
            // Rapid page switches exercise cancellation while microphone reads are pending.
            for(int i=0;i<4;i++){
                ins.runOnMainSync(()->a.findViewById(R.id.tab_metronome).performClick());
                ins.runOnMainSync(()->a.findViewById(R.id.tab_tuner).performClick());Thread.sleep(120);
            }
            Thread.sleep(800);shell(ins,"screencap -p /sdcard/Download/tunebeat-tuner.png");
            // Returning to the metronome page must not revive playback.
            ins.runOnMainSync(()->a.findViewById(R.id.tab_metronome).performClick());
            assertFalse(TuneBeatCore.getInstance().getIsPlaying());
        }finally{ins.runOnMainSync(()->{a.stopMetronome();a.finish();});ins.waitForIdleSync();}
    }
}
