package com.home.adbremote;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ActionService extends Service {
    public static final String TAG = "ActionService";
    private static final String DEVIEDOMIN = "homedevice.iask.in";
    private static final int PORT = 19900;

   /*activity与service绑定时会调用*/
    public IBinder onBind(Intent intent) {
    	Log.d(TAG, "onBind");
        return new MyBinder();
    }

    /*第一次创建时才会调用，之后在服务没被销毁都不会调用 ，创建*/
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    /*第一次创建时和以后每次启动都会调用，重建*/
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        return super.onStartCommand(intent, flags, startId);
    }

   /*和onStartCommand()方法一样每次都会调用，启动*/
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(TAG, "onStart");
    }

    /*销毁时调用*/
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    public class MyBinder extends Binder {
        ActionService getService() {
            return ActionService.this;
        }
    }
    
    private void connectServerWithTCPSocket() {   
        Socket socket;  
        try {// 创建一个Socket对象，并指定服务端的IP及端口号  
            socket = new Socket(DEVIEDOMIN, PORT);  
            // 创建一个InputStream用户读取要发送的文件。  
            InputStream inputStream = new FileInputStream("/sdcard/");  
            // 获取Socket的OutputStream对象用于发送数据。  
            OutputStream outputStream = socket.getOutputStream();  
            // 创建一个byte类型的buffer字节数组，用于存放读取的本地文件  
            byte buffer[] = new byte[4 * 1024];  
            int temp = 0;  
            // 循环读取文件  
            while ((temp = inputStream.read(buffer)) != -1) {  
                // 把数据写入到OuputStream对象中  
                outputStream.write(buffer, 0, temp);  
            }  
            // 发送读取的数据到服务端  
            outputStream.flush();  
  
            /** 或创建一个报文，使用BufferedWriter写入,看你的需求 **/  
//          String socketData = "[2143213;21343fjks;213]";  
//          BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(  
//                  socket.getOutputStream()));  
//          writer.write(socketData.replace("\n", " ") + "\n");  
//          writer.flush();  
            /************************************************/  
        } catch (UnknownHostException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
  
    }
}