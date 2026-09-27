package org.kalinisa.diatronome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import org.kalinisa.diatronome.Cores.TuneBeatCore;
import org.kalinisa.diatronome.Cores.MetronomePlaybackService;
import org.kalinisa.diatronome.Tools.*;

public class MainActivity extends AppCompatActivity {
    protected SharedPreferences prefs;
    protected LinearLayout root;
    protected FrameLayout body;
    protected TextView title;
    private TextView bpmText,meterText,meterDetail;
    private Button play;
    private DialView dial;
    private BeatDots dots;
    private ScrollView pageScroll;
    private RhythmScoreView scoreView;
    private boolean practicePage;
    private int lastPracticeBar=-1;
    private RhythmGenerator.Options practiceOptions=RhythmGenerator.Options.DEFAULT;
    protected TuneBeatCore core;
    private boolean compact;

    private final Handler messages=new Handler(Looper.getMainLooper()){
        @Override public void handleMessage(Message msg){
            if(msg.what==TuneBeatCore.HANDLER_MSG_TICK && dots!=null && msg.obj instanceof BeatSequence.Event){
                BeatSequence.Event e=(BeatSequence.Event)msg.obj;dots.setConfig(e.config);dots.setBeat(e.tick);
            }
            if(msg.what==TuneBeatCore.HANDLER_MSG_TICK && practicePage && scoreView!=null && msg.obj instanceof BeatSequence.Event){
                BeatSequence.Event e=(BeatSequence.Event)msg.obj;
                if(e.rhythmIndex>=0)showPracticePosition(e.rhythmIndex);
            }
            if(msg.what==TuneBeatCore.HANDLER_MSG_CONFIG)renderMeter();
            if(msg.what==TuneBeatCore.HANDLER_MSG_ERROR)Toast.makeText(MainActivity.this,"音频暂不可用，请重试",Toast.LENGTH_SHORT).show();
            if(msg.what==TuneBeatCore.HANDLER_MSG_PLAY)updatePlaying();
        }
    };
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("tunebeat",MODE_PRIVATE);
        compact=getResources().getConfiguration().screenHeightDp<700;
        Window w=getWindow();w.setStatusBarColor(ToolUi.BG);w.setNavigationBarColor(ToolUi.BG);
        WindowCompat.setDecorFitsSystemWindows(w,false);
        WindowCompat.getInsetsController(w,w.getDecorView()).setAppearanceLightStatusBars(false);
        WindowCompat.getInsetsController(w,w.getDecorView()).setAppearanceLightNavigationBars(false);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(ToolUi.BG);
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });
        title=ToolUi.text(this,"节拍器",22,ToolUi.TEXT);root.addView(title,new LinearLayout.LayoutParams(-1,dp(compact?48:64)));
        body=new FrameLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);setVolumeControlStream(AudioManager.STREAM_MUSIC);
        showInitialPage();
    }
    protected void showInitialPage(){
        // Retire obsolete tuner state without resetting tempo or meter preferences.
        prefs.edit().remove("tunerPage").remove("string").remove("microphoneAsked").apply();
        if(TuneBeatCore.getInstance().getRhythmScore()!=null)showPractice();else showMetronome();
    }

    protected int dp(float n){return ToolUi.dp(this,n);}
    protected LinearLayout page(){
        body.removeAllViews();
        ScrollView scroll=new ScrollView(this);pageScroll=scroll;scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(24),dp(compact?8:12),dp(24),dp(compact?8:16));
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));body.addView(scroll,new FrameLayout.LayoutParams(-1,-1));return content;
    }
    protected void ensureCore(){
        if(core==null){core=TuneBeatCore.getInstance();MetronomePlaybackService.configure(this,core);}
        core.setHandler(messages);
    }
    protected void showMetronome(){
        practicePage=false;scoreView=null;lastPracticeBar=-1;
        title.setText("节拍器");ensureCore();LinearLayout content=page();
        LinearLayout numbers=new LinearLayout(this);numbers.setGravity(Gravity.CENTER);numbers.setOrientation(LinearLayout.HORIZONTAL);
        Button minus=roundButton("−","速度减一");minus.setId(R.id.tempo_minus);numbers.addView(minus,new LinearLayout.LayoutParams(dp(52),dp(52)));
        LinearLayout readout=new LinearLayout(this);readout.setOrientation(LinearLayout.VERTICAL);readout.setGravity(Gravity.CENTER);
        bpmText=ToolUi.text(this,""+core.getTempoBpm(),compact?54:62,ToolUi.TEXT);bpmText.setId(R.id.tempo_value);bpmText.setContentDescription("输入每分钟拍数");bpmText.setOnClickListener(v->inputTempo());bpmText.setFocusable(true);
        readout.addView(bpmText,new LinearLayout.LayoutParams(-1,-2));readout.addView(ToolUi.text(this,"每分钟拍数",12,ToolUi.MUTED));
        numbers.addView(readout,new LinearLayout.LayoutParams(0,-2,1));
        Button plus=roundButton("+","速度加一");plus.setId(R.id.tempo_plus);numbers.addView(plus,new LinearLayout.LayoutParams(dp(52),dp(52)));
        minus.setOnClickListener(v->changeTempo(core.getTempoBpm()-1));plus.setOnClickListener(v->changeTempo(core.getTempoBpm()+1));
        content.addView(numbers,new LinearLayout.LayoutParams(-1,-2));
        int size=Math.min(dp(380),getResources().getDisplayMetrics().widthPixels-dp(48));
        if(compact)size=Math.min(size,dp(228));
        FrameLayout disk=new FrameLayout(this);LinearLayout.LayoutParams diskLp=new LinearLayout.LayoutParams(size,size);diskLp.topMargin=dp(compact?12:24);content.addView(disk,diskLp);
        dial=new DialView(this);dial.setId(R.id.tempo_dial);dial.setTempo(core.getTempoBpm());dial.setListener(this::changeTempo);disk.addView(dial,new FrameLayout.LayoutParams(-1,-1));
        play=roundButton("▶","开始节拍");play.setId(R.id.play_pause);play.setTextColor(ToolUi.MINT);play.setTextSize(32);play.setBackground(ToolUi.shape(ToolUi.BG,dp(64),0));
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(112),dp(112),Gravity.CENTER);disk.addView(play,p);play.setOnClickListener(v->togglePlayback());
        dots=new BeatDots(this);dots.setId(R.id.beat_dots);content.addView(dots,new LinearLayout.LayoutParams(-1,dp(compact?28:42)));
        meterText=ToolUi.text(this,"",22,ToolUi.TEXT);meterText.setId(R.id.meter_config);meterText.setFocusable(true);meterText.setOnClickListener(v->chooseMeter());LinearLayout.LayoutParams meterLp=new LinearLayout.LayoutParams(-1,dp(48));meterLp.topMargin=dp(compact?6:14);content.addView(meterText,meterLp);
        meterDetail=ToolUi.text(this,"",12,ToolUi.MUTED);content.addView(meterDetail);renderMeter();updatePlaying();
        Button practice=roundButton("节奏练习  →","打开连续节奏练习");practice.setTextSize(18);
        LinearLayout.LayoutParams practiceLp=new LinearLayout.LayoutParams(-1,dp(52));practiceLp.topMargin=dp(22);
        content.addView(practice,practiceLp);practice.setOnClickListener(v->loadRhythmExercise(RhythmGenerator.generate(practiceOptions)));
    }
    /** A caller can supply a validated 4/4 score via RhythmScore.of(...). */
    public void loadRhythmExercise(RhythmScore score){
        if(core==null)ensureCore();
        if(core.getIsPlaying())stopMetronome();
        core.loadRhythmExercise(score);
        if(score==null)showMetronome();else showPractice();
    }
    private void showPractice(){
        practicePage=true;lastPracticeBar=-1;dots=null;dial=null;meterText=null;meterDetail=null;
        title.setText("节奏练习");ensureCore();
        RhythmScore score=core.getRhythmScore();if(score==null){showMetronome();return;}
        LinearLayout content=page();
        content.setPadding(content.getPaddingLeft(),content.getPaddingTop(),content.getPaddingRight(),dp(105));
        TextView info=ToolUi.text(this,"4/4 · "+score.bars()+" 小节 · 播完自动循环",15,ToolUi.MUTED);
        content.addView(info,new LinearLayout.LayoutParams(-1,dp(40)));
        LinearLayout tempo=new LinearLayout(this);tempo.setGravity(Gravity.CENTER_VERTICAL);
        Button minus=roundButton("−","速度减一"),plus=roundButton("+","速度加一");
        tempo.addView(minus,new LinearLayout.LayoutParams(dp(46),dp(46)));
        bpmText=ToolUi.text(this,core.getTempoBpm()+" BPM  ▾",25,ToolUi.TEXT);bpmText.setId(R.id.tempo_value);
        tempo.addView(bpmText,new LinearLayout.LayoutParams(0,dp(54),1));
        tempo.addView(plus,new LinearLayout.LayoutParams(dp(46),dp(46)));
        minus.setOnClickListener(v->changeTempo(core.getTempoBpm()-1));plus.setOnClickListener(v->changeTempo(core.getTempoBpm()+1));
        bpmText.setOnClickListener(v->inputTempo());content.addView(tempo,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout choices=new LinearLayout(this);choices.setGravity(Gravity.CENTER_VERTICAL);
        Button difficulty=roundButton("难度："+difficultyName(),"选择音型范围");difficulty.setTextSize(14);
        Button theme=roundButton("主题："+themeName(),"选择练习主题");theme.setTextSize(14);
        choices.addView(difficulty,new LinearLayout.LayoutParams(0,dp(44),1));
        LinearLayout.LayoutParams themeLp=new LinearLayout.LayoutParams(0,dp(44),1);themeLp.leftMargin=dp(8);choices.addView(theme,themeLp);
        difficulty.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("难度").setSingleChoiceItems(
            new String[]{"基础 · 常规节奏","进阶 · 加入附点","综合 · 加入三连音"},practiceOptions.difficulty.ordinal(),(dialog,which)->{
                dialog.dismiss();practiceOptions=new RhythmGenerator.Options(RhythmGenerator.Difficulty.values()[which],practiceOptions.theme);
                loadRhythmExercise(RhythmGenerator.generate(practiceOptions));
            }).setNegativeButton("取消",null).show());
        theme.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("主题").setSingleChoiceItems(
            new String[]{"综合","附点专项","三连音专项"},practiceOptions.theme.ordinal(),(dialog,which)->{
                dialog.dismiss();practiceOptions=new RhythmGenerator.Options(practiceOptions.difficulty,RhythmGenerator.Theme.values()[which]);
                loadRhythmExercise(RhythmGenerator.generate(practiceOptions));
            }).setNegativeButton("取消",null).show());
        LinearLayout.LayoutParams choicesLp=new LinearLayout.LayoutParams(-1,-2);choicesLp.topMargin=dp(5);content.addView(choices,choicesLp);
        play=roundButton("▶","开始跟拍");play.setId(R.id.play_pause);play.setTextColor(ToolUi.MINT);play.setTextSize(28);
        play.setBackground(ToolUi.shape(ToolUi.PANEL,dp(48),ToolUi.MINT));
        FrameLayout.LayoutParams playLp=new FrameLayout.LayoutParams(dp(76),dp(76),Gravity.BOTTOM|Gravity.END);
        playLp.bottomMargin=dp(16);playLp.rightMargin=dp(16);
        body.addView(play,playLp);play.setOnClickListener(v->togglePlayback());
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);
        Button regenerate=roundButton("换一段","重新随机生成十二小节");regenerate.setTextSize(16);
        Button back=roundButton("返回节拍器","退出节奏练习");back.setTextSize(16);
        actions.addView(regenerate,new LinearLayout.LayoutParams(0,dp(48),1));
        LinearLayout.LayoutParams backLp=new LinearLayout.LayoutParams(0,dp(48),1);backLp.leftMargin=dp(8);actions.addView(back,backLp);
        regenerate.setOnClickListener(v->loadRhythmExercise(RhythmGenerator.generate(practiceOptions)));
        back.setOnClickListener(v->loadRhythmExercise(null));
        LinearLayout.LayoutParams actionsLp=new LinearLayout.LayoutParams(-1,-2);actionsLp.topMargin=dp(15);content.addView(actions,actionsLp);
        TextView guidance=ToolUi.text(this,"跟随高亮弹奏，休止时停下",13,ToolUi.MUTED);
        LinearLayout.LayoutParams guideLp=new LinearLayout.LayoutParams(-1,-2);guideLp.topMargin=dp(14);content.addView(guidance,guideLp);
        scoreView=new RhythmScoreView(this,score);scoreView.setId(R.id.rhythm_score);
        LinearLayout.LayoutParams scoreLp=new LinearLayout.LayoutParams(-1,-2);scoreLp.topMargin=dp(12);content.addView(scoreView,scoreLp);
        updatePlaying();if(core.getCurrentRhythmIndex()>=0)showPracticePosition(core.getCurrentRhythmIndex());
    }
    private void showPracticePosition(int index){
        RhythmScore score=core.getRhythmScore();if(score==null||index>=score.size())return;
        scoreView.setCurrentNote(index);
        int bar=score.note(index).bar;
        if(bar!=lastPracticeBar){
            pageScroll.smoothScrollTo(0,Math.max(0,scoreView.getTop()+scoreView.rowTopForBar(bar)-dp(175)));
        }
        lastPracticeBar=bar;
    }
    private String difficultyName(){return new String[]{"基础","进阶","综合"}[practiceOptions.difficulty.ordinal()];}
    private String themeName(){return new String[]{"综合","附点","三连音"}[practiceOptions.theme.ordinal()];}
    private void renderMeter(){
        if(core==null||meterText==null)return;
        BeatConfig current=core.getConfig(),pending=core.getRequestedConfig();
        if(bpmText!=null)bpmText.setText(""+core.getTempoBpm());if(dial!=null)dial.setTempo(core.getTempoBpm());
        meterText.setText(current.meter()+" ▾");
        meterDetail.setText(current.detail()+(current.equals(pending)?"":"\n下小节生效："+pending.meter()+" · "+pending.detail()));
        if(dots!=null)dots.setConfig(current);
    }
    private void chooseMeter(){
        BeatConfig requested=core.getRequestedConfig();String[] labels={"2/4","3/4","4/4","6/8"};
        new AlertDialog.Builder(this).setTitle("拍号").setSingleChoiceItems(labels,requested.denominator==8?3:requested.numerator-2,(dialog,which)->{
            dialog.dismiss();if(which==3){core.requestConfig(BeatConfig.of(6,8,3));return;}
            int n=which+2;String[] subdivisions={"每拍 1 下","每拍 2 下","每拍 3 连音","每拍 4 下"};
            new AlertDialog.Builder(this).setTitle(labels[which]+" · 四分音符为一拍").setSingleChoiceItems(subdivisions,requested.denominator==4?requested.subdivision-1:0,(d,i)->{core.requestConfig(BeatConfig.of(n,4,i+1));d.dismiss();}).setNegativeButton("取消",null).show();
        }).setNegativeButton("取消",null).show();
    }
    protected Button roundButton(String text,String description){
        Button b=new Button(this);b.setText(text);b.setTextSize(26);b.setTextColor(ToolUi.TEXT);b.setAllCaps(false);b.setPadding(0,0,0,0);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);
        b.setBackground(ToolUi.shape(ToolUi.BG,dp(32),0xff737b84));b.setContentDescription(description);return b;
    }
    private void inputTempo(){
        EditText input=new EditText(this);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setText(""+core.getTempoBpm());input.selectAll();input.setTextColor(ToolUi.TEXT);input.setPadding(dp(24),dp(14),dp(24),dp(14));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("每分钟拍数").setMessage("30–240 BPM").setView(input).setNegativeButton("取消",null).setPositiveButton("确定",null).create();
        dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{int n=Integer.parseInt(input.getText().toString().trim());if(n<30||n>240)throw new NumberFormatException();changeTempo(n);dialog.dismiss();}
            catch(NumberFormatException e){input.setError("请输入 30–240 的整数");}
        });});dialog.show();input.requestFocus();dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
    private void changeTempo(int value){
        int bpm=Math.max(30,Math.min(240,value));core.setTempoBpm(bpm);prefs.edit().putInt("bpm",bpm).apply();
        if(bpmText!=null)bpmText.setText(practicePage?bpm+" BPM  ▾":""+bpm);
        if(dial!=null)dial.setTempo(bpm);
    }
    protected void togglePlayback(){
        if(core.getIsPlaying())stopMetronome();
        else{
            if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED && !prefs.getBoolean("notificationAsked",false)){
                prefs.edit().putBoolean("notificationAsked",true).apply();requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},401);
            }
            Intent i=new Intent(this,MetronomePlaybackService.class).setAction(MetronomePlaybackService.PLAY);androidx.core.content.ContextCompat.startForegroundService(this,i);}
    }
    protected void stopMetronome(){
        if(core!=null)core.stop();stopService(new Intent(this,MetronomePlaybackService.class));updatePlaying();
    }
    private void updatePlaying(){
        if(play==null||core==null)return;renderMeter();boolean active=core.getIsPlaying();play.setText(active?"Ⅱ":"▶");play.setContentDescription(active?"暂停节拍":"开始节拍");if(!active&&dots!=null)dots.setBeat(-1);
        if(!active&&scoreView!=null){scoreView.setCurrentNote(-1);lastPracticeBar=-1;}
    }
    @Override public void onBackPressed(){if(practicePage)loadRhythmExercise(null);else super.onBackPressed();}
    @Override protected void onResume(){super.onResume();if(core!=null)core.setHandler(messages);updatePlaying();}
    @Override protected void onDestroy(){if(core!=null)core.setHandler(null);messages.removeCallbacksAndMessages(null);super.onDestroy();}
}
