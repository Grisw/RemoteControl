package pers.lxt.remotecontrol.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import pers.lxt.remotecontrol.R;
import pers.lxt.remotecontrol.util.Network;
import pers.lxt.remotecontrol.util.NetworkStatusReceiver;

public class MainActivity extends AppCompatActivity {

    private static Context context;
    private static Thread thread;
    private static List<String> list;
    private static ArrayAdapter<String> adapter;
    private static ProgressDialog proDialog=null;
    private static Button btn;
    private static boolean isAnimOngoing=true;

    public enum Msg{
        STOP_THREAD,SEND_MESSAGE,UPDATE_LIST,NETWORK_CHANGE
    }

    public static Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(msg.what==Msg.STOP_THREAD.ordinal()&&thread!=null){
                thread.interrupt();
            }else if(msg.what==Msg.SEND_MESSAGE.ordinal()){
                Toast.makeText(context, (String) msg.obj, Toast.LENGTH_SHORT).show();
            }else if(msg.what==Msg.UPDATE_LIST.ordinal()){
                adapter.notifyDataSetChanged();
            }else if(msg.what==Msg.NETWORK_CHANGE.ordinal()){
                if(btn!=null){
                    boolean flag= (boolean) msg.obj;
                    btn.setEnabled(flag);
                    if(flag){
                        btn.setBackgroundResource(R.drawable.round_button_shape);
                    }else{
                        btn.setBackgroundResource(R.drawable.round_button_shape_invalid);
                        isAnimOngoing=false;
                        btn.clearAnimation();
                    }
                }
            }
            return false;
        }
    });

    private NetworkStatusReceiver networkStatusReceiver=new NetworkStatusReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(networkStatusReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        context=this;
        final ListView listView= (ListView) findViewById(R.id.listView);
        list=new ArrayList<>();
        adapter= new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, final int position, long id) {
                final String ip = list.get(position);
                final View view = View.inflate(context, R.layout.password_check, new LinearLayout(context));
                new AlertDialog.Builder(context)
                        .setView(view)
                        .setTitle("输入密码")
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
                                proDialog = new ProgressDialog(context);
                                proDialog.setCancelable(true);
                                proDialog.setCanceledOnTouchOutside(false);
                                proDialog.setIndeterminate(true);
                                proDialog.setMessage("请确保另一台设备输入了相同的密码...");
                                proDialog.setTitle("正在连接");
                                proDialog.setCancelMessage(handler.obtainMessage(Msg.STOP_THREAD.ordinal()));
                                proDialog.show();
                                thread = new Thread() {
                                    public void run() {
                                        if (Network.link(ip, ((EditText) view.findViewById(R.id.password)).getText().toString())) {
                                            proDialog.dismiss();
                                            handler.sendMessage(handler.obtainMessage(Msg.SEND_MESSAGE.ordinal(), "连接成功"));
                                            list.clear();
                                            handler.sendMessage(handler.obtainMessage(Msg.UPDATE_LIST.ordinal()));
                                            Intent intent = new Intent(context, ControlActivity.class);
                                            startActivity(intent);
                                            dialog.dismiss();
                                        } else {
                                            proDialog.dismiss();
                                            handler.sendMessage(handler.obtainMessage(Msg.SEND_MESSAGE.ordinal(), "拒绝连接"));
                                            dialog.dismiss();
                                        }
                                    }
                                };
                                thread.start();
                            }
                        }).show();
            }
        });
        btn= (Button) findViewById(R.id.button);
        if(!Network.isWifiConnected(this)){
            btn.setEnabled(false);
            btn.setBackgroundResource(R.drawable.round_button_shape_invalid);
        }
        btn.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN){
                    isAnimOngoing=true;
                    final Animation zoom = AnimationUtils.loadAnimation(MainActivity.this,R.anim.round_button_zoom);
                    final Animation shrink=AnimationUtils.loadAnimation(MainActivity.this,R.anim.round_button_shrink);
                    zoom.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if(isAnimOngoing)
                                btn.startAnimation(shrink);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                    shrink.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (isAnimOngoing)
                                btn.startAnimation(zoom);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    btn.startAnimation(zoom);
                    list.clear();
                    adapter.notifyDataSetChanged();
                    searching(adapter);
                }else if(event.getAction()==MotionEvent.ACTION_UP){
                    isAnimOngoing=false;
                    btn.clearAnimation();
                    handler.sendMessage(handler.obtainMessage(Msg.STOP_THREAD.ordinal()));
                }
                return true;
            }
        });
    }

    private static void searching(final ArrayAdapter<String> adapter){
        if(Network.isWifiConnected(context)){
            thread=new Thread(){
                public void run(){
                    Network.search(context,list);
                }
            };
            thread.start();
        }else{
            Toast.makeText(context, "请打开WIFI", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            System.exit(0);
        }
        return super.onKeyDown(keyCode, event);
    }

}
