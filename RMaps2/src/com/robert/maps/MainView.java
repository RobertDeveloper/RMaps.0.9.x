package com.robert.maps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.andnav.osm.OpenStreetMapActivity;
import org.andnav.osm.util.TypeConverter;
import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.controller.OpenStreetMapViewController;
import org.andnav.osm.views.overlay.OpenStreetMapViewSimpleLocationOverlay;
import org.andnav.osm.views.util.OpenStreetMapRendererInfo;
import org.andnav.osm.views.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

import com.robert.maps.utils.Ut;

public class MainView extends OpenStreetMapActivity implements OpenStreetMapConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private OpenStreetMapView mOsmv; //, mOsmvMinimap;
	private OpenStreetMapViewSimpleLocationOverlay mMyLocationOverlay;

	private PowerManager.WakeLock myWakeLock;
	private boolean mFullScreen;
	private boolean mShowTitle;
	private String mStatusListener = "";
	private boolean mAutoFollow = true;
	private Handler mCallbackHandler = new MainActivityCallbackHandler();
	private ImageView ivAutoFollow;
	private CompassView mCompassView;
	private SensorManager mOrientationSensorManager;
	private boolean mCompassEnabled;

	private final SensorEventListener mListener = new SensorEventListener() {

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// Auto-generated method stub

		}

		public void onSensorChanged(SensorEvent event) {
			mCompassView.setAzimuth(event.values[0]);
			mCompassView.invalidate();
		}

	};

	// ===========================================================
	// Constructors
	// ===========================================================

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, false); // Pass true here to actually contribute to OSM!

		mOrientationSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
       	final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

 		final RelativeLayout rl = new RelativeLayout(this);
        OpenStreetMapRendererInfo RendererInfo = getRendererInfo(getResources(), getPreferences(Activity.MODE_PRIVATE), "mapnik");
        this.mOsmv = new OpenStreetMapView(this, RendererInfo);
        this.mOsmv.setMainActivityCallbackHandler(mCallbackHandler);
        rl.addView(this.mOsmv, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        /* SingleLocation-Overlay */
        {
	        /* Create a static Overlay showing a single location. (Gets updated in onLocationChanged(Location loc)! */
        	if(RendererInfo.YANDEX_TRAFFIC_ON == 1)
        		this.mOsmv.getOverlays().add(new YandexTrafficOverlay(this, this.mOsmv));

	        this.mMyLocationOverlay = new OpenStreetMapViewSimpleLocationOverlay(this);
	        this.mOsmv.getOverlays().add(mMyLocationOverlay);
        }

        {
        	final boolean sideBottom = pref.getBoolean("pref_bottomzoomcontrol", false);

            /* Compass */
        	mCompassView = new CompassView(this, sideBottom);
	        mCompassView.setVisibility(mCompassEnabled ? View.VISIBLE : View.INVISIBLE);
	        /* Create RelativeLayoutParams, that position in in the top right corner. */
	        final RelativeLayout.LayoutParams compassParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        compassParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	        compassParams.addRule(!sideBottom ? RelativeLayout.ALIGN_PARENT_BOTTOM : RelativeLayout.ALIGN_PARENT_TOP);
	        rl.addView(mCompassView, compassParams);

            /* AutoFollow */
	        ivAutoFollow = new ImageView(this);
	        ivAutoFollow.setImageResource(R.drawable.autofollow);
	        ivAutoFollow.setVisibility(ImageView.INVISIBLE);
	        /* Create RelativeLayoutParams, that position in in the top right corner. */
	        final RelativeLayout.LayoutParams followParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        followParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	        followParams.addRule(!sideBottom ? RelativeLayout.ALIGN_PARENT_BOTTOM : RelativeLayout.ALIGN_PARENT_TOP);
	        rl.addView(ivAutoFollow, followParams);

	        ivAutoFollow.setOnClickListener(new OnClickListener(){
				// @Override
				public void onClick(View v) {
					setAutoFollow(true);
					setLastKnownLocation();
				}
	        });

	        /* ZoomControls */
	        /* Create a ImageView with a zoomIn-Icon. */
	        final ImageView ivZoomIn = new ImageView(this);
	        ivZoomIn.setImageResource(R.drawable.zoom_in);
	        /* Create RelativeLayoutParams, that position in in the top right corner. */
	        final RelativeLayout.LayoutParams zoominParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        zoominParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	        zoominParams.addRule(sideBottom ? RelativeLayout.ALIGN_PARENT_BOTTOM : RelativeLayout.ALIGN_PARENT_TOP);
	        rl.addView(ivZoomIn, zoominParams);

	        ivZoomIn.setOnClickListener(new OnClickListener(){
				// @Override
				public void onClick(View v) {
					MainView.this.mOsmv.zoomIn();
					setTitle();

					if(MainView.this.mOsmv.getZoomLevel() > 16 && MainView.this.mOsmv.getRenderer().YANDEX_TRAFFIC_ON == 1)
						Toast.makeText(MainView.this, R.string.no_traffic, Toast.LENGTH_SHORT).show();
				}
	        });
	        ivZoomIn.setOnLongClickListener(new OnLongClickListener(){
				// @Override
				public boolean onLongClick(View v) {
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainView.this);
					final int zoom = Integer.parseInt(pref.getString("pref_zoommaxlevel", "0"));
					if (zoom > 0) {
						MainView.this.mOsmv.setZoomLevel(zoom - 1);
						setTitle();
					}
					return true;
				}
	        });


	        /* Create a ImageView with a zoomOut-Icon. */
	        final ImageView ivZoomOut = new ImageView(this);
	        ivZoomOut.setImageResource(R.drawable.zoom_out);

	        /* Create RelativeLayoutParams, that position in in the top left corner. */
	        final RelativeLayout.LayoutParams zoomoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        zoomoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	        zoomoutParams.addRule(sideBottom ? RelativeLayout.ALIGN_PARENT_BOTTOM : RelativeLayout.ALIGN_PARENT_TOP);
	        rl.addView(ivZoomOut, zoomoutParams);

	        ivZoomOut.setOnClickListener(new OnClickListener(){
				// @Override
				public void onClick(View v) {
					MainView.this.mOsmv.zoomOut();
					setTitle();
				}
	        });
	        ivZoomOut.setOnLongClickListener(new OnLongClickListener(){
				// @Override
				public boolean onLongClick(View v) {
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainView.this);
					final int zoom = Integer.parseInt(pref.getString("pref_zoomminlevel", "0"));
					if (zoom > 0) {
						MainView.this.mOsmv.setZoomLevel(zoom - 1);
						setTitle();
					}
					return true;
				}
	        });
        }


     	mFullScreen = pref.getBoolean("pref_showstatusbar", true);
		if (mFullScreen)
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		else
			getWindow()
					.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		mShowTitle = pref.getBoolean("pref_showtitle", true);
		if (mShowTitle)
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		else
			requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.setContentView(rl);

        if(mShowTitle)
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.main_title);

		restoreUIState();

    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_option_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case (R.id.settings):
			startActivityForResult(new Intent(this, MainPreferences.class), 12345);
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
			else
				mOrientationSensorManager.unregisterListener(mListener);
			return true;
		case (R.id.mylocation):
			setAutoFollow(true);
			setLastKnownLocation();
			return true;
		default:
 			OpenStreetMapRendererInfo RendererInfo = getRendererInfo(getResources(), getPreferences(Activity.MODE_PRIVATE), (String)item.getTitleCondensed());
			mOsmv.setRenderer(RendererInfo);

			this.mOsmv.getOverlays().clear();
			if(RendererInfo.YANDEX_TRAFFIC_ON == 1){
	       		this.mOsmv.getOverlays().add(new YandexTrafficOverlay(this, this.mOsmv));
			}
	        this.mOsmv.getOverlays().add(mMyLocationOverlay);

	        setTitle();

			return true;
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
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
		}
		return null;
	}

	private void setLastKnownLocation() {
		final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		final Location loc1 = lm.getLastKnownLocation("gps");
		final Location loc2 = lm.getLastKnownLocation("network");
		Location loc = null;
		if(loc1 == null && loc2 != null)
			loc = loc2;
		else if (loc1 != null && loc2 == null)
			loc = loc1;
		else
			loc = loc1.getTime() > loc2.getTime() ? loc1 : loc2;

		if (loc != null)
			this.mOsmv
					.getController()
					.animateTo(
							TypeConverter.locationToGeoPoint(loc),
							OpenStreetMapViewController.AnimationType.MIDDLEPEAKSPEED,
							OpenStreetMapViewController.ANIMATION_SMOOTHNESS_HIGH,
							OpenStreetMapViewController.ANIMATION_DURATION_DEFAULT);
	}

	private void setAutoFollow(boolean autoFollow) {
		if (mAutoFollow != autoFollow) {
			mAutoFollow = autoFollow;

			if (autoFollow) {
				ivAutoFollow.setVisibility(ImageView.INVISIBLE);
				Toast.makeText(this, R.string.auto_follow_enabled, Toast.LENGTH_SHORT).show();
			} else {
				ivAutoFollow.setVisibility(ImageView.VISIBLE);
				Toast.makeText(this, R.string.auto_follow_disabled, Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onLocationChanged(Location loc) {
		this.mMyLocationOverlay.setLocation(loc);

		if(mAutoFollow)
			this.mOsmv.getController().animateTo(TypeConverter.locationToGeoPoint(loc), OpenStreetMapViewController.AnimationType.MIDDLEPEAKSPEED, OpenStreetMapViewController.ANIMATION_SMOOTHNESS_HIGH, OpenStreetMapViewController.ANIMATION_DURATION_DEFAULT);
		else
			this.mOsmv.invalidate();
	}

	@Override
	public void onLocationLost() {
		// Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		int cnt = extras.getInt("satellites", Integer.MIN_VALUE);
		mStatusListener = provider+ " " + status + " " + (cnt >= 0 ? cnt : 0);
		setTitle();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Menu submenu = menu.findItem(R.id.mapselector).getSubMenu();
		submenu.clear();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

		File folder = Util.getRMapsFolder("maps", true);
		if (folder.exists()) {
			File[] files = folder.listFiles();
			if (files != null)
				for (int i = 0; i < files.length; i++) {
					if (files[i].getName().toLowerCase().endsWith(".mnm")
							|| files[i].getName().toLowerCase().endsWith(".tar")) {
						String name = Util.FileName2ID(files[i].getName());
						if (pref.getBoolean("pref_usermaps_" + name + "_enabled", false)) {
							MenuItem item = submenu.add(pref.getString("pref_usermaps_" + name + "_name", files[i]
									.getName()));
							item.setTitleCondensed("usermap_" + name);
						}
					}
				}
		}
		InputStream in = getResources().openRawResource(R.raw.predefmaps);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
		Document doc = null;
		try {
			doc = db.parse(in);

			NodeList nl = doc.getDocumentElement().getElementsByTagName("map");
			for(int i = 0; i < nl.getLength(); i++){
				NamedNodeMap nnm =nl.item(i).getAttributes();

				Node nodeLayer = nnm.getNamedItem("layer");
				boolean ItIsLayer = false;
				if(nodeLayer != null)
					ItIsLayer = nodeLayer.getNodeValue().equalsIgnoreCase("true");

				if(pref.getBoolean("pref_predefmaps_"+nnm.getNamedItem("id").getNodeValue(), true) && !ItIsLayer){
					MenuItem item = submenu.add(nnm.getNamedItem("name").getNodeValue());
					item.setTitleCondensed(nnm.getNamedItem("id").getNodeValue());
					Log.e(DEBUGTAG, nnm.getNamedItem("name").getNodeValue());
				}
			}
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onPause() {
		SharedPreferences uiState = getPreferences(0);
		SharedPreferences.Editor editor = uiState.edit();
		editor.putString("MapName", mOsmv.getRenderer().ID);
		editor.putInt("Latitude", mOsmv.getMapCenterLatitudeE6());
		editor.putInt("Longitude", mOsmv.getMapCenterLongitudeE6());
		editor.putInt("ZoomLevel", mOsmv.getZoomLevel());
		editor.putBoolean("CompassEnabled", mCompassEnabled);
		editor.putString("app_version", Ut.getAppVersion(this));
		editor.commit();

		if (myWakeLock != null) {
			myWakeLock.release();
		}

		mOrientationSensorManager.unregisterListener(mListener);

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

    	SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

		if (pref.getBoolean("pref_keepscreenon", true)) {
			myWakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(
					PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "RMaps");
			myWakeLock.acquire();
		} else {
			myWakeLock = null;
		}

		if(mCompassEnabled)
			mOrientationSensorManager.registerListener(mListener, mOrientationSensorManager
				.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
	}

	private OpenStreetMapRendererInfo getRendererInfo(final Resources aRes, final SharedPreferences aPref, final String aName){
		OpenStreetMapRendererInfo RendererInfo = new OpenStreetMapRendererInfo(aRes, aName);
		RendererInfo.LoadFromResources(aName, PreferenceManager.getDefaultSharedPreferences(this));


		return RendererInfo;
	}

	private void restoreUIState() {
		SharedPreferences settings = getPreferences(Activity.MODE_PRIVATE);

		OpenStreetMapRendererInfo RendererInfo = getRendererInfo(getResources(), settings, settings.getString("MapName", "mapnik"));
		mOsmv.setRenderer(RendererInfo);

		this.mOsmv.getOverlays().clear();
		if(RendererInfo.YANDEX_TRAFFIC_ON == 1){
       		this.mOsmv.getOverlays().add(new YandexTrafficOverlay(this, this.mOsmv));
		}
        this.mOsmv.getOverlays().add(mMyLocationOverlay);

		mOsmv.setZoomLevel(settings.getInt("ZoomLevel", 0));
		mOsmv.setMapCenter(settings.getInt("Latitude", 0), settings.getInt("Longitude", 0));

		mCompassEnabled = settings.getBoolean("CompassEnabled", false);
		mCompassView.setVisibility(mCompassEnabled ? View.VISIBLE : View.INVISIBLE);

		setTitle();

		if(!settings.getString("app_version", "").equalsIgnoreCase(Ut.getAppVersion(this)))
			showDialog(R.id.whatsnew);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == 12345){
			startActivity(new Intent(this, this.getClass()));
			finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void setTitle(){
		final TextView leftText = (TextView) findViewById(R.id.left_text);
		if(leftText != null)
			leftText.setText(mOsmv.getRenderer().NAME);

		final TextView rightText = (TextView) findViewById(R.id.right_text);
		if(rightText != null){
			rightText.setText(mStatusListener + " " + (1+mOsmv.getZoomLevel()));
		}
	}

	private class MainActivityCallbackHandler extends Handler{
		@Override
		public void handleMessage(final Message msg) {
			final int what = msg.what;
			switch(what){
				case R.id.user_moved_map:
					Log.d(DEBUGTAG, "user_moved_map");
					setAutoFollow(false);
					break;
			}
		}
	}



}
