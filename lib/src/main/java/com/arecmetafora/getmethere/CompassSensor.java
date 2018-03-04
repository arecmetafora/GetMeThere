package com.arecmetafora.getmethere;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

/**
 * Location compass sensor, using GPS and device sensors.
 */
public final class CompassSensor implements SensorEventListener {

    /**
     * Callback to notify when the compass sensors found a new orientation angle.
     */
    public interface Callback {

        /**
         * Notifies when the angle between user`s orientation and the desired location has changed.
         *
         * @param myLocation The current user location.
         * @param bearingToLocation The angle from user`s orientation and the tracked location
         */
        void onSensorUpdate(Location myLocation, float bearingToLocation);
    }

    /**
     * Minimum angle change to notify listeners.
     */
    private static final int MINIMUM_ANGLE_CHANGE = 5;

    /**
     * Minimum user displacement in meters, so a new location is captured.
     */
    private static final int MINIMUM_DISPLACEMENT = 10;

    /**
     * Interval between location captures.
     */
    private static final int LOCATION_CAPTURE_INTERVAL = 3000;

    // GPS sensor
    private FusedLocationProviderClient mLocationProvider;
    private LocationRequest locationRequest;
    private Location mCurrentLocation;
    private Location mLocationToTrack;

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

    private Callback mListener;

    private LocationCallback mLocationCallback = new LocationCallback() {
        public void onLocationResult(LocationResult result) {
            mCurrentLocation = result.getLastLocation();
            onSensorValueChanged();
        }
    };

    /**
     * Creates a location compass sensor, using GPS and device orientation sensors.
     *
     * @param context The application context.
     * @param listener The sensor listener.
     */
    public CompassSensor(Context context, @NonNull Callback listener) {
        this(context, null, listener);
    }

    /**
     * Creates a location compass sensor, using GPS and device orientation sensors.
     *
     * @param context The application context.
     * @param locationToTrack The location to be tracked by the sensors.
     * @param listener The sensor listener.
     */
    public CompassSensor(Context context, Location locationToTrack, @NonNull Callback listener) {
        this.mListener = listener;
        this.mLocationToTrack = locationToTrack;

        mLocationProvider = LocationServices.getFusedLocationProviderClient(context);

        locationRequest = LocationRequest.create()
                .setInterval(LOCATION_CAPTURE_INTERVAL)
                .setFastestInterval(LOCATION_CAPTURE_INTERVAL)
                .setSmallestDisplacement(MINIMUM_DISPLACEMENT)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        mGravityFieldSensor = this.mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticFieldSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    /**
     * Starts the compass sensors.
     */
    @SuppressLint("MissingPermission")
    public void start() {
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

            if(Math.abs(mLastCalculatedBearingToLocation - bearingToLocation) > MINIMUM_ANGLE_CHANGE) {
                mLastCalculatedBearingToLocation = bearingToLocation;
                if (mListener != null) {
                    mListener.onSensorUpdate(mCurrentLocation, mLastCalculatedBearingToLocation);
                }
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * Sets the location to track.
     *
     * @param locationToTrack The location which the angle with the device orientation will be calculated.
     */
    public void setLocationToTrack(Location locationToTrack) {
        this.mLocationToTrack = locationToTrack;
    }
}
