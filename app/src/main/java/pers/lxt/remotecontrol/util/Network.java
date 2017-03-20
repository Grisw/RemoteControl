package pers.lxt.remotecontrol.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.zip.GZIPInputStream;

import pers.lxt.remotecontrol.activity.MainActivity;

public class Network {

	private static Socket socket=null;
	private static PrintWriter writer=null;
	private static BufferedReader reader=null;
	private static Queue<byte[]> imgBuffer;
	private static DatagramSocket server = null;

	public static void setCursorPos(int x,int y){
		if(writer!=null){
			writer.println("move,"+x+","+y);
			writer.flush();
		}
	}

	public static void cursorLeftClick(){
		if(writer!=null){
			writer.println("lclick,");
			writer.flush();
		}
	}

	public static void cursorRightClick(){
		if(writer!=null){
			writer.println("rclick,");
			writer.flush();
		}
	}

	public static void scrollUp(){
		if(writer!=null){
			writer.println("wheelup,");
			writer.flush();
		}
	}

	public static void scrollDown(){
		if(writer!=null){
			writer.println("wheeldown,");
			writer.flush();
		}
	}

	public static void sendString(String str){
		if(writer!=null){
			writer.println("string,"+str);
			writer.flush();
		}
	}

	public static void reset(){
		if(writer!=null){
			writer.println("reset,");
			writer.flush();
		}
	}

	public static void close(){
		if(writer!=null)
			writer.close();
		if(socket!=null)
			try {
				socket.close();
			} catch (IOException e) {
				Log.e("close", e.getMessage(), e);
			}
	}

	private static byte[] unGzip(byte[] buf) throws IOException {
		GZIPInputStream gzi = null;
		ByteArrayOutputStream bos = null;
		try {
			gzi = new GZIPInputStream(new ByteArrayInputStream(buf));
			bos = new ByteArrayOutputStream(buf.length);
			int count = 0;
			byte[] tmp = new byte[2048];
			while ((count = gzi.read(tmp)) != -1) {
				bos.write(tmp, 0, count);
			}
			buf = bos.toByteArray();
		} finally {
			if (bos != null) {
				bos.flush();
				bos.close();
			}
			if (gzi != null)
				gzi.close();
		}
		return buf;
	}

	public static Bitmap getDesktop() throws IOException {
		if(imgBuffer!=null&&!imgBuffer.isEmpty()){
			byte[] data = unGzip(imgBuffer.poll());
			return BitmapFactory.decodeByteArray(data,0,data.length);
		}
		return null;
	}

	public static void startRecv(){
		imgBuffer=new LinkedList<>();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
					try{
						byte[] data = Base64.decode(reader.readLine(),Base64.DEFAULT);
						imgBuffer.offer(data);
						Log.i("link",data.length+"");
					}catch (NullPointerException |IOException e){
						Log.e("link", "Error:" + e.getMessage());
					}
				}
			}
		}).start();
	}

	public static String getLocalIpAddress() {
        try {   
        	String ip="192.168.1.100";
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {   
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {   
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                    	Log.i("ip", inetAddress.getHostAddress().toString());
						if(!inetAddress.getHostAddress().toString().contains(":"))
                        	ip=inetAddress.getHostAddress().toString();
                    }   
                }   
            }
			Log.i("ip Confirm", ip);
            return ip;
        } catch (SocketException ex) {
            Log.e("WifiPreferenceIpAddress", ex.toString());
        }   
        return null;   
    }   
	
	private static int totalCount=0;
	
	public static void search(final Context context, final List<String> list) {
		final String myIp;
		String host=getLocalIpAddress();
		myIp=host.substring(0, host.lastIndexOf(".")+1);
		Log.i("cardinal", myIp);
		final Thread curThread= Thread.currentThread();
		while(true){
			for(int ti=0;ti<=7;ti++){
				final int titmp=ti;
				new Thread(){
					public void run() {
						for(int i=titmp*32;i<(titmp+1)*32;i++){
							if(curThread.isInterrupted()){
								break;
							}
							Socket socket=null;
							try {
								Log.i("check", myIp + i);
								socket=new Socket();
								socket.connect(new InetSocketAddress(myIp + i, 4869), 300);
								Log.i("check", myIp + i + " available!");
								if(!list.contains(socket.getInetAddress().getHostAddress())){
									list.add(socket.getInetAddress().getHostAddress());
									MainActivity.handler.sendEmptyMessage(MainActivity.Msg.UPDATE_LIST.ordinal());
								}
							} catch (IOException e) {
								if(list.contains(myIp + i)){
									list.remove(myIp + i);
									MainActivity.handler.sendEmptyMessage(MainActivity.Msg.UPDATE_LIST.ordinal());
								}
							} finally {
								if(socket!=null)
									try {
										socket.close();
									} catch (IOException e) {
										Log.e("check", e.getMessage(), e);
									}
							}
						}
						totalCount++;
					}
				}.start();
			}
			while(true){
				if(totalCount==8){
					totalCount=0;
					break;
				}
			}
			if(curThread.isInterrupted()){
				break;
			}
		}
	}
	
	public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(wifiNetworkInfo.isConnected()) {
            return true ;
        }
        return false ;
    }
	
	public static boolean link(String ip, String password){
		close();
		Socket socket=null;
		PrintWriter writer=null;
		BufferedReader reader=null;
		try {
			socket=new Socket(ip, 4869);
			writer=new PrintWriter(socket.getOutputStream());
			writer.println(password);
			writer.flush();
			reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String res=reader.readLine();
			if(res.equals("accept")){
				Network.socket=socket;
				Network.writer=writer;
				Network.reader=reader;
				return true;
			}else{
				if(writer!=null)
					writer.close();
				if(reader!=null)
					reader.close();
				if(socket!=null)
					socket.close();
				return false;
			}
		} catch (IOException e) {
			Log.e("link", e.getMessage(), e);
			if(writer!=null)
				writer.close();
			if(reader!=null)
				try {
					reader.close();
				} catch (IOException e2) {
					Log.e("link", e2.getMessage(), e2);
				}
			if(socket!=null)
				try {
					socket.close();
				} catch (IOException e1) {
					Log.e("link", e1.getMessage(), e1);
				}
			return false;
		}
	}
}
