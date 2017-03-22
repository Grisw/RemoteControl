package pers.lxt.remotecontrol.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import pers.lxt.remotecontrol.R;
import pers.lxt.remotecontrol.util.Network;
import pers.lxt.remotecontrol.util.NetworkStatusReceiver;

public class MainActivity extends AppCompatActivity {

    private Button btn;

    private Thread thread;
    private List<String> list;
    private ArrayAdapter<String> adapter;
    private ProgressDialog proDialog=null;
    private boolean isAnimOngoing=true;

    public static abstract class MessageType {
        static final int STOP_THREAD = 0;
        static final int SEND_MESSAGE = 1;
        public static final int UPDATE_LIST = 2;
        public static final int NETWORK_CHANGE = 3;
    }

    public static Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MessageType.STOP_THREAD:{
                    Thread thread = (Thread) msg.obj;
                    if(thread!=null)
                        thread.interrupt();
                }break;

                case MessageType.SEND_MESSAGE:{
                    Context context = (Context) msg.obj;
                    Toast.makeText(context, msg.getData().getString("msg"), Toast.LENGTH_SHORT).show();
                }break;

                case MessageType.UPDATE_LIST:{
                    BaseAdapter adapter = (BaseAdapter) msg.obj;
                    adapter.notifyDataSetChanged();
                }break;

                case MessageType.NETWORK_CHANGE:{
                    MainActivity context = (MainActivity) msg.obj;
                    if(context.btn!=null){
                        boolean flag= msg.arg1==1;
                        context.btn.setEnabled(flag);
                        if(flag){
                            context.btn.setBackgroundResource(R.drawable.round_button_shape);
                        }else{
                            context.btn.setBackgroundResource(R.drawable.round_button_shape_invalid);
                            context.isAnimOngoing=false;
                            context.btn.clearAnimation();
                        }
                    }
                }break;
            }

            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NetworkStatusReceiver networkStatusReceiver = new NetworkStatusReceiver(this);
        registerReceiver(networkStatusReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        final ListView listView= (ListView) findViewById(R.id.listView);
        list=new ArrayList<>();
        adapter= new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, final int position, long id) {
                final String ip = list.get(position);
                final View view = View.inflate(MainActivity.this, R.layout.password_check, new LinearLayout(MainActivity.this));
                new AlertDialog.Builder(MainActivity.this)
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
                                if(((EditText) view.findViewById(R.id.password)).getText().toString().isEmpty()){
                                    showToast(MainActivity.this,"输入点什么");
                                    return;
                                }
                                proDialog = new ProgressDialog(MainActivity.this);
                                proDialog.setCancelable(true);
                                proDialog.setIndeterminate(true);
                                proDialog.setMessage("请确保另一台设备输入了相同的密码...");
                                proDialog.setTitle("正在连接");
                                proDialog.setCancelMessage(handler.obtainMessage(MessageType.STOP_THREAD,thread));
                                proDialog.show();
                                thread = new Thread() {
                                    public void run() {
                                        if (Network.link(ip, ((EditText) view.findViewById(R.id.password)).getText().toString())) {
                                            proDialog.dismiss();
                                            showToast(MainActivity.this,"连接成功");
                                            list.clear();
                                            handler.obtainMessage(MessageType.UPDATE_LIST, adapter).sendToTarget();
                                            Intent intent = new Intent(MainActivity.this, ControlActivity.class);
                                            startActivity(intent);
                                            dialog.dismiss();
                                        } else {
                                            proDialog.dismiss();
                                            showToast(MainActivity.this,"拒绝连接");
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
                    searching();
                }else if(event.getAction()==MotionEvent.ACTION_UP){
                    isAnimOngoing=false;
                    btn.clearAnimation();
                    handler.obtainMessage(MessageType.STOP_THREAD, thread).sendToTarget();
                }
                return true;
            }
        });
    }

    private void searching(){
        if(Network.isWifiConnected(this)){
            thread=new Thread(){
                public void run(){
                    Network.search(list, adapter);
                }
            };
            thread.start();
        }else{
            Toast.makeText(MainActivity.this, "请打开WIFI", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            System.exit(0);
        }
        return super.onKeyDown(keyCode, event);
    }

    public static void showToast(Context context,String msg){
        Message message = handler.obtainMessage(MessageType.SEND_MESSAGE, context);
        Bundle bundle = new Bundle();
        bundle.putString("msg",msg);
        message.setData(bundle);
        message.sendToTarget();
    }

}
