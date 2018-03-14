package com.arecmetafora.getmethere;

import android.graphics.PointF;
import android.location.Location;

/**
 * Implementation of a Map projection using the Mercator projection system.
 */
public class MercatorProjection implements MapProjection {

    /**
     * Size of a map tile.
     */
    private int mTileSize;

    /**
     * Zoom level applied for this map.
     */
    private int mZoom;

    /**
     * Creates a new Mercator projection
     *
     * @param zoom Zoom level applied for this map.
     * @param tileSize Size of a map tile.
     */
    public MercatorProjection(int zoom, int tileSize) {
        this.mZoom = zoom;
        this.mTileSize = tileSize;
    }

    @Override
    public PointF toCartesian(Location geographicLocation) {

        double siny = Math.sin(Math.toRadians(geographicLocation.getLatitude()));

        // Truncating to 0.9999 effectively limits latitude to 89.189. This is
        // about a third of a tile past the edge of the world tile.
        siny = Math.min(Math.max(siny, -0.9999), 0.9999);

        int scale = 1 << mZoom;

        PointF worldCoordinate = new PointF(
                (float) (mTileSize * (0.5 + geographicLocation.getLongitude() / 360)),
                (float) (mTileSize * (0.5 - Math.log((1 + siny) / (1 - siny)) / (4 * Math.PI))));

        return new PointF(
                (float) Math.floor(worldCoordinate.x * scale),
                (float) Math.floor(worldCoordinate.y * scale));
    }

    @Override
    public Location toGeographic(PointF cartesianPoint) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
