
# GetMeThere

GetMeThere consists in a set of views and tools to help you build offline applications to guide a user to get to a specific location on the globe.

```groovy
compile 'com.arecmetafora:getMeThere:1.0.0'
```

# UI Components

GetMeThere provides two out-of-the-box UI components to work with offline navigation and georeference:  `Map` and `Compass`

A `Map` is a tiny and lightweight offline map of a specific location's neighborhood. It helps users to find their way home by providing a simple guidance through a small offline map. It is very useful when the user needs to get an overview about his surroundings and can be used in a variety of situations, for instance, finding his hotel, points of interests in a tracking challenge, etc.

<img src="https://github.com/arecmetafora/GetMeThere/raw/master/Video.gif" width="300" align="middle">


You can declare a `Map` via XML as shown in the example below:

```XML
<com.arecmetafora.getmethere.Map
    xmlns:lib="http://schemas.android.com/apk/res-auto"
    android:id="@+id/map"
    android:layout_width="match_parent"
    android:layout_height="300dp"
    lib:arcWidth="@dimen/map_arc_width"
    lib:arcColor="@color/map_arc"
    lib:textSize="@dimen/map_distance_text_size"
    lib:textColor="@color/map_distance"
    lib:pointer="@drawable/map_location_pointer"
    lib:locationIcon="@drawable/map_location_pin"/>
```
_The custom styles (under `lib` namespace) are optional._

After declaring your `Map` view, you still need to set an offline map which contains the map image that will be drawn. How to do that will be described later, in the **Offline Maps** section.

A `Compass` is like a common compass used for navigation. However, instead of pointing to magnetic north, it points to the location that the user is heading to. Also, in addition to the pointer, it provides the distance between the current user's location and the point of interest, in meters.

Its declaration in XML can be done in the following way:

```XML
<com.arecmetafora.getmethere.Compass
    xmlns:lib="http://schemas.android.com/apk/res-auto"
    android:id="@+id/compass"
    android:layout_width="match_parent"
    android:layout_height="300dp"
    lib:locationIcon="@drawable/compass_location"/>
```

_The custom styles (under `lib` namespace) are optional._

# Compass Sensors

To get the UI components working you need to bind them to a `CompassSensor`. This class is responsible for tracking a location, providing callbacks with the current user's location, the distance in meters between the current location and the point of interest, the north [azimuth](https://en.wikipedia.org/wiki/Azimuth) and the azimuth between the user's orientation and the location, etc.

You can bind a `CompassSensor` to any object which class implements the  `CompassSensor.CompassSensorListener` interface.

The bind of a `CompassSensor` to a UI component can be done in the `onCreate` method of your Activity, as shown below:

```java
mCompass = findViewById(R.id.compass);
mMap = findViewById(R.id.map);
Location location = ...; // Where the user is heading to

CompassSensor sensor = new CompassSensor(this, getLifecycle())
		.bindTo(mMap)
		.bindTo(mCompass);
		
sensor.setLocationToTrack(location);
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

# Required Permissions

This library needs some permissions to work. Make sure your app requests this permissions to the user before using their components:

 - `android.permission.ACCESS_COARSE_LOCATION`
 - `android.permission.ACCESS_FINE_LOCATION`

