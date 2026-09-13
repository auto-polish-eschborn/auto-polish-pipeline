package de.autopolish.nativeapp;

import android.os.Bundle;
import android.widget.*;

public class ProfileActivity extends FormActivity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);shell("Mein Profil");title("Benutzerkonto");note("Benutzername und persönliches Anmeldepasswort ändern.");
        EditText user=field("Benutzername",getSharedPreferences("session",0).getString("username",""),false);
        EditText current=field("Bisheriges Benutzerpasswort oder System-Passwort","",true);
        EditText next=field("Neues Benutzerpasswort (optional)","",true);
        EditText confirm=field("Neues Benutzerpasswort wiederholen","",true);
        Button save=action("Profil speichern",()->{
            if(value(user).trim().length()<2||value(current).isEmpty()){message("Bitte Benutzername und bisheriges Passwort eingeben.");return;}
            if(!value(next).equals(value(confirm))||(!value(next).isEmpty()&&value(next).length()<4)){message("Neue Passwörter müssen übereinstimmen und mindestens 4 Zeichen haben.");return;}
            call("POST","/api/profile",json("username",value(user).trim(),"currentPassword",value(current),"newPassword",value(next)),r->{
                getSharedPreferences("session",0).edit().putString("username",r.getString("username")).apply();current.setText("");next.setText("");confirm.setText("");message("Profil wurde gespeichert.");
            });
        });
        note("Das Benutzerpasswort ist für die Anmeldung. Das System-Passwort bleibt separat für Rabatte, Löschen und geschützte Freigaben.");
        call("GET","/api/profile",null,r->user.setText(r.optString("username",value(user))));
    }
}
