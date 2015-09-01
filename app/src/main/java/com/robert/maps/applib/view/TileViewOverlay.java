package com.robert.maps.applib.view;

import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.MotionEvent;

public abstract class TileViewOverlay {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for SuperClass/Interfaces
	// ===========================================================
	
	/**
	 * Managed Draw calls gives Overlays the possibility to first draw manually and after that do a final draw. This is very useful, i sth. to be drawn needs to be <b>topmost</b>.
	 */
	public void onManagedDraw(final Canvas c, final TileView tileView){
		onDraw(c, tileView);
		onDrawFinished(c, tileView);
	}
	
	protected abstract void onDraw(final Canvas c, final TileView tileView);
	
	protected abstract void onDrawFinished(final Canvas c, final TileView tileView);

	// ===========================================================
	// Methods
	// ===========================================================
	
	/**
	 * By default does nothing (<code>return false</code>). If you handled the Event, return <code>true</code>, otherwise return <code>false</code>.
	 * If you returned <code>true</code> none of the following Overlays or the underlying {@link OpenStreetMapView} has the chance to handle this event. 
	 */
	public boolean onKeyDown(final int keyCode, KeyEvent event, final TileView mapView) {
		return false;
	}
	
	/**
	 * By default does nothing (<code>return false</code>). If you handled the Event, return <code>true</code>, otherwise return <code>false</code>.
	 * If you returned <code>true</code> none of the following Overlays or the underlying {@link OpenStreetMapView} has the chance to handle this event. 
	 */
	public boolean onKeyUp(final int keyCode, KeyEvent event, final TileView mapView) {
		return false;
	}
	
	/**
	 * <b>You can prevent all(!) other Touch-related events from happening!</b><br />
	 * By default does nothing (<code>return false</code>). If you handled the Event, return <code>true</code>, otherwise return <code>false</code>.
	 * If you returned <code>true</code> none of the following Overlays or the underlying {@link OpenStreetMapView} has the chance to handle this event. 
	 */
	public boolean onTouchEvent(final MotionEvent event, final TileView mapView) {
		return false;
	}
	
	/**
	 * By default does nothing (<code>return false</code>). If you handled the Event, return <code>true</code>, otherwise return <code>false</code>.
	 * If you returned <code>true</code> none of the following Overlays or the underlying {@link OpenStreetMapView} has the chance to handle this event. 
	 */
	public boolean onTrackballEvent(final MotionEvent event, final TileView mapView) {
		return false;
	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the Event, return <code>true</code>, otherwise return <code>false</code>.
	 * If you returned <code>true</code> none of the following Overlays or the underlying {@link OpenStreetMapView} has the chance to handle this event. 
	 */
	public boolean onSingleTapUp(MotionEvent e, TileView openStreetMapView) {
		return false;
	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the Event, return <code>true</code>, otherwise return <code>false</code>.
	 * If you returned <code>true</code> none of the following Overlays or the underlying {@link OpenStreetMapView} has the chance to handle this event. 
	 */
	public int onLongPress(MotionEvent e, TileView openStreetMapView) {
		return 0;
	}
	
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY, final TileView tileView) {
		return false;
	}
	
	public boolean onDown(MotionEvent e, final TileView tileView) {
		return false;
	}
	
	public void onUp(MotionEvent e, final TileView tileView) {
		
	}

	public void Free() {
		
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
