import org.kalinisa.diatronome.Tools.*;
import java.util.*;
import java.nio.file.*;
public class V02Probe {
 static int checks=0;
 static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static GuitarPitchDetector.Result tone(double hz){return new GuitarPitchDetector.Result(hz,1,.1,false);}
 static TuningTracker.Reading settle(TuningTracker t,double hz,long start){TuningTracker.Reading r=null;for(int i=0;i<6;i++)r=t.accept(tone(hz),start+i*46);return r;}
 public static void main(String[] args)throws Exception{
  List<BeatConfig> configs=new ArrayList<>();for(int n=2;n<=4;n++)for(int s=1;s<=4;s++)configs.add(BeatConfig.of(n,4,s));configs.add(BeatConfig.of(6,8,3));
  for(BeatConfig c:configs)for(int bpm:new int[]{30,50,60,120,137,240}){
   BeatSequence seq=new BeatSequence(c,bpm);long previous=-1;
   for(int i=0;i<c.ticks()*20;i++){
    BeatSequence.Event e=seq.next();long expected=Math.round(i*44100*60.0/(bpm*c.subdivision));
    check(e.frame==expected,"sample drift "+c.meter()+" "+bpm);check(e.frame>previous,"duplicate tick");previous=e.frame;
    check(e.config.accent(e.tick)==(i%c.ticks()==0?4:i%c.subdivision==0?3:2),"accent");
   }
  }
  BeatSequence compound=new BeatSequence(BeatConfig.of(6,8,3),60);
  for(int i=0;i<=6;i++){BeatSequence.Event e=compound.next();if(i==3)check(e.frame==44100,"6/8 second main beat");if(i==6)check(e.frame==88200,"6/8 two seconds per bar");}
  for(BeatConfig before:configs)for(BeatConfig after:configs){
   BeatSequence seq=new BeatSequence(before,60);seq.next();seq.request(BeatConfig.of(2,4,4),60);seq.request(after,60);
   for(int i=1;i<before.ticks();i++)check(seq.next().config.equals(before),"mid-bar change");
   BeatSequence.Event e=seq.next();check(e.tick==0&&e.config.equals(after),"last choice boundary");
   check(e.frame==before.mainBeats()*44100,"bar timing after switch");
  }
  check(BeatConfig.of(6,8,1).equals(BeatConfig.DEFAULT),"invalid config");
  check(BeatConfig.of(99,4,1).equals(BeatConfig.DEFAULT),"invalid meter");
  // Inspect actual rendered PCM: verify an audible impulse follows each event, including 240 BPM x4.
  BeatSequence seq=new BeatSequence(BeatConfig.of(4,4,4),240);BeatRenderer renderer=new BeatRenderer(seq);
  ArrayDeque<BeatSequence.Event> events=new ArrayDeque<>();short[] pcm=new short[44100];renderer.render(pcm,events);
  check(events.size()==16,"PCM event count");for(BeatSequence.Event e:events){boolean sound=false;for(int i=(int)e.frame+1;i<e.frame+100;i++)sound|=pcm[i]!=0;check(sound,"missing PCM click");}
  for(int i:new int[]{5,1,4,0,3,2,5}){
   for(int offset:new int[]{-25,-10,-5,0,5,10,25})for(int kind=0;kind<5;kind++){
    double truth=TuningMath.frequency(i)*Math.pow(2,offset/1200.0);GuitarPitchDetector d=new GuitarPitchDetector();TuningTracker t=new TuningTracker();
    float[] f=PitchComparison.frame(truth,kind==1?.004:.3,kind,22);short[] input=new short[f.length];for(int n=0;n<f.length;n++)input[n]=(short)(f[n]*32768);
    TuningTracker.Reading r=null;for(int n=0;n<6;n++)r=t.accept(d.detect(input),n*46);
    check(r.hz>0,"pipeline lost string "+i+" offset "+offset+" kind "+kind);check(r.string==i,"wrong automatic target "+i);
    check(Math.abs(1200*Math.log(r.hz/truth)/Math.log(2))<12,"pipeline error");
    if(offset>=10)check(r.cents>0&&!r.inTune,"sharp false green string="+i+" offset="+offset+" kind="+kind+" measured="+r.cents);if(offset<=-10)check(r.cents<0&&!r.inTune,"flat false green string="+i+" offset="+offset+" kind="+kind+" measured="+r.cents);
   }
  }
  TuningTracker t=new TuningTracker();long clock=0;
  for(int i:new int[]{5,0,4,1,3,2}){TuningTracker.Reading r=settle(t,TuningMath.frequency(i),clock);check(r.string==i&&r.inTune,"continuous switch");clock+=300;}
  t.lock(5);TuningTracker.Reading r=settle(t,TuningMath.frequency(0),clock);check(r.string==5&&!r.inTune&&r.cents>2300,"manual octave fold");
  t.automatic();r=settle(t,TuningMath.frequency(0),clock+300);check(r.string==0&&!r.locked,"unlock");
  r=t.accept(new GuitarPitchDetector.Result(0,0,0,true),clock+600);check(r.hz==0&&!r.inTune&&r.string==-1,"silence stale target");
  r=t.accept(tone(TuningMath.frequency(5)),clock+650);check(r.hz==0,"history leaked");
  GuitarPitchDetector detector=new GuitarPitchDetector();short[] noise=new short[4096];Random random=new Random(71);for(int i=0;i<noise.length;i++)noise[i]=(short)(random.nextGaussian()*800);
  check(!detector.detect(noise).valid(),"noise accepted");check(!detector.detect(new short[4096]).valid(),"silence accepted");
  short[] mix=new short[4096];for(int i=0;i<mix.length;i++)mix[i]=(short)(6000*(Math.sin(2*Math.PI*82.406889*i/44100)+Math.sin(2*Math.PI*195.997718*i/44100)));
  check(!detector.detect(mix).valid(),"mixed strings accepted");
  // Half-frequency must remain half-frequency even while a string is locked.
  float[] half=PitchComparison.frame(TuningMath.frequency(5)/2,.3,0,1);short[] input=new short[4096];for(int i=0;i<4096;i++)input[i]=(short)(half[i]*32768);
  GuitarPitchDetector.Result h=detector.detect(input);check(h.valid()&&h.hz<43,"half-frequency folded");t.lock(5);r=null;for(int i=0;i<6;i++)r=t.accept(h,i*46);check(!r.inTune,"half frequency green");
  System.out.println("PASS "+checks+" assertions: 13 configs, all 169 boundary transitions, sample PCM, six-string PCM pipeline, noise, silence, lock and octave handling.");
  if(args.length>0){
    String raw=Files.readString(Path.of(args[0]));String[] tokens=raw.trim().split("[\\s,]+");double[] samples=new double[tokens.length];double max=0;for(int i=0;i<samples.length;i++){samples[i]=Double.parseDouble(tokens[i]);max=Math.max(max,Math.abs(samples[i]));}
    if(max<=1)for(int i=0;i<samples.length;i++)samples[i]*=32768;
    detector=new GuitarPitchDetector();t=new TuningTracker();int total=0,valid=0,wrong=0;double first=-1;ArrayList<Double> errors=new ArrayList<>();
    for(int start=0;start+4096<=samples.length;start+=2048){for(int i=0;i<4096;i++)input[i]=(short)samples[start+i];r=t.accept(detector.detect(input),Math.round((start+4096)*1000.0/44100));total++;
      if(r.hz>0){valid++;if(first<0)first=(start+4096)*1000.0/44100;if(r.string!=5)wrong++;errors.add(Math.abs(r.cents));}
    }Collections.sort(errors);
    System.out.printf(Locale.ROOT,"Real E2 pipeline: frames=%d valid=%d wrong_target=%d first_stable_ms=%.1f nominal_median_cents=%.2f nominal_p95_cents=%.2f%n",total,valid,wrong,first,PitchComparison.percentile(errors,.5),PitchComparison.percentile(errors,.95));
    check(valid>0&&wrong==0,"real E2 pipeline failed");
  }
 }
}
