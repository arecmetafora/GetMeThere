package com.arecmetafora.getmethere;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;

/**
 * Offline map of a location`s neighborhood.
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
    private static final int DEFAULT_LOCATION_ICON_SIZE = 40;
    private static final int DEFAULT_MY_LOCATION_ICON_SIZE = 40;
    private static final int DEFAULT_ANGLE_ANIMATION_TIME = 200;

    /**
     * Representation of a offline map.
     */
    private OfflineMap mOfflineMap;

    /**
     * The current user location.
     */
    private Location mMyLocation;

    /**
     * Azimuth to north pole.
     */
    private float mAzimuth;

    private CompassSensor mCompassSensor;

    private boolean mSensorStarted = false;
    private Bitmap mLocationBitmap;
    private RectF mLocationRect;
    private Bitmap mMyLocationOnlineBitmap;
    private Bitmap mMyLocationOfflineBitmap;
    private Bitmap mMyLocationBearingBitmap;
    private RectF mMyLocationRect;
    private RectF mMyLocationBearingRect;
    private Matrix mLocationMatrix;
    private ValueAnimator mCurrentAnimation;

    public Map(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        Drawable myLocationIcon = null;

        if (attrs != null) {
            TypedArray atts = context.obtainStyledAttributes(attrs,
                    R.styleable.Map, 0, 0);

            myLocationIcon = atts.getDrawable(R.styleable.Map_locationIcon);

            atts.recycle();
        }

        if(myLocationIcon == null) {
            myLocationIcon = getResources().getDrawable(DEFAULT_LOCATION_ICON);
        }
        setLocationIcon(myLocationIcon);

        float iconSize = DEFAULT_MY_LOCATION_ICON_SIZE * getResources().getDisplayMetrics().density;

        mMyLocationOnlineBitmap = ((BitmapDrawable)getResources()
                .getDrawable(R.drawable.position_online)).getBitmap();
        mMyLocationOfflineBitmap = ((BitmapDrawable)getResources()
                .getDrawable(R.drawable.position_offline)).getBitmap();
        mMyLocationBearingBitmap = ((BitmapDrawable)getResources()
                .getDrawable(R.drawable.position_bearing)).getBitmap();

        mMyLocationRect = new RectF(0, 0, iconSize, iconSize);
        mMyLocationBearingRect = new RectF(0, 0, iconSize*2, iconSize*2);
        mLocationMatrix = new Matrix();

        mCompassSensor = new CompassSensor(getContext(), this);
    }

    /**
     * Starts the compass sensors.
     *
     * Must be used along with the activity/fragment lifecycle events.
     */
    public void onStart() {
        mSensorStarted = true;
        mCompassSensor.start();
    }

    /**
     * Stops the compass sensors.
     *
     * Must be used along with the activity/fragment lifecycle events.
     */
    public void onStop() {
        mSensorStarted = false;
        mCompassSensor.stop();
    }

    @Override
    public void onSensorUpdate(Location myLocation, float bearingToLocation, float azimuth) {
        mMyLocation = myLocation;
        mAzimuth = azimuth;

        float newAzimuth = azimuth;
        float oldAzimuth= mAzimuth;

        // Fix animation from last to first quadrant and vice versa
        if(oldAzimuth > 270 && newAzimuth < 90) {
            newAzimuth += 360;
        }
        if(newAzimuth > 270 && oldAzimuth < 90) {
            oldAzimuth += 360;
        }

        // Cancels previous animation
        if(mCurrentAnimation != null) {
            mCurrentAnimation.cancel();
        }

        // Animate the sensor change, interpolating the difference
        mCurrentAnimation = ValueAnimator.ofFloat(oldAzimuth, newAzimuth);
        mCurrentAnimation.setDuration(DEFAULT_ANGLE_ANIMATION_TIME);
        mCurrentAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                mAzimuth = (float) animation.getAnimatedValue() % 360f;
                invalidate();
            }
        });

        mCurrentAnimation.start();
    }

    /**
     * Draws the map elements.
     *
     * @param canvas The canvas to draw on.
     * @param width The map width, in pixels
     * @param height The map height, in pixels
     */
    private void drawMapOverlay(Canvas canvas, int width, int height) {

        Paint mPaint = new Paint();
        mPaint.setColor(Color.argb(255, 31, 43, 76));
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(4);

        if(mMyLocation != null) {

            PointF myLocationXY = mOfflineMap.projectToPixel(mMyLocation);

            // Pointer (blue dot) of my current location
            mMyLocationRect.offsetTo(
                    myLocationXY.x - mMyLocationRect.width()/2f,
                    myLocationXY.y - mMyLocationRect.width()/2f);
            canvas.drawBitmap(mMyLocationOnlineBitmap, null, mMyLocationRect, mPaint);

            // Draw the user`s view horizon
            float widthScale = mMyLocationBearingRect.width() / mMyLocationBearingBitmap.getWidth();
            float heightScale = mMyLocationBearingRect.height() / mMyLocationBearingBitmap.getHeight();
            mLocationMatrix.reset();
            mLocationMatrix.postScale(widthScale, heightScale);
            mLocationMatrix.postTranslate(-mMyLocationBearingRect.width()/2, -mMyLocationBearingRect.height()/2);
            mLocationMatrix.postRotate(mAzimuth);
            mLocationMatrix.postTranslate(myLocationXY.x, myLocationXY.y);
            canvas.drawBitmap(mMyLocationBearingBitmap, mLocationMatrix, mPaint);

            // Location to track is always the center of this map
            mLocationRect.offsetTo(
                    width/2f - mLocationRect.width()/2,
                    height/2f - mLocationRect.height());
            canvas.drawBitmap(mLocationBitmap, null, mLocationRect, mPaint);
        }
    }

    /**
     * Sets the offline map to be used by this view.
     *
     * @param offlineMap The offline map data.
     */
    public void setOfflineMap(OfflineMap offlineMap) {
        mOfflineMap = offlineMap;
        mCompassSensor.setLocationToTrack(offlineMap.getCenterGeoCoordinate());
        this.setImageDrawable(new MapDrawable(offlineMap.getMapBitmap()));

        if(mSensorStarted) {
            mCompassSensor.start();
        }
    }

    /**
     * Sets the location image, draw into the offline map.
     *
     * @param locationIcon Drawable representing the location which the user is heading to.
     */
    public void setLocationIcon(@NonNull Drawable locationIcon) {
        float density = getResources().getDisplayMetrics().density;
        int locationIconSize = (int) (DEFAULT_LOCATION_ICON_SIZE * density);
        mLocationBitmap = Bitmap.createScaledBitmap(((BitmapDrawable) locationIcon).getBitmap(),
                locationIconSize, locationIconSize, true);
        mLocationRect = new RectF(0, 0, mLocationBitmap.getWidth(), mLocationBitmap.getHeight());
        invalidate();
    }

    /**
     * Sets the location image, draw into the offline map, using resources.
     *
     * @param locationIconRes Drawable representing the location which the user is heading to.
     */
    public void setLocationIcon(int locationIconRes) {
        setLocationIcon(getResources().getDrawable(locationIconRes));
    }
}
