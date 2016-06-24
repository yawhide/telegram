package com.telegram.telegram;

import android.*;
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    protected static final String TAG = "map-activity";

    private GoogleMap mMap;
    public static final String SERVER_URI = "http://ubuntu@ec2-107-22-150-246.compute-1.amazonaws.com:5000/";

    private GoogleApiClient mGoogleApiClient;

    private Location mCurrentLocation;
    private final int CHECK_SETTINGS_START_LOCATION_UPDATES = 15;
    private final int CHECK_SETTINGS_GET_LAST_LOCATION = 20;
    private final int CHECK_SETTINGS_INITIALIZE_LOCATION = 25;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 15000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private LocationListener mLocationListener;

    private FloatingActionButton fab;

    private ArrayList<Telegram> unlockedTelegrams = new ArrayList<>();
    private ArrayList<Telegram> lockedTelegrams = new ArrayList<>();

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    void post(String url, RequestBody formBody, Callback cb) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(cb);
    }

    /**
     * Performs get request given a URL and calls the callback.
     * @param url String
     * @param cb Callback
     */
    void get(String url, final Callback cb) {

        Request request = new Request.Builder()
                //.header("Authorization", "token abcd")
                .url(url)
                .build();

        client.newCall(request).enqueue(cb);

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
                Intent i = new Intent(MapsActivity.this, CreateTelegram.class);
                i.putExtra("lat", mCurrentLocation.getLatitude());
                i.putExtra("lng", mCurrentLocation.getLongitude());
                startActivityForResult(i, 123);
            }
        });
        fab.setEnabled(false);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Request code has to match the activity code it was called with
        if (requestCode == 123 && resultCode == RESULT_OK) {
            Log.d("t", "got data from message activity, now post it");
            // Extract the inputted text from the user
            final String telegramMessage = data.getStringExtra("message");

            // Do some error checking on the message, ie. make sure its not bullshit or blank
            Log.d("t", "THIS IS FROM MESSAGE DIALOG: " + telegramMessage);
            final double lat = mCurrentLocation.getLatitude();
            final double lng = mCurrentLocation.getLongitude();

            final String strLat = String.valueOf(mCurrentLocation.getLatitude());
            final String strLng = String.valueOf(mCurrentLocation.getLongitude());

            RequestBody formBody = new FormBody.Builder()
                    .add("uid", "Shayanovic")
                    .add("msg", telegramMessage)
                    .add("img", "nada")
                    .add("lat", strLat)
                    .add("lng", strLng)
                    .build();

            try {
                post(SERVER_URI + "drop", formBody, new Callback() {
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
                                Telegram t = new Telegram(1234567890.0, telegramMessage, "naaaada",
                                        lat, lng);
                                unlockedTelegrams.add(t);
                                addTelegramToMap(t);
                            }
                        });
                    }
                });
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
        } else if (requestCode == CHECK_SETTINGS_START_LOCATION_UPDATES) {
            // check if user put location setting on from createLocationRequest
            if (resultCode == RESULT_OK) {
                startLocationUpdates();
            } else {
                checkLocationSettings(CHECK_SETTINGS_START_LOCATION_UPDATES);
            }
        } else if (requestCode == CHECK_SETTINGS_GET_LAST_LOCATION) {
            if (resultCode == RESULT_OK) {
                getLastLocation();
            } else {
                checkLocationSettings(CHECK_SETTINGS_GET_LAST_LOCATION);
            }
        } else if (requestCode == CHECK_SETTINGS_INITIALIZE_LOCATION) {
            if (resultCode == RESULT_OK) {
                mMap.setMyLocationEnabled(true);
                getLastLocation();
                startLocationUpdates();
                fab.setEnabled(true);
            } else {
                checkLocationSettings(CHECK_SETTINGS_INITIALIZE_LOCATION);
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
        for (Telegram t: unlockedTelegrams) {
            addTelegramToMap(t);
        }
        for (Telegram t: lockedTelegrams) {
            addTelegramToMap(t);
        }
    }


    /**
     * Gets all Telegrams in radius using lastLat and lastLng
     * TODO: Figure out why so many try-catches are needed.
     * @throws JSONException
     */
    private void getTelegrams() throws JSONException {

        int rad = 1;
        String URL = SERVER_URI + "telegrams/within?&" +
                "lat=" + String.valueOf(mCurrentLocation.getLatitude()) +
                "&lng=" + String.valueOf(mCurrentLocation.getLongitude()) +
                "&rad=" + rad;

        get(URL, new Callback() {
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
                String responseData = response.body().string();

                try {
                    // Convert String to json object
                    final JSONObject jsonResp = new JSONObject(responseData);

                    MapsActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            // iterate over two arrays
                            // put marker on the map for 1 telegram
                            try {
                                JSONArray unlockedJsonArray = jsonResp.getJSONArray("1");
                                JSONArray lockedJsonArray = jsonResp.getJSONArray("2");

                                for (int i = 0; i < unlockedJsonArray.length(); i++) {
                                    JSONObject telegramObj = unlockedJsonArray.getJSONObject(i);
                                    Telegram telegram = new Telegram(
                                            telegramObj.getDouble("uid"),
                                            telegramObj.getString("msg"),
                                            telegramObj.getString("img"),
                                            telegramObj.getJSONObject("loc").getJSONArray("coordinates").getDouble(1),
                                            telegramObj.getJSONObject("loc").getJSONArray("coordinates").getDouble(0));
                                    unlockedTelegrams.add(telegram);
                                    //addTelegramToMap(telegram);
                                }

                                for (int i = 0; i < lockedJsonArray.length(); i++) {
                                    JSONObject telegramObj = unlockedJsonArray.getJSONObject(i);
                                    Telegram telegram = new Telegram(
                                            telegramObj.getDouble("uid"),
                                            telegramObj.getString("msg"),
                                            telegramObj.getString("img"),
                                            telegramObj.getJSONObject("loc").getJSONArray("coordinates").getDouble(1),
                                            telegramObj.getJSONObject("loc").getJSONArray("coordinates").getDouble(0));
                                    lockedTelegrams.add(telegram);
                                    //addTelegramToMap(telegram);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }

                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


//    @Override
//    public void OnMarkerClickListener(Marker marker) {
//
//    }

    /**
     * Adds a Telegram to the map and colours it according to unlockable or locked.
     * @param telegram
     */
    private void addTelegramToMap(Telegram telegram) {
        mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(telegram.getLat(), telegram.getLng()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow))
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CHECK_SETTINGS_GET_LAST_LOCATION) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                checkLocationSettings(requestCode);
            }
        } else if (requestCode == CHECK_SETTINGS_INITIALIZE_LOCATION) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                getLastLocation();
                startLocationUpdates();
                fab.setEnabled(true);
            } else {
                checkLocationSettings(requestCode);
            }
        } else if (requestCode == CHECK_SETTINGS_START_LOCATION_UPDATES) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                checkLocationSettings(requestCode);
            }
        }
    }

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
        mGoogleApiClient.connect();
        createLocationRequest();
        createLocationListener();
