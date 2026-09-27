package org.kalinisa.diatronome.Tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Single-line rhythm notation with one full-width bar per row. */
public final class RhythmScoreView extends View {
    private final RhythmScore score;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int current = -1;
    private final int rowHeight;

    public RhythmScoreView(Context context, RhythmScore score) {
        super(context);this.score=score;rowHeight=ToolUi.dp(context,118);
        setContentDescription(score.bars()+" 小节节奏谱，跟随节拍高亮当前位置");
    }
    public void setCurrentNote(int index) { current=index;invalidate(); }
    public int rowTopForBar(int bar) { return bar*rowHeight; }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec),score.bars()*rowHeight);
    }
    private void color(int c,float size) {paint.setColor(c);paint.setTextSize(ToolUi.dp(getContext(),size));paint.setStyle(Paint.Style.FILL);paint.setStrokeWidth(ToolUi.dp(getContext(),2));}
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float margin=ToolUi.dp(getContext(),7);
        float width=getWidth()-2*margin;
        for(int bar=0;bar<score.bars();bar++){
            float left=margin,top=bar*rowHeight;
            float line=top+ToolUi.dp(getContext(),68),start=left+ToolUi.dp(getContext(),18),end=left+width-ToolUi.dp(getContext(),8);
            color(ToolUi.MUTED,11);canvas.drawText(String.valueOf(bar+1),left+ToolUi.dp(getContext(),3),top+ToolUi.dp(getContext(),23),paint);
            paint.setColor(0xff626b72);paint.setStrokeWidth(ToolUi.dp(getContext(),1));
            canvas.drawLine(start,line,end,line,paint);canvas.drawLine(end,line-ToolUi.dp(getContext(),25),end,line+ToolUi.dp(getContext(),12),paint);
            for(int beat=1;beat<4;beat++){
                float x=start+(end-start)*beat/4f;paint.setColor(0xff454b50);
                canvas.drawLine(x,line-ToolUi.dp(getContext(),17),x,line+ToolUi.dp(getContext(),18),paint);
            }
            for(int i=0;i<score.size();i++){
                RhythmScore.Note n=score.note(i);if(n.bar!=bar)continue;
                float x=start+(end-start)*(n.startUnit+1.5f)/RhythmScore.UNITS_PER_BAR;
                float length=(end-start)*n.kind.units/RhythmScore.UNITS_PER_BAR;
                if(i==current){
                    color(0x5530d9ae,11);
                    canvas.drawRoundRect(new RectF(x-ToolUi.dp(getContext(),8),line-ToolUi.dp(getContext(),37),
                        Math.min(end,x+length-ToolUi.dp(getContext(),2)),line+ToolUi.dp(getContext(),38)),ToolUi.dp(getContext(),8),ToolUi.dp(getContext(),8),paint);
                }
                int ink=i==current?ToolUi.MINT:ToolUi.TEXT;
                if(n.kind.rest){
                    color(ink,17);canvas.drawText("休",x-ToolUi.dp(getContext(),8),line+ToolUi.dp(getContext(),5),paint);
                }else{
                    color(ink,11);
                    if(n.kind==RhythmScore.Kind.HALF){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(ToolUi.dp(getContext(),2));}
                    canvas.save();canvas.rotate(-24,x,line);
                    canvas.drawOval(new RectF(x-ToolUi.dp(getContext(),6),line-ToolUi.dp(getContext(),4),x+ToolUi.dp(getContext(),6),line+ToolUi.dp(getContext(),4)),paint);
                    canvas.restore();paint.setStyle(Paint.Style.FILL);
                    float stemX=x+ToolUi.dp(getContext(),5);
                    canvas.drawLine(stemX,line,stemX,line-ToolUi.dp(getContext(),27),paint);
                    if(n.kind==RhythmScore.Kind.EIGHTH||n.kind==RhythmScore.Kind.TRIPLET_EIGHTH)
                        canvas.drawLine(stemX,line-ToolUi.dp(getContext(),27),stemX+ToolUi.dp(getContext(),7),line-ToolUi.dp(getContext(),17),paint);
                    if(n.kind==RhythmScore.Kind.DOTTED_QUARTER)canvas.drawCircle(x+ToolUi.dp(getContext(),11),line-ToolUi.dp(getContext(),1),ToolUi.dp(getContext(),1.7f),paint);
                    if(n.kind==RhythmScore.Kind.TRIPLET_EIGHTH&&n.startUnit%RhythmScore.UNITS_PER_BEAT==0){
                        color(ink,11);canvas.drawText("3",x+ToolUi.dp(getContext(),5),line-ToolUi.dp(getContext(),33),paint);
                    }
                }
                String label=n.kind==RhythmScore.Kind.HALF?"二分":n.kind==RhythmScore.Kind.DOTTED_QUARTER?"附点":n.kind.rest?"休":"";
                if(!label.isEmpty()){
                    color(i==current?ToolUi.MINT:ToolUi.MUTED,9);
                    canvas.drawText(label,x-ToolUi.dp(getContext(),9),line+ToolUi.dp(getContext(),31),paint);
                }
            }
        }
    }
}
