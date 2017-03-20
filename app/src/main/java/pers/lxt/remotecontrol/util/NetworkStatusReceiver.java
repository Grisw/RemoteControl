package pers.lxt.remotecontrol.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import pers.lxt.remotecontrol.activity.MainActivity;

public class NetworkStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(wifiInfo.isConnected()){
            MainActivity.handler.sendMessage(MainActivity.handler.obtainMessage(MainActivity.Msg.NETWORK_CHANGE.ordinal(),true));
        }else{
            MainActivity.handler.sendMessage(MainActivity.handler.obtainMessage(MainActivity.Msg.NETWORK_CHANGE.ordinal(),false));
        }
    }
}
