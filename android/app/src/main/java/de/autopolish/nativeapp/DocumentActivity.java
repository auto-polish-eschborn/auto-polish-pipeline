package de.autopolish.nativeapp;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import org.json.*;

/** Native detail view for tire reminders opened from the dashboard bell. */
public class DocumentActivity extends FormActivity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);shell("Radeinlagerung");String id=getIntent().getStringExtra("record_id");if(id==null){finish();return;}
        call("GET","/api/tire-storages?id="+Uri.encode(id),null,r->{JSONObject item=r.optJSONObject("record");if(item==null)throw new Exception("Einlagerung wurde nicht gefunden.");title(item.optString("storageNumber","Radeinlagerung"));
            String[][] fields={{"firstName","Vorname"},{"lastName","Nachname"},{"licensePlate","Kennzeichen"},{"storageDate","Einlagerungsdatum"},{"dueDate","Fälligkeit"},{"storageLocation","Lagerplatz"},{"phone","Telefon"},{"email","E-Mail"},{"notes","Notizen"}};
            for(String[] f:fields)if(!item.optString(f[0]).isEmpty())note(f[1]+": "+item.optString(f[0]));
            action("PDF öffnen",()->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(ApiClient.BASE_URL+"/api/tire-storages/"+Uri.encode(id)+"/pdf"))));});
    }
}
