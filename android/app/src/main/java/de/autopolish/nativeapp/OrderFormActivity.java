package de.autopolish.nativeapp;
import android.os.Bundle;
public class OrderFormActivity extends BusinessActivity {
    @Override protected void onCreate(Bundle state){getIntent().putExtra("module","order");String id=getIntent().getStringExtra("order_id");if(id!=null)getIntent().putExtra("record_id",id);super.onCreate(state);}
}
