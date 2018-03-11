package com.arecmetafora.getmethere;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

public class OfflineLocationsActivity extends AppCompatActivity implements OfflineLocationsAdapter.Callback {

    OfflineLocationsAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_locations);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.offline_locations_add);
        fab.setOnClickListener(v ->
            startActivity(new Intent(getApplicationContext(), LocationDownloaderActivity.class))
        );

        RecyclerView offlineLocationsList = findViewById(R.id.offline_locations_list);
        offlineLocationsList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new OfflineLocationsAdapter(this);
        offlineLocationsList.setAdapter(mAdapter);

        OfflineLocationViewModel model = ViewModelProviders.of(this).get(OfflineLocationViewModel.class);
        model.getSavedLocations().observe(this, offlineLocations -> mAdapter.setItems(offlineLocations) );

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                OfflineLocation location = mAdapter.removeItem(viewHolder.getLayoutPosition());
                if(location.mapFile.delete()) {
                    Snackbar.make(findViewById(R.id.offline_locations_layout),
                            String.format(getString(R.string.offline_map_deleted), location.description),
                            Snackbar.LENGTH_LONG).show();
                }
            }
        }).attachToRecyclerView(offlineLocationsList);
    }

    @Override
    public void onSelectedLocation(OfflineLocation location) {
        Intent intent = new Intent( this, GetMeThereActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(GeoURI.fromLocation(location.location));
        startActivity(intent);
    }
}
