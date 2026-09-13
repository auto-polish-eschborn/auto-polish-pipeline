package de.autopolish.nativeapp;
import org.json.*;
import java.util.*;

final class Notifications {
    static final class Item {
        final String key,id,title,detail;final boolean tire;
        Item(JSONObject o,boolean tire){this.tire=tire;id=o.optString("id");key=tire?"tire:"+id+":"+o.optString("dueDate"):"customer:"+id;
            title=(tire?"6 Monate · ":"Neu · ")+o.optString(tire?"storageNumber":"orderNumber");
            detail=(o.optString("firstName")+" "+o.optString("lastName")).trim()+" · "+o.optString("licensePlate")+"\n"+(tire?"Fällig seit: ":"Eingegangen: ")+o.optString(tire?"dueDate":"usedAt");}
    }
    static List<Item> load() throws Exception {
        Set<String> dismissed=new HashSet<>();JSONArray hidden=ApiClient.request("GET","/api/notifications",null).getJSONArray("dismissed");for(int i=0;i<hidden.length();i++)dismissed.add(hidden.getString(i));
        List<Item> result=new ArrayList<>();
        JSONArray replies=ApiClient.request("GET","/api/portal-links?notifications=1",null).getJSONArray("notifications");
        JSONArray tires=ApiClient.request("GET","/api/tire-storages?due=1",null).getJSONArray("records");
        for(int j=0;j<2;j++){JSONArray a=j==0?replies:tires;for(int i=0;i<a.length();i++){Item item=new Item(a.getJSONObject(i),j==1);if(!dismissed.contains(item.key))result.add(item);}}
        return result;
    }
}
