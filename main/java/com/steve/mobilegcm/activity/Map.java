package com.steve.mobilegcm.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.steve.mobilegcm.app.ChattApp;
import com.steve.mobilegcm.model.User;
import com.steve.mobilegcm.utils.Config;
import com.steve.mobilegcm.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Map extends Activity implements GoogleMap.OnMarkerClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;
    protected Boolean mRequestingLocationUpdates;
    protected String mLastUpdateTime;
    private String userName;
    private GoogleMap googleMap;
    private String longitude;
    private String latitude;
    private ArrayList<User> users;
    private float radius;
    private Menu optionsMenu;
    private MarkerOptions myMarker;
    private CircleOptions circle;
    private static int refreshCount;
    private static boolean firstTimeInitialized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        userName = ParseUser.getCurrentUser().getUsername();
        radius = ChattApp.getSearchDistance();
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("user", ParseUser.getCurrentUser());
        installation.saveInBackground();
        users = new ArrayList<>();
        refreshCount = 0;
        firstTimeInitialized = true;
        try {
            initializeMap();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";
        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();
    }

    private void initializeMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(), "Ανεπιτυχής δημιουργία του χάρτη!", Toast.LENGTH_SHORT).show();
            }
        }
        googleMap.setOnMarkerClickListener(this);
        myMarker = new MarkerOptions().title("Εσύ");
        circle = new CircleOptions().radius(radius).fillColor(Color.argb(80, 173, 192, 232)).strokeWidth(0);
        if ( firstTimeInitialized ) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0.0, 0.0), 10.0f));
            firstTimeInitialized = false;
        }
    }

    public void sendLocation()
    {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("UserLocation");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> scoreList, ParseException e) {
                if (e == null) {
                    if (scoreList.size() == 0) {
                        ParseObject userLocation = new ParseObject("UserLocation");
                        userLocation.put("user", ParseUser.getCurrentUser());
                        userLocation.put("username", ParseUser.getCurrentUser().getUsername());
                        userLocation.put("location", new ParseGeoPoint(Double.parseDouble(latitude), Double.parseDouble(longitude)));
                        final ProgressDialog dialog = new ProgressDialog(Map.this);
                        dialog.setMessage("Αποθήκευση θέσης χρήστη...");
                        dialog.show();
                        userLocation.saveInBackground(new SaveCallback() {
                            public void done(ParseException e) {
                                if (e == null) {
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(Map.this, "Αποτυχία: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                    else {
                        ParseObject object = scoreList.get(0);
                        object.put("location", new ParseGeoPoint(Double.parseDouble(latitude), Double.parseDouble(longitude)));
                        final ProgressDialog dialog = new ProgressDialog(Map.this);
                        dialog.setMessage("Αποθήκευση θέσης χρήστη...");
                        dialog.show();
                        object.saveInBackground(new SaveCallback() {
                            public void done(ParseException e) {
                                if (e == null) {
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(Map.this, "Αποτυχία: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                } else {
                    Toast.makeText(Map.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from
            // the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly
            // enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
            }
            // Update the value of mCurrentLocation from
            // the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the
                // Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime=savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateValues();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void startUpdates() {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            startLocationUpdates();
        }
    }

    public void stopUpdates() {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            stopLocationUpdates();
        }
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void updateValues() {
        if (mCurrentLocation != null) {
            latitude = String.valueOf(mCurrentLocation.getLatitude());
            longitude = String.valueOf(mCurrentLocation.getLongitude());
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeMap();
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
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
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            updateValues();
            startUpdates();
        }
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        if (refreshCount % 5 == 0) {
            refresh();
        }
        refreshCount++;
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                refresh();
                return true;
            case R.id.play:
                item.setVisible(false);
                optionsMenu.findItem(R.id.pause).setVisible(true);
                startUpdates();
                refresh();
                return true;
            case R.id.pause:
                item.setVisible(false);
                optionsMenu.findItem(R.id.play).setVisible(true);
                stopUpdates();
                return true;
            case R.id.eggegramenoi:
                Intent intent = new Intent(getApplicationContext(), UserList.class);
                intent.putExtra("username", userName);
                startActivity(intent);
                return true;
            case R.id.logout:
                logout();
                return true;
            case R.id.radius:
                intent = new Intent(getApplicationContext(), Radius.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        ParseUser.logOut();
        // Start and intent for the dispatch activity
        Intent intent = new Intent(Map.this, Dispatcher.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void refresh() {
        setRefreshActionButtonState(true);
        updateValues();
        sendLocation();
        updateMyMarker();
        updateRadiusCircle();
        updateUsers();
        setRefreshActionButtonState(false);
    }

    private void updateUsers() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("UserLocation");
        query.whereNotEqualTo("user", ParseUser.getCurrentUser());
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> scoreList, ParseException e) {
                if (e == null) {
                    clearUsers();
                    for (int i = 0; i < scoreList.size(); i++) {
                        ParseGeoPoint location = (ParseGeoPoint) scoreList.get(i).get("location");
                        String name = scoreList.get(i).getString("username");
                        MarkerOptions marker = new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)).title(name);
                        User user = new User(name);
                        user.setMarker(marker);
                        addUser(user);
                    }
                    showUsers();
                } else {
                    Toast.makeText(Map.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateRadiusCircle() {
        circle.center(new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)));
        googleMap.addCircle(circle);
    }

    private void updateMyMarker() {
        googleMap.clear();
        myMarker.position(new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)));
        createZoomAnimation();
        googleMap.addMarker(myMarker);
    }

    private void createZoomAnimation() {
        CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(Double.parseDouble(latitude),
                Double.parseDouble(longitude))).zoom(17).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public void setRefreshActionButtonState(final boolean refreshing) {
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu.findItem(R.id.refresh);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
                }
                else {
                    refreshItem.setActionView(null);
                }
            }
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        if (!marker.getTitle().equals("Εσύ")) {
            LayoutInflater layoutInflater = (LayoutInflater)getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            final View popupView = layoutInflater.inflate(R.layout.popup, null);
            final PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            Button btnDismiss = (Button) popupView.findViewById(R.id.dismiss);
            Button btnChat = (Button) popupView.findViewById(R.id.chat);
            String message = this.getApplicationContext().getString(R.string.protropi_sinomilias, marker.getTitle());
            TextView textView1 = (TextView) popupView.findViewById(R.id.textView1);
            textView1.setText(message);
            btnDismiss.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                    {
                        Drawable d = v.getBackground();
                        d.mutate();
                        d.setAlpha(80);
                        v.setBackgroundDrawable(d);
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                    {
                        Drawable d = v.getBackground();
                        d.setAlpha(255);
                        v.setBackgroundDrawable(d);
                        popupWindow.dismiss();
                    }
                    return false;
                }
            });
            btnChat.setOnTouchListener(new View.OnTouchListener() {
                int motionEventFinished;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                    {
                        motionEventFinished = 0;
                        Drawable d = v.getBackground();
                        d.mutate();
                        d.setAlpha(80);
                        v.setBackgroundDrawable(d);
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                    {
                        Drawable d = v.getBackground();
                        d.setAlpha(255);
                        v.setBackgroundDrawable(d);
                        motionEventFinished++;

                    }
                    if (motionEventFinished == 1) {
                        popupWindow.dismiss();
                        Intent intent = new Intent(Map.this, Chat.class);
                        intent.putExtra("username", marker.getTitle());
                        startActivity(intent);
                    }
                    return false;
                }
            });
            popupWindow.showAtLocation(findViewById(R.id.map), Gravity.CENTER, ((int) marker.getPosition().latitude), ((int) marker.getPosition().longitude));
            return true;
        }
        return false;
    }

    public void clearUsers() {
        users.clear();
    }

    public void addUser(User user) {
        users.add(user);
    }

    public void showUsers() {
        float[] results = new float[1];
        for (int i = 0; i < users.size(); i++) {
            MarkerOptions marker = users.get(i).getMarker();
            Location.distanceBetween(Double.parseDouble(latitude), Double.parseDouble(longitude), marker.getPosition().latitude, marker.getPosition().longitude, results);
            if (results[0] <= radius) {
                googleMap.addMarker(marker);
            }
        }
    }
}
