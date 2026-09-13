package de.autopolish.nativeapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BLUE = Color.rgb(21, 84, 209);
    static final int BLACK = Color.rgb(17, 19, 24);
    static final int SURFACE = Color.rgb(243, 245, 248);
    static final int MUTED = Color.rgb(102, 112, 133);
    static final int BORDER = Color.rgb(217, 222, 232);
    static final int RED = Color.rgb(198, 40, 40);
    static final int GREEN = Color.rgb(35, 122, 59);
    static final int ORANGE = Color.rgb(184, 92, 0);

    private Ui() {}
    static boolean dark(Context c) { return c.getSharedPreferences("app_preferences", 0).getBoolean("dark", false); }
    static int accent(Context c) {
        switch(c.getSharedPreferences("app_preferences",0).getString("accent","blue")) {
            case "red": return Color.rgb(185,28,28);
            case "green": return Color.rgb(21,128,61);
            case "black": return Color.rgb(55,65,81);
            default: return BLUE;
        }
    }
    static int surface(Context c) { return dark(c) ? Color.rgb(17,19,24) : SURFACE; }
    static int card(Context c) { return dark(c) ? Color.rgb(30,34,42) : Color.WHITE; }
    static int ink(Context c) { return dark(c) ? Color.rgb(240,243,248) : BLACK; }
    static int muted(Context c) { return dark(c) ? Color.rgb(172,184,203) : MUTED; }
    static int border(Context c) { return dark(c) ? Color.rgb(66,76,90) : BORDER; }
    static int readable(Context c,int color) {
        if(!dark(c)) return color;
        if(color==BLACK) return ink(c);
        if(color==MUTED) return muted(c);
        if(color==BLUE) return Color.rgb(116,165,255);
        if(color==GREEN) return Color.rgb(104,206,136);
        if(color==ORANGE) return Color.rgb(255,185,106);
        return color;
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable background(int color, int radiusDp, Context context) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color == Color.WHITE ? card(context) : color);
        shape.setCornerRadius(dp(context, radiusDp));
        return shape;
    }

    static GradientDrawable bordered(int color, int strokeColor, int radiusDp, Context context) {
        GradientDrawable shape = background(color, radiusDp, context);
        shape.setStroke(dp(context, 1), strokeColor == BORDER ? border(context) : strokeColor);
        return shape;
    }

    static TextView text(Context context, String value, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(readable(context, color));
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setLineSpacing(0, 1.12f);
        return view;
    }

    static android.widget.FrameLayout scanField(BaseActivity activity,EditText field,java.util.function.Consumer<String> result){android.widget.FrameLayout box=new android.widget.FrameLayout(activity);field.setPadding(dp(activity,14),0,dp(activity,48),0);box.addView(field,new android.widget.FrameLayout.LayoutParams(-1,-1));android.widget.FrameLayout button=new android.widget.FrameLayout(activity);AppIcon icon=new AppIcon(activity,"barcode",accent(activity));android.widget.FrameLayout.LayoutParams ip=new android.widget.FrameLayout.LayoutParams(dp(activity,22),dp(activity,22),Gravity.CENTER);button.addView(icon,ip);button.setContentDescription("Barcode scannen");button.setFocusable(true);button.setOnClickListener(v->activity.scan(code->{field.setText(code);result.accept(code);}));box.addView(button,new android.widget.FrameLayout.LayoutParams(dp(activity,48),-1,Gravity.END));return box;}
    static void alignLogo(android.widget.ImageView logo){logo.setScaleType(android.widget.ImageView.ScaleType.MATRIX);logo.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{android.graphics.drawable.Drawable d=logo.getDrawable();if(d==null)return;float scale=Math.min((r-l)/(float)d.getIntrinsicWidth(),(b-t)/(float)d.getIntrinsicHeight());android.graphics.Matrix m=new android.graphics.Matrix();m.setScale(scale,scale);m.postTranslate(0,((b-t)-d.getIntrinsicHeight()*scale)/2);logo.setImageMatrix(m);});}
    static EditText input(Context context, String hint) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setTextColor(ink(context));
        input.setHintTextColor(muted(context));
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        input.setBackground(bordered(Color.WHITE, BORDER, 12, context));
        input.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 52)));
        return input;
    }

    static Button button(Context context, String label, int color) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setElevation(0);
        button.setStateListAnimator(null);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setBackground(background(color == BLUE ? accent(context) : color, 12, context));
        boolean secondary=label.contains("ansehen")||label.contains("Unterschrift")||label.startsWith("PDF")||label.equals("Drucken")||label.startsWith("Als Arbeitskarte")||label.contains("löschen")||label.equals("Löschen");
        if(secondary){button.setBackground(bordered(card(context),border(context),12,context));button.setTextColor(label.toLowerCase(java.util.Locale.ROOT).contains("löschen")?readable(context,RED):ink(context));}
        button.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        button.setMinHeight(dp(context, 48));
        LinearLayout.LayoutParams spacing=new LinearLayout.LayoutParams(-1,-2);spacing.topMargin=dp(context,10);button.setLayoutParams(spacing);
        return button;
    }

    static LinearLayout vertical(Context context, int paddingDp) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(context, paddingDp), dp(context, paddingDp), dp(context, paddingDp), dp(context, paddingDp));
        return layout;
    }

    static void marginTop(View view, Context context, int dp) {
        ViewGroup.LayoutParams raw = view.getLayoutParams();
        LinearLayout.LayoutParams params = raw instanceof LinearLayout.LayoutParams
                ? (LinearLayout.LayoutParams) raw
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Ui.dp(context, dp);
        view.setLayoutParams(params);
    }
}
