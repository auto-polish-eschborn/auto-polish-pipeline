package de.autopolish.nativeapp;

import android.os.Bundle;
import android.widget.*;
import android.view.*;
import org.json.*;
import java.util.concurrent.*;

public abstract class FormActivity extends BaseActivity {
    protected LinearLayout content;
    protected final ExecutorService requests=Executors.newSingleThreadExecutor();
    interface Result { void accept(JSONObject value) throws Exception; }
    protected void shell(String title) {
        LinearLayout root=Ui.vertical(this,0); root.setBackgroundColor(Ui.surface(this));
        LinearLayout top=Ui.vertical(this,12);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);top.setBackgroundColor(Ui.BLACK);
        Button back=Ui.button(this,"‹",Ui.BLACK);back.setContentDescription("Zurück");back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        top.addView(Ui.text(this,title,21,android.graphics.Color.WHITE,true));root.addView(top);
        ScrollView scroll=new ScrollView(this);content=Ui.vertical(this,18);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    protected void title(String text) { TextView v=Ui.text(this,text,19,Ui.BLACK,true);Ui.marginTop(v,this,22);content.addView(v); }
    protected void note(String text) { TextView v=Ui.text(this,text,14,Ui.MUTED,false);Ui.marginTop(v,this,10);content.addView(v); }
    protected EditText field(String label,String value,boolean password) {
        TextView caption=Ui.text(this,label,14,Ui.BLACK,true);Ui.marginTop(caption,this,14);content.addView(caption);
        EditText input=Ui.input(this,label);input.setText(value);if(password)input.setInputType(129);Ui.marginTop(input,this,6);content.addView(input);return input;
    }
    protected Button action(String label,Runnable run) {Button b=Ui.button(this,label,Ui.BLUE);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,Ui.dp(this,52));params.topMargin=Ui.dp(this,14);content.addView(b,params);b.setOnClickListener(v->run.run());return b;}
    protected void call(String method,String path,JSONObject body,Result result) { requests.execute(()->{
        try { JSONObject value=ApiClient.request(method,path,body);runOnUiThread(()->{if(isFinishing()||isDestroyed())return;try{result.accept(value);}catch(Exception e){message(e.getMessage());}}); }
        catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())message(e.getMessage());});}
    }); }
    protected void message(String text) { Toast.makeText(this,text,Toast.LENGTH_LONG).show(); }
    static JSONObject json(Object... kv) {JSONObject o=new JSONObject();try{for(int i=0;i<kv.length;i+=2)o.put((String)kv[i],kv[i+1]);}catch(JSONException e){throw new IllegalArgumentException(e);}return o;}
    static String value(EditText v){return v.getText().toString();}
    @Override protected void onDestroy(){requests.shutdownNow();super.onDestroy();}
}
