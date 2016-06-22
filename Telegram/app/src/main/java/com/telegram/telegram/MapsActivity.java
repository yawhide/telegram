package com.telegram.telegram;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private static final String LOG_PREFIX = "telegramLogging";
    private GoogleApiClient googleApiClient;
    private Location mLastLocation;
    private GPS gps;
    private GPS.LocationPermissionResponseListener gpsPermissionListener;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
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
                        Log.d(LOG_PREFIX, responseData);
                    }
                });
            }
        });
    }

    public static boolean isLocationEnabled(Activity activity) {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_PREFIX, "they do not have location services on");
                ActivityCompat.requestPermissions(activity, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.INTERNET
                }, 10);
            } else {
                Log.d(LOG_PREFIX, String.valueOf(ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION))
                        + " " + String.valueOf(ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION))
                        + " " + PackageManager.PERMISSION_GRANTED
                        + " " + PackageManager.PERMISSION_DENIED);
                Log.d(LOG_PREFIX, "user has location services on and is using android M or greater, current location:", );
            }
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(activity.getContentResolver(), Settings.Secure.LOCATION_MODE);
                Log.d(LOG_PREFIX, locationMode + " " + Settings.Secure.LOCATION_MODE_OFF);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context that = this;

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_PREFIX, "ayee");
                try {
                    get("http://107.22.150.246:5000");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        gps = GPS.sharedInstance(this);

//        mLocationListener = new LocationListener() {
//            @Override
//            public void onLocationChanged(final Location location) {
//                mLastLocation = location;
//                double lat = location.getLatitude(), lon = location.getLongitude();
//                LatLng newPos = new LatLng(lat, lon);
//                Log.d(LOG_PREFIX, lat + " " + lon + " " + newPos.toString());
//                // location changes here
//                Toast.makeText(that, "New lat/lon: " + lat + " " + lon, Toast.LENGTH_LONG).show();
//
//                mMap.addMarker(new MarkerOptions().position(newPos));
//                mMap.moveCamera(CameraUpdateFactory.newLatLng(newPos));
//            }
//
//            @Override
//            public void onStatusChanged(String provider, int status, Bundle extras) {
//
//            }
//
//            @Override
//            public void onProviderEnabled(String provider) {
//                Log.d(LOG_PREFIX, "on provider enabled...");
//            }
//
//            @Override
//            public void onProviderDisabled(String provider) {
//                // not sure what should go here, i thin kwe restart the activity?
//                Log.d(LOG_PREFIX, "on provider disabled... they dont have location services on");
//                if (isLocationEnabled(MapsActivity.this)) {
//                    // do nothing
//                    Log.d(LOG_PREFIX, "user has location on");
//                } else {
//                    Toast.makeText(MapsActivity.this, "Must have location services on", Toast.LENGTH_LONG).show();
//                    Log.d(LOG_PREFIX, "user does not have location on");
//                }
//            }
//        };
//        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//        if (isLocationEnabled(MapsActivity.this)) {
//            Log.d(LOG_PREFIX, "user has location on");
//            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, mLocationListener);
//        } else {
//            Toast.makeText(MapsActivity.this, "Must have location services on", Toast.LENGTH_LONG).show();
//            Log.d(LOG_PREFIX, "need to check if they have location on");
//        }
    }

//    @Override
//    protected void onPostCreate(Bundle savedInstanceState) {
//        super.onPostCreate(savedInstanceState);
//        int count = 0;
//        do {
//            if(count == 0){
//                try{
//                    Thread.sleep(1000);
//                }catch(InterruptedException e){
//                    Log.e(LOG_PREFIX, e.getMessage());
//                }
//            }
//            count++;
//        } while(mMap == null && mLastLocation == null && count < 5);
//
//
//    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        new GPS.LocationPermissionResponseListener() {
            @Override
            public void onResponse(Boolean permissionGranted) {
                if (permissionGranted) {
                    mMap.setMyLocationEnabled(true);
                }
            }
        };

//        if (isLocationEnabled(MapsActivity.this)) {
////            LatLng lastPos = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
////            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastPos, 15));
////            mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
//        } else {
//            Toast.makeText(MapsActivity.this, "Must have location services on", Toast.LENGTH_LONG).show();
//            Log.d(LOG_PREFIX, "user does not have location on in map ready");
//        }
    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (googleApiClient != null) {
//            googleApiClient.connect();
//        }
//    }
//
//    @Override
//    protected void onStop() {
//        googleApiClient.disconnect();
//        super.onStop();
//    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        Log.d(LOG_PREFIX, "not sure what on request permission result is used for");
//        switch (requestCode) {
//            case 10:
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // All good!
//                } else {
//                    Toast.makeText(this, "Need your location!", Toast.LENGTH_LONG).show();
//                }
//                break;
//            default:
//                Log.d(LOG_PREFIX, "we got a requestCode that is not 10" + " " + requestCode);
//                break;
//        }
//    }

//    @Override
//    public void onConnected(@Nullable Bundle bundle) {
//        //        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
////                == PackageManager.PERMISSION_GRANTED) {
////
////
//////            Location lastLocation = LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, );
////
////        }
//        if (isLocationEnabled(MapsActivity.this)) {
//            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
//        } else {
//            Toast.makeText(MapsActivity.this, "Must have location services on", Toast.LENGTH_LONG).show();
//            Log.d(LOG_PREFIX, "user does not have location on in google services on connected");
//        }
//    }
//
//    @Override
//    public void onConnectionSuspended(int i) {
//
//    }
//
//    @Override
//    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
//        Log.e(LOG_PREFIX, "Failed to connect to google play services. " + String.valueOf(connectionResult));
//        mLastLocation = new Location(LocationManager.GPS_PROVIDER);
//        mLastLocation.setLatitude(43.4807540);
//        mLastLocation.setLongitude(-80.5242860);
//    }
}
