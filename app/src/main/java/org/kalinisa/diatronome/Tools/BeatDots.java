package org.kalinisa.diatronome.Tools;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
public final class BeatDots extends View {
    private int beat=-1;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    public BeatDots(Context c){super(c);setContentDescription("四拍节奏，第一拍重音");}
    public void setBeat(int n){beat=n;invalidate();}
    @Override protected void onDraw(Canvas c){
        float space=ToolUi.dp(getContext(),28),cx=getWidth()/2f;
        for(int i=0;i<4;i++){
            p.setColor(i==beat?ToolUi.MINT:0xff606771);
            c.drawCircle(cx+(i-1.5f)*space,getHeight()/2f,ToolUi.dp(getContext(),i==0?6:4.5f),p);
        }
    }
}
