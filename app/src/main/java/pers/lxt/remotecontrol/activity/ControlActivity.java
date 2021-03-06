package pers.lxt.remotecontrol.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
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

    private ClickPad clickPad;
    private Thread thread;

    static abstract class MessageType {
        static final int SEND_MESSAGE = 0;
        static final int UPDATE_CLICKPAD = 1;
    }

    public static Handler handler=new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MessageType.SEND_MESSAGE:{
                    Context context = (Context) msg.obj;
                    Toast.makeText(context, msg.getData().getString("msg"), Toast.LENGTH_SHORT).show();
                }break;

                case MessageType.UPDATE_CLICKPAD:{
                    ClickPad clickPad = (ClickPad) ((Object[])msg.obj)[0];
                    Bitmap bmp = (Bitmap) ((Object[])msg.obj)[1];
                    clickPad.refresh(bmp);
                }break;
            }

            return false;
        }
    });

    @Override
    protected void onDestroy() {
        if(thread != null)
            thread.interrupt();
        Network.close();
        super.onDestroy();
    }

    private long time=0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            if(System.currentTimeMillis()-time>3000){
                Toast.makeText(this, "再次按下返回键退出", Toast.LENGTH_SHORT).show();
                time=System.currentTimeMillis();
                return true;
            }else{
                this.finish();
            }
        }else if(keyCode==KeyEvent.KEYCODE_MENU){
            final View v=View.inflate(this, R.layout.string_enter, new LinearLayout(this));
            new AlertDialog.Builder(this)
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
                            Toast.makeText(ControlActivity.this, "已发送至剪贴板", Toast.LENGTH_SHORT).show();
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
            Log.e("control-activity", "Could not access FLAG_NEEDS_MENU_KEY in addLegacyOverflowButton()", e);
        }

        setContentView(R.layout.activity_control);
        clickPad= (ClickPad) findViewById(R.id.clickpad);
        thread = new Thread(){
            @SuppressWarnings("InfiniteLoopStatement")
            @Override
            public void run() {
                try {
                    Network.startRecv();
                    while(true){
                        Bitmap bmp=Network.getDesktop();
                        if(bmp!=null){
                            Object[] objs = {clickPad, bmp};
                            handler.obtainMessage(MessageType.UPDATE_CLICKPAD, objs).sendToTarget();
                        }
                    }
                } catch (Exception e) {
                    Log.e("getdesktop",e.getMessage(),e);
                    showToast(ControlActivity.this,"已断开连接");
                } finally{
                    Network.close();
                }
            }
        };
        thread.start();
    }

    public static void showToast(Context context,String msg){
        Message message = handler.obtainMessage(MessageType.SEND_MESSAGE, context);
        Bundle bundle = new Bundle();
        bundle.putString("msg",msg);
        message.setData(bundle);
        message.sendToTarget();
    }
}
