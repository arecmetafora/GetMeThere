package com.arecmetafora.getmethere;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
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
import com.google.android.gms.location.LocationCallback;
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
     * Accuracy from compass sensors.
     */
    public enum Accuracy {
        UNRELIABLE,
        LOW,
        MEDIUM,
        HIGH
    }

    /**
     * Represents an entity which is observing the compass for sensors changes.
     */
    public interface CompassSensorListener {

        /**
         * Callback trigger when any sensor value has changed (user's location, direction angle, etc).
         *
         * @param myLocation The current user location.
         * @param bearingToLocation The angle between user`s orientation and the tracked location.
         * @param azimuth Azimuth to north pole.
         * @param accuracy Sensor accuracies.
         */
        void onSensorUpdate(Location myLocation, float bearingToLocation, float azimuth, Accuracy accuracy);

        /**
         * Callback trigger when the listener when the compass is targeting another location to be tracked.
         *
         * @param location The new location to be tracked.
         */
        void onTrackingNewLocation(Location location);
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
    private int mMagneticFieldSensorAccuracy;

    // Orientation sensors
    private SensorManager mSensorManager;
    private Sensor mGravityFieldSensor;
    private Sensor mAccelerometerSensor;
    private Sensor mMagneticFieldSensor;
    private boolean mHasGravitySensor;
    private boolean mHasAccelerometerSensor;
    private boolean mHasMagneticFieldSensor;
    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationData = new float[3];
    private float[] mGravityData;
    private float[] mMagneticFieldData;
    private float mLastCalculatedBearingToLocation = 0;
    private float mNorthAzimuth = 0;

    // Listeners
    private List<CompassSensorListener> mListeners;

    // GPS sensor callback
    private LocationCallback mLocationCallback = new LocationCallback() {
        public void onLocationResult(LocationResult result) {
            mCurrentLocation = result.getLastLocation();
            notifySensorUpdate();
        }
    };

    /**
     * Creates a location compass sensor, using GPS and device orientation sensors.
     *
     * @param context The application context.
     * @param lifecycle Android life cycle controller.
     */
    public CompassSensor(@NonNull Context context, @NonNull Lifecycle lifecycle) {
        this(context, lifecycle, null);
    }

    /**
     * Creates a location compass sensor, using GPS and device orientation sensors.
     *
     * @param context The application context.
     * @param lifecycle Android life cycle controller.
     * @param locationToTrack The location which the user is heading to.
     */
    public CompassSensor(@NonNull Context context, @NonNull Lifecycle lifecycle, Location locationToTrack) {
        mContext = context;
        mLocationToTrack = locationToTrack;
        mListeners = new LinkedList<>();

        lifecycle.addObserver(this);

        mLocationProvider = LocationServices.getFusedLocationProviderClient(mContext);

        locationRequest = LocationRequest.create()
                .setInterval(LOCATION_CAPTURE_INTERVAL)
                .setFastestInterval(LOCATION_CAPTURE_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        mGravityFieldSensor = this.mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticFieldSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    /**
     * Binds a compass sensor receiver to this sensor.
     *
     * @param listener The compass sensor listener.
     * @return The same compass sensor instance.
     */
    public CompassSensor bindTo(CompassSensorListener listener) {
        mListeners.add(listener);
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
            mLocationProvider.requestLocationUpdates(locationRequest, mLocationCallback, null);
            mHasGravitySensor = mSensorManager.registerListener(this, mGravityFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
            mHasMagneticFieldSensor = mSensorManager.registerListener(this, mMagneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
            if(!mHasGravitySensor) {
                mHasAccelerometerSensor = mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }

            if(!mHasMagneticFieldSensor || !(mHasGravitySensor || mHasAccelerometerSensor)) {
                // Not enough sensors to continue
                stop();
            }
        }
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

        mLastCalculatedBearingToLocation = 0;
        mCurrentLocation = null;
        mGravityData = null;
        mMagneticFieldData = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_GRAVITY:
                mGravityData = event.values;
                break;
            case Sensor.TYPE_ACCELEROMETER:
                mGravityData = event.values;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagneticFieldData = event.values;
                break;
        }

        onSensorValueChanged();
    }

    /**
     * Whenever ANY sensor changes, this method must be called to calculate rotation
     */
    private void onSensorValueChanged() {
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
            mNorthAzimuth = (azimuth + 360) % 360;

            if(Math.abs(mLastCalculatedBearingToLocation - bearingToLocation) > MINIMUM_ANGLE_CHANGE) {
                mLastCalculatedBearingToLocation = bearingToLocation;
                notifySensorUpdate();
            }
        }
    }

    /**
     * Notify when a sensor has changed.
     */
    private void notifySensorUpdate() {
        if (mListeners.size() > 0) {
            Accuracy acc;
            if(mMagneticFieldSensorAccuracy >= 4) {
                acc = Accuracy.HIGH;
            } else if(mMagneticFieldSensorAccuracy == 3) {
                acc = Accuracy.MEDIUM;
            } else if(mMagneticFieldSensorAccuracy == 2) {
                acc = Accuracy.LOW;
            } else {
                acc = Accuracy.UNRELIABLE;
            }

            for(CompassSensorListener listener : mListeners) {
                listener.onSensorUpdate(mCurrentLocation, mLastCalculatedBearingToLocation, mNorthAzimuth, acc);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if(sensor == mMagneticFieldSensor) {
            mMagneticFieldSensorAccuracy = accuracy;
        }
    }

    /**
     * Sets the location to track.
     *
     * @param locationToTrack The location which the angle with the device orientation will be calculated.
     */
    public void setLocationToTrack(Location locationToTrack) {
        this.mLocationToTrack = locationToTrack;
        for(CompassSensorListener listener : mListeners) {
            listener.onTrackingNewLocation(locationToTrack);
        }
    }
}
