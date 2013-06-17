package com.robert.maps.applib.dashboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

import org.andnav.osm.util.GeoPoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.robert.maps.applib.MainActivity;
import com.robert.maps.applib.R;
import com.robert.maps.applib.utils.CoordFormatter;
import com.robert.maps.applib.utils.DistanceFormatter;
import com.robert.maps.applib.utils.Ut;

public class IndicatorManager implements Indicator {
	private HashMap<String, Object> mIndicators = new HashMap<String, Object>();
	private HashMap<String, String> mIndicatorTitles = new HashMap<String, String>();
	private SampleLocationListener mLocationListener = new SampleLocationListener();
	private ArrayList<IndicatorView> mIndicatorViewList = new ArrayList<IndicatorView>();
	private final CoordFormatter mCf;
	private final DistanceFormatter mDf;
	private final SimpleDateFormat sdf;

	public IndicatorManager(MainActivity ctx) {
		mCf = new CoordFormatter(ctx);
		mDf = new DistanceFormatter(ctx);
        final Configuration config = ctx.getResources().getConfiguration();
		sdf = new SimpleDateFormat("HH:mm:ss", config.locale);

		setUpIndicators(ctx);

       	initView(ctx, (ViewGroup) ctx.findViewById(R.id.dashboard_area));

		((LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE)).requestLocationUpdates(GPS, 0, 0, mLocationListener);
	}
	
	private void setUpIndicators(Context ctx) {
		final Resources res = ctx.getResources();
		// GPS indicators
		putIndicator(GPSACCURACY, res.getString(R.string.dashboard_title_gps_accuracy), mDf.formatDistance2(0));
		putIndicator(GPSELEV, res.getString(R.string.dashboard_title_gps_altitude), mDf.formatDistance2(0));
		putIndicator(GPSBEARING, res.getString(R.string.dashboard_title_gps_bearing), EMPTY);
		putIndicator(GPSTIME, res.getString(R.string.dashboard_title_gps_time), EMPTY);
		putIndicator(GPSLAT, res.getString(R.string.dashboard_title_gps_latitude), EMPTY);
		putIndicator(GPSLON, res.getString(R.string.dashboard_title_gps_longitude), EMPTY);
		putIndicator(GPSPROVIDER, res.getString(R.string.dashboard_title_gps_provider), "off");
		putIndicator(GPSSPEED, res.getString(R.string.dashboard_title_gps_speed), mDf.formatSpeed2(0));
		// Map indicators
		putIndicator(MAPNAME, res.getString(R.string.dashboard_title_map_name), EMPTY);
		putIndicator(MAPCENTERLAT, res.getString(R.string.dashboard_title_map_center_lat), EMPTY);
		putIndicator(MAPCENTERLON, res.getString(R.string.dashboard_title_map_center_lon), EMPTY);
		putIndicator(MAPZOOM, res.getString(R.string.dashboard_title_map_zoom), EMPTY);
	}
	
	private void putIndicator(String tag, String title, Object initValue) {
		mIndicators.put(tag, initValue);
		mIndicatorTitles.put(tag, title);
	}
	
	public HashMap<String, Object> getIndicators() {
		return mIndicators;
	}

	public void setCenter(GeoPoint point) {
		mIndicators.put(MAPCENTERLAT, mCf.convertLat(point.getLatitude()));
		mIndicators.put(MAPCENTERLON, mCf.convertLon(point.getLongitude()));
		
		updateIndicatorViewValues();
	}
	
	public void setZoom(int zoom) {
		mIndicators.put(MAPZOOM, zoom);
		
		updateIndicatorViewValues();
	}
	
	public void setMapName(String name) {
		mIndicators.put(MAPNAME, name);
		
		updateIndicatorViewValues();
	}
	
	public void Pause(Context ctx) {
		((LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE)).removeUpdates(mLocationListener);
	}
	
	public void Resume(Context ctx) {
		((LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE)).requestLocationUpdates(GPS, 0, 0, mLocationListener);
	}
	
