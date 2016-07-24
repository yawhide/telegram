package com.telegram.telegram;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
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

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
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
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import okhttp3.Call;
import okhttp3.Callback;
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

    private GoogleSignInAccount user;
    private String testUserEmail;

    private GoogleMap mMap;
    public static final String SERVER_URI = "http://ubuntu@ec2-107-22-150-246.compute-1.amazonaws.com:5000/";

    private GoogleApiClient mGoogleApiClient;

    private Location mCurrentLocation;
    private final int CHECK_SETTINGS_START_LOCATION_UPDATES = 15;
    private final int CHECK_SETTINGS_GET_LAST_LOCATION = 20;
    private final int CHECK_SETTINGS_INITIALIZE_LOCATION = 25;
    private final int CHECK_LOCATION_ON_CREATE = 30;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private LocationListener mLocationListener;
    private float GOOGLE_MAP_DEFAULT_ZOOM = 16.0f;
    private static final int ANIMATION_DURATION = 500;
    private boolean movedCameraToFirstUpdate = false;

    private ClusterManager<ClusterTelegram> telegramClusterManager;
    private ArrayList<ClusterTelegram> telegramCluster;

    private FloatingActionButton fab;
    private FloatingActionButton logoutFab;


//    private HashMap<String, Telegram> unlockedTelegrams = new HashMap<>();
//    private HashMap<String, Telegram> lockedTelegrams = new HashMap<>();

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

        Intent intent = getIntent();
        if (intent.getExtras().get("oath") != null) {
            user = (GoogleSignInAccount) intent.getExtras().get("oath");
            Log.d(TAG, user.getEmail() + " signed in");
        } else {
            testUserEmail = "avie@gmail.com";
            Log.d(TAG, "failed to sign in...using " + testUserEmail);
        }

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLastUpdateTime = "";
        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();

        if (Build.VERSION.SDK_INT >= 23
                && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, CHECK_LOCATION_ON_CREATE);
        }
        createLocationRequest();
        createLocationListener();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MapsActivity.this, CreateTelegram.class);
                i.putExtra("lat", mCurrentLocation.getLatitude());
                i.putExtra("lng", mCurrentLocation.getLongitude());
                i.putExtra("uid", user != null ? user.getEmail() : testUserEmail);
                startActivityForResult(i, 123);
            }
        });
        fab.setEnabled(false);

        logoutFab = (FloatingActionButton) findViewById(R.id.logout_fab);
        logoutFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Logout, aka just finish this maps activity
                finish();
            }
        });

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Request code has to match the activity code it was called with
        if (requestCode == 123 && resultCode == RESULT_OK) {
            Log.d(TAG, "got data from message activity, now post it");
            // Extract the inputted text from the user
            final Telegram telegram = (Telegram) data.getExtras().get("telegram");

            RequestBody formBody = telegram.createDropFormBody();

            try {
                post(SERVER_URI + "telegrams/drop", formBody, new Callback() {
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
                                Log.d(TAG, responseData);
                                addTelegramToMap(telegram);
//                                unlockedTelegrams.put(telegram.getTid(), telegram);
                            }
                        });
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        else if (requestCode == 124) {
            final Telegram telegram = (Telegram) data.getExtras().get("telegram");

            RequestBody formBody = telegram.createSeenFormBody(user != null ? user.getEmail() : testUserEmail);
            try {
                post(SERVER_URI + "telegrams/seen", formBody, new Callback() {
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
                        telegram.setSeen(true);
                        pollForNewTelegrams();

                        // Run view-related code back on the main thread
                        MapsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, responseData);
                            }
                        });
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.animateCamera(CameraUpdateFactory.zoomTo(GOOGLE_MAP_DEFAULT_ZOOM));

        telegramCluster = new ArrayList<ClusterTelegram>();
        telegramClusterManager = new ClusterManager<ClusterTelegram>(this, mMap);
        telegramClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<ClusterTelegram>() {
            @Override
            public boolean onClusterClick(Cluster<ClusterTelegram> cluster) {
                // create telegram listview
                Log.i(TAG, "Map cluster clicked: " + cluster.getSize() + " items.");
                ArrayList<Telegram> telegrams = new ArrayList<Telegram>();
                for(ClusterTelegram ct : cluster.getItems()) {
                    final Telegram telegram = ct.getTelegram();
                    telegrams.add(telegram);
                    RequestBody formBody = telegram.createSeenFormBody(user != null ? user.getEmail() : testUserEmail);
                    try {
                        post(SERVER_URI + "telegrams/seen", formBody, new Callback() {
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
                                telegram.setSeen(true);
                                pollForNewTelegrams();

                                // Run view-related code back on the main thread
                                MapsActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, responseData);
                                    }
                                });
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Intent i = new Intent(MapsActivity.this, TelegramListActivity.class);
                i.putExtra("telegrams", telegrams);
                startActivityForResult(i, 125);
                return false;
            }
        });
