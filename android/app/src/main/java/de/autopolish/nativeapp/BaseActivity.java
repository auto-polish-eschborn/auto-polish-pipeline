package de.autopolish.nativeapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.graphics.Color;

public class BaseActivity extends Activity {
    private boolean dark;
    private java.util.function.Consumer<String> scanned;
    protected void scan(java.util.function.Consumer<String> callback){scanned=callback;startActivityForResult(new android.content.Intent(this,BarcodeActivity.class),904);}
    @Override protected void onActivityResult(int request,int result,android.content.Intent data){super.onActivityResult(request,result,data);if(request==904&&result==RESULT_OK&&data!=null){String code=data.getStringExtra("barcode");if(code!=null&&!code.trim().isEmpty()){startActivity(new android.content.Intent(this,ScanResultActivity.class).putExtra("barcode",code.trim()));}}}
    @Override protected void onCreate(Bundle state) {
        dark = Ui.dark(this);
        setTheme(dark ? R.style.AppThemeDark : R.style.AppTheme);
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarContrastEnforced(false);
    }
    @Override public void setContentView(View view) {
        FrameLayout safe = new FrameLayout(this);
        safe.setBackgroundColor(Ui.BLACK);
        safe.addView(view);
        safe.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        super.setContentView(safe);
        safe.requestApplyInsets();
    }
    @Override protected void onResume() {
        super.onResume();
        if (dark != Ui.dark(this)) recreate();
    }
}
