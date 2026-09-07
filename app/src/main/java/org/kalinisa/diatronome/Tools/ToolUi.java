package org.kalinisa.diatronome.Tools;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.TextView;

public final class ToolUi {
    public static final int BG = Color.rgb(25,27,29), PANEL = Color.rgb(37,40,43);
    public static final int TEXT = Color.rgb(242,244,246), MUTED = Color.rgb(146,153,161);
    public static final int MINT = Color.rgb(18,201,158);
    public static int dp(Context c, float n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    public static GradientDrawable shape(int color, float radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius);
        if (stroke != 0) d.setStroke(2, stroke); return d;
    }
    public static TextView text(Context c, String label, float size, int color) {
        TextView t = new TextView(c); t.setText(label); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); t.setFontFeatureSettings("tnum");
        t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL)); return t;
    }
    private ToolUi() {}
}