	public void Dismiss(MainActivity ctx) {
		((LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE)).removeUpdates(mLocationListener);
		
		final JSONObject json = new JSONObject();
		try {
			json.put(JNAME, JMAIN);
			final JSONArray jarrv = new JSONArray();
			
			ViewGroup llv = (ViewGroup) ctx.findViewById(R.id.dashboard_area);
			for(int i = 0; i < llv.getChildCount(); i++) {
				final JSONArray jarr = new JSONArray();
				ViewGroup ll = (ViewGroup) llv.getChildAt(i);
				
				for(int j = 0; j < ll.getChildCount(); j++) {
					final IndicatorView iv = (IndicatorView) ll.getChildAt(j);
					final JSONObject jiv = new JSONObject();
					jiv.put(JINDEX, j);
					jiv.put(JTAG, iv.getIndicatorTag());
					jarr.put(jiv);
				}
				
				jarrv.put(jarr);
			}
			json.put(JINDICATORS, jarrv);
			
			final File folder = Ut.getRMapsMainDir(ctx, DASHBOARD_DIR);
			if(folder.exists()) {
				FileWriter writer = new FileWriter(String.format(DASHBOARD_FILE, folder.getAbsolutePath(), JMAIN));
				writer.write(json.toString());
				writer.close();
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		mIndicatorViewList.clear();
		((ViewGroup) ((MainActivity) ctx).findViewById(R.id.dashboard_area)).removeAllViews();
	}
	
	private class SampleLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			if(location != null) {
				mIndicators.put(GPSACCURACY, mDf.formatDistance2(location.getAccuracy()));
				mIndicators.put(GPSELEV, mDf.formatDistance2(location.getAltitude()));
				mIndicators.put(GPSBEARING, String.format(Locale.UK, "%.1f�", location.getBearing()));
				mIndicators.put(GPSTIME, sdf.format(Long.valueOf(location.getTime())));
				mIndicators.put(GPSLAT, mCf.convertLat(Double.valueOf(location.getLatitude())));
				mIndicators.put(GPSLON, mCf.convertLon(Double.valueOf(location.getLongitude())));
				mIndicators.put(GPSPROVIDER, location.getProvider());
				mIndicators.put(GPSSPEED, mDf.formatSpeed2(location.getSpeed()));
				
				updateIndicatorViewValues();
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			Ut.e("onProviderDisabled="+provider);
		}

		@Override
		public void onProviderEnabled(String provider) {
			Ut.e("onProviderEnabled="+provider);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Ut.e("onStatusChanged="+provider+" "+status);
		}
		
	}

	private void initView(Context context, ViewGroup viewGroup) {
		final File folder = Ut.getRMapsMainDir(context, DASHBOARD_DIR);
		if (folder.exists()) {
			final File file = new File(String.format(DASHBOARD_FILE, folder.getAbsolutePath(), JMAIN));
			if (file.exists()) {
				FileInputStream fis;
				try {
					fis = new FileInputStream(file);
					final BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
					StringBuilder sb = new StringBuilder();
					String line = null;
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}
					reader.close();
					fis.close();

					final JSONObject json = new JSONObject(sb.toString());
					if (json.get(JNAME).equals(JMAIN)) {
						final JSONArray jarrv = json.getJSONArray(JINDICATORS);
						for (int i = 0; i < jarrv.length(); i++) {
							final LinearLayout ll = new LinearLayout(context);
							ll.setOrientation(LinearLayout.HORIZONTAL);
							ll.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
							viewGroup.addView(ll);
							
							final JSONArray jarr = jarrv.getJSONArray(i);
							
							for(int j = 0; j < jarr.length(); j++) {
								final JSONObject jiv = jarr.getJSONObject(j);
								ll.addView(addIndicatorView(context, R.layout.indicator_simple, jiv.getString(JTAG), EMPTY, EMPTY), jiv.getInt(JINDEX), new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
							}
						}
					}
					
					return;
					
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		
		// if json file not exists
		LinearLayout ll = new LinearLayout(context); // new BoardView(context);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSLAT, EMPTY, EMPTY), 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSLON, EMPTY, EMPTY), 1, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));

		viewGroup.addView(ll);

		ll = new LinearLayout(context); // new BoardView(context);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSSPEED, EMPTY, EMPTY), 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSELEV, EMPTY, EMPTY), 1, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSBEARING, EMPTY, EMPTY), 2, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));

		viewGroup.addView(ll);
}
	
	private IndicatorView addIndicatorView(Context context, int resId, String tag, String header, String units) {
		final IndicatorView iv = (IndicatorView) LayoutInflater.from(context).inflate(resId, null, false);
		iv.setIndicatorTag(tag);
		iv.setIndicatorManager(this);
		((TextView) iv.findViewById(R.id.data_header)).setText(mIndicatorTitles.get(tag).toUpperCase());
		((TextView) iv.findViewById(R.id.data_unit)).setText(units);
		iv.updateIndicator(this);
		mIndicatorViewList.add(iv);
		return iv;
	}
	
	public void addIndicatorView(Context ctx, IndicatorView iv, String tag, boolean toNextLine) {
		LinearLayout ll = (LinearLayout) iv.getParent();
		final LinearLayout llv = (LinearLayout) ll.getParent();
		
		if(toNextLine) {
			ll = new LinearLayout(ctx); // new BoardView(context);
			ll.setOrientation(LinearLayout.HORIZONTAL);
			ll.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			
			llv.addView(ll);
		}
			
		ll.addView(addIndicatorView(ctx, R.layout.indicator_simple, tag, EMPTY, EMPTY), ll.getChildCount(), new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
	}
	
	public void updateIndicatorViewValues() {
		IndicatorView iv = null;
		Iterator<IndicatorView> it = mIndicatorViewList.iterator();
		while(it.hasNext()) {
			iv = it.next();
			iv.updateIndicator(this);
		}
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.add(0, R.id.menu_dashboard_delete, 0, R.string.menu_delete);
		menu.add(0, R.id.menu_dashboard_add, 0, R.string.menu_add);
		menu.add(0, R.id.menu_dashboard_add_line, 0, R.string.menu_dashboard_add_line);
		
		Entry<String, String> entry;
		int ind = 0;
		final Iterator<Entry<String, String>> it = mIndicatorTitles.entrySet().iterator();
		while(it.hasNext()) {
			entry = it.next();
			
			final MenuItem item = menu.add(R.id.menu_dashboard_edit, R.id.menu_dashboard_edit, ind, entry.getValue());
			item.setTitleCondensed(entry.getKey());
			
			ind++;
		}
	}

	public void putTagToIndicatorView(IndicatorView iv, String tag) {
		iv.setIndicatorTag(tag);
		((TextView) iv.findViewById(R.id.data_header)).setText(mIndicatorTitles.get(tag).toUpperCase());
		
		updateIndicatorViewValues();
	}

	public void removeIndicatorView(MainActivity ctx, IndicatorView iv) {
		final LinearLayout ll = (LinearLayout) iv.getParent();
		final LinearLayout llv = (LinearLayout) ll.getParent();
		
		if(ll.getChildCount() == 1 && llv.getChildCount() == 1) {
			Toast.makeText(ctx, R.string.dashboard_message_cant_remove_last_indicator, Toast.LENGTH_LONG).show();
			return;
		}
		
		mIndicatorViewList.remove(iv);
		ll.removeView(iv);
		
		if(ll.getChildCount() == 0) {
			llv.removeView(ll);
		}
	}
}
