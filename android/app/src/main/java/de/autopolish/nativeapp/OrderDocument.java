package de.autopolish.nativeapp;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import org.json.*;
import java.io.*;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;

/** Each service consumes its measured height before the totals block begins. */
final class OrderDocument {
    private final PdfDocument doc=new PdfDocument();
    private final Paint ink=new Paint(3);
    private Canvas canvas;
    private PdfDocument.Page page;
    private int pages;
    private float y;
    private JSONObject config,order;
    private void begin()throws Exception{page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,++pages).create());canvas=page.getCanvas();canvas.drawColor(Color.WHITE);y=35;line(config.optString("companyName","Auto-Polish"),19,true,350);line(config.optString("subtitle","Fahrzeugaufbereitung"),10,false,350);line(config.optString("street")+" · "+config.optString("postalCity"),9,false,350);line(config.optString("phone")+" · "+config.optString("email"),9,false,350);String number=order.optString("orderNumber");BitMatrix bars=new com.google.zxing.oned.Code128Writer().encode(number,BarcodeFormat.CODE_128,160,30);ink.setColor(Color.BLACK);for(int x=0;x<bars.getWidth();x++)if(bars.get(x,0))canvas.drawRect(395+x,25,396+x,55,ink);ink.setTextSize(10);canvas.drawText(number,395,70,ink);y=Math.max(y+10,110);line("Auftragsformular",17,true,523);}
    private void end(){ink.setColor(Color.GRAY);ink.setTextSize(9);canvas.drawText(order.optString("orderNumber")+" · Seite "+pages,36,817,ink);doc.finishPage(page);}
    private void room(float height)throws Exception{if(y+height>785){end();begin();}}
    private void line(String value,float size,boolean bold,float width)throws Exception{ink.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));ink.setTextSize(size);ink.setColor(Color.rgb(25,30,38));for(String paragraph:value.split("\n",-1)){String rest=paragraph;do{int n=ink.breakText(rest,true,width,null);if(n==0&&!rest.isEmpty())n=1;room(size+5);ink.setTextSize(size);ink.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));canvas.drawText(rest.substring(0,n),36,y,ink);y+=size+5;rest=rest.substring(n);}while(!rest.isEmpty());}}
    private static String money(long cents){return String.format(java.util.Locale.GERMANY,"%,.2f €",cents/100.0);}
    private void amount(String label,long cents,boolean total)throws Exception{room(24);ink.setTextSize(total?14:11);ink.setTypeface(Typeface.create("sans-serif",total?Typeface.BOLD:Typeface.NORMAL));ink.setColor(Color.BLACK);canvas.drawText(label,350,y,ink);String price=money(cents);canvas.drawText(price,559-ink.measureText(price),y,ink);y+=total?25:19;}
    static void write(File target,JSONObject order,JSONObject config,TireDocument.Images images)throws Exception{OrderDocument d=new OrderDocument();d.order=order;d.config=config;try{d.begin();d.line("Auftraggeber",12,true,523);d.line(order.optString("firstName")+" "+order.optString("lastName"),11,false,523);d.line(order.optString("street")+" · "+order.optString("postalCode")+" "+order.optString("city"),10,false,523);d.line(order.optString("phone")+" · "+order.optString("email"),10,false,523);d.y+=8;d.line("Fahrzeug",12,true,523);d.line(order.optString("vehicle")+" · "+order.optString("licensePlate")+" · Kilometer: "+order.optString("mileage"),11,false,523);d.line("FIN: "+order.optString("vin")+" · Status: "+order.optString("status"),10,false,523);d.y+=12;d.line("Dienstleistungen",12,true,523);JSONArray services=BusinessData.array(order,"services","servicesJson");for(int n=0;n<services.length();n++){JSONObject service=services.getJSONObject(n);d.room(40);float top=d.y;d.ink.setTextSize(11);String price=BusinessData.money(service.get("price").toString()).toPlainString().replace('.',',')+" €";d.canvas.drawText(price,559-d.ink.measureText(price),top,d.ink);d.line(service.optString("name"),11,false,405);d.y+=8;}
        d.room(110);d.y+=12;d.ink.setColor(Color.LTGRAY);d.canvas.drawLine(36,d.y-9,559,d.y-9,d.ink);if(order.optLong("discountCents")>0)d.amount("Rabatt "+order.optInt("discountPercent")+" %",-order.optLong("discountCents"),false);d.amount("Netto",order.optLong("netCents"),false);d.amount("19 % MwSt.",order.optLong("vatCents"),false);d.amount("Gesamtpreis",order.optLong("grossCents"),true);d.y+=10;
        d.line("Zahlungsart:  □ Barzahlung    □ EC    □ Überweisung (Firmenkunden)",10,false,523);String key=order.optString("signatureKey");if(!key.isEmpty()){Bitmap b=images.load(key);try{d.room(115);d.line("Gespeicherte Unterschrift Auftraggeber",10,true,523);float scale=Math.min(270f/b.getWidth(),70f/b.getHeight());d.canvas.drawBitmap(b,null,new RectF(36,d.y,36+b.getWidth()*scale,d.y+b.getHeight()*scale),d.ink);d.y+=b.getHeight()*scale+15;}finally{b.recycle();}}
        d.room(115);d.line("Hinweis zu Storno & Anzahlung",10,true,523);d.line("Absagen oder Stornierungen, die nicht mindestens 24 Stunden vorher erfolgen, werden mit 20 % des Auftragswertes berechnet.",9,false,523);d.line("Bei Bestellungen von Teilen, Sonderanfertigungen sowie Lackier- oder Karosseriearbeiten ist eine Anzahlung von 50 % vor Beginn fällig.",9,false,523);d.line("Mit der Unterschrift bestätigt der Auftraggeber die Kenntnisnahme und Zustimmung zu den Allgemeinen Geschäftsbedingungen.",9,false,523);d.end();try(OutputStream out=new FileOutputStream(target)){d.doc.writeTo(out);}}finally{d.doc.close();}}
}
