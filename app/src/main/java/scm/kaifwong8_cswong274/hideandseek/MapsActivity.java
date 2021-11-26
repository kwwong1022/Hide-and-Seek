package scm.kaifwong8_cswong274.hideandseek;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {
    private static final String TAG = "MapsActivity";
    private static final int DEFAULT_UPDATE_INTERVAL = 5000;
    private static final int FAST_UPDATE_INTERVAL = 1000;
    private static final int FINE_LOCATION_REQUEST_CODE = 1;
    // ==================================== =================== ====================================

    private GoogleMap mMap;
    private ArFragment arFragment;
    private ModelRenderable modelRenderable;
    private LocationRequest locationRequest;
    private LocationCallback locationCallBack;
    private FusedLocationProviderClient fusedLocationClient;

    private Location currLocation;

    private boolean isShootAvailable;
    private boolean isShieldAvailable;
    private boolean isCameraEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton CameraBtn = (FloatingActionButton) findViewById(R.id.btn_camera);
        CameraBtn.setOnClickListener(v -> {
            Log.i("Tag1","BtnCam");
            if (!isCameraEnabled){
                ConstraintLayout mConstrainLayout  = (ConstraintLayout) findViewById(R.id.top_fragment);
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) mConstrainLayout.getLayoutParams();
                lp.matchConstraintPercentHeight = (float) 1;
                mConstrainLayout.setLayoutParams(lp);
                isCameraEnabled = !isCameraEnabled;
            }
            else {
                ConstraintLayout mConstrainLayout  = (ConstraintLayout) findViewById(R.id.top_fragment);
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) mConstrainLayout.getLayoutParams();
                lp.matchConstraintPercentHeight = (float) 0;
                mConstrainLayout.setLayoutParams(lp);
                isCameraEnabled = !isCameraEnabled;

            }
        });

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


        // ======================================= location ========================================
        initLocationRequest();
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currLocation = locationResult.getLastLocation();
                updateMapUI(currLocation);
                if (locationResult.getLastLocation().getSpeed()>5) {
                    updateMapCamera(currLocation);
                }
            }
        };

        // ========================================== UI ===========================================
        ConstraintLayout ui_background = findViewById(R.id.ui_background);
        FloatingActionButton btn_shoot = findViewById(R.id.btn_shoot);
        FloatingActionButton btn_shield = findViewById(R.id.btn_shield);
        FloatingActionButton btn_camera = findViewById(R.id.btn_camera);
        FloatingActionButton btn_map_focus = findViewById(R.id.btn_map_focus);

        btn_map_focus.setOnClickListener(v -> {
            updateMapCamera(currLocation);
        });

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
            });
        } else {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_REQUEST_CODE);
        }
    }

    private void updateMapUI(Location location) {
        mMap.clear();

        if (location != null) {
            mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(), location.getLongitude()))
                    //.title("")
                    //.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_user))
            ).showInfoWindow();
        }
    }

    private void updateMapCamera(Location location) {
        if (location != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                    .bearing(0)   // degrees clockwise from north
                    .zoom(17).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle));
            if (!success) Log.e(TAG, "Style parsing failed.");
        } catch (Exception e) { Log.e(TAG, "Can't find style. Error: ", e); }

        updateLocation();
    }





    // ==================================== activity life cycle ====================================
    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            updateLocation();
            Log.d(TAG, "onDestroy: locationCallBack requested");
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        }
    }

    @Override
    protected void onPause() {
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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_camera:
                Log.i("Tag1","BtnCam");
                break;
        }

    }
}