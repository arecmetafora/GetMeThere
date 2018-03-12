package com.arecmetafora.getmethere.app;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.Serializable;

class OfflineLocation implements Parcelable {

    enum Result {
        SAVING,
        SUCCESS,
        ERROR
    }

    transient File mapFile;
    transient Bitmap mapBitmap;
    String description;
    Location location;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(description);
        dest.writeParcelable(location, flags);
    }
}
