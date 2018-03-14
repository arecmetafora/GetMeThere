package com.arecmetafora.getmethere.app;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

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

    OfflineLocation() {
    }

    OfflineLocation(Parcel in) {
        description = in.readString();
        location = in.readParcelable(Location.class.getClassLoader());
    }

    // I do not know why I need this, but Android Studio say I do, so I do.
    public static final Creator<OfflineLocation> CREATOR = new Creator<OfflineLocation>() {
        @Override
        public OfflineLocation createFromParcel(Parcel in) {
            return new OfflineLocation(in);
        }

        @Override
        public OfflineLocation[] newArray(int size) {
            return new OfflineLocation[size];
        }
    };

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
