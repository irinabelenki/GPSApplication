package com.greenroad.candidate.assignment;

import com.google.android.gms.location.FusedLocationProviderApi;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

public class LSService extends IntentService  {
	public static final String TAG = "LSService";
	
	public LSService() {
		super("MyLocation");
	}


	@Override
	protected void onHandleIntent(Intent intent) {
		GPSApplication application = (GPSApplication) getApplication();

		Location l = intent.getParcelableExtra(FusedLocationProviderApi.KEY_LOCATION_CHANGED);
		
		if (l != null) {
			Log.d(TAG, "Service got location " + l.getLatitude() + "," + l.getLongitude());
			
			application.addPoint(l);
		}
	}
}
