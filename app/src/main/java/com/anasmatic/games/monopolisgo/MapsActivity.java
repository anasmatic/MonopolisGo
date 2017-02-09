/**
 * TODO: test places API -high prior 2 use webservice urls ( api key : AIzaSyDu1kBkNL-TKHsZiJdFwaaIEvJv_vbbCyA )
 *          https://developers.google.com/places/web-service/search
 *          http://www.androidhive.info/2012/08/android-working-with-google-places-and-maps-tutorial/
 *          https://github.com/railskarthi/GooglePlaces-Android/blob/master/src/com/titutorial/mapdemo/PlacesService.java
 * TODO: detect user movement and update map view and Places according to how much use moves to make less requests to Places API
 * TODO: check on app sleep and on awake , if the location cahnged -low prior
 * TODO: check for closing gps -low prior
 * TODO: (DONE) apply onLocationChange -high prior 1
 */

package com.anasmatic.games.monopolisgo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
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
import com.google.android.gms.location.places.PlaceFilter;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.util.Date;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MapsActivity extends FragmentActivity
        implements
        OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {


    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequestBalancedPowerAccuracy;
    private Location mCurrentLocation;
    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean isPermissionDenied = false;
    private boolean isRequestingLocationUpdates = false;

    private static final int PLACE_PICKER_REQUEST = 574;
    private static final int REQUEST_CHECK_SETTINGS = 682;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting_location_updates_key";
    private static final String LOCATION_KEY = "location_key";
    private static final String LAST_UPDATED_TIME_STRING_KEY = "last_updated_time_string_key";

    private String mLastUpdateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        updateValuesFromBundle(savedInstanceState);

        createLocationRequest();

        buildGoogleApiClient();
    }

    private void createLocationRequest() {
        mLocationRequestBalancedPowerAccuracy = new LocationRequest();
        mLocationRequestBalancedPowerAccuracy.setInterval(30000);//30second
        mLocationRequestBalancedPowerAccuracy.setFastestInterval(5000);//5seconds
        mLocationRequestBalancedPowerAccuracy.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    private synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient
                    .Builder(this)
                    .enableAutoManage(this /* FragmentActivity */,
                            this /* OnConnectionFailedListener */)//I don't need this now !
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .enableAutoManage(this,this)
                    .build();
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
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.setMinZoomPreference(15.0f);
        mMap.setMaxZoomPreference(25.0f);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        enableMyLocation();


    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        } else {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("ENABLE_MY_LOCATION", "result=" + requestCode + ", " + permissions[0] + ", " + grantResults[0]);
        if (requestCode == 1) {
            Log.d("ENABLE_MY_LOCATION", "requestCode pass");
            if (grantResults[0] == PERMISSION_GRANTED) {
                Log.d("ENABLE_MY_LOCATION", "PERMISSION_GRANTED =" + PERMISSION_GRANTED);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Log.d("ENABLE_MY_LOCATION", "Toast this motherfucker");
                Toast.makeText(this, "This game needs location permission to run.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Log.d("@MyLocationButtonClick", "eh!");
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    //ConnectionCallbacks--------------------------------
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getMyLocation();
    }

    private void getMyLocation() {

        if (canGetMyLastLocation()) {
            startLocationUpdates();
            Log.d("@        Lat", String.valueOf(mCurrentLocation.getLatitude()));
            Log.d("@        Lng", String.valueOf(mCurrentLocation.getLongitude()));
        } else {
            checkLocationSettings();
        }
    }

    private boolean canGetMyLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            Log.d("@Current ", "checkSelfPermission passed");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        } else {
            //mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        Log.d("@Current ", String.valueOf(mCurrentLocation));
        if (mCurrentLocation != null)
            return true;
        else
            return false;
    }


    private void checkLocationSettings() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequestBalancedPowerAccuracy);
        //ask for result using PendingResult
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        //listen for result
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates locationSettingsStates = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        Log.d("@LocationSettings", "SUCCESS , requsest update here");
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        Log.d("@LocationSettings", "RESOLUTION_REQUIRED");
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    getActivity(),//this
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.d("@LocationSettings", "A7eeh ! SendIntentException ");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        Log.d("@LocationSettings", "SETTINGS_CHANGE_UNAVAILABLE");
                        break;
                }
            }
        });
    }



    @Override
    public void onConnectionSuspended(int i) {

    }
//--------------------------------ConnectionCallbacks|

//OnConnectionFailedListener--------------------------------
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("@FAIL", "onConnectionFailed(): "+connectionResult.getErrorMessage());
    }
// --------------------------------OnConnectionFailedListener|

//LocationListener--------------------------------
    @Override
    public void onLocationChanged(Location location) {
        Log.d("@LocationChanged","yes location changed: "+location);
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        LatLng locationLatLng = new LatLng(location.getLatitude(),location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 10));

        //update places
        //TODO: check how much locations changed before updating Places
        nearbyPlacesRequest();
    }

    private void nearbyPlacesRequest() {

    }

    //--------------------------------LocationListener|
@Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !isRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    protected void startLocationUpdates() {
        //mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequestBalancedPowerAccuracy, this);
        }
    }
    protected void stopLocationUpdates() {
        if(mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,isRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                isRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                //setButtonsEnabledState();//TODO:show or hide according to state
            }

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
            //updateUI();
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        //try get location again
                        Toast.makeText(this, "Thank you, loading you location now !", Toast.LENGTH_LONG).show();
                        getMyLocationWithDelay();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        Toast.makeText(this, "For accuracy please activate location from settings", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        break;
                }
                break;
        }
    }
    private int retries = 5;
    private void getMyLocationWithDelay() {

        if (canGetMyLastLocation()) {
            getMyLocation();
        }else{
            if(retries > 0) {
                retries--;
                //delay
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("@postDelayed","trying..."+retries);
                        getMyLocationWithDelay();
                    }
                }, 2000);
            }
            else{
                Toast.makeText(this, "Activating location failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private Activity getActivity(){
        return this;
    }
}
