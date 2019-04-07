package com.example.marcus.uberdoc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class DoctorMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, RoutingListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleAPIClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;


    private Button dLogout, dSettings, dMoreInfo;

    private String patientID = "";
    private LinearLayout patientInformation;
    private TextView pPatientName, pPatientNumber;
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        polylines = new ArrayList<>();

        dLogout = (Button) findViewById(R.id.logout);
        dSettings = (Button) findViewById(R.id.settings);

        patientInformation = (LinearLayout) findViewById(R.id.patientInfo);
        pPatientName = (TextView) findViewById(R.id.patientName);
        pPatientNumber = (TextView) findViewById(R.id.patientNumber);
        dMoreInfo = (Button) findViewById(R.id.moreInfo);


        dLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();

                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DoctorMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        dMoreInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DoctorMapActivity.this, PatientInformation.class);
                //intent.putExtra("patientID", patientID);
                startActivity(intent);
                return;
            }
        });

        dSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DoctorMapActivity.this, DoctorSettings.class);
                startActivity(intent);
                return;
            }
        });
        getAssignedPatient();
    }


    private void getAssignedPatient(){
        String doctorID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedPatientRef  = FirebaseDatabase.getInstance().getReference().child("users").child("doctor").child(doctorID).child("nextPatient");

        assignedPatientRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                        patientID = dataSnapshot.getValue().toString();
                        getAssignedPatientLocation();
                        getAssignedPatientInfo();

                }
                else
                {
                    clearMap();
                    patientID = "";
                    if(pickUpMarker != null)
                    {
                        pickUpMarker.remove();
                    }
                    patientInformation.setVisibility(View.GONE);
                    pPatientName.setText("");
                    pPatientNumber.setText("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    /*==============================================================================================

    Function to get the assigned patients information
    We use the patientID to read the customers name and number from the database
    We then set the layout to be visible and populate the name and number fields

    ================================================================================================ */


    private void getAssignedPatientInfo()
    {
        patientInformation.setVisibility(View.VISIBLE);
        DatabaseReference pPatientDatabase = FirebaseDatabase.getInstance().getReference().child("users").child("patient").child(patientID);
        pPatientDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0)
                {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name") != null)
                    {
                        pPatientName.setText(map.get("name").toString());
                    }
                    if(map.get("number") != null)
                    {
                        pPatientNumber.setText(map.get("number").toString());

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /*==============================================================================================

    Function to get the assigned patients location
    We get the patients latitude and longitude and add them to a List, then creating a Marker

    ================================================================================================ */
    Marker pickUpMarker;
    DatabaseReference assignedPatientLocationRef;
    private ValueEventListener assignedPatientLocationRefListener;
    private void getAssignedPatientLocation(){
        assignedPatientLocationRef = FirebaseDatabase.getInstance().getReference().child("patientRequest").child(patientID).child("l");
        assignedPatientLocationRefListener = assignedPatientLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && !patientID.equals("")){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLong = 0;

                    if(map.get(0) != null){
                        locationLat =  Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){
                        locationLong = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng doctorLatLng = new LatLng(locationLat, locationLong);
                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(doctorLatLng).title("Patient Location"));
                    //Get the Route on map
                    getRoute(doctorLatLng);
                }
            }

            private void getRoute(LatLng doctorLatLng) {
                Routing routing = new Routing.Builder()
                        .key("AIzaSyCJ17YTHo4lX5GtTEzCr8sN43A4wJGGEws")
                        .travelMode(AbstractRouting.TravelMode.DRIVING)
                        .withListener(DoctorMapActivity.this)
                        .alternativeRoutes(false)
                        .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), doctorLatLng)
                        .build();
                routing.execute();
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
        if(getApplicationContext() != null) {

            mLastLocation = location;
            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("doctorAvailable");
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("doctorsOnCall");

            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            GeoFire geoFireWorking = new GeoFire(refWorking);

            switch (patientID) {

                case "":
                    geoFireWorking.removeLocation(userID);
                    geoFireAvailable.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;

                default:
                    geoFireAvailable.removeLocation(userID);
                    geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;

            }
        }
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

    protected void logout(){
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("doctorAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userID);
    }
    @Override
    protected void onStop() {
        super.onStop();

        logout();
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void clearMap()
    {
        for(Polyline line : polylines)
        {
            line.remove();
        }
        polylines.clear();
    }
}
