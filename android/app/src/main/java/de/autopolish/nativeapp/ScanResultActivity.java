package de.autopolish.nativeapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import org.json.*;

/** Resolve a scanned document number before opening a saved form. */
public class ScanResultActivity extends FormActivity {
    private String code;
    @Override protected void onCreate(Bundle state){super.onCreate(state);code=getIntent().getStringExtra("barcode");if(code==null||code.trim().isEmpty()){finish();return;}code=code.trim();load();}
    private void load(){shell("Formular öffnen");note("Suche nach "+code+" …");requests.execute(()->{try{JSONArray candidates;if(code.toUpperCase(java.util.Locale.ROOT).startsWith("RAD-")){JSONArray records=ApiClient.request("GET","/api/tire-storages?q="+Uri.encode(code),null).getJSONArray("records");candidates=new JSONArray();for(int n=0;n<records.length();n++){JSONObject r=records.getJSONObject(n);candidates.put(json("id",r.getString("id"),"number",r.optString("storageNumber"),"kind","Radeinlagerung","subtitle",r.optString("firstName")+" "+r.optString("lastName")));}}else candidates=ApiClient.request("GET","/api/global-search?q="+Uri.encode(code),null).getJSONArray("results");JSONArray exact=ScanMatch.exact(candidates,code);runOnUiThread(()->{if(isFinishing()||isDestroyed())return;try{if(exact.length()==1){open(exact.getJSONObject(0));return;}shell("Formular öffnen");note(exact.length()==0?"Kein Formular mit der Nummer "+code+" gefunden.":"Mehrere Formulare verwenden diese Nummer. Bitte auswählen:");for(int n=0;n<exact.length();n++){JSONObject r=exact.getJSONObject(n);action(r.optString("kind")+" · "+r.optString("number")+"\n"+r.optString("subtitle"),()->open(r));}action("Suchergebnisse anzeigen",()->{startActivity(new Intent(this,BusinessActivity.class).putExtra("module","search").putExtra("query",code));finish();});}catch(Exception e){failure();}});}catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())failure();});}});}
    private void failure(){shell("Formular öffnen");note("Das Formular konnte nicht geladen werden. Bitte prüfe die Internetverbindung.");action("Erneut versuchen",this::load);}
    private void open(JSONObject r){String kind=r.optString("kind");if(kind.equals("Auftrag"))startActivity(new Intent(this,OrderDetailActivity.class).putExtra("order_id",r.optString("id")));else BusinessActivity.open(this,kind.equals("Radeinlagerung")?"tire":"work-card",r.optString("id"));finish();}
}
