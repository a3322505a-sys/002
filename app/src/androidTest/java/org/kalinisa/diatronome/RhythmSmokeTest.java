package org.kalinisa.diatronome;

import android.app.Instrumentation;
import android.content.Intent;
import java.io.InputStream;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kalinisa.diatronome.Cores.TuneBeatCore;
import org.kalinisa.diatronome.Tools.BeatConfig;
import org.kalinisa.diatronome.Tools.RhythmScore;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RhythmSmokeTest {
    @Test public void scoreFollowsAudioAndReturnsToSavedMeter() throws Exception {
        Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        MainActivity activity=(MainActivity)ins.startActivitySync(new Intent(ins.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.waitForIdleSync();TuneBeatCore core=TuneBeatCore.getInstance();
        try {
            ins.runOnMainSync(()->{
                activity.stopMetronome();core.requestConfig(BeatConfig.of(6,8,3));core.setTempoBpm(120);
                activity.loadRhythmExercise(RhythmScore.randomPractice());
                assertNotNull(activity.findViewById(R.id.rhythm_score));
                assertEquals(12,core.getRhythmScore().bars());
                activity.findViewById(R.id.play_pause).performClick();
            });
            Thread.sleep(1800);
            assertTrue(core.getIsPlaying());
            assertTrue("score cursor never reached the audio head",core.getCurrentRhythmIndex()>=0);
            try(InputStream image=new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                ins.getUiAutomation().executeShellCommand("screencap -p /sdcard/Download/tunebeat-rhythm.png"))){while(image.read()!=-1){}}
            ins.runOnMainSync(()->{
                activity.loadRhythmExercise(null);
                assertNull(core.getRhythmScore());assertEquals(BeatConfig.of(6,8,3),core.getConfig());
                assertNotNull(activity.findViewById(R.id.meter_config));
            });
        } finally {
            ins.runOnMainSync(()->{activity.stopMetronome();core.loadRhythmExercise(null);core.setTempoBpm(60);core.requestConfig(BeatConfig.DEFAULT);activity.finish();});
            ins.waitForIdleSync();
        }
    }
}