//        checkLocationSettings(CHECK_SETTINGS_INITIALIZE_LOCATION);
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
                        if (activityResultCode == 0) {
                            break;
                        } else if (activityResultCode == CHECK_SETTINGS_START_LOCATION_UPDATES) {
                            startLocationUpdates();
                        } else if (activityResultCode == CHECK_SETTINGS_GET_LAST_LOCATION) {
                            getLastLocation();
                        } else if (activityResultCode == CHECK_SETTINGS_INITIALIZE_LOCATION) {
                            if (Build.VERSION.SDK_INT >= 23
                                    && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MapsActivity.this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, activityResultCode);
                            }
                            mMap.setMyLocationEnabled(true);
                            getLastLocation();
                            startLocationUpdates();
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
                        showLocationAlert(activityResultCode);
                        break;
                }
            }
        });
    }

    protected void createLocationRequest() {
        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
            mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, mLocationListener);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, mLocationListener);
//        PendingResult<Status> result = LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                super.onLocationResult(locationResult);
//
//            }
//        });
    }

    protected void getLastLocation() {
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            updateUI();
        }
    }

    private void updateUI() {
        if (mCurrentLocation == null){
            return;
        }
        String text = String.format("Lat: %f, Lng: %f, Update time: %s",
                mCurrentLocation.getLatitude(),
                mCurrentLocation.getLongitude(),
                mLastUpdateTime);
        Toast.makeText(MapsActivity.this, text, Toast.LENGTH_LONG).show();
    }

    private void showLocationAlert(final int activityResultCode) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
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
                        checkLocationSettings(activityResultCode);
                    }
                });

        alertDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "telegram started");
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "telegram resumed");
        if (mGoogleApiClient.isConnected()) {
            checkLocationSettings(CHECK_SETTINGS_START_LOCATION_UPDATES);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "telegram paused");
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "telegram stoped");
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "google api client connected");
        checkLocationSettings(CHECK_SETTINGS_INITIALIZE_LOCATION);
//        checkLocationSettings(CHECK_SETTINGS_GET_LAST_LOCATION);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "google api client suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "google api client failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }
}
