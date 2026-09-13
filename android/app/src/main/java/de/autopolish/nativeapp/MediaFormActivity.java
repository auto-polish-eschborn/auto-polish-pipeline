package de.autopolish.nativeapp;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;

public abstract class MediaFormActivity extends FormActivity {
    protected final ArrayList<String> photos=new ArrayList<>();
    protected String registration="",signature="";
    protected SignaturePad pad;
    private String pendingKind="photo",cameraUri="";
    private LinearLayout mediaRows;
    private final Map<String,String> uploaded=new HashMap<>();
    @Override protected void onCreate(Bundle state){super.onCreate(state);if(state!=null){photos.addAll(state.getStringArrayList("mediaPhotos")==null?new ArrayList<>():state.getStringArrayList("mediaPhotos"));registration=state.getString("mediaRegistration","");signature=state.getString("mediaSignature","");pendingKind=state.getString("pendingKind","photo");cameraUri=state.getString("cameraUri","");}}
    @Override protected void onSaveInstanceState(Bundle out){out.putStringArrayList("mediaPhotos",photos);out.putString("mediaRegistration",registration);out.putString("mediaSignature",signature);out.putString("pendingKind",pendingKind);out.putString("cameraUri",cameraUri);if(pad!=null&&pad.hasInk()){try{File f=new File(SharedFiles.directory(this),"signature-"+UUID.randomUUID()+".png");try(FileOutputStream stream=new FileOutputStream(f)){stream.write(pad.png());}signature=Uri.fromFile(f).toString();out.putString("mediaSignature",signature);}catch(Exception ignored){}}super.onSaveInstanceState(out);}
    protected void media(boolean hasRegistration,boolean hasSignature){
        title("Fotos und Dokumente");
        if(hasRegistration)action("Fahrzeugschein aufnehmen / auswählen",()->choose("registration"));
        action("Fotos aufnehmen / auswählen",()->choose("photo"));
        mediaRows=Ui.vertical(this,0);content.addView(mediaRows);refreshMedia();
        if(hasSignature){title("Unterschrift");note(signature.isEmpty()?"Bitte hier unterschreiben.":"Eine gespeicherte Unterschrift ist vorhanden. Zum Ersetzen neu unterschreiben.");if(!signature.isEmpty())action("Gespeicherte Unterschrift ansehen",()->preview(signature));pad=new SignaturePad();content.addView(pad,new LinearLayout.LayoutParams(-1,Ui.dp(this,170)));action("Neue Unterschrift löschen",()->pad.clear());}
    }
    private void takePhoto(){if(checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.CAMERA},405);return;}try{File f=new File(SharedFiles.directory(this),"photo-"+UUID.randomUUID()+".jpg");f.createNewFile();Uri uri=SharedFiles.uri(this,f);cameraUri=uri.toString();Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);i.setClipData(ClipData.newRawUri("Foto",uri));startActivityForResult(i,401);}catch(Exception e){message("Kamera konnte nicht geöffnet werden.");}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==405&&g.length>0&&g[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)takePhoto();}
    private void choose(String kind){pendingKind=kind;new AlertDialog.Builder(this).setTitle("Bild hinzufügen").setItems(new String[]{"Kamera","Bilder auswählen"},(d,n)->{try{if(n==0){takePhoto();}else{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,kind.equals("photo"));startActivityForResult(i,402);}}catch(Exception e){message("Kamera oder Bildauswahl konnte nicht geöffnet werden.");}}).show();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK)return;if(request==401){accept(Uri.parse(cameraUri));}else if(request==402&&data!=null){if(data.getClipData()!=null){for(int n=0;n<data.getClipData().getItemCount();n++)accept(data.getClipData().getItemAt(n).getUri());}else if(data.getData()!=null)accept(data.getData());}refreshMedia();}
    private void accept(Uri uri){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}if(pendingKind.equals("registration"))registration=uri.toString();else if(!photos.contains(uri.toString()))photos.add(uri.toString());}
    protected void refreshMedia(){if(mediaRows==null)return;mediaRows.removeAllViews();if(!registration.isEmpty())mediaRow("Fahrzeugschein",registration,()->{registration="";refreshMedia();});for(int n=0;n<photos.size();n++){String ref=photos.get(n);mediaRow("Foto "+(n+1),ref,()->{photos.remove(ref);refreshMedia();});}}
    private void mediaRow(String title,String ref,Runnable remove){LinearLayout row=Ui.vertical(this,6);row.setOrientation(LinearLayout.HORIZONTAL);Button view=Ui.button(this,title,Ui.BLUE);view.setOnClickListener(v->preview(ref));row.addView(view,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));Button del=Ui.button(this,"×",Ui.RED);del.setContentDescription(title+" entfernen");del.setOnClickListener(v->remove.run());row.addView(del,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));mediaRows.addView(row);}
    protected void preview(String ref){requests.execute(()->{try{Bitmap image=image(ref,1200);runOnUiThread(()->{if(isDestroyed())return;ImageView v=new ImageView(this);v.setImageBitmap(image);v.setAdjustViewBounds(true);new AlertDialog.Builder(this).setView(v).setPositiveButton("Schließen",(d,n)->image.recycle()).show();});}catch(Exception e){runOnUiThread(()->message("Bild konnte nicht angezeigt werden."));}});}
    private InputStream stream(String ref)throws Exception{if(ref.startsWith("content:")||ref.startsWith("file:"))return getContentResolver().openInputStream(Uri.parse(ref));return new ByteArrayInputStream(ApiClient.download("/api/files?key="+Uri.encode(ref),12*1024*1024));}
    protected Bitmap image(String ref,int max)throws Exception{BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;try(InputStream in=stream(ref)){BitmapFactory.decodeStream(in,null,options);}if(options.outWidth<=0)throw new IOException("Ungültiges Bild.");options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>max*2)options.inSampleSize*=2;options.inJustDecodeBounds=false;Bitmap bitmap;try(InputStream in=stream(ref)){bitmap=BitmapFactory.decodeStream(in,null,options);}if(bitmap==null)throw new IOException("Bild nicht lesbar.");
        try(InputStream in=stream(ref)){android.media.ExifInterface exif=new android.media.ExifInterface(in);int orientation=exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1);Matrix m=new Matrix();switch(orientation){case 2:m.setScale(-1,1);break;case 3:m.setRotate(180);break;case 4:m.setScale(1,-1);break;case 5:m.setRotate(90);m.postScale(-1,1);break;case 6:m.setRotate(90);break;case 7:m.setRotate(-90);m.postScale(-1,1);break;case 8:m.setRotate(-90);break;}if(!m.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,true);if(rotated!=bitmap){bitmap.recycle();bitmap=rotated;}}}catch(IOException ignored){}
        float scale=Math.min(1f,max/(float)Math.max(bitmap.getWidth(),bitmap.getHeight()));if(scale<1){Bitmap scaled=Bitmap.createScaledBitmap(bitmap,Math.max(1,Math.round(bitmap.getWidth()*scale)),Math.max(1,Math.round(bitmap.getHeight()*scale)),true);if(scaled!=bitmap){bitmap.recycle();bitmap=scaled;}}return bitmap;
    }
    protected String uploadRef(String ref,String id,String kind)throws Exception{if(ref.isEmpty()||(!ref.startsWith("content:")&&!ref.startsWith("file:")))return ref;if(uploaded.containsKey(ref))return uploaded.get(ref);Bitmap bitmap=image(ref,1600);ByteArrayOutputStream out=new ByteArrayOutputStream();boolean png=kind.contains("signature")||kind.equals("logo");bitmap.compress(png?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.JPEG,82,out);bitmap.recycle();String key=ApiClient.upload(out.toByteArray(),id,kind,png?"image/png":"image/jpeg");uploaded.put(ref,key);return key;}
    protected MediaSnapshot snapshot(){return new MediaSnapshot(new ArrayList<>(photos),registration,signature,pad!=null&&pad.hasInk()?pad.png():null);}
    protected final class MediaSnapshot {
        final ArrayList<String> refs;final String reg,sig;final byte[] ink;
        MediaSnapshot(ArrayList<String> refs,String reg,String sig,byte[] ink){this.refs=refs;this.reg=reg;this.sig=sig;this.ink=ink;}
        void apply(JSONObject model,String id,String photoField,boolean includeRegistration)throws Exception{JSONArray keys=new JSONArray();for(String ref:refs)keys.put(uploadRef(ref,id,"photo"));model.put(photoField,keys);if(includeRegistration)model.put("vehicleRegistrationKey",uploadRef(reg,id,"registration"));String key=ink!=null?ApiClient.upload(ink,id,"signature","image/png"):uploadRef(sig,id,"signature");if(!key.isEmpty())model.put("signatureKey",key);}
    }
    protected class SignaturePad extends View {
        private final Path path=new Path();private final Paint paint=new Paint(3);private boolean ink;
        SignaturePad(){super(MediaFormActivity.this);setBackgroundColor(Color.WHITE);setContentDescription("Unterschriftsfeld");paint.setColor(Color.BLACK);paint.setStrokeWidth(Ui.dp(MediaFormActivity.this,3));paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);}
        protected void onDraw(Canvas c){c.drawPath(path,paint);}
        public boolean onTouchEvent(android.view.MotionEvent e){getParent().requestDisallowInterceptTouchEvent(true);switch(e.getAction()){case MotionEvent.ACTION_DOWN:path.moveTo(e.getX(),e.getY());break;case MotionEvent.ACTION_MOVE:path.lineTo(e.getX(),e.getY());ink=true;break;case MotionEvent.ACTION_UP:getParent().requestDisallowInterceptTouchEvent(false);performClick();break;}invalidate();return true;}
        public boolean performClick(){super.performClick();return true;}
        boolean hasInk(){return ink;}void clear(){path.reset();ink=false;invalidate();}
        byte[] png(){Bitmap b=Bitmap.createBitmap(Math.max(1,getWidth()),Math.max(1,getHeight()),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);c.drawPath(path,paint);ByteArrayOutputStream out=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,out);b.recycle();return out.toByteArray();}
    }
}
