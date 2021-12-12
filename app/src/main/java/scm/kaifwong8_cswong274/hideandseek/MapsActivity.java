package scm.kaifwong8_cswong274.hideandseek;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.LinearInterpolator;
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
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Vector3Evaluator;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Collection;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "MapsActivity";
    private static final int DEFAULT_UPDATE_INTERVAL = 5000, FAST_UPDATE_INTERVAL = 1000;
    private static final int FINE_LOCATION_REQUEST_CODE = 1;
    private static final int DIST_CLOSE = 1, DIST_MEDIUM = 3, DIST_FAR = 6;
    private final int HINT_NUMBER_NEEDED = 3;
    private final float HINT_DETECTION_RADIUS = 700;
    private static final double METER_IN_LATLNG_DEG = 0.00000661131;
    // services instance
    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallBack;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currLocation;
    private SensorManager sensorManager;
    private Sensor sensor_a, sensor_m;
    // view
    private TopFragment topFragment;
    private TextView tv_distToHint, tv_hintFound, tv_walkTime, tv_walkDistance;
    private AimView aimView;
    private DistToHintGraph distToHintGraph;
    private CircleOptions circleOptions;
    private ConstraintLayout topFragmentContainer, uiContainer;
    private ConstraintLayout.LayoutParams topFragmentParams;
    // time & distance
    private int timeSecond = 0;
    private int diffDist = 1;
    private float distance = 0, fullDistance = 0, totalSpd = 0;
    private String timeString = "00:00:00";

    private int score = 0, bossHP = 999;

    private float heading = 0;
    private int currentHintNumber;
    private float currentDistToHint;
    private boolean isCameraEnabled = false;
    private boolean bossAreaFound, isInsideDetArea = false;
    private Circle hintCircle;
    private Marker playerMarker, hintMarker;
    private CameraPosition cameraPosition;

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        private float[] values_a = new float[3], values_m = new float[3];
        private boolean aReady = false, mReady = false;

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
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        //
        private void calculateOrientation() {
            float[] values = new float[3];
            float[] R = new float[9];
            SensorManager.getRotationMatrix(R, null, values_a, values_m);
            SensorManager.getOrientation(R, values);
            values[0] = (float) Math.toDegrees(values[0]);
            heading = values[0];
        }
    };

    /** AR VAR & FUNCTIONS */
    private ArFragment arFragment;
    private Scene scene;
    private final int HINT_INDEX = 0, BOSS_INDEX = 1, BULLET_INDEX = 2, SHIELD_INDEX = 3;
    private final int[] models = {R.raw.ufo, R.raw.boss, R.raw.origami_fish, R.raw.dog_standing};
    private final String[] modelNames = {"Hint", "Boss", "Bullet", "Shield"};
    private ModelRenderable[] renderables = new ModelRenderable[models.length];
    private FloatingActionButton btn_camera;
    private Anchor bossAnchor;
    private TransformableNode hintNode, bossNode;
    private boolean isHintGenerated, isBossGenerated = false;

    private void loadModels() {
        for (int i=0; i<models.length; i++) {
            int finalIndex = i;
            ModelRenderable.builder()
                    .setSource(this, models[finalIndex])
                    .setIsFilamentGltf(true)
                    .build()
                    .thenAccept(renderable -> renderables[finalIndex] = renderable)
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Unable to load Renderable", throwable);
                                return null;
                            });
        }
    }

    private void removeNode(Node node, boolean deleteParent) {
        if (deleteParent) {
            AnchorNode parent = (AnchorNode)node.getParent();
            parent.getAnchor().detach();
            parent.removeChild(node);
        }

        scene.removeChild(node);
    }
    /** END */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //game diff
        SharedPreferences sharedPreferences = getSharedPreferences("settingPreferences", MODE_PRIVATE);
        switch (sharedPreferences.getInt("GAME_DIFFICULTY", MainActivity.DIFFICULTY_EASY)) {
            case MainActivity.DIFFICULTY_EASY:
                diffDist = DIST_CLOSE;
                break;
            case MainActivity.DIFFICULTY_MEDIUM:
                diffDist = DIST_MEDIUM;
                break;
            case MainActivity.DIFFICULTY_HARD:
                diffDist = DIST_FAR;
                break;
        }
        // init view
        FloatingActionButton btn_shoot = findViewById(R.id.btn_shoot);
        FloatingActionButton btn_shield = findViewById(R.id.btn_shield);
        btn_camera = (FloatingActionButton) findViewById(R.id.btn_camera);
        FloatingActionButton btn_map_focus = findViewById(R.id.btn_map_focus);
        topFragment = (TopFragment) getSupportFragmentManager().findFragmentById(R.id.top_fragment);
        tv_distToHint = topFragment.getView().findViewById(R.id.tv_distToHint);
        tv_hintFound = topFragment.getView().findViewById(R.id.tv_hintFound);
        tv_walkDistance = topFragment.getView().findViewById(R.id.tv_walk_distance);
        tv_walkTime = topFragment.getView().findViewById(R.id.tv_walk_time);
        uiContainer = findViewById(R.id.ui_container);
        topFragmentContainer = MapsActivity.this.findViewById(R.id.top_fragment);
        topFragmentParams = (ConstraintLayout.LayoutParams) topFragmentContainer.getLayoutParams();
        // customView & tv_init
        {
            ConstraintLayout aimViewContainer = findViewById(R.id.aim_view_container);
            this.aimView = new AimView(this);
            aimViewContainer.addView(aimView);
            ConstraintLayout distToHintGraphContainer = topFragment.getView().findViewById(R.id.distToHint_container);
            this.distToHintGraph = new DistToHintGraph(this);
            distToHintGraphContainer.addView(distToHintGraph);
            // tv_init
            tv_hintFound.setText("Hint Found: " + currentHintNumber + "/" + HINT_NUMBER_NEEDED);   // move to other place? generate hint?
        }

        //uiContainer.setTranslationY(5000);
        topFragmentParams.matchConstraintPercentHeight = !MapsActivity.this.isCameraEnabled? (float)1:(float)0;
        topFragmentContainer.setLayoutParams(topFragmentParams);

        // Location
        initLocationRequest();
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currLocation = locationResult.getLastLocation();
                updateMarker(playerMarker, currLocation);

                if (locationResult.getLastLocation().getSpeed() > 5) updateMapCamera(currLocation);

                if (hintMarker == null) {
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));   //this line represent update map to player <- === updateMapCamera(currLocation);?
                    GenerateHint(currLocation);
                } else {
                    LatLng tempLatLng = hintMarker.getPosition();
                    Location tempLocation = new Location("");
                    tempLocation.setLatitude(tempLatLng.latitude);
                    tempLocation.setLongitude(tempLatLng.longitude);


                    currentDistToHint = currLocation.distanceTo(tempLocation);
                    float temp = (float) (Math.round(currentDistToHint/1000*10)/10d);
                    if (fullDistance==0) fullDistance = (float) (Math.round(currLocation.distanceTo(tempLocation)*10)/10d);
                    tv_distToHint.setText("Distance to Hint: " + temp + " km");
                    isInsideDetArea = currentDistToHint > HINT_DETECTION_RADIUS? false:true;
                    Log.i("Dist",currentDistToHint+" , "+ HINT_DETECTION_RADIUS);
                }
            }
        };

        /**========================================== AR =========================================*/
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        scene = arFragment.getArSceneView().getScene();
        loadModels();

        //initBoss();

        btn_shoot.setOnClickListener(v -> {
            shoot();
        });
        // btn_shield.setOnClickListener(v -> defence());
        /**========================================== AR =========================================*/

        // Sensor
        {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            sensor_a = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensor_m = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (sensor_a == null) Log.e(TAG, "onCreateView: accelerometer not detected");
            if (sensor_m == null) Log.e(TAG, "onCreateView: magnetic field sensor not detected");
        }

        // UI
        btn_camera.setOnClickListener(v -> {
            // top fragment control rebuild?
            topFragmentParams.matchConstraintPercentHeight = !MapsActivity.this.isCameraEnabled? (float)1:(float)0;
            topFragmentContainer.setLayoutParams(topFragmentParams);
            isCameraEnabled = !isCameraEnabled;

            if (isCameraEnabled) {
                uiContainer.setTranslationY(0);
            } else { uiContainer.setTranslationY(5000); }

            aimView.toggleHide();

            if (!bossAreaFound){
                Log.i(TAG,"hint model init");
                initHint();
            } else {
                Log.i(TAG,"boss model init");
                initBoss();
            }
        });

        btn_map_focus.setOnClickListener(v -> mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition)) );

        // Timer - 1s
        Timer secondTimer = new Timer();
        secondTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                timeSecond++;
                timeString = getTime();

                if (currLocation!=null) {
                    totalSpd+=currLocation.getSpeed();
                    distance = (float) (Math.round((totalSpd/timeSecond/3600) * timeSecond*10)/10.d);
                    // distToHintGraph update
                    float temp = (fullDistance-currentDistToHint);
                    distToHintGraph.setDistMark(fullDistance - HINT_DETECTION_RADIUS, temp - HINT_DETECTION_RADIUS/2);
                }
            }
        }, 1000, 1000);

        Timer uiTimer = new Timer();
        TextView tv_cameraState = findViewById(R.id.tv_camera_status);
        TimerTask uiTimerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    MapsActivity.this.tv_walkTime.setText("Adventure time: " + timeString);
                    if (currLocation!=null) {

                        MapsActivity.this.tv_walkDistance.setText("Distance: " + distance + " km");

                        if (isInsideDetArea){
                            btn_camera.setEnabled(true);
                            tv_cameraState.setText("Camera Ready");
                            try {
                                arFragment.getArSceneView().resume();
                            } catch (CameraNotAvailableException e) {
                                e.printStackTrace();
                            }
                        } else {
                            btn_camera.setEnabled(false);
                            tv_cameraState.setText("Camera Not Ready");
                            arFragment.getArSceneView().pause();
                        }
                    }
                });
            }
        };
        uiTimer.scheduleAtFixedRate(uiTimerTask, 0, 1000);

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
                updateMapCamera(location);
                updateMarker(playerMarker, location);

                if (location != null && playerMarker == null) {

                    playerMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                }
            });
        } else { ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_REQUEST_CODE); }
    }
    // user marker
    private void updateMarker(Marker marker, Location location) {
        if (location != null && marker!= null) {
            marker.showInfoWindow();
            marker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }
    // to user pos
    private void updateMapCamera(Location location) {
        if (location != null) {
            cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                    .bearing(heading)
                    .zoom(14)
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // style & map basic setting
        {
            try {
                boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle));
                if (!success) Log.e(TAG, "Style parsing failed.");
            } catch (Exception e) {
                Log.e(TAG, "Can't find style. Error: ", e);
            }

            mMap.getUiSettings().setCompassEnabled(false);
            mMap.getUiSettings().setAllGesturesEnabled(true);
            mMap.setBuildingsEnabled(false);
            mMap.setTrafficEnabled(false);

            updateLocation();
            mMap.clear();
        }
    }

    public void GenerateHint(Location location) {
        int negFactor = Math.random()>0.5? -1:1;
        float rngLat = (float) ((diffDist + Math.random()) * (HINT_DETECTION_RADIUS * METER_IN_LATLNG_DEG * (1 + Math.random())) * negFactor);
        negFactor = Math.random()>0.5? -1:1;
        float rngLong = (float) ((diffDist + Math.random()) * (HINT_DETECTION_RADIUS * METER_IN_LATLNG_DEG * (1 + Math.random())) * negFactor);

        LatLng rngPos = new LatLng(location.getLatitude() + rngLat, location.getLongitude() +rngLong);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(rngPos);
        hintMarker = mMap.addMarker(markerOptions);
        hintMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_none));
        hintMarker.setTitle("!!!");

        circleOptions = new CircleOptions();
        circleOptions.center(new LatLng(currLocation.getLatitude() + rngLat,currLocation.getLongitude() +rngLong));
        circleOptions.radius(HINT_DETECTION_RADIUS);

        if (!bossAreaFound){
            circleOptions.fillColor(Color.argb(64,240,240,240));
            circleOptions.strokeColor(Color.argb(255,240,240,240));
            circleOptions.strokeWidth(6);
        } else {
            circleOptions.fillColor(Color.argb(64,240,20,20));
            circleOptions.strokeColor(Color.argb(255,240,240,240));
            circleOptions.strokeWidth(10);
        }

        hintCircle = mMap.addCircle(circleOptions);

        LatLng markerLatLng = hintMarker.getPosition();
        Location markerLocation = new Location("");
        markerLocation.setLatitude(markerLatLng.latitude);
        markerLocation.setLongitude(markerLatLng.longitude);

        currentDistToHint = currLocation.distanceTo(markerLocation);
        Log.i(TAG, "Detection Radius In Meter: " + HINT_DETECTION_RADIUS);
    }

    private String getTime() {
        String hr, min, s;
        s = timeSecond%60<10? "0"+(timeSecond%60):String.valueOf(timeSecond%60);
        min = timeSecond/60<10? "0"+(timeSecond/60):String.valueOf(timeSecond/60);
        hr = timeSecond/3600<10? "0"+(timeSecond/3600):String.valueOf(timeSecond/3600);
        return hr+" : "+min+" : "+s;
    }

    // Activity life cycle
    @Override
    protected void onResume() {
        super.onResume();

        if (sensor_a != null) sensorManager.registerListener(sensorEventListener, sensor_a, SensorManager.SENSOR_DELAY_UI);
        if (sensor_m != null) sensorManager.registerListener(sensorEventListener, sensor_m, SensorManager.SENSOR_DELAY_UI);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            updateLocation();

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    updateMapCamera(location);
                    updateMarker(playerMarker, location);
                });
            } else { ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_REQUEST_CODE); }
            Log.d(TAG, "onDestroy: locationCallBack requested");
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallBack, null);
            //fusedLocationClient.removeLocationUpdates(locationCallBack);
        }
    }
    @Override
    protected void onPause() {
        if (sensor_a != null) sensorManager.unregisterListener(sensorEventListener, sensor_a);
        if (sensor_m != null) sensorManager.unregisterListener(sensorEventListener, sensor_m);

        super.onPause();
    }

    private void initHint(){
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Frame frame = arFragment.getArSceneView().getArFrame();
            Camera camera = frame.getCamera();
            Collection<Plane> planes = frame.getUpdatedTrackables(Plane.class);

            for (Plane plane : planes) {
                if (camera.getTrackingState() != TrackingState.TRACKING) return;
                if (plane.getTrackingState() == TrackingState.TRACKING) {
                    Anchor anchor = plane.createAnchor(plane.getCenterPose());
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(scene);

                    if (!isHintGenerated) {
                        hintNode = new HintNode(arFragment.getTransformationSystem());
                        hintNode.setParent(anchorNode);
                        hintNode.setName(modelNames[HINT_INDEX]);
                        hintNode.setRenderable(renderables[HINT_INDEX]);
                        Random random = new Random();
                        Vector3 tempV = new Vector3();
                        int negFactor;
                        negFactor = random.nextInt(3) > 1? 1:-1;
                        tempV.x += (hintNode.getLocalPosition().x + random.nextInt(4) +1) * negFactor;
                        negFactor = random.nextInt(3) > 1? 1:-1;
                        tempV.y += hintNode.getLocalPosition().y;
                        tempV.z += (hintNode.getLocalPosition().z + random.nextInt(4) +1) * negFactor;
                        hintNode.setLocalPosition(tempV);
                        isHintGenerated = !isHintGenerated;
                    }
                }
            }
        });
    }

    private void initBoss(){
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Frame frame = arFragment.getArSceneView().getArFrame();
            Camera camera = frame.getCamera();
            Collection<Plane> planes = frame.getUpdatedTrackables(Plane.class);

            for (Plane plane : planes) {
                if (camera.getTrackingState() != TrackingState.TRACKING) return;
                if (plane.getTrackingState() == TrackingState.TRACKING) {
                    bossAnchor = plane.createAnchor(plane.getCenterPose());
                    AnchorNode anchorNode = new AnchorNode(bossAnchor);
                    anchorNode.setParent(scene);

                    if (!isBossGenerated) {
                        bossNode = new BossNode(arFragment.getTransformationSystem());
                        bossNode.setParent(anchorNode);
                        bossNode.setName(modelNames[BOSS_INDEX]);
                        bossNode.setRenderable(renderables[BOSS_INDEX]);
                        Random random = new Random();
                        Vector3 tempV = new Vector3();
                        int negFactor;
                        negFactor = random.nextInt(3) > 1? 1:-1;
                        tempV.x += (bossNode.getLocalPosition().x + random.nextInt(4) +1) * negFactor;
                        negFactor = random.nextInt(3) > 1? 1:-1;
                        tempV.y += bossNode.getLocalPosition().y;
                        tempV.z += (bossNode.getLocalPosition().z + random.nextInt(4) +1) * negFactor;
                        bossNode.setLocalPosition(tempV);
                        isBossGenerated = !isBossGenerated;
                    }
                }
            }
        });
    }

    private void shootToUser() {
        Frame frame = arFragment.getArSceneView().getArFrame();
        Camera camera = frame.getCamera();
        Collection<Plane> planes = frame.getUpdatedTrackables(Plane.class);
        Node bulletNode;
        AnchorNode startNode, endNode;

        for (Plane plane : planes) {
            if (camera.getTrackingState() != TrackingState.TRACKING) return;

            if (plane.getTrackingState() == TrackingState.TRACKING) {
                Anchor anchor = plane.createAnchor(plane.getCenterPose());
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(scene);

                bulletNode = new BulletNode(arFragment.getTransformationSystem());
                bulletNode.setParent(anchorNode);
                bulletNode.setName(modelNames[BULLET_INDEX]);
                bulletNode.setRenderable(renderables[BULLET_INDEX]);
                bulletNode.setWorldPosition(bossNode.getWorldPosition());
            }
        }
    }

    private int[] screenPt = new int[2];
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        aimView.getLocationOnScreen(screenPt);
    }
    private HitTestResult sceneHitTest(float x, float y) {
        Ray ray = scene.getCamera().screenPointToRay(x, y);
        return scene.hitTest(ray);
    }
    private void shoot() {
        Frame frame = arFragment.getArSceneView().getArFrame();
        Camera camera = frame.getCamera();

        if (camera.getTrackingState() != TrackingState.TRACKING) return;

        // check if any scene node gets hit
        HitTestResult sceneHit = sceneHitTest(screenPt[0], screenPt[1]);
        if (sceneHit != null && sceneHit.getNode() != null) {
            Node node = sceneHit.getNode();

            if (node.getName().equals(modelNames[HINT_INDEX])) {
                // update hint on map

                if (isInsideDetArea) {
                    hintMarker.remove();
                    hintCircle.remove();
                    btn_camera.performClick();

                    if (!bossAreaFound){
                        currentHintNumber++;
                        tv_hintFound.setText(currentHintNumber+"/"+ HINT_NUMBER_NEEDED);
                        bossAreaFound = currentHintNumber >= HINT_NUMBER_NEEDED? true:false;
                        GenerateHint(currLocation);
                    }

                    isInsideDetArea = false;
                }

                boolean deleteParent = currentHintNumber < HINT_NUMBER_NEEDED? false:true;
                removeNode(hintNode, deleteParent);

            } else if (node.getName().equals(modelNames[BOSS_INDEX])) {
                // - boss hp
//                bossHP -= 400;
                removeNode(bossNode, false);
//                Intent i = new Intent(MapsActivity.this, ResultActivity.class);
//                // putExtra
//                i.putExtra("TIME_STRING", timeString);
//                i.putExtra("DISTANCE", distance);
//                i.putExtra("TIME_SECOND", timeSecond);
//                startActivity(i);
            }
        }
    }
}


