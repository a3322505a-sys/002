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
 @Test public void pitchDetectorOnAndroid(){
  GuitarPitchDetector detector=new GuitarPitchDetector();short[] pcm=new short[4096];long started=System.nanoTime();int count=0;
  for(int string=0;string<6;string++)for(int repeat=0;repeat<3;repeat++){
   double hz=TuningMath.frequency(string);
   for(int i=0;i<pcm.length;i++)pcm[i]=(short)(8000*Math.sin(2*Math.PI*hz*i/44100));
   GuitarPitchDetector.Result result=detector.detect(pcm);assertTrue(result.valid());assertEquals(string,TuningMath.nearestString(result.hz));count++;
  }
  android.util.Log.i("TuneBeat-V02","Emulator detector mean_ms="+((System.nanoTime()-started)/1e6/count)+" frames="+count+"; phone performance unmeasured");
 }
 @Test public void headstockTargetsAtNarrowWidth(){
  Instrumentation ins=InstrumentationRegistry.getInstrumentation();
  ins.runOnMainSync(()->{
   HeadstockView v=new HeadstockView(ins.getTargetContext());float density=ins.getTargetContext().getResources().getDisplayMetrics().density;
   int w=(int)(232*density),h=(int)(370*density);v.layout(0,0,w,h);
   float scale=Math.min(w/320f,h/640f),dx=(w-320*scale)/2;
   float[] x={207,192,177,162,147,132},y={62,146,230,314,398,482};
   for(int i=0;i<6;i++){float cx=dx+(x[i]-16)*scale,cy=y[i]*scale;assertEquals(i,v.hit(cx,cy));assertEquals(i,v.hit(cx-23*density,cy-23*density));assertEquals(i,v.hit(cx+23*density,cy+23*density));}
  });
 }
}
