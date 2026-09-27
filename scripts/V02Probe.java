import org.kalinisa.diatronome.Tools.*;
import java.util.*;
import java.nio.file.*;
public class V02Probe {
 static int checks=0;
 static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
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
  for(int run=0;run<20;run++){
   RhythmScore score=RhythmScore.randomPractice();EnumSet<RhythmScore.Kind> kinds=EnumSet.noneOf(RhythmScore.Kind.class);
   check(score.bars()==12,"practice length");
   int[] barUnits=new int[12];for(int i=0;i<score.size();i++){RhythmScore.Note note=score.note(i);kinds.add(note.kind);barUnits[note.bar]+=note.kind.units;}
   for(int total:barUnits)check(total==RhythmScore.UNITS_PER_BAR,"invalid bar duration");
   check(kinds.size()==RhythmScore.Kind.values().length,"missing initial rhythm kind");
  }
  RhythmScore score=RhythmScore.randomPractice();
  BeatRenderer practice=new BeatRenderer(new BeatSequence(BeatConfig.DEFAULT,120),score);
  ArrayDeque<BeatSequence.Event> cursor=new ArrayDeque<>();
  int cycle=score.bars()*4*BeatSequence.RATE/2;
  short[] audio=new short[cycle+1024];practice.render(audio,cursor);
  int nextNote=0;boolean wrapped=false;int accentPeak=0,ordinaryPeak=0;
  for(int i=0;i<1000;i++)accentPeak=Math.max(accentPeak,Math.abs((int)audio[i]));
  for(int i=BeatSequence.RATE/2;i<BeatSequence.RATE/2+1000;i++)ordinaryPeak=Math.max(ordinaryPeak,Math.abs((int)audio[i]));
  check(accentPeak>20000&&accentPeak>ordinaryPeak*1.15,"click gain and accent contrast");
  for(BeatSequence.Event event:cursor){
   if(event.rhythmIndex<0)continue;
   if(event.frame>=cycle){check(event.rhythmIndex==0&&event.frame==cycle,"loop onset");wrapped=true;break;}
   check(event.rhythmIndex==nextNote,"cursor order");
   RhythmScore.Note note=score.note(nextNote++);
   long expected=Math.round((note.bar*4+note.startUnit/12.0)*BeatSequence.RATE/2.0);
   check(event.frame==expected,"cursor sample onset");
  }
  check(nextNote==score.size()&&wrapped,"complete rhythm loop");
  System.out.println("PASS "+checks+" assertions: 13 configs, all 169 boundary transitions, sample PCM.");
 }
}
