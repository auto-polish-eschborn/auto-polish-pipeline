package de.autopolish.nativeapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import java.util.*;
import java.util.concurrent.*;

/** Camera frames are decoded locally and never stored or uploaded. */
public class BarcodeActivity extends BaseActivity implements SurfaceHolder.Callback {
    private SurfaceView preview;
    private TextView hint;
    private Camera camera;
    private boolean resumed,ready,decoding,done;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    @Override protected void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);LinearLayout root=Ui.vertical(this,16);root.setBackgroundColor(Ui.surface(this));root.addView(Ui.text(this,"Barcode scannen",21,Ui.BLACK,true));hint=Ui.text(this,"Barcode vollständig ins Bild halten. Die Suche startet automatisch.",15,Ui.MUTED,false);root.addView(hint);preview=new SurfaceView(this);root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));preview.getHolder().addCallback(this);Button cancel=Ui.button(this,"Abbrechen",Ui.BLACK);cancel.setOnClickListener(v->finish());root.addView(cancel);setContentView(root);}
    @Override protected void onResume(){super.onResume();resumed=true;if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.CAMERA},41);else open();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] results){super.onRequestPermissionsResult(r,p,results);if(r==41){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)open();else hint.setText("Ohne Kamerafreigabe kannst du die Nummer weiterhin im Suchfeld eingeben.");}}
    private void open(){if(!resumed||!ready||camera!=null||done||checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;try{int id=-1;Camera.CameraInfo info=new Camera.CameraInfo();for(int n=0;n<Camera.getNumberOfCameras();n++){Camera.getCameraInfo(n,info);if(info.facing==Camera.CameraInfo.CAMERA_FACING_BACK){id=n;break;}}if(id<0)throw new IllegalStateException();camera=Camera.open(id);Camera.Parameters p=camera.getParameters();Camera.Size size=p.getSupportedPreviewSizes().stream().filter(s->s.width<=1280&&s.height<=960).max(Comparator.comparingInt(s->s.width*s.height)).orElse(p.getPreviewSize());p.setPreviewSize(size.width,size.height);p.setPreviewFormat(android.graphics.ImageFormat.NV21);if(p.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);camera.setParameters(p);int rotation=getWindowManager().getDefaultDisplay().getRotation()*90;camera.setDisplayOrientation((info.orientation-rotation+360)%360);camera.setPreviewDisplay(preview.getHolder());camera.setPreviewCallback((bytes,c)->decode(bytes,size.width,size.height));camera.startPreview();}catch(Exception e){release();hint.setText("Kamera konnte nicht geöffnet werden. Bitte schließe andere Kamera-Apps und öffne den Scanner erneut.");}}
    private void decode(byte[] bytes,int w,int h){if(decoding||done||!resumed)return;decoding=true;byte[] copy=bytes.clone();worker.execute(()->{String result=BarcodeDecoder.decodeFrame(copy,w,h);runOnUiThread(()->{decoding=false;if(result!=null&&resumed&&!done){done=true;setResult(RESULT_OK,new Intent().putExtra("barcode",result.trim()));finish();}});});}
    private void release(){if(camera!=null){camera.setPreviewCallback(null);camera.stopPreview();camera.release();camera=null;}}
    @Override protected void onPause(){resumed=false;release();super.onPause();}
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
    public void surfaceCreated(SurfaceHolder holder){ready=true;open();}
    public void surfaceChanged(SurfaceHolder h,int f,int w,int height){}
    public void surfaceDestroyed(SurfaceHolder holder){ready=false;release();}
}
