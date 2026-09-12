package org.kalinisa.diatronome.Tools;

/** Single musical definition. BPM always counts the main pulse, including dotted quarters in 6/8. */
public final class BeatConfig {
    public static final BeatConfig DEFAULT=new BeatConfig(4,4,1);
    public final int numerator,denominator,subdivision;
    private BeatConfig(int n,int d,int s){numerator=n;denominator=d;subdivision=s;}
    public static BeatConfig of(int n,int d,int s){
        if(n==6&&d==8&&s==3)return new BeatConfig(6,8,3);
        if(d==4&&n>=2&&n<=4&&s>=1&&s<=4)return new BeatConfig(n,d,s);
        return DEFAULT;
    }
    public int mainBeats(){return denominator==8?2:numerator;}
    public int ticks(){return mainBeats()*subdivision;}
    public int accent(int tick){return tick==0?4:tick%subdivision==0?3:2;}
    public String meter(){return numerator+"/"+denominator;}
    public String detail(){return denominator==8?"附点四分音符为一拍 · 两组各三下":subdivision==3?"四分音符为一拍 · 三连音":"四分音符为一拍 · 每拍 "+subdivision+" 下";}
    @Override public boolean equals(Object o){if(!(o instanceof BeatConfig))return false;BeatConfig b=(BeatConfig)o;return numerator==b.numerator&&denominator==b.denominator&&subdivision==b.subdivision;}
    @Override public int hashCode(){return numerator*100+denominator*10+subdivision;}
}
