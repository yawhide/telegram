package com.telegram.telegram;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    protected static final String TAG = "map-activity";

    private GoogleMap mMap;
    public static final String SERVER_URI = "http://ubuntu@ec2-107-22-150-246.compute-1.amazonaws.com:5000/";

    private GoogleApiClient mGoogleApiClient;

    private Location mCurrentLocation;
    private final int REQUEST_CHECK_SETTINGS = 15;
    private final int REQUEST_GET_LAST_LOCATION = 20;
    private final int REQUEST_INITIAL_CHECK_SETTINGS = 25;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private LocationListener mLocationListener;

    private FloatingActionButton fab;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    void post(String url, RequestBody formBody) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                // Read data on the worker thread
                final String responseData = response.body().string();

                // Run view-related code back on the main thread
                MapsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("t", responseData);
                    }
                });
            }
        });
    }

    void get(String url) throws IOException {
        Request request = new Request.Builder()
                //.header("Authorization", "token abcd")
                .url(url)
                .build();
        // Get a handler that can be used to post to the main thread
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                // Read data on the worker thread
                final String responseData = response.body().string();

                // Run view-related code back on the main thread
                MapsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("t", responseData);
                    }
                });
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLastUpdateTime = "";
        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MapsActivity.this, TelegramMessage.class);
                i.putExtra("lat", mCurrentLocation.getLatitude());
                i.putExtra("lng", mCurrentLocation.getLongitude());
                startActivityForResult(i, 123);
            }
        });
        fab.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Request code has to match the activity code it was called with
        if (requestCode == 123 && resultCode == RESULT_OK) {
            Log.d("t", "got data from message activity, now post it");
            // Extract the inputted text from the user
            String telegramMessage = data.getStringExtra("message");

            // Do some error checking on the message, ie. make sure its not bullshit or blank
            Log.d("t", "THIS IS FROM MESSAGE DIALOG: " + telegramMessage);

            RequestBody formBody = new FormBody.Builder()
                    .add("uid", "Shayanovic")
                    .add("msg", telegramMessage)
                    .add("img", "nada")
                    .add("lat", String.valueOf(mCurrentLocation.getLatitude()))
                    .add("lng", String.valueOf(mCurrentLocation.getLongitude()))
                    .build();

            try {
                post(SERVER_URI + "drop", formBody);
//                addTelegramToMap();
            } catch (IOException e) {
                e.printStackTrace();
            }
            /*  Do some shit here that actually posts the data

            try {
                get("http://107.22.150.246:5000");
            } catch (IOException e) {
                e.printStackTrace();
            }
            */
        } else if (requestCode == REQUEST_CHECK_SETTINGS) {
            // check if user put location setting on from createLocationRequest
            if (resultCode == RESULT_OK) {
                startLocationUpdates();
            } else {
                checkLocationSettings(REQUEST_CHECK_SETTINGS);
            }
        } else if (requestCode == REQUEST_GET_LAST_LOCATION) {
            if (resultCode == RESULT_OK) {
                getLastLocation();
            } else {
                checkLocationSettings(REQUEST_GET_LAST_LOCATION);
            }
        } else if (requestCode == REQUEST_INITIAL_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                mMap.setMyLocationEnabled(true);
                fab.setEnabled(true);
            } else {
                checkLocationSettings(REQUEST_INITIAL_CHECK_SETTINGS);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
//        LatLng waterloo = new LatLng(43.4807540, -80.5242860);
//        mMap.addMarker(new MarkerOptions().position(waterloo).title("Marker in Waterloo"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(waterloo));
    }

//    @Override
//    public void OnMarkerClickListener(Marker marker) {
//
//    }

    private void addTelegramToMap(Telegram telegram) {
        mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(telegram.getLat(), telegram.getLng()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow))
        );
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == 100) {
//            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
//                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
//                enable_locations();
//            } else {
//                locationIsOn(MapsActivity.this);
//            }
//        }
//    }

    // ================================ Location stuff ============================ //
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
        createLocationListener();
        checkLocationSettings(REQUEST_INITIAL_CHECK_SETTINGS);
    }

    protected void createLocationListener() {
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mCurrentLocation = location;
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateUI();
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude())));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
            }
        };
    }

    protected void checkLocationSettings(final int activityResultCode) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates locationSettingState = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        if (activityResultCode == REQUEST_CHECK_SETTINGS) {
                            startLocationUpdates();
                        } else if (activityResultCode == REQUEST_GET_LAST_LOCATION) {
                            getLastLocation();
                        } else if (activityResultCode == REQUEST_INITIAL_CHECK_SETTINGS) {
                            mMap.setMyLocationEnabled(true);
                            fab.setEnabled(true);
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MapsActivity.this,
                                    activityResultCode);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
                        alertDialog.setTitle("GPS Settings");
                        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu ?");

                        alertDialog.setPositiveButton("Settings",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(
                                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        MapsActivity.this.startActivity(intent);
                                    }
                                });

                        alertDialog.setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });

                        alertDialog.show();
                        break;
                }
            }
        });
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, mLocationListener);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, mLocationListener);
    }

    protected void getLastLocation() {
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            if (mCurrentLocation != null) {
                updateUI();
            }
        }
    }

    private void updateUI() {
        String text = String.format("Lat: %f, Lng: %f, Update time: %s",
                mCurrentLocation.getLatitude(),
                mCurrentLocation.getLongitude(),
                mLastUpdateTime);
        Toast.makeText(MapsActivity.this, text, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            checkLocationSettings(REQUEST_CHECK_SETTINGS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        checkLocationSettings(REQUEST_GET_LAST_LOCATION);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }
}
