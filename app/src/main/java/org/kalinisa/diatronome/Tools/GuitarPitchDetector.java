package org.kalinisa.diatronome.Tools;
import be.tarsos.dsp.pitch.McLeodPitchMethod;
import be.tarsos.dsp.pitch.PitchDetectionResult;

/** PCM16 -> measured pitch. Does not know the selected string or fold octaves. */
public final class GuitarPitchDetector {
    public static final int RATE=44100,SAMPLES=4096,HOP=2048;
    public static final class Result {
        public final double hz,confidence,rms;
        public final boolean quiet;
        public Result(double h,double c,double r,boolean q){hz=h;confidence=c;rms=r;quiet=q;}
        public boolean valid(){return (!Double.isNaN(hz)&&!Double.isInfinite(hz))&&hz>=40&&hz<=700&&confidence>=.95;}
    }
    private final McLeodPitchMethod detector=new McLeodPitchMethod(RATE,SAMPLES);
    private final float[] signal=new float[SAMPLES];
    private double noise=.00015;
    public Result detect(short[] pcm){
        if(pcm.length!=SAMPLES)throw new IllegalArgumentException("4096 PCM16 samples required");
        double mean=0,power=0;int clipped=0;
        for(short v:pcm){mean+=v/32768.0;if(Math.abs((int)v)>32500)clipped++;}mean/=SAMPLES;
        for(int i=0;i<SAMPLES;i++){signal[i]=(float)(pcm[i]/32768.0-mean);power+=signal[i]*signal[i];}
        double rms=Math.sqrt(power/SAMPLES),gate=Math.max(.0004,Math.min(.003,noise*2.5));
        if(rms<gate){noise=.95*noise+.05*Math.min(rms,.0012);return new Result(0,0,rms,true);}
        if(clipped>SAMPLES/50)return new Result(0,0,rms,false);
        PitchDetectionResult pitch=detector.getPitch(signal);
        Result r=new Result(pitch.getPitch(),pitch.getProbability(),rms,false);
        if(!r.valid())noise=.98*noise+.02*Math.min(rms,.0012);
        return r;
    }
}
