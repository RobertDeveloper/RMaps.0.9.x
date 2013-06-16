package com.robert.maps.applib.dashboard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import org.andnav.osm.util.GeoPoint;

import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.robert.maps.applib.MainActivity;
import com.robert.maps.applib.R;
import com.robert.maps.applib.utils.CoordFormatter;
import com.robert.maps.applib.utils.DistanceFormatter;
import com.robert.maps.applib.utils.Ut;

public class IndicatorManager implements Indicator {
	private HashMap<String, Object> mIndicators = new HashMap<String, Object>();
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

		setUpIndicators();

       	initView(ctx, (ViewGroup) ctx.findViewById(R.id.dashboard_area));

		((LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE)).requestLocationUpdates(GPS, 0, 0, mLocationListener);
	}
	
	private void setUpIndicators() {
		// GPS indicators
		mIndicators.put(GPSACCURACY, mDf.formatDistance2(0));
		mIndicators.put(GPSELEV, mDf.formatDistance2(0));
		mIndicators.put(GPSBEARING, EMPTY);
		mIndicators.put(GPSTIME, EMPTY);
		mIndicators.put(GPSLAT, EMPTY);
		mIndicators.put(GPSLON, EMPTY);
		mIndicators.put(GPSPROVIDER, "off");
		mIndicators.put(GPSSPEED, mDf.formatSpeed2(0));
		// Map indicators
		mIndicators.put(MAPNAME, EMPTY);
		mIndicators.put(MAPCENTERLAT, EMPTY);
		mIndicators.put(MAPCENTERLON, EMPTY);
		mIndicators.put(MAPZOOM, EMPTY);
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
	
	public void Dismiss(Context ctx) {
		((LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE)).removeUpdates(mLocationListener);
		
		mIndicatorViewList.clear();
		((ViewGroup) ((MainActivity) ctx).findViewById(R.id.dashboard_area)).removeAllViews();
	}
	
	private class SampleLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			if(location != null) {
				mIndicators.put(GPSACCURACY, mDf.formatDistance2(location.getAccuracy()));
				mIndicators.put(GPSELEV, mDf.formatDistance2(location.getAltitude()));
				mIndicators.put(GPSBEARING, String.format(Locale.UK, "%.1f°", location.getBearing()));
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
		LinearLayout ll = new LinearLayout(context); // new BoardView(context);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		//ll.addView(addIndicatorView(context, R.layout.indicator_simple, MAPZOOM, "ZOOM", EMPTY), 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		//ll.addView(addIndicatorView(context, R.layout.indicator_simple, MAPNAME, "MAP NAME", EMPTY), 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		//ll.addView(addIndicatorView(context, R.layout.indicator_simple, MAPCENTERLAT, "CENTER LATITUDE", EMPTY), 1, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSLAT, GPSLAT, EMPTY), 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSLON, GPSLON, EMPTY), 1, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));

		viewGroup.addView(ll);

		ll = new LinearLayout(context); // new BoardView(context);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSACCURACY, GPSACCURACY, EMPTY), 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSELEV, GPSELEV, EMPTY), 1, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSBEARING, GPSBEARING, EMPTY), 2, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));

		viewGroup.addView(ll);

		ll = new LinearLayout(context); // new BoardView(context);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSTIME, GPSTIME, EMPTY), 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSSPEED, GPSSPEED, EMPTY), 1, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, GPSPROVIDER, GPSPROVIDER, EMPTY), 2, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));

		viewGroup.addView(ll);
}
	
	private IndicatorView addIndicatorView(Context context, int resId, String tag, String header, String units) {
		final IndicatorView iv = (IndicatorView) LayoutInflater.from(context).inflate(resId, null, false);
		iv.setIndicatorTag(tag);
		((TextView) iv.findViewById(R.id.data_header)).setText(header.toUpperCase());
		((TextView) iv.findViewById(R.id.data_unit)).setText(units);
		iv.updateIndicator(this);
		mIndicatorViewList.add(iv);
		return iv;
	}
	
	private void updateIndicatorViewValues() {
		IndicatorView iv = null;
		Iterator<IndicatorView> it = mIndicatorViewList.iterator();
		while(it.hasNext()) {
			iv = it.next();
			iv.updateIndicator(this);
		}
	}
}
