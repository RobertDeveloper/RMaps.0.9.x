package com.robert.maps.applib.reflection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public abstract class VerScaleGestureDetector {

	OnGestureListener mListener;

    public static VerScaleGestureDetector newInstance(Context context, OnGestureListener listener) {
        final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
        VerScaleGestureDetector detector = null;
        if (sdkVersion < Build.VERSION_CODES.FROYO) {
            detector = new CupcakeDetector();
        } else {
            detector = new FroyoDetector(context);
        }
        
        detector.mListener = listener;

        return detector;
    }

    public abstract boolean onTouchEvent(MotionEvent ev);

    public interface OnGestureListener {
        public void onScale(double aScaleFactor);
        public void onScaleEnd();
    }

    private static class CupcakeDetector extends VerScaleGestureDetector {

		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			return false;
		}
    	
    }

    private static class FroyoDetector extends VerScaleGestureDetector {
    	
    	private ScaleGestureDetector mScaleDetector;

		@SuppressLint("NewApi")
		public FroyoDetector(Context context) {
			super();
			mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		}

		@SuppressLint("NewApi")
		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			return mScaleDetector.onTouchEvent(ev);
		}
		
		@SuppressLint("NewApi")
		private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				mListener.onScale(detector.getScaleFactor());
				return super.onScale(detector);
			}

			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector) {
				return super.onScaleBegin(detector);
			}

			@Override
			public void onScaleEnd(ScaleGestureDetector detector) {
				mListener.onScaleEnd();
				super.onScaleEnd(detector);
			}
			
		}
    	
    }
}
