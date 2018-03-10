package com.arecmetafora.getmethere;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

class LocationDownloaderActivity extends AppCompatActivity {

    private EditText mTxtSearchLocation;
    private ImageView mImgLocation;
    private Button mBtnConfirm;
    private FrameLayout mLoadingFrame;

    private OfflineLocation mLastSearchedLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_downloader);

        mTxtSearchLocation = findViewById(R.id.location_downloader_search);
        mImgLocation = findViewById(R.id.location_downloader_current_location);
        mBtnConfirm = findViewById(R.id.location_downloader_confirm);
        mLoadingFrame = findViewById(R.id.location_downloader_loading_frame);

        mImgLocation.setVisibility(View.INVISIBLE);
        mBtnConfirm.setVisibility(View.INVISIBLE);
        mLoadingFrame.setVisibility(View.INVISIBLE);
    }

    public void onSearchLocation(View view) {
        mLoadingFrame.setVisibility(View.VISIBLE);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        OfflineLocationViewModel model = ViewModelProviders.of(this).get(OfflineLocationViewModel.class);
        model.getSample(mTxtSearchLocation.getText().toString()).observe(this, sampleLocation -> {
            if(sampleLocation == null) {
                Snackbar.make(findViewById(android.R.id.content),
                          "Error while loading location", Snackbar.LENGTH_LONG).show();
                mLastSearchedLocation = null;
                mImgLocation.setVisibility(View.INVISIBLE);
                mBtnConfirm.setVisibility(View.INVISIBLE);
                mLoadingFrame.setVisibility(View.INVISIBLE);

            } else if(sampleLocation.mapBitmap != null) {
                mLastSearchedLocation = sampleLocation;
                mImgLocation.setImageBitmap(sampleLocation.mapBitmap);
                mImgLocation.setVisibility(View.VISIBLE);
                mBtnConfirm.setVisibility(View.VISIBLE);
                mLoadingFrame.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void onConfirmSaveLocation(View view) {
        mLoadingFrame.setVisibility(View.VISIBLE);

        OfflineLocationViewModel model = ViewModelProviders.of(this).get(OfflineLocationViewModel.class);
        model.saveLocation(mLastSearchedLocation).observe(this, result -> {

            if(result == null) {
                result = OfflineLocation.Result.ERROR;
            }

            switch (result) {
                case ERROR:
                    Snackbar.make(findViewById(android.R.id.content),
                            "Error while saving location", Snackbar.LENGTH_LONG).show();
                    mLoadingFrame.setVisibility(View.INVISIBLE);
                    break;

                case SUCCESS:
                    finish();
                    break;
            }
        });
    }
}
