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
    protected TuneBeatCore core;
    private boolean compact;

    private final Handler messages=new Handler(Looper.getMainLooper()){
        @Override public void handleMessage(Message msg){
            if(msg.what==TuneBeatCore.HANDLER_MSG_TICK && dots!=null && msg.obj instanceof BeatSequence.Event){
                BeatSequence.Event e=(BeatSequence.Event)msg.obj;dots.setConfig(e.config);dots.setBeat(e.tick);
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
        showMetronome();
    }

    protected int dp(float n){return ToolUi.dp(this,n);}
    protected LinearLayout page(){
        body.removeAllViews();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(24),dp(compact?8:12),dp(24),dp(compact?8:16));
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));body.addView(scroll,new FrameLayout.LayoutParams(-1,-1));return content;
    }
    protected void ensureCore(){
        if(core==null){core=TuneBeatCore.getInstance();MetronomePlaybackService.configure(this,core);}
        core.setHandler(messages);
    }
    protected void showMetronome(){
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
    }
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
        int bpm=Math.max(30,Math.min(240,value));core.setTempoBpm(bpm);prefs.edit().putInt("bpm",bpm).apply();bpmText.setText(""+bpm);dial.setTempo(bpm);
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
    }
    @Override protected void onResume(){super.onResume();if(core!=null)core.setHandler(messages);updatePlaying();}
    @Override protected void onDestroy(){if(core!=null)core.setHandler(null);messages.removeCallbacksAndMessages(null);super.onDestroy();}
}
