package com.greenroad.candidate.assignment;

import java.util.Date;
import java.util.LinkedList;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;

public class MainActivity extends FragmentActivity implements 
			 LocationListener, GoogleApiClient.ConnectionCallbacks {	
	private GoogleMap map;
	private TextView locationInfo, speed, timeField;
	protected GPSApplication application;
	
	Circle accuracyCircle;
	Polyline trackPolyline;
	
	public static final String TAG = "MainActivity";
	
    LocationRequest locationRequest = new LocationRequest()
    	.setInterval(1000)
    	.setFastestInterval(500)
    	.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    
    
    NotificationCompat.Builder notificationBuilder;
	NotificationManager notifyMgr;

    
    static final int notificationId = 101;
    
    Handler timeHandler;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		application = (GPSApplication)getApplication();
		
		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		map.setMyLocationEnabled(true);
		
		locationInfo = (TextView)findViewById(R.id.location_info);
		speed = (TextView)findViewById(R.id.speed);
		timeField  = (TextView)findViewById(R.id.time_field);
		
		ToggleButton trackButton = (ToggleButton)findViewById(R.id.toggleTrack);
		trackButton.setChecked(application.trackingEnabled);
		
		trackPolyline = map.addPolyline(new PolylineOptions().color(Color.BLUE)
				.width(3));
		
		application.stopBackgroundLocation();

		application.googleApiClient.registerConnectionCallbacks(this);
		
		Intent relaunchIntent = new Intent(this, MainActivity.class);
		
		PendingIntent relaunchPendingIntent =
		    PendingIntent.getActivity(
		    this,
		    0,
		    relaunchIntent,
		    PendingIntent.FLAG_UPDATE_CURRENT
		);

		notificationBuilder =
			    new NotificationCompat.Builder(this)
			    .setSmallIcon(R.drawable.notification_icon)
			    .setContentTitle("Tracking")
			    .setContentText("Tracking")
			    .setContentIntent(relaunchPendingIntent)
			    .setAutoCancel(true);
		notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		setTracking(application.trackingEnabled);
		
		if (application.googleApiClient.isConnected()) {
			Location location = LocationServices.FusedLocationApi.getLastLocation(application.googleApiClient);
			onLocationChanged(location);
		}
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		application.googleApiClient.unregisterConnectionCallbacks(this);
		
		if (application.googleApiClient.isConnected()) {
			application.startBackgroundCollection();
			showNotification();
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (application.googleApiClient.isConnected())
			showNotification();
	};
	
	@Override
	protected void onRestart() {
		super.onRestart();
		hideNotification();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void onTrackingClicked(View v)
	{
		application.trackingEnabled = ((ToggleButton) v).isChecked();
		setTracking(application.trackingEnabled);
	}
	
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));

		application.addPoint(location);
		
		
		double averageSpeed = application.getAverageSpeed();
		
		String info = "Location " + location.getLatitude() + " , " +
			 	   + location.getLongitude();
		locationInfo.setText(info);
		
		String speedText = "Speed:" + String.format("%.2f", averageSpeed);
		speed.setText(speedText);
		
		float accuracy = location.getAccuracy();
		LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
		
		if (accuracyCircle == null) {
			accuracyCircle = map.addCircle(new CircleOptions()
					     .center(position)
					     .radius(accuracy)
					     .strokeColor(Color.GREEN)
					     .strokeWidth(1.0f)
					     .fillColor(0x3fffffff));
			
		} else {
			accuracyCircle.setCenter(position);
			accuracyCircle.setRadius(accuracy);
		}
		
		LinkedList<LatLng> list = new LinkedList<LatLng>();
		application.copyTrackingPoints(list);
		trackPolyline.setPoints(list);
		timeField.setText(String.format("Last update: %tT", new Date()));
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				LocationServices.FusedLocationApi.requestLocationUpdates(application.googleApiClient,
						locationRequest, MainActivity.this);
			}
		});
	}

	@Override
	public void onConnectionSuspended(int cause) {
	}
	
	void showNotification()
	{
		// Builds the notification and issues it.
		notifyMgr.notify(notificationId, notificationBuilder.build());
	}
	
	void hideNotification()
	{
		notifyMgr.cancel(notificationId);
	}
	
	void setTracking(boolean v)
	{
		if (v) {
     		trackPolyline.setVisible(true);
     		if (application.googleApiClient.isConnected()) {
     			Location location = LocationServices.FusedLocationApi.getLastLocation(application.googleApiClient);
     			application.addPoint(location);
     		}
		} else {
			trackPolyline.setVisible(false);
			application.clearPoints();
		}
	}
}
