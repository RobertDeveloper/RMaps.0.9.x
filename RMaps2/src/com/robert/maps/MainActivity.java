package com.robert.maps;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.TypeConverter;
import org.andnav.osm.views.util.StreamUtils;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.provider.SearchRecentSuggestions;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.robert.maps.downloader.AreaSelectorActivity;
import com.robert.maps.kml.PoiActivity;
import com.robert.maps.kml.PoiListActivity;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.PoiPoint;
import com.robert.maps.kml.Track;
import com.robert.maps.kml.TrackListActivity;
import com.robert.maps.kml.XMLparser.PredefMapsParser;
import com.robert.maps.overlays.CurrentTrackOverlay;
import com.robert.maps.overlays.MyLocationOverlay;
import com.robert.maps.overlays.PoiOverlay;
import com.robert.maps.overlays.SearchResultOverlay;
import com.robert.maps.overlays.TrackOverlay;
import com.robert.maps.overlays.YandexTrafficOverlay;
import com.robert.maps.tileprovider.TileSource;
import com.robert.maps.utils.CompassView;
import com.robert.maps.utils.CrashReportHandler;
import com.robert.maps.utils.RException;
import com.robert.maps.utils.SearchSuggestionsProvider;
import com.robert.maps.utils.Ut;
import com.robert.maps.view.IMoveListener;
import com.robert.maps.view.MapView;
import com.robert.maps.view.TileView;
import com.robert.maps.view.TileViewOverlay;

public class MainActivity extends Activity {
	private static final String MAPNAME = "MapName";
	private static final String ACTION_SHOW_POINTS = "com.robert.maps.action.SHOW_POINTS";
	
	private MapView mMap;
	private ImageView ivAutoFollow;
	private CompassView mCompassView;

	private TileSource mTileSource;
	private PoiManager mPoiManager;
	private Handler mCallbackHandler = new MainActivityCallbackHandler();
	private MoveListener mMoveListener = new MoveListener();
	private SensorManager mOrientationSensorManager;
	private PowerManager.WakeLock myWakeLock;
	
	// Overlays
	private YandexTrafficOverlay mYandexTrafficOverlay = null;
	private boolean mShowOverlay = false;
	private String mOverlayId = "";
	private MyLocationOverlay mMyLocationOverlay;
	private PoiOverlay mPoiOverlay;
	private TrackOverlay mTrackOverlay;
	private CurrentTrackOverlay mCurrentTrackOverlay;
	private SearchResultOverlay mSearchResultOverlay;

	private int mMarkerIndex;
	private boolean mAutoFollow = true;
	private String mGpsStatusName = "";
	private int mGpsStatusSatCnt = 0;
	private int mGpsStatusState = 0;
	private float mLastSpeed, mLastBearing;
	private boolean mCompassEnabled;
	private boolean mDrivingDirectionUp;
	private boolean mNorthDirectionUp;
	
	private GoogleAnalyticsTracker mTracker;
	private ImageView mLayerView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        if(!OpenStreetMapViewConstants.DEBUGMODE)
        	CrashReportHandler.attach(this);

		mTracker = GoogleAnalyticsTracker.getInstance();
		mTracker.startNewSession("UA-10715419-3", 20, this);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		CreateContentView();
		
		mPoiManager = new PoiManager(this);
		mLocationListener = new SampleLocationListener();
		mMap.setMoveListener(mMoveListener);
		mOrientationSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
 		SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
 		
 		// Init 
		mCompassEnabled = uiState.getBoolean("CompassEnabled", false);
		mCompassView.setVisibility(mCompassEnabled ? View.VISIBLE : View.INVISIBLE);
		mAutoFollow = uiState.getBoolean("AutoFollow", true);
		
		mMap.getController().setCenter(new GeoPoint(uiState.getInt("Latitude", 0), uiState.getInt("Longitude", 0)));
		mGPSFastUpdate = pref.getBoolean("pref_gpsfastupdate", true);
		mAutoFollow = uiState.getBoolean("AutoFollow", true);
		setAutoFollow(mAutoFollow, true);

		this.mTrackOverlay = new TrackOverlay(this, mPoiManager, mCallbackHandler);
		this.mCurrentTrackOverlay = new CurrentTrackOverlay(this, mPoiManager);
		this.mPoiOverlay = new PoiOverlay(this, mPoiManager, null, pref.getBoolean("pref_hidepoi", false));
		mPoiOverlay.setTapIndex(uiState.getInt("curShowPoiId", -1));
        this.mMyLocationOverlay = new MyLocationOverlay(this);
        this.mSearchResultOverlay = new SearchResultOverlay(this);
        mSearchResultOverlay.fromPref(uiState);
        FillOverlays();
		
		mDrivingDirectionUp = pref.getBoolean("pref_drivingdirectionup", true);
		mNorthDirectionUp = pref.getBoolean("pref_northdirectionup", true);

		final int screenOrientation = Integer.parseInt(pref.getString("pref_screen_orientation", "-1"));
		setRequestedOrientation(screenOrientation);

     	final boolean fullScreen = pref.getBoolean("pref_showstatusbar", true);
		if (fullScreen)
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		else
			getWindow()
					.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


