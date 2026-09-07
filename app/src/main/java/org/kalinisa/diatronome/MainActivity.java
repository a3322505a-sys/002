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
import org.kalinisa.diatronome.Cores.MetronomeCore;
import org.kalinisa.diatronome.Cores.MetronomePlaybackService;
import org.kalinisa.diatronome.Tools.*;

public class MainActivity extends AppCompatActivity {
    protected SharedPreferences prefs;
    protected LinearLayout root;
    protected FrameLayout body;
    protected TextView title;
    private TextView bpmText;
    private Button play;
    private DialView dial;
    private BeatDots dots;
    protected MetronomeCore core;
    private boolean tunerPage, resumed;
    private int selectedString=5;
    private Button tunerTab,metronomeTab,microphone;
    private final Button[] strings=new Button[6];
    private TextView target,status;
    private HeadstockView headstock;
    private DeviationView deviation;
    private TunerCapture capture;
    private final Handler tuningUi=new Handler(Looper.getMainLooper());
    private final Runnable stalePitch=()->renderPitch(0);

    private final Handler messages=new Handler(Looper.getMainLooper()){
        @Override public void handleMessage(Message msg){
            if(msg.what==MetronomeCore.HANDLER_MSG_TICK && dots!=null)dots.setBeat(msg.arg1);
            if(msg.what==MetronomeCore.HANDLER_MSG_PLAY)updatePlaying();
        }
    };
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("tunebeat",MODE_PRIVATE);
        Window w=getWindow();w.setStatusBarColor(ToolUi.BG);w.setNavigationBarColor(ToolUi.BG);
        WindowCompat.setDecorFitsSystemWindows(w,false);
        WindowCompat.getInsetsController(w,w.getDecorView()).setAppearanceLightStatusBars(false);
        WindowCompat.getInsetsController(w,w.getDecorView()).setAppearanceLightNavigationBars(false);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(ToolUi.BG);
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });
        title=ToolUi.text(this,"节拍器",22,ToolUi.TEXT);root.addView(title,new LinearLayout.LayoutParams(-1,dp(64)));
        body=new FrameLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);setVolumeControlStream(AudioManager.STREAM_MUSIC);
        showInitialPage();
    }
    protected void showInitialPage(){
        capture=new TunerCapture(new TunerCapture.Listener(){
            public void pitch(double hz){
                if(!tunerPage||!resumed)return;
                if(hz>0){renderPitch(hz);tuningUi.removeCallbacks(stalePitch);tuningUi.postDelayed(stalePitch,500);}
            }
            public void error(){if(tunerPage){renderPitch(0);microphone.setVisibility(View.VISIBLE);microphone.setText("麦克风暂不可用 · 点此重试");}}
        });
        LinearLayout tabs=new LinearLayout(this);tabs.setPadding(dp(24),dp(8),dp(24),dp(12));tabs.setGravity(Gravity.CENTER);
        tunerTab=tab("调音",R.id.tab_tuner);metronomeTab=tab("节拍",R.id.tab_metronome);
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(46),1);a.rightMargin=dp(6);tabs.addView(tunerTab,a);
        LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,dp(46),1);b.leftMargin=dp(6);tabs.addView(metronomeTab,b);
        root.addView(tabs,new LinearLayout.LayoutParams(-1,-2));
        tunerTab.setOnClickListener(v->switchPage(true,true));metronomeTab.setOnClickListener(v->switchPage(false,false));
        try{selectedString=prefs.getInt("string",5);}catch(ClassCastException ignored){}
        selectedString=Math.max(0,Math.min(5,selectedString));
        switchPage(prefs.getBoolean("tunerPage",false),false);
    }
    private Button tab(String label,int id){Button b=roundButton(label,label);b.setId(id);b.setTextSize(15);return b;}
    private void switchPage(boolean tuner,boolean request){
        tunerPage=tuner;prefs.edit().putBoolean("tunerPage",tuner).apply();
        capture.stop();tuningUi.removeCallbacksAndMessages(null);
        tunerTab.setBackground(ToolUi.shape(tuner?ToolUi.PANEL:ToolUi.BG,dp(12),0));tunerTab.setTextColor(tuner?ToolUi.MINT:ToolUi.MUTED);
        metronomeTab.setBackground(ToolUi.shape(!tuner?ToolUi.PANEL:ToolUi.BG,dp(12),0));metronomeTab.setTextColor(!tuner?ToolUi.MINT:ToolUi.MUTED);
        tunerTab.setSelected(tuner);metronomeTab.setSelected(!tuner);
        if(tuner){stopMetronome();showTuner();if(resumed&&hasMicrophone())capture.start();else if(request&&!prefs.getBoolean("microphoneAsked",false))enableMicrophone();}
        else showMetronome();
    }
    private boolean hasMicrophone(){return androidx.core.content.ContextCompat.checkSelfPermission(this,android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED;}
    private void enableMicrophone(){
        if(hasMicrophone()){microphone.setVisibility(View.GONE);capture.start();return;}
        if(android.os.Build.VERSION.SDK_INT<23)return;
        if(prefs.getBoolean("microphoneAsked",false)&&!shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO)){
            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())));return;
        }
        prefs.edit().putBoolean("microphoneAsked",true).apply();requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},402);
    }
    private void showTuner(){
        title.setText("调音器");LinearLayout content=page();content.setPadding(dp(22),0,dp(22),dp(8));
        deviation=new DeviationView(this);content.addView(deviation,new LinearLayout.LayoutParams(-1,dp(112)));
        status=ToolUi.text(this,"拨动琴弦",15,ToolUi.MUTED);status.setId(R.id.tuner_status);content.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));
        target=ToolUi.text(this,TuningMath.NOTES[selectedString],48,ToolUi.TEXT);target.setId(R.id.target_note);content.addView(target,new LinearLayout.LayoutParams(-1,dp(68)));
        LinearLayout instrument=new LinearLayout(this);instrument.setOrientation(LinearLayout.HORIZONTAL);instrument.setGravity(Gravity.CENTER);
        LinearLayout selectors=new LinearLayout(this);selectors.setOrientation(LinearLayout.VERTICAL);selectors.setGravity(Gravity.CENTER);
        int[] ids={R.id.string_1,R.id.string_2,R.id.string_3,R.id.string_4,R.id.string_5,R.id.string_6};
        for(int i=0;i<6;i++){
            final int index=i;Button b=roundButton(TuningMath.LETTERS[i]+"\n"+(i+1)+"弦",(i+1)+"弦 "+TuningMath.NOTES[i]);b.setId(ids[i]);b.setTextSize(17);b.setLineSpacing(0,.85f);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(50),dp(50));lp.topMargin=dp(5);lp.bottomMargin=dp(5);selectors.addView(b,lp);strings[i]=b;b.setOnClickListener(v->selectString(index));
        }
        instrument.addView(selectors,new LinearLayout.LayoutParams(dp(60),-1));
        headstock=new HeadstockView(this);headstock.setListener(this::selectString);instrument.addView(headstock,new LinearLayout.LayoutParams(0,-1,1));
        content.addView(instrument,new LinearLayout.LayoutParams(-1,dp(370)));
        microphone=roundButton("开启麦克风，开始调音","开启麦克风，开始调音");microphone.setTextSize(14);microphone.setTextColor(ToolUi.MINT);microphone.setBackground(ToolUi.shape(ToolUi.PANEL,dp(12),0));microphone.setOnClickListener(v->enableMicrophone());
        microphone.setVisibility(hasMicrophone()?View.GONE:View.VISIBLE);content.addView(microphone,new LinearLayout.LayoutParams(-1,dp(46)));
        TextView standard=ToolUi.text(this,"标准六弦 · A₄ = 440 Hz",11,ToolUi.MUTED);content.addView(standard,new LinearLayout.LayoutParams(-1,dp(30)));
        selectString(selectedString);
    }
    private void selectString(int string){
        selectedString=string;prefs.edit().putInt("string",string).apply();
        target.setText(TuningMath.NOTES[string]);target.setContentDescription((string+1)+"弦，目标音 "+TuningMath.NOTES[string]);
        for(int i=0;i<6;i++){strings[i].setTextColor(i==string?ToolUi.BG:ToolUi.TEXT);strings[i].setBackground(ToolUi.shape(i==string?ToolUi.MINT:ToolUi.PANEL,dp(28),0));strings[i].setSelected(i==string);}
        headstock.setSelected(string);tuningUi.removeCallbacks(stalePitch);renderPitch(0);
    }
    void renderPitch(double hz){
        if(!tunerPage||status==null)return;double cents=TuningMath.cents(hz,selectedString);deviation.setCents(cents);
        status.setText(TuningMath.direction(cents));status.setTextColor(Double.isNaN(cents)?ToolUi.MUTED:Math.abs(cents)<=5?ToolUi.MINT:0xffecaa7c);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==402&&tunerPage){microphone.setVisibility(hasMicrophone()?View.GONE:View.VISIBLE);if(hasMicrophone()&&resumed)capture.start();}
    }

    protected int dp(float n){return ToolUi.dp(this,n);}
    protected LinearLayout page(){
        body.removeAllViews();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(24),dp(12),dp(24),dp(16));
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));body.addView(scroll,new FrameLayout.LayoutParams(-1,-1));return content;
    }
    protected void ensureCore(){
        if(core==null){core=MetronomeCore.getInstance();MetronomePlaybackService.configure(this,core);}
        core.setHandler(messages);
    }
    protected void showMetronome(){
        title.setText("节拍器");ensureCore();LinearLayout content=page();
        LinearLayout numbers=new LinearLayout(this);numbers.setGravity(Gravity.CENTER);numbers.setOrientation(LinearLayout.HORIZONTAL);
        Button minus=roundButton("−","速度减一");minus.setId(R.id.tempo_minus);numbers.addView(minus,new LinearLayout.LayoutParams(dp(52),dp(52)));
        LinearLayout readout=new LinearLayout(this);readout.setOrientation(LinearLayout.VERTICAL);readout.setGravity(Gravity.CENTER);
        bpmText=ToolUi.text(this,""+core.getTempoBpm(),62,ToolUi.TEXT);bpmText.setId(R.id.tempo_value);bpmText.setContentDescription("输入每分钟拍数");bpmText.setOnClickListener(v->inputTempo());bpmText.setFocusable(true);
        readout.addView(bpmText,new LinearLayout.LayoutParams(-1,-2));readout.addView(ToolUi.text(this,"每分钟拍数",12,ToolUi.MUTED));
        numbers.addView(readout,new LinearLayout.LayoutParams(dp(172),-2));
        Button plus=roundButton("+","速度加一");plus.setId(R.id.tempo_plus);numbers.addView(plus,new LinearLayout.LayoutParams(dp(52),dp(52)));
        minus.setOnClickListener(v->changeTempo(core.getTempoBpm()-1));plus.setOnClickListener(v->changeTempo(core.getTempoBpm()+1));
        content.addView(numbers,new LinearLayout.LayoutParams(-1,-2));
        int size=Math.min(dp(380),getResources().getDisplayMetrics().widthPixels-dp(48));
        FrameLayout disk=new FrameLayout(this);LinearLayout.LayoutParams diskLp=new LinearLayout.LayoutParams(size,size);diskLp.topMargin=dp(24);content.addView(disk,diskLp);
        dial=new DialView(this);dial.setId(R.id.tempo_dial);dial.setTempo(core.getTempoBpm());dial.setListener(this::changeTempo);disk.addView(dial,new FrameLayout.LayoutParams(-1,-1));
        play=roundButton("▶","开始节拍");play.setId(R.id.play_pause);play.setTextColor(ToolUi.MINT);play.setTextSize(32);play.setBackground(ToolUi.shape(ToolUi.BG,dp(64),0));
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(112),dp(112),Gravity.CENTER);disk.addView(play,p);play.setOnClickListener(v->togglePlayback());
        dots=new BeatDots(this);dots.setId(R.id.beat_dots);content.addView(dots,new LinearLayout.LayoutParams(-1,dp(42)));
        TextView meter=ToolUi.text(this,"4/4",22,ToolUi.TEXT);LinearLayout.LayoutParams meterLp=new LinearLayout.LayoutParams(-1,dp(50));meterLp.topMargin=dp(14);content.addView(meter,meterLp);
        content.addView(ToolUi.text(this,"第一拍重音 · 每拍一下",12,ToolUi.MUTED));updatePlaying();
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
        if(play==null||core==null)return;boolean active=core.getIsPlaying();play.setText(active?"Ⅱ":"▶");play.setContentDescription(active?"暂停节拍":"开始节拍");if(!active&&dots!=null)dots.setBeat(-1);
    }
    @Override protected void onResume(){super.onResume();resumed=true;if(core!=null)core.setHandler(messages);updatePlaying();if(tunerPage&&capture!=null){renderPitch(0);microphone.setVisibility(hasMicrophone()?View.GONE:View.VISIBLE);if(hasMicrophone())capture.start();}}
    @Override protected void onPause(){resumed=false;if(capture!=null)capture.stop();tuningUi.removeCallbacksAndMessages(null);if(tunerPage)renderPitch(0);super.onPause();}
    @Override protected void onDestroy(){if(capture!=null)capture.close();tuningUi.removeCallbacksAndMessages(null);if(core!=null)core.setHandler(null);messages.removeCallbacksAndMessages(null);super.onDestroy();}
}
