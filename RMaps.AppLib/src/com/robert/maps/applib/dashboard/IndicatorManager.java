package com.robert.maps.applib.dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.andnav.osm.util.GeoPoint;

import android.content.Context;
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

public class IndicatorManager implements Indicator {
	private HashMap<String, Object> mIndicators = new HashMap<String, Object>();
	private SampleLocationListener mLocationListener = new SampleLocationListener();
	private ArrayList<IndicatorView> mIndicatorViewList = new ArrayList<IndicatorView>();
	private CoordFormatter mCf;

	public IndicatorManager(MainActivity ctx) {
		mCf = new CoordFormatter(ctx);
		setUpIndicators();

       	((ViewGroup) ctx.findViewById(R.id.dashboard_area)).addView(getView(ctx));
}
	
	private void setUpIndicators() {
		// GPS indicators
		mIndicators.put(GPSACCURACY, Double.valueOf(0));
		mIndicators.put(GPSELEV, Double.valueOf(0));
		mIndicators.put(GPSBEARING, Double.valueOf(0));
		mIndicators.put(GPSTIME, Long.valueOf(0));
		mIndicators.put(GPSLAT, Double.valueOf(0));
		mIndicators.put(GPSLON, Double.valueOf(0));
		mIndicators.put(GPSPROVIDER, new String());
		mIndicators.put(GPSSPEED, Double.valueOf(0));
		// Map indicators
		mIndicators.put(MAPNAME, new String());
		mIndicators.put(MAPCENTERLAT, Double.valueOf(0));
		mIndicators.put(MAPCENTERLON, Double.valueOf(0));
		mIndicators.put(MAPZOOM, Integer.valueOf(0));
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
				mIndicators.put(GPSACCURACY, Double.valueOf(location.getAccuracy()));
				mIndicators.put(GPSELEV, Double.valueOf(location.getAltitude()));
				mIndicators.put(GPSBEARING, Double.valueOf(location.getBearing()));
				mIndicators.put(GPSTIME, Long.valueOf(location.getTime()));
				mIndicators.put(GPSLAT, Double.valueOf(location.getLatitude()));
				mIndicators.put(GPSLON, Double.valueOf(location.getLongitude()));
				mIndicators.put(GPSPROVIDER, location.getProvider());
				mIndicators.put(GPSSPEED, Double.valueOf(location.getSpeed()));
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
		
	}

	public LinearLayout getView(Context context) {
		LinearLayout ll = new LinearLayout(context); // new BoardView(context);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		//ll.addView(addIndicatorView(context, R.layout.indicator_simple, MAPZOOM, "ZOOM", ""), 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, MAPNAME, "MAP NAME", ""), 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		ll.addView(addIndicatorView(context, R.layout.indicator_simple, MAPCENTERLAT, "CENTER LATITUDE", ""), 1, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));

		return ll;
	}
	
	private IndicatorView addIndicatorView(Context context, int resId, String tag, String header, String units) {
		final IndicatorView iv = (IndicatorView) LayoutInflater.from(context).inflate(resId, null, false);
		iv.setIndicatorTag(tag);
		((TextView) iv.findViewById(R.id.data_header)).setText(header);
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
