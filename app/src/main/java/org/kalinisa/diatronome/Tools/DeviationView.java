package org.kalinisa.diatronome.Tools;
import android.content.Context;
import android.graphics.*;
import android.view.View;
public final class DeviationView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private double cents=Double.NaN;
    public DeviationView(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    public void setCents(double value){cents=value;invalidate();}
    @Override protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight(),cx=w/2;
        p.setStrokeWidth(1);p.setColor(0xff2a2e32);
        for(int i=0;i<=20;i++){float x=w*i/20;c.drawLine(x,h*.3f,x,h*.92f,p);}
        for(int i=0;i<4;i++)c.drawLine(0,h*(.36f+i*.17f),w,h*(.36f+i*.17f),p);
        p.setColor(0xff667069);p.setStrokeWidth(2);c.drawLine(cx,h*.24f,cx,h*.95f,p);
        p.setTextSize(ToolUi.dp(getContext(),11));p.setTextAlign(Paint.Align.CENTER);p.setColor(ToolUi.MUTED);
        c.drawText("−50",w*.12f,h*.96f,p);c.drawText("0",cx,h*.96f,p);c.drawText("+50",w*.88f,h*.96f,p);
        if(Double.isNaN(cents))return;
        float x=cx+(float)Math.max(-50,Math.min(50,cents))*w*.0076f;
        int color=Math.abs(cents)<=5?ToolUi.MINT:0xffecaa7c;
        p.setColor(color);p.setStrokeWidth(ToolUi.dp(getContext(),2));c.drawLine(x,h*.32f,x,h*.76f,p);
        float r=ToolUi.dp(getContext(),22),cy=Math.max(r+1,h*.25f);p.setColor(ToolUi.PANEL);c.drawCircle(x,cy,r,p);
        p.setColor(color);p.setStyle(Paint.Style.STROKE);c.drawCircle(x,cy,r,p);p.setStyle(Paint.Style.FILL);
        Path tip=new Path();tip.moveTo(x-5,cy+r);tip.lineTo(x+5,cy+r);tip.lineTo(x,cy+r+7);tip.close();c.drawPath(tip,p);
        p.setColor(ToolUi.TEXT);p.setTextSize(ToolUi.dp(getContext(),13));c.drawText(String.format(java.util.Locale.ROOT,"%+.0f",cents),x,cy+p.getTextSize()*.34f,p);
    }
}
