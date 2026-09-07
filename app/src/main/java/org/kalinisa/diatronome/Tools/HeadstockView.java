package org.kalinisa.diatronome.Tools;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

/** Original static vector artwork. Geometry and all six highlight overlays share coordinates. */
public final class HeadstockView extends View {
    public interface Listener{void selected(int string);}
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path head=new Path();
    private int selected=5;
    private Listener listener;
    private float scale,dx;
    private final float[] x={207,192,177,162,147,132},y={100,164,228,292,356,420};
    private final Path[] grain=new Path[34];
    public HeadstockView(Context c){
        super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        head.moveTo(123,503);head.cubicTo(124,466,111,454,91,437);head.lineTo(181,101);
        head.cubicTo(192,55,210,31,243,33);head.cubicTo(292,33,313,74,289,111);
        head.cubicTo(269,143,258,178,257,227);head.lineTo(257,425);head.cubicTo(234,449,225,468,224,503);head.close();
        Random rand=new Random(22);
        for(int i=0;i<grain.length;i++){Path line=new Path();float base=100+i*5;line.moveTo(base,25);line.cubicTo(base-20+rand.nextFloat()*20,150,base+22,320,base-18,510);grain[i]=line;}
    }
    public void setSelected(int s){selected=s;invalidate();}
    public void setListener(Listener l){listener=l;}
    @Override protected void onDraw(Canvas c){
        float h=getHeight();scale=Math.min(getWidth()/320f,h/610f);dx=(getWidth()-320*scale)/2;c.save();c.translate(dx,0);c.scale(scale,scale);
        // Neck and frets, under the headstock silhouette.
        p.setShader(new LinearGradient(123,0,224,0,new int[]{0xff38291f,0xff64513e,0xff30241e},null,Shader.TileMode.CLAMP));c.drawRect(123,498,224,610,p);p.setShader(null);
        p.setColor(0xffaab0b0);c.drawRect(123,558,224,562,p);p.setColor(0xfff0f0e9);c.drawRect(123,558,224,559,p);
        p.setShadowLayer(8,4,5,0x99000000);p.setColor(0xff8c653a);c.drawPath(head,p);p.clearShadowLayer();
        p.setShader(new LinearGradient(110,0,285,0,new int[]{0xffb98449,0xffe4bd7a,0xfff2d6a0,0xffcca268},new float[]{0,.3f,.65f,1},Shader.TileMode.CLAMP));c.drawPath(head,p);p.setShader(null);
        c.save();c.clipPath(head);p.setColor(0x1b936330);p.setStrokeWidth(.65f);p.setStyle(Paint.Style.STROKE);for(Path g:grain)c.drawPath(g,p);c.restore();
        p.setColor(0xffe9c58b);p.setStrokeWidth(1.4f);c.drawPath(head,p);p.setStyle(Paint.Style.FILL);
        // Six inline chrome tuning keys. First string is highest and thinnest.
        for(int i=0;i<6;i++){
            float px=x[i],py=y[i];
            p.setShader(new LinearGradient(px-50,py-18,px-24,py+18,new int[]{0xff151b1e,0xffd7dee1,0xff59676e,0xffeff4f5,0xff343d41},new float[]{0,.28f,.5f,.67f,1},Shader.TileMode.CLAMP));
            c.drawRoundRect(px-49,py-18,px-26,py+17,5,5,p);p.setShader(null);p.setColor(0xff7b8589);c.drawRect(px-27,py-4,px-15,py+4,p);
            p.setShader(new RadialGradient(px-5,py-6,22,new int[]{0xfff5f8f8,0xff46525a,0xffe6ebed,0xff727e84,0xff11191d},new float[]{0,.33f,.55f,.78f,1},Shader.TileMode.CLAMP));c.drawCircle(px,py,19,p);p.setShader(null);
            p.setColor(0xffc1cacc);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);c.drawCircle(px,py,17,p);p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(px-6,py-6,px+7,py+7,0xffedf0ef,0xff303a3d,Shader.TileMode.CLAMP));c.drawCircle(px,py,7,p);p.setShader(null);
            if(i==selected){p.setColor(ToolUi.MINT);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.7f);c.drawCircle(px,py,22,p);p.setStyle(Paint.Style.FILL);}
        }
        // Nut, string retainer and six continuous strings (the selected one uses mint).
        p.setShader(new LinearGradient(0,493,0,503,0xfff2ece0,0xffa6a49a,Shader.TileMode.CLAMP));c.drawRect(123,493,224,503,p);p.setShader(null);
        for(int i=5;i>=0;i--){
            float nut=216-i*16.5f;
            p.setColor(0x66000000);p.setStrokeWidth(1.4f+i*.27f);c.drawLine(x[i]+1,y[i],nut+1,498,p);c.drawLine(nut+1,498,nut+1,610,p);
            p.setColor(i==selected?ToolUi.MINT:0xffc8cece);p.setStrokeWidth(i==selected?2.4f:1.1f+i*.21f);c.drawLine(x[i],y[i],nut,498,p);c.drawLine(nut,498,nut,610,p);
        }
        p.setColor(0xffa0a8aa);c.drawRoundRect(193,265,217,275,2,2,p);p.setColor(0xff323a3f);c.drawCircle(205,270,3,p);
        c.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_UP){float px=(e.getX()-dx)/scale,py=e.getY()/scale;
            for(int i=0;i<6;i++)if(Math.hypot(px-x[i],py-y[i])<29){if(listener!=null)listener.selected(i);performClick();return true;}
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
