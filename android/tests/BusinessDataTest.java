package de.autopolish.nativeapp;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class BusinessDataTest {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    interface Checked { void run()throws Exception; }
    private static void fails(Checked action,String name)throws Exception{try{action.run();throw new AssertionError(name+" was accepted");}catch(IllegalArgumentException|IOException expected){checks++;}}
    public static void main(String[] args)throws Exception {
        check(BusinessData.money("1.234,56 €").toPlainString().equals("1234.56"),"German price");
        check(BusinessData.money("12.50").toPlainString().equals("12.50"),"Decimal point");
        fails(()->BusinessData.money("-1"),"negative price");fails(()->BusinessData.money("NaN"),"NaN");fails(()->BusinessData.money("1.001"),"fractional cents");fails(()->BusinessData.money("Infinity"),"infinity");
        JSONArray items=new JSONArray("[{\"name\":\"Politur\",\"price\":100},{\"name\":\"Innenraum\",\"price\":50}]");
        JSONObject totals=BusinessData.totals(items,0);check(totals.getDouble("net")==150,"net");check(totals.getDouble("vat")==28.5,"VAT");check(totals.getDouble("gross")==178.5,"gross");
        totals=BusinessData.totals(items,10);check(totals.getDouble("net")==135,"discounted net");check(totals.getDouble("gross")==160.65,"discounted gross");
        check(BusinessData.totals(items,100).getDouble("gross")==0,"full discount");fails(()->BusinessData.totals(items,101),"invalid discount");
        check(BusinessData.array(new JSONObject("{\"photoKeysJson\":\"[\\\"existing-photo\\\"]\"}"),"photoKeys","photoKeysJson").getString(0).equals("existing-photo"),"preserve legacy photo array");
        check(BusinessData.array(new JSONObject("{\"photoKeys\":[\"current\"],\"photoKeysJson\":\"[]\"}"),"photoKeys","photoKeysJson").getString(0).equals("current"),"prefer parsed photo array");
        final Fake[] last={null};final String[] path={null};ApiClient.connections=p->{path[0]=p;return last[0]=new Fake(200,"{\"saved\":true}");};
        ApiClient.request("POST","/api/notes",new JSONObject("{\"content\":\"Ölwechsel prüfen\"}"),"secret-test");
        check(path[0].equals("/api/notes"),"notes endpoint");check(last[0].getRequestProperty("x-system-password").equals("secret-test"),"notes password header");check(new JSONObject(last[0].body.toString("UTF-8")).getString("content").equals("Ölwechsel prüfen"),"UTF8 body");check(last[0].closed,"connection closed");
        JSONObject payload=new JSONObject().put("kind","offer").put("payload",new JSONObject().put("firstName","Test").put("lastName","Kunde").put("items",items).put("net",150).put("vat",28.5).put("gross",178.5));
        ApiClient.request("POST","/api/portal-links",payload);check(new JSONObject(last[0].body.toString("UTF-8")).getJSONObject("payload").getJSONArray("items").length()==2,"offer payload preserved");
        ApiClient.connections=p->{path[0]=p;return last[0]=new Fake(200,"{\"key\":\"stored-photo\"}");};String key=ApiClient.upload(new byte[]{1,2,3},"12345678-1234-1234-1234-123456789012","signature","image/png");
        check(key.equals("stored-photo"),"upload key");check(path[0].equals("/api/uploads"),"upload path");String multipart=last[0].body.toString("UTF-8");check(multipart.contains("name=\"orderId\"")&&multipart.contains("name=\"kind\"")&&multipart.contains("name=\"file\""),"multipart fields");check(multipart.contains("Content-Type: image/png"),"signature content type");fails(()->ApiClient.upload(new byte[4*1024*1024+1],"id","photo","image/jpeg"),"upload size");
        ApiClient.connections=p->new Fake(403,"{\"error\":\"Das System-Passwort ist nicht richtig.\"}");try{ApiClient.request("POST","/api/notes",new JSONObject());throw new AssertionError("403 ignored");}catch(IOException expected){check(expected.getMessage().contains("System-Passwort"),"server error shown");}
        fails(()->ApiClient.download("https://unrelated.example",10),"foreign download path");
        ApiClient.connections=p->new Fake(200,"01234567890");fails(()->ApiClient.download("/api/files?key=test",5),"download size cap");
        System.out.println(checks+" checks passed. No live records created or modified.");
    }
    static final class Fake extends HttpURLConnection {
        final ByteArrayOutputStream body=new ByteArrayOutputStream();final int code;final byte[] response;boolean closed;
        Fake(int code,String text)throws MalformedURLException{super(new URL("https://example.invalid"));this.code=code;response=text.getBytes(StandardCharsets.UTF_8);}
        public void connect(){}public void disconnect(){closed=true;}public boolean usingProxy(){return false;}public int getResponseCode(){return code;}public OutputStream getOutputStream(){return body;}public InputStream getInputStream(){return new ByteArrayInputStream(response);}public InputStream getErrorStream(){return getInputStream();}
    }
}
