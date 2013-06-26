package com.robert.maps.applib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
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
import com.robert.maps.applib.dashboard.IndicatorManager;
import com.robert.maps.applib.dashboard.IndicatorView;
import com.robert.maps.applib.dashboard.IndicatorView.IndicatorViewMenuInfo;
import com.robert.maps.applib.downloader.AreaSelectorActivity;
import com.robert.maps.applib.downloader.FileDownloadListActivity;
import com.robert.maps.applib.kml.PoiActivity;
import com.robert.maps.applib.kml.PoiListActivity;
import com.robert.maps.applib.kml.PoiManager;
import com.robert.maps.applib.kml.PoiPoint;
import com.robert.maps.applib.kml.Track;
import com.robert.maps.applib.kml.TrackListActivity;
import com.robert.maps.applib.kml.XMLparser.PredefMapsParser;
import com.robert.maps.applib.overlays.CurrentTrackOverlay;
import com.robert.maps.applib.overlays.MeasureOverlay;
import com.robert.maps.applib.overlays.MyLocationOverlay;
import com.robert.maps.applib.overlays.PoiOverlay;
import com.robert.maps.applib.overlays.SearchResultOverlay;
import com.robert.maps.applib.overlays.TileOverlay;
import com.robert.maps.applib.overlays.TrackOverlay;
import com.robert.maps.applib.overlays.YandexTrafficOverlay;
import com.robert.maps.applib.preference.MixedMapsPreference;
import com.robert.maps.applib.tileprovider.TileSource;
import com.robert.maps.applib.tileprovider.TileSourceBase;
import com.robert.maps.applib.utils.CompassView;
import com.robert.maps.applib.utils.CrashReportHandler;
import com.robert.maps.applib.utils.RException;
import com.robert.maps.applib.utils.SearchSuggestionsProvider;
import com.robert.maps.applib.utils.SimpleThreadFactory;
import com.robert.maps.applib.utils.Ut;
import com.robert.maps.applib.view.IMoveListener;
import com.robert.maps.applib.view.MapView;
import com.robert.maps.applib.view.TileView;
import com.robert.maps.applib.view.TileViewOverlay;

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
	private IndicatorManager mIndicatorManager;
	
	// Overlays
	private YandexTrafficOverlay mYandexTrafficOverlay = null;
	private TileOverlay mTileOverlay = null;
	private boolean mShowOverlay = false;
	private String mMapId = null;
	private String mOverlayId = "";
	private MyLocationOverlay mMyLocationOverlay;
	private PoiOverlay mPoiOverlay;
	private TrackOverlay mTrackOverlay;
	private CurrentTrackOverlay mCurrentTrackOverlay;
	private SearchResultOverlay mSearchResultOverlay;
	private MeasureOverlay mMeasureOverlay;

	private int mMarkerIndex;
	private boolean mAutoFollow = true;
	private String mGpsStatusName = "";
	private int mGpsStatusSatCnt = 0;
	private int mGpsStatusState = 0;
	private float mLastSpeed, mLastBearing;
	private boolean mCompassEnabled;
	private boolean mDrivingDirectionUp;
	private boolean mNorthDirectionUp;
	private int mPrefOverlayButtonBehavior;
	private int mPrefOverlayButtonVisibility;
	
	private GoogleAnalyticsTracker mTracker;
	private ImageView mOverlayView;
	private ExecutorService mThreadPool = Executors.newSingleThreadExecutor(new SimpleThreadFactory("MainActivity.Search"));
	
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
		//if(!OpenStreetMapViewConstants.DEBUGMODE) // эмулятор стал виснуть на след строчке
			mOrientationSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
 		SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
 		
 		// Init 
 		mPrefOverlayButtonBehavior = Integer.parseInt(pref.getString("pref_overlay_button_behavior", "0"));
 		mPrefOverlayButtonVisibility = Integer.parseInt(pref.getString("pref_overlay_button_visibility", "0"));
 		if(mPrefOverlayButtonVisibility == 1) // Always hide
 			mOverlayView.setVisibility(View.GONE);
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
		mPoiOverlay.setTapIndex(uiState.getInt("curShowPoiId", PoiOverlay.NO_TAP));
        this.mMyLocationOverlay = new MyLocationOverlay(this);
        this.mSearchResultOverlay = new SearchResultOverlay(this, mMap);
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
			mTracker.setCustomVar(2, "Ver", Ut.getPackVersion(this), 1);
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
			if (uri.getScheme().equalsIgnoreCase("geo")) {
				final String latlon = uri.getEncodedSchemeSpecificPart().replace("?" + uri.getEncodedQuery(), "");
				if (latlon.equals("0,0")) {
					final String query = uri.getEncodedQuery().replace("q=", "");
					queryIntent.putExtra(SearchManager.QUERY, query);
					doSearchQuery(queryIntent);
					
				} else {
					GeoPoint point = GeoPoint.fromDoubleString(latlon);
					mPoiOverlay.clearPoiList();
					mPoiOverlay.setGpsStatusGeoPoint(0, point, "GEO", "");
					setAutoFollow(false);
					mMap.getController().setCenter(point);
				}
			}
		} else if("SHOW_MAP_ID".equalsIgnoreCase(queryAction)) {
			final Bundle bundle = queryIntent.getExtras(); 
			mMapId = bundle.getString(MAPNAME);
			if(bundle.containsKey("center")) {
				try {
					final GeoPoint geo = GeoPoint.fromDoubleString(bundle.getString("center"));
					mMap.getController().setCenter(geo);
				} catch (Exception e) {
				}
			}
			if(bundle.containsKey("zoom")) {
				try {
					final int zoom = Integer.valueOf(bundle.getString("zoom"));
					mMap.getController().setZoom(zoom);
					SharedPreferences.Editor editor = uiState.edit();
					editor.putInt("ZoomLevel", mMap.getZoomLevel());
					editor.commit();
				} catch (Exception e) {
				}
			}
			queryIntent.setAction("");
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

        final String queryAction = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(queryAction)) {
            doSearchQuery(intent);
        } else if (ACTION_SHOW_POINTS.equalsIgnoreCase(queryAction)) {
			ActionShowPoints(intent);
		} else if("SHOW_MAP_ID".equalsIgnoreCase(queryAction)) {
			final Bundle bundle = intent.getExtras(); 
			mMapId = bundle.getString(MAPNAME);
			if(bundle.containsKey("center")) {
				try {
					final GeoPoint geo = GeoPoint.fromDoubleString(bundle.getString("center"));
					mMap.getController().setCenter(geo);
				} catch (Exception e) {
				}
			}
			if(bundle.containsKey("zoom")) {
				try {
					final int zoom = Integer.valueOf(bundle.getString("zoom"));
					mMap.getController().setZoom(zoom);
					SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
					SharedPreferences.Editor editor = uiState.edit();
					editor.putInt("ZoomLevel", mMap.getZoomLevel());
					editor.commit();
				} catch (Exception e) {
				}
			}
        }
	}

	private void doSearchQuery(Intent queryIntent) {
		mSearchResultOverlay.Clear();
		this.mMap.invalidate();

		final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);

        // Record the query string in the recent queries suggestions provider.
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
        suggestions.saveRecentQuery(queryString, null);
        
        mThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				Handler handler = MainActivity.this.mCallbackHandler;
				Resources resources = MainActivity.this.getApplicationContext().getResources();

				InputStream in = null;
				OutputStream out = null;

				try {
					//final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
					Configuration config = getBaseContext().getResources().getConfiguration();
			        final String lang = config.locale.getLanguage();
			        
					URL url = new URL(
							"http://ajax.googleapis.com/ajax/services/search/local?v=1.0&sll="
									+ MainActivity.this.mMap.getMapCenter().toDoubleString()
									+ "&q=" + URLEncoder.encode(queryString, "UTF-8")
									+ "&hl="+ lang /*pref.getString("pref_googlelanguagecode", "en")*/
									+ "");
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
						handler.obtainMessage(Ut.ERROR_MESSAGE, resources.getString(R.string.no_items));
						return;
					}
					JSONObject res = results.getJSONObject(0);

					//handler.obtainMessage(Ut.SEARCH_OK_MESSAGE, res);
					final String address = res.getString("addressLines").replace("\"", "").replace("[", "").replace("]", "").replace(",", ", ").replace("  ", " ");
					setAutoFollow(false, true);
					final GeoPoint point = new GeoPoint((int)(res.getDouble("lat")* 1E6), (int)(res.getDouble("lng")* 1E6));
					mSearchResultOverlay.setLocation(point, address);
					mMap.getController().setZoom((int) (2 * res.getInt("accuracy")));
					mMap.getController().setCenter(point);
					setTitle();

				} catch (Exception e) {
					try {
						handler.obtainMessage(Ut.ERROR_MESSAGE, resources.getString(R.string.no_inet_conn));
					} catch (NotFoundException e1) {
					}
				} finally {
					StreamUtils.closeStream(in);
					StreamUtils.closeStream(out);
				}
			}
		});

	}
	
	private View CreateContentView() {
		setContentView(R.layout.main);
		
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		final RelativeLayout rl = (RelativeLayout) findViewById(R.id.map_area);
		final int sideBottom = Integer.parseInt(pref.getString("pref_zoomctrl", "1"));
		final boolean showTitle = pref.getBoolean("pref_showtitle", true);
		final boolean showAutoFollow = pref.getBoolean("pref_show_autofollow_button", true);
		
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
        if(!(sideBottom == 2 ? false : true)) {
	        compassParams.addRule(RelativeLayout.ABOVE, R.id.scale_bar);
        } else {
	        compassParams.addRule(RelativeLayout.BELOW, R.id.dashboard_area);
        }
        mMap.addView(mCompassView, compassParams);

		if (showAutoFollow) {
			ivAutoFollow = new ImageView(this);
			ivAutoFollow.setImageResource(R.drawable.autofollow);
			ivAutoFollow.setVisibility(ImageView.INVISIBLE);

			final RelativeLayout.LayoutParams followParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			followParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			if (!(sideBottom == 2 ? false : true)) {
				followParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			} else {
				followParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			}
			((RelativeLayout) findViewById(R.id.right_area)).addView(ivAutoFollow, followParams);

			ivAutoFollow.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setAutoFollow(true);
					mSearchResultOverlay.Clear();
					setLastKnownLocation();
				}
			});
		}
       
        mOverlayView = new ImageView(this);
        mOverlayView.setImageResource(R.drawable.r_overlays);
        final int pad = getResources().getDimensionPixelSize(R.dimen.zoom_ctrl_padding);
        mOverlayView.setPadding(0, pad, 0, pad);
        ((LinearLayout) mMap.findViewById(R.id.right_panel)).addView(mOverlayView);
        
        mOverlayView.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				if(mTileSource.YANDEX_TRAFFIC_ON == 1) {
					mShowOverlay = !mShowOverlay;
					FillOverlays();
				} else {
					if(mPrefOverlayButtonBehavior == 1) {
						v.showContextMenu();
					} else if(mPrefOverlayButtonBehavior == 2) {
						setTileSource(mTileSource.ID, mOverlayId, !mShowOverlay);
					} else if(mOverlayId.equalsIgnoreCase("") && mTileSource.MAP_TYPE != TileSourceBase.MIXMAP_PAIR) {
						v.showContextMenu();
					} else {
						setTileSource(mTileSource.ID, mOverlayId, !mShowOverlay);
					}
				}
				
				mMap.invalidate(); //postInvalidate();
			}
		});
        mOverlayView.setOnLongClickListener(new View.OnLongClickListener() {
			
			public boolean onLongClick(View v) {
				if(mTileSource.YANDEX_TRAFFIC_ON != 1 && mPrefOverlayButtonBehavior == 0) {
					mMap.getTileView().mPoiMenuInfo.EventGeoPoint = null;
					v.showContextMenu();
				}
				return true;
			}
		});
        mOverlayView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				mMap.getTileView().mPoiMenuInfo.EventGeoPoint = null;
				
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
								if (pref.getBoolean("pref_usermaps_" + name + "_enabled", false)
										//&& (mTileSource.PROJECTION == 0 || mTileSource.PROJECTION == Integer.parseInt(pref.getString("pref_usermaps_" + name + "_projection", "1")))
										&& pref.getBoolean("pref_usermaps_" + name + "_isoverlay", false)) {
									MenuItem item = menu.add(R.id.isoverlay, Menu.NONE, Menu.NONE, pref.getString("pref_usermaps_" + name + "_name", files[i].getName()));
									item.setTitleCondensed("usermap_" + name);
								}
							}
						}
				}

				Cursor c = mPoiManager.getGeoDatabase().getMixedMaps();
				if(c != null) {
					if(c.moveToFirst()) {
						do {
							if (pref.getBoolean("PREF_MIXMAPS_" + c.getInt(0) + "_enabled", false) && c.getInt(2) == 3) {
								final JSONObject json = MixedMapsPreference.getMapCustomParams(c.getString(3));
								//if(mTileSource.PROJECTION == 0 || mTileSource.PROJECTION == json.optInt(MixedMapsPreference.MAPPROJECTION)) {
									MenuItem item = menu.add(R.id.isoverlay, Menu.NONE, Menu.NONE, c.getString(1));
									item.setTitleCondensed("mixmap_" + c.getInt(0));
								//}
							}
						} while(c.moveToNext());
					}
					c.close();
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
		
		if(mMeasureOverlay != null)
			this.mMap.getOverlays().add(mMeasureOverlay);
		
		if(mTileOverlay != null)
			this.mMap.getOverlays().add(mTileOverlay);

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
			if(ivAutoFollow != null) ivAutoFollow.setVisibility(ImageView.INVISIBLE);
			if(!supressToast)
				Toast.makeText(this, R.string.auto_follow_enabled, Toast.LENGTH_SHORT).show();
		} else {
			if(ivAutoFollow != null) ivAutoFollow.setVisibility(ImageView.VISIBLE);
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
			final Location loc1 = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			final Location loc2 = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	
			boolean boolGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
			boolean boolNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
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
		if(mIndicatorManager != null)
			mIndicatorManager.setMapName(mMap.getTileSource().NAME);
		
		try {
			final TextView leftText = (TextView) findViewById(R.id.left_text);
			if(leftText != null) {
				String overlayName = "";
				if(mMap.getTileSource() != null && mMap.getTileSource().MAP_TYPE != TileSourceBase.MIXMAP_PAIR)
					if(mMap.getTileSource().getTileSourceBaseOverlay() != null)
						overlayName = " / " + mMap.getTileSource().getTileSourceBaseOverlay().NAME;
				leftText.setText(mMap.getTileSource().NAME + overlayName);
			}
			
			final TextView gpsText = (TextView) findViewById(R.id.gps_text);
			if(gpsText != null){
				gpsText.setText(mGpsStatusName);
			}

			final TextView rightText = (TextView) findViewById(R.id.right_text);
			if(rightText != null){
				final double zoom = mMap.getZoomLevelScaled();
				if(zoom > mMap.getTileSource().ZOOM_MAXLEVEL) {
					rightText.setText(""+(mMap.getTileSource().ZOOM_MAXLEVEL+1)+"+");
					if(mIndicatorManager != null)
						mIndicatorManager.setZoom(mMap.getTileSource().ZOOM_MAXLEVEL+1);
				} else {
					rightText.setText(""+(1 + Math.round(zoom)));
					if(mIndicatorManager != null)
						mIndicatorManager.setZoom((int)(1 + Math.round(zoom)));
				}
			}
		} catch (Exception e) {
		}
	}

	@Override
	protected void onResume() {
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
		
		if(mMapId == null)
			mMapId = uiState.getString(MAPNAME, TileSource.MAPNIK);
		mOverlayId = uiState.getString("OverlayID", "");
		mShowOverlay = uiState.getBoolean("ShowOverlay", true);
		mMyLocationOverlay.setTargetLocation(GeoPoint.fromDoubleStringOrNull(uiState.getString("targetlocation", "")));
		
		setTileSource(mMapId, mOverlayId, mShowOverlay);
		mMapId = null;
		
		if(uiState.getBoolean("show_dashboard", true) && mIndicatorManager == null) {
	        mIndicatorManager = new IndicatorManager(this);
	        mIndicatorManager.setCenter(mMap.getMapCenter());
	        mIndicatorManager.setLocation(mMyLocationOverlay.getLastLocation());
	        mIndicatorManager.setTargetLocation(mMyLocationOverlay.getTargetLocation());
		}
		
 		mMap.getController().setZoom(uiState.getInt("ZoomLevel", 0));
 		setTitle();
 		
 		FillOverlays();
	
		if(mCompassEnabled)
			mOrientationSensorManager.registerListener(mListener, mOrientationSensorManager
				.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);

		if(mTrackOverlay != null)
			mTrackOverlay.setStopDraw(false);
		if(mCurrentTrackOverlay != null)
			mCurrentTrackOverlay.onResume();
		
		Ut.d("onResume getBestProvider");
		mLocationListener.getBestProvider();

		if(mIndicatorManager != null)
			mIndicatorManager.Resume(this);

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
	protected void onStop() {
		Ut.d("onStop");
		super.onStop();
	}

	@Override
	protected void onPause() {
		Ut.d("onPause");
		final GeoPoint point = mMap.getMapCenter();

		SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = uiState.edit();
		if(mTileSource != null) {
			editor.putString("MapName", mTileSource.ID);
			try {
				editor.putString("OverlayID", mTileOverlay == null ? mTileSource.getOverlayName() : mTileOverlay.getTileSource().ID);
			} catch (Exception e) {
			}
		}
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
		editor.putBoolean("show_dashboard", mIndicatorManager == null ? false : true);
		editor.putString("targetlocation", mMyLocationOverlay.getTargetLocation() == null ? "" : mMyLocationOverlay.getTargetLocation().toDoubleString());
		editor.commit();
		
		uiState = getSharedPreferences("MapName", Activity.MODE_PRIVATE);
		editor = uiState.edit();
		if(mTileSource != null)
			editor.putString("MapName", mTileSource.ID);
		editor.putInt("Latitude", point.getLatitudeE6());
		editor.putInt("Longitude", point.getLongitudeE6());
		editor.putInt("ZoomLevel", mMap.getZoomLevel());
		editor.putBoolean("CompassEnabled", mCompassEnabled);
		editor.putBoolean("AutoFollow", mAutoFollow);
		editor.commit();

		if (myWakeLock != null) 
			myWakeLock.release();

		if(mOrientationSensorManager != null)
			mOrientationSensorManager.unregisterListener(mListener);
		
		if(mCurrentTrackOverlay != null)
			mCurrentTrackOverlay.onPause();
		
		if(mTileSource != null)
			mTileSource.Free();
		mTileSource = null;
		mPoiManager.FreeDatabases();
		
		if(mTileOverlay != null)
			mTileOverlay.Free();
		
		mLocationListener.getLocationManager().removeUpdates(mLocationListener);
		if(mNetListener != null) {
			mLocationListener.getLocationManager().removeUpdates(mNetListener);
			mNetListener = null;
		}
		
		if(mIndicatorManager != null)
			mIndicatorManager.Pause(this);

		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Ut.d("onDestroy");
		if(mIndicatorManager != null) {
			mIndicatorManager.Dismiss(this);
			mIndicatorManager = null;
		}
		
		for (TileViewOverlay osmvo : mMap.getOverlays())
			osmvo.Free();
		if(mTileSource != null)
			mTileSource.Free();
		mTileSource = null;
		mMap.setMoveListener(null);
		mTracker.stopSession();
		mThreadPool.shutdown();
		
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
		
		if(mTileSource == null) {
			menu.findItem(R.id.reload).setVisible(false);
		} else if(mTileSource.MAP_TYPE == TileSourceBase.PREDEF_ONLINE || mTileSource.MAP_TYPE == TileSourceBase.MIXMAP_CUSTOM) {
			menu.findItem(R.id.reload).setVisible(true);
		} else {
			menu.findItem(R.id.reload).setVisible(false);
		}

		File folder = Ut.getRMapsMapsDir(this);
		if (folder.exists()) {
			File[] files = folder.listFiles();
			if (files != null)
				for (int i = 0; i < files.length; i++) {
					if (files[i].getName().toLowerCase().endsWith(".mnm")
							|| files[i].getName().toLowerCase().endsWith(".tar")
							|| files[i].getName().toLowerCase().endsWith(".sqlitedb")) {
						String name = Ut.FileName2ID(files[i].getName());
						if (pref.getBoolean("pref_usermaps_" + name + "_enabled", false) && !pref.getBoolean("pref_usermaps_" + name + "_isoverlay", false)) {
							MenuItem item = submenu.add(pref.getString("pref_usermaps_" + name + "_name",
									files[i].getName()));
							item.setTitleCondensed("usermap_" + name);
						}
					}
				}
		}
		
		Cursor c = mPoiManager.getGeoDatabase().getMixedMaps();
		if(c != null) {
			if(c.moveToFirst()) {
				do {
					if (pref.getBoolean("PREF_MIXMAPS_" + c.getInt(0) + "_enabled", true) && c.getInt(2) < 3) {
						MenuItem item = submenu.add(c.getString(1));
						item.setTitleCondensed("mixmap_" + c.getInt(0));
					}
				} while(c.moveToNext());
			}
			c.close();
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

		if(item.getItemId() == R.id.area_selector) {
			startActivity(new Intent(this, AreaSelectorActivity.class).putExtra("new", true).putExtra(MAPNAME, mTileSource.ID).putExtra("Latitude", point.getLatitudeE6()).putExtra("Longitude", point.getLongitudeE6()).putExtra("ZoomLevel", mMap.getZoomLevel()));
			return true;
		} else if(item.getItemId() == R.id.menu_show_dashboard) {
			if(mIndicatorManager == null) {
				mIndicatorManager = new IndicatorManager(this);
				mIndicatorManager.setCenter(mMap.getMapCenter());
				mIndicatorManager.setMapName(mTileSource.NAME);
				mIndicatorManager.setZoom(mMap.getZoomLevel());
		        mIndicatorManager.setLocation(mMyLocationOverlay.getLastLocation());
		        mIndicatorManager.setTargetLocation(mMyLocationOverlay.getTargetLocation());
			} else {
				mIndicatorManager.Dismiss(this);
				mIndicatorManager = null;
			}
			return true;
		} else if(item.getItemId() == R.id.downloadprepared) {
			startActivity(new Intent(this, FileDownloadListActivity.class));
			return true;
		} else if(item.getItemId() == R.id.tools) {
			return true;
		} else if(item.getItemId() == R.id.findthemap) {
			doFindTheMap();
			return true;
		} else if(item.getItemId() == R.id.reload) {
			mTileSource.setReloadTileMode(true);
			mMap.invalidate(); //postInvalidate();
			return true;
		} else if(item.getItemId() == R.id.measure) {
			doMeasureStart();
			return true;
		} else if(item.getItemId() == R.id.gpsstatus) {
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
		} else if (item.getItemId() == R.id.poilist) {
			startActivityForResult((new Intent(this, PoiListActivity.class)).putExtra("lat", point.getLatitude()).putExtra("lon", point.getLongitude()).putExtra("title", "POI"), R.id.poilist);
			return true;
		} else if (item.getItemId() == R.id.tracks) {
			startActivityForResult(new Intent(this, TrackListActivity.class), R.id.tracks);
			return true;
		} else if (item.getItemId() == R.id.search) {
			onSearchRequested();
			return true;
		} else if (item.getItemId() == R.id.settings) {
			startActivityForResult(new Intent(this, MainPreferences.class), R.id.settings_activity_closed);
			return true;
		} else if (item.getItemId() == R.id.about) {
			showDialog(R.id.about);
			return true;
		} else if (item.getItemId() == R.id.mapselector) {
			return true;
		} else if (item.getItemId() == R.id.compass) {
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
		} else if (item.getItemId() == R.id.mylocation) {
			setAutoFollow(true);
			setLastKnownLocation();
			return true;
		} else if (item.getItemId() == R.id.exit) {
			onPause();
			System.exit(10);
			return true;
		} else {
			
			final String mapid = (String)item.getTitleCondensed();
			setTileSource(mapid, "", true);
			
			if(mTileSource.MAP_TYPE == TileSource.PREDEF_ONLINE) {
				mTracker.setCustomVar(1, "MAP", mapid);
				mTracker.trackPageView("/maps");
			}
			
			FillOverlays();

	        setTitle();

			return true;
		}

	}

	private void doFindTheMap() {
		final GeoPoint geo = mTileSource.findTheMap(mMap.getZoomLevel());
		if(geo != null)
			mMap.getController().setCenter(geo);
	}

	private void doMeasureStart() {
		if(mMeasureOverlay == null)
	        mMeasureOverlay = new MeasureOverlay(this, findViewById(R.id.bottom_area));
			
		final View viewBottomArea = findViewById(R.id.bottom_area);
		viewBottomArea.findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mMeasureOverlay.addPointOnCenter(mMap.getTileView());
				mMap.invalidate(); //postInvalidate();
			}
		});
		viewBottomArea.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mMeasureOverlay = null;
				((ViewGroup) findViewById(R.id.bottom_area)).removeAllViews();
				FillOverlays();
			}
		});
		
		final View viewMenuButton = viewBottomArea.findViewById(R.id.menu);
		viewMenuButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				v.showContextMenu();
			}
		});
		viewMenuButton.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				{
					final MenuItem item = menu.add(Menu.NONE, R.id.menu_showinfo, Menu.NONE, R.string.menu_showinfo);
					item.setCheckable(true);
					final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
					item.setChecked(pref.getBoolean("pref_show_measure_info", true));
				}
				{
					final MenuItem item = menu.add(Menu.NONE, R.id.menu_showlineinfo, Menu.NONE, R.string.menu_showlineinfo);
					item.setCheckable(true);
					final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
					item.setChecked(pref.getBoolean("pref_show_measure_line_info", true));
				}
				menu.add(Menu.NONE, R.id.menu_addmeasurepoint, Menu.NONE, R.string.menu_add);
				menu.add(Menu.NONE, R.id.menu_undo, Menu.NONE, R.string.menu_undo);
				menu.add(Menu.NONE, R.id.clear, Menu.NONE, R.string.clear);
			}
		});
		
		FillOverlays();
	}

	private void setTileSource(String aMapId, String aOverlayId, boolean aShowOverlay) {
		final String mapId = aMapId == null ? (mTileSource == null ? TileSource.MAPNIK : mTileSource.ID) : aMapId;
		final String overlayId = aOverlayId == null ? mOverlayId : aOverlayId;
		final String lastMapID = mTileSource == null ? TileSource.MAPNIK : mTileSource.ID;
		
		if(mTileSource != null) mTileSource.Free();
		
		if(overlayId != null && !overlayId.equalsIgnoreCase("") && aShowOverlay) {
			mOverlayId = overlayId;
			mShowOverlay = true;
			try {
				mTileSource = new TileSource(this, mapId, overlayId);
				
			} catch (RException e) {
				mTileSource = null;
				addMessage(e);
			} catch (Exception e) {
				mTileSource = null;
				addMessage(new RException(R.string.error_other, e.getMessage()));
			}
		} else {
			if(mTileOverlay != null) {
				mTileOverlay.Free();
				mTileOverlay = null;
			}
			
			try {
				mTileSource = new TileSource(this, mapId, aShowOverlay);
				
				mShowOverlay = aShowOverlay;
				if(mapId != lastMapID)
					mOverlayId = "";
			} catch (RException e) {
				mTileSource = null;
				addMessage(e);
			} catch (Exception e) {
				mTileSource = null;
				addMessage(new RException(R.string.error_other, e.getMessage()));
			}
		}
		
		if(mTileSource != null) {
			final TileSource tileSource = mTileSource.getTileSourceForTileOverlay();
			if(tileSource != null) {
				if(mTileOverlay == null)
					mTileOverlay = new TileOverlay(mMap.getTileView(), true);
				mTileOverlay.setTileSource(tileSource);
			} else if(mTileOverlay != null) {
				mTileOverlay.Free();
				mTileOverlay = null;
			}
		} else {
			try {
				mTileSource = new TileSource(this, TileSource.MAPNIK);
			} catch (SQLiteException e) {
			} catch (RException e) {
			}
		}
		
		mMap.setTileSource(mTileSource);
		FillOverlays();
		
		if(mMyLocationOverlay != null && mTileSource != null)
			mMyLocationOverlay.correctScale(mTileSource.MAPTILE_SIZE_FACTOR, mTileSource.GOOGLESCALE_SIZE_FACTOR);
		
		if(mPrefOverlayButtonVisibility == 2)
			mOverlayView.setVisibility(mTileSource.MAP_TYPE == TileSourceBase.MIXMAP_PAIR || mTileSource.YANDEX_TRAFFIC_ON == 1 ? View.VISIBLE : View.GONE);
	}

	private void addMessage(RException e) {
		
		LinearLayout msgbox = (LinearLayout) findViewById(e.getID());
		if(msgbox == null) {
			msgbox = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.error_message_box, (ViewGroup) findViewById(R.id.message_list));
			msgbox.setId(e.getID());
			msgbox.setVisibility(View.VISIBLE);
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
		};

		((TextView) msgbox.findViewById(R.id.descr)).setText(e.getStringRes(this));
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if(menuInfo instanceof TileView.PoiMenuInfo) {
			final TileView.PoiMenuInfo info = (TileView.PoiMenuInfo) menuInfo;
			if(info.EventGeoPoint != null) {
				if(info.MarkerIndex > PoiOverlay.NO_TAP) {
					mMarkerIndex = info.MarkerIndex;
					if(info.MarkerIndex >= 0) {
						menu.add(0, R.id.menu_editpoi, 0, getText(R.string.menu_edit));
						menu.add(0, R.id.menu_hide, 0, getText(R.string.menu_hide));
						menu.add(0, R.id.menu_deletepoi, 0, getText(R.string.menu_delete));
					}
					menu.add(0, R.id.menu_share, 0, getText(R.string.menu_share));
					menu.add(0, R.id.menu_toradar, 0, getText(R.string.menu_toradar));
				} else {
					menu.add(0, R.id.menu_addpoi, 0, getText(R.string.menu_addpoi));
					menu.add(0, R.id.menu_i_am_here, 0, getText(R.string.menu_i_am_here));
					menu.add(0, R.id.menu_add_target_point, 0, getText(R.string.menu_add_target_point));
					if(mMyLocationOverlay.getTargetLocation() != null)
						menu.add(0, R.id.menu_remove_target_point, 0, getText(R.string.menu_remove_target_point));
				}
			}
		}

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getGroupId() == R.id.isoverlay) {
			final String overlayid = (String)item.getTitleCondensed();
			setTileSource(mTileSource.ID, overlayid, true);
			
			if(mTileSource.MAP_TYPE == TileSource.PREDEF_ONLINE) {
				mTracker.setCustomVar(1, "OVERLAY", overlayid);
				mTracker.trackPageView("/overlays");
			}
			
			FillOverlays();
	        setTitle();
	        
		} else if(item.getGroupId() == R.id.menu_dashboard_edit) {
			final IndicatorViewMenuInfo info = (IndicatorViewMenuInfo) item.getMenuInfo();
			final IndicatorView iv = info.IndicatorView;
			mIndicatorManager.putTagToIndicatorView(iv, item.getTitleCondensed().toString());
			mMap.invalidate(); //postInvalidate();
			
		} else {
			if(item.getItemId() == R.id.clear) {
				mMeasureOverlay.Clear();
				mMap.invalidate(); //postInvalidate();
			} else if (item.getItemId() == R.id.menu_dashboard_delete) {
				final IndicatorViewMenuInfo info = (IndicatorViewMenuInfo) item.getMenuInfo();
				final IndicatorView iv = info.IndicatorView;
				mIndicatorManager.removeIndicatorView(this, iv);
				mMap.invalidate(); //postInvalidate();
				
			} else if (item.getItemId() == R.id.menu_add_target_point) {
				TileView.PoiMenuInfo info = (TileView.PoiMenuInfo) item.getMenuInfo();
				mMyLocationOverlay.setTargetLocation(info.EventGeoPoint);
				if(mIndicatorManager != null)
					mIndicatorManager.setTargetLocation(info.EventGeoPoint);
				mMap.invalidate();
				
			} else if (item.getItemId() == R.id.menu_remove_target_point) {
				mMyLocationOverlay.setTargetLocation(null);
				if(mIndicatorManager != null)
					mIndicatorManager.setTargetLocation(null);
				mMap.invalidate();
				
			} else if (item.getItemId() == R.id.menu_dashboard_add) {
				final IndicatorViewMenuInfo info = (IndicatorViewMenuInfo) item.getMenuInfo();
				final IndicatorView iv = info.IndicatorView;
				mIndicatorManager.addIndicatorView(this, iv, iv.getIndicatorTag(), false);
				mMap.invalidate(); //postInvalidate();
			
			} else if (item.getItemId() == R.id.menu_dashboard_add_line) {
				final IndicatorViewMenuInfo info = (IndicatorViewMenuInfo) item.getMenuInfo();
				final IndicatorView iv = info.IndicatorView;
				mIndicatorManager.addIndicatorView(this, iv, iv.getIndicatorTag(), true);
				mMap.invalidate(); //postInvalidate();

			} else if (item.getItemId() == R.id.menu_undo) {
				mMeasureOverlay.Undo();
				mMap.invalidate(); //postInvalidate();
			} else if (item.getItemId() == R.id.menu_showinfo) {
				item.setChecked(!item.isChecked());
				final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
				Editor editor = pref.edit();
				editor.putBoolean("pref_show_measure_info", item.isChecked());
				editor.commit();
				mMeasureOverlay.setShowInfoBubble(item.isChecked());

				mMap.invalidate(); //postInvalidate();
			} else if (item.getItemId() == R.id.menu_showlineinfo) {
				item.setChecked(!item.isChecked());
				final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
				Editor editor = pref.edit();
				editor.putBoolean("pref_show_measure_line_info", item.isChecked());
				editor.commit();
				mMeasureOverlay.setShowLineInfo(item.isChecked());

				mMap.invalidate(); //postInvalidate();
			} else if (item.getItemId() == R.id.menu_addmeasurepoint) {
				mMeasureOverlay.addPointOnCenter(mMap.getTileView());
				mMap.invalidate(); //postInvalidate();
			} else if (item.getItemId() == R.id.hide_overlay) {
				setTileSource(mTileSource.ID, mOverlayId, false);
				
				FillOverlays();
		        setTitle();
			} else if (item.getItemId() == R.id.menu_i_am_here) {
				final Location loc = new Location("gps");
				TileView.PoiMenuInfo info = (TileView.PoiMenuInfo) item.getMenuInfo();
				loc.setLatitude(info.EventGeoPoint.getLatitude());
				loc.setLongitude(info.EventGeoPoint.getLongitude());

				mMyLocationOverlay.setLocation(loc);
				mSearchResultOverlay.setLocation(loc);
				if(mIndicatorManager != null)
					mIndicatorManager.setLocation(loc);
				
				mMap.invalidate(); //postInvalidate();
				
			} else if (item.getItemId() == R.id.menu_addpoi) {
				TileView.PoiMenuInfo info = (TileView.PoiMenuInfo) item.getMenuInfo(); //).EventGeoPoint;
				startActivityForResult((new Intent(this, PoiActivity.class))
						.putExtra("lat", info.EventGeoPoint.getLatitude())
						.putExtra("lon", info.EventGeoPoint.getLongitude())
						.putExtra("alt", info.Elevation)
						.putExtra("title", "POI"), R.id.menu_addpoi);
			} else if (item.getItemId() == R.id.menu_editpoi) {
				startActivityForResult((new Intent(this, PoiActivity.class)).putExtra("pointid", mMarkerIndex),
						R.id.menu_editpoi);
				mMap.invalidate(); //postInvalidate();
			} else if (item.getItemId() == R.id.menu_deletepoi) {
				final int pointid = mPoiOverlay.getPoiPoint(mMarkerIndex).getId();
				new AlertDialog.Builder(this) 
				.setTitle(R.string.app_name)
				.setMessage(getResources().getString(R.string.question_delete, getText(R.string.poi)) )
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						mPoiManager.deletePoi(pointid);
						mPoiOverlay.UpdateList();
						mMap.invalidate(); //postInvalidate();
					}
				}).setNegativeButton(R.string.no, null).create().show();
			} else if (item.getItemId() == R.id.menu_hide) {
				final PoiPoint poi = mPoiOverlay.getPoiPoint(mMarkerIndex);
				poi.Hidden = true;
				mPoiManager.updatePoi(poi);
				mPoiOverlay.UpdateList();
				mMap.invalidate(); //postInvalidate();
			} else if (item.getItemId() == R.id.menu_share) {
				try {
					final PoiPoint poi = mPoiOverlay.getPoiPoint(mMarkerIndex);
					Intent intent1 = new Intent(Intent.ACTION_SEND); 
					intent1.setType("text/plain"); 
					intent1.putExtra(Intent.EXTRA_TEXT, new StringBuilder()
					.append(poi.Title)
					.append("\nhttp://maps.google.com/?q=")
					.append(poi.GeoPoint.toDoubleString())
					.toString()); 
					startActivity(Intent.createChooser(intent1, getText(R.string.menu_share)));
				} catch (Exception e) {
				} 
			} else if (item.getItemId() == R.id.menu_toradar) {
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
			}
		}
		
		final ContextMenuInfo menuInfo = item.getMenuInfo();
		if(menuInfo != null && menuInfo instanceof TileView.PoiMenuInfo) {
			((TileView.PoiMenuInfo) menuInfo).EventGeoPoint = null;
		}
		
		return super.onContextItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == R.id.add_yandex_bookmark) {
			return new AlertDialog.Builder(this)
				.setTitle(R.string.ya_dialog_title)
				.setMessage(R.string.ya_dialog_message)
				.setPositiveButton(R.string.ya_dialog_button_caption, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Browser.saveBookmark(MainActivity.this, "Мобильный Яндекс", "m.yandex.ru");
						}
				}).create();
		} else if (id == R.id.whatsnew) {
			return new AlertDialog.Builder(this) //.setIcon( R.drawable.alert_dialog_icon)
					.setTitle(R.string.about_dialog_whats_new)
					.setMessage(R.string.whats_new_dialog_text)
					.setNegativeButton(R.string.about_dialog_close, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked Cancel so do some stuff */
						}
					})
					.setPositiveButton(R.string.donation, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							try {
								startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://search?q=pname:com.robert.maps.ext")));
							} catch (Exception e1) {
							}
						}
					})
					.create();
		} else if (id == R.id.about) {
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
		} else if (id == R.id.error) {
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
		if (requestCode == R.id.menu_editpoi || requestCode == R.id.menu_addpoi) {
			mPoiOverlay.UpdateList();
			mMap.invalidate(); //postInvalidate();
		} else if (requestCode == R.id.poilist) {
			if(resultCode == RESULT_OK){
				PoiPoint point = mPoiManager.getPoiPoint(data.getIntExtra("pointid", PoiPoint.EMPTY_ID()));
				if(point != null){
					setAutoFollow(false);
					mPoiOverlay.UpdateList();
					mMap.getController().setCenter(point.GeoPoint);
				}
			} else {
				mPoiOverlay.UpdateList();
				mMap.invalidate(); //postInvalidate();
			}
		} else if (requestCode == R.id.tracks) {
			if(resultCode == RESULT_OK){
				Track track = mPoiManager.getTrack(data.getIntExtra("trackid", PoiPoint.EMPTY_ID()));
				if(track != null){
					setAutoFollow(false);
					mMap.getController().setCenter(track.getBeginGeoPoint());
				}
			}
		} else if (requestCode == R.id.settings_activity_closed) {
			finish();
			startActivity(new Intent(this, this.getClass()));
		}

		super.onActivityResult(requestCode, resultCode, data);
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
					Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
			}
		}
	}

	private boolean mGPSFastUpdate;
	private SampleLocationListener mLocationListener, mNetListener;
	
	private class SampleLocationListener implements LocationListener {
		public static final String OFF = "off";

		public void onLocationChanged(Location loc) {
			mMyLocationOverlay.setLocation(loc);
			mSearchResultOverlay.setLocation(loc);
			
			if (loc.getProvider().equals(LocationManager.GPS_PROVIDER) && mNetListener != null) {
				getLocationManager().removeUpdates(mNetListener);
				mNetListener = null;
				mGpsStatusName = LocationManager.GPS_PROVIDER;
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
			if(provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER) && mNetListener != null)
				mGpsStatusName = LocationManager.NETWORK_PROVIDER;
			else
				mGpsStatusName = OFF;
			
			if(provider.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER) && mNetListener != null) {
				getLocationManager().removeUpdates(mNetListener);
				mNetListener = null;
				if(getLocationManager().isProviderEnabled(LocationManager.GPS_PROVIDER))
					mGpsStatusName = LocationManager.GPS_PROVIDER;
				else
					mGpsStatusName = OFF;
			}
			setTitle();
		}

		public void onProviderEnabled(String provider) {
			Ut.d("onProviderEnabled "+provider);
			if(provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER) && mNetListener == null)
				mGpsStatusName = LocationManager.GPS_PROVIDER;
			setTitle();
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
			final LocationManager lm = getLocationManager();
			final List<String> listProviders = lm.getAllProviders();
			mGpsStatusName = OFF;
			
			if (!mGPSFastUpdate) {
				minTime = 2000;
				minDistance = 20;
			}
			;
			
			lm.removeUpdates(mLocationListener);
			
			if (mNetListener != null)
				lm.removeUpdates(mNetListener);
			
			if (listProviders.contains(LocationManager.GPS_PROVIDER)) {
				Ut.d("GPS Provider available");
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, mLocationListener);
				if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
					mGpsStatusName = LocationManager.GPS_PROVIDER;
				
				try {
					if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
						Ut.d("NETWORK Provider Enabled");
						mNetListener = new SampleLocationListener();
						lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, mNetListener);
						mGpsStatusName = LocationManager.NETWORK_PROVIDER;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else if (listProviders.contains(LocationManager.NETWORK_PROVIDER) && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				Ut.d("only NETWORK Provider Enabled");
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, mLocationListener);
				mGpsStatusName = LocationManager.NETWORK_PROVIDER;
			} else {
				Ut.d("NO Provider Enabled");
			}
			
			setTitle();
		}
	}
	
	private class MoveListener implements IMoveListener {

		public void onMoveDetected() {
			if(mIndicatorManager != null)
				mIndicatorManager.setCenter(mMap.getMapCenter());

			if(mAutoFollow)
				setAutoFollow(false);
		}

		public void onZoomDetected() {
			setTitle();
		}

		@Override
		public void onCenterDetected() {
			if(mIndicatorManager != null)
				mIndicatorManager.setCenter(mMap.getMapCenter());
		}
		
	}
	
	private final SensorEventListener mListener = new SensorEventListener() {
		private int iOrientation = -1;

		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		public void onSensorChanged(SensorEvent event) {
			if (iOrientation < 0) {
				iOrientation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
						.getDefaultDisplay().getOrientation();
			}

			mCompassView.setAzimuth(event.values[0] + 90 * iOrientation);
			mCompassView.invalidate();

			if (mCompassEnabled)
				if (mNorthDirectionUp)
					if (mDrivingDirectionUp == false || mLastSpeed == 0) {
						mMap.setBearing(updateBearing(event.values[0]) + 90 * iOrientation);
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
			GeoPoint point = null;
			mPoiOverlay.clearPoiList();
			int id = -1;
			Iterator<String> it = locations.iterator();
			while(it.hasNext()) {
				final String [] fields = it.next().split(";");
				String locns = "", title = "", descr = "";
				if(fields.length>0) locns = fields[0];
				if(fields.length>1) title = fields[1];
				if(fields.length>2) descr = fields[2];
	
				point = GeoPoint.fromDoubleString(locns);
				mPoiOverlay.setGpsStatusGeoPoint(id--, point, title, descr);
			}
			setAutoFollow(false);
			if(point != null)
				mMap.getController().setCenter(point);
		}
	}

}
