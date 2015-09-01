package com.robert.maps.applib.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.robert.maps.R;
import com.robert.maps.applib.kml.PoiManager;
import com.robert.maps.applib.overlays.MyLocationOverlay;
import com.robert.maps.applib.overlays.PoiOverlay;
import com.robert.maps.applib.overlays.TrackOverlay;
import com.robert.maps.applib.tileprovider.TileSource;
import com.robert.maps.applib.tileprovider.TileSourceBase;
import com.robert.maps.applib.utils.Ut;
import com.robert.maps.applib.view.IMoveListener;
import com.robert.maps.applib.view.MapView;

import org.andnav.osm.util.GeoPoint;

public class OffsetActivity extends Activity {
	private static final String OFFSET_TEXT = "%s: %d m, %d m";
	private MapView mMap;
	private TileSource mTileSource;
	private MyLocationOverlay mMyLocationOverlay;
	private PoiOverlay mPoiOverlay;
	private TrackOverlay mTrackOverlay;
	private PoiManager mPoiManager;
	private Handler mCallbackHandler = new MainActivityCallbackHandler();
	private MoveListener mMoveListener = new MoveListener();
	private double mOffsetLat, mOffsetLon;
	private CharSequence mGpsStatusName;
	private GeoPoint mGeo0 = new GeoPoint(0, 0);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.offsetactivity);
		
		mPoiManager = new PoiManager(this);
		mMap = (MapView) findViewById(R.id.map);
		mMap.setMoveListener(mMoveListener);
		mMap.getTileView().setOffsetMode(true);
		this.mTrackOverlay = new TrackOverlay(null, mPoiManager, mCallbackHandler);
       	this.mMap.getOverlays().add(mTrackOverlay);
       	this.mPoiOverlay = new PoiOverlay(this, mPoiManager, null, false);
       	this.mMap.getOverlays().add(mPoiOverlay);
       	mMyLocationOverlay = new MyLocationOverlay(this);
       	mMap.getOverlays().add(mMyLocationOverlay);
       	
       	mLocationListener = new SampleLocationListener();
       	
       	findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(OffsetActivity.this);
				Editor editor = pref.edit();
				editor.putFloat(mTileSource.ID+TileSourceBase.OFFSETLAT_, (float)mOffsetLat);
				editor.putFloat(mTileSource.ID+TileSourceBase.OFFSETLON_, (float)mOffsetLon);
				editor.commit();
				
				finish();
			}
		});
		
	}

	@Override
	protected void onResume() {
		Intent intent = getIntent();
		if (intent != null) {
			if (mTileSource != null)
				mTileSource.Free();

			try {
				mTileSource = new TileSource(this, intent.getStringExtra("MAPID"));
			} catch (Exception e) {
			}
			mMap.setTileSource(mTileSource);
			
			SharedPreferences uiState = getSharedPreferences("MapName", Activity.MODE_PRIVATE);
			mMap.getController().setZoom(uiState.getInt("ZoomLevel", 0));
			mMap.getController().setCenter(new GeoPoint(uiState.getInt("Latitude", 0), uiState.getInt("Longitude", 0)));
			setTitle();
			
			mOffsetLat = mTileSource.OFFSET_LAT;
			mOffsetLon = mTileSource.OFFSET_LON;
			setOffsetText();
		}
		
		mLocationListener.getBestProvider();

		super.onResume();
	}

	@Override
	protected void onPause() {
		mLocationListener.getLocationManager().removeUpdates(mLocationListener);
		
		if(mTileSource != null)
			mTileSource.Free();
		mPoiManager.FreeDatabases();
		
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		mMap.setMoveListener(null);
		
		super.onDestroy();
	}

	private void setOffsetText() {
		final int lat = (mOffsetLat < 0 ? -1 : 1) * mGeo0.distanceTo(new GeoPoint((int) (1E6 * mOffsetLat), 0));
		final int lon = (mOffsetLon < 0 ? -1 : 1) * mGeo0.distanceTo(new GeoPoint(0, (int) (1E6 * mOffsetLon)));
		((TextView) findViewById(R.id.textOffset)).setText(String.format(OFFSET_TEXT, getResources().getString(R.string.offset_text), lat, lon));
	}

	private class MoveListener implements IMoveListener {

		public void onMoveDetected() {
			final double[] offset = mMap.getTileView().getCurrentOffset();
			mOffsetLat = offset[0];
			mOffsetLon = offset[1];
			setOffsetText();
		}

		public void onZoomDetected() {
			setTitle();
		}

		@Override
		public void onCenterDetected() {
		}
		
	}
	
	private class MainActivityCallbackHandler extends Handler {
		@Override
		public void handleMessage(final Message msg) {
			final int what = msg.what;
			if (what == Ut.MAPTILEFSLOADER_SUCCESS_ID) {
				mMap.invalidate(); //postInvalidate();
			} else if (what == R.id.user_moved_map) {
				// setAutoFollow(false);
			} else if (what == R.id.set_title) {
				setTitle();
			} else if (what == R.id.add_yandex_bookmark) {
				showDialog(R.id.add_yandex_bookmark);
			} else if (what == Ut.ERROR_MESSAGE) {
				if (msg.obj != null)
					Toast.makeText(OffsetActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
			}
		}
	}

	private void setTitle(){
		try {
			final TextView leftText = (TextView) findViewById(R.id.left_text);
			if(leftText != null)
				leftText.setText(mMap.getTileSource().NAME);
			
			final TextView gpsText = (TextView) findViewById(R.id.gps_text);
			if(gpsText != null){
				gpsText.setText(mGpsStatusName);
			}

			final TextView rightText = (TextView) findViewById(R.id.right_text);
			if(rightText != null){
				final double zoom = mMap.getZoomLevelScaled();
				if(zoom > mMap.getTileSource().ZOOM_MAXLEVEL)
					rightText.setText(""+(mMap.getTileSource().ZOOM_MAXLEVEL+1)+"+");
				else
					rightText.setText(""+(1 + Math.round(zoom)));
			}
		} catch (Exception e) {
		}
	}

	private SampleLocationListener mLocationListener;
	
	private class SampleLocationListener implements LocationListener {
		public static final String GPS = "gps";
		public static final String NETWORK = "network";
		public static final String OFF = "off";

		public void onLocationChanged(Location loc) {
			mMyLocationOverlay.setLocation(loc);
			
			mGpsStatusName = loc.getProvider(); // + " 2 " + (cnt >= 0 ? cnt : 0);
			mMap.invalidate();
			
			setTitle();
		}

		public void onProviderDisabled(String provider) {
			getBestProvider();
		}

		public void onProviderEnabled(String provider) {
			getBestProvider();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			mGpsStatusName = provider;
			setTitle();
		}
		
		private LocationManager getLocationManager() {
			return (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}

		private void getBestProvider() {
			int minTime = 0;
			int minDistance = 0;
			
			getLocationManager().removeUpdates(mLocationListener);
			
			if (getLocationManager().isProviderEnabled(GPS)) {
				getLocationManager().requestLocationUpdates(GPS, minTime, minDistance, mLocationListener);
				mGpsStatusName = GPS;
			} else {
				mGpsStatusName = OFF;
			}
			
			setTitle();
		}
	}
	
}
