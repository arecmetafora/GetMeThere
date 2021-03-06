
# GetMeThere

GetMeThere consists in a set of views and tools to help you build offline applications to guide a user to get to a specific location on the globe.

```groovy
compile 'com.arecmetafora:getMeThere:1.0.0'
```

https://play.google.com/store/apps/details?id=com.arecmetafora.getmethere

# UI Components

GetMeThere provides three out-of-the-box UI components to work with offline navigation and georeference:  `Map`, `Compass` and `AugmentedRealityCompass`.

<img src="https://github.com/arecmetafora/GetMeThere/raw/master/Video.gif" width="300" align="middle">

A `Map` is a tiny and lightweight offline map of a specific location's neighborhood. It helps users to find their way home by providing a simple guidance through a small offline map. It is very useful when the user needs to get an overview about his surroundings and can be used in a variety of situations, for instance, finding his hotel, points of interests in a tracking challenge, etc.

You can declare a `Map` via XML as shown in the example below:

```XML
<com.arecmetafora.getmethere.Map
    xmlns:lib="http://schemas.android.com/apk/res-auto"
    android:id="@+id/map"
    android:layout_width="match_parent"
    android:layout_height="300dp"
    lib:locationIcon="@drawable/map_location_pin"/>
```

After declaring your `Map` view, you still need to set an offline map which contains the map image that will be drawn. How to do that will be described later, in the **Offline Maps** section.

A `Compass` is like a common compass used for navigation. However, instead of pointing to magnetic north, it points to the location that the user is heading to. Also, in addition to the pointer, it provides the distance between the current user's location and the point of interest, in meters.

Its declaration in XML can be done in the following way:

```XML
<com.arecmetafora.getmethere.Compass
    xmlns:lib="http://schemas.android.com/apk/res-auto"
    android:id="@+id/compass"
    android:layout_width="match_parent"
    android:layout_height="300dp"
    lib:arcWidth="@dimen/compass_arc_width"
    lib:arcColor="@color/compass_arc"
    lib:textSize="@dimen/compass_distance_text_size"
    lib:textColor="@color/compass_distance"
    lib:pointer="@drawable/compass_pointer"
    lib:locationIcon="@drawable/compass_location"/>
```

<img src="https://github.com/arecmetafora/GetMeThere/raw/master/Video2.gif" width="300" align="middle">

An `AugmentedRealityCompass` is similar to `Compass`, but uses the camera to project the location on the screen, simulating an augmented reality scenario.

Its declaration in XML can be done in the following way:

```XML
<com.arecmetafora.getmethere.AugmentedRealityCompass
    xmlns:lib="http://schemas.android.com/apk/res-auto"
    android:id="@+id/ar_compass"
    android:layout_width="match_parent"
    android:layout_height="300dp"
    lib:textSize="@dimen/ar_compass_text_size"
    lib:textColor="@color/ar_compass_distance"
    lib:pointer="@drawable/ar_compass_pointer"
    lib:locationIcon="@drawable/ar_compass_location"/>
```

_The custom styles (under `lib` namespace) are optional._

# Compass Sensors

To get the UI components working you need to bind them to a `CompassSensor`. This class is responsible for tracking a location, providing callbacks with the current user's location, the distance in meters between the current location and the point of interest, the north [azimuth](https://en.wikipedia.org/wiki/Azimuth) and the azimuth between the user's orientation and the location, etc.

You can bind a `CompassSensor` to any object which class implements the  `CompassSensor.CompassSensorListener` interface.

The bind of a `CompassSensor` to a UI component can be done in the `onCreate` method of your Activity, as shown below:

```java
mMap = findViewById(R.id.map);
mCompass = findViewById(R.id.compass);
mAugmentedRealityCompass = findViewById(R.id.ar_compass);

Location location = ...; // Where the user is heading to

mCompassSensor = CompassSensor.from(this, this)
        .bindTo(mMap)
        .bindTo(mCompass)
        .bindTo(mAugmentedRealityCompass)
        .track(mLocationToTrack);
```
You do not need to worry about the Activity lifecycle and the compass sensor, since it is already aware of Activity lifecycle events, thanks to [Android architecture components](https://developer.android.com/topic/libraries/architecture/index.html).

# Offline Maps

So that your `Map` can work entirely offline, you need to provide it with an offline map. An offline map consists in a small image, the center location (in lat/lon degrees) and the scale used to get the map image.

`GetMeThere` already provides an out-of-the-box implementation for a **GoogleMaps** map (which is obtained using the [Google Static Maps API](https://developers.google.com/maps/documentation/static-maps/)). However, you can create your own offine map, by implementing the interface `OfflineMap` and `MapProjection` (or use the `MercatorProjection` instead).

The example below shows how to create an offline GoogleMaps map:

```java
OfflineGoogleMaps.cache(context, location, description);
```

This method will download a map with the default resolution of 1200x800 pixels (with 2x scale of a 600x400 map) using the zoom level of 15x.

After downloaded, you can load this map and append to a `Map` by calling the method `setOfflineMap` as show below:

```java
OfflineMap offlineMap =  OfflineGoogleMaps.fromLocation(context, location);
mMap.setOfflineMap(offlineMap);
```

# Required Permissions and Depedencies

This library needs some permissions to work. Make sure your app requests this permissions to the user before using their components:

 - `android.permission.ACCESS_COARSE_LOCATION`
 - `android.permission.ACCESS_FINE_LOCATION`
 - `android.permission.CAMERA`

This library also uses _Google Play Services_ to get the user's location. Make sure you add this dependency in your gradle script:

```groovy
compile 'com.google.android.gms:play-services-location:11.8.0'
```

# Example

The following example shows how to build a simple UI with the two view components, `Map` and `Compass`.

```Java
public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<OfflineMap> {

    Map mMap;
    Compass mCompass;
    Location mLocation;

    // Simple loader to get the map from the cloud and cache on disk
    private static class MapLoader extends AsyncTaskLoader<OfflineMap> {
        Location mLocation;

        private MapLoader(Context context, Location location) {
            super(context);
            mLocation = location;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Nullable
        @Override
        public OfflineMap loadInBackground() {
            try {
                OfflineGoogleMaps.cache(getContext(), mLocation, "TESTE");
                return OfflineGoogleMaps.fromLocation(getContext(), mLocation);
            } catch (Exception ignored) {
                // TODO: Deal with exceptions...
            }
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCompass = findViewById(R.id.compass);
        mMap = findViewById(R.id.map);
        
        // Mock Location. Get yours from intent args, for example
        mLocation = new Location("");
        mLocation.setLatitude(-23.595498);
        mLocation.setLongitude(-46.686404);

        CompassSensor.from(this, this)
                .bindTo(mMap)
                .bindTo(mCompass)
                .track(mLocation);

        getSupportLoaderManager().initLoader(0, null, this);

        // Make sure you request permissions before using the API views
        ActivityCompat.requestPermissions(this, new String[] {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mCompassSensor.start();
    }

    @NonNull
    @Override
    public Loader<OfflineMap> onCreateLoader(int id, @Nullable Bundle args) {
        return new MapLoader(this, mLocation);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<OfflineMap> loader, OfflineMap data) {
        mMap.setOfflineMap(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<OfflineMap> loader) {
    }
}
```
