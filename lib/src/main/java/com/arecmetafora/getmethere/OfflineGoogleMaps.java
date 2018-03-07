package com.arecmetafora.getmethere;

import android.graphics.Bitmap;
import android.location.Location;

/**
 * Creates a representation of a small offline map using GoogleMaps engine.
 */
public class OfflineGoogleMaps extends OfflineMap {

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
     * @param zoom Scale of this map.
     */
    public OfflineGoogleMaps(Bitmap mapBitmap, Location centerCoordinate, int zoom, Scale scale) {
        super(mapBitmap, scale.value, centerCoordinate, new MercatorProjection(zoom, TILE_SIZE));
    }
}
