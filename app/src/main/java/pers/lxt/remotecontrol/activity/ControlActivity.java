package pers.lxt.remotecontrol.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import pers.lxt.remotecontrol.R;
import pers.lxt.remotecontrol.util.Network;
import pers.lxt.remotecontrol.view.ClickPad;

public class ControlActivity extends AppCompatActivity {

    public static int movingSpeed=5;
    public static final String PREFERENCE="RMT_PRE";

    private static Context context;
    private static ClickPad clickPad;

    public enum Msg{
        STOP_THREAD,SEND_MESSAGE,UPDATE_CLICKPAD
    }

    private static Thread thread=null;

    public static Handler handler=new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            if(msg.what==Msg.STOP_THREAD.ordinal()){
                thread.interrupt();
            }else if(msg.what==Msg.SEND_MESSAGE.ordinal()){
                Toast.makeText(context, (String) msg.obj, Toast.LENGTH_SHORT).show();
            }else if(msg.what==Msg.UPDATE_CLICKPAD.ordinal()){
                clickPad.refresh((Bitmap) msg.obj);
            }
            return false;
        }
    });

    @Override
    protected void onDestroy() {
        Network.close();
        super.onDestroy();
    }

    private long time=0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            if(System.currentTimeMillis()-time>3000){
                Toast.makeText(context, "再次按下返回键退出", Toast.LENGTH_SHORT).show();
                time=System.currentTimeMillis();
                return true;
            }else{
                this.finish();
            }
        }else if(keyCode==KeyEvent.KEYCODE_MENU){
            final View v=View.inflate(context, R.layout.string_enter, new LinearLayout(context));
            new AlertDialog.Builder(context)
                    .setView(v)
                    .setTitle("输入文本")
                    .setCancelable(true)
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            final String string = ((EditText) v.findViewById(R.id.string_enter)).getText().toString();
                            new Thread() {
                                public void run() {
                                    Network.sendString(string);
                                    Log.i("sendstring", string);
                                }
                            }.start();
                            Toast.makeText(context, "已发送至剪贴板", Toast.LENGTH_SHORT).show();
                        }
                    }).show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try {
            getWindow().addFlags(WindowManager.LayoutParams.class.getField("FLAG_NEEDS_MENU_KEY").getInt(null));
        }catch (NoSuchFieldException e) {
            // Ignore since this field won't exist in most versions of Android
        }catch (IllegalAccessException e) {
            Log.w("info", "Could not access FLAG_NEEDS_MENU_KEY in addLegacyOverflowButton()", e);
        }

        setContentView(R.layout.activity_control);
        SharedPreferences preferences=getSharedPreferences(PREFERENCE, Activity.MODE_PRIVATE);
        if(preferences.contains("moving_speed")){
            movingSpeed=preferences.getInt("moving_speed",5);
        }else{
            SharedPreferences.Editor preEditor=preferences.edit();
            preEditor.putInt("moving_speed", 5);
            preEditor.apply();
        }
        ControlActivity.context=this;
        clickPad= (ClickPad) findViewById(R.id.clickpad);
        new Thread(){
            @Override
            public void run() {
                try {
                    Network.startRecv();
                    while(true){
                        Bitmap bmp=Network.getDesktop();
                        if(bmp!=null){
                            ControlActivity.handler.sendMessage(ControlActivity.handler.obtainMessage(ControlActivity.Msg.UPDATE_CLICKPAD.ordinal(), bmp));
                        }
                    }
                } catch (Exception e) {
                    Log.e("getdesktop",e.getMessage(),e);
                    ControlActivity.handler.sendMessage(ControlActivity.handler.obtainMessage(ControlActivity.Msg.SEND_MESSAGE.ordinal(),"已断开连接"));
                } finally{
                    Network.close();
                }
            }
        }.start();
    }
}
