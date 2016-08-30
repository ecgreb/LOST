package com.mapzen.android.lost.internal;

import com.mapzen.android.lost.api.FusedLocationProviderApi;
import com.mapzen.android.lost.api.LocationAvailability;
import com.mapzen.android.lost.api.LocationCallback;
import com.mapzen.android.lost.api.LocationListener;
import com.mapzen.android.lost.api.LocationRequest;
import com.mapzen.android.lost.api.LostApiClient;
import com.mapzen.android.lost.api.PendingResult;
import com.mapzen.android.lost.api.Status;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.Map;

/**
 * Implementation of the {@link FusedLocationProviderApi}.
 */
public class FusedLocationProviderApiImpl
    implements FusedLocationProviderApi {

  private static final String TAG = FusedLocationProviderApiImpl.class.getSimpleName();

  private final Context context;
  private FusedLocationProviderService service;

  private final ServiceConnection serviceConnection = new ServiceConnection() {
    @Override public void onServiceConnected(ComponentName name, IBinder binder) {
      FusedLocationProviderService.FusedLocationProviderBinder fusedBinder =
          (FusedLocationProviderService.FusedLocationProviderBinder) binder;
      if (fusedBinder != null) {
        service = fusedBinder.getService();
      }

      if (connectionCallbacks != null) {
        connectionCallbacks.onConnected();
      }
      Log.d(TAG, "[onServiceConnected]");
    }

    @Override public void onServiceDisconnected(ComponentName name) {
      if (connectionCallbacks != null) {
        connectionCallbacks.onConnectionSuspended();
      }
      Log.d(TAG, "[onServiceDisconnected]");
    }
  };

  LostApiClient.ConnectionCallbacks connectionCallbacks;

  public FusedLocationProviderApiImpl(Context context) {
    this.context = context;
  }

  public void connect(LostApiClient.ConnectionCallbacks callbacks) {
    Intent intent = new Intent(context, FusedLocationProviderService.class);
    context.startService(intent);

    connectionCallbacks = callbacks;
    intent = new Intent(context, FusedLocationProviderService.class);
    context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
  }

  public void disconnect() {
    context.unbindService(serviceConnection);

    Intent intent = new Intent(context, FusedLocationProviderService.class);
    context.stopService(intent);
  }

  @Override public Location getLastLocation() {
    return service.getLastLocation();
  }

  @Override public LocationAvailability getLocationAvailability() {
    return service.getLocationAvailability();
  }

  @Override public PendingResult<Status> requestLocationUpdates(LocationRequest request,
      LocationListener listener) {
    return service.requestLocationUpdates(request, listener);
  }

  @Override public PendingResult<Status> requestLocationUpdates(LocationRequest request,
      LocationListener listener, Looper looper) {
    throw new RuntimeException("Sorry, not yet implemented");
  }

  @Override public PendingResult<Status> requestLocationUpdates(LocationRequest request,
      LocationCallback callback, Looper looper) {
    return service.requestLocationUpdates(request, callback, looper);
  }

  @Override
  public PendingResult<Status> requestLocationUpdates(LocationRequest request,
      PendingIntent callbackIntent) {
    return service.requestLocationUpdates(request, callbackIntent);
  }

  @Override public PendingResult<Status> removeLocationUpdates(LocationListener listener) {
    return service.removeLocationUpdates(listener);
  }

  @Override public PendingResult<Status> removeLocationUpdates(PendingIntent callbackIntent) {
    return service.removeLocationUpdates(callbackIntent);
  }

  @Override public PendingResult<Status> removeLocationUpdates(LocationCallback callback) {
    return service.removeLocationUpdates(callback);
  }

  @Override public PendingResult<Status> setMockMode(boolean isMockMode) {
    return service.setMockMode(isMockMode);
  }

  @Override public PendingResult<Status> setMockLocation(Location mockLocation) {
    return service.setMockLocation(mockLocation);
  }

  @Override public PendingResult<Status> setMockTrace(File file) {
    return service.setMockTrace(file);
  }

  @Override public boolean isProviderEnabled(String provider) {
    return service.isProviderEnabled(provider);
  }

  public Map<LocationListener, LocationRequest> getListeners() {
    return service.getListeners();
  }

  public FusedLocationProviderService getService() {
    return service;
  }
}
