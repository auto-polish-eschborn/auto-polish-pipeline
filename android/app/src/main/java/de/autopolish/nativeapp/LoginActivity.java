package de.autopolish.nativeapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends BaseActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private EditText username;
    private EditText password;
    private CheckBox remember;
    private Button login;
    private ProgressBar progress;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildContent());
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.surface(this));
        LinearLayout outer = Ui.vertical(this, 24);
        outer.setGravity(Gravity.CENTER);

        LinearLayout card = Ui.vertical(this, 24);
        card.setBackground(Ui.bordered(Color.WHITE, Ui.BORDER, 20, this));
        card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView logo = new ImageView(this);
        logo.setImageResource(de.autopolish.nativeapp.R.drawable.auto_polish_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setBackgroundColor(Color.TRANSPARENT);
        int tone=Ui.ink(this);logo.setColorFilter(new android.graphics.ColorMatrixColorFilter(new float[]{0,0,0,0,Color.red(tone),0,0,0,0,Color.green(tone),0,0,0,0,Color.blue(tone),-0.33333334f,-0.33333334f,-0.33333334f,0,255}));
        logo.setContentDescription("Auto-Polish Fahrzeugaufbereitung");
        card.addView(logo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 88)));
        TextView title = Ui.text(this, "Anmelden", 30, Ui.BLACK, true);
        Ui.marginTop(title, this, 8);
        card.addView(title);
        TextView hint = Ui.text(this, "Ihre Auftragsverwaltung", 16, Ui.MUTED, false);
        Ui.marginTop(hint, this, 6);
        card.addView(hint);

        username = Ui.input(this, "Benutzername");
        username.setText("Santino");
        Ui.marginTop(username, this, 26);
        card.addView(username);

        password = Ui.input(this, "Passwort");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Ui.marginTop(password, this, 12);
        card.addView(password);

        remember = new CheckBox(this);
        remember.setText("Angemeldet bleiben");
        remember.setTextSize(15);
        remember.setChecked(true);
        Ui.marginTop(remember, this, 12);
        card.addView(remember);

        login = Ui.button(this, "Sicher anmelden", Ui.BLUE);
        login.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 54)));
        login.setOnClickListener(v -> authenticate());
        Ui.marginTop(login, this, 16);
        card.addView(login);

        progress = new ProgressBar(this);
        progress.setVisibility(ProgressBar.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = Ui.dp(this, 12);
        progress.setLayoutParams(progressParams);
        card.addView(progress);

        outer.addView(card);
        scroll.addView(outer);
        return scroll;
    }

    private void authenticate() {
        String user = username.getText().toString().trim();
        String pass = password.getText().toString();
        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Bitte Benutzername und Passwort eingeben.", Toast.LENGTH_LONG).show();
            return;
        }
        boolean rememberLogin = remember.isChecked();
        setLoading(true);
        executor.execute(() -> {
            try {
                ApiClient.login(user, pass);
                SessionState.active = true;
                getSharedPreferences("session", MODE_PRIVATE).edit()
                        .putBoolean("logged_in", rememberLogin)
                        .putString("username", user)
                        .putBoolean("skip_biometric_once", true)
                        .apply();
                main.post(() -> {
                    setLoading(false);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
            } catch (Exception error) {
                main.post(() -> {
                    setLoading(false);
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean value) {
        progress.setVisibility(value ? ProgressBar.VISIBLE : ProgressBar.GONE);
        login.setEnabled(!value);
        username.setEnabled(!value);
        password.setEnabled(!value);
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
