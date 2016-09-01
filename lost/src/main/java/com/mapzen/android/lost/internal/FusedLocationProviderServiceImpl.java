package com.mapzen.android.lost.internal;

import com.mapzen.android.lost.api.LocationAvailability;
import com.mapzen.android.lost.api.LocationCallback;
import com.mapzen.android.lost.api.LocationListener;
import com.mapzen.android.lost.api.LocationRequest;
import com.mapzen.android.lost.api.LocationResult;
import com.mapzen.android.lost.api.LostApiClient;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.RequiresPermission;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class FusedLocationProviderServiceImpl implements LocationEngine.Callback {

  private Context context;

  private boolean mockMode;
  private LocationEngine locationEngine;

  private ClientManager clientManager;

  public FusedLocationProviderServiceImpl(Context context, ClientManager manager) {
    this.context = context;
    this.clientManager = manager;
    locationEngine = new FusionEngine(context, this);
  }

  public void shutdown() {
    locationEngine.setRequest(null);
    clientManager.shutdown();
  }

  public Location getLastLocation(LostApiClient apiClient) {
    return locationEngine.getLastLocation();
  }

  @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
  public LocationAvailability getLocationAvailability(LostApiClient apiClient) {
    return locationEngine.createLocationAvailability();
  }

  public void requestLocationUpdates(LostApiClient apiClient, LocationRequest request,
      LocationListener listener) {
    clientManager.addListener(apiClient, request, listener);
    locationEngine.setRequest(request);
  }

  public void requestLocationUpdates(LostApiClient apiClient, LocationRequest request,
      PendingIntent callbackIntent) {
    clientManager.addPendingIntent(apiClient, request, callbackIntent);
    locationEngine.setRequest(request);
  }

  public void requestLocationUpdates(LostApiClient apiClient, LocationRequest request,
      LocationCallback callback, Looper looper) {
    clientManager.addLocationCallback(apiClient, request, callback, looper);
    locationEngine.setRequest(request);
  }

  public void removeLocationUpdates(LostApiClient apiClient, LocationListener listener) {
    clientManager.removeListener(apiClient, listener);
    checkAllListenersPendingIntentsAndCallbacks();
  }

  public void removeLocationUpdates(LostApiClient apiClient, PendingIntent callbackIntent) {
    clientManager.removePendingIntent(apiClient, callbackIntent);
    checkAllListenersPendingIntentsAndCallbacks();
  }

  public void removeLocationUpdates(LostApiClient apiClient, LocationCallback callback) {
    clientManager.removeLocationCallback(apiClient, callback);
    checkAllListenersPendingIntentsAndCallbacks();
  }

  public void setMockMode(LostApiClient apiClient, boolean isMockMode) {
    if (mockMode != isMockMode) {
      toggleMockMode();
    }
  }

  public void setMockLocation(LostApiClient apiClient, Location mockLocation) {
    if (mockMode) {
      ((MockEngine) locationEngine).setLocation(mockLocation);
    }
  }

  public void setMockTrace(LostApiClient apiClient, File file) {
    if (mockMode) {
      ((MockEngine) locationEngine).setTrace(file);
    }
  }

  public boolean isProviderEnabled(LostApiClient apiClient, String provider) {
    return locationEngine.isProviderEnabled(provider);
  }

  @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
  public void reportLocation(Location location) {
    clientManager.reportLocationChanged(location);

    LocationAvailability availability = locationEngine.createLocationAvailability();
    ArrayList<Location> locations = new ArrayList<>();
    locations.add(location);
    final LocationResult result = LocationResult.create(locations);
    clientManager.sendPendingIntent(context, location, availability, result);


    clientManager.reportLocationResult(result);
  }

  @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
  public void reportProviderDisabled(String provider) {
    clientManager.reportProviderDisabled(provider);
    notifyLocationAvailabilityChanged();
  }

  @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
  public void reportProviderEnabled(String provider) {
    clientManager.reportProviderEnabled(provider);
    notifyLocationAvailabilityChanged();
  }

  public Map<LostApiClient, Map<LocationListener, LocationRequest>> getListeners() {
    return clientManager.getListeners();
  }

  public Map<LostApiClient, Map<PendingIntent, LocationRequest>> getPendingIntents() {
    return clientManager.getPendingIntents();
  }

  public Map<LostApiClient, Map<LocationCallback, Looper>> getLocationListeners() {
    return clientManager.getLocationListeners();
  }

  public void disconnect(LostApiClient client) {
    clientManager.disconnect(client);
  }

  /**
   * Checks if any listeners or pending intents are still registered for location updates. If not,
   * then shutdown the location engines.
   */
  private void checkAllListenersPendingIntentsAndCallbacks() {
    if (clientManager.hasNoListeners()) {
      locationEngine.setRequest(null);
    }
  }

  private void toggleMockMode() {
    mockMode = !mockMode;
    locationEngine.setRequest(null);
    if (mockMode) {
      locationEngine = new MockEngine(context, this);
    } else {
      locationEngine = new FusionEngine(context, this);
    }
  }

  @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
  private void notifyLocationAvailabilityChanged() {
    final LocationAvailability availability = locationEngine.createLocationAvailability();
    clientManager.notifyLocationAvailability(availability);
  }

}
