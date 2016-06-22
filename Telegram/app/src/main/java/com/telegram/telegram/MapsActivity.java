package com.telegram.telegram;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    public static final String SERVER_URI = "http://ubuntu@ec2-107-22-150-246.compute-1.amazonaws.com:5000/";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    void post(String url, RequestBody formBody) throws IOException {

        formBody = new FormBody.Builder()
                .add("uid", "NERD")
                .add("msg", "i r nerd")
                .add("img", "nada")
                .add("lat", "43.432540")
                .add ("lng", "-80.522320")
                .build();

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

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                // This method call initalizes a new activity and when the 2nd activity returns
                // it calls onActivityResult
                startActivityForResult(new Intent(getApplicationContext(), TelegramMessage.class), 123);
            }
        });

        final Context that = this;

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                double lat = location.getLatitude(), lon = location.getLongitude();
                LatLng newPos = new LatLng(lat, lon);
                Log.d("t", lat + " " + lon + " " + newPos.toString());
                // location changes here
                Toast.makeText(that, "New lat/lon: " + lat + " " + lon, Toast.LENGTH_LONG).show();

                mMap.addMarker(new MarkerOptions().position(newPos));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(newPos));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d("t", "on provider enabled...");
            }

            @Override
            public void onProviderDisabled(String provider) {
                // not sure what should go here, i thin kwe restart the activity?
                Log.d("t", "on provider disabled... they dont have location services on");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        Log.d("t", "they do not have location services on");
                        ActivityCompat.requestPermissions(MapsActivity.this, new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                android.Manifest.permission.INTERNET
                        }, 10);
                    }
                } else {
                    if (isLocationEnabled(MapsActivity.this)) {
                        Toast.makeText(MapsActivity.this, "Must have location services on", Toast.LENGTH_LONG).show();
                    } else {
                        Log.d("t", "user does not have location on");
                    }
                }
            }
        };
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("t", "they do not have location services on");
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.INTERNET
                }, 10);
            }
        } else {
            if (isLocationEnabled(MapsActivity.this)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, mLocationListener);
            } else {
                Log.d("t", "need to check if they have location on");
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Request code has to match the activity code it was called with
        if(requestCode == 123 && resultCode == RESULT_OK) {
            Log.d("t", "got data from message activity, now post it");
            // Extract the inputted text from the user
            String telegramMessage = data.getStringExtra("message");

            // Do some error checking on the message, ie. make sure its not bullshit or blank
            Log.d("t", "THIS IS FROM MESSAGE DIALOG: " + telegramMessage);

            RequestBody formBody = new FormBody.Builder()
                    .add("uid", "Stefanovic")
                    .add("msg", "i r nerd")
                    .add("img", "nada")
                    .add("lat", "43.4807540")
                    .add("lng", "-80.5242860")
                    .build();

            try {
                post(SERVER_URI + "drop", formBody);
            }
            catch (IOException e) {
                e.printStackTrace();
            }



            /*  Do some shit here that actually posts the data

            try {
                get("http://107.22.150.246:5000");
            } catch (IOException e) {
                e.printStackTrace();
            }
            */
        }
    }

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

        // Add a marker in Sydney and move the camera
        LatLng waterloo = new LatLng(43.4807540, -80.5242860);
        mMap.addMarker(new MarkerOptions().position(waterloo).title("Marker in Waterloo"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(waterloo));
    }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                } else {
                    Toast.makeText(this, "Need your location!", Toast.LENGTH_LONG).show();
                }

                break;
            default:
                break;
        }
    }

//    @Override
//    public void onConnected(Bundle bundle) {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED) {
//
//
////            Location lastLocation = LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, );
//
//        }
//    }

}
