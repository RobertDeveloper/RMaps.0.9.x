// Created by plusminus on 00:14:42 - 02.10.2008
package org.andnav.osm;

import org.andnav.osm.util.constants.OpenStreetMapConstants;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Baseclass for Activities who want to contribute to the OpenStreetMap Project.
 * @author Nicolas Gramlich
 *
 */
public abstract class OpenStreetMapActivity extends Activity implements OpenStreetMapConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	protected static final String PROVIDER_NAME = LocationManager.GPS_PROVIDER;

	// ===========================================================
	// Fields
	// ===========================================================

	protected SampleLocationListener mLocationListener, mNetListener;

//	protected RouteRecorder mRouteRecorder = new RouteRecorder();

	protected boolean mDoGPSRecordingAndContributing;

	protected LocationManager mLocationManager;

	public int mNumSatellites = NOT_SET;

	private boolean mGPSFastUpdate = true;

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * Calls <code>onCreate(final Bundle savedInstanceState, final boolean pDoGPSRecordingAndContributing)</code> with <code>pDoGPSRecordingAndContributing == true</code>.<br/>
	 * That means it automatically contributes to the OpenStreetMap Project in the background.
	 * @param savedInstanceState
	 */
	public void onCreate(final Bundle savedInstanceState) {
		onCreate(savedInstanceState, true);
	}
	/**
	 * Called when the activity is first created. Registers LocationListener.
	 * @param savedInstanceState
	 * @param pDoGPSRecordingAndContributing If <code>true</code>, it automatically contributes to the OpenStreetMap Project in the background.
	 */
	public void onCreate(final Bundle savedInstanceState, final boolean pDoGPSRecordingAndContributing) {
		super.onCreate(savedInstanceState);
		
//		if(pDoGPSRecordingAndContributing)
//			this.enableDoGPSRecordingAndContributing();
//		else
//			this.disableDoGPSRecordingAndContributing(false);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		mGPSFastUpdate = pref.getBoolean("pref_gpsfastupdate", true);

		// register location listener
		initLocation();
	}

	private LocationManager getLocationManager() {
		if(this.mLocationManager == null)
			this.mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		return this.mLocationManager;
	}

	private void getBestProvider(){
		int minTime = 0;
		int minDistance = 0;

		if(!mGPSFastUpdate){
			minTime = 2000;
			minDistance = 20;
		};

		getLocationManager().removeUpdates(mLocationListener);
		if(mNetListener != null)
			getLocationManager().removeUpdates(mNetListener);

		if(getLocationManager().isProviderEnabled(GPS)){
				getLocationManager().requestLocationUpdates(GPS, minTime, minDistance, this.mLocationListener);

				try {
					if(getLocationManager().isProviderEnabled(NETWORK)){
						this.mNetListener = new SampleLocationListener();
						getLocationManager().requestLocationUpdates(NETWORK, minTime, minDistance, this.mNetListener);
					}
				} catch (Exception e) {
					Log.e(DEBUGTAG, "isProviderEnabled(NETWORK) exception");
					e.printStackTrace();
				}

		} else if(getLocationManager().isProviderEnabled(NETWORK)) {
			getLocationManager().requestLocationUpdates(NETWORK, minTime, minDistance, this.mLocationListener);
		}
	}

	private void initLocation() {
		this.mLocationListener = new SampleLocationListener();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	public abstract void onLocationLost();

	public abstract void onLocationChanged(final Location pLoc);

	public abstract void onStatusChanged(String provider, int status, Bundle extras);

	/**
	 * Called when activity is destroyed. Unregisters LocationListener.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		getLocationManager().removeUpdates(mLocationListener);
		if(mNetListener != null)
			getLocationManager().removeUpdates(mNetListener);

//		if(this.mDoGPSRecordingAndContributing){
//			OSMUploader.uploadAsync(this.mRouteRecorder.getRecordedGeoPoints());
//		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

//	public void enableDoGPSRecordingAndContributing(){
//		/* If already true, return. */
//		if(this.mDoGPSRecordingAndContributing)
//			return;
//
//		this.mRouteRecorder = new RouteRecorder();
//
//		this.mDoGPSRecordingAndContributing = true;
//	}
//
//	public void disableDoGPSRecordingAndContributing(final boolean pContributdeCurrentRoute){
//		/* If already false, return. */
//		if(!this.mDoGPSRecordingAndContributing)
//			return;
//
//		if(pContributdeCurrentRoute){
//			OSMUploader.uploadAsync(this.mRouteRecorder.getRecordedGeoPoints());
//		}
//
//		this.mRouteRecorder = null;
//
//		this.mDoGPSRecordingAndContributing = false;
//	}

	@Override
	protected void onStart() {
		getBestProvider();
		super.onStart();
	}
	@Override
	protected void onStop() {
		getLocationManager().removeUpdates(mLocationListener);
		super.onStop();
	}

	protected void StatusChanged(String provider, int status, Bundle b){
//		Log.e(DEBUGTAG, "onStatusChanged povider = " + provider + " status = " + status + " satellites = " + b.getInt("satellites", NOT_SET));
		if(mNetListener != null) {
			if(provider.equals(GPS) && status == 2) {
				getLocationManager().removeUpdates(mNetListener);
				mNetListener = null;
				Log.e(DEBUGTAG, "Stop NETWORK listener");
			}
		}
		if(mNetListener == null)
			OpenStreetMapActivity.this.onStatusChanged(provider, status, b);
		else if (mNetListener != null && provider.equals(NETWORK))
			OpenStreetMapActivity.this.onStatusChanged(provider, status, b);

	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	/**
	 * Logs all Location-changes to <code>mRouteRecorder</code>.
	 * @author plusminus
	 */
	private class SampleLocationListener implements LocationListener {
		public void onLocationChanged(final Location loc) {
			if (loc != null){
//				if(OpenStreetMapActivity.this.mDoGPSRecordingAndContributing)
//					OpenStreetMapActivity.this.mRouteRecorder.add(loc, OpenStreetMapActivity.this.mNumSatellites);

				OpenStreetMapActivity.this.onLocationChanged(loc);
			}else{
				OpenStreetMapActivity.this.onLocationLost();
			}
		}

		public void onStatusChanged(String a, int status, Bundle b) {
			OpenStreetMapActivity.this.mNumSatellites = b.getInt("satellites", NOT_SET); // LATER Check on an actual device
			//Log.e(DEBUGTAG, "onStatusChanged status = " + status + " satellites = " + b.getInt("satellites", NOT_SET));
			StatusChanged(a, status, b);
		}

		public void onProviderEnabled(String a) {
			Log.e(DEBUGTAG, "onProviderEnabled");
			getBestProvider();
		}
		public void onProviderDisabled(String a) {
			Log.e(DEBUGTAG, "onProviderDisabled");
			getBestProvider();
		}
	}
}
