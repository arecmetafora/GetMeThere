package com.arecmetafora.getmethere;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;

import java.io.File;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<File> {

    private Map mMap;
    private Compass mCompass;
    private Location mLocationToTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCompass = findViewById(R.id.compass);
        mMap = findViewById(R.id.map);

        mLocationToTrack = new Location("");
        mLocationToTrack.setLatitude(-23.605689);
        mLocationToTrack.setLongitude(-46.664609);

        if(getIntent() != null && Intent.ACTION_VIEW.equals(getIntent().getAction()) &&
            getIntent().getData() != null && "geo".equals(getIntent().getData().getScheme())) {
            GeoURI geoUri = GeoURI.parse(getIntent().getData());
            if(geoUri != null) {
                mLocationToTrack.setLatitude(geoUri.getLatitude());
                mLocationToTrack.setLongitude(geoUri.getLongitude());
            }
        }

        mCompass.setLocationToTrack(mLocationToTrack);
        mMap.setLocationToTrack(mLocationToTrack);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                    0);
        }

        getSupportLoaderManager().initLoader(1, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCompass.onStart();
        mMap.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCompass.onStop();
        mMap.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mCompass.onStart();
    }

    @Override
    public Loader<File> onCreateLoader(int id, Bundle args) {
        return new MapLoader(this, mLocationToTrack);
    }

    @Override
    public void onLoadFinished(Loader<File> loader, File data) {
        Bitmap mapImage = BitmapFactory.decodeFile(data.getAbsolutePath());
        mMap.setMapImage(mapImage);
    }

    @Override
    public void onLoaderReset(Loader<File> loader) {
    }
}
