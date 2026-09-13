package de.autopolish.nativeapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView totalValue, openValue, completedValue;
    private EditText search;
    private ProgressBar progress;
    private boolean initialized;
    private JSONObject layout;
    private JSONObject savedLayout;
    private boolean editing, notificationsLoading;
    private java.util.List<Notifications.Item> notices = new java.util.ArrayList<>();
    private String notificationError = "";
    private TextView bellCount;
    private android.widget.FrameLayout bellButton;
    private AppIcon bellIcon;
    private android.animation.ValueAnimator bellAnimation;
    private final Runnable refreshNotices = new Runnable() { public void run() { loadNotifications(); main.postDelayed(this,30000); } };
    private JSONObject defaults() {
        try { return new JSONObject(DEFAULT_LAYOUT); } catch(Exception e) { throw new IllegalStateException(e); }
    }
    private static final String DEFAULT_LAYOUT = "{\"headerBrand\": \"Auto-Polish\", \"headerTitle\": \"Auftragsverwaltung\", \"dashboardTitle\": \"Dashboard von Auto-Polish\", \"dashboardIcon\": \"car\", \"dashboardIconColor\": \"#1d4ed8\", \"dashboardIconSize\": 48, \"showDashboardIcon\": true, \"sectionTitles\": {\"customers\": \"Aufträge & Kunden\", \"workshop\": \"Werkstatt\", \"tires\": \"Radeinlagerung\", \"organization\": \"Organisation\"}, \"tileTitles\": {\"new-order\": \"Neuer Fahrzeugauftrag\", \"orders\": \"Alle Aufträge\", \"customer-link\": \"Kundenauftrag senden\", \"offer\": \"Angebot erstellen\", \"new-work-card\": \"Arbeitskarte erstellen\", \"work-cards\": \"Arbeitskarten\", \"new-tire-storage\": \"Reifeneinlagerung\", \"tire-storages\": \"Radeinlagerung Formulare\", \"notes\": \"Notizen\", \"letterhead\": \"Briefpapier\"}, \"tileOrder\": {\"customers\": [\"new-order\", \"orders\", \"customer-link\", \"offer\"], \"workshop\": [\"new-work-card\", \"work-cards\"], \"tires\": [\"new-tire-storage\", \"tire-storages\"], \"organization\": [\"notes\", \"letterhead\"]}, \"blockOrder\": [\"overview\", \"customers\", \"workshop\", \"tires\", \"organization\"]}";


    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!SessionState.active && !getSharedPreferences("session", MODE_PRIVATE).getBoolean("logged_in", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        boolean skip = getSharedPreferences("session", MODE_PRIVATE).getBoolean("skip_biometric_once", false);
        if (skip) {
            getSharedPreferences("session", MODE_PRIVATE).edit().remove("skip_biometric_once").apply();
            initializeDashboard();
        } else unlock();
    }

    private void unlock() {
        try {
            BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                    .setTitle("Auto-Polish entsperren")
                    .setSubtitle("Fingerabdruck oder Geräte-PIN verwenden")
                    .setDeviceCredentialAllowed(true).build();
            prompt.authenticate(new CancellationSignal(), getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) { initializeDashboard(); }
                @Override public void onAuthenticationError(int code, CharSequence message) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            });
        } catch (Exception ignored) { initializeDashboard(); }
    }

    private void initializeDashboard() {
        if (initialized) return;
        initialized = true;
        layout=defaults();
        try { layout=new JSONObject(getSharedPreferences("app_preferences",0).getString("dashboard_layout",DEFAULT_LAYOUT)); } catch(Exception ignored) {}
        loadLayout();
        main.removeCallbacks(refreshNotices); main.post(refreshNotices);
        setContentView(buildDashboard());
        loadStats();
    }

    private View buildDashboard() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.surface(this));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10));
        header.setBackgroundColor(Ui.BLACK);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.auto_polish_logo);
        Ui.alignLogo(logo);
        logo.setBackgroundColor(Color.TRANSPARENT);
        logo.setColorFilter(new android.graphics.ColorMatrixColorFilter(new float[]{0,0,0,0,255, 0,0,0,0,255, 0,0,0,0,255, -0.33333334f,-0.33333334f,-0.33333334f,0,255}));
        logo.setContentDescription("Auto-Polish Fahrzeugaufbereitung");
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1);
        logoParams.rightMargin = Ui.dp(this, 12);
        header.addView(logo, logoParams);
        bellButton=new android.widget.FrameLayout(this);bellButton.setContentDescription("Benachrichtigungen");
        bellIcon=new AppIcon(this,"bell",Ui.MUTED);bellButton.addView(bellIcon,new android.widget.FrameLayout.LayoutParams(Ui.dp(this,28),Ui.dp(this,28),Gravity.CENTER));
        bellCount=Ui.text(this,"",11,Color.WHITE,true);bellCount.setGravity(Gravity.CENTER);bellCount.setBackground(Ui.background(Ui.RED,10,this));bellButton.addView(bellCount,new android.widget.FrameLayout.LayoutParams(Ui.dp(this,20),Ui.dp(this,20),Gravity.TOP|Gravity.RIGHT));
        bellButton.setOnClickListener(v->showNotifications());LinearLayout.LayoutParams bellParams=new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,44));bellParams.rightMargin=Ui.dp(this,6);header.addView(bellButton,bellParams);
        android.widget.FrameLayout settingsButton=new android.widget.FrameLayout(this);AppIcon gear=new AppIcon(this,"settings",Color.WHITE);android.widget.FrameLayout.LayoutParams gearParams=new android.widget.FrameLayout.LayoutParams(Ui.dp(this,28),Ui.dp(this,28),Gravity.CENTER);settingsButton.addView(gear,gearParams);settingsButton.setContentDescription("Einstellungen");settingsButton.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));header.addView(settingsButton,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,52)));
        Button menu = Ui.button(this, "⋮", Ui.BLACK);
        menu.setTextSize(24);
        menu.setContentDescription("Weitere Optionen");
        menu.setOnClickListener(this::showMenu);
        header.addView(menu, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        root.addView(header);
        updateBell();

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.vertical(this, 16);
        LinearLayout profile=Ui.vertical(this,12);profile.setOrientation(LinearLayout.HORIZONTAL);profile.setGravity(Gravity.CENTER_VERTICAL);profile.setBackground(Ui.bordered(Color.WHITE,Ui.BORDER,12,this));profile.setContentDescription("Mein Profil");profile.addView(new AppIcon(this,"profile",Ui.readable(this,Ui.BLUE)),new LinearLayout.LayoutParams(Ui.dp(this,28),Ui.dp(this,28)));TextView user=Ui.text(this,"  "+getSharedPreferences("session",0).getString("username","Mein Profil"),16,Ui.BLACK,true);profile.addView(user);profile.setOnClickListener(v->startActivity(new Intent(this,ProfileActivity.class)));content.addView(profile,new LinearLayout.LayoutParams(-1,Ui.dp(this,56)));
        TextView dashboardTitle=Ui.text(this,layout.optString("dashboardTitle","Dashboard von Auto-Polish"),19,Ui.BLACK,true);Ui.marginTop(dashboardTitle,this,18);content.addView(dashboardTitle);
        if(editing) dashboardTitle.setOnClickListener(v->rename(layout,"dashboardTitle","Dashboard-Titel"));
        Button edit=Ui.button(this,editing?"Anordnung speichern":"Dashboard bearbeiten",Ui.BLUE);Ui.marginTop(edit,this,12);edit.setOnClickListener(v->{if(editing)saveLayout();else{try{savedLayout=new JSONObject(layout.toString());}catch(Exception ignored){}editing=true;redraw();}});if(editing)content.addView(edit);
        if(editing){Button cancel=Ui.button(this,"Abbrechen",Ui.MUTED);cancel.setOnClickListener(v->{if(savedLayout!=null)layout=savedLayout;editing=false;redraw();});content.addView(cancel);content.addView(Ui.text(this,"Kacheln gedrückt halten und ziehen oder mit Pfeilen verschieben. Titel antippen zum Ändern.",14,Ui.MUTED,false));}
        TextView date = Ui.text(this, new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMANY).format(new Date()), 14, Ui.MUTED, false);
        Ui.marginTop(date, this, 5);
        content.addView(date);

        TextView searchTitle = Ui.text(this, "Schnellsuche", 18, Ui.BLACK, true);
        Ui.marginTop(searchTitle, this, 22);
        content.addView(searchTitle);
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        search = Ui.input(this, "Auftrag, Kennzeichen oder Kunde");
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { openOrderSearch(); return true; }
            return false;
        });
        searchRow.addView(Ui.scanField(this,search,code->openOrderSearch()), new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1));
        Button find = Ui.button(this, "Suchen", Ui.BLUE);
        LinearLayout.LayoutParams findParams = new LinearLayout.LayoutParams(Ui.dp(this, 96), Ui.dp(this, 52));
        findParams.leftMargin = Ui.dp(this, 8);
        searchRow.addView(find, findParams);
        find.setOnClickListener(v -> openOrderSearch());
        Ui.marginTop(searchRow, this, 10);
        content.addView(searchRow);

        org.json.JSONArray blocks=layout.optJSONArray("blockOrder");
        for(int i=0;i<blocks.length();i++){
            String block=blocks.optString(i);
            LinearLayout section=Ui.vertical(this,0);
            if(editing) addMoveButtons(section,blocks,i,()->{try{layout.put("blockOrder",blocks);}catch(Exception ignored){}redraw();});
            if(block.equals("overview")) addOverview(section); else addConfiguredSection(section,block);
            content.addView(section);
        }

        LinearLayout calendar = Ui.vertical(this, 14);
        calendar.setBackground(Ui.bordered(Color.WHITE, Ui.BORDER, 14, this));
        TextView calendarTitle = Ui.text(this, "▾  Auto-Polish Kalender", 17, Ui.BLACK, true);
        calendar.addView(calendarTitle);
        calendar.setOnClickListener(v -> BusinessActivity.open(this,"calendar",null));
        LinearLayout.LayoutParams calendarParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        calendarParams.topMargin = Ui.dp(this, 22);
        calendarParams.bottomMargin = Ui.dp(this, 18);
        content.addView(calendar, calendarParams);

        TextView version = Ui.text(this, "Auto-Polish App · Version 1.3.21", 12, Ui.MUTED, false);
        version.setGravity(Gravity.CENTER);
        content.addView(version);
        if(!editing){edit.setTextSize(11);edit.setTextColor(Ui.muted(this));edit.setBackgroundColor(Color.TRANSPARENT);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-2,Ui.dp(this,48));ep.gravity=Gravity.END;ep.topMargin=Ui.dp(this,4);content.addView(edit,ep);}
        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 42)));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    private TextView addStat(LinearLayout parent, String label, int color) {
        LinearLayout card = Ui.vertical(this, 10);
        card.setGravity(Gravity.CENTER);
        card.setBackground(Ui.bordered(Color.WHITE, Ui.BORDER, 14, this));
        TextView number = Ui.text(this, "–", 25, color, true);
        number.setGravity(Gravity.CENTER);
        card.addView(number);
        TextView title = Ui.text(this, label, 12, Ui.MUTED, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Ui.dp(this, 86), 1);
        params.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
        parent.addView(card, params);
        return number;
    }

    private void addSection(LinearLayout content, String title, Tile[] tiles) {
        TextView heading = Ui.text(this, title, 19, Ui.BLACK, true);
        Ui.marginTop(heading, this, 24);
        content.addView(heading);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        for (Tile tile : tiles) {
            LinearLayout card = Ui.vertical(this, 14);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackground(Ui.bordered(Color.WHITE, Ui.BORDER, 16, this));
            if(tile.id.contains("tire"))card.addView(new AppIcon(this,tile.id.equals("tire-storages")?"warehouse":"tire",Ui.readable(this,Ui.BLUE)),new LinearLayout.LayoutParams(Ui.dp(this,34),Ui.dp(this,34)));
            else {TextView icon=Ui.text(this,tile.icon,25,Ui.BLUE,true);card.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,34),Ui.dp(this,34)));}
            TextView label = Ui.text(this, tile.label, 15, Ui.BLACK, true);
            Ui.marginTop(label, this, 8);
            card.addView(label);
            card.setOnClickListener(v -> {if(editing)rename(layout.optJSONObject("tileTitles"),tile.id,"Kacheltitel");else tile.action.run();});
            if(editing){
                org.json.JSONArray order=layout.optJSONObject("tileOrder").optJSONArray(tile.section);
                int position=0;for(int n=0;n<order.length();n++)if(order.optString(n).equals(tile.id))position=n;
                addMoveButtons(card,order,position,()->redraw());
                card.setOnLongClickListener(v->v.startDragAndDrop(android.content.ClipData.newPlainText("tile",tile.id),new View.DragShadowBuilder(v),tile,0));
                card.setOnDragListener((v,event)->{if(event.getAction()==android.view.DragEvent.ACTION_DRAG_STARTED)return event.getLocalState() instanceof Tile && ((Tile)event.getLocalState()).section.equals(tile.section);if(event.getAction()==android.view.DragEvent.ACTION_DROP){Tile source=(Tile)event.getLocalState();moveTo(order,source.id,tile.id);redraw();return true;}return true;});
            }
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = Ui.dp(this, editing?180:126);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(Ui.dp(this, 4), Ui.dp(this, 5), Ui.dp(this, 4), Ui.dp(this, 5));
            grid.addView(card, params);
        }
        Ui.marginTop(grid, this, 8);
        content.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void loadStats() {
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                JSONObject stats = ApiClient.orderStats().getJSONObject("stats");
                main.post(() -> {
                    if(isDestroyed())return;
                    totalValue.setText(String.valueOf(stats.optInt("total")));
                    openValue.setText(String.valueOf(stats.optInt("open")));
                    completedValue.setText(String.valueOf(stats.optInt("completed")));
                    progress.setVisibility(View.GONE);
                });
            } catch (Exception error) {
                main.post(() -> { progress.setVisibility(View.GONE); Toast.makeText(this, "Übersicht konnte nicht geladen werden.", Toast.LENGTH_LONG).show(); });
            }
        });
    }

    private void openOrderSearch() {
        Intent intent = new Intent(this, BusinessActivity.class).putExtra("module","search");
        intent.putExtra("query", search.getText().toString().trim());
        startActivity(intent);
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Dashboard aktualisieren");
        popup.getMenu().add("Abmelden");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().toString().startsWith("Dashboard")) loadStats();
            else {
                SessionState.active=false;
                getSharedPreferences("session", MODE_PRIVATE).edit().clear().apply();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
            return true;
        });
        popup.show();
    }

    @Override protected void onResume() {
        super.onResume();
        if (initialized && progress != null) {loadStats();main.removeCallbacks(refreshNotices);main.post(refreshNotices);}
    }

    @Override protected void onDestroy() {
        main.removeCallbacks(refreshNotices);if(bellAnimation!=null)bellAnimation.cancel();
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override protected void onPause(){main.removeCallbacks(refreshNotices);if(bellAnimation!=null)bellAnimation.cancel();super.onPause();}
    private void redraw(){setContentView(buildDashboard());loadStats();}
    private void addOverview(LinearLayout content){
        TextView overview = Ui.text(this, "Übersicht", 18, Ui.BLACK, true);
        Ui.marginTop(overview, this, 22);
        content.addView(overview);
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setWeightSum(3);
        totalValue = addStat(stats, "Insgesamt", Ui.BLUE);
        openValue = addStat(stats, "Offen", Ui.ORANGE);
        completedValue = addStat(stats, "Abgeschlossen", Ui.GREEN);
        Ui.marginTop(stats, this, 10);
        content.addView(stats);

    }
    private void loadLayout(){executor.execute(()->{try{JSONObject remote=ApiClient.request("GET","/api/dashboard-layout",null).getJSONObject("layout");main.post(()->{if(isDestroyed()||editing)return;layout=remote;getSharedPreferences("app_preferences",0).edit().putString("dashboard_layout",remote.toString()).apply();redraw();});}catch(Exception error){main.post(()->Toast.makeText(this,"Gespeicherte Anordnung konnte nicht geladen werden.",Toast.LENGTH_LONG).show());}});}
    private void saveLayout(){final String snapshot=layout.toString();executor.execute(()->{try{JSONObject result=ApiClient.request("POST","/api/dashboard-layout",new JSONObject(snapshot));main.post(()->{if(isDestroyed())return;layout=result.optJSONObject("layout");editing=false;getSharedPreferences("app_preferences",0).edit().putString("dashboard_layout",layout.toString()).apply();redraw();Toast.makeText(this,"Dashboard-Anordnung gespeichert.",Toast.LENGTH_SHORT).show();});}catch(Exception e){main.post(()->Toast.makeText(this,"Speichern fehlgeschlagen. Deine Änderungen bleiben zum erneuten Speichern geöffnet.",Toast.LENGTH_LONG).show());}});}
    private void rename(JSONObject object,String key,String title){EditText field=Ui.input(this,title);field.setText(object.optString(key));new android.app.AlertDialog.Builder(this).setTitle(title).setView(field).setPositiveButton("Übernehmen",(d,w)->{String value=field.getText().toString().trim();if(!value.isEmpty()){try{object.put(key,value);}catch(Exception ignored){}redraw();}}).setNegativeButton("Abbrechen",null).show();}
    private void addMoveButtons(LinearLayout parent,org.json.JSONArray order,int index,Runnable changed){LinearLayout row=new LinearLayout(this);for(int direction:new int[]{-1,1}){Button b=Ui.button(this,direction<0?"↑":"↓",Ui.BLUE);b.setContentDescription(direction<0?"Nach oben verschieben":"Nach unten verschieben");b.setEnabled(index+direction>=0&&index+direction<order.length());b.setOnClickListener(v->{try{Object other=order.get(index+direction);order.put(index+direction,order.get(index));order.put(index,other);changed.run();}catch(Exception ignored){}});row.addView(b,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));}parent.addView(row);}
    private void moveTo(org.json.JSONArray order,String from,String to){java.util.List<String> ids=new java.util.ArrayList<>();for(int i=0;i<order.length();i++)ids.add(order.optString(i));int target=ids.indexOf(to);if(!ids.contains(from)||target<0)return;ids.remove(from);ids.add(target,from);for(int i=0;i<ids.size();i++)try{order.put(i,ids.get(i));}catch(Exception ignored){}}
    private void addConfiguredSection(LinearLayout content,String section){org.json.JSONArray ids=layout.optJSONObject("tileOrder").optJSONArray(section);if(ids==null)return;Tile[] tiles=new Tile[ids.length()];for(int i=0;i<ids.length();i++){String id=ids.optString(i);String title=layout.optJSONObject("tileTitles").optString(id,id);tiles[i]=new Tile(id,section,id.equals("orders")?"▤":id.equals("customer-link")?"✉":id.equals("offer")?"€":id.equals("work-cards")?"✓":"＋",title,()->{if(id.equals("new-order"))startActivity(new Intent(this,OrderFormActivity.class));else if(id.equals("orders"))startActivity(new Intent(this,OrdersActivity.class));else BusinessActivity.open(this, id.equals("new-work-card")?"work-card":id.equals("new-tire-storage")?"tire":id.equals("tire-storages")?"tires":id,null);});}String title=layout.optJSONObject("sectionTitles").optString(section,section);addSection(content,title,tiles);if(editing){Button rename=Ui.button(this,"Bereich umbenennen",Ui.MUTED);rename.setOnClickListener(v->rename(layout.optJSONObject("sectionTitles"),section,"Bereichstitel"));content.addView(rename);}}
    private void loadNotifications(){if(!initialized||notificationsLoading)return;notificationsLoading=true;executor.execute(()->{try{java.util.List<Notifications.Item> loaded=Notifications.load();main.post(()->{notificationsLoading=false;if(isDestroyed())return;notices=loaded;notificationError="";updateBell();});}catch(Exception error){main.post(()->{notificationsLoading=false;notificationError="Benachrichtigungen konnten nicht geladen werden. Bitte erneut versuchen.";updateBell();});}});}
    private void updateBell(){if(bellCount==null)return;if(bellAnimation!=null)bellAnimation.cancel();bellCount.setText(notificationError.isEmpty()?(notices.isEmpty()?"":String.valueOf(notices.size())):"!");bellCount.setVisibility(notices.isEmpty()&&notificationError.isEmpty()?View.GONE:View.VISIBLE);bellIcon.color(notices.isEmpty()?Color.WHITE:Color.rgb(248,90,90));bellIcon.setAlpha(1f);bellButton.setBackgroundColor(Color.TRANSPARENT);bellButton.setContentDescription(notificationError.isEmpty()?notices.size()+" Benachrichtigungen":notificationError);if(!notices.isEmpty()&&android.animation.ValueAnimator.areAnimatorsEnabled()){bellAnimation=android.animation.ValueAnimator.ofFloat(1f,0.3f,1f);bellAnimation.setDuration(2400);bellAnimation.setRepeatCount(-1);bellAnimation.addUpdateListener(a->{float f=(float)a.getAnimatedValue();bellIcon.setAlpha(f);});bellAnimation.start();}}
    private void showNotifications(){if(!notificationError.isEmpty()){new android.app.AlertDialog.Builder(this).setTitle("Benachrichtigungen").setMessage(notificationError).setPositiveButton("Erneut versuchen",(d,w)->loadNotifications()).setNegativeButton("Schließen",null).show();return;}
        if(notices.isEmpty()){new android.app.AlertDialog.Builder(this).setTitle("Benachrichtigungen").setMessage(notificationsLoading?"Wird geladen …":"Keine neuen Benachrichtigungen.").setPositiveButton("Schließen",null).show();return;}
        java.util.List<Notifications.Item> shown=new java.util.ArrayList<>(notices);String[] labels=new String[shown.size()];for(int i=0;i<labels.length;i++)labels[i]=shown.get(i).title+"\n"+shown.get(i).detail;
        new android.app.AlertDialog.Builder(this).setTitle("Benachrichtigungen").setItems(labels,(d,n)->{Notifications.Item item=shown.get(n);if(item.tire)BusinessActivity.open(this,"tire",item.id);else startActivity(new Intent(this,OrderDetailActivity.class).putExtra("order_id",item.id));}).setNegativeButton("Schließen",null).setNeutralButton("Benachrichtigungen leeren",(d,w)->{executor.execute(()->{try{org.json.JSONArray ids=new org.json.JSONArray();for(Notifications.Item item:shown)ids.put(item.key);ApiClient.request("POST","/api/notifications",FormActivity.json("ids",ids));main.post(()->loadNotifications());}catch(Exception e){main.post(()->Toast.makeText(this,"Benachrichtigungen konnten nicht geleert werden.",Toast.LENGTH_LONG).show());}});}).show();
    }
    private static final class Tile {
        final String id,section,icon,label;final Runnable action;
        Tile(String id,String section,String icon,String label,Runnable action){this.id=id;this.section=section;this.icon=icon;this.label=label;this.action=action;}
    }
}
