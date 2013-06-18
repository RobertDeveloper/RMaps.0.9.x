package com.robert.maps.applib.downloader;

import java.io.File;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.util.Util;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.robert.maps.applib.R;
import com.robert.maps.applib.kml.PoiManager;
import com.robert.maps.applib.kml.XMLparser.PredefMapsParser;
import com.robert.maps.applib.tileprovider.TileProviderInet;
import com.robert.maps.applib.tileprovider.TileSource;
import com.robert.maps.applib.tileprovider.TileSourceBase;
import com.robert.maps.applib.utils.RException;
import com.robert.maps.applib.utils.Ut;
import com.robert.maps.applib.view.IMoveListener;
import com.robert.maps.applib.view.MapView;
import com.robert.maps.applib.view.TileViewOverlay;

public class AreaSelectorActivity extends Activity {
	private static final String MAPNAME = "MapName";
	private static final String MAPNAMEAREASELECTOR = "MapNameAreaSelector";

	private MapView mMap;
	private AreaSelectorOverlay mAreaSelectorOverlay;
	private TileSource mTileSource;
	private MoveListener mMoveListener = new MoveListener();
	private int[] mZoomArr = new int[0];
//	ServiceConnection mConnection;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
//		mConnection = new ServiceConnection() {
//			public void onServiceConnected(ComponentName className, IBinder service) {
//				Toast.makeText(AreaSelectorActivity.this, R.string.downloader_notif_text, Toast.LENGTH_LONG).show();
//				AreaSelectorActivity.this.finish();
//			}
//			public void onServiceDisconnected(ComponentName className) {}
//		};

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
		((CheckBox) findViewById(R.id.online_cache)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				onOnlineCacheBoxChecked();
			}
		});
		
		Intent intent = getIntent();
		if(intent != null) {
			SharedPreferences.Editor editor = uiState.edit();
			editor.putString(MAPNAMEAREASELECTOR, intent.getStringExtra(MAPNAME));
			editor.putInt("ZoomLevelAS", intent.getIntExtra("ZoomLevel", 0));

			if(intent.getBooleanExtra("new", false)) {
				intent.putExtra("new", false);
				editor.putInt("LatitudeAS", intent.getIntExtra("Latitude", 0));
				editor.putInt("LongitudeAS", intent.getIntExtra("Longitude", 0));
				editor.putInt("LatitudeAS1", 0);
				editor.putInt("LongitudeAS1", 0);
				editor.putInt("LatitudeAS2", 0);
				editor.putInt("LongitudeAS2", 0);
				editor.putBoolean("step2", false);
			};
			
			editor.commit();
			
		}
	}
	
	protected void doBack() {
		getZoomArr();
		((LinearLayout) findViewById(R.id.LayerArea1)).removeAllViews();
		((LinearLayout) findViewById(R.id.LayerArea2)).removeAllViews();
		
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
		LinearLayout ll1 = (LinearLayout) findViewById(R.id.LayerArea1);
		LinearLayout ll2 = (LinearLayout) findViewById(R.id.LayerArea2);
		CheckBox cb;
		final double tileLength = ((TileProviderInet) mTileSource.getTileProvider()).getTileLength();
		final int[] coordArr = mAreaSelectorOverlay.getCoordArr();
		
		for(int i = mTileSource.ZOOM_MINLEVEL; i <= mTileSource.ZOOM_MAXLEVEL; i++) {
			final int c0[] = Util.getMapTileFromCoordinates(coordArr[0], coordArr[1], i, null, mTileSource.PROJECTION);
			final int c1[] = Util.getMapTileFromCoordinates(coordArr[2], coordArr[3], i, null, mTileSource.PROJECTION);
			final int yMin = Math.min(c0[0], c1[0]);
			final int yMax = Math.max(c0[0], c1[0]);
			final int xMin = Math.min(c0[1], c1[1]);
			final int xMax = Math.max(c0[1], c1[1]);
			final int tileCnt = (yMax - yMin + 1) * (xMax - xMin + 1);

			cb = new CheckBox(this);
			cb.setTag("Layer"+i);
			cb.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
			cb.setText("Zoom "+(i+1)+"\n"+tileCnt+" tiles, ~"+Ut.formatSize(tileCnt * tileLength));
			
			if(i - mTileSource.ZOOM_MINLEVEL + 1 > (int)((mTileSource.ZOOM_MAXLEVEL - mTileSource.ZOOM_MINLEVEL + 1) / 2.0 + 0.5))
				ll2.addView(cb);
			else
				ll1.addView(cb);
		}
		for(int i = 0; i < mZoomArr.length; i++) {
			cb = (CheckBox) ll1.findViewWithTag("Layer"+mZoomArr[i]);
			if(cb != null)
				cb.setChecked(true);
			cb = (CheckBox) ll2.findViewWithTag("Layer"+mZoomArr[i]);
			if(cb != null)
				cb.setChecked(true);
		}
		
		findViewById(R.id.step1).setVisibility(View.GONE);
		findViewById(R.id.step2).setVisibility(View.VISIBLE);
	}

	private void startDownLoad() {
		getZoomArr();
		
		if(mZoomArr.length == 0) {
			Toast.makeText(this, R.string.select_zoom, Toast.LENGTH_LONG).show();
			return;
		}
			
		findViewById(R.id.start_download).setVisibility(View.GONE);
		
		final Intent intent = new Intent("com.robert.maps.mapdownloader");
		intent.putExtra("ZOOM", mZoomArr);
		intent.putExtra("COORD", mAreaSelectorOverlay.getCoordArr());
		intent.putExtra("MAPID", mTileSource.ID);
		intent.putExtra("ZOOMCUR", mMap.getZoomLevel());
		intent.putExtra("overwritefile", ((CheckBox) findViewById(R.id.overwritefile)).isChecked());
		intent.putExtra("overwritetiles", ((CheckBox) findViewById(R.id.overwritetiles)).isChecked());
		intent.putExtra("online_cache", ((CheckBox) findViewById(R.id.online_cache)).isChecked());
		
		String filename = ((EditText) findViewById(R.id.name)).getText().toString();
		if(!((CheckBox) findViewById(R.id.online_cache)).isChecked()) {
			if(filename.equalsIgnoreCase("")) {
				Toast.makeText(this, "Invalid file name", Toast.LENGTH_LONG).show();
				return;
			}
	
			final File folder = Ut.getRMapsMapsDir(this);
			if(folder != null) {
				File[] files = folder.listFiles();
				if(files != null) {
					for(int i = 0; i < files.length; i++) {
						if(files[i].getName().equalsIgnoreCase(filename+".sqlitedb")) {
							filename = files[i].getName().substring(0, files[i].getName().length() - 9);
							break;
						}
					}
				}
			}
		}
		
		intent.putExtra("OFFLINEMAPNAME", filename);
		
		startService(intent);
		
		final GeoPoint point = mMap.getMapCenter();
		startActivity(new Intent(this, DownloaderActivity.class)
			.putExtra("MAPID", mTileSource.ID)
			.putExtra("Latitude", point.getLatitudeE6())
			.putExtra("Longitude", point.getLongitudeE6())
			.putExtra("ZoomLevel", mMap.getZoomLevel())
			.putExtra("OFFLINEMAPNAME", filename)
			.putExtra("online_cache", ((CheckBox) findViewById(R.id.online_cache)).isChecked())
			);
		finish();
	}

	private class MoveListener implements IMoveListener {

		public void onMoveDetected() {
		}

		public void onZoomDetected() {
			setTitle();
		}

		@Override
		public void onCenterDetected() {
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
//		bindService(new Intent(IRemoteService.class.getName()), mConnection, 0); 
		
		final SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
		
		if(mTileSource != null)
			mTileSource.Free();

		try {
			mTileSource = new TileSource(this, uiState.getString(MAPNAMEAREASELECTOR, TileSource.MAPNIK));
			if(mTileSource.MAP_TYPE != TileSourceBase.PREDEF_ONLINE && mTileSource.MAP_TYPE != TileSourceBase.MIXMAP_CUSTOM) {
				mTileSource.Free();
				mTileSource = new TileSource(this, TileSource.MAPNIK);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		mMap.setTileSource(mTileSource);
 		mMap.getController().setZoom(uiState.getInt("ZoomLevelAS", 0));
 		mMap.getController().setCenter(new GeoPoint(uiState.getInt("LatitudeAS", 0), uiState.getInt("LongitudeAS", 0)));
 		setTitle();
 		
 		final GeoPoint[] p = new GeoPoint[2];
 		p[0] = new GeoPoint(uiState.getInt("LatitudeAS1", 0), uiState.getInt("LongitudeAS1", 0));
 		p[1] = new GeoPoint(uiState.getInt("LatitudeAS2", 0), uiState.getInt("LongitudeAS2", 0));
		mAreaSelectorOverlay.Init(this, mMap.getTileView(), p);
		((EditText) findViewById(R.id.name)).setText(uiState.getString("filename", "NewFile"));
		((CheckBox) findViewById(R.id.overwritefile)).setChecked(uiState.getBoolean("overwritefile", true));
		((CheckBox) findViewById(R.id.overwritetiles)).setChecked(uiState.getBoolean("overwritetiles", false));
		((CheckBox) findViewById(R.id.online_cache)).setChecked(uiState.getBoolean("online_cache", false));
		
		onOnlineCacheBoxChecked();
		
		mZoomArr = new int[uiState.getInt("zoomCnt", 0)];
		for(int i = 0; i < mZoomArr.length; i++) {
			mZoomArr[i] = uiState.getInt("zoom"+i, 0);
		}
		if(uiState.getBoolean("step2", false)) {
			doNext();
		} else {
			findViewById(R.id.step1).setVisibility(View.VISIBLE);
			findViewById(R.id.step2).setVisibility(View.GONE);
		}
 		
		super.onResume();
	}

	private void onOnlineCacheBoxChecked() {
		final boolean isOnlineCache = ((CheckBox) findViewById(R.id.online_cache)).isChecked();
		findViewById(R.id.name).setVisibility(isOnlineCache ? View.GONE : View.VISIBLE);
		findViewById(R.id.overwritefile).setVisibility(isOnlineCache ? View.GONE : View.VISIBLE);
		findViewById(R.id.fileNameTitle).setVisibility(isOnlineCache ? View.GONE : View.VISIBLE);
	}

	@Override
	protected void onPause() {
//		unbindService(mConnection);

		SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = uiState.edit();
		editor.putString(MAPNAMEAREASELECTOR, mTileSource.ID);
		final GeoPoint point = mMap.getMapCenter();
		editor.putBoolean("new", false);
		editor.putInt("LatitudeAS", point.getLatitudeE6());
		editor.putInt("LongitudeAS", point.getLongitudeE6());
		editor.putInt("ZoomLevelAS", mMap.getZoomLevel());
		editor.putString("filename", ((EditText) findViewById(R.id.name)).getText().toString());
		editor.putBoolean("overwritefile", ((CheckBox) findViewById(R.id.overwritefile)).isChecked());
		editor.putBoolean("overwritetiles", ((CheckBox) findViewById(R.id.overwritetiles)).isChecked());
		editor.putBoolean("online_cache", ((CheckBox) findViewById(R.id.online_cache)).isChecked());
		
		mAreaSelectorOverlay.put(editor);
		if(findViewById(R.id.step2).getVisibility() == View.VISIBLE) {
			editor.putBoolean("step2", true);
			getZoomArr();
		} else {
			editor.putBoolean("step2", false);
		}
		editor.putInt("zoomCnt", mZoomArr.length);
		for(int i = 0; i < mZoomArr.length; i++) {
			editor.putInt("zoom"+i, mZoomArr[i]);
		}
		editor.commit();

		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if(mMap != null) {
			for (TileViewOverlay osmvo : mMap.getOverlays())
				osmvo.Free();
			mMap.setMoveListener(null);
		}

		if(mTileSource != null)
			mTileSource.Free();
		mTileSource = null;
		
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
						if (pref.getBoolean("PREF_MIXMAPS_" + c.getInt(0) + "_enabled", true) && c.getInt(2) == 2) { // Only ownsourcemap
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
					in.close();

					final InputStream in2 = getResources().openRawResource(R.raw.predefmaps);
					parser.parse(in2, new PredefMapsParser(menu, pref, true, mTileSource.PROJECTION));
					in2.close();
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK && findViewById(R.id.step2).getVisibility() == View.VISIBLE) {
			doBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
