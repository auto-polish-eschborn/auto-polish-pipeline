package de.autopolish.nativeapp;

import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import java.io.*;
import org.json.*;

/** Paginated copy of the saved record; independent of server PDF generation. */
final class TireDocument {
    interface Images { Bitmap load(String key) throws Exception; }
    private final PdfDocument doc=new PdfDocument();
    private PdfDocument.Page page;
    private Canvas canvas;
    private final Paint paint=new Paint(3);
    private int pages;
    private float y;
    private String number;
    private void begin(){page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,++pages).create());canvas=page.getCanvas();canvas.drawColor(Color.WHITE);paint.setColor(Color.rgb(17,19,24));canvas.drawRect(0,0,595,78,paint);paint.setColor(Color.WHITE);paint.setTextSize(22);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));canvas.drawText("Auto-Polish",36,36,paint);paint.setTextSize(11);canvas.drawText("Radeinlagerungsformular",36,58,paint);y=105;}
    private void end(){paint.setColor(Color.GRAY);paint.setTextSize(9);canvas.drawText("Auto-Polish · "+number+" · Seite "+pages,36,816,paint);doc.finishPage(page);}
    private void space(float h){if(y+h>780){end();begin();}}
    private void line(String text,boolean bold){paint.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));paint.setTextSize(bold?12:11);paint.setColor(bold?Color.rgb(21,84,209):Color.rgb(17,19,24));for(String paragraph:text.split("\n",-1)){String remaining=paragraph;do{int n=paint.breakText(remaining,true,523,null);if(n==0&&remaining.length()>0)n=1;space(17);canvas.drawText(remaining.substring(0,n),36,y,paint);y+=17;remaining=remaining.substring(n);}while(!remaining.isEmpty());}}
    private void field(JSONObject r,String label,String key){line(label,true);line(r.optString(key,"").isEmpty()?"—":r.optString(key),false);y+=7;}
    private void picture(String key,String label,Images images)throws Exception{if(key.isEmpty())return;Bitmap b=images.load(key);try{float scale=Math.min(523f/b.getWidth(),240f/b.getHeight());float h=b.getHeight()*scale;space(h+40);line(label,true);canvas.drawBitmap(b,null,new RectF(36,y,36+b.getWidth()*scale,y+h),paint);y+=h+20;}finally{b.recycle();}}
    static void write(File file,JSONObject r,Images images)throws Exception{TireDocument d=new TireDocument();d.number=r.optString("storageNumber");try{d.begin();String[][] fields={{"Einlagerungsnummer","storageNumber"},{"Status","status"},{"Einlagerungsdatum","storageDate"},{"Ausgelagert am","removedAt"},{"Vorname","firstName"},{"Nachname","lastName"},{"Straße","street"},{"PLZ","postalCode"},{"Ort","city"},{"Telefon","phone"},{"E-Mail","email"},{"Kennzeichen","licensePlate"},{"Fahrzeug","vehicle"},{"Saison","season"},{"Art","wheelType"},{"Hersteller","manufacturer"},{"DOT","dot"},{"Größe vorne","sizeFront"},{"Größe hinten","sizeRear"},{"Profiltiefe vorne links (mm)","treadFrontLeft"},{"Profiltiefe vorne rechts (mm)","treadFrontRight"},{"Profiltiefe hinten links (mm)","treadRearLeft"},{"Profiltiefe hinten rechts (mm)","treadRearRight"},{"Lagerplatz","storageLocation"}};for(String[] f:fields){d.space(55);d.field(r,f[0],f[1]);}d.line("Saisonpreis: "+java.math.BigDecimal.valueOf(r.optLong("priceCents"),2).toPlainString()+" €",true);d.line(r.optBoolean("damaged")?"Beschädigungen vorhanden":"Keine Beschädigungen vermerkt",true);d.field(r,"Hinweise", "notes");d.picture(r.optString("signatureKey"),"Gespeicherte Kundenunterschrift",images);JSONArray photos=BusinessData.array(r,"photoKeys","photoKeysJson");for(int n=0;n<photos.length();n++)d.picture(photos.getString(n),"Foto "+(n+1),images);d.end();try(OutputStream out=new FileOutputStream(file)){d.doc.writeTo(out);}}finally{d.doc.close();}}
}
