package com.arecmetafora.getmethere;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.location.Location;

/**
 * Base of a offline map representation.
 */
public abstract class OfflineMap {

    private static final double EARTH_RADIUS = 6378100.0;

    /**
     * Bitmap which represents this offline map.
     */
    private Bitmap mMapBitmap;

    /**
     * Map drawing scale (like dpi)
     */
    private int mScale;

    /**
     * Implementation of a projector to map geographic coordinates to cartesian points and vice-versa.
     */
    private MapProjection mMapProjection;

    /**
     * Center of this offline map in geographic coordinate system.
     */
    private Location mCenterGeoCoordinate;

    /**
     * Center of this offline map in cartesian coordinate system (pixels).
     */
    private PointF mCenterXYCoordinate;

    /**
     * Creates a new offline map.
     *
     * @param mapBitmap Image which represents this offline map.
     * @param scale Map drawing scale.
     * @param centerGeoCoordinate Center of this offline map in geographic coordinate system.
     * @param mapProjection Center of this offline map in cartesian coordinate system.
     */
    protected OfflineMap(Bitmap mapBitmap, int scale, Location centerGeoCoordinate, MapProjection mapProjection) {
        mMapBitmap = mapBitmap;
        mScale = scale;
        mCenterGeoCoordinate = centerGeoCoordinate;
        mMapProjection = mapProjection;
        mCenterXYCoordinate = mapProjection.toCartesian(mCenterGeoCoordinate);
    }

    /**
     * Projects a geographic coordinate to pixel position (cartesian coordinate).
     *
     * @param geoCoordinate The geographic coordinate to be projected.
     * @return The pixel position of the projected geographic coordinate in the map`s surface.
     */
    public PointF projectToPixel(Location geoCoordinate) {
        PointF cartesianCoordinate = mMapProjection.toCartesian(geoCoordinate);
        return new PointF(
                mMapBitmap.getWidth()/2  + (cartesianCoordinate.x - mCenterXYCoordinate.x) * mScale,
                mMapBitmap.getHeight()/2 + (cartesianCoordinate.y - mCenterXYCoordinate.y) * mScale);
    }

    /**
     * Projects a radius (in meters) from center coordinate to pixel distance.
     *
     * @param radius The radius in meters
     *
     * @return The radius in pixel distance.
     */
    public float projectDistanceFromCenter(float radius) {
        Location l1 = new Location("");
        Location l2 = new Location("");
        l1.setLatitude(radius / EARTH_RADIUS);
        l1.setLongitude(radius / (EARTH_RADIUS * Math.cos(Math.toRadians(mCenterGeoCoordinate.getLatitude()))));

        l2.setLatitude(mCenterGeoCoordinate.getLatitude() + Math.toDegrees(l1.getLatitude()));
        l2.setLongitude(mCenterGeoCoordinate.getLongitude() + Math.toDegrees(l1.getLongitude()));

        PointF p = projectToPixel(l2);

        return Math.abs(mMapBitmap.getWidth()/2 - p.x) * mScale;
    }

    /**
     * @return Center of this offline map in geographic coordinate system.
     */
    public Location getCenterGeoCoordinate() {
        return mCenterGeoCoordinate;
    }

    /**
     * @return Bitmap which represents this offline map.
     */
    public Bitmap getMapBitmap() {
        return mMapBitmap;
    }
}
