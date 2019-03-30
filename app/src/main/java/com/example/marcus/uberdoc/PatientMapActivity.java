package com.example.marcus.uberdoc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class PatientMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleAPIClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button mLogout, mRequest;
    private Boolean requestSwitch = false;
    private Marker pickUpMarker;
    private LatLng pickUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLogout = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.request);

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(PatientMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(requestSwitch)
                {
                    cancelDoctor();
                }
                else
                {
                    requestSwitch = true;

                    String userID = FirebaseAuth.getInstance().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("patientRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userID, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    pickUp = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(pickUp).title("Patient Location"));

                    mRequest.setText("Calling doctor");

                    getClosestDoctor();
                }
            }
        });
    }

    private void cancelDoctor()
    {
        requestSwitch = false;
        geoQuery.removeAllListeners();
        doctorLocationRef.removeEventListener(doctorLocationRefListener);

        if(doctorFoundID != null)
        {
            DatabaseReference doctorRef = FirebaseDatabase.getInstance().getReference().child("users").child("doctor").child(doctorFoundID).child("nextPatient");
            doctorRef.removeValue();
            doctorFoundID = null;
        }
        doctorFound = false;
        radius = 1;

        String userID = FirebaseAuth.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("patientRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userID);

        if(pickUpMarker != null)
        {
            pickUpMarker.remove();
        }
        mRequest.setText("Call Doctor");
    }
    private int radius = 1;
    private Boolean doctorFound = false;
    private String doctorFoundID;
    GeoQuery geoQuery;

    private void getClosestDoctor(){
        DatabaseReference doctorLocation = FirebaseDatabase.getInstance().getReference().child("doctorAvailable");

        GeoFire geoFire = new GeoFire(doctorLocation);

        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickUp.latitude, pickUp.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!doctorFound && requestSwitch) {
                    doctorFound = true;
                    doctorFoundID = key;

                    DatabaseReference doctorRef = FirebaseDatabase.getInstance().getReference().child("users").child("doctor").child(doctorFoundID);
                    String patientID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    HashMap map = new HashMap();
                    map.put("nextPatient", patientID);
                    doctorRef.updateChildren(map);

                    getDoctorLocation();
                    mRequest.setText("Looking for doctor...");
                }
            }

            @Override
            public void onKeyExited(String key) { }
            @Override
            public void onKeyMoved(String key, GeoLocation location) { }

            @Override
            public void onGeoQueryReady() {
                if(!doctorFound){
                    radius++;
                    getClosestDoctor();
                }
            }
            @Override
            public void onGeoQueryError(DatabaseError error) { }

        });
    }


    Marker mDoctorMarker;
    private DatabaseReference doctorLocationRef;
    private ValueEventListener doctorLocationRefListener;
    private void getDoctorLocation(){
            doctorLocationRef = FirebaseDatabase.getInstance().getReference().child("doctorsOnCall").child(doctorFoundID).child("l");
            doctorLocationRefListener = doctorLocationRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists() && requestSwitch){
                        List<Object> map = (List<Object>) dataSnapshot.getValue();
                        double locationLat = 0;
                        double locationLong = 0;

                        mRequest.setText("Doctor Found");

                        if(map.get(0) != null){
                            locationLat =  Double.parseDouble(map.get(0).toString());
                        }
                        if(map.get(1) != null){
                            locationLong = Double.parseDouble(map.get(1).toString());
                        }
                        LatLng doctorLatLng = new LatLng(locationLat, locationLong);
                        if(mDoctorMarker != null){
                            mDoctorMarker.remove();
                        }
                        Location location1 = new Location("");
                        location1.setLatitude(pickUp.latitude);
                        location1.setLongitude(pickUp.longitude);

                        Location location2 = new Location("");
                        location2.setLatitude(doctorLatLng.latitude);
                        location2.setLongitude(doctorLatLng.longitude);

                        float distance = location1.distanceTo(location2);

                        if (distance < 50)
                        {
                            mRequest.setText("Doctor has arrived");
                        }
                        else
                        {
                            mRequest.setText("Doctor found " + String.valueOf(distance));
                        }

                        mDoctorMarker = mMap.addMarker(new MarkerOptions().position(doctorLatLng).title("Your Doctor"));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleAPIClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleAPIClient(){
        mGoogleAPIClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleAPIClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleAPIClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}