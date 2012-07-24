package com.robert.maps.reflection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;

public abstract class VerGestureDetector {

    public static VerGestureDetector newInstance() {
        final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
        VerGestureDetector detector = null;
        if (sdkVersion < Build.VERSION_CODES.FROYO) {
            detector = new CupcakeDetector();
        } else {
            detector = new FroyoDetector();
        }

        return detector;
    }
    
    public abstract GestureDetector getGestureDetector(Context context, GestureDetector.OnGestureListener listener);

    private static class CupcakeDetector extends VerGestureDetector {

		@Override
		public GestureDetector getGestureDetector(Context context, OnGestureListener listener) {
			return new GestureDetector(context, listener);
		}
    	
    }

    private static class FroyoDetector extends VerGestureDetector {

		@SuppressLint("NewApi")
		@Override
		public GestureDetector getGestureDetector(Context context, OnGestureListener listener) {
			return new GestureDetector(context, listener, null, false);
		}
    	
    }
}
