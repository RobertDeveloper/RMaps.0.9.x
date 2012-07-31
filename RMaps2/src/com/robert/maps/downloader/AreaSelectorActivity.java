package com.robert.maps.downloader;

import org.andnav.osm.util.GeoPoint;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;
import android.widget.TextView;

import com.robert.maps.R;
import com.robert.maps.tileprovider.TileSource;
import com.robert.maps.view.IMoveListener;
import com.robert.maps.view.MapView;

public class AreaSelectorActivity extends Activity {
	private static final String MAPNAME = "MapName";
	private static final String MAPNAMEAREASELECTOR = "MapNameAreaSelector";

	private MapView mMap;
	private AreaSelectorOverlay mAreaSelectorOverlay;
	private TileSource mTileSource;
	private MoveListener mMoveListener = new MoveListener();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.area_selector);
		
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);

 		mMap = (MapView) findViewById(R.id.map);
		mMap.setMoveListener(mMoveListener);
		mMap.displayZoomControls(Integer.parseInt(pref.getString("pref_zoomctrl", "1")));
		mMap.getController().setCenter(new GeoPoint(uiState.getInt("Latitude", 0), uiState.getInt("Longitude", 0)));
		mMap.setLongClickable(false);
		mAreaSelectorOverlay = new AreaSelectorOverlay();
		mMap.getOverlays().add(mAreaSelectorOverlay);
		
		Intent intent = getIntent();
		if(intent != null) {
			SharedPreferences.Editor editor = uiState.edit();
			editor.putString(MAPNAMEAREASELECTOR, intent.getStringExtra(MAPNAME));
			editor.putInt("ZoomLevelAS", intent.getIntExtra("ZoomLevel", 0));

			if(intent.getBooleanExtra("SetArea", false)) {
				editor.putInt("LatitudeAS1", intent.getIntExtra("Latitude1", 0));
				editor.putInt("LongitudeAS1", intent.getIntExtra("Longitude1", 0));
				editor.putInt("LatitudeAS2", intent.getIntExtra("Latitude2", 0));
				editor.putInt("LongitudeAS2", intent.getIntExtra("Longitude2", 0));
			} else {
				editor.putInt("LatitudeAS", intent.getIntExtra("Latitude", 0));
				editor.putInt("LongitudeAS", intent.getIntExtra("Longitude", 0));
				editor.putInt("LatitudeAS1", 0);
				editor.putInt("LongitudeAS1", 0);
				editor.putInt("LatitudeAS2", 0);
				editor.putInt("LongitudeAS2", 0);
			}
			editor.commit();
			
		}
	}
	
	private class MoveListener implements IMoveListener {

		public void onMoveDetected() {
		}

		public void onZoomDetected() {
			setTitle();
		}
		
	}
	
	private void setTitle(){
		try {
			final TextView leftText = (TextView) findViewById(R.id.left_text);
			if(leftText != null)
				leftText.setText(mMap.getTileSource().NAME);
			
			final TextView gpsText = (TextView) findViewById(R.id.gps_text);
			if(gpsText != null){
				gpsText.setText("");
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

	@Override
	protected void onResume() {
		final SharedPreferences pref = getPreferences(Activity.MODE_PRIVATE);
		
		if(mTileSource != null)
			mTileSource.Free();

		try {
			mTileSource = new TileSource(this, pref.getString(MAPNAMEAREASELECTOR, TileSource.MAPNIK));
		} catch (Exception e) {
			e.printStackTrace();
		}
		mMap.setTileSource(mTileSource);
 		mMap.getController().setZoom(pref.getInt("ZoomLevelAS", 0));
 		mMap.getController().setCenter(new GeoPoint(pref.getInt("LatitudeAS", 0), pref.getInt("LongitudeAS", 0)));
 		setTitle();
 		
 		int lat1, lon1, lat2, lon2;
 		lat1 = pref.getInt("LatitudeAS1", 0);
 		lon1 = pref.getInt("LongitudeAS1", 0);
 		if(lat1+lon1 == 0)
 			mAreaSelectorOverlay.Init(this, mMap.getTileView());
 		
		super.onResume();
	}

	@Override
	protected void onPause() {
		SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = uiState.edit();
		editor.putString(MAPNAMEAREASELECTOR, mTileSource.ID);
		final GeoPoint point = mMap.getMapCenter();
		editor.putInt("LatitudeAS", point.getLatitudeE6());
		editor.putInt("LongitudeAS", point.getLongitudeE6());
		editor.putInt("ZoomLevelAS", mMap.getZoomLevel());
		editor.commit();

		super.onPause();
	}
}
