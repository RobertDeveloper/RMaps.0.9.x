package org.andnav.osm.views.util;

import android.os.Build;
import android.view.MotionEvent;

public abstract class VersionedGestureDetector {
    @SuppressWarnings("unused")
	private static final String TAG = "VersionedGestureDetector";

    OnGestureListener mListener;

    public static VersionedGestureDetector newInstance(OnGestureListener listener) {
        final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
        VersionedGestureDetector detector = null;
        if (sdkVersion < Build.VERSION_CODES.ECLAIR) {
            detector = new CupcakeDetector();
        } else {
            detector = new EclairDetector();
        }

        detector.mListener = listener;

        return detector;
    }

    public abstract boolean onTouchEvent(MotionEvent ev);

    public interface OnGestureListener {
        public void onDown(MotionEvent event);
        public void onMove(MotionEvent event, int count, float x1, float y1, float x2, float y2);
        public void onUp(MotionEvent event);
        public void onDown2(MotionEvent event, float x1, float y1, float x2, float y2);
        public void onUp2(MotionEvent event);
    }

    private static class CupcakeDetector extends VersionedGestureDetector {
        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
            	mListener.onDown(ev);
                break;
            }
            case MotionEvent.ACTION_UP: {
            	mListener.onUp(ev);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                mListener.onMove(ev, 1, ev.getX(), ev.getY(), 0, 0);
                break;
            }
            }
            return true;
        }
    }

    private static class EclairDetector extends CupcakeDetector {
        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            final int action = ev.getAction();
            switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_CANCEL:
            	mListener.onUp(ev);
               break;
    		case MotionEvent.ACTION_POINTER_DOWN:
    		case MotionEvent.ACTION_POINTER_2_DOWN:
             	mListener.onDown2(ev, ev.getX(ev.findPointerIndex(0)), ev.getY(ev.findPointerIndex(0)), ev.getX(ev.findPointerIndex(1)), ev.getY(ev.findPointerIndex(1)));
    			break;
    		case MotionEvent.ACTION_POINTER_UP:
    		case MotionEvent.ACTION_POINTER_2_UP:
    			mListener.onUp2(ev);
                break;
            case MotionEvent.ACTION_MOVE:
            	if(ev.getPointerCount()>1){
            		final int index0 = ev.findPointerIndex(0);
            		final int index1 = ev.findPointerIndex(1);
            		if(index0 == -1 || index1 == -1)
            			mListener.onMove(ev, 1, ev.getX(), ev.getY(), 0, 0);
            		else
            			mListener.onMove(ev, 2, ev.getX(ev.findPointerIndex(0)), ev.getY(ev.findPointerIndex(0)), ev.getX(ev.findPointerIndex(1)), ev.getY(ev.findPointerIndex(1)));
            	} else
            		mListener.onMove(ev, 1, ev.getX(), ev.getY(), 0, 0);
                break;
            }

            return super.onTouchEvent(ev);
        }
    }

}
