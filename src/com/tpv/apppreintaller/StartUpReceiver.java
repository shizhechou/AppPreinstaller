package com.tpv.apppreintaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartUpReceiver extends BroadcastReceiver {
	private static final String LOG_TAG = "AppPreInstaller";

	@Override
	public void onReceive(Context context, Intent intent) {
		/*
		 * Use follows command to test
		 * adb shell am broadcast -a android.intent.action.BOOT_COMPLETED 
		 * -c android.intent.category.HOME -n com.tpv.apppreintaller/.StartUpReceiver
		 */
		String action = intent.getAction();
		Log.i(LOG_TAG, "StartUpReceiver:" + action);
		Intent serviceIntent = new Intent(context, AppPreInstallService.class);
		
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {			
			serviceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startService(serviceIntent);
			
		}else if(Intent.ACTION_USER_INITIALIZE.equals(action)){
			Log.i(LOG_TAG,"intent action : "+action);	

		}
		
	}

}
