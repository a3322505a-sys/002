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
import org.kalinisa.diatronome.Tools.*;
import java.io.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class MeterSmokeTest {
 @Test public void activeConfigPendingAndSilentRestart()throws Exception{
  Instrumentation ins=InstrumentationRegistry.getInstrumentation();
  ins.getTargetContext().getSharedPreferences("tunebeat",0).edit().putBoolean("tunerPage",false).commit();
  MainActivity a=(MainActivity)ins.startActivitySync(new Intent(ins.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));ins.waitForIdleSync();
  TuneBeatCore c=TuneBeatCore.getInstance();
  try{
   ins.runOnMainSync(()->{c.setTempoBpm(240);c.requestConfig(BeatConfig.of(4,4,4));a.findViewById(R.id.play_pause).performClick();});
   Thread.sleep(400);assertTrue(c.getIsPlaying());
   ins.runOnMainSync(()->{c.requestConfig(BeatConfig.of(3,4,2));c.requestConfig(BeatConfig.of(6,8,3));});
   Thread.sleep(1500);assertEquals(BeatConfig.of(6,8,3),c.getConfig());assertTrue(c.getIsPlaying());
   ins.runOnMainSync(()->assertTrue(((TextView)a.findViewById(R.id.meter_config)).getText().toString().contains("6/8")));
   try(InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.getUiAutomation().executeShellCommand("screencap -p /sdcard/Download/tunebeat-meter-v02.png"))){while(in.read()!=-1){}}
   ins.runOnMainSync(()->{c.requestConfig(BeatConfig.of(2,4,3));a.stopMetronome();});Thread.sleep(200);
   assertEquals(BeatConfig.of(2,4,3),c.getConfig());assertFalse(c.getIsPlaying());
   assertEquals(2,ins.getTargetContext().getSharedPreferences("tunebeat",0).getInt("meter_n",0));
  }finally{ins.runOnMainSync(()->{a.stopMetronome();c.setTempoBpm(60);c.requestConfig(BeatConfig.DEFAULT);a.finish();});ins.waitForIdleSync();}
 }
}
