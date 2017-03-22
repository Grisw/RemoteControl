package pers.lxt.remotecontrol.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import pers.lxt.remotecontrol.activity.MainActivity;

public class NetworkStatusReceiver extends BroadcastReceiver {

    private MainActivity mainActivity;

    public NetworkStatusReceiver(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
            MainActivity.handler.obtainMessage(MainActivity.MessageType.NETWORK_CHANGE,1,0,mainActivity).sendToTarget();
        }else{
            MainActivity.handler.obtainMessage(MainActivity.MessageType.NETWORK_CHANGE,0,0,mainActivity).sendToTarget();
        }
    }
}
