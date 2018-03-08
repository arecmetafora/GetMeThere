package com.arecmetafora.getmethere;

import android.net.Uri;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to parse URI parts of a geo URI scheme
 */
public class GeoURI {

    private static final Pattern GEO_URI_PATTERN =
            Pattern.compile("geo:([\\-0-9.]+),([\\-0-9.]+)(?:,([\\-0-9.]+))?(?:\\?(.*))?", Pattern.CASE_INSENSITIVE);

    private static final Pattern GEO_URI_QUERY_PATTERN =
            Pattern.compile("q=([\\-0-9.]+),([\\-0-9.]+)(?:,([\\-0-9.]+))?\\(.*\\)?", Pattern.CASE_INSENSITIVE);

    private double mLatitude;
    private double mLongitude;
    private double mAltitude;

    /**
     * Constructor for a Geolocation URI representation.
     *
     * @param latitude The coordinate latitude.
     * @param longitude The coordinate latitude.
     * @param altitude The coordinate altitude.
     */
    private GeoURI(Double latitude, Double longitude, Double altitude) {
        mLatitude = latitude;
        mLongitude = longitude;
        mAltitude = altitude;
    }

    public static GeoURI parse(Uri uri) {
        //geo:0,0?q=-33.4390426969755,-70.6447425484657(Park%20Plaza%20Apart%20Hotel)

        Matcher matcher = GEO_URI_PATTERN.matcher(uri.toString());
        if (!matcher.matches()) {
            return null;
        }

        double latitude;
        double longitude;
        double altitude = 0d;
        try {
            latitude = Double.parseDouble(matcher.group(1));
            if (latitude > 90.0 || latitude < -90.0) {
                return null;
            }
            longitude = Double.parseDouble(matcher.group(2));
            if (longitude > 180.0 || longitude < -180.0) {
                return null;
            }
            if (matcher.group(3) != null) {
                altitude = Double.parseDouble(matcher.group(3));
                if (altitude < 0.0) {
                    return null;
                }
            }

            String query = matcher.group(4);
            if(query != null) {
                matcher = GEO_URI_QUERY_PATTERN.matcher(query);
                if (matcher.matches()) {
                    if (matcher.group(1) != null) {
                        latitude = Double.parseDouble(matcher.group(1));
                        if (latitude > 90.0 || latitude < -90.0) {
                            return null;
                        }
                    }
                    if (matcher.group(2) != null) {
                        longitude = Double.parseDouble(matcher.group(2));
                        if (longitude > 180.0 || longitude < -180.0) {
                            return null;
                        }
                    }
                    if (matcher.group(3) != null) {
                        altitude = Double.parseDouble(matcher.group(3));
                        if (altitude < 0.0) {
                            return null;
                        }
                    }
                }
            }
        } catch (NumberFormatException ignored) {
            return null;
        }

        return new GeoURI(latitude, longitude, altitude);
    }

    /**
     * @return The coordinate latitude.
     */
    public double getLatitude() {
        return mLatitude;
    }

    /**
     * @return The coordinate longitude.
     */
    public double getLongitude() {
        return mLongitude;
    }

    /**
     * @return The coordinate altitude.
     */
    public double getAltitude() {
        return mAltitude;
    }
}