//        telegramClusterManager.setOnClusterItemClickListener(new telegramClusterManager.onClusterItemClickListener() {
//            @Override
//            public boolean onMarkerClick(Marker marker) {
//                Telegram telegram = null;
//                Boolean found = false;
//
//                if (unlockedTelegrams.containsKey(marker.getId())) {
//                    telegram = unlockedTelegrams.get(marker.getId());
//                    found = true;
//                }
//                else if (lockedTelegrams.containsKey(marker.getId())) {
//                    if (lockedTelegrams.get(marker.getId()).getSeen()) {
//                        telegram = lockedTelegrams.get(marker.getId());
//                        found = true;
//                    }
//                }
//
//                if (found) {
//                    Intent i = new Intent(MapsActivity.this, ViewTelegram.class);
//                    i.putExtra("telegram", telegram);
//
//                    startActivityForResult(i, 124);
//                }
//                else {
//                    final CharSequence[] items = { "I understand" };
//                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
//                    builder.setTitle("You must be in a 1 mile radius to view this telegram.");
//                    builder.setItems(items, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int item) {
//
//                            if (items[item].equals("I understand")) {
//                                dialog.dismiss();
//                            }
//                        }
//                    });
//
//                    builder.show();
//                }
//
//                return true;
//            }
//        });

        telegramClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<ClusterTelegram>() {
            @Override
            public boolean onClusterItemClick(ClusterTelegram ct) {
                if (!ct.isLocked()) {
                    Intent i = new Intent(MapsActivity.this, ViewTelegram.class);
                    i.putExtra("telegram", ct.getTelegram());

                    startActivityForResult(i, 124);
                }
                else {
                    final CharSequence[] items = { "I understand" };
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setTitle("You must be in a 1 mile radius to view this telegram.");
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {

                            if (items[item].equals("I understand")) {
                                dialog.dismiss();
                            }
                        }
                    });

                    builder.show();
                }

                return true;
            }
        });
        telegramClusterManager.setRenderer(new DefaultClusterRenderer<ClusterTelegram>(MapsActivity.this, mMap, telegramClusterManager) {

            @Override
            protected void onBeforeClusterItemRendered(ClusterTelegram ct, MarkerOptions markerOptions) {
                float colour = ct.getSeen() ? BitmapDescriptorFactory.HUE_YELLOW : ct.isLocked() ? BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_GREEN;
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(colour));
                super.onBeforeClusterItemRendered(ct, markerOptions);
            }
        });
        mMap.setOnCameraChangeListener(telegramClusterManager);
        mMap.setOnMarkerClickListener(telegramClusterManager);
    }


    private void pollForNewTelegrams() {

        int rad = 1;
        String URL = SERVER_URI + "telegrams/within?" +
                "lat=" + String.valueOf(mCurrentLocation.getLatitude()) +
                "&lng=" + String.valueOf(mCurrentLocation.getLongitude()) +
                "&rad=" + rad +
                "&uid=" + (user != null ? user.getEmail() : testUserEmail);

        get(URL, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) {
                try {
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
                                    JSONArray seenTelegramsArray = jsonResp.getJSONArray("3");

                                    // Create these arrays for later if we need them
                                    ArrayList<Telegram> unlocked = new ArrayList<Telegram>();
                                    ArrayList<Telegram> locked = new ArrayList<Telegram>();

                                    Set<String> seen  = new TreeSet<String>();

                                    for (int i = 0; i < seenTelegramsArray.length(); i++) {
                                        JSONObject telegramObj = seenTelegramsArray.getJSONObject(i);
//                                        if (!telegramObj.getString("uid").equals((user != null ? user.getEmail() : testUserEmail))) continue;
                                        seen.add(telegramObj.getString("tid"));
                                    }

                                    for (int i = 0; i < unlockedJsonArray.length(); i++) {
                                        JSONObject telegramObj = unlockedJsonArray.getJSONObject(i);
//                                        if (!telegramObj.getString("uid").equals((user != null ? user.getEmail() : testUserEmail))) continue;
                                        JSONObject tid = (JSONObject) telegramObj.get("_id");
                                        String strTid = (String) tid.get("$oid");
                                        Telegram telegram = new Telegram(
                                                telegramObj.getString("uid"),
                                                strTid,
                                                telegramObj.getString("msg"),
                                                telegramObj.getString("img"),
                                                telegramObj.getJSONObject("loc").getJSONArray("coordinates").getDouble(1),
                                                telegramObj.getJSONObject("loc").getJSONArray("coordinates").getDouble(0),
                                                false);

                                        if (seen.contains(strTid)) {
                                            telegram.setSeen(true);
                                        } else{
                                            telegram.setSeen(false);
                                        }

                                        unlocked.add(telegram);
                                    }

                                    for (int i = 0; i < lockedJsonArray.length(); i++) {
                                        JSONObject telegramObj = lockedJsonArray.getJSONObject(i);
//                                        if (!telegramObj.getString("uid").equals((user != null ? user.getEmail() : testUserEmail))) continue;
                                        JSONObject tid = (JSONObject) telegramObj.get("_id");
                                        String strTid = (String) tid.get("$oid");
                                        Telegram telegram = new Telegram(
                                                telegramObj.getString("uid"),
                                                strTid,
                                                telegramObj.getString("msg"),
                                                telegramObj.getString("img"),
                                                telegramObj.getJSONObject("loc").getJSONArray("coordinates").getDouble(1),
                                                telegramObj.getJSONObject("loc").getJSONArray("coordinates").getDouble(0),
                                                true);

                                        if (seen.contains(strTid)) {
                                            telegram.setSeen(true);
                                        } else{
                                            telegram.setSeen(false);
                                        }

                                        locked.add(telegram);
                                    }

//                                    unlockedTelegrams.clear();
//                                    lockedTelegrams.clear();
//                                    mMap.clear();

                                    // prune telegrams that do not exist anymore
                                    ArrayList<Telegram> telegrams = new ArrayList<Telegram>();
                                    telegrams.addAll(locked);
                                    telegrams.addAll(unlocked);
                                    for(ClusterTelegram ct : telegramCluster) {
                                        Boolean exists = false;
                                        for (Telegram t : telegrams) {
                                            if (ct.getTid().equals(t.getTid())) {
                                                exists = true;
                                                break;
                                            }
                                        }
                                        if (!exists) {
                                            telegramClusterManager.removeItem(ct);
                                        }
                                    }

                                    for (Telegram t : locked) {
                                        addTelegramToMap(t);
//                                        lockedTelegrams.put(t.getTid(), t);
                                    }
                                    for (Telegram t : unlocked) {
                                        addTelegramToMap(t);
//                                        unlockedTelegrams.put(t.getTid(), t);
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                     e.printStackTrace();
                }
            }
        });
    }

    /**
     * Adds a Telegram to the map and colours it according to unlockable or locked.
     * @param telegram
     */
    private void addTelegramToMap(Telegram telegram) {
        float colour = telegram.getSeen() ? BitmapDescriptorFactory.HUE_YELLOW : telegram.isLocked() ? BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_GREEN;
//        Marker marker = mMap.addMarker(new MarkerOptions()
//                        .position(new LatLng(telegram.getLat(), telegram.getLng()))
//                        .icon(BitmapDescriptorFactory.defaultMarker(colour)));

//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow))
        for (ClusterTelegram t : telegramCluster) {
            if (t.getTid().equals(telegram.getTid())) {
                return;
            }
        }
        ClusterTelegram t = new ClusterTelegram(telegram);
        telegramCluster.add(t);
        telegramClusterManager.addItem(t);

//        return "";//marker.getId();
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
        } else if (requestCode == CHECK_LOCATION_ON_CREATE) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // TODO: 6/23/2016
            } else {
                checkLocationSettings(requestCode);
            }
        }
    }

    // ================================ Location stuff ============================ //
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }
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
    }

    protected void createLocationListener() {
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mCurrentLocation = location;
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                if (!movedCameraToFirstUpdate) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude())));
                    movedCameraToFirstUpdate = true;
                }
                pollForNewTelegrams();
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
                        } else if (activityResultCode == CHECK_LOCATION_ON_CREATE) {
                            // TODO: 6/23/2016  
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
    }

    protected void getLastLocation() {
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        }
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
