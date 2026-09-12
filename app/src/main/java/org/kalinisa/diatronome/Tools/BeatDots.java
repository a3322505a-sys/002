package org.kalinisa.diatronome.Tools;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
public final class BeatDots extends View {
    private int beat=-1;
    private BeatConfig config=BeatConfig.DEFAULT;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    public BeatDots(Context c){super(c);setConfig(config);}
    public void setConfig(BeatConfig c){config=c;setContentDescription(c.meter()+"，"+c.detail());invalidate();}
    public void setBeat(int n){beat=n;invalidate();}
    @Override protected void onDraw(Canvas c){
        int count=config.ticks();float space=Math.min(ToolUi.dp(getContext(),28),(getWidth()-ToolUi.dp(getContext(),20))/(float)count),cx=getWidth()/2f;
        for(int i=0;i<count;i++){
            p.setColor(i==beat?ToolUi.MINT:0xff606771);
            float size=config.accent(i)==4?6:config.accent(i)==3?4.5f:2.5f;
            c.drawCircle(cx+(i-(count-1)/2f)*space,getHeight()/2f,ToolUi.dp(getContext(),size),p);
        }
    }
}
