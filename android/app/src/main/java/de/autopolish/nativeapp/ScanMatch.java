package de.autopolish.nativeapp;
import org.json.*;
final class ScanMatch {
    static JSONArray exact(JSONArray candidates,String code)throws JSONException{JSONArray result=new JSONArray();for(int n=0;n<candidates.length();n++){JSONObject r=candidates.getJSONObject(n);if(!r.optString("id").isEmpty()&&r.optString("number").trim().equalsIgnoreCase(code.trim()))result.put(r);}return result;}
}
