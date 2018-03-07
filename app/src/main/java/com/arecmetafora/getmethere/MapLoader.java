package com.arecmetafora.getmethere;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MapLoader extends AsyncTaskLoader<OfflineMap> {

    private static final String URL_TEMPLATE = "http://maps.google.com/maps/api/staticmap?center=%s,%s&zoom=%s&size=%sx%s&scale=%s";
    private static final String FILE_CACHE_TEMPLATE = "map_%s,%s";
    private static final int DEFAULT_ZOOM_LEVEL = 15;
    private static final int DEFAULT_MAP_WIDTH = 600;
    private static final int DEFAULT_MAP_HEIGHT = 400;
    private static final int DEFAULT_MAP_SCALE = 2; // 1, 2 or 4* (*only available for Google API Premium)
    private static final int BUFFER_SIZE = 1024;

    private String mUrl;
    private File mCacheFile;
    private Location mCenterLocation;

    MapLoader(@NonNull Context context, Location centerLocation) {
        super(context);
        mCenterLocation = centerLocation;

        mUrl = String.format(URL_TEMPLATE,
                centerLocation.getLatitude(), centerLocation.getLongitude(),
                DEFAULT_ZOOM_LEVEL, DEFAULT_MAP_WIDTH, DEFAULT_MAP_HEIGHT, DEFAULT_MAP_SCALE);

        String cacheFileName = String.format(FILE_CACHE_TEMPLATE,
                centerLocation.getLatitude(), centerLocation.getLongitude());
        mCacheFile = new File(context.getFilesDir(), cacheFileName);
    }

    @Override
    protected void onStartLoading() {
        if(mCacheFile.exists()) {
            deliverResult(newMap());
        } else {
            forceLoad();
        }
    }

    @Override
    public OfflineMap loadInBackground() {

        HttpURLConnection connection = null;
        BufferedInputStream in = null;
        FileOutputStream out = null;

        try {
            connection = (HttpURLConnection) new URL(mUrl).openConnection();
            in = new BufferedInputStream(connection.getInputStream());
            out = getContext().openFileOutput(mCacheFile.getName(), Context.MODE_PRIVATE);

            byte[] buf = new byte[BUFFER_SIZE];
            int readBytes;
            while((readBytes = in.read(buf)) > 0) {
                out.write(buf, 0, readBytes);
            }

            return newMap();

        } catch (Exception e) {
            // Ignore. Let`s see what to do here
            return null;

        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private OfflineMap newMap() {
        Bitmap mapImage = BitmapFactory.decodeFile(mCacheFile.getAbsolutePath());
        return new OfflineGoogleMaps(mapImage, mCenterLocation, DEFAULT_ZOOM_LEVEL, OfflineGoogleMaps.Scale.ENHANCED);
    }
}
