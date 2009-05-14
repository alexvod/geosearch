package org.alexvod;

import android.util.Log;

public class FastIndexOf {
    static {
        try {
            Log.i("JNI", "Trying to load libfastindexof_jni.so");
            System.load("/data/data/org.alexvod.geosearch/libfastindexof_jni.so");
            Log.i("JNI", "Successfully loaded!");
            nativeLibraryAvailable = true;
        }
        catch (UnsatisfiedLinkError ule) {
            Log.e("JNI", "WARNING: Could not load libfastindexof_jni.so");
            nativeLibraryAvailable = false;
        }
    }

    public static native int fastIndexOf(byte[] str, byte[] substr, int start);
    public static boolean nativeLibraryAvailable;
}
