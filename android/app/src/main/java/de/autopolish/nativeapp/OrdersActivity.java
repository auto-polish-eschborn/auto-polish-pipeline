package de.autopolish.nativeapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrdersActivity extends BaseActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Order> orders = new ArrayList<>();
    private OrderAdapter adapter;
    private EditText search;
    private ProgressBar progress;
    private TextView empty;
    private boolean initialized;
    private long lastLoadAt;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        initializeApp();
    }

    private void initializeApp() {
        if (initialized) return;
        initialized = true;
        setContentView(buildContent());
        String initialQuery = getIntent().getStringExtra("query");
        if (initialQuery == null) initialQuery = "";
        search.setText(initialQuery);
        loadOrders(initialQuery);
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.surface(this));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10));
        top.setBackgroundColor(Ui.BLACK);
        ImageView logo = new ImageView(this);
        logo.setImageResource(de.autopolish.nativeapp.R.drawable.auto_polish_logo);
        Ui.alignLogo(logo);
        logo.setBackgroundColor(Color.TRANSPARENT);
        logo.setColorFilter(new android.graphics.ColorMatrixColorFilter(new float[]{0,0,0,0,255,0,0,0,0,255,0,0,0,0,255,-0.33333334f,-0.33333334f,-0.33333334f,0,255}));
        logo.setContentDescription("Auto-Polish Fahrzeugaufbereitung");
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1);
        logoParams.rightMargin = Ui.dp(this, 16);
        top.addView(logo, logoParams);
        Button menu = Ui.button(this, "Menü", Ui.BLACK);
        menu.setOnClickListener(this::showMenu);
        top.addView(menu, new LinearLayout.LayoutParams(Ui.dp(this, 90), Ui.dp(this, 46)));
        root.addView(top);

        LinearLayout controls = Ui.vertical(this, 16);
        TextView heading = Ui.text(this, "Alle Aufträge", 28, Ui.BLACK, true);
        controls.addView(heading);
        TextView subheading = Ui.text(this, "Auftrag, Kennzeichen oder Kunde suchen", 15, Ui.MUTED, false);
        Ui.marginTop(subheading, this, 4);
        controls.addView(subheading);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        search = Ui.input(this, "Suchen …");
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadOrders(search.getText().toString());
                return true;
            }
            return false;
        });
        row.addView(Ui.scanField(this,search,code->loadOrders(code)), new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1));
        Button find = Ui.button(this, "Suchen", Ui.BLUE);
        LinearLayout.LayoutParams findParams = new LinearLayout.LayoutParams(Ui.dp(this, 96), Ui.dp(this, 52));
        findParams.leftMargin = Ui.dp(this, 8);
        row.addView(find, findParams);
        find.setOnClickListener(v -> loadOrders(search.getText().toString()));
        Ui.marginTop(row, this, 16);
        controls.addView(row);

        Button add = Ui.button(this, "+  Neuer Fahrzeugauftrag", Ui.BLUE);
        add.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 54)));
        add.setOnClickListener(v -> startActivity(new Intent(this, OrderFormActivity.class)));
        Ui.marginTop(add, this, 12);
        controls.addView(add);
        root.addView(controls);

        progress = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = Ui.dp(this, 8);
        root.addView(progress, progressParams);

        empty = Ui.text(this, "Noch keine Aufträge vorhanden.", 16, Ui.MUTED, false);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        root.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 80)));

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 24));
        list.setClipToPadding(false);
        adapter = new OrderAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Intent detail = new Intent(this, OrderDetailActivity.class);
            detail.putExtra("order_id", orders.get(position).id);
            startActivity(detail);
        });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Aufträge aktualisieren");
        popup.getMenu().add("Abmelden");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().toString().startsWith("Aufträge")) {
                loadOrders(search.getText().toString());
            } else {
                getSharedPreferences("session", MODE_PRIVATE).edit().clear().apply();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
            return true;
        });
        popup.show();
    }

    private void loadOrders(String query) {
        lastLoadAt = android.os.SystemClock.elapsedRealtime();
        progress.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        executor.execute(() -> {
            try {
                JSONObject result = ApiClient.orders(query);
                JSONArray rows = result.optJSONArray("orders");
                List<Order> loaded = new ArrayList<>();
                if (rows != null) for (int i = 0; i < rows.length(); i++) loaded.add(Order.fromJson(rows.getJSONObject(i)));
                main.post(() -> {
                    orders.clear();
                    orders.addAll(loaded);
                    adapter.notifyDataSetChanged();
                    progress.setVisibility(View.GONE);
                    empty.setText(query == null || query.trim().isEmpty() ? "Noch keine Aufträge vorhanden." : "Kein passender Auftrag gefunden.");
                    empty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
                });
            } catch (Exception error) {
                main.post(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Aufträge konnten nicht geladen werden: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (initialized && adapter != null && search != null
                && android.os.SystemClock.elapsedRealtime() - lastLoadAt > 500) {
            loadOrders(search.getText().toString());
        }
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private final class OrderAdapter extends ArrayAdapter<Order> {
        OrderAdapter() { super(OrdersActivity.this, android.R.layout.simple_list_item_1, orders); }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Order order = getItem(position);
            LinearLayout card = Ui.vertical(OrdersActivity.this, 16);
            card.setBackground(Ui.bordered(Color.WHITE, Ui.BORDER, 16, OrdersActivity.this));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(Ui.dp(OrdersActivity.this, 4), Ui.dp(OrdersActivity.this, 7), Ui.dp(OrdersActivity.this, 4), Ui.dp(OrdersActivity.this, 7));
            card.setLayoutParams(cardParams);

            LinearLayout header = new LinearLayout(OrdersActivity.this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView number = Ui.text(OrdersActivity.this, order.orderNumber, 14, Ui.BLUE, true);
            header.addView(number, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView status = Ui.text(OrdersActivity.this, order.status, 13, Color.WHITE, true);
            status.setGravity(Gravity.CENTER);
            status.setPadding(Ui.dp(OrdersActivity.this, 10), Ui.dp(OrdersActivity.this, 5), Ui.dp(OrdersActivity.this, 10), Ui.dp(OrdersActivity.this, 5));
            int color = "abgeschlossen".equals(order.status) ? Ui.GREEN : "in Bearbeitung".equals(order.status) ? Ui.ORANGE : Ui.BLUE;
            status.setBackground(Ui.background(color, 16, OrdersActivity.this));
            header.addView(status);
            card.addView(header);

            TextView plate = Ui.text(OrdersActivity.this, order.licensePlate, 24, Ui.BLACK, true);
            Ui.marginTop(plate, OrdersActivity.this, 10);
            card.addView(plate);
            TextView customer = Ui.text(OrdersActivity.this, order.customerName(), 16, Ui.BLACK, true);
            Ui.marginTop(customer, OrdersActivity.this, 4);
            card.addView(customer);
            String meta = (order.vehicle.isEmpty() ? "Fahrzeug nicht angegeben" : order.vehicle) + "  ·  " + order.gross();
            TextView details = Ui.text(OrdersActivity.this, meta, 14, Ui.MUTED, false);
            Ui.marginTop(details, OrdersActivity.this, 4);
            card.addView(details);
            return card;
        }
    }
}
