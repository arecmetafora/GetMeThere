package com.arecmetafora.getmethere;

import android.location.Location;

/**
 * Class responsible for:
 * <ul>
 *     <li>Convert GPS coordinate to ECEF coordinate (Earth-centered Earth-fixed coordinate)</li>
 *     <li>Convert ECEF coordinate to Navigation coordinate (variation of <a href="https://en.wikipedia.org/wiki/North_east_down">NED</a>)</li>
 * </ul>
 * From https://github.com/dat-ng/ar-location-based-android
 */
public final class ECEF {
    private final static double WGS84_A = 6378137.0;           // WGS 84 semi-major axis constant in meters
    private final static double WGS84_E2 = 0.00669437999014;   // square of WGS 84 eccentricity

    /**
     * Converts a GPS coordinate to a ECEF coordinate (Earth-centered Earth-fixed coordinate)
     *
     * @param location The GPS coordinate.
     * @return An array with the three axis, X, Y and Z.
     */
    public static float[] fromWSG84(Location location) {
        double radLat = Math.toRadians(location.getLatitude());
        double radLon = Math.toRadians(location.getLongitude());

        float clat = (float) Math.cos(radLat);
        float slat = (float) Math.sin(radLat);
        float clon = (float) Math.cos(radLon);
        float slon = (float) Math.sin(radLon);

        float N = (float) (WGS84_A / Math.sqrt(1.0 - WGS84_E2 * slat * slat));

        float x = (float) ((N + location.getAltitude()) * clat * clon);
        float y = (float) ((N + location.getAltitude()) * clat * slon);
        float z = (float) ((N * (1.0 - WGS84_E2) + location.getAltitude()) * slat);

        return new float[] {x , y, z};
    }

    /**
     * Convert a ECEF coordinate to a Navigation coordinate
     *
     * @param location The GPS coordinate.
     * @return An array with the values of the three orientations, East, North and Up
     */
    public static float[] toENU(Location location, float[] ecefCurrentLocation, float[] ecefPOI) {
        double radLat = Math.toRadians(location.getLatitude());
        double radLon = Math.toRadians(location.getLongitude());

        float clat = (float)Math.cos(radLat);
        float slat = (float)Math.sin(radLat);
        float clon = (float)Math.cos(radLon);
        float slon = (float)Math.sin(radLon);

        float dx = ecefCurrentLocation[0] - ecefPOI[0];
        float dy = ecefCurrentLocation[1] - ecefPOI[1];
        float dz = ecefCurrentLocation[2] - ecefPOI[2];

        float east = -slon*dx + clon*dy;

        float north = -slat*clon*dx - slat*slon*dy + clat*dz;

        float up = clat*clon*dx + clat*slon*dy + slat*dz;

        return new float[] {east , north, up, 1};
    }
}
