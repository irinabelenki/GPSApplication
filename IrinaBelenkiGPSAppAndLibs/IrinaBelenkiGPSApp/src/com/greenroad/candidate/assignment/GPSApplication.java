package com.greenroad.candidate.assignment;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.collections15.buffer.CircularFifoBuffer;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;

public class GPSApplication extends Application {
	public GoogleApiClient googleApiClient;
    public static final String TAG = "GPSApplication";
    
    /* Collect points here while in background; copy into activity on start */
    PendingIntent backgroundPendingIntent;
    LocationRequest backgroundLocationRequest;
    
    CircularFifoBuffer<LatLng> trackingPoints = new CircularFifoBuffer<LatLng>(1000);
    CircularFifoBuffer<Location> speedingPoints = new CircularFifoBuffer<Location>(20);
    Location lastAddedSpeedLocation = null;
    boolean trackingEnabled = false;

    
	@Override
	public void onCreate() {
		super.onCreate();
		
		googleApiClient = new GoogleApiClient.Builder(this)
        .addApi(LocationServices.API)
        .build();
		
		Intent intent = new Intent(this,LSService.class);
		backgroundPendingIntent = PendingIntent.getService(this, 1, intent, 0);
		
		backgroundLocationRequest = new LocationRequest()
				.setInterval(10000)
				.setFastestInterval(5000)
				.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

		googleApiClient.connect();
		
    }
	
	void startBackgroundCollection() {
		if (googleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
				backgroundLocationRequest, backgroundPendingIntent);
		}
		
	}
	
	void stopBackgroundLocation() {
		if (googleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,
				backgroundPendingIntent);
		}
	}
	
	synchronized void clearPoints() {
		trackingPoints.clear();
	}
	
	synchronized void copyTrackingPoints(LinkedList<LatLng> list) {
		for (LatLng ll : trackingPoints)
			list.add(ll);
	}
	
	synchronized void addPoint(Location l) {
		if (trackingEnabled)
			trackingPoints.add(new LatLng(l.getLatitude(), l.getLongitude()));
		
		if (lastAddedSpeedLocation != null) {
			if ((l.getTime() - lastAddedSpeedLocation.getTime()) > 100) {
				speedingPoints.add(l);
				lastAddedSpeedLocation = l;
			}
		} else {
			speedingPoints.add(l);
			lastAddedSpeedLocation = l;
		}
	}
	
	synchronized double getAverageSpeed() {
		if (speedingPoints.size() < 2)
			return 0;
		
		Iterator<Location> it1 = speedingPoints.iterator(), it2 = speedingPoints.iterator();
		long time1, time2;
		Location l1, l2;
		double totalSpeed = 0;
		
		it2.next();
		
		while(it2.hasNext()) {
			l1 = it1.next();
			l2 = it2.next();
			
			time1 = l1.getTime();
			time2 = l2.getTime();	

			float distance = l1.distanceTo(l2);
			float speedMps = distance * 1000 / ( time2 - time1 );
			float speedKph = (speedMps * 3600.0f) / 1000.0f;
			
			totalSpeed += speedKph;
		}
		return totalSpeed/(speedingPoints.size() - 1);
	}

}