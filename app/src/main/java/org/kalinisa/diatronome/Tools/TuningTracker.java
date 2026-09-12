package org.kalinisa.diatronome.Tools;
import java.util.Arrays;

/** Short continuous evidence in cents; invalid frames never preserve an in-tune verdict. */
public final class TuningTracker {
    public static final class Reading {
        public final int string;public final double hz,cents;public final boolean locked,inTune;
        public final String status;
        Reading(int s,double h,boolean l,boolean tune,String text){string=s;hz=h;locked=l;inTune=tune;cents=s<0?Double.NaN:TuningMath.cents(h,s);status=text;}
    }
    private int locked=-1,selected=-1,candidate=-1,count;
    private final double[] recent=new double[3];
    private long lastTime=-1;
    private int tuneCount;
    public void lock(int s){if(s<0||s>=6)throw new IllegalArgumentException();locked=s;reset();}
    public void automatic(){locked=-1;reset();}
    public boolean isLocked(){return locked>=0;}
    public void reset(){selected=locked;candidate=-1;count=0;tuneCount=0;lastTime=-1;}
    public Reading waiting(String text){return new Reading(locked>=0?locked:-1,0,isLocked(),false,text);}
    public Reading accept(GuitarPitchDetector.Result r,long now){
        if(lastTime>=0&&now-lastTime>400)reset();lastTime=now;
        if(!r.valid()) {count=0;candidate=-1;tuneCount=0;if(r.quiet&&locked<0)selected=-1;return waiting(r.quiet?"拨动琴弦":"信号不稳 · 请单独拨弦");}
        int next=locked>=0?locked:TuningMath.nearestString(r.hz);
        if(locked<0&&Math.abs(TuningMath.cents(r.hz,next))>100){count=0;tuneCount=0;return waiting("偏差较大 · 请锁定琴弦");}
        // Keep the current string around a midpoint until the alternative is at least 30 cents closer.
        if(locked<0&&selected>=0&&next!=selected&&Math.abs(TuningMath.cents(r.hz,selected))<Math.abs(TuningMath.cents(r.hz,next))+30)next=selected;
        if(next!=candidate || (count>0&&Math.abs(1200*Math.log(r.hz/recent[(count-1)%3])/Math.log(2))>30)){
            candidate=next;count=0;tuneCount=0;
        }
        recent[count%3]=r.hz;count++;
        if(count<3)return waiting("正在识别");
        double[] sorted=recent.clone();Arrays.sort(sorted);double hz=sorted[1];
        double spread=1200*Math.log(sorted[2]/sorted[0])/Math.log(2);
        if(spread>12){tuneCount=0;return waiting("信号不稳 · 请单独拨弦");}
        if(selected!=next){selected=next;tuneCount=0;}
        double cents=TuningMath.cents(hz,selected);
        // Conservative entry at 3 cents; a confirmed state exits outside 5 cents.
        if(Math.abs(cents)<=3||(tuneCount>=2&&Math.abs(cents)<=5))tuneCount++;else tuneCount=0;
        boolean tuned=tuneCount>=2;
        String status=Math.abs(cents)>100?"偏差较大 · 核对弦与八度":tuned?"已调准":Math.abs(cents)<=5?"接近调准 · 稳定中":cents>0?"偏高 · 调低":"偏低 · 调高";
        return new Reading(selected,hz,isLocked(),tuned,status);
    }
}
