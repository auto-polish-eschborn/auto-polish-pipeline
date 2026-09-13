package de.autopolish.nativeapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderDetailActivity extends BaseActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private String orderId;
    private LinearLayout content;
    private ProgressBar progress;
    private Order order;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        orderId = getIntent().getStringExtra("order_id");
        if (orderId == null || orderId.isEmpty()) { finish(); return; }
        setContentView(buildShell());
        load();
    }

    private View buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.surface(this));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12));
        top.setBackgroundColor(Ui.BLACK);
        Button back = Ui.button(this, "‹", Ui.BLACK);
        back.setTextSize(28);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        TextView title = Ui.text(this, "Auftragsdetails", 20, Color.WHITE, true);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(top);

        ScrollView scroll = new ScrollView(this);
        content = Ui.vertical(this, 16);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 46)));
        return root;
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                JSONObject result = ApiClient.order(orderId);
                Order loaded = Order.fromJson(result.getJSONObject("order"));
                main.post(() -> { order = loaded; progress.setVisibility(View.GONE); render(); });
            } catch (Exception error) {
                main.post(() -> { progress.setVisibility(View.GONE); Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); });
            }
        });
    }

    private void render() {
        content.removeAllViews();
        LinearLayout hero = card();
        LinearLayout topLine = new LinearLayout(this);
        topLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView number = Ui.text(this, order.orderNumber, 16, Ui.BLUE, true);
        topLine.addView(number, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView amount = Ui.text(this, order.gross(), 20, Ui.BLACK, true);
        topLine.addView(amount);
        hero.addView(topLine);
        TextView plate = Ui.text(this, order.licensePlate, 32, Ui.BLACK, true);
        Ui.marginTop(plate, this, 12);
        hero.addView(plate);
        TextView customer = Ui.text(this, order.customerName(), 18, Ui.BLACK, true);
        Ui.marginTop(customer, this, 4);
        hero.addView(customer);
        content.addView(hero);

        LinearLayout statusCard = card();
        statusCard.addView(Ui.text(this, "Auftragsstatus", 17, Ui.BLACK, true));
        Spinner spinner = new Spinner(this);
        String[] statuses = {"offen", "in Bearbeitung", "abgeschlossen"};
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses));
        for (int i = 0; i < statuses.length; i++) if (statuses[i].equals(order.status)) spinner.setSelection(i);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        Ui.marginTop(spinner, this, 8);
        statusCard.addView(spinner);
        Button saveStatus = Ui.button(this, "Status speichern", Ui.BLUE);
        saveStatus.setOnClickListener(v -> updateStatus(statuses[spinner.getSelectedItemPosition()]));
        Ui.marginTop(saveStatus, this, 8);
        statusCard.addView(saveStatus);
        Ui.marginTop(statusCard, this, 12);
        content.addView(statusCard);

        content.addView(infoCard("Kundendaten",
                order.customerName(),
                order.street,
                (order.postalCode + " " + order.city).trim(),
                order.phone,
                order.email));
        content.addView(infoCard("Fahrzeugdaten", order.vehicle, order.licensePlate, "FIN: " + order.vin, "Kilometerstand: " + order.mileage));

        LinearLayout services = card();
        services.addView(Ui.text(this, "Durchzuführende Arbeiten", 17, Ui.BLACK, true));
        for (int i = 0; i < order.services.length(); i++) {
            JSONObject service = order.services.optJSONObject(i);
            if (service == null) continue;
            String price = String.format(java.util.Locale.GERMANY, "%.2f €", service.optDouble("price"));
            LinearLayout line = new LinearLayout(this);
            line.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
            TextView name = Ui.text(this, "•  " + service.optString("name"), 15, Ui.BLACK, false);
            line.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            line.addView(Ui.text(this, price, 15, Ui.BLACK, true));
            services.addView(line);
        }
        Ui.marginTop(services, this, 12);
        content.addView(services);

        LinearLayout actions = card();
        Button edit = Ui.button(this, "Auftrag bearbeiten", Ui.BLUE);
        edit.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderFormActivity.class);
            intent.putExtra("order_id", orderId);
            startActivity(intent);
        });
        actions.addView(edit, fullButton());
        Button pdf = Ui.button(this, "Dokument ansehen", Ui.BLACK);
        pdf.setOnClickListener(v -> downloadPdf());
        Ui.marginTop(pdf, this, 10);
        actions.addView(pdf, fullButton());
        Button workCard=Ui.button(this,"Als Arbeitskarte speichern",Ui.BLUE);
        workCard.setOnClickListener(v->{workCard.setEnabled(false);executor.execute(()->{try{JSONObject result=ApiClient.request("POST","/api/work-cards",FormActivity.json("sourceOrderId",orderId));main.post(()->{if(isDestroyed())return;workCard.setEnabled(true);BusinessActivity.open(this,"work-card",result.optJSONObject("record").optString("id"));});}catch(Exception e){main.post(()->{if(isDestroyed())return;workCard.setEnabled(true);Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();});}});});
        actions.addView(workCard,fullButton());
        Button photos=Ui.button(this,"Fotos und Unterschrift",Ui.BLUE);photos.setOnClickListener(v->BusinessActivity.open(this,"order",orderId));actions.addView(photos,fullButton());
        Button delete = Ui.button(this, "Auftrag löschen", Ui.RED);
        delete.setOnClickListener(v -> askDelete());
        Ui.marginTop(delete, this, 10);
        actions.addView(delete, fullButton());
        Ui.marginTop(actions, this, 12);
        content.addView(actions);
    }

    private LinearLayout card() {
        LinearLayout card = Ui.vertical(this, 16);
        card.setBackground(Ui.bordered(Color.WHITE, Ui.BORDER, 16, this));
        card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private LinearLayout infoCard(String title, String... values) {
        LinearLayout card = card();
        card.addView(Ui.text(this, title, 17, Ui.BLACK, true));
        for (String value : values) {
            if (value == null || value.trim().isEmpty() || value.endsWith(": ")) continue;
            TextView line = Ui.text(this, value, 15, Ui.MUTED, false);
            Ui.marginTop(line, this, 7);
            card.addView(line);
        }
        Ui.marginTop(card, this, 12);
        return card;
    }

    private LinearLayout.LayoutParams fullButton() {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,Ui.dp(this,52));p.topMargin=Ui.dp(this,10);return p;
    }

    private void updateStatus(String status) {
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                ApiClient.updateStatus(orderId, status);
                main.post(() -> { progress.setVisibility(View.GONE); Toast.makeText(this, "Status gespeichert.", Toast.LENGTH_SHORT).show(); load(); });
            } catch (Exception error) {
                main.post(() -> { progress.setVisibility(View.GONE); Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); });
            }
        });
    }

    private void downloadPdf() { PdfActivity.open(this,"/api/orders/"+Uri.encode(orderId)+"/pdf","Auftrag "+order.orderNumber); }

    private void askDelete() {
        EditText password = Ui.input(this, "System-Passwort");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
                .setTitle("Auftrag wirklich löschen?")
                .setMessage("Zum Löschen bitte das System-Passwort eingeben.")
                .setView(password)
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Löschen", (dialog, which) -> delete(password.getText().toString()))
                .show();
    }

    private void delete(String password) {
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                ApiClient.deleteOrder(orderId, password);
                main.post(() -> { Toast.makeText(this, "Auftrag gelöscht.", Toast.LENGTH_SHORT).show(); finish(); });
            } catch (Exception error) {
                main.post(() -> { progress.setVisibility(View.GONE); Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); });
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (order != null) load();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