		if(uiState.getString("error", "").length() > 0){
			showDialog(R.id.error);
		}

		
		if (!uiState.getString("app_version", "").equalsIgnoreCase(Ut.getAppVersion(this))) {
			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
			
			mTracker.setCustomVar(1, "Build", Ut.getAppVersion(this), 1);
			mTracker.setCustomVar(2, "Ver", "Free", 1);
			mTracker.setCustomVar(3, "DisplaySize", ""+Math.min(metrics.widthPixels, metrics.heightPixels)+"x"+Math.max(metrics.widthPixels, metrics.heightPixels), 1);
			mTracker.setCustomVar(4, "DisplayDensity", ""+(int)(160*metrics.density), 1);
			mTracker.setCustomVar(5, "APILevel", Build.VERSION.SDK, 1);
			mTracker.trackPageView("/InstallApp");
			
			showDialog(R.id.whatsnew);
		}
		
		final Intent queryIntent = getIntent();
		final String queryAction = queryIntent.getAction();

		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			doSearchQuery(queryIntent);
		} else if (ACTION_SHOW_POINTS.equalsIgnoreCase(queryAction)) {
			ActionShowPoints(queryIntent);
		} else if (Intent.ACTION_VIEW.equalsIgnoreCase(queryAction)) {
			Uri uri = queryIntent.getData();
			if(uri.getScheme().equalsIgnoreCase("geo")) {
				final String latlon = uri.getEncodedSchemeSpecificPart().replace("?"+uri.getEncodedQuery(), "");
				if(latlon.equals("0,0")) {
					final String query = uri.getEncodedQuery().replace("q=", "");
					queryIntent.putExtra(SearchManager.QUERY, query);
					doSearchQuery(queryIntent);
					
				} else {
					GeoPoint point = GeoPoint.fromDoubleString(latlon);
					mPoiOverlay.setGpsStatusGeoPoint(point, "GEO", "");
					setAutoFollow(false);
					mMap.getController().setCenter(point);
				}
			}
		}
	}

