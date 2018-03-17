package com.arecmetafora.getmethere;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.LinkedList;
import java.util.List;

/**
 * Location compass sensor, using GPS and device sensors.
 */
public final class CompassSensor implements SensorEventListener, LifecycleObserver {

    /**
     * Base class for compass listeners.
     */
    public interface CompassSensorListener {

        /**
         * Callback trigger when the listener when the compass is targeting another location to be tracked.
         *
         * @param location The new location to be tracked.
         */
        void onTrackingNewLocation(Location location);
    }

    /**
     * Callback to receive location updates.
     */
    public interface LocationCallback extends CompassSensorListener {

        /**
         * Callback trigger when a new user`s location was detected.
         *
         * @param myLocation The current user location.
         */
        void onNewLocation(Location myLocation);
    }

    /**
     * Callback to receive azimuth and bearing updates.
     */
    public interface BearingCallback extends LocationCallback {

        /**
         * Callback trigger when the bearing between user`s location and the tracked location was changed.
         *
         * @param bearingToLocation The angle between user`s orientation and the tracked location.
         * @param azimuth Azimuth to north pole.
         */
        void onNewBearing(float bearingToLocation, float azimuth);
    }

    /**
     * Callback to receive device rotation updates.
     */
    public interface RotationCallback extends LocationCallback {

        /**
         * Callback trigger when the user rotated his device.
         *
         * @param rotationMatrix The rotation matrix.
         */
        void onNewRotation(float[] rotationMatrix);
    }

    /**
     * Minimum angle change to notify listeners.
     */
    private static final int MINIMUM_ANGLE_CHANGE = 5;

    /**
     * Interval between location captures.
     */
    private static final int LOCATION_CAPTURE_INTERVAL = 3000;

    private Context mContext;

    // GPS sensor
    private FusedLocationProviderClient mLocationProvider;
    private LocationRequest locationRequest;
    private Location mCurrentLocation;
    private Location mLocationToTrack;
    private int mMagneticFieldSensorAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;

    // Orientation sensors
    private SensorManager mSensorManager;
    private Sensor mGravityFieldSensor;
    private Sensor mAccelerometerSensor;
    private Sensor mMagneticFieldSensor;
    private Sensor mRotationVectorSensor;
    private boolean mHasGravitySensor;
    private boolean mHasAccelerometerSensor;
    private boolean mHasMagneticFieldSensor;
    private boolean mHasRotationVectorSensor;
    private final float[] mRotationMatrix = new float[9];
    private final float[] mRotationMatrixFromVector = new float[16];
    private final float[] mOrientationData = new float[3];
    private float[] mGravityData;
    private float[] mMagneticFieldData;
    private float[] mRotationVectorData;
    private float mLastCalculatedBearingToLocation = 0;

    private final Object mMonitor = new Object();

    // Listeners
    private List<LocationCallback> mLocationListeners;
    private List<BearingCallback> mBearingListeners;
    private List<RotationCallback> mRotationListeners;

    // GPS sensor callback
    private com.google.android.gms.location.LocationCallback mLocationCallback =
        new com.google.android.gms.location.LocationCallback() {
            public void onLocationResult(LocationResult result) {
                synchronized (mMonitor) {
                    mCurrentLocation = result.getLastLocation();
                    for(LocationCallback listener : mLocationListeners) {
                        listener.onNewLocation(mCurrentLocation);
                        onBearingSensorsChanged();
                        onRotationSensorChanged();
                    }
                }
            }
    };

