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
  for(int run=0;run<40;run++)for(RhythmGenerator.Difficulty difficulty:RhythmGenerator.Difficulty.values())
   for(RhythmGenerator.Theme theme:RhythmGenerator.Theme.values()){
   RhythmGenerator.Options options=new RhythmGenerator.Options(difficulty,theme);
   RhythmScore score=RhythmGenerator.generate(options,run);
   RhythmScore same=RhythmGenerator.generate(options,run);
   EnumSet<RhythmScore.Kind> kinds=EnumSet.noneOf(RhythmScore.Kind.class);
   check(score.bars()==12,"practice length");
   int[] barUnits=new int[12];for(int i=0;i<score.size();i++){RhythmScore.Note note=score.note(i);kinds.add(note.kind);barUnits[note.bar]+=note.kind.units;}
   for(int total:barUnits)check(total==RhythmScore.UNITS_PER_BAR,"invalid bar duration");
   check(score.size()==same.size(),"seed length");
   for(int i=0;i<score.size();i++)check(score.note(i).kind==same.note(i).kind,"seed reproducibility");
   if(options.difficulty==RhythmGenerator.Difficulty.BASIC)
    check(!kinds.contains(RhythmScore.Kind.DOTTED_QUARTER)&&!kinds.contains(RhythmScore.Kind.TRIPLET_EIGHTH),"basic range");
   if(options.difficulty==RhythmGenerator.Difficulty.INTERMEDIATE)
    check(!kinds.contains(RhythmScore.Kind.TRIPLET_EIGHTH),"intermediate range");
   if(theme!=RhythmGenerator.Theme.MIXED)for(int phrase=0;phrase<3;phrase++){
    int count=0;for(int i=0;i<score.size();i++)if(score.note(i).bar/4==phrase&&
      score.note(i).kind==(theme==RhythmGenerator.Theme.DOTTED?RhythmScore.Kind.DOTTED_QUARTER:RhythmScore.Kind.TRIPLET_EIGHTH))count++;
    check(count>=2,"focused phrase missing target");
   }
   for(int phrase=1;phrase<3;phrase++){
    check(score.note(0).kind==score.note(indexOfBar(score,phrase*4)).kind,"motif across phrases");
   }
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
  // Verify PCM, not just cursor events: offbeat notes sound, rests and held beats do not.
  RhythmScore fixture=RhythmScore.of(Arrays.asList(
   Arrays.asList(RhythmScore.Kind.EIGHTH_REST,RhythmScore.Kind.EIGHTH,RhythmScore.Kind.QUARTER,RhythmScore.Kind.EIGHTH,RhythmScore.Kind.EIGHTH,RhythmScore.Kind.QUARTER),
   Arrays.asList(RhythmScore.Kind.HALF,RhythmScore.Kind.QUARTER_REST,RhythmScore.Kind.QUARTER),
   Arrays.asList(RhythmScore.Kind.DOTTED_QUARTER,RhythmScore.Kind.EIGHTH,RhythmScore.Kind.TRIPLET_EIGHTH,RhythmScore.Kind.TRIPLET_EIGHTH,RhythmScore.Kind.TRIPLET_EIGHTH,RhythmScore.Kind.QUARTER)));
  for(int tempo:new int[]{30,120,137,240}){
   BeatRenderer r=new BeatRenderer(new BeatSequence(BeatConfig.DEFAULT,tempo),fixture);
   int length=(int)Math.ceil(2*fixture.bars()*4*BeatSequence.RATE*60.0/tempo);
   short[] samples=new short[length];ArrayDeque<BeatSequence.Event> positions=new ArrayDeque<>();
   // Streaming boundaries must not change note timing.
   for(int offset=0;offset<length;){short[] chunk=new short[Math.min(256,length-offset)];r.render(chunk,positions);System.arraycopy(chunk,0,samples,offset,chunk.length);offset+=chunk.length;}
   boolean[] allowed=new boolean[length];int index=0;
   for(BeatSequence.Event e:positions){
    check(e.rhythmIndex==index++%fixture.size(),"practice event order without metronome ticks");
    RhythmScore.Note n=fixture.note(e.rhythmIndex);int onset=(int)e.frame;
    if(n.kind.rest){for(int j=onset;j<Math.min(length,onset+1000);j++)check(samples[j]==0,"rest must be silent");}
    else{boolean audible=false;for(int j=onset;j<Math.min(length,onset+1058);j++){allowed[j]=true;audible|=samples[j]!=0;}check(audible,"score note must sound");}
   }
   check(index==fixture.size()*2,"two complete audible score loops");
   for(int j=0;j<length;j++)if(!allowed[j])check(samples[j]==0,"unexpected fixed metronome click or held-note retrigger");
  }
  System.out.println("PASS "+checks+" assertions: 13 configs, all 169 boundary transitions, sample PCM.");
 }
 static int indexOfBar(RhythmScore score,int bar){for(int i=0;i<score.size();i++)if(score.note(i).bar==bar)return i;throw new AssertionError();}
}
