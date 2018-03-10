package com.arecmetafora.getmethere;

import android.graphics.Bitmap;
import android.location.Location;

import java.io.File;

class OfflineLocation {

    enum Result {
        SAVING,
        SUCCESS,
        ERROR
    }

    File mapFile;
    Bitmap mapBitmap;
    String description;
    Location location;
}
