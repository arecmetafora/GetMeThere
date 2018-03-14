package com.arecmetafora.getmethere;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.text.DecimalFormat;

/**
 * Compass which points to a specific location.
 */
public class Compass extends View implements CompassSensor.CompassSensorListener {

    // Defaults (units in DP)
    private static final int DEFAULT_ARC_COLOR = Color.argb(255, 31, 43, 76);
    private static final int DEFAULT_ARC_WIDTH = 16;
    private static final int DEFAULT_TEXT_SIZE = 30;
    private static final int DEFAULT_TEXT_COLOR = Color.argb(255,244, 67, 54);
    private static final int DEFAULT_PADDING = 40;
    private static final int DEFAULT_LOCATION_ICON = R.drawable.default_location_icon;
    private static final int DEFAULT_LOCATION_ICON_SIZE = 60;
    private static final int DEFAULT_POINTER = R.drawable.default_pointer;
    private static final int DEFAULT_POINTER_MARGIN = 20;
    private static final int DEFAULT_ANGLE_ANIMATION_TIME = 400;

    /**
     * Width of the location`s icon path, while the user moves his devices around.
     */
    private float mArcWidth;

    /**
     * Color of the location`s icon path, while the user moves his devices around.
     */
    private int mArcColor;

    /**
     * Size of the distance text.
     */
    private float mTextSize;

    /**
     * Color of the distance text.
     */
    private int mTextColor;

    /**
     * Drawable representing the compass arrow (always point ahead)
     */
    private Drawable mPointer;

    /**
     * The location of the place where this widget is pointing at.
     */
    private Location mLocation;

    /**
     * Bearing from my actual location to the desired location, in degrees.
     */
    private float mLocationBearing = Integer.MIN_VALUE;

    /**
     * Distance between current user location and the tracked location.
     */
    private float mDistanceToLocation;

    // Utilities used to draw the compass
    private int mPointerMargin;
    private int mArcRadius;
    private RectF mArcRect;
    private Paint mArcPaint;
    private Rect mTextRect;
    private Paint mTextPaint;
    private Paint mImagePaint = new Paint(Paint.DITHER_FLAG);
    private Bitmap mPointerBitmap;
    private RectF mPointerRect;
    private Matrix mPointerMatrix;
    private Bitmap mLocationBitmap;
    private RectF mLocationRect;
    private DecimalFormat mNumberFormatter = new DecimalFormat(".##");
    private ValueAnimator mCurrentAnimation;

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
    public Compass(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        float density = getResources().getDisplayMetrics().density;

        mArcWidth = DEFAULT_ARC_WIDTH * density;
        mArcColor = DEFAULT_ARC_COLOR;
        mTextSize = DEFAULT_TEXT_SIZE * density;
        mTextColor = DEFAULT_TEXT_COLOR;

        Drawable myLocationIcon = null;

        if (attrs != null) {
            TypedArray atts = context.obtainStyledAttributes(attrs,
                    R.styleable.Compass, 0, 0);

            mArcColor = atts.getColor(R.styleable.Compass_arcColor, mArcColor);
            mArcWidth = (int) atts.getDimension(R.styleable.Compass_arcWidth, mArcWidth);
            mTextSize = (int) atts.getDimension(R.styleable.Compass_textSize, mTextSize);
            mTextColor = atts.getColor(R.styleable.Compass_textColor, mTextColor);

            myLocationIcon = atts.getDrawable(R.styleable.Compass_locationIcon);

            Drawable pointer = atts.getDrawable(R.styleable.Compass_pointer);
            if (pointer != null) {
                mPointer = pointer;
            }

            atts.recycle();
        }

        if(myLocationIcon == null) {
            myLocationIcon = getResources().getDrawable(DEFAULT_LOCATION_ICON);
        }
        setLocationIcon(myLocationIcon);

        if(mPointer == null) {
            mPointer = getResources().getDrawable(DEFAULT_POINTER);
        }
        setPointer(mPointer);

        mPointerMargin = (int) (DEFAULT_POINTER_MARGIN * density);
        int padding = (int) (DEFAULT_PADDING * density);
        setPadding(padding, padding, padding, padding);

        mArcRect = new RectF();
        mTextRect = new Rect();
        mPointerRect = new RectF();
        mPointerMatrix = new Matrix();
        mLocationRect = new RectF();

        initArcPaint();
        initTextPaint();
    }

