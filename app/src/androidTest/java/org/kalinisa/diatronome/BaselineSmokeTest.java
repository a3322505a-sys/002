package org.kalinisa.diatronome;

import android.app.Instrumentation;
import android.content.Intent;
import androidx.appcompat.widget.Toolbar;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kalinisa.diatronome.Cores.MetronomeCore;
import static org.junit.Assert.*;

/** Emulator smoke only: this does not measure audible timing or microphone accuracy. */
@RunWith(AndroidJUnit4.class)
public class BaselineSmokeTest {
    @Test public void launchSwitchToolsAndStartStopMetronome() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(instrumentation.getTargetContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity activity = (MainActivity) instrumentation.startActivitySync(intent);
        try {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                Toolbar toolbar = activity.findViewById(R.id.toolbar);
                activity.onOptionsItemSelected(toolbar.getMenu().findItem(R.id.action_metronome));
            });
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                assertNotNull(activity.findViewById(R.id.viewMetronome));
                MetronomeCore.getInstance().setTempoBpm(60);
                MetronomeCore.getInstance().play();
            });
            Thread.sleep(2500);
            assertTrue(MetronomeCore.getInstance().getIsPlaying());
            assertTrue(MetronomeCore.getInstance().getCurrentTick() >= 0);
            instrumentation.runOnMainSync(() -> {
                MetronomeCore.getInstance().stop();
                assertFalse(MetronomeCore.getInstance().getIsPlaying());
                Toolbar toolbar = activity.findViewById(R.id.toolbar);
                activity.onOptionsItemSelected(toolbar.getMenu().findItem(R.id.action_tuner));
            });
            instrumentation.waitForIdleSync();
            Thread.sleep(1000);
            instrumentation.runOnMainSync(() -> assertNotNull(activity.findViewById(R.id.viewNeedle)));
        } finally {
            instrumentation.runOnMainSync(() -> {
                MetronomeCore.getInstance().stop();
                activity.finish();
            });
            instrumentation.waitForIdleSync();
        }
    }
}
