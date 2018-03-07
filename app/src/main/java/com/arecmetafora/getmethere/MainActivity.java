package com.arecmetafora.getmethere;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<OfflineMap> {

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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                    0);
        }

        getSupportLoaderManager().initLoader(1, null, this);
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

        mMap.setLocationIcon(R.drawable.location_icon);

        mCompass.setArcWidth(10 * density);
        mCompass.setArcColor(Color.argb(255, 76, 175, 80));
        mCompass.setTextSize(40 * density);
        mCompass.setTextColor(Color.argb(255, 96, 125, 139));
        mCompass.setLocationIcon(R.drawable.location_icon);
        mCompass.setPointer(R.drawable.compass_pointer);
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
    public Loader<OfflineMap> onCreateLoader(int id, Bundle args) {
        return new MapLoader(this, mLocationToTrack);
    }

    @Override
    public void onLoadFinished(Loader<OfflineMap> loader, OfflineMap data) {
        mMap.setOfflineMap(data);
    }

    @Override
    public void onLoaderReset(Loader<OfflineMap> loader) {
    }
}
