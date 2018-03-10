package com.arecmetafora.getmethere;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates a representation of a small offline map using GoogleMaps engine.
 */
public class OfflineGoogleMaps extends OfflineMap {

    // Constants
    private static final String DOWNLOAD_URL_TEMPLATE = "http://maps.google.com/maps/api/staticmap?center=%s,%s&zoom=%s&size=%sx%s&scale=%s";
    private static final String CACHE_NAME_TEMPLATE = "map(%.6f,%.6f,%s) - %s.png";
    public static final Pattern CACHE_NAME_PATTERN = Pattern.compile("map\\(([\\-0-9\\.]+),([\\-0-9\\.]+),(\\d*)\\) \\- (.*)\\.png");
    private static final int DEFAULT_ZOOM_LEVEL = 15;
    private static final int DEFAULT_MAP_WIDTH = 600;
    private static final int DEFAULT_MAP_HEIGHT = 400;
    private static final int DEFAULT_MAP_SCALE = 2; // 1, 2 or 4* (*only available for Google API Premium)

    /**
     * Types of pre-defined GoogleMaps scales.
     */
    public enum Scale {
        NORMAL(1),
        ENHANCED(2),
        PREMIUM(3);

        int value;
        Scale(int value) {
            this.value = value;
        }
    }

    /**
     * Size of a GoogleMaps tile.
     */
    private static final int TILE_SIZE = 256;

    /**
     * Creates a new offline map.
     *
     * @param mapBitmap Bitmap which represents this offline map.
     * @param centerCoordinate Center of this offline map in geographic coordinate system.
     * @param zoom Zoom level applied for this map.
     * @param scale Scale of this map.
     */
    public OfflineGoogleMaps(Bitmap mapBitmap, Location centerCoordinate, int zoom, Scale scale) {
        super(mapBitmap, scale.value, centerCoordinate, new MercatorProjection(zoom, TILE_SIZE));
    }

    /**
     * Loads an Offline map from a file.
     *
     * @param file The file containing the offline map.
     *
     * @return The offline map.
     */
    public static OfflineMap fromFile(File file) {
        Matcher m = CACHE_NAME_PATTERN.matcher(file.getName());

        if(m.matches()) {
            Location centerLocation = new Location("");
            centerLocation.setLatitude(Double.parseDouble(m.group(1)));
            centerLocation.setLongitude(Double.parseDouble(m.group(2)));

            double zoom = Double.parseDouble(m.group(3));

            Bitmap mapImage = BitmapFactory.decodeFile(file.getAbsolutePath());

            return new OfflineGoogleMaps(mapImage, centerLocation, DEFAULT_ZOOM_LEVEL, OfflineGoogleMaps.Scale.ENHANCED);

        } else {
            return null;
        }
    }

    /**
     * Loads an Offline map from a location.
     *
     * @param context The application context.
     * @param location The location to open the map.
     *
     * @return The offline map.
     */
    public static OfflineMap fromLocation(Context context, Location location) {
        String cacheFileName = String.format(Locale.US, CACHE_NAME_TEMPLATE,
                location.getLatitude(), location.getLongitude(), DEFAULT_ZOOM_LEVEL, "")
                .replace(".png", "");

        for(File mapFile : context.getFilesDir().listFiles()) {
            if(mapFile.getName().startsWith(cacheFileName)) {
                return fromFile(mapFile);
            }
        }

        return null;
    }

    /**
     * Gets a sample image of a GoogleMaps map.
     *
     * @param context The application context.
     * @param location The location that will be the center of the map.
     */
    public static Bitmap getSample(Context context, Location location) {

        String url = String.format(Locale.US,DOWNLOAD_URL_TEMPLATE + "&markers=color:red%%7C%s,%s",
                location.getLatitude(), location.getLongitude(),
                DEFAULT_ZOOM_LEVEL, DEFAULT_MAP_WIDTH, DEFAULT_MAP_HEIGHT, DEFAULT_MAP_SCALE,
                location.getLatitude(), location.getLongitude());

        HttpURLConnection connection = null;
        BufferedInputStream in = null;

        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            in = new BufferedInputStream(connection.getInputStream());
            return BitmapFactory.decodeStream(in);

        } catch (Exception e) {
            return null;

        } finally {
            try {
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

    /**
     * Downloads a GoogleMaps snapshot and save it locally.
     *
     * @param context The application context.
     * @param location The location that will be the center of the map.
     * @param description The description about the location that is being downloaded.
     */
    public static void cache(Context context, Location location, String description) throws Exception {

        String url = String.format(Locale.US, DOWNLOAD_URL_TEMPLATE,
                location.getLatitude(), location.getLongitude(),
                DEFAULT_ZOOM_LEVEL, DEFAULT_MAP_WIDTH, DEFAULT_MAP_HEIGHT, DEFAULT_MAP_SCALE);

        String cacheFileName = String.format(Locale.US, CACHE_NAME_TEMPLATE,
                location.getLatitude(), location.getLongitude(), DEFAULT_ZOOM_LEVEL, description);

        File mapFile = new File(context.getFilesDir(), cacheFileName);
        if (mapFile.exists()) {
            return;
        }

        HttpURLConnection connection = null;
        BufferedInputStream in = null;
        FileOutputStream out = null;

        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            in = new BufferedInputStream(connection.getInputStream());
            out = context.openFileOutput(mapFile.getName(), Context.MODE_PRIVATE);

            byte[] buf = new byte[1024];
            int readBytes;
            while ((readBytes = in.read(buf)) > 0) {
                out.write(buf, 0, readBytes);
            }
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
}
