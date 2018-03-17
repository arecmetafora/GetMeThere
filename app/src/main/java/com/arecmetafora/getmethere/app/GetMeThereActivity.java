package com.arecmetafora.getmethere.app;

import android.Manifest;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.arecmetafora.getmethere.AugmentedRealityCompass;
import com.arecmetafora.getmethere.Map;
import com.arecmetafora.getmethere.Compass;
import com.arecmetafora.getmethere.CompassSensor;
import com.arecmetafora.getmethere.GeoURI;
import com.arecmetafora.getmethere.OfflineGoogleMaps;
import com.arecmetafora.getmethere.OfflineMap;

public class GetMeThereActivity extends AppCompatActivity implements CompassSensor.RotationCallback {

    private Map mMap;
    private Compass mCompass;
    private Location mLocationToTrack;
    private CompassSensor mCompassSensor;

    private static final int ANGLE_CHANGE_LAYOUT = 60;
    private static boolean sUsingAR = false;

    private final float[] mOrientationData = new float[3];
    int easterEggNumberOfTaps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!sUsingAR) {
            setContentView(R.layout.activity_getmethere);
        } else {
            setContentView(R.layout.activity_getmethere_ar);
        }

        // Reading intent parameters
        if(getIntent() != null && Intent.ACTION_VIEW.equals(getIntent().getAction()) &&
            getIntent().getData() != null && "geo".equals(getIntent().getData().getScheme())) {
            GeoURI geoUri = GeoURI.parse(getIntent().getData());
            if(geoUri != null) {
                mLocationToTrack = new Location("");
                mLocationToTrack.setLatitude(geoUri.getLatitude());
                mLocationToTrack.setLongitude(geoUri.getLongitude());
            }
        }

        // No location to track. Fininsh activity
        if(mLocationToTrack == null) {
            finish();
            return;
        }

        OfflineMap offlineMap = OfflineGoogleMaps.fromLocation(this, mLocationToTrack);
        if(offlineMap == null) {
            Snackbar.make(findViewById(android.R.id.content),
                        getResources().getString(R.string.offline_location_not_downloaded),
                        Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.offline_location_download,
                            v -> LocationDownloaderActivity.requestDownloadLocation(this, mLocationToTrack))
                    .show();
        }

        mCompass = findViewById(R.id.compass);
        mMap = findViewById(R.id.map);
        AugmentedRealityCompass mAugmetedRealityCompass = findViewById(R.id.augmented_reality_compass);

        mCompassSensor = CompassSensor.from(this, (LifecycleOwner) this)
                .bindTo(this)
                .track(mLocationToTrack);

        if(mCompass != null) {
            mCompassSensor.bindTo(mCompass);
            mCompass.setOnClickListener((view) -> {
                if(++easterEggNumberOfTaps == 10) {
                    bookingEasterEgg();
                }
            });
        }
        if(mMap != null) {
            mCompassSensor.bindTo(mMap);
            mMap.setOfflineMap(offlineMap);
        }
        if(mAugmetedRealityCompass != null) {
            mCompassSensor.bindTo(mAugmetedRealityCompass);
        }

        // Check permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA },
                    0);
        }
    }

    private void bookingEasterEgg() {
        float density = getResources().getDisplayMetrics().density;

        mMap.setLocationIcon(R.drawable.location_booking);

        mCompass.setArcWidth(20 * density);
        mCompass.setArcColor(Color.argb(255, 0, 53, 128));
        mCompass.setTextSize(30 * density);
        mCompass.setTextColor(Color.argb(255, 0, 158, 230));
        mCompass.setLocationIcon(R.drawable.location_hotel);
        mCompass.setPointer(R.drawable.compass_pointer);

        Toast.makeText(this, "Hello, Booking.com! Hire me, please! :)", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mCompassSensor.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            if(mMap != null) {
                mMap.setOfflineMap(OfflineGoogleMaps.fromLocation(this, mLocationToTrack));
            }
        }
    }

    @Override
    public void onTrackingNewLocation(Location location) {
    }

    @Override
    public void onNewLocation(Location myLocation) {
    }

    @Override
    public void onNewRotation(float[] rotationMatrix) {
        SensorManager.getOrientation(rotationMatrix, mOrientationData);
        double pitchAngle = Math.abs(Math.toDegrees(mOrientationData[1]));
        if(!sUsingAR && pitchAngle > ANGLE_CHANGE_LAYOUT) {
            sUsingAR = true;
            mCompassSensor.stop();
            recreate();
        } else if(sUsingAR && pitchAngle < ANGLE_CHANGE_LAYOUT) {
            sUsingAR = false;
            mCompassSensor.stop();
            recreate();
        }
    }
}
