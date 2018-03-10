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
import android.view.ViewTreeObserver;

import com.otaliastudios.zoom.ZoomEngine;
import com.otaliastudios.zoom.ZoomImageView;

/**
 * Offline map of a location`s neighborhood.
 */
public class Map extends ZoomImageView implements CompassSensor.CompassSensorListener {

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
            drawMapOverlay(canvas, this.getBitmap().getWidth(), this.getBitmap().getHeight(), getEngine().getRealZoom());
        }
    }

    // Defaults
    private static final int DEFAULT_LOCATION_ICON = R.drawable.default_location_icon;
    private static final int DEFAULT_LOCATION_ICON_SIZE = 60;
    private static final int DEFAULT_MY_LOCATION_ICON_SIZE = 50;
    private static final int DEFAULT_ANGLE_ANIMATION_TIME = 200;

    /**
     * Timeout to consider a good location (3 packages lost is bad)
     */
    private static final int LOCATION_TIMEOUT = 30000;

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

    /**
     * Current location accuracy.
     */
    private float mCurrentAccuracy = 0;

    // Drawing utils
    private Bitmap mLocationBitmap;
    private Bitmap mMyLocationOnlineBitmap;
    private Bitmap mMyLocationOfflineBitmap;
    private Bitmap mMyLocationBearingBitmap;
    private RectF mLocationRect;
    private RectF mMyLocationRect;
    private RectF mMyLocationBearingRect;
    private Paint mAccuracyRadiusContour;
    private Paint mAccuracyRadiusFill;
    private Matrix mLocationMatrix;
    private ValueAnimator mCurrentSensorAnimation;
    private ValueAnimator mCurrentAccuracyAnimation;
    private float mMyLocationIconSize;

    /**
     * Constructor that is called when inflating a view from XML. This is called
     * when a view is being constructed from an XML file, supplying attributes
     * that were specified in the XML file. This version uses a default style of
     * 0, so the only attribute values applied are those in the Context's Theme
     * and the given AttributeSet.
     *
     * <p>
     * The method onFinishInflate() will be called after all children have been
     * added.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
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

        mMyLocationIconSize = DEFAULT_MY_LOCATION_ICON_SIZE * getResources().getDisplayMetrics().density;

        mMyLocationOnlineBitmap = ((BitmapDrawable)getResources()
                .getDrawable(R.drawable.position_online)).getBitmap();
        mMyLocationOfflineBitmap = ((BitmapDrawable)getResources()
                .getDrawable(R.drawable.position_offline)).getBitmap();
        mMyLocationBearingBitmap = ((BitmapDrawable)getResources()
                .getDrawable(R.drawable.position_bearing)).getBitmap();

        mLocationRect = new RectF();
        mMyLocationRect = new RectF();
        mMyLocationBearingRect = new RectF();
        mLocationMatrix = new Matrix();

        mAccuracyRadiusContour = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAccuracyRadiusContour.setColor(0x110000FF);
        mAccuracyRadiusContour.setStyle(Paint.Style.FILL);

        mAccuracyRadiusFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAccuracyRadiusFill.setColor(0xFF0000FF);
        mAccuracyRadiusFill.setStyle(Paint.Style.STROKE);

        getEngine().setMaxZoom(5f, ZoomEngine.TYPE_REAL_ZOOM);
        getEngine().setMinZoom(1f, ZoomEngine.TYPE_REAL_ZOOM);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(mOfflineMap != null && mOfflineMap.getMapBitmap() != null) {
                    float minZoom = Math.min(
                            (float) getMeasuredWidth() / mOfflineMap.getMapBitmap().getWidth(),
                            (float) getMeasuredHeight() / mOfflineMap.getMapBitmap().getHeight());
                    getEngine().setMinZoom(minZoom, ZoomEngine.TYPE_REAL_ZOOM);
                    getEngine().zoomTo(getEngine().getZoom() / getEngine().getRealZoom(), true);
                }
            }
        });
    }

    @Override
    public void onTrackingNewLocation(Location location) {
    }

    @Override
    public void onSensorUpdate(Location myLocation, float bearingToLocation, float azimuth, CompassSensor.Accuracy accuracy) {
        mMyLocation = myLocation;

        // Discard bad accuracies
        if(accuracy == CompassSensor.Accuracy.UNRELIABLE || accuracy == CompassSensor.Accuracy.LOW) {
            return;
        }

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
        if(mCurrentSensorAnimation != null) {
            mCurrentSensorAnimation.cancel();
        }
        // Animate the sensor change, interpolating the difference
        mCurrentSensorAnimation = ValueAnimator.ofFloat(oldAzimuth, newAzimuth);
        mCurrentSensorAnimation.setDuration(DEFAULT_ANGLE_ANIMATION_TIME);
        mCurrentSensorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                mAzimuth = (float) animation.getAnimatedValue() % 360f;
                invalidate();
            }
        });
        mCurrentSensorAnimation.start();

        float oldAccuracy = mCurrentAccuracy;
        float newAccuracy = mMyLocation.getAccuracy();

        // Cancels previous animation
        if(mCurrentAccuracyAnimation != null) {
            mCurrentAccuracyAnimation.cancel();
        }
        // Animate accuracy change
        mCurrentAccuracyAnimation = ValueAnimator.ofFloat(oldAccuracy, newAccuracy);
        mCurrentAccuracyAnimation.setDuration(DEFAULT_ANGLE_ANIMATION_TIME);
        mCurrentAccuracyAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurrentAccuracy = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mCurrentAccuracyAnimation.start();
    }

    /**
     * Draws the map elements.
     *
     * @param canvas The canvas to draw on
     * @param width The map width, in pixels
     * @param height The map height, in pixels
     * @param zoomScale The current map zoom scale
     */
    private void drawMapOverlay(Canvas canvas, int width, int height, float zoomScale) {

        Paint mPaint = new Paint();
        mPaint.setColor(Color.argb(255, 31, 43, 76));
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(4);

        // Location to track is always the center of this map
        mLocationRect.set(0, 0, mLocationBitmap.getWidth() / zoomScale, mLocationBitmap.getHeight() / zoomScale);
        mLocationRect.offsetTo(
                width/2f - mLocationRect.width()/2,
                height/2f - mLocationRect.height());
        canvas.drawBitmap(mLocationBitmap, null, mLocationRect, mPaint);

        if(mMyLocation != null) {

            PointF myLocationXY = mOfflineMap.projectToPixel(mMyLocation);

            // Accuracy radius for my location
            float radius = mOfflineMap.projectDistanceFromCenter(mCurrentAccuracy);
            if (radius > mMyLocationRect.width()/4) {
                canvas.drawCircle(myLocationXY.x, myLocationXY.y, radius, mAccuracyRadiusFill);
                canvas.drawCircle(myLocationXY.x, myLocationXY.y, radius, mAccuracyRadiusContour);
            }

            Bitmap myLocationBitmap;
            //if(System.currentTimeMillis() - mMyLocation.getTime() > LOCATION_TIMEOUT) {
            if(mCurrentAccuracy > 100) {
                myLocationBitmap = mMyLocationOfflineBitmap;
            } else {
                myLocationBitmap = mMyLocationOnlineBitmap;

                // Draw the user`s view horizon
                float size = mMyLocationIconSize*2 / zoomScale;
                mMyLocationBearingRect.set(0, 0, size, size);
                float widthScale = mMyLocationBearingRect.width() / mMyLocationBearingBitmap.getWidth();
                float heightScale = mMyLocationBearingRect.height() / mMyLocationBearingBitmap.getHeight();
                mLocationMatrix.reset();
                mLocationMatrix.postScale(widthScale, heightScale);
                mLocationMatrix.postTranslate(-mMyLocationBearingRect.width() / 2, -mMyLocationBearingRect.height() / 2);
                mLocationMatrix.postRotate(mAzimuth);
                mLocationMatrix.postTranslate(myLocationXY.x, myLocationXY.y);
                canvas.drawBitmap(mMyLocationBearingBitmap, mLocationMatrix, mPaint);
            }

            // Pointer (blue dot) of my current location
            float size = mMyLocationIconSize / zoomScale;
            mMyLocationRect.set(0, 0, size, size);
            mMyLocationRect.offsetTo(
                    myLocationXY.x - mMyLocationRect.width()/2f,
                    myLocationXY.y - mMyLocationRect.width()/2f);
            canvas.drawBitmap(myLocationBitmap, null, mMyLocationRect, mPaint);
        }
    }

    /**
     * Sets the offline map to be used by this view.
     *
     * @param offlineMap The offline map data.
     */
    public void setOfflineMap(OfflineMap offlineMap) {
        if(offlineMap != null) {
            mOfflineMap = offlineMap;
            this.setImageDrawable(new MapDrawable(offlineMap.getMapBitmap()));
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
