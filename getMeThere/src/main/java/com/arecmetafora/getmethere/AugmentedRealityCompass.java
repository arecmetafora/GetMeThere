package com.arecmetafora.getmethere;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.location.Location;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Augmented reality compass which points to a specific location.
 */
public class AugmentedRealityCompass extends FrameLayout
        implements SurfaceHolder.Callback, CompassSensor.RotationCallback {

    // Defaults (units in DP)
    private static final int DEFAULT_TEXT_SIZE = 30;
    private static final int DEFAULT_TEXT_COLOR = Color.argb(255,244, 67, 54);
    private static final int DEFAULT_LOCATION_ICON = R.drawable.default_location_icon;
    private static final int DEFAULT_LOCATION_ICON_SIZE = 60;
    private static final int DEFAULT_POINTER = R.drawable.default_navigation;
    private static final int DEFAULT_POINTER_MARGIN = 5;

    /**
     * Size of the distance text.
     */
    private float mTextSize;

    /**
     * Color of the distance text.
     */
    private int mTextColor;

    /**
     * The location of the place where this widget is pointing at.
     */
    private Location mLocation;

    /**
     * The current user location.
     */
    private Location mMyLocation;

    private float mPointerMargin;
    private Rect mTextRect;
    private Paint mTextPaint;
    private Paint mImagePaint = new Paint(Paint.DITHER_FLAG);
    private Bitmap mPointerBitmap;
    private Bitmap mLocationBitmap;
    private Bitmap mTurnBitmap;
    private android.graphics.Matrix mPointerMatrix;
    private RectF mPointerRect;
    private RectF mLocationRect;
    private RectF mTurnRect;
    private Rect mCanvasRect;
    private DecimalFormat mNumberFormatter = new DecimalFormat(".##");

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private LocationOverlay mLocationOverlayView;

    private final float[] mProjectionMatrix = new float[16];
    private final float[] mRotatedProjectionMatrix = new float[16];
    private float[] mCameraCoordinateVector = new float[4];

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
    public AugmentedRealityCompass(Context context, AttributeSet attrs) {
        super(context, attrs);

        float density = getResources().getDisplayMetrics().density;

        mPointerMargin = DEFAULT_POINTER_MARGIN * density;
        mTextSize = DEFAULT_TEXT_SIZE * density;
        mTextColor = DEFAULT_TEXT_COLOR;

        Drawable myLocationIcon = null;
        Drawable pointerIcon = null;

        if (attrs != null) {
            TypedArray atts = context.obtainStyledAttributes(attrs,
                    R.styleable.AugmentedRealityCompass, 0, 0);

            mTextSize = (int) atts.getDimension(R.styleable.Compass_textSize, mTextSize);
            mTextColor = atts.getColor(R.styleable.Compass_textColor, mTextColor);

            myLocationIcon = atts.getDrawable(R.styleable.Compass_locationIcon);
            pointerIcon = atts.getDrawable(R.styleable.Compass_pointer);

            atts.recycle();
        }

        mTextRect = new Rect();
        mPointerMatrix = new android.graphics.Matrix();
        mLocationRect = new RectF();
        mPointerRect = new RectF();
        mTurnRect = new RectF(0, 0, DEFAULT_LOCATION_ICON_SIZE*density, DEFAULT_LOCATION_ICON_SIZE*density);
        mCanvasRect = new Rect();

        if(myLocationIcon == null) {
            myLocationIcon = getResources().getDrawable(DEFAULT_LOCATION_ICON);
        }
        setLocationIcon(myLocationIcon);

        if(pointerIcon == null) {
            pointerIcon = getResources().getDrawable(DEFAULT_POINTER);
        }
        setPointer(pointerIcon);

        int iconSize = (int) (DEFAULT_LOCATION_ICON_SIZE * density);
        mTurnBitmap = Bitmap.createScaledBitmap(
                ((BitmapDrawable) getResources().getDrawable(R.drawable.default_turn_phone)).getBitmap(),
                iconSize, iconSize, true);

        initTextPaint();

        mCamera = Camera.open();

        Camera.Parameters params = mCamera.getParameters();
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(params);
        }

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        SurfaceView cameraView = new SurfaceView(context);
        mLocationOverlayView = new LocationOverlay(context);
        this.addView(cameraView, layoutParams);
        this.addView(mLocationOverlayView, layoutParams);

        mHolder = cameraView.getHolder();
        mHolder.addCallback(this);
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
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException ignored) {
            // TODO: Manage exceptions when starting camera preview
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mPointerBitmap.recycle();
        mLocationBitmap.recycle();
        mTurnBitmap.recycle();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        if (mHolder.getSurface() == null){
            return;
        }

        // Stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception ignored){
        }

        // Set preview size and make any resize, rotate or reformatting changes
        Camera.Parameters params = mCamera.getParameters();

        int orientation = getCameraOrientation();
        mCamera.setDisplayOrientation(orientation);
        mCamera.getParameters().setRotation(orientation);

        Camera.Size previewSize = getOptimalPreviewSize(params.getSupportedPreviewSizes(), w, h);
        params.setPreviewSize(previewSize.width, previewSize.height);
        mCamera.setParameters(params);

        // Start preview with new settings
        surfaceCreated(mHolder);
    }

    /**
     * Gets the device orientation.
     *
     * @return The current device orientation, in degrees.
     */
    private int getCameraOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);

        int rotation = Surface.ROTATION_0;
        WindowManager window = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if(window != null) {
            rotation = window.getDefaultDisplay().getRotation();
        }

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int orientation;
        if(info.facing==Camera.CameraInfo.CAMERA_FACING_FRONT){
            orientation = (info.orientation + degrees) % 360;
            orientation =  (360 - orientation) % 360;
        } else {
            orientation = (info.orientation -degrees + 360) % 360;
        }

        return orientation;
    }

    /**
     * Gets the optional preview size of the camera, based on the view`s size and the
     * available camera preview sizes.
     *
     * @param sizes The available camera preview sizes.
     * @param width The desired width.
     * @param height The desired height.
     *
     * @return The best size based on the camera capabilities.
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) width / height;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }

            if (Math.abs(size.height - height) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - height);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - height);
                }
            }
        }

        if(optimalSize == null) {
            optimalSize = sizes.get(0);
        }

        return optimalSize;
    }

    @Override
    public void onTrackingNewLocation(Location location) {
        mLocation = location;
    }

    @Override
    public void onNewLocation(Location myLocation) {
        mMyLocation = myLocation;
        mLocation.setAltitude(myLocation.getAltitude());
    }

    @Override
    public void onNewRotation(float[] rotationMatrix) {

        if(mMyLocation != null) {
            float ratio = (float) getWidth() / getHeight();
            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 0.5f, 2000);
            Matrix.multiplyMM(mRotatedProjectionMatrix, 0, mProjectionMatrix, 0, rotationMatrix, 0);

            float[] myLocationInECEF = ECEF.fromWSG84(mMyLocation);
            float[] locationInECEF = ECEF.fromWSG84(mLocation);
            float[] pointInENU = ECEF.toENU(mMyLocation, myLocationInECEF, locationInECEF);

            Matrix.multiplyMV(mCameraCoordinateVector, 0, mRotatedProjectionMatrix, 0, pointInENU, 0);

            mLocationOverlayView.invalidate();
        }
    }

    /**
     * Class to draw the camera overlay (compass indicator and current location).
     */
    private class LocationOverlay extends View {

        /**
         * Simple constructor to use when creating a view from code.
         *
         * @param context The Context the view is running in, through which it can
         *        access the current theme, resources, etc.
         */
        public LocationOverlay(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if(mLocation != null && mMyLocation != null) {

                int centerX = canvas.getWidth()/2;
                int centerY = canvas.getHeight()/2;

                // cameraCoordinateVector[2] is z, that always less than 0 to display on right position
                // if z > 0, the point will display on the opposite
                if (mCameraCoordinateVector[2] < 0) {
                    float x  = (0.5f + mCameraCoordinateVector[0]/mCameraCoordinateVector[3]) * canvas.getWidth();
                    float y = (0.5f - mCameraCoordinateVector[1]/mCameraCoordinateVector[3]) * canvas.getHeight();

                    // Check if location is inside the camera frame
                    if(x < -mLocationRect.width() || x > canvas.getWidth() + mLocationRect.width() ||
                            y < -mLocationRect.height() || y > canvas.getHeight() + mLocationRect.height()) {

                        canvas.getClipBounds(mCanvasRect);
                        PointF intersectionPoint = getIntersectionPoint(centerX, centerY, x, y, mCanvasRect);
                        if(intersectionPoint != null) {
                            float angle = (float) Math.toDegrees(Math.atan2(y - centerY, x - centerY)) + 90;

                            float widthScale = mPointerRect.width() / mPointerBitmap.getWidth();
                            float heightScale = mPointerRect.height() / mPointerBitmap.getHeight();
                            mPointerMatrix.reset();
                            mPointerMatrix.postScale(widthScale, heightScale);
                            mPointerMatrix.postTranslate(-mPointerRect.width() / 2, -mPointerRect.height() / 2);
                            mPointerMatrix.postRotate(angle);
                            mPointerMatrix.postTranslate(intersectionPoint.x, intersectionPoint.y);
                            canvas.drawBitmap(mPointerBitmap, mPointerMatrix, mImagePaint);
                        }

                    } else {
                        // Draw location icon
                        mLocationRect.offsetTo(x - mLocationRect.width()/2, y - mLocationRect.height());
                        canvas.drawBitmap(mLocationBitmap, null, mLocationRect, mImagePaint);

                        // Draw distance to location
                        float distanceToLocation = mMyLocation.distanceTo(mLocation);
                        String distanceStr;
                        if(distanceToLocation < 1000) {
                            distanceStr = (int) distanceToLocation + " m";
                        } else {
                            distanceStr = mNumberFormatter.format(distanceToLocation / 1000) + " km";
                        }
                        mTextPaint.getTextBounds(distanceStr, 0, distanceStr.length(), mTextRect);
                        float xPos = x - mTextRect.width() / 2;
                        float yPos = mLocationRect.top - mTextRect.height();
                        canvas.drawText(distanceStr, xPos, yPos, mTextPaint);
                    }
                } else {
                    // Draw rotation device icon
                    mTurnRect.offsetTo( centerX - mTurnRect.width()/2, centerY - mTurnRect.height());
                    canvas.drawBitmap(mTurnBitmap, null, mTurnRect, mImagePaint);
                }
            }
        }

        /**
         * Calculates the intersection between a line segment and a rectangle.
         *
         * @param x1 The starting x position.
         * @param y1 The starting y position.
         * @param x2 The ending x position.
         * @param y2 The ending y position.
         * @param rect The rectangle positions.
         *
         * @return The first point of intersection between the line and the rectangle.
         */
        private PointF getIntersectionPoint(float x1, float y1, float x2, float y2, Rect rect) {

            PointF intersectionPoint;

            if(y2 < y1) {
                // Top line
                intersectionPoint = getIntersectionPoint(x1, y1, x2, y2,
                        rect.left, rect.top, rect.right, rect.top);

                if (intersectionPoint != null && intersectionPoint.x >= 0 && intersectionPoint.x <= rect.right) {
                    intersectionPoint.y += (mPointerMargin + mPointerRect.height());
                    return intersectionPoint;
                }
            } else {
                // Bottom line
                intersectionPoint = getIntersectionPoint(x1, y1, x2, y2,
                        rect.left, rect.bottom, rect.right, rect.bottom);

                if (intersectionPoint != null && intersectionPoint.x >= 0 && intersectionPoint.x <= rect.right) {
                    intersectionPoint.y -= (mPointerMargin + mPointerRect.height());
                    return intersectionPoint;
                }
            }

            if(x2 < x1) {
                // Left side
                intersectionPoint = getIntersectionPoint(x1, y1, x2, y2,
                        rect.left, rect.top, rect.left, rect.bottom);

                if (intersectionPoint != null && intersectionPoint.y >= 0 && intersectionPoint.y <= rect.bottom) {
                    intersectionPoint.x += (mPointerMargin + mPointerRect.width());
                    return intersectionPoint;
                }
            } else {
                // Right side
                intersectionPoint = getIntersectionPoint(x1, y1, x2, y2,
                        rect.right, rect.top, rect.right, rect.bottom);

                if (intersectionPoint != null && intersectionPoint.y >= 0 && intersectionPoint.y <= rect.bottom) {
                    intersectionPoint.x -= (mPointerMargin + mPointerRect.width());
                    return intersectionPoint;
                }
            }

            return null;
        }

        /**
         * Calculates the intersection between two lines.
         *
         * @param x1 The starting x position of the first line.
         * @param y1 The starting y position of the first line.
         * @param x2 The ending x position of the first line.
         * @param y2 The ending y position of the first line.
         * @param x3 The starting x position of the second line.
         * @param y3 The starting y position of the second line.
         * @param x4 The ending x position of the second line.
         * @param y4 The ending y position of the second line.
         *
         * @return The point of intersection between the two lines.
         */
        private PointF getIntersectionPoint(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {

            float d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
            if (d != 0) {
                float xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
                float yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
                return new PointF(xi, yi);
            }
            return null;
        }
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
        float iconSize = (DEFAULT_LOCATION_ICON_SIZE * density);
        int locationIconSize = (int) (DEFAULT_LOCATION_ICON_SIZE * density);
        mLocationBitmap = Bitmap.createScaledBitmap(((BitmapDrawable) locationIcon).getBitmap(),
                locationIconSize, locationIconSize, true);
        mLocationRect.set(0, 0, iconSize, iconSize);
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
        float density = getResources().getDisplayMetrics().density;
        float iconSize = (DEFAULT_LOCATION_ICON_SIZE * density);
        int pointerIconSize = (int) (DEFAULT_LOCATION_ICON_SIZE * density);
        mPointerBitmap = Bitmap.createScaledBitmap(((BitmapDrawable) pointer).getBitmap(),
                pointerIconSize, pointerIconSize, true);
        mPointerRect.set(0, 0, iconSize, iconSize);
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
