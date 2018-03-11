package com.arecmetafora.getmethere;

import android.graphics.Bitmap;
import android.location.Location;

import java.io.File;
import java.io.Serializable;

class OfflineLocation implements Serializable {

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
