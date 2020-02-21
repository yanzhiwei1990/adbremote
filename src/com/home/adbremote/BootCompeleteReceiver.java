package com.home.adbremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompeleteReceiver extends BroadcastReceiver {
	private static final String TAG = "BootCompeleteReceiver";
	private static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if(intent.getAction().equals(ACTION_BOOT_COMPLETED)){
			//Intent serIntent = new Intent(context, ActionService.class);
			//serIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			//context.startService(serIntent);
		    Log.d(TAG, "start ActionService after boot!");
		}
	}
}
