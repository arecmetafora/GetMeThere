package com.arecmetafora.getmethere.app;

import android.app.Activity;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

public class LocationDownloaderActivity extends AppCompatActivity {

    public static class GetMapDescriptionDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final EditText inputField = new EditText(getContext());

            AlertDialog alert = new AlertDialog.Builder(getContext())
                    .setTitle(getResources().getString(R.string.choose_map_description))
                    .setPositiveButton(android.R.string.ok,
                        (DialogInterface dialog, int whichButton) -> {

                            InputMethodManager imm = (InputMethodManager) getContext()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            assert imm != null;
                            imm.hideSoftInputFromWindow(inputField.getWindowToken(), 0);

                            ((LocationDownloaderActivity)getActivity())
                                    .saveLocation(inputField.getText().toString());
                            dismiss();
                        }
                    )
                    .setNegativeButton(android.R.string.cancel,
                        (DialogInterface dialog, int whichButton) -> dismiss()
                    )
                    .create();

            int margin = (int)(8 * getResources().getDisplayMetrics().density);
            alert.setView(inputField, margin, margin, margin, margin);

            return alert;
        }
    }

    private static final String PARAM_OFFLINE_LOCATION = "OFFLINE_LOCATION";
    private static final String LOCATION_TO_DOWNLOAD = "LOCATION_TO_DOWNLOAD";

    private EditText mTxtSearchLocation;
    private ImageView mImgLocation;
    private View mConfirmLayout;
    private FrameLayout mLoadingFrame;

    private OfflineLocation mLastSearchedLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_downloader);

        mTxtSearchLocation = findViewById(R.id.location_downloader_search);
        mImgLocation = findViewById(R.id.location_downloader_current_location);
        mConfirmLayout = findViewById(R.id.location_downloader_confirm_layout);
        mLoadingFrame = findViewById(R.id.location_downloader_loading_frame);

        mConfirmLayout.setVisibility(View.INVISIBLE);
        mLoadingFrame.setVisibility(View.INVISIBLE);

        mTxtSearchLocation.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                onSearchLocation(v);
                return true;
            }
            return false;
        });

        if(savedInstanceState != null && savedInstanceState.containsKey(PARAM_OFFLINE_LOCATION)) {
            mLastSearchedLocation = (OfflineLocation) savedInstanceState.getParcelable(PARAM_OFFLINE_LOCATION);
            loadLocation(mLastSearchedLocation);
        } else if(savedInstanceState == null && getIntent().hasExtra(LOCATION_TO_DOWNLOAD)) {
            Location location = getIntent().getParcelableExtra(LOCATION_TO_DOWNLOAD);
            mTxtSearchLocation.setText(String.format(Locale.US,
                    "%.6f,%.6f", location.getLatitude(), location.getLongitude()));
            onSearchLocation(mTxtSearchLocation);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mLastSearchedLocation != null) {
            outState.putParcelable(PARAM_OFFLINE_LOCATION, mLastSearchedLocation);
        }
    }

    public void onSearchLocation(View view) {
        mLoadingFrame.setVisibility(View.VISIBLE);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        OfflineLocationViewModel model = ViewModelProviders.of(this).get(OfflineLocationViewModel.class);
        model.getSample(mTxtSearchLocation.getText().toString()).observe(this, this::loadLocation);
    }

    public void onConfirmSaveLocation(View view) {
        new GetMapDescriptionDialog().show(getSupportFragmentManager(), "dialog");
    }

    private void loadLocation(OfflineLocation location) {
        if(location == null) {
            Snackbar.make(findViewById(android.R.id.content),
                    getString(R.string.could_not_retrieve_location), Snackbar.LENGTH_LONG).show();
            mLastSearchedLocation = null;
            mConfirmLayout.setVisibility(View.INVISIBLE);
            mLoadingFrame.setVisibility(View.INVISIBLE);

        } else if(location.mapBitmap != null) {
            mLastSearchedLocation = location;
            mImgLocation.setImageBitmap(location.mapBitmap);
            mConfirmLayout.setVisibility(View.VISIBLE);
            mLoadingFrame.setVisibility(View.INVISIBLE);
        }
    }

    private void saveLocation(String description) {
        mLoadingFrame.setVisibility(View.VISIBLE);

        mLastSearchedLocation.description = description;
        OfflineLocationViewModel model = ViewModelProviders.of(this).get(OfflineLocationViewModel.class);
        model.saveLocation(mLastSearchedLocation).observe(this, result -> {

            if(result == null) {
                result = OfflineLocation.Result.ERROR;
            }

            switch (result) {
                case ERROR:
                    Snackbar.make(findViewById(android.R.id.content),
                            getString(R.string.error_while_saving_location), Snackbar.LENGTH_LONG).show();
                    mLoadingFrame.setVisibility(View.INVISIBLE);
                    break;

                case SUCCESS:
                    setResult(RESULT_OK);
                    finish();
                    break;
            }
        });
    }

    static void requestDownloadLocation(Activity activity, Location location) {
        Intent intent = new Intent(activity, LocationDownloaderActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(LOCATION_TO_DOWNLOAD, location);
        activity.startActivityForResult(intent, 0);
    }
}
