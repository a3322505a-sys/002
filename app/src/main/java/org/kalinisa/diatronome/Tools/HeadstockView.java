package org.kalinisa.diatronome.Tools;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

/** 1961 Strat front reference: docs/V02-sources.md. Geometry and hit regions share coordinates. */
public final class HeadstockView extends View {
    public interface Listener{void selected(int string);}
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path head=new Path();
    private int selected=-1;
    private boolean locked,valid,tuned;
    private int down=-1;
    private Listener listener;
    private float scale,dx;
    private final float[] x={207,192,177,162,147,132},y={62,146,230,314,398,482};
    private final Path[] grain=new Path[34];
    public HeadstockView(Context c){
        super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        // Small pre-CBS Strat outline: sloping tuner edge, rounded tip, concave treble waist.
        head.moveTo(123,555);head.cubicTo(125,526,114,516,103,505);
        head.lineTo(190,70);head.cubicTo(195,41,210,20,237,20);
        head.cubicTo(277,17,305,38,297,72);head.cubicTo(295,94,268,110,246,117);
        head.cubicTo(219,127,217,154,226,178);head.cubicTo(246,220,253,276,253,321);
        head.lineTo(252,442);head.cubicTo(251,475,226,492,224,555);head.close();
        Random rand=new Random(22);
        for(int i=0;i<grain.length;i++){Path line=new Path();float base=100+i*5;line.moveTo(base,25);line.cubicTo(base-20+rand.nextFloat()*20,150,base+22,320,base-18,510);grain[i]=line;}
    }
    public void setSelected(int s){selected=s;invalidate();}
    public void setState(int s,boolean l,boolean v,boolean t){selected=s;locked=l;valid=v;tuned=t;invalidate();}
    public void setListener(Listener l){listener=l;}
    @Override protected void onDraw(Canvas c){
        float h=getHeight();scale=Math.min(getWidth()/320f,h/640f);dx=(getWidth()-320*scale)/2;c.save();c.translate(dx,0);c.scale(scale,scale);
        // Neck and frets, under the headstock silhouette.
        p.setShader(new LinearGradient(123,0,224,0,new int[]{0xff38291f,0xff64513e,0xff30241e},null,Shader.TileMode.CLAMP));c.drawRect(123,548,224,640,p);p.setShader(null);
        p.setColor(0xffaab0b0);c.drawRect(123,608,224,612,p);p.setColor(0xfff0f0e9);c.drawRect(123,608,224,609,p);
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
            if(i==selected){p.setColor(valid?ToolUi.MINT:0xffecaa7c);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.7f);if(locked)c.drawRoundRect(px-23,py-23,px+23,py+23,5,5,p);else c.drawCircle(px,py,22,p);p.setStyle(Paint.Style.FILL);}
        }
        // Nut, string retainer and six continuous strings (the selected one uses mint).
        p.setShader(new LinearGradient(0,545,0,555,0xfff2ece0,0xffa6a49a,Shader.TileMode.CLAMP));c.drawRect(123,545,224,555,p);p.setShader(null);
        for(int i=5;i>=0;i--){
            float nut=216-i*16.5f;
            p.setColor(0x66000000);p.setStrokeWidth(1.4f+i*.27f);c.drawLine(x[i]+1,y[i],nut+1,550,p);c.drawLine(nut+1,550,nut+1,640,p);
            p.setColor(i==selected?(valid?ToolUi.MINT:0xffecaa7c):0xffc8cece);p.setStrokeWidth(i==selected?2.4f:1.1f+i*.21f);c.drawLine(x[i],y[i],nut,550,p);c.drawLine(nut,550,nut,640,p);
        }
        // Butterfly retainer spans the first and second string paths.
        p.setColor(0xffa0a8aa);c.drawRoundRect(200,365,217,376,2,2,p);p.setColor(0xff323a3f);c.drawCircle(209,370,3,p);
        p.setColor(0xff4c3520);p.setTextSize(14);p.setTypeface(Typeface.create(Typeface.SERIF,Typeface.ITALIC));
        c.save();c.rotate(-78,230,370);c.drawText("Stratocaster",230,370,p);c.restore();p.setTypeface(Typeface.DEFAULT);
        c.restore();
    }
    // At 390dp height the 84-unit spacing is over 48dp. Nearest-center partition avoids overlaps.
    public int hit(float viewX,float viewY){
        float scale=Math.min(getWidth()/320f,getHeight()/640f),dx=(getWidth()-320*scale)/2;
        if(scale<=0)return -1;float radius=ToolUi.dp(getContext(),24);
        int best=-1;double distance=Double.MAX_VALUE;
        for(int i=0;i<6;i++){
            float cx=dx+(x[i]-16)*scale,cy=y[i]*scale;
            if(Math.abs(viewY-cy)<=radius&&viewX>=cx-radius&&viewX<=cx+radius+16*scale){
                double d=Math.hypot(viewX-cx,viewY-cy);if(d<distance){distance=d;best=i;}
            }
        }return best;
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){down=hit(e.getX(),e.getY());return down>=0;}
        if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){down=-1;return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP){int index=hit(e.getX(),e.getY());if(index==down&&index>=0){if(listener!=null)listener.selected(index);performClick();}down=-1;return true;}
        return down>=0;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
