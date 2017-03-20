package pers.lxt.remotecontrol.test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import pers.lxt.remotecontrol.R;
import pers.lxt.remotecontrol.view.ClickPad;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Bitmap bmp0 = BitmapFactory.decodeResource(getResources(),R.drawable.i0);
        Bitmap bmp1 = BitmapFactory.decodeResource(getResources(),R.drawable.i1);
        Log.i("aa",bmp0.getWidth()+" "+bmp0.getHeight());
        Log.i("bb",bmp1.getWidth()+" "+bmp1.getHeight());
        ClickPad.compoundImage(bmp0,1);
        ClickPad.compoundImage(bmp1,1);

        ImageView img = (ImageView) findViewById(R.id.img);
        img.setImageBitmap(bmp1);
    }
}