    /**
     * Initializes the arc paint brush.
     */
    private void initArcPaint() {
        mArcPaint = new Paint();
        mArcPaint.setColor(mArcColor);
        mArcPaint.setAntiAlias(true);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mArcWidth);
    }

    /**
     * Initializes the text paint brush.
     */
    private void initTextPaint() {
        mTextPaint = new Paint();
        mTextPaint.setColor(mTextColor);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextSize(mTextSize);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        float centerX = width / 2f;
        float centerY = height / 2f;

        // Measure the arc boundaries
        int arcDiameter = Math.min(width - getPaddingLeft() - getPaddingRight(),
                height - getPaddingTop() - getPaddingBottom());
        mArcRadius = arcDiameter / 2;
        float arcLeft = centerX - (arcDiameter / 2f);
        float arcTop = centerY - (arcDiameter / 2f);
        mArcRect.set(arcLeft, arcTop, arcLeft + arcDiameter, arcTop + arcDiameter);

        // Measure the pointer boundaries
        float pointerMargin = mArcWidth + mPointerMargin;
        mPointerRect.set(mArcRect.left + pointerMargin, mArcRect.top + pointerMargin,
                mArcRect.right - pointerMargin, mArcRect.bottom - pointerMargin);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(mLocation != null && mLocationBearing != Integer.MIN_VALUE) {

            // Draw the compass arc
            canvas.drawArc(mArcRect, 0, 360, false, mArcPaint);

            // Draw the compass pointer (arrow)
            float widthScale = mPointerRect.width() / mPointerBitmap.getWidth();
            float heightScale = mPointerRect.height() / mPointerBitmap.getHeight();
            mPointerMatrix.reset();
            mPointerMatrix.postScale(widthScale, heightScale);
            mPointerMatrix.postTranslate(-mPointerRect.width() / 2, -mPointerRect.height() / 2);
            mPointerMatrix.postRotate(-mLocationBearing);
            mPointerMatrix.postTranslate(mPointerRect.centerX(), mPointerRect.centerY());
            canvas.drawBitmap(mPointerBitmap, mPointerMatrix, mImagePaint);

            // Draw the location marker along the compass arc boundaries

            double bearingInRadians = Math.toRadians(mLocationBearing);
            double locationX = mArcRect.centerX() - mArcRadius * Math.sin(bearingInRadians);
            double locationY = mArcRect.centerY() - mArcRadius * Math.cos(bearingInRadians);

            float locationLeft = (float) locationX - mLocationBitmap.getWidth() / 2;
            float locationTop = (float) locationY - mLocationBitmap.getHeight() / 2;
            mLocationRect.set(locationLeft, locationTop, locationLeft + mLocationBitmap.getWidth(),
                    locationTop + mLocationBitmap.getHeight());

            canvas.drawBitmap(mLocationBitmap, null, mLocationRect, mImagePaint);

            // Draw the distance
            String distanceStr;
            if(mDistanceToLocation < 1000) {
                distanceStr = (int) mDistanceToLocation + " m";
            } else {
                distanceStr = mNumberFormatter.format(mDistanceToLocation / 1000) + " km";
            }
            mTextPaint.getTextBounds(distanceStr, 0, distanceStr.length(), mTextRect);
            float xPos = canvas.getWidth() / 2 - mTextRect.width() / 2;
            float yPos = mArcRect.centerY() + 2 * (mArcRect.bottom - mArcRect.centerY()) / 3 - mTextPaint.descent() / 2;
            canvas.drawText(distanceStr, xPos, yPos, mTextPaint);
        }
    }

    @Override
    public void onTrackingNewLocation(Location location) {
        mLocation = location;
    }

    @Override
    public void onSensorUpdate(Location myLocation, float bearingToLocation, float azimuth, CompassSensor.Accuracy accuracy) {
        mDistanceToLocation = myLocation.distanceTo(mLocation);

        // Discard bad accuracies
        if(accuracy == CompassSensor.Accuracy.UNRELIABLE || accuracy == CompassSensor.Accuracy.LOW) {
            return;
        }

        // First update
        if(mLocationBearing == Integer.MIN_VALUE) {
            mLocationBearing = bearingToLocation;
            invalidate();
            return;
        }

        float newBearing = bearingToLocation;
        float oldBearing = mLocationBearing;

        // Fix animation from last to first quadrant and vice versa
        if(oldBearing > 270 && newBearing < 90) {
            newBearing += 360;
        }
        if(newBearing > 270 && oldBearing < 90) {
            oldBearing += 360;
        }

        // Cancels previous animation
        if(mCurrentAnimation != null) {
            mCurrentAnimation.cancel();
        }

        // Animate the sensor change, interpolating the difference
        mCurrentAnimation = ValueAnimator.ofFloat(oldBearing, newBearing);
        mCurrentAnimation.setDuration(DEFAULT_ANGLE_ANIMATION_TIME);
        mCurrentAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                mLocationBearing = (float) animation.getAnimatedValue() % 360f;
                invalidate();
            }
        });

        mCurrentAnimation.start();
    }

    /**
     * Sets the compass arc width.
     *
     * @param arcWidth Width of the location`s icon path, while the user moves his devices around.
     */
    public void setArcWidth(float arcWidth) {
        mArcWidth = arcWidth;
        initArcPaint();
    }

    /**
     * Sets the compass arc color.
     *
     * @param arcColor Color of the location`s icon path, while the user moves his devices around.
     */
    public void setArcColor(int arcColor) {
        mArcColor = arcColor;
        initArcPaint();
    }

    /**
     * Sets the size of the distance text.
     *
     * @param textSize Size of the distance text.
     */
    public void setTextSize(float textSize) {
        mTextSize = textSize;
        initTextPaint();
    }

    /**
     * Sets the color of the distance text.
     *
     * @param textColor Color of the distance text.
     */
    public void setTextColor(int textColor) {
        mTextColor = textColor;
        initTextPaint();
    }

    /**
     * Sets the location image, draw along the compass arc.
     *
     * @param locationIcon Drawable representing the location which this widget is pointing at.
     */
    public void setLocationIcon(@NonNull Drawable locationIcon) {
        float density = getResources().getDisplayMetrics().density;
        int locationIconSize = (int) (DEFAULT_LOCATION_ICON_SIZE * density);
        mLocationBitmap = Bitmap.createScaledBitmap(((BitmapDrawable) locationIcon).getBitmap(),
                locationIconSize, locationIconSize, true);
        invalidate();
    }

    /**
     * Sets the location image, draw along the compass arc, using the resources.
     *
     * @param locationIconRes Drawable resource representing the location which this widget is pointing at.
     */
    public void setLocationIcon(int locationIconRes) {
        setLocationIcon(getResources().getDrawable(locationIconRes));
    }

    /**
     * Sets the compass pointer (arrow).
     *
     * @param pointer Drawable representing the compass arrow (always point ahead, statically).
     */
    public void setPointer(@NonNull Drawable pointer) {
        mPointer = pointer;
        mPointerBitmap = ((BitmapDrawable) mPointer).getBitmap();
        invalidate();
    }

    /**
     * Sets the compass pointer (arrow), using the resources.
     *
     * @param pointerRes Drawable resource representing the compass arrow (always point ahead, statically).
     */
    public void setPointer(int pointerRes) {
        setPointer(getResources().getDrawable(pointerRes));
    }
}
