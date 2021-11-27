package scm.kaifwong8_cswong274.hideandseek;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {
    private static final String TAG = "MapsActivity";
    private static final int DEFAULT_UPDATE_INTERVAL = 5000;
    private static final int FAST_UPDATE_INTERVAL = 1000;
    private static final int FINE_LOCATION_REQUEST_CODE = 1;
    private static final double METER_IN_LATLNG_DEG = 0.00000661131;
    private static final int DIST_CLOSE = 1;
    private static final int DIST_MEDIUM = 3;
    private static final int DIST_FAR = 6;
    // ==================================== =================== ====================================

    private TextView DistText;

    private GoogleMap mMap;
    private ArFragment arFragment;
    private ModelRenderable modelRenderable;
    private LocationRequest locationRequest;
    private LocationCallback locationCallBack;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currLocation;
    private int currentHintNumber;
    private float currentDistToHint;
    private TopFragment topFragment;

    private SensorManager sensorManager;
    private Sensor sensor_a;
    private Sensor sensor_m;

    private float heading = 0;
    private float hintDetectionRadius = 700;
    private boolean inHintArea = false;
    private boolean bossAreaFound = false;
    private Circle hintCircle;
    private Marker playerMarker, hintMarker;

    private boolean isShootAvailable;
    private boolean isShieldAvailable;
    private boolean isCameraEnabled = true;

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        private float[] values_a = new float[3]; // data from accelerometer sensor
        private float[] values_m = new float[3]; // data from magnetic field sensor
        private boolean aReady = false;
        private boolean mReady = false;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                values_a = event.values.clone();
                aReady = true;
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                values_m = event.values.clone();
                mReady = true;
            }
            if (aReady && mReady) {
                calculateOrientation();
                aReady = mReady = false;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        private void calculateOrientation() {
            float[] values = new float[3];
            float[] R = new float[9];
            SensorManager.getRotationMatrix(R, null, values_a, values_m);
            SensorManager.getOrientation(R, values);

            values[0] = (float) Math.toDegrees(values[0]);
            values[1] = (float) Math.toDegrees(values[1]);
            values[2] = (float) Math.toDegrees(values[2]);

            heading = values[0];
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        topFragment = (TopFragment) getSupportFragmentManager().findFragmentById(R.id.top_fragment);
        DistText = (TextView)topFragment.getView().findViewById(R.id.distTxt);
        // ======================================= location ========================================
        initLocationRequest();
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currLocation = locationResult.getLastLocation();
                updateMapUI(currLocation);

                if (locationResult.getLastLocation().getSpeed()>5) updateMapCamera(currLocation);

                if (hintMarker == null) {
                    GenerateHint(currLocation);
                }
                else {
                    LatLng tempLatLng = hintMarker.getPosition();
                    Location tempLocation = new Location("");
                    tempLocation.setLatitude(tempLatLng.latitude);
                    tempLocation.setLongitude(tempLatLng.longitude);
                    currentDistToHint = currLocation.distanceTo(tempLocation);

                    DistText.setText(currentDistToHint/1000+" km");


                    //DistText.setText(currentHintNumber);
                }

            }
        };

        // ========================================== AR ===========================================
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);

        ModelRenderable.builder()
                .setSource(this, R.raw.dog_standing)
                .setIsFilamentGltf(true)
                .build() // returns a completableFuture
                .thenAccept(renderable -> modelRenderable = renderable)
                .exceptionally(throwable -> {
                    Log.d(TAG, "Model Renderable: unable to load Renderable", throwable);
                    return null;
                });

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            // if (modelRenderable == null) return;

            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
            modelNode.setParent(anchorNode);
            modelNode.setRenderable(modelRenderable);

            arFragment.getArSceneView().getScene().addChild(anchorNode);

            modelNode.select();

            // modelNode.setOnTapListener((hitTestResult, motionEvent1) -> {});
        });

        // ======================================== Sensor =========================================
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor_a = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensor_m = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (sensor_a == null) Log.e(TAG, "onCreateView: accelerometer not detected");
        if (sensor_m == null) Log.e(TAG, "onCreateView: magnetic field sensor not detected");

        // ========================================== UI ===========================================
        ConstraintLayout ui_background = findViewById(R.id.ui_background);
        FloatingActionButton btn_shoot = findViewById(R.id.btn_shoot);
        FloatingActionButton btn_shield = findViewById(R.id.btn_shield);
        FloatingActionButton btn_camera = (FloatingActionButton) findViewById(R.id.btn_camera);
        FloatingActionButton btn_map_focus = findViewById(R.id.btn_map_focus);

        btn_camera.setOnClickListener(v -> {
            ConstraintLayout mConstrainLayout  = (ConstraintLayout) findViewById(R.id.top_fragment);
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) mConstrainLayout.getLayoutParams();

            if (!isCameraEnabled){
                lp.matchConstraintPercentHeight = (float) 1;
            } else {
                lp.matchConstraintPercentHeight = (float) 0;
            }

            mConstrainLayout.setLayoutParams(lp);
            isCameraEnabled = !isCameraEnabled;
        });

        btn_map_focus.setOnClickListener(v -> updateMapCamera(currLocation));

        // must be the last func in onCreate
        updateLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0) {
            switch (requestCode) {
                case FINE_LOCATION_REQUEST_CODE:
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        updateLocation();
                    } else {
                        Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_REQUEST_CODE);
                    }
            }
        }
    }

    private void initLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(DEFAULT_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FAST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void updateLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                updateMapUI(location);
                updateMapCamera(location);
                mMap.clear();
                playerMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            });
        } else { ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_REQUEST_CODE); }
    }

    private void updateMapUI(Location location) {
        if (location != null && playerMarker!= null) {
            //.title("")
            //.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_user))
            playerMarker.showInfoWindow();
            playerMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }

    private void updateMapCamera(Location location) {
        if (location != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                    .bearing(heading)   // degrees clockwise from north
                    .zoom(15).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);

        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle));
            if (!success) Log.e(TAG, "Style parsing failed.");
        } catch (Exception e) { Log.e(TAG, "Can't find style. Error: ", e); }

        updateLocation();
        mMap.clear();
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        Log.i("MapClick","Clicked Map");
        hintMarker.remove();
        hintCircle.remove();
        currentHintNumber++;
        GenerateHint(currLocation);
    }

    public void GenerateHint(Location location) {
        int negFactor = Math.random()>0.5? -1:1;
        float rngLat = (float) ((DIST_CLOSE + Math.random()) * (hintDetectionRadius * METER_IN_LATLNG_DEG * (1 + Math.random())) * negFactor);
        negFactor = Math.random()>0.5? -1:1;
        float rngLong = (float) ((DIST_CLOSE + Math.random()) * (hintDetectionRadius * METER_IN_LATLNG_DEG * (1 + Math.random())) * negFactor);

        LatLng rngPos = new LatLng(location.getLatitude() + rngLat, location.getLongitude() +rngLong);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(rngPos);
        hintMarker = mMap.addMarker(markerOptions);

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(new LatLng(currLocation.getLatitude() + rngLat,currLocation.getLongitude() +rngLong));
        circleOptions.radius(hintDetectionRadius);
        circleOptions.fillColor(Color.argb(64,240,240,240));
        circleOptions.strokeColor(Color.argb(255,240,240,240));
        circleOptions.strokeWidth(6);
        hintCircle = mMap.addCircle(circleOptions);

        LatLng markerLatLng = hintMarker.getPosition();
        Location markerLocation = new Location("");
        markerLocation.setLatitude(markerLatLng.latitude);
        markerLocation.setLongitude(markerLatLng.longitude);

        currentDistToHint = currLocation.distanceTo(markerLocation);
        DistText.setText(currentDistToHint/1000+" km");

        Log.i(TAG, "Distance: " + String.valueOf(currLocation.distanceTo(markerLocation)));
    }

    // ==================================== activity life cycle ====================================
    @Override
    protected void onResume() {
        super.onResume();

        if (sensor_a != null) sensorManager.registerListener(sensorEventListener, sensor_a, SensorManager.SENSOR_DELAY_UI);
        if (sensor_m != null) sensorManager.registerListener(sensorEventListener, sensor_m, SensorManager.SENSOR_DELAY_UI);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            updateLocation();
            Log.d(TAG, "onDestroy: locationCallBack requested");
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        }
    }

    @Override
    protected void onPause() {
        if (sensor_a != null) sensorManager.unregisterListener(sensorEventListener, sensor_a);
        if (sensor_m != null) sensorManager.unregisterListener(sensorEventListener, sensor_m);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onDestroy: locationCallBack removed");
            fusedLocationClient.removeLocationUpdates(locationCallBack);
        }

        super.onDestroy();
    }
}


