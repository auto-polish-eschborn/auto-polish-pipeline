package de.autopolish.nativeapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;
import org.json.*;

public class SettingsActivity extends FormActivity {
    private static final String[] RIGHTS={"orders_view","orders_edit","orders_delete","workcards","offers","settings"};
    private static final String[] LABELS={"Aufträge ansehen","Aufträge bearbeiten","Aufträge löschen","Arbeitskarten","Angebote","Einstellungen"};
    @Override protected void onCreate(Bundle state){super.onCreate(state);shell("Einstellungen");
        title("Darstellung der App");
        Switch dark=new Switch(this);dark.setText("Dark Mode");dark.setTextSize(17);dark.setTextColor(Ui.ink(this));dark.setPadding(0,Ui.dp(this,16),0,Ui.dp(this,16));dark.setChecked(Ui.dark(this));content.addView(dark);
        dark.setOnCheckedChangeListener((v,on)->{getSharedPreferences("app_preferences",0).edit().putBoolean("dark",on).apply();recreate();});
        note("Diese Einstellung gilt nur für die App auf diesem Handy.");
        title("Benachrichtigungen");note("Die Glocke lädt neue Kundenantworten und fällige Radeinlagerungen automatisch. Push bei geschlossener App ist noch nicht eingerichtet.");
        title("Farbe der Oberfläche");String[] colors={"blue","red","green","black"};String[] names={"Blau","Rot","Grün","Schwarz"};Spinner color=new Spinner(this);color.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));content.addView(color);
        call("GET","/api/settings/appearance",null,r->{for(int i=0;i<colors.length;i++)if(colors[i].equals(r.optString("color")))color.setSelection(i);});
        action("Farbe speichern",()->call("POST","/api/settings/appearance",json("color",colors[color.getSelectedItemPosition()]),r->{getSharedPreferences("app_preferences",0).edit().putString("accent",colors[color.getSelectedItemPosition()]).apply();message("Farbe wurde gespeichert.");recreate();}));
        title("System-Passwort");EditText current=field("Bisheriges System-Passwort","",true),next=field("Neues System-Passwort","",true),confirm=field("Neues System-Passwort wiederholen","",true);
        action("System-Passwort speichern",()->{if(value(next).length()<4||!value(next).equals(value(confirm))){message("Neue Passwörter müssen übereinstimmen und mindestens 4 Zeichen haben.");return;}call("POST","/api/settings/discount-password",json("currentPassword",value(current),"newPassword",value(next)),r->{current.setText("");next.setText("");confirm.setText("");message("System-Passwort gespeichert.");});});
        title("Auto-Polish Kalender");EditText calendar=field("Google Kalender-ID","",false),zone=field("Zeitzone","Europe/Berlin",false);Spinner view=new Spinner(this);String[] views={"MONTH","WEEK","AGENDA"};view.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Monat","Woche","Agenda"}));content.addView(view);CheckBox weekends=new CheckBox(this);weekends.setText("Wochenenden anzeigen");weekends.setTextColor(Ui.ink(this));content.addView(weekends);
        call("GET","/api/settings/calendar",null,r->{JSONObject s=r.getJSONObject("settings");calendar.setText(s.optString("calendarId"));zone.setText(s.optString("timezone","Europe/Berlin"));weekends.setChecked(s.optBoolean("showWeekends",true));for(int i=0;i<views.length;i++)if(views[i].equals(s.optString("defaultView")))view.setSelection(i);});
        action("Kalender speichern",()->call("POST","/api/settings/calendar",json("calendarId",value(calendar),"timezone",value(zone),"defaultView",views[view.getSelectedItemPosition()],"showWeekends",weekends.isChecked()),r->message("Kalender-Einstellungen gespeichert.")));
        title("Benutzer & Rechte");action("Benutzer verwalten",this::users);
    }
    private void users(){call("GET","/api/users",null,r->{JSONArray a=r.getJSONArray("users");String[] labels=new String[a.length()+1];for(int i=0;i<a.length();i++){JSONObject u=a.getJSONObject(i);labels[i]=u.optString("username")+" · "+u.optString("role")+" · "+u.optString("status");}labels[a.length()]="Benutzer hinzufügen";new AlertDialog.Builder(this).setTitle("Benutzer & Rechte").setItems(labels,(d,n)->editUser(n<a.length()?a.optJSONObject(n):null)).setNegativeButton("Schließen",null).show();});}
    private void editUser(JSONObject user){
        LinearLayout form=Ui.vertical(this,18);EditText email=Ui.input(this,"E-Mail-Adresse"),role=Ui.input(this,"Rolle");email.setInputType(33);email.setText(user==null?"":user.optString("email"));email.setEnabled(user==null);role.setText(user==null?"Mitarbeiter":user.optString("role"));form.addView(email);form.addView(role);
        CheckBox[] boxes=new CheckBox[RIGHTS.length];String old=user==null||user.optJSONArray("permissions")==null?"":user.optJSONArray("permissions").toString();for(int i=0;i<boxes.length;i++){boxes[i]=new CheckBox(this);boxes[i].setText(LABELS[i]);boxes[i].setChecked(old.contains("\""+RIGHTS[i]+"\""));form.addView(boxes[i]);}
        CheckBox blocked=new CheckBox(this);blocked.setText("Benutzer sperren");blocked.setChecked(user!=null&&"blocked".equals(user.optString("status")));if(user!=null)form.addView(blocked);
        if(user!=null){Button remove=Ui.button(this,"Benutzer löschen",Ui.RED);form.addView(remove);remove.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Benutzer löschen?").setMessage(user.optString("username")+" wird aus der Benutzerverwaltung entfernt.").setNegativeButton("Abbrechen",null).setPositiveButton("Löschen",(d,n)->call("DELETE","/api/users",json("id",user.optString("id")),r->{message("Benutzer gelöscht.");recreate();})).show());}
        ScrollView scroll=new ScrollView(this);scroll.addView(form);AlertDialog dialog=new AlertDialog.Builder(this).setTitle(user==null?"Benutzer hinzufügen":"Benutzer bearbeiten").setView(scroll).setPositiveButton("Speichern",null).setNegativeButton("Abbrechen",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{JSONArray rights=new JSONArray();for(int i=0;i<boxes.length;i++)if(boxes[i].isChecked())rights.put(RIGHTS[i]);JSONObject body=json("email",value(email),"role",value(role),"permissions",rights);if(user!=null){try{body.put("id",user.getString("id"));body.put("status",blocked.isChecked()?"blocked":("blocked".equals(user.optString("status"))?"active":user.optString("status")));}catch(Exception e){message(e.getMessage());return;}}
            call(user==null?"POST":"PUT","/api/users",body,r->{dialog.dismiss();String link=r.getJSONObject("user").optString("inviteLink");if(!link.isEmpty()){TextView text=Ui.text(this,link,16,Ui.BLACK,false);text.setTextIsSelectable(true);new AlertDialog.Builder(this).setTitle("Einladungslink").setView(text).setPositiveButton("Schließen",null).show();}else message("Benutzer gespeichert.");});}));dialog.show();
    }
}
