import be.tarsos.dsp.pitch.*;
import org.kalinisa.diatronome.Cores.SoundAnalyzer.SoundAnalyzerEnvelop;
import java.util.*;
import java.nio.file.*;

/** Same PCM16 frames through all candidates; speed measures desktop JVM only. */
public class PitchComparison {
  static final int RATE=44100,N=4096;
  static final double[] F={82.406889,110,146.832384,195.997718,246.941651,329.627557};
  static float[] frame(double f,double amplitude,int kind,int seed){
    float[] x=new float[N];Random r=new Random(seed);
    for(int i=0;i<N;i++){
      double t=i/(double)RATE,a=2*Math.PI*f*t;
      double v=kind==2? .12*Math.sin(a)+.65*Math.sin(2*a)+.23*Math.sin(3*a):Math.sin(a);
      if(kind==3)v=(.65*Math.sin(a)+.25*Math.sin(2*a)+.1*Math.sin(3*a))*Math.exp(-8*t)*Math.min(1,t/.004);
      if(kind==4)v+=.08*r.nextGaussian();
      x[i]=(short)Math.max(-32768,Math.min(32767,Math.round(amplitude*v*32767)))/32768f;
    }return x;
  }
  public static void main(String[] args)throws Exception{
    SoundAnalyzerEnvelop old=new SoundAnalyzerEnvelop(RATE);
    PitchDetector[] engines={new Yin(RATE,N,.1),new McLeodPitchMethod(RATE,N)};
    String[] names={"Envelop+old gate","Tarsos YIN .10","Tarsos MPM .97"};
    System.out.println("candidate,group,frames,median_abs_cents,p95_abs_cents,invalid,octave_errors,mean_ms");
    for(int e=0;e<3;e++)for(int kind=0;kind<5;kind++){
      ArrayList<Double> errors=new ArrayList<>();int invalid=0,octaves=0,total=0;long nanos=0;
      for(double f:F)for(int offset:new int[]{-25,-10,-5,0,5,10,25})for(int j=0;j<3;j++){
        double truth=f*Math.pow(2,offset/1200.0);float[] x=frame(truth,kind==1?.004:.3,kind,j);
        double[] d=new double[N];double power=0;for(int i=0;i<N;i++){d[i]=x[i];power+=d[i]*d[i];}
        long t=System.nanoTime();double hz=e==0?(Math.sqrt(power/N)<.012?0:old.getPitch(d)):engines[e-1].getPitch(x).getPitch();nanos+=System.nanoTime()-t;total++;
        if(hz<=0||!Double.isFinite(hz)){invalid++;continue;}
        double err=Math.abs(1200*Math.log(hz/truth)/Math.log(2));errors.add(err);if(err>1100)octaves++;
      }
      Collections.sort(errors);System.out.printf(Locale.ROOT,"%s,%s,%d,%.3f,%.3f,%d,%d,%.3f%n",names[e],new String[]{"sine","weak","harmonics","attack-decay","noise"}[kind],total,percentile(errors,.5),percentile(errors,.95),invalid,octaves,nanos/1e6/total);
    }
    if(args.length>0){
      String raw=Files.readString(Path.of(args[0]));if(raw.startsWith("version "))throw new IllegalArgumentException("LFS pointer is not audio");
      String[] tokens=raw.trim().split("[\\s,]+");float[] samples=new float[tokens.length];float max=0;
      for(int i=0;i<samples.length;i++){samples[i]=Float.parseFloat(tokens[i]);max=Math.max(max,Math.abs(samples[i]));}
      if(max>1)for(int i=0;i<samples.length;i++)samples[i]/=32768f;
      // Upstream E2 excerpt is 4095 samples: append one zero (0.023ms), never repeat the clip.
      if(samples.length==N-1){samples=Arrays.copyOf(samples,N);System.out.println("# real E2: 4095 original samples + 1 zero; one-frame detector check only");}
      for(int e=0;e<3;e++){
        ArrayList<Double> errors=new ArrayList<>();int invalid=0,octaves=0,total=0;long ns=0;
        for(int start=0;start+N<=samples.length;start+=2048){
          float[] x=Arrays.copyOfRange(samples,start,start+N);double[] d=new double[N];double p=0;for(int i=0;i<N;i++){d[i]=x[i];p+=d[i]*d[i];}
          long t=System.nanoTime();double hz=e==0?(Math.sqrt(p/N)<.012?0:old.getPitch(d)):engines[e-1].getPitch(x).getPitch();ns+=System.nanoTime()-t;total++;
          if(hz<=0||!Double.isFinite(hz)){invalid++;continue;}double err=Math.abs(1200*Math.log(hz/F[0])/Math.log(2));errors.add(err);if(err>1100)octaves++;
        }Collections.sort(errors);System.out.printf(Locale.ROOT,"%s,real-E2-nominal-reference,%d,%.3f,%.3f,%d,%d,%.3f%n",names[e],total,percentile(errors,.5),percentile(errors,.95),invalid,octaves,ns/1e6/total);
      }
    }
  }
  static double percentile(List<Double> x,double p){return x.isEmpty()?Double.NaN:x.get(Math.min(x.size()-1,(int)(p*x.size())));}
}