    /**
     * Creates a location compass sensor, using GPS and device orientation sensors.
     *
     * @param context The application context.
     * @param lifecycleOwner Android life cycle controller.
     * @param locationToTrack The location which the user is heading to.
     */
    private CompassSensor(@NonNull Context context, @NonNull LifecycleOwner lifecycleOwner, Location locationToTrack) {
        mContext = context;
        mLocationToTrack = locationToTrack;

        mLocationListeners = new LinkedList<>();
        mBearingListeners = new LinkedList<>();
        mRotationListeners = new LinkedList<>();

        lifecycleOwner.getLifecycle().addObserver(this);

        mLocationProvider = LocationServices.getFusedLocationProviderClient(mContext);

        locationRequest = LocationRequest.create()
                .setInterval(LOCATION_CAPTURE_INTERVAL)
                .setFastestInterval(LOCATION_CAPTURE_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        mGravityFieldSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticFieldSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    /**
     * Creates a location compass sensor, using GPS and device orientation sensors.
     *
     * @param context The application context.
     * @param lifecycleOwner Android life cycle controller.
     */
    public static CompassSensor from(@NonNull Context context, @NonNull LifecycleOwner lifecycleOwner) {
        return new CompassSensor(context, lifecycleOwner, null);
    }

    /**
     * Binds a compass sensor receiver to this sensor.
     *
     * @param listener The compass sensor listener.
     * @return The same compass sensor instance.
     */
    public CompassSensor bindTo(CompassSensorListener listener) {
        if(listener instanceof LocationCallback) {
            mLocationListeners.add((LocationCallback) listener);
        }
        if(listener instanceof BearingCallback) {
            mBearingListeners.add((BearingCallback) listener);
        }
        if(listener instanceof RotationCallback) {
            mRotationListeners.add((RotationCallback) listener);
        }
        if(this.mLocationToTrack != null) {
            listener.onTrackingNewLocation(this.mLocationToTrack);
        }
        return this;
    }

    /**
     * Sets the location to track.
     *
     * @param locationToTrack The location which the angle with the device orientation will be calculated.
     * @return The same compass sensor instance.
     */
    public CompassSensor track(Location locationToTrack) {
        this.mLocationToTrack = locationToTrack;
        for(CompassSensorListener listener : mLocationListeners) {
            listener.onTrackingNewLocation(locationToTrack);
        }
        for(CompassSensorListener listener : mBearingListeners) {
            listener.onTrackingNewLocation(locationToTrack);
        }
        for(CompassSensorListener listener : mRotationListeners) {
            listener.onTrackingNewLocation(locationToTrack);
        }
        return this;
    }

    /**
     * Starts the compass sensors.
     */
    @SuppressLint("MissingPermission")
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void start() {

        // Checking permissions
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if(mLocationToTrack != null) {

            if(mLocationListeners.size() > 0) {
                mLocationProvider.requestLocationUpdates(locationRequest, mLocationCallback, null);
            }
            if(mBearingListeners.size() > 0) {
                mHasGravitySensor = mSensorManager.registerListener(this, mGravityFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
                mHasMagneticFieldSensor = mSensorManager.registerListener(this, mMagneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
                if (!mHasGravitySensor) {
                    mHasAccelerometerSensor = mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
            if(mRotationListeners.size() > 0) {
                mHasRotationVectorSensor = mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    protected void destroy() {
        // Just to make sure nothing gets leaked
        mLocationListeners.clear();
        mBearingListeners.clear();
        mRotationListeners.clear();
        mContext = null;
    }

    /**
     * Stops the compass sensors.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stop() {
        mLocationProvider.removeLocationUpdates(mLocationCallback);

        if(mHasGravitySensor) {
            mSensorManager.unregisterListener(this, mGravityFieldSensor);
        }
        if(mHasAccelerometerSensor) {
            mSensorManager.unregisterListener(this, mAccelerometerSensor);
        }
        if(mHasMagneticFieldSensor) {
            mSensorManager.unregisterListener(this, mMagneticFieldSensor);
        }
        if(mHasRotationVectorSensor) {
            mSensorManager.unregisterListener(this, mRotationVectorSensor);
        }

        mLastCalculatedBearingToLocation = 0;
        mCurrentLocation = null;
        mGravityData = null;
        mMagneticFieldData = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        synchronized (mMonitor) {

            // Ignore low quality data
            if(mMagneticFieldSensorAccuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                return;
            }

            switch (event.sensor.getType()) {
                case Sensor.TYPE_GRAVITY:
                    mGravityData = event.values;
                    onBearingSensorsChanged();
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    mGravityData = event.values;
                    onBearingSensorsChanged();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mMagneticFieldData = event.values;
                    onBearingSensorsChanged();
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    mRotationVectorData = event.values;
                    onRotationSensorChanged();
                    break;
            }
        }
    }

    /**
     * Whenever any sensor related to bearing and azimuth orientation has changed.
     */
    private void onBearingSensorsChanged() {
        if (mGravityData != null && mMagneticFieldData != null && mCurrentLocation != null) {
            SensorManager.getRotationMatrix(mRotationMatrix, null, mGravityData, mMagneticFieldData);
            SensorManager.getOrientation(mRotationMatrix, mOrientationData);

            GeomagneticField geomagneticField = new GeomagneticField(
                    (float) mCurrentLocation.getLatitude(),
                    (float) mCurrentLocation.getLongitude(),
                    (float) mCurrentLocation.getAltitude(), System.currentTimeMillis());

            float azimuth = (float) Math.toDegrees(mOrientationData[0]) + geomagneticField.getDeclination();
            float bearing = mCurrentLocation.bearingTo(mLocationToTrack);
            float bearingToLocation = (azimuth - bearing + 360) % 360;
            float northAzimuth = (azimuth + 360) % 360;

            if(Math.abs(mLastCalculatedBearingToLocation - bearingToLocation) > MINIMUM_ANGLE_CHANGE) {
                mLastCalculatedBearingToLocation = bearingToLocation;

                for(BearingCallback listener : mBearingListeners) {
                    listener.onNewBearing(mLastCalculatedBearingToLocation, northAzimuth);
                }
            }
        }
    }

    /**
     * Whenever ANY sensor changes, this method must be called to calculate rotation
     */
    private void onRotationSensorChanged() {
        if(mRotationVectorData != null && mCurrentLocation != null) {
            SensorManager.getRotationMatrixFromVector(mRotationMatrixFromVector, mRotationVectorData);
            for (RotationCallback listener : mRotationListeners) {
                listener.onNewRotation(mRotationMatrixFromVector);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if(sensor == mMagneticFieldSensor) {
            mMagneticFieldSensorAccuracy = accuracy;
        }
    }
}
