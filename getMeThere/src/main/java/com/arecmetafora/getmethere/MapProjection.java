package com.arecmetafora.getmethere;

import android.graphics.PointF;
import android.location.Location;

/**
 * Common operations of a map projection.
 */
public interface MapProjection {

    /**
     * Converts a geographic coordinate to cartesian coordinate system.
     *
     * @param geographicLocation The geographic coordinate
     *
     * @return The cartesian projection of geographic coordinate
     */
    PointF toCartesian(Location geographicLocation);

    /**
     * Converts a cartesian coordinate to geographic coordinate system.
     *
     * @param cartesianPoint The cartesian coordinate
     *
     * @return The geographic projection of cartesian coordinate
     */
    Location toGeographic(PointF cartesianPoint);
}
