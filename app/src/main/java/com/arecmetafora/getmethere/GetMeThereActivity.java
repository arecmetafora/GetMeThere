package com.arecmetafora.getmethere;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class GetMeThereActivity extends AppCompatActivity {

    private Map mMap;
    private Compass mCompass;
    private Location mLocationToTrack;
    private CompassSensor mCompassSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_getmethere);

        mCompass = findViewById(R.id.compass);
        mMap = findViewById(R.id.map);

        mCompassSensor = new CompassSensor(this, getLifecycle())
                .bindTo(mMap)
                .bindTo(mCompass);

        if(getIntent() != null && Intent.ACTION_VIEW.equals(getIntent().getAction()) &&
            getIntent().getData() != null && "geo".equals(getIntent().getData().getScheme())) {
            GeoURI geoUri = GeoURI.parse(getIntent().getData());
            if(geoUri != null) {
                mLocationToTrack = new Location("");
                mLocationToTrack.setLatitude(geoUri.getLatitude());
                mLocationToTrack.setLongitude(geoUri.getLongitude());
            }
        }

        if(mLocationToTrack == null) {
            mLocationToTrack = new Location("");
            mLocationToTrack.setLatitude(-23.605689);
            mLocationToTrack.setLongitude(-46.664609);
        }

        mCompassSensor.setLocationToTrack(mLocationToTrack);
        mMap.setOfflineMap(OfflineGoogleMaps.fromLocation(this, mLocationToTrack));

        // Check permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                    0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.main_menu_style) {
            beautifyIt();
            return true;
        }
        return false;
    }

    private void beautifyIt() {
        float density = getResources().getDisplayMetrics().density;

        mMap.setLocationIcon(R.drawable.location_booking);

        mCompass.setArcWidth(20 * density);
        mCompass.setArcColor(Color.argb(255, 0, 53, 128));
        mCompass.setTextSize(30 * density);
        mCompass.setTextColor(Color.argb(255, 0, 158, 230));
        mCompass.setLocationIcon(R.drawable.location_hotel);
        mCompass.setPointer(R.drawable.compass_pointer);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mCompassSensor.start();
    }
}
