package de.autopolish.nativeapp;
import java.math.*;
import org.json.*;

final class BusinessData {
    static BigDecimal money(String raw) {
        String value=raw.trim().replace("€","").replace(" ","");
        if(value.isEmpty())return BigDecimal.ZERO.setScale(2);
        if(value.contains(","))value=value.replace(".","").replace(',','.');
        try {BigDecimal n=new BigDecimal(value).setScale(2,RoundingMode.UNNECESSARY);if(n.signum()<0||n.compareTo(new BigDecimal("99999999.99"))>0)throw new NumberFormatException();return n;}
        catch(Exception e){throw new IllegalArgumentException("Bitte einen gültigen, nicht negativen Preis mit höchstens zwei Nachkommastellen eingeben.");}
    }
    static JSONObject totals(JSONArray items,int discount) throws Exception {
        if(discount<0||discount>100)throw new IllegalArgumentException("Rabatt muss zwischen 0 und 100 liegen.");
        BigDecimal subtotal=BigDecimal.ZERO;for(int i=0;i<items.length();i++)subtotal=subtotal.add(money(items.getJSONObject(i).get("price").toString()));
        BigDecimal net=subtotal.multiply(BigDecimal.valueOf(100-discount)).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
        BigDecimal vat=net.multiply(new BigDecimal("0.19")).setScale(2,RoundingMode.HALF_UP);
        JSONObject result=new JSONObject();result.put("net",net.doubleValue());result.put("vat",vat.doubleValue());result.put("gross",net.add(vat).doubleValue());return result;
    }
    static JSONArray array(JSONObject o,String key,String fallback) {
        JSONArray a=o.optJSONArray(key);if(a!=null)return a;
        try{return new JSONArray(o.optString(fallback,"[]"));}catch(Exception e){return new JSONArray();}
    }
}
