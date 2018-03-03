package com.arecmetafora.getmethere;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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
public class Compass extends View implements CompassSensor.Callback {

    // Defaults (units in DP)
    private static final int DEFAULT_ARC_COLOR = Color.argb(255, 31, 43, 76);
    private static final int DEFAULT_ARC_WIDTH = 16;
    private static final int DEFAULT_TEXT_SIZE = 30;
    private static final int DEFAULT_TEXT_COLOR = DEFAULT_ARC_COLOR;
    private static final int DEFAULT_PADDING = 40;
    private static final int DEFAULT_LOCATION_ICON = R.drawable.default_location_icon;
    private static final int DEFAULT_LOCATION_ICON_SIZE = 80;
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
     * Drawable representing the location which this widget is pointing at.
     */
    private Drawable mLocationIcon;

    /**
     * Drawable representing the compass arrow (always point ahead)
     */
    private Drawable mPointer;

    /**
     * The location of the place where this widget is pointing at.
     */
    private Location mLocation;

    /**
     * Bearing from my actual location to the desired location, in radians.
     */
    private float mLocationBearing = Integer.MIN_VALUE;

    /**
     * Distance between current user location and the tracked location.
     */
    private float mDistanceToLocation;

    private CompassSensor mCompassSensor;

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

        if (attrs != null) {
            TypedArray atts = context.obtainStyledAttributes(attrs,
                    R.styleable.Compass, 0, 0);

            mArcColor = atts.getColor(R.styleable.Compass_arcColor, mArcColor);
            mArcWidth = (int) atts.getDimension(R.styleable.Compass_arcWidth, mArcWidth);
            mTextSize = (int) atts.getDimension(R.styleable.Compass_textSize, mTextSize);
            mTextColor = atts.getColor(R.styleable.Compass_textColor, mTextColor);

            Drawable locationIcon = atts.getDrawable(R.styleable.Compass_locationIcon);
            if (locationIcon != null) {
                mLocationIcon = locationIcon;
            }
            Drawable pointer = atts.getDrawable(R.styleable.Compass_pointer);
            if (pointer != null) {
                mPointer = pointer;
            }

            atts.recycle();
        }

        if(mLocationIcon == null) {
            mLocationIcon = getResources().getDrawable(DEFAULT_LOCATION_ICON);
        }
        if(mPointer == null) {
            mPointer = getResources().getDrawable(DEFAULT_POINTER);
        }

        mPointerMargin = (int) (DEFAULT_POINTER_MARGIN * density);

        int padding = (int) (DEFAULT_PADDING * density);
        setPadding(padding, padding, padding, padding);

        int locationIconSize = (int) (DEFAULT_LOCATION_ICON_SIZE * density);
        mPointerBitmap = ((BitmapDrawable) mPointer).getBitmap();
        mLocationBitmap = Bitmap.createScaledBitmap(((BitmapDrawable) mLocationIcon).getBitmap(),
                locationIconSize, locationIconSize, true);

        mArcRect = new RectF();
        mTextRect = new Rect();
        mPointerRect = new RectF();
        mLocationRect = new RectF();

        initArcPaint();
        initTextPaint();

        mCompassSensor = new CompassSensor(getContext(), this);
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
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        onStop();
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

        // Draw the compass arc
        canvas.drawArc(mArcRect, 0, 360, false, mArcPaint);

        // Draw the compass pointer (arrow)
        canvas.drawBitmap(mPointerBitmap, null, mPointerRect, mImagePaint);

        if(mLocation != null && mLocationBearing != Integer.MIN_VALUE) {
            // Draw the location marker along the compass arc boundaries

            double locationX = mArcRect.centerX() - mArcRadius * Math.sin(mLocationBearing);
            double locationY = mArcRect.centerY() - mArcRadius * Math.cos(mLocationBearing);

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
                distanceStr = mNumberFormatter.format(mDistanceToLocation) + " km";
            }
            mTextPaint.getTextBounds(distanceStr, 0, distanceStr.length(), mTextRect);
            float xPos = canvas.getWidth() / 2 - mTextRect.width() / 2;
            float yPos = mArcRect.centerY() + 2 * (mArcRect.bottom - mArcRect.centerY()) / 3 - mTextPaint.descent() / 2;
            canvas.drawText(distanceStr, xPos, yPos, mTextPaint);
        }
    }

    @Override
    public void onSensorUpdate(float angle, float distance) {
        float newBearing = (float)Math.toRadians(angle);
        mDistanceToLocation = distance;

        // Cancels previous animation
        if(mCurrentAnimation != null) {
            mCurrentAnimation.cancel();
        }

        // Animate the sensor change, interpolating the difference
        mCurrentAnimation = ValueAnimator.ofFloat(mLocationBearing, newBearing);
        mCurrentAnimation.setDuration(DEFAULT_ANGLE_ANIMATION_TIME);
        mCurrentAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                mLocationBearing = (float) animation.getAnimatedValue();
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
        mLocationIcon = locationIcon;
    }

    /**
     * Sets the location image, draw along the compass arc, using the resources.
     *
     * @param locationIconRes Drawable resource representing the location which this widget is pointing at.
     */
    public void setLocationIcon(int locationIconRes) {
        Drawable locationIcon = getResources().getDrawable(locationIconRes);
        if (locationIcon != null) {
            mLocationIcon = locationIcon;
        }
    }

    /**
     * Sets the compass pointer (arrow).
     *
     * @param pointer Drawable representing the compass arrow (always point ahead, statically).
     */
    public void setPointer(@NonNull Drawable pointer) {
        mPointer = pointer;
    }

    /**
     * Sets the compass pointer (arrow), using the resources.
     *
     * @param pointerRes Drawable resource representing the compass arrow (always point ahead, statically).
     */
    public void setPointer(int pointerRes) {
        Drawable pointer = getResources().getDrawable(pointerRes);
        if (pointer != null) {
            mPointer = pointer;
        }
    }

    /**
     * Sets the location of the place which this compass is pointing at.
     *
     * @param location The location of the place where this widget is pointing at.
     */
    public void setLocationToTrack(@NonNull Location location) {
        mLocation = location;
        mCompassSensor.setLocationToTrack(mLocation);
    }
}
