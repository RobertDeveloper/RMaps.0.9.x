package com.robert.maps.reflection;

import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

public interface OnExGestureListener extends OnGestureListener, OnDoubleTapListener {
	void onUp(MotionEvent e);
}
