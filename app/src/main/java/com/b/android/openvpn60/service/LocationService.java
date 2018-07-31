package com.b.android.openvpn60.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.support.v4.content.LocalBroadcastManager;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.helper.LogHelper;


public class LocationService extends MainService implements LocationListener {
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
    private boolean isGPSEnabled = false;
    private boolean isNetworkEnabled = false;
    private boolean canGetLocation = false;
    private Location location; // location
    protected LocationManager locationManager;


    public void onCreate() {
        super.onCreate();
        handlerThread = new HandlerThread("LocationService.HandlerThread");
        handlerThread.start();
        context = getApplicationContext();
        LOG_HELPER = LogHelper.getLogHelper(LocationService.class.getName());
        serviceHandler = new ServiceHandler(handlerThread.getLooper());
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        serviceHandler.post(new Runnable() {
            @Override
            public void run() {
                String action = intent.getAction();
                if (action != null && action.equals(AppConstants.GET_LOCATION.toString())) {
                    responseIntent = new Intent(AppConstants.GET_LOCATION.toString());
                    getLocation();
                }
            }
        });
        return START_STICKY;
    }

    @Override
    public void stopService() {
        localBroadcastManager.sendBroadcast(responseIntent);
        stopUsingGPS();
        stopSelf();
    }

    @SuppressLint("MissingPermission")
    public void getLocation() {
        try {
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!isGPSEnabled && !isNetworkEnabled) {
                LOG_HELPER.logWarning("No Network Provider!");
            } else {
                this.canGetLocation = true;
                // First get location from Network Provider
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        if (locationManager != null) {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                            if (location != null) {
                                responseIntent.putExtra("latitude", location.getLatitude());
                                responseIntent.putExtra("longitude", location.getLongitude());
                            }
                        }
                    }
                } else {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        if (location != null) {
                            responseIntent.putExtra("latitude", location.getLatitude());
                            responseIntent.putExtra("longitude", location.getLongitude());
                        }
                    }
                }
            }
            stopService();
        } catch (Exception e) {
            LOG_HELPER.logException(e);
        }
    }

    public void stopUsingGPS(){
        if(locationManager != null){
            locationManager.removeUpdates(LocationService.this);
        }
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
