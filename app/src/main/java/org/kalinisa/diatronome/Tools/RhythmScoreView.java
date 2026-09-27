package org.kalinisa.diatronome.Tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** One bar per line, with a shared twelve-unit beat coordinate. */
public final class RhythmScoreView extends View {
    private final RhythmScore score;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int rowHeight;
    private int current=-1;
    public RhythmScoreView(Context context,RhythmScore score){
        super(context);this.score=score;rowHeight=dp(114);
        setContentDescription(score.bars()+" 小节节奏谱，跟随高亮弹奏，休止时停下");
    }
    private int dp(float n){return ToolUi.dp(getContext(),n);}
    private void ink(int c,float width){p.setColor(c);p.setStrokeWidth(dp(width));p.setStyle(Paint.Style.FILL);}
    private float pos(int units,float left,float span){return left+span*units/48f;}
    public void setCurrentNote(int index){current=index;invalidate();}
    public int rowTopForBar(int bar){return bar*rowHeight;}
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),score.bars()*rowHeight);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float left=dp(47),right=getWidth()-dp(18),span=right-left;
        for(int bar=0;bar<score.bars();bar++){
            float top=bar*rowHeight,y=top+dp(68);
            ink(ToolUi.MUTED,1);p.setTextSize(dp(11));c.drawText(""+(bar+1),dp(6),top+dp(22),p);
            if(bar==0){p.setTextSize(dp(15));c.drawText("4",dp(25),y-dp(9),p);c.drawText("4",dp(25),y+dp(8),p);}
            ink(0xff737b84,1);c.drawLine(left-dp(9),y,right,y,p);
            ink(0xffadb4ba,1.5f);c.drawLine(right,y-dp(29),right,y+dp(13),p);
            for(int beat=1;beat<4;beat++){ink(0xff626b72,1);c.drawCircle(pos(beat*12,left,span),y+dp(19),dp(1.6f),p);}
            int first=-1,last=-1;
            for(int i=0;i<score.size();i++)if(score.note(i).bar==bar){if(first<0)first=i;last=i;}
            for(int i=first;i>=0&&i<=last;i++){
                RhythmScore.Note n=score.note(i);float x=pos(n.startUnit,left,span);
                if(i==current){ink(0x5530d9ae,1);c.drawRoundRect(new RectF(x-dp(7),y-dp(47),
                    Math.min(right,pos(n.startUnit+n.kind.units,left,span)-dp(2)),y+dp(23)),dp(6),dp(6),p);}
                int color=i==current?ToolUi.MINT:ToolUi.TEXT;
                if(n.kind.rest)rest(c,x,y,n.kind,color);else note(c,x,y,n.kind,color,beamed(i,first,last));
            }
            for(int i=first;i>=0&&i<=last;){
                RhythmScore.Note n=score.note(i);
                int count=n.kind==RhythmScore.Kind.TRIPLET_EIGHTH?3:n.kind==RhythmScore.Kind.EIGHTH?2:0;
                if(count>0&&n.startUnit%12==0&&i+count-1<=last){
                    boolean valid=true;
                    for(int j=0;j<count;j++){
                        RhythmScore.Note m=score.note(i+j);
                        if(m.kind!=n.kind||m.startUnit!=n.startUnit+j*n.kind.units)valid=false;
                    }
                    if(valid){
                        float a=pos(n.startUnit,left,span)+dp(5);
                        float b=pos(n.startUnit+12-n.kind.units,left,span)+dp(5);
                        ink(ToolUi.TEXT,3);c.drawLine(a,y-dp(28),b,y-dp(28),p);
                        if(count==3){ink(ToolUi.TEXT,1);p.setTextSize(dp(12));p.setTextAlign(Paint.Align.CENTER);
                            c.drawText("3",(a+b)/2,y-dp(35),p);p.setTextAlign(Paint.Align.LEFT);}
                        i+=count;continue;
                    }
                }
                i++;
            }
        }
    }
    private boolean beamed(int i,int first,int last){
        RhythmScore.Note n=score.note(i);
        if(n.kind==RhythmScore.Kind.TRIPLET_EIGHTH)return true;
        if(n.kind!=RhythmScore.Kind.EIGHTH)return false;
        if(n.startUnit%12==0)return i+1<=last&&score.note(i+1).kind==n.kind;
        return n.startUnit%12==6&&i>first&&score.note(i-1).kind==n.kind
            &&score.note(i-1).startUnit==n.startUnit-6;
    }
    private void note(Canvas c,float x,float y,RhythmScore.Kind kind,int color,boolean beamed){
        ink(color,2);p.setStyle(kind==RhythmScore.Kind.HALF?Paint.Style.STROKE:Paint.Style.FILL);
        c.save();c.rotate(-24,x,y);
        c.drawOval(new RectF(x-dp(5.7f),y-dp(4),x+dp(5.7f),y+dp(4)),p);c.restore();
        ink(color,1.8f);float stem=x+dp(5);c.drawLine(stem,y,stem,y-dp(28),p);
        if(kind==RhythmScore.Kind.EIGHTH&&!beamed){
            Path flag=new Path();flag.moveTo(stem,y-dp(28));
            flag.cubicTo(stem+dp(12),y-dp(27),stem+dp(12),y-dp(18),stem+dp(5),y-dp(13));
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.3f));c.drawPath(flag,p);p.setStyle(Paint.Style.FILL);
        }
        if(kind==RhythmScore.Kind.DOTTED_QUARTER)c.drawCircle(x+dp(11),y-dp(1),dp(1.8f),p);
    }
    private void rest(Canvas c,float x,float y,RhythmScore.Kind kind,int color){
        ink(color,2.5f);Path path=new Path();
        if(kind==RhythmScore.Kind.QUARTER_REST){
            path.moveTo(x+dp(1),y-dp(21));path.lineTo(x-dp(3),y-dp(14));
            path.lineTo(x+dp(3),y-dp(7));path.lineTo(x-dp(2),y-dp(2));
            path.cubicTo(x-dp(10),y-dp(7),x-dp(10),y+dp(5),x-dp(3),y+dp(7));
        }else{
            c.drawCircle(x-dp(4),y-dp(15),dp(3.3f),p);
            path.moveTo(x-dp(2),y-dp(14));path.quadTo(x+dp(2),y-dp(12),x+dp(5),y-dp(17));
            path.moveTo(x+dp(5),y-dp(20));path.lineTo(x-dp(1),y+dp(6));
        }
        p.setStyle(Paint.Style.STROKE);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
    }
}
