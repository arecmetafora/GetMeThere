package com.arecmetafora.getmethere.app;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.FileObserver;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.arecmetafora.getmethere.OfflineGoogleMaps;

public class OfflineLocationViewModel extends AndroidViewModel {

    class OfflineLocationLiveData extends MutableLiveData<List<OfflineLocation>> {

        private FileObserver mFileObserver;

        private OfflineLocationLiveData(Context context) {
            // Observer changes in disk to refresh saved offline maps
            mFileObserver = new FileObserver(context.getFilesDir().getAbsolutePath(), FileObserver.CREATE) {
                @Override
                public void onEvent(int event, String file) {
                    loadSavedLocations();
                }
            };
        }

        @Override
        protected void onActive() {
            mFileObserver.startWatching();
        }
    }

    private OfflineLocationLiveData mSavedLocations;

    public OfflineLocationViewModel(@NonNull Application application) {
        super(application);
    }

    OfflineLocationLiveData getSavedLocations() {
        if (mSavedLocations == null) {
            mSavedLocations = new OfflineLocationLiveData(getApplication());
            loadSavedLocations();
        }
        return mSavedLocations;
    }

    @SuppressLint("StaticFieldLeak")
    private void loadSavedLocations() {

        new AsyncTask<Void, Void, List<OfflineLocation>>() {
            @Override
            protected List<OfflineLocation> doInBackground(Void... voids) {
                List<OfflineLocation> offlineLocations = new LinkedList<>();

                for(File mapFile : getApplication().getFilesDir().listFiles()) {
                    Matcher m = OfflineGoogleMaps.CACHE_NAME_PATTERN.matcher(mapFile.getName());
                    if(m.matches()) {
                        Location location = new Location("");
                        location.setLatitude(Double.parseDouble(m.group(1)));
                        location.setLongitude(Double.parseDouble(m.group(2)));

                        OfflineLocation offlineLocation = new OfflineLocation();
                        offlineLocation.mapFile = mapFile;
                        offlineLocation.location = location;
                        offlineLocation.description = m.group(4);
                        offlineLocations.add(offlineLocation);
                    }
                }

                return offlineLocations;
            }

            @Override
            protected void onPostExecute(List<OfflineLocation> offlineLocations) {
                mSavedLocations.setValue(offlineLocations);
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    LiveData<OfflineLocation> getSample(String description) {
        MutableLiveData<OfflineLocation> sampleLocation = new MutableLiveData<>();
        sampleLocation.setValue(new OfflineLocation());

        new AsyncTask<String, Void, OfflineLocation>() {

            @Override
            protected OfflineLocation doInBackground(String... locations) {
                try {
                    List<Address> addresses;

                    Pattern pattern = Pattern.compile("([\\-0-9\\.]+)\\s*,\\s*([\\-0-9\\.]+)");
                    Matcher matcher = pattern.matcher(locations[0]);
                    double latitude = 0;
                    double longitude = 0;
                    if(matcher.matches()) {
                        latitude = Double.valueOf(matcher.group(1));
                        longitude = Double.valueOf(matcher.group(2));
                        addresses = new Geocoder(getApplication())
                                .getFromLocation(latitude, longitude, 1);
                    } else {
                        addresses = new Geocoder(getApplication())
                                .getFromLocationName(locations[0], 1);
                    }

                    if(addresses != null && addresses.size() > 0) {
                        Address address = addresses.get(0);
                        Location location = new Location("");
                        if(latitude != 0 && longitude != 0) {
                            location.setLatitude(latitude);
                            location.setLongitude(longitude);
                        } else {
                            location.setLatitude(address.getLatitude());
                            location.setLongitude(address.getLongitude());
                        }

                        OfflineLocation sample = new OfflineLocation();
                        sample.location = location;
                        sample.description = description;
                        sample.mapBitmap = OfflineGoogleMaps.getSample(location);
                        return sample;
                    }

                } catch (IOException ignored) {
                }

                return null;
            }

            @Override
            protected void onPostExecute(OfflineLocation sample) {
                sampleLocation.setValue(sample);
            }
        }.execute(description);

        return sampleLocation;
    }

    @SuppressLint("StaticFieldLeak")
    LiveData<OfflineLocation.Result> saveLocation(OfflineLocation offlineLocation) {
        MutableLiveData<OfflineLocation.Result> result = new MutableLiveData<>();
        result.setValue(OfflineLocation.Result.SAVING);

        new AsyncTask<OfflineLocation, Void, OfflineLocation.Result>() {

            @Override
            protected OfflineLocation.Result doInBackground(OfflineLocation... offlineLocations) {
                try {
                    OfflineLocation offlineLocation = offlineLocations[0];
                    OfflineGoogleMaps.cache(getApplication(), offlineLocation.location, offlineLocation.description);
                    return OfflineLocation.Result.SUCCESS;
                } catch (Exception ignored) {
                }

                return OfflineLocation.Result.ERROR;
            }

            @Override
            protected void onPostExecute(OfflineLocation.Result r) {
                result.setValue(r);
            }
        }.execute(offlineLocation);

        return result;
    }
}
