package com.home.adbremote;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.os.PowerManager;


public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	//private static final String DEVIEDOMIN = "homedevice.iask.in";
	private static final String DEVIEDOMIN = "192.168.0.106";
    //private static final int PORT = 39627;
	private static final int PORT = 8080;
    private boolean isConnect = false;
    private Socket mSocket;
    //private PrintWriter mSend;
    private BufferedWriter mSend;
    private BufferedReader mRead;
    private boolean isRun = true;
    private boolean isonline = false;
    private long onlinetimeoutrecord = -1;
    private long onlinetimecount = 0;
    private long onlinejudgetimeoutrecord = -1;
    private long onlinejudgetimecount = 0;
    
    private Button Button_connect;
    private Button Button_ring;
    private Button Button_manual_answer;
    private Button Button_auto_answer;
    private EditText EditText_ip;
    private EditText EditText_port;
    private TextView TextView_status;
    private Button Button_current;
    private Button Button_restart;
    
    private final int STATUS = 1;
    private final int START_SOCKET = 2;
    private final int CLOSE_SOCKET = 3;
    private final int SERIESACTION = 4;
    private final int ADBCOMMAND = 5;
    private final int ADBRESULT = 6;
    
    private boolean adbconnected = false;
    
    private PowerManager mPowerManager = null;
    private WakeLock mWakeLock = null; 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPowerManager = (PowerManager)MainActivity.this.getSystemService(Context.POWER_SERVICE);
        init_button();
        mHandler.sendEmptyMessage(STATUS);
        //mHandler.sendEmptyMessage(START_SOCKET);
        Log.i(TAG, "onCreate finished");
    }

    //private HandlerThread mChildThread;
    //private Handler mChildHandler;
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
        	switch (msg.what){
			case STATUS:
				Button_connect.setText(isConnect ? "已连接" : "连接");
				if (mSocket != null) {
					isConnect = mSocket.isConnected() && !mSocket.isClosed();
				} else {
					isConnect = false;
				}
				Button_connect.setText(isConnect ? "已连接" : "连接");
				if (isConnect) {
					Button_ring.setEnabled(true);
				} else {
					Button_ring.setEnabled(false);
			    	Button_manual_answer.setEnabled(false);
			    	Button_auto_answer.setEnabled(false);
			    	Button_current.setEnabled(false);
			    	Button_restart.setEnabled(false);
			    	TextView_status.setText("");
				}
				onlinejudgetimecount++;
				/*if (onlinetimeoutrecord < 0) {
					onlinetimeoutrecord = onlinetimecount;
				}
				if (onlinejudgetimeoutrecord < 0) {
					onlinejudgetimeoutrecord = onlinejudgetimecount;
				}
				long onlineperiod = onlinetimecount - onlinetimeoutrecord;
				long onlinejudgeperiod = onlinejudgetimecount - onlinejudgetimeoutrecord;
				if (onlinejudgeperiod >= onlineperiod + 5) {
					onlinetimeoutrecord = onlinetimecount;
					onlinejudgetimeoutrecord = onlinejudgetimecount;
					isonline = false;
				}*/
				if (onlinejudgetimecount - onlinetimecount > 5) {
					isonline = false;
					onlinetimecount = 0;
					onlinejudgetimecount = 0;
				}
				//Log.i(TAG, "onlineperiod = " + onlineperiod + ", onlinejudgeperiod = " + onlinejudgeperiod + ", isonline = " + isonline + ", isConnect = " + isConnect);
				//Log.i(TAG, "onlinejudgetimecount = " + onlinejudgetimecount + ", onlinetimecount = " + onlinetimecount + "isonline = " + isonline + ", isConnect = " + isConnect);
				mHandler.sendEmptyMessageDelayed(STATUS, 1000);
				sendSocketMessage("client online");
				break;
			case START_SOCKET:
				connectServerWithTCPSocket();
				break;
			case CLOSE_SOCKET:
            	closeSocket();
				break;
			case SERIESACTION:
				if (msg.obj != null) {
					sendSocketMessage((String)msg.obj);
				}
				break;
			case ADBCOMMAND:
				if (msg.obj != null) {
					sendSocketMessage("cmd:" + (String)msg.obj);
				}
				break;
			case ADBRESULT:
				if (msg.obj != null) {
					TextView_status.setText((String)msg.obj);
				}
				break;
			default:
				break;
		}
        }
    };
    
    /*private void initBackThread()
    {
    	mChildThread = new HandlerThread("childthread");
    	mChildThread.start();
    	mChildHandler = new Handler(mChildThread.getLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
            	
            }
        };
    }*/
    
    private void init_button() {
    	EditText_ip = (EditText) findViewById(R.id.editText_ip);
    	EditText_port = (EditText) findViewById(R.id.EditText_port);
    	TextView_status = (TextView) findViewById(R.id.TextView_status);
    	Button_connect = (Button) findViewById(R.id.button_connect);
    	Button_connect.requestFocus();
    	Button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isConnect) {
                	mHandler.sendEmptyMessage(START_SOCKET);
                	Log.d(TAG, "connect......");
                } else {
                	mHandler.sendEmptyMessage(CLOSE_SOCKET);
                	Log.d(TAG, "disconnect......");
                }
            }
        });
    	Button_ring = (Button) findViewById(R.id.Button_ring);
    	Button_ring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if (!adbconnected) {
            		adbconnected = true;
            		Button_ring.setText("adb已连接");
            		sendToHandle(ADBCOMMAND, "adb connect homedevice.iask.in:39634", 0);

			    	Button_manual_answer.setEnabled(true);
			    	Button_auto_answer.setEnabled(true);
			    	Button_current.setEnabled(true);
			    	Button_restart.setEnabled(true);
            	} else {
            		adbconnected = false;
            		Button_ring.setText("连接adb");
            		sendToHandle(ADBCOMMAND, "adb disconnect", 0);
            		Button_manual_answer.setEnabled(false);
			    	Button_auto_answer.setEnabled(false);
			    	Button_current.setEnabled(false);
			    	Button_restart.setEnabled(false);
            	}
            	/*for (int i = 0; i < 2; i++) {
            		period = period + 500;
            		sendToHandle(SERIESACTION, "magic back", period);
            	}*/
            	/*period = period + 500;
            	sendToHandle(SERIESACTION, "magic home", period);
            	period = period + 500;
            	sendToHandle(SERIESACTION, "magic home", period);
            	period = period + 500;
            	sendToHandle(SERIESACTION, "magic m", period);
            	for (int i = 0; i < 5; i++) {
            		period = period + 500;
            		sendToHandle(SERIESACTION, "magic left", period);
            	}
            	period = period + 500;
            	sendToHandle(SERIESACTION, "magic ok", period);*/
            }
        });
    	Button_manual_answer = (Button) findViewById(R.id.Button_manual_answer);
    	Button_manual_answer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	//sendSocketMessage("magic ok");
            	//sendToHandle(SERIESACTION, "iptv0 ok", 0);
            	sendToHandle(ADBCOMMAND, "adb shell \"input keyevent 23\"", 0);//KEYCODE_DPAD_CENTER     = 23;
            }
        });
    	Button_auto_answer = (Button) findViewById(R.id.Button_auto_answer);
    	Button_auto_answer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	/*Button_connect.setEnabled(false);
            	Button_ring.setEnabled(false);
            	Button_manual_answer.setEnabled(false);
            	Button_auto_answer.setEnabled(false);*/
            	int period = 0;
            	for (int i = 0; i < 4; i++) {
            		period = period + 500;
            		sendToHandle(ADBCOMMAND, "adb shell \"input keyevent 4\"", period);//KEYCODE_BACK            = 4;
            	}
            	/*period = period + 500;
            	sendToHandle(SERIESACTION, "magic home", period);
            	period = period + 500;
            	sendToHandle(SERIESACTION, "magic home", period);
            	period = period + 500;
            	sendToHandle(SERIESACTION, "magic m", period);
            	for (int i = 0; i < 5; i++) {
            		period = period + 500;
            		sendToHandle(SERIESACTION, "magic left", period);
            	}*/
            	period = period + 500;
            	//sendToHandle(SERIESACTION, "iptv0 home", period);
            	sendToHandle(ADBCOMMAND, "adb shell \"input keyevent 3\"", period);//KEYCODE_HOME            = 3;
            	period = period + 500;
            	//sendToHandle(SERIESACTION, "iptv0 number1", period);
            	sendToHandle(ADBCOMMAND, "adb shell \"input keyevent 8\"", period);//KEYCODE_1               = 8;
            	period = period + 500;
            	//sendToHandle(SERIESACTION, "iptv0 number1", period);//打开小鹰直播
            	sendToHandle(ADBCOMMAND, "adb shell \"input keyevent 8\"", period);//KEYCODE_1               = 8;
            }
        });

    	Button_current = (Button) findViewById(R.id.Button_current);
    	Button_current.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	//sendSocketMessage("camera right");
            	sendToHandle(ADBCOMMAND, "adb shell \"dumpsys window | grep mCurrentFocus\"", 0);
            }
        });
    	
        Button_restart = (Button) findViewById(R.id.Button_restart);
    	Button_restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	//sendSocketMessage("camera right");
            	sendToHandle(ADBCOMMAND, "adb reboot", 0);
            }
        });
    	Button_ring.setEnabled(false);
    	Button_manual_answer.setEnabled(false);
    	Button_auto_answer.setEnabled(false);
    	Button_current.setEnabled(false);
    	Button_restart.setEnabled(false);
    }
    
    private void sendToHandle(int type, String mess, int delay) {
    	Message message = new Message();
    	message.what = type;
    	message.obj = mess;
    	mHandler.sendMessageDelayed(message, delay);
    	Log.i(TAG, "type = " + type + ", tv command = " + message + ", delay = " + delay);
    }
    
    private void connectServerWithTCPSocket() {   
    	Log.i(TAG, "connect");
    	String host = EditText_ip.getText().toString();
    	int port = Integer.valueOf(EditText_port.getText().toString());
    	//host = "192.168.1.88";
    	//port = 19900;
    	Log.i(TAG, "new socket host = " + host + ", port = " + port);
    	initClientSocket();
    }
    
    private void initClientSocket()
    {
       //�?启子线程
        new Thread(new Runnable() {
          
          @Override
          public void run() {
              try {
            	  //closeSocket();
            	  String host = EditText_ip.getText().toString();
              	  int port = Integer.valueOf(EditText_port.getText().toString());
              	  mSocket = new Socket();
              	  mSocket.connect(new InetSocketAddress(host, port), 5000);
              	  isConnect = mSocket.isConnected() && !mSocket.isClosed();
              	  isRun = true;
              	  mRead = new BufferedReader(new InputStreamReader(
              			mSocket.getInputStream()));
              	  mSend = new BufferedWriter(new OutputStreamWriter(  
              			mSocket.getOutputStream()));  
              			/*new PrintWriter(new BufferedWriter(new OutputStreamWriter(
              			mSocket.getOutputStream())), true);*/
              	  Log.i(TAG, "new socket creat success isConnect = " + isConnect);
            	  sendSocketMessage("client online");
              	  String line = "";
              	  char[] temp = new char[4096];
              	  int len = 0;
              	  while (isRun) {
              		  try {   
              			while ((len = mRead.read(temp)) > 0) {
              				line = new String(temp,0,len);
                            Log.i(TAG, "getdata = " + line + ", len = " + len);
                            if (line.contains("magic online") || line.contains("result magic online")) {
                            	isonline = true;//design to update per 2 seconds
                            	onlinetimecount++;
                            }
                            sendToHandle(ADBRESULT, line, 0);
                        }
              			//Log.i(TAG, "read socket....");
              		  } catch (Exception e) {
                        Log.i(TAG, "receive erro = " + e.getMessage());
                        e.printStackTrace();
              		  }
              	  }
              	Log.i(TAG, "read socket end");
              } catch (UnknownHostException e) {
                    Log.i(TAG, "connect UnknownHostException: " + e.getMessage());
              } catch (IOException e) {
            	  Log.i(TAG, "connect IOException: " + e.getMessage());
              }
          }
      }).start();
    }

    
    private void sendSocketMessage(final String mess) {
    	new Thread(new Runnable() {
            @Override
            public void run() {
            	if (mSocket != null && isConnect && !TextUtils.isEmpty(mess)) {
                	try {   
                    	/*BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(  
                    			mSocket.getOutputStream()));*/
                    	mSend.write(mess);  
                    	mSend.flush();  
                    } catch (UnknownHostException e) {  
                        e.printStackTrace();  
                        Log.i(TAG, "sendSocketMessage UnknownHostException = " + e.getMessage());
                    } catch (IOException e) {  
                        e.printStackTrace();  
                        Log.i(TAG, "sendSocketMessage UnknownHostException = " + e.getMessage());
                    }
            	} else {
            		//initClientSocket();
            	}
            }
        }).start();
    }
    
    private void closeSocket() {
    	new Thread(new Runnable() {
            
            @Override
            public void run() {
            	if (mSocket != null) {
            		Log.i(TAG, "closeSocket");
            		sendSocketMessage("client close");
            		isRun = false;
            		try {
        				mSocket.close();
        				mRead.close();
            			mSend.close();
        				mSocket = null;
        				isConnect = false;
        				Log.i(TAG, "closeSocket end");
        			} catch (IOException e) {
        				e.printStackTrace();
        				Log.i(TAG, "closeSocket IOException = " + e.getMessage());
        			}
            	}
            }
        }).start();
    }
    
    private void acquireWakeLock() {
    	if (mWakeLock == null && mPowerManager != null) {
    		mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "remote");
    	}
    	if (mWakeLock != null) {
    		mWakeLock.acquire();
    	}
    }
    
    private void releaseWakeLock() {
    	if (mWakeLock != null) {
    		mWakeLock.release();
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    protected void onStart(){ 
        super.onStart(); 
        Log.i(TAG, "onStart"); 
      } 
      protected void onRestart(){ 
        super.onRestart(); 
        Log.i(TAG, "onReatart"); 
      } 
      protected void onResume(){ 
        super.onResume();
        acquireWakeLock();
        Button_ring.setEnabled(false);
    	Button_manual_answer.setEnabled(false);
    	Button_auto_answer.setEnabled(false);
    	Button_current.setEnabled(false);
    	Button_restart.setEnabled(false);
        //mHandler.sendEmptyMessage(START_SOCKET);
        Log.i(TAG, "onResume"); 
      } 
      protected void onPause(){ 
        super.onPause();
        releaseWakeLock();
        if (isConnect) {
    		if (adbconnected) {
        		Button_ring.setText("连接adb");
        		TextView_status.setText("");
        		sendToHandle(ADBCOMMAND, "adb disconnect", 0);
        		adbconnected = false;
        	}
    		mHandler.sendEmptyMessageDelayed(CLOSE_SOCKET, 200);
        }
        Log.i(TAG, "onPause"); 
        if(isFinishing()){ 
          Log.w(TAG, "will be destroyed!"); 
        }else{ 
          Log.w(TAG, "just pausing!"); 
        } 
      } 
      protected void onStop(){ 
        super.onStop(); 
        Log.i(TAG, "onStop"); 
      } 
      protected void onDestroy(){ 
        super.onDestroy();
        mHandler.sendEmptyMessage(CLOSE_SOCKET);
        mHandler.removeCallbacksAndMessages(null);
        Log.i(TAG, "onDestroy"); 
      } 
}

