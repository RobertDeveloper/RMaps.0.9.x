package com.robert.maps.applib.reflection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.GestureDetector;

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
    
    public abstract GestureDetector getGestureDetector(Context context, OnExGestureListener listener);

    private static class CupcakeDetector extends VerGestureDetector {

		@Override
		public RGestureDetectorCupcake getGestureDetector(Context context, OnExGestureListener listener) {
			return new RGestureDetectorCupcake(context, listener);
		}

    }

    private static class FroyoDetector extends VerGestureDetector {

		@SuppressLint("NewApi")
		@Override
		public RGestureDetectorFroyo getGestureDetector(Context context, OnExGestureListener listener) {
			return new RGestureDetectorFroyo(context, listener, null, false);
		}

    }
}
