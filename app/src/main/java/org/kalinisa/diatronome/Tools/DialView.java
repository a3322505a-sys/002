package org.kalinisa.diatronome.Tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** A speed control only. Audio scheduling never depends on drawing or gestures. */
public final class DialView extends View {
    public interface Listener { void onTempo(int bpm); }
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int tempo = 60;
    private Listener listener;
    private boolean dragging;
    private double lastAngle, accumulated;
    private int startTempo;
    public DialView(Context c) { super(c); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    public void setTempo(int bpm) { tempo=bpm; invalidate(); }
    public void setListener(Listener l) { listener=l; }
    private float r() { return Math.min(getWidth(),getHeight())*.455f; }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx=getWidth()/2f, cy=getHeight()/2f, r=r();
        for (int i=0;i<=105;i++) {
            double a=Math.toRadians(120+i*300.0/105);
            boolean major=i%15==0;
            p.setColor(i*2+30<=tempo?0xff3d8877:0xff4b5058); p.setStrokeWidth(major?2f:1.3f);
            float inner=r-(major?r*.155f:r*.105f);
            c.drawLine(cx+(float)Math.cos(a)*inner,cy+(float)Math.sin(a)*inner,
                cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,p);
        }
        int[] labels={30,75,135,195,240};
        p.setTextSize(ToolUi.dp(getContext(),12)); p.setTextAlign(Paint.Align.CENTER); p.setColor(ToolUi.MUTED);
        for(int n:labels) {
            double a=Math.toRadians(120+(n-30)*300.0/210);
            c.drawText(""+n,cx+(float)Math.cos(a)*r*.73f,cy+(float)Math.sin(a)*r*.73f+p.getTextSize()*.35f,p);
        }
        double a=Math.toRadians(120+(tempo-30)*300.0/210);
        float x=cx+(float)Math.cos(a)*r*.947f,y=cy+(float)Math.sin(a)*r*.947f;
        p.setColor(ToolUi.BG);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r*.086f,p);
        p.setColor(ToolUi.MINT);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(ToolUi.dp(getContext(),2));
        c.drawCircle(x,y,r*.086f,p);p.setStyle(Paint.Style.FILL);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        double x=e.getX()-getWidth()/2f,y=e.getY()-getHeight()/2f;
        switch(e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                double distance=Math.hypot(x,y);
                if(distance<r()*.64 || distance>r()*1.16)return false;
                dragging=true;lastAngle=Math.atan2(y,x);accumulated=0;startTempo=tempo;
                getParent().requestDisallowInterceptTouchEvent(true);return true;
            case MotionEvent.ACTION_MOVE:
                if(!dragging)return false;
                double angle=Math.atan2(y,x),delta=angle-lastAngle;
                if(delta>Math.PI)delta-=2*Math.PI;if(delta<-Math.PI)delta+=2*Math.PI;
                accumulated+=Math.toDegrees(delta);lastAngle=angle;
                int next=Math.max(30,Math.min(240,startTempo+(int)Math.round(accumulated*210/300)));
                if(listener!=null && next!=tempo)listener.onTempo(next);return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(!dragging)return false;dragging=false;getParent().requestDisallowInterceptTouchEvent(false);return true;
            default:return dragging;
        }
    }
}
