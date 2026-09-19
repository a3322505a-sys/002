package org.kalinisa.diatronome;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kalinisa.diatronome.Cores.TuneBeatCore;
import java.io.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class BaselineSmokeTest {
    @Test public void metronomeControlsAndPlayback() throws Exception {
        Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        ins.getTargetContext().getSharedPreferences("tunebeat",0).edit().clear().putInt("bpm",999).putBoolean("tunerPage",true).putInt("string",5).putBoolean("microphoneAsked",true).commit();
        Intent intent=new Intent(ins.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity a=(MainActivity)ins.startActivitySync(intent);ins.waitForIdleSync();
        try{
            ins.runOnMainSync(()->{
                assertFalse(a.getSharedPreferences("tunebeat",0).contains("tunerPage"));
                assertEquals("60",((TextView)a.findViewById(R.id.tempo_value)).getText().toString());
                a.findViewById(R.id.tempo_plus).performClick();
                assertEquals("61",((TextView)a.findViewById(R.id.tempo_value)).getText().toString());
                a.findViewById(R.id.tempo_minus).performClick();
                a.findViewById(R.id.play_pause).performClick();
            });
            Thread.sleep(2500);assertTrue(TuneBeatCore.getInstance().getIsPlaying());
            ins.runOnMainSync(()->a.findViewById(R.id.tempo_plus).performClick());
            Thread.sleep(1300);assertTrue(TuneBeatCore.getInstance().getIsPlaying());
            ins.runOnMainSync(()->a.findViewById(R.id.play_pause).performClick());
            Thread.sleep(300);assertFalse(TuneBeatCore.getInstance().getIsPlaying());
            Bitmap b=ins.getUiAutomation().takeScreenshot();File dir=new File(ins.getTargetContext().getExternalFilesDir(null),"screens");dir.mkdirs();
            try(FileOutputStream out=new FileOutputStream(new File(dir,"metronome.png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}
        }finally{ins.runOnMainSync(()->{a.stopMetronome();a.finish();});ins.waitForIdleSync();}
    }
}
