package com.robert.maps.downloader;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.andnav.osm.util.GeoPoint;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.robert.maps.R;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.XMLparser.PredefMapsParser;
import com.robert.maps.tileprovider.TileSource;
import com.robert.maps.tileprovider.TileSourceBase;
import com.robert.maps.utils.RException;
import com.robert.maps.utils.Ut;
import com.robert.maps.view.IMoveListener;
import com.robert.maps.view.MapView;
import com.robert.maps.view.TileViewOverlay;

public class AreaSelectorActivity extends Activity {
	private static final String MAPNAME = "MapName";
	private static final String MAPNAMEAREASELECTOR = "MapNameAreaSelector";

	private MapView mMap;
	private AreaSelectorOverlay mAreaSelectorOverlay;
	private TileSource mTileSource;
	private MoveListener mMoveListener = new MoveListener();
	private int[] mZoomArr = new int[0];

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
		
		findViewById(R.id.clear).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mAreaSelectorOverlay.clearArea(mMap.getTileView());
			}});
		registerForContextMenu(findViewById(R.id.maps));
		findViewById(R.id.maps).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				v.showContextMenu();
			}
		});
		findViewById(R.id.start_download).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startDownLoad();
			}
		});
		findViewById(R.id.stop_download).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				stopDownLoad();
			}
		});
		findViewById(R.id.next).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doNext();
			}
		});
		findViewById(R.id.back).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doBack();
			}
		});
		
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
	
	protected void doBack() {
		getZoomArr();
		LinearLayout ll = (LinearLayout) findViewById(R.id.LayerArea);
		ll.removeAllViews();
		
		findViewById(R.id.step1).setVisibility(View.VISIBLE);
		findViewById(R.id.step2).setVisibility(View.GONE);
	}
	
	private int[] getZoomArr() {
		LinearLayout ll = (LinearLayout) findViewById(R.id.LayerArea);
		CheckBox cb;
		final int[] zoomArr = new int[mTileSource.ZOOM_MAXLEVEL - mTileSource.ZOOM_MINLEVEL + 1];
		int j = 0;
		for(int i = mTileSource.ZOOM_MINLEVEL; i <= mTileSource.ZOOM_MAXLEVEL; i++) {
			cb = (CheckBox) ll.findViewWithTag("Layer"+i);
			if(cb != null)
				if(cb.isChecked()) {
					zoomArr[j] = i;
					j++;
				}
		}
		mZoomArr = new int[j];
		for(;j > 0; j--) {
			mZoomArr[j-1] = zoomArr[j-1];
		}
		
		return mZoomArr;
	}

	protected void doNext() {
		LinearLayout ll = (LinearLayout) findViewById(R.id.LayerArea);
		CheckBox cb;
		for(int i = mTileSource.ZOOM_MINLEVEL; i <= mTileSource.ZOOM_MAXLEVEL; i++) {
			cb = new CheckBox(this);
			cb.setTag("Layer"+i);
			Ut.w((String)cb.getTag());
			cb.setText("Zoom "+(i+1));
			ll.addView(cb);
		}
		for(int i = 0; i < mZoomArr.length; i++) {
			cb = (CheckBox) ll.findViewWithTag("Layer"+mZoomArr[i]);
			if(cb != null)
				cb.setChecked(true);
		}
		
		findViewById(R.id.step1).setVisibility(View.GONE);
		findViewById(R.id.step2).setVisibility(View.VISIBLE);
	}

	private void startDownLoad() {
		findViewById(R.id.start_download).setVisibility(View.GONE);
		//findViewById(R.id.stop_download).setVisibility(View.VISIBLE);
		
		final Intent intent = new Intent("com.robert.maps.mapdownloader");
		intent.putExtra("ZOOM", getZoomArr());
		intent.putExtra("COORD", mAreaSelectorOverlay.getCoordArr());
		intent.putExtra("MAPID", mTileSource.ID);
		intent.putExtra("OFFLINEMAPNAME", "TESTMAPNAME");
		startService(intent);
		
		final GeoPoint point = mMap.getMapCenter();
		startActivity(new Intent(this, DownloaderActivity.class)
			.putExtra("MAPID", mTileSource.ID)
			.putExtra("Latitude", point.getLatitudeE6())
			.putExtra("Longitude", point.getLongitudeE6())
			.putExtra("ZoomLevel", mMap.getZoomLevel())
			);
		finish();
	}

	private void stopDownLoad() {
//		findViewById(R.id.stop_download).setVisibility(View.GONE);
//		findViewById(R.id.start_download).setVisibility(View.VISIBLE);
//		
//		stopService(new Intent("com.robert.maps.mapdownloader"));
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
			if(mTileSource.MAP_TYPE != TileSourceBase.PREDEF_ONLINE && mTileSource.MAP_TYPE != TileSourceBase.MIXMAP_CUSTOM) {
				mTileSource.Free();
				mTileSource = new TileSource(this, TileSource.MAPNIK);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		mMap.setTileSource(mTileSource);
 		mMap.getController().setZoom(pref.getInt("ZoomLevelAS", 0));
 		mMap.getController().setCenter(new GeoPoint(pref.getInt("LatitudeAS", 0), pref.getInt("LongitudeAS", 0)));
 		setTitle();
 		
 		int lat1, lon1;
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

	@Override
	protected void onDestroy() {
		for (TileViewOverlay osmvo : mMap.getOverlays())
			osmvo.Free();

		mTileSource.Free();
		mTileSource = null;
		mMap.setMoveListener(null);
		
		super.onDestroy();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if(v.getId() == R.id.maps) {
			menu.clear();
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

			final PoiManager poiManager = new PoiManager(this);
			Cursor c = poiManager.getGeoDatabase().getMixedMaps();
			if(c != null) {
				if(c.moveToFirst()) {
					do {
						if (pref.getBoolean("PREF_MIXMAPS_" + c.getInt(0) + "_enabled", false) && c.getInt(2) < 3) {
							MenuItem item = menu.add(c.getString(1));
							item.setTitleCondensed("mixmap_" + c.getInt(0));
						}
					} while(c.moveToNext());
				}
				c.close();
			}
			poiManager.FreeDatabases();

			final SAXParserFactory fac = SAXParserFactory.newInstance();
			SAXParser parser = null;
			try {
				parser = fac.newSAXParser();
				if(parser != null){
					final InputStream in = getResources().openRawResource(R.raw.predefmaps);
					parser.parse(in, new PredefMapsParser(menu, pref));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final String mapid = (String)item.getTitleCondensed();
		if(mTileSource != null)
			mTileSource.Free();
		try {
			mTileSource = new TileSource(this, mapid);
		} catch (RException e) {
			//addMessage(e);
		}
		mMap.setTileSource(mTileSource);
		
        setTitle();

		return true;
	}

}