//	@Override
//	public boolean onSearchRequested() {
//        startSearch("", false, null, false);
//		return true;
//	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

        final String queryAction = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(queryAction)) {
            doSearchQuery(intent);
        } else if (ACTION_SHOW_POINTS.equalsIgnoreCase(queryAction))
			ActionShowPoints(intent);

	}

	private void doSearchQuery(Intent queryIntent) {
		mSearchResultOverlay.Clear();
		this.mMap.invalidate();

		final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);

        // Record the query string in the recent queries suggestions provider.
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
        suggestions.saveRecentQuery(queryString, null);

		InputStream in = null;
		OutputStream out = null;

		try {
			final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
			URL url = new URL(
					"http://ajax.googleapis.com/ajax/services/search/local?v=1.0&sll="
							+ this.mMap.getMapCenter().toDoubleString()
							+ "&q=" + URLEncoder.encode(queryString, "UTF-8")
							+ "&hl="+ pref.getString("pref_googlelanguagecode", "en")
							+ "");
			Ut.dd(url.toString());
			in = new BufferedInputStream(url.openStream(), StreamUtils.IO_BUFFER_SIZE);

			final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
			out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
			StreamUtils.copy(in, out);
			out.flush();

			String str = dataStream.toString();
			JSONObject json = new JSONObject(str);
			//Ut.dd(json.toString(4)); //
			JSONArray results = (JSONArray) ((JSONObject) json.get("responseData")).get("results");
			//Ut.dd("results.length="+results.length());
			if(results.length() == 0){
				Toast.makeText(this, R.string.no_items, Toast.LENGTH_SHORT).show();
				return;
			}
			JSONObject res = results.getJSONObject(0);
			//Ut.dd(res.toString(4));
			//Toast.makeText(this, res.getString("titleNoFormatting"), Toast.LENGTH_LONG).show();
			final String address = res.getString("addressLines").replace("\"", "").replace("[", "").replace("]", "").replace(",", ", ").replace("  ", " ");
			//Toast.makeText(this, address, Toast.LENGTH_LONG).show();
			//Toast.makeText(this, ((JSONObject) json.get("addressLines")).toString(), Toast.LENGTH_LONG).show();

			setAutoFollow(false, true);
			final GeoPoint point = new GeoPoint((int)(res.getDouble("lat")* 1E6), (int)(res.getDouble("lng")* 1E6));
			this.mSearchResultOverlay.setLocation(point, address);
			this.mMap.getController().setZoom((int) (2 * res.getInt("accuracy")));
			mMap.getController().setCenter(point);
			//this.mOsmv.getController().animateTo(new GeoPoint((int)(res.getDouble("lat")* 1E6), (int)(res.getDouble("lng")* 1E6)), OpenStreetMapViewController.AnimationType.MIDDLEPEAKSPEED, OpenStreetMapViewController.ANIMATION_SMOOTHNESS_HIGH, OpenStreetMapViewController.ANIMATION_DURATION_DEFAULT);

			setTitle();

		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.no_inet_conn, Toast.LENGTH_LONG).show();
		} finally {
			StreamUtils.closeStream(in);
			StreamUtils.closeStream(out);
		}
	}

	private View CreateContentView() {
		setContentView(R.layout.main);
		
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		final RelativeLayout rl = (RelativeLayout) findViewById(R.id.map_area);
		final int sideBottom = Integer.parseInt(pref.getString("pref_zoomctrl", "1"));
		final boolean showTitle = pref.getBoolean("pref_showtitle", true);
		
		if(!showTitle) 
			findViewById(R.id.screen).setVisibility(View.GONE);
		
		mMap = new MapView(this, Integer.parseInt(pref.getString("pref_zoomctrl", "1")), pref.getBoolean("pref_showscalebar", true) ? 1 : 0);
		mMap.setId(R.id.main);
		final RelativeLayout.LayoutParams pMap = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        rl.addView(mMap, pMap);
		
        mCompassView = new CompassView(this, sideBottom == 2 ? false : true);
        mCompassView.setVisibility(mCompassEnabled ? View.VISIBLE : View.INVISIBLE);

        final RelativeLayout.LayoutParams compassParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        compassParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        compassParams.addRule(!(sideBottom == 2 ? false : true) ? RelativeLayout.ALIGN_BOTTOM : RelativeLayout.ALIGN_TOP, R.id.main);
        rl.addView(mCompassView, compassParams);

        ivAutoFollow = new ImageView(this);
        ivAutoFollow.setImageResource(R.drawable.autofollow);
        ivAutoFollow.setVisibility(ImageView.INVISIBLE);

        final RelativeLayout.LayoutParams followParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        followParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        if(!(sideBottom == 2 ? false : true))
        	followParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        else
        	followParams.addRule(RelativeLayout.ALIGN_TOP, R.id.main);
        rl.addView(ivAutoFollow, followParams);

        ivAutoFollow.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				setAutoFollow(true);
				mSearchResultOverlay.Clear();
				setLastKnownLocation();
			}
        });
        
        mLayerView = new ImageView(this);
        mLayerView.setImageResource(R.drawable.zoom_out);
        final RelativeLayout.LayoutParams layerParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layerParams.addRule(RelativeLayout.CENTER_VERTICAL);
       	layerParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rl.addView(mLayerView, layerParams);
        
        mLayerView.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				if(mTileSource.YANDEX_TRAFFIC_ON == 1) {
					mShowOverlay = !mShowOverlay;
					FillOverlays();
				} else {
					final String mapId = mTileSource.ID;

					if(mShowOverlay) {
						mShowOverlay = !mShowOverlay;
						mOverlayId = mTileSource.getOverlayName();
						
						if(mTileSource != null)
							mTileSource.Free();
						try {
							mTileSource = new TileSource(MainActivity.this, mapId);
						} catch (RException e) {
							addMessage(e);
						}
						mMap.setTileSource(mTileSource);
						
						FillOverlays();
				        setTitle();
					} else if(!mOverlayId.equalsIgnoreCase("")) {
						mShowOverlay = !mShowOverlay;

						if(mTileSource != null)
							mTileSource.Free();
						try {
							mTileSource = new TileSource(MainActivity.this, mapId, mOverlayId);
						} catch (RException e) {
							addMessage(e);
						}
						mMap.setTileSource(mTileSource);
						
						FillOverlays();
				        setTitle();
					} else
						v.showContextMenu();
				}
				
				mMap.postInvalidate();
			}
		});
        mLayerView.setOnLongClickListener(new View.OnLongClickListener() {
			
			public boolean onLongClick(View v) {
				if(mTileSource.YANDEX_TRAFFIC_ON != 1)
					v.showContextMenu();
				return true;
			}
		});
        mLayerView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(R.string.menu_title_overlays);
				menu.add(Menu.NONE, R.id.hide_overlay, Menu.NONE, R.string.menu_hide_overlay);
				
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

				File folder = Ut.getRMapsMapsDir(MainActivity.this);
				if (folder.exists()) {
					File[] files = folder.listFiles();
					if (files != null)
						for (int i = 0; i < files.length; i++) {
							if (files[i].getName().toLowerCase().endsWith(".mnm")
									|| files[i].getName().toLowerCase().endsWith(".tar")
									|| files[i].getName().toLowerCase().endsWith(".sqlitedb")) {
								String name = Ut.FileName2ID(files[i].getName());
								if (pref.getBoolean("pref_usermaps_" + name + "_enabled", false) && pref.getBoolean("pref_usermaps_" + name + "_isoverlay", false)) {
									MenuItem item = menu.add(Menu.NONE, R.id.isoverlay, Menu.NONE, pref.getString("pref_usermaps_" + name + "_name", files[i].getName()));
									item.setTitleCondensed("usermap_" + name);
								}
							}
						}
				}

				final SAXParserFactory fac = SAXParserFactory.newInstance();
				SAXParser parser = null;
				try {
					parser = fac.newSAXParser();
					if(parser != null){
						final InputStream in = getResources().openRawResource(R.raw.predefmaps);
						parser.parse(in, new PredefMapsParser(menu, pref, true, mTileSource.PROJECTION));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				
				
			}
		});
        
        registerForContextMenu(mMap);
 		
		return rl;
	}
	
	private void FillOverlays() {
		this.mMap.getOverlays().clear();

		if(mTileSource == null) {
		} else if(mTileSource.YANDEX_TRAFFIC_ON == 1 && mShowOverlay && mYandexTrafficOverlay == null) {
 			mYandexTrafficOverlay = new YandexTrafficOverlay(this, mMap.getTileView());
 		} else if((mTileSource.YANDEX_TRAFFIC_ON != 1 || !mShowOverlay) && mYandexTrafficOverlay != null) {
 			mYandexTrafficOverlay.Free();
 			mYandexTrafficOverlay = null;
 		}
		
		if(mYandexTrafficOverlay != null)
			this.mMap.getOverlays().add(mYandexTrafficOverlay);
        if(mTrackOverlay != null)
        	this.mMap.getOverlays().add(mTrackOverlay);
        if(mCurrentTrackOverlay != null)
        	this.mMap.getOverlays().add(mCurrentTrackOverlay);
        if(mPoiOverlay != null)
        	this.mMap.getOverlays().add(mPoiOverlay);
        this.mMap.getOverlays().add(mMyLocationOverlay);
        this.mMap.getOverlays().add(mSearchResultOverlay);
	}

	private void setAutoFollow(boolean autoFollow) {
		setAutoFollow(autoFollow, false);
	}

	private void setAutoFollow(boolean autoFollow, final boolean supressToast) {
		mAutoFollow = autoFollow;

		if (autoFollow) {
			ivAutoFollow.setVisibility(ImageView.INVISIBLE);
			if(!supressToast)
				Toast.makeText(this, R.string.auto_follow_enabled, Toast.LENGTH_SHORT).show();
		} else {
			ivAutoFollow.setVisibility(ImageView.VISIBLE);
			if(!supressToast)
				Toast.makeText(this, R.string.auto_follow_disabled, Toast.LENGTH_SHORT).show();
		}
	}

	private void setLastKnownLocation() {
		final GeoPoint p = mMyLocationOverlay.getLastGeoPoint();
		if(p != null)
			mMap.getController().setCenter(p);
		else {
			final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			final Location loc1 = lm.getLastKnownLocation(SampleLocationListener.GPS);
			final Location loc2 = lm.getLastKnownLocation(SampleLocationListener.NETWORK);
	
			boolean boolGpsEnabled = lm.isProviderEnabled(SampleLocationListener.GPS);
			boolean boolNetworkEnabled = lm.isProviderEnabled(SampleLocationListener.NETWORK);
			String str = "";
			Location loc = null;
	
			if(loc1 == null && loc2 != null)
				loc = loc2;
			else if (loc1 != null && loc2 == null)
				loc = loc1;
			else if (loc1 == null && loc2 == null)
				loc = null;
			else
				loc = loc1.getTime() > loc2.getTime() ? loc1 : loc2;
	
			if(boolGpsEnabled){}
			else if(boolNetworkEnabled)
				str = getString(R.string.message_gpsdisabled);
			else if(loc == null)
				str = getString(R.string.message_locationunavailable);
			else
				str = getString(R.string.message_lastknownlocation);
	
			if(str.length() > 0)
				Toast.makeText(this, str, Toast.LENGTH_LONG).show();
			
			if(loc != null)
				mMap.getController().setCenter(TypeConverter.locationToGeoPoint(loc));
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

	@Override
	protected void onResume() {
		final SharedPreferences pref = getPreferences(Activity.MODE_PRIVATE);
		
		mShowOverlay = pref.getBoolean("ShowOverlay", true);
		mOverlayId = pref.getString("OverlayID", "");
		
		if(mTileSource != null)
			mTileSource.Free();
		try {
			if(mShowOverlay && !mOverlayId.equalsIgnoreCase(""))
				mTileSource = new TileSource(this, pref.getString(MAPNAME, TileSource.MAPNIK), mOverlayId);
			else
				mTileSource = new TileSource(this, pref.getString(MAPNAME, TileSource.MAPNIK));
		} catch (RException e) {
			addMessage(e);
		}
		mMap.setTileSource(mTileSource);
 		mMap.getController().setZoom(pref.getInt("ZoomLevel", 0));
 		setTitle();
 		
 		FillOverlays();
	
		if(mCompassEnabled)
			mOrientationSensorManager.registerListener(mListener, mOrientationSensorManager
				.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);

		if(mTrackOverlay != null)
			mTrackOverlay.setStopDraw(false);
		if(mCurrentTrackOverlay != null)
			mCurrentTrackOverlay.onResume();
		
		mLocationListener.getBestProvider();

		if (pref.getBoolean("pref_keepscreenon", true)) {
		myWakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "RMaps");
		myWakeLock.acquire();
	} else {
		myWakeLock = null;
	}
		
		super.onResume();
	}

	@Override
	protected void onRestart() {
		if(mTrackOverlay != null)
			mTrackOverlay.clearTrack();
		
		super.onRestart();
	}

	@Override
	protected void onPause() {
		final GeoPoint point = mMap.getMapCenter();

		SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = uiState.edit();
		editor.putString("MapName", mTileSource.ID);
		editor.putString("OverlayID", mTileSource.getOverlayName());
		editor.putBoolean("ShowOverlay", mShowOverlay);
		editor.putInt("Latitude", point.getLatitudeE6());
		editor.putInt("Longitude", point.getLongitudeE6());
		editor.putInt("ZoomLevel", mMap.getZoomLevel());
		editor.putBoolean("CompassEnabled", mCompassEnabled);
		editor.putBoolean("AutoFollow", mAutoFollow);
		editor.putString("app_version", Ut.getAppVersion(this));
		if(mPoiOverlay != null)
			editor.putInt("curShowPoiId", mPoiOverlay.getTapIndex());
		mSearchResultOverlay.toPref(editor);
		editor.commit();
		
		uiState = getSharedPreferences("MapName", Activity.MODE_PRIVATE);
		editor = uiState.edit();
		editor.putString("MapName", mTileSource.ID);
		editor.putInt("Latitude", point.getLatitudeE6());
		editor.putInt("Longitude", point.getLongitudeE6());
		editor.putInt("ZoomLevel", mMap.getZoomLevel());
		editor.putBoolean("CompassEnabled", mCompassEnabled);
		editor.putBoolean("AutoFollow", mAutoFollow);
		editor.commit();

		if (myWakeLock != null) 
			myWakeLock.release();

		mOrientationSensorManager.unregisterListener(mListener);
		
		if(mCurrentTrackOverlay != null)
			mCurrentTrackOverlay.onPause();
		
		mPoiManager.FreeDatabases();
		
		mLocationListener.getLocationManager().removeUpdates(mLocationListener);
		if(mNetListener != null)
			mLocationListener.getLocationManager().removeUpdates(mNetListener);

		super.onPause();
	}

	@Override
	protected void onDestroy() {
		for (TileViewOverlay osmvo : mMap.getOverlays())
			osmvo.Free();

		mTileSource.Free();
		mTileSource = null;
		mMap.setMoveListener(null);
		mTracker.stopSession();
		
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_option_menu, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Menu submenu = menu.findItem(R.id.mapselector).getSubMenu();
		submenu.clear();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

		File folder = Ut.getRMapsMapsDir(this);
		if (folder.exists()) {
			File[] files = folder.listFiles();
			if (files != null)
				for (int i = 0; i < files.length; i++) {
					if (files[i].getName().toLowerCase().endsWith(".mnm")
							|| files[i].getName().toLowerCase().endsWith(".tar")
							|| files[i].getName().toLowerCase().endsWith(".sqlitedb")) {
						String name = Ut.FileName2ID(files[i].getName());
						if (pref.getBoolean("pref_usermaps_" + name + "_enabled", false)) {
							MenuItem item = submenu.add(pref.getString("pref_usermaps_" + name + "_name",
									files[i].getName()));
							item.setTitleCondensed("usermap_" + name);
						}
					}
				}
		}

		final SAXParserFactory fac = SAXParserFactory.newInstance();
		SAXParser parser = null;
		try {
			parser = fac.newSAXParser();
			if(parser != null){
				final InputStream in = getResources().openRawResource(R.raw.predefmaps);
				parser.parse(in, new PredefMapsParser(submenu, pref));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		final GeoPoint point = mMap.getMapCenter();

		switch (item.getItemId()) {
		case (R.id.area_selector):
			startActivity(new Intent(this, AreaSelectorActivity.class).putExtra(MAPNAME, mTileSource.ID).putExtra("Latitude", point.getLatitudeE6()).putExtra("Longitude", point.getLongitudeE6()).putExtra("ZoomLevel", mMap.getZoomLevel()));
			return true;
		case (R.id.gpsstatus):
			try {
				startActivity(new Intent("com.eclipsim.gpsstatus.VIEW"));
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this,
						R.string.message_nogpsstatus,
						Toast.LENGTH_LONG).show();
				try {
					startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri
							.parse("market://search?q=pname:com.eclipsim.gpsstatus2")));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			return true;
		case (R.id.poilist):
			startActivityForResult((new Intent(this, PoiListActivity.class)).putExtra("lat", point.getLatitude()).putExtra("lon", point.getLongitude()).putExtra("title", "POI"), R.id.poilist);
			return true;
		case (R.id.tracks):
			startActivityForResult(new Intent(this, TrackListActivity.class), R.id.tracks);
			return true;
		case (R.id.search):
			onSearchRequested();
			return true;
		case (R.id.settings):
			startActivityForResult(new Intent(this, MainPreferences.class), R.id.settings_activity_closed);
			return true;
		case (R.id.about):
			showDialog(R.id.about);
			return true;
		case (R.id.mapselector):
			return true;
		case (R.id.compass):
			mCompassEnabled = !mCompassEnabled;
			mCompassView.setVisibility(mCompassEnabled ? View.VISIBLE : View.INVISIBLE);
			if(mCompassEnabled)
				mOrientationSensorManager.registerListener(mListener, mOrientationSensorManager
					.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
			else {
				mOrientationSensorManager.unregisterListener(mListener);
				mMap.setBearing(0);
			};
			return true;
		case (R.id.mylocation):
			setAutoFollow(true);
			setLastKnownLocation();
			return true;
		default:
			mOverlayId = "";
			mShowOverlay = false;
			
			final String mapid = (String)item.getTitleCondensed();
			if(mTileSource != null)
				mTileSource.Free();
			try {
				mTileSource = new TileSource(this, mapid);
			} catch (RException e) {
				addMessage(e);
			}
			mMap.setTileSource(mTileSource);
			
			if(mTileSource.MAP_TYPE == TileSource.PREDEF_ONLINE) {
				mTracker.setCustomVar(1, "MAP", mapid);
				mTracker.trackPageView("/maps");
			}
			
			FillOverlays();

	        setTitle();

			return true;
		}

	}

	private void addMessage(RException e) {
		
		LinearLayout msgbox = (LinearLayout) findViewById(e.getID());
		if(msgbox == null) {
			msgbox = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.error_message_box, (ViewGroup) findViewById(R.id.message_list));
			msgbox.setId(e.getID());
		}
		msgbox.setVisibility(View.VISIBLE);
		((TextView) msgbox.findViewById(R.id.descr)).setText(e.getStringRes(this));
		msgbox.findViewById(R.id.message).setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (v.findViewById(R.id.descr).getVisibility() == View.GONE)
					v.findViewById(R.id.descr).setVisibility(View.VISIBLE);
				else
					v.findViewById(R.id.descr).setVisibility(View.GONE);
			}
		});
		msgbox.findViewById(R.id.btn).setTag(Integer.valueOf(e.getID()));
		msgbox.findViewById(R.id.btn).setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				final int id = (Integer)v.getTag();
				findViewById(id).setVisibility(View.GONE);
			}
		});
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if(menuInfo instanceof TileView.PoiMenuInfo && ((TileView.PoiMenuInfo) menuInfo).MarkerIndex >= 0) {
			menu.add(0, R.id.menu_editpoi, 0, getText(R.string.menu_edit));
			menu.add(0, R.id.menu_hide, 0, getText(R.string.menu_hide));
			menu.add(0, R.id.menu_deletepoi, 0, getText(R.string.menu_delete));
			menu.add(0, R.id.menu_toradar, 0, getText(R.string.menu_toradar));
		} else {
			menu.add(0, R.id.menu_addpoi, 0, getText(R.string.menu_addpoi));
		}

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getGroupId() == R.id.isoverlay) {
			final String overlayid = (String)item.getTitleCondensed();
			mOverlayId = overlayid;
			mShowOverlay = true;
			
			if(mTileSource != null)
				mTileSource.Free();
			try {
				mTileSource = new TileSource(this, mTileSource.ID, overlayid);
			} catch (RException e) {
				addMessage(e);
			}
			mMap.setTileSource(mTileSource);
			
			if(mTileSource.MAP_TYPE == TileSource.PREDEF_ONLINE) {
				mTracker.setCustomVar(1, "OVERLAY", overlayid);
				mTracker.trackPageView("/overlays");
			}
			
			FillOverlays();

	        setTitle();

		} else {
			switch (item.getItemId()) {
			case R.id.hide_overlay:
				mShowOverlay = false;
				
				if(mTileSource != null)
					mTileSource.Free();
				try {
					mTileSource = new TileSource(this, mTileSource.ID);
				} catch (RException e) {
					addMessage(e);
				}
				mMap.setTileSource(mTileSource);
				
				FillOverlays();
		        setTitle();
				break;
			case R.id.menu_addpoi:
				GeoPoint point = ((TileView.PoiMenuInfo) item.getMenuInfo()).EventGeoPoint;
				startActivityForResult((new Intent(this, PoiActivity.class)).putExtra("lat", point.getLatitude()).putExtra("lon", point.getLongitude())
						.putExtra("title", "POI"), R.id.menu_addpoi);
				break;
			case R.id.menu_editpoi:
				startActivityForResult((new Intent(this, PoiActivity.class)).putExtra("pointid", mPoiOverlay.getPoiPoint(mMarkerIndex).getId()),
						R.id.menu_editpoi);
				mMap.postInvalidate();
				break;
			case R.id.menu_deletepoi:
				mPoiManager.deletePoi(mPoiOverlay.getPoiPoint(mMarkerIndex).getId());
				mPoiOverlay.UpdateList();
				mMap.postInvalidate();
				break;
			case R.id.menu_hide:
				final PoiPoint poi = mPoiOverlay.getPoiPoint(mMarkerIndex);
				poi.Hidden = true;
				mPoiManager.updatePoi(poi);
				mPoiOverlay.UpdateList();
				mMap.postInvalidate();
				break;
			case R.id.menu_toradar:
				final PoiPoint poi1 = mPoiOverlay.getPoiPoint(mMarkerIndex);
				try {
					Intent i = new Intent("com.google.android.radar.SHOW_RADAR");
					i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					i.putExtra("name", poi1.Title);
					i.putExtra("latitude", (float) (poi1.GeoPoint.getLatitudeE6() / 1000000f));
					i.putExtra("longitude", (float) (poi1.GeoPoint.getLongitudeE6() / 1000000f));
					startActivity(i);
				} catch (Exception e) {
					Toast.makeText(this, R.string.message_noradar, Toast.LENGTH_LONG).show();
				}
				break;
			}
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.add_yandex_bookmark:
			return new AlertDialog.Builder(this)
				.setTitle(R.string.ya_dialog_title)
				.setMessage(R.string.ya_dialog_message)
				.setPositiveButton(R.string.ya_dialog_button_caption, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Browser.saveBookmark(MainActivity.this, "Мобильный Яндекс", "m.yandex.ru");
						}
				}).create();
		case R.id.whatsnew:
			return new AlertDialog.Builder(this) //.setIcon( R.drawable.alert_dialog_icon)
					.setTitle(R.string.about_dialog_whats_new)
					.setMessage(R.string.whats_new_dialog_text)
					.setNegativeButton(R.string.about_dialog_close, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked Cancel so do some stuff */
						}
					}).create();
		case R.id.about:
			return new AlertDialog.Builder(this) //.setIcon(R.drawable.alert_dialog_icon)
					.setTitle(R.string.menu_about)
					.setMessage(getText(R.string.app_name) + " v." + Ut.getAppVersion(this) + "\n\n"
							+ getText(R.string.about_dialog_text))
					.setPositiveButton(R.string.about_dialog_whats_new, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							showDialog(R.id.whatsnew);
						}
					}).setNegativeButton(R.string.about_dialog_close, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked Cancel so do some stuff */
						}
					}).create();
		case R.id.error:
			return new AlertDialog.Builder(this) //.setIcon(R.drawable.alert_dialog_icon)
			.setTitle(R.string.error_title)
			.setMessage(getText(R.string.error_text))
			.setPositiveButton(R.string.error_send, new DialogInterface.OnClickListener() {
				@SuppressWarnings("static-access")
				public void onClick(DialogInterface dialog, int whichButton) {

					SharedPreferences settings = getPreferences(Activity.MODE_PRIVATE);
					String text =  settings.getString("error", "");
					String subj = "RMaps error: ";
					try {
						final String[] lines = text.split("\n", 2);
						final Pattern p = Pattern.compile("[.][\\w]+[:| |\\t|\\n]");
						final Matcher m = p.matcher(lines[0]+"\n");
						if (m.find())
							subj += m.group().replace(".", "").replace(":", "").replace("\n", "")+" at ";
						final Pattern p2 = Pattern.compile("[.][\\w]+[(][\\w| |\\t]*[)]");
						final Matcher m2 = p2.matcher(lines[1]);
						if (m2.find())
							subj += m2.group().substring(2);
					} catch (Exception e) {
					}

					final Build b = new Build();
					final Build.VERSION v = new Build.VERSION();
					text = "Your message:"
						+"\n\nRMaps: "+Ut.getAppVersion(MainActivity.this)
						+"\nAndroid: "+v.RELEASE
						+"\nDevice: "+b.BOARD+" "+b.BRAND+" "+b.DEVICE+/*" "+b.MANUFACTURER+*/" "+b.MODEL+" "+b.PRODUCT
						+"\n\n"+text;

					startActivity(Ut.SendMail(subj, text));

					SharedPreferences uiState = getPreferences(0);
					SharedPreferences.Editor editor = uiState.edit();
					editor.putString("error", "");
					editor.commit();

				}
			}).setNegativeButton(R.string.about_dialog_close, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					SharedPreferences uiState = getPreferences(0);
					SharedPreferences.Editor editor = uiState.edit();
					editor.putString("error", "");
					editor.commit();
				}
			}).create();

		}
		return null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
		case R.id.menu_addpoi:
		case R.id.menu_editpoi:
			mPoiOverlay.UpdateList();
			mMap.postInvalidate();
			break;
		case R.id.poilist:
			if(resultCode == RESULT_OK){
				PoiPoint point = mPoiManager.getPoiPoint(data.getIntExtra("pointid", PoiPoint.EMPTY_ID()));
				if(point != null){
					setAutoFollow(false);
					mPoiOverlay.UpdateList();
					mMap.getController().setCenter(point.GeoPoint);
				}
			} else {
				mPoiOverlay.UpdateList();
				mMap.postInvalidate();
			}
			break;
		case R.id.tracks:
			if(resultCode == RESULT_OK){
				Track track = mPoiManager.getTrack(data.getIntExtra("trackid", PoiPoint.EMPTY_ID()));
				if(track != null){
					setAutoFollow(false);
					mMap.getController().setCenter(track.getBeginGeoPoint());
				}
			}
			break;
		case R.id.settings_activity_closed:
			finish();
			startActivity(new Intent(this, this.getClass()));
			break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private class MainActivityCallbackHandler extends Handler{
		@Override
		public void handleMessage(final Message msg) {
			final int what = msg.what;
			switch(what){
				case Ut.MAPTILEFSLOADER_SUCCESS_ID:
					mMap.postInvalidate();
					break;
				case R.id.user_moved_map:
					//setAutoFollow(false);
					break;
				case R.id.set_title:
					setTitle();
					break;
				case R.id.add_yandex_bookmark:
					showDialog(R.id.add_yandex_bookmark);
					break;
				case Ut.ERROR_MESSAGE:
					if(msg.obj != null)
						Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
					break;
			}
		}
	}

	private boolean mGPSFastUpdate;
	private SampleLocationListener mLocationListener, mNetListener;
	
	private class SampleLocationListener implements LocationListener {
		public static final String GPS = "gps";
		public static final String NETWORK = "network";
		public static final String OFF = "off";

		public void onLocationChanged(Location loc) {
			mMyLocationOverlay.setLocation(loc);
			Ut.d("onLocationChanged " + loc.getProvider());
			
			if (loc.getProvider().equals(GPS) && mNetListener != null) {
				getLocationManager().removeUpdates(mNetListener);
				mNetListener = null;
				Ut.d("NETWORK provider removed");
			}
			
			//int cnt = loc.getExtras().getInt("satellites", Integer.MIN_VALUE);
			mGpsStatusName = loc.getProvider(); // + " 2 " + (cnt >= 0 ? cnt : 0);
			setTitle();
			
			mLastSpeed = loc.getSpeed();
			
			if (mAutoFollow) {
				if (mDrivingDirectionUp)
					if (loc.getSpeed() > 0.5)
						mMap.setBearing(loc.getBearing());
				
				mMap.getController().setCenter(TypeConverter.locationToGeoPoint(loc));
			} else
				mMap.invalidate();
			
			setTitle();
		}

		public void onProviderDisabled(String provider) {
			Ut.d("onProviderDisabled "+provider);
			getBestProvider();
		}

		public void onProviderEnabled(String provider) {
			Ut.d("onProviderEnabled "+provider);
			getBestProvider();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			Ut.d("onStatusChanged "+provider);
			mGpsStatusSatCnt = extras.getInt("satellites", Integer.MIN_VALUE);
			mGpsStatusState = status;
			mGpsStatusName = provider;
			Ut.d(provider+" status: "+status+" cnt: "+extras.getInt("satellites", Integer.MIN_VALUE));
			setTitle();
		}
		
		private LocationManager getLocationManager() {
			return (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}

		private void getBestProvider() {
			int minTime = 0;
			int minDistance = 0;
			
			if (!mGPSFastUpdate) {
				minTime = 2000;
				minDistance = 20;
			}
			;
			
			getLocationManager().removeUpdates(mLocationListener);
			if (mNetListener != null)
				getLocationManager().removeUpdates(mNetListener);
			
			if (getLocationManager().isProviderEnabled(GPS)) {
				Ut.d("GPS Provider Enabled");
				getLocationManager().requestLocationUpdates(GPS, minTime, minDistance, mLocationListener);
				mGpsStatusName = GPS;
				
				try {
					if (getLocationManager().isProviderEnabled(NETWORK)) {
						Ut.d("NETWORK Provider Enabled");
						mNetListener = new SampleLocationListener();
						getLocationManager().requestLocationUpdates(NETWORK, minTime, minDistance, mNetListener);
						mGpsStatusName = NETWORK;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else if (getLocationManager().isProviderEnabled(NETWORK)) {
				Ut.d("only NETWORK Provider Enabled");
				getLocationManager().requestLocationUpdates(NETWORK, minTime, minDistance, mLocationListener);
				mGpsStatusName = NETWORK;
			} else {
				Ut.d("NO Provider Enabled");
				mGpsStatusName = OFF;
			}
			
			setTitle();
		}
	}
	
	private class MoveListener implements IMoveListener {

		public void onMoveDetected() {
			if(mAutoFollow)
				setAutoFollow(false);
		}

		public void onZoomDetected() {
			setTitle();
		}
		
	}
	
	private final SensorEventListener mListener = new SensorEventListener() {
		private int iOrientation = -1;

		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		public void onSensorChanged(SensorEvent event) {
			if (iOrientation < 0)
				iOrientation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
						.getDefaultDisplay().getOrientation();

			mCompassView.setAzimuth(event.values[0] + 0 * iOrientation);
			mCompassView.invalidate();

			if (mCompassEnabled)
				if (mNorthDirectionUp)
					if (mDrivingDirectionUp == false || mLastSpeed == 0) {
						mMap.setBearing(updateBearing(event.values[0]) + 0 * iOrientation);
						mMap.invalidate();
					}
		}

	};

	private float updateBearing(float newBearing) {
		float dif = newBearing - mLastBearing;
		// find difference between new and current position
		if (Math.abs(dif) > 180)
			dif = 360 - dif;
		// if difference is bigger than 180 degrees,
		// it's faster to rotate in opposite direction
		if (Math.abs(dif) < 1)
			return mLastBearing;
		// if difference is less than 1 degree, leave things as is
		if (Math.abs(dif) >= 90)
			return mLastBearing = newBearing;
		// if difference is bigger than 90 degress, just update it
		mLastBearing += 90 * Math.signum(dif) * Math.pow(Math.abs(dif) / 90, 2);
		// bearing is updated proportionally to the square of the difference
		// value
		// sign of difference is paid into account
		// if difference is 90(max. possible) it is updated exactly by 90
		while (mLastBearing > 360)
			mLastBearing -= 360;
		while (mLastBearing < 0)
			mLastBearing += 360;
		// prevent bearing overrun/underrun
		return mLastBearing;
	}


	private void ActionShowPoints(Intent queryIntent) {
		final ArrayList<String> locations = queryIntent.getStringArrayListExtra("locations");
		if(!locations.isEmpty()){
			Ut.dd("Intent: "+ACTION_SHOW_POINTS+" locations: "+locations.toString());
			String [] fields = locations.get(0).split(";");
			String locns = "", title = "", descr = "";
			if(fields.length>0) locns = fields[0];
			if(fields.length>1) title = fields[1];
			if(fields.length>2) descr = fields[2];

			GeoPoint point = GeoPoint.fromDoubleString(locns);
			mPoiOverlay.setGpsStatusGeoPoint(point, title, descr);
			setAutoFollow(false);
			mMap.getController().setCenter(point);
		}
	}

}
