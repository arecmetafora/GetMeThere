package com.arecmetafora.getmethere;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

/**
 * Very little offline map of the location`s neighborhood.
 */
public class Map extends PinchZoomImageView implements CompassSensor.Callback {

    /**
     * Drawable for this view
     */
    private class MapDrawable extends BitmapDrawable {
        MapDrawable(Bitmap bitmap) {
            super(Map.this.getResources(), bitmap);
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            drawMapOverlay(canvas, this.getBitmap().getWidth(), this.getBitmap().getHeight());
        }
    }

    // Defaults
    private static final int DEFAULT_LOCATION_ICON = R.drawable.default_location_icon;

    /**
     * Drawable representing the location of the place which the user is heading to.
     */
    private Drawable mLocationIcon;

    /**
     * The location of the place which the user is heading to
     */
    private Location mLocationToTrack;

    /**
     * The current user location.
     */
    private Location mMyLocation;

    private CompassSensor mCompassSensor;

    public Map(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        if (attrs != null) {
            TypedArray atts = context.obtainStyledAttributes(attrs,
                    R.styleable.Map, 0, 0);

            Drawable locationIcon = atts.getDrawable(R.styleable.Map_locationIcon);
            if (locationIcon != null) {
                mLocationIcon = locationIcon;
            }

            atts.recycle();
        }

        if(mLocationIcon == null) {
            mLocationIcon = getResources().getDrawable(DEFAULT_LOCATION_ICON);
        }

        mCompassSensor = new CompassSensor(getContext(), this);
    }

    /**
     * Starts the compass sensors.
     *
     * Must be used along with the activity/fragment lifecycle events.
     */
    public void onStart() {
        mCompassSensor.start();
    }

    /**
     * Stops the compass sensors.
     *
     * Must be used along with the activity/fragment lifecycle events.
     */
    public void onStop() {
        mCompassSensor.stop();
    }

    @Override
    public void onSensorUpdate(Location myLocation, float bearingToLocation) {
        mMyLocation = myLocation;
        invalidate();
    }

    /**
     * Draws the map elements.
     *
     * @param canvas The canvas to draw on.
     * @param mapWidth The map width, in pixels
     * @param mapHeight The map height, in pixels
     */
    private void drawMapOverlay(Canvas canvas, int mapWidth, int mapHeight) {

        Paint mPaint = new Paint();
        mPaint.setColor(Color.argb(255, 31, 43, 76));
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(4);

        if(mMyLocation != null) {
            PointF myLocationXY = project(mMyLocation, mapWidth, mapHeight);
            canvas.drawCircle(myLocationXY.x, myLocationXY.y, 20, mPaint);

            PointF locationXY = project(mLocationToTrack, mapWidth, mapHeight);
            canvas.drawCircle(locationXY.x, locationXY.y, 10, mPaint);
        }
    }

    /**
     * Projects a location coordinate to a X,Y point.
     *
     * @param location The location to be projected
     * @param mapWidth The map width, in pixels
     * @param mapHeight The map height, in pixels
     *
     * @return The project X,Y point at cartesian coordinates.
     */
    private PointF project(Location location, int mapWidth, int mapHeight) {

        double siny = Math.sin(Math.toRadians(location.getLatitude()));

        // Truncating to 0.9999 effectively limits latitude to 89.189. This is
        // about a third of a tile past the edge of the world tile.
        siny = Math.min(Math.max(siny, -0.9999), 0.9999);

        return new PointF(
                (float) (mapWidth * (0.5 + location.getLongitude() / 360)),
                (float) (mapHeight * (0.5 - Math.log((1 + siny) / (1 - siny)) / (4 * Math.PI))));

/*
        double x = (location.getLongitude() + 180) * (mapWidth/360);
        double latitudeInRadians = location.getLatitude() * Math.PI/180;
        double mercN = Math.log(Math.tan((Math.PI/4) + (latitudeInRadians/2)));
        double y = (mapHeight/2) - (mapWidth*mercN / (2*Math.PI));
        return new PointF((float)x, (float)y);
*/
    }

    /**
     * Sets the map image resource
     *
     * @param mapBitmap Map bitmap.
     */
    public void setMapImage(Bitmap mapBitmap) {
        this.setImageDrawable(new MapDrawable(mapBitmap));
    }

    /**
     * Sets the location image, draw into the offline map.
     *
     * @param locationIcon Drawable representing the location which the user is heading to.
     */
    public void setLocationIcon(@NonNull Drawable locationIcon) {
        mLocationIcon = locationIcon;
    }

    /**
     * Sets the location image, draw into the offline map, using resources.
     *
     * @param locationIconRes Drawable representing the location which the user is heading to.
     */
    public void setLocationIcon(int locationIconRes) {
        Drawable locationIcon = getResources().getDrawable(locationIconRes);
        if (locationIcon != null) {
            mLocationIcon = locationIcon;
        }
    }

    /**
     * Sets the location of the place which the user is heading to.
     *
     * @param location The location of the place which the user is heading to.
     */
    public void setLocationToTrack(@NonNull Location location) {
        mLocationToTrack = location;
        mCompassSensor.setLocationToTrack(mLocationToTrack);
    }
}
