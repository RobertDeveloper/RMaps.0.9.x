package com.robert.maps.applib.dashboard;

import java.util.HashMap;

import org.andnav.osm.util.GeoPoint;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.LinearLayout.LayoutParams;

import com.robert.maps.applib.R;

public class IndicatorManager implements Indicator {
	private HashMap<String, Object> mIndicators = new HashMap<String, Object>();
	private SampleLocationListener mLocationListener = new SampleLocationListener();

	public IndicatorManager(Context ctx) {
		setUpIndicators();
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
		mIndicators.put(MAPCENTERLAT, point.getLatitude());
		mIndicators.put(MAPCENTERLON, point.getLongitude());
	}
	
	public void setZoom(int zoom) {
		mIndicators.put(MAPZOOM, zoom);
	}
	
	public void setMapName(String name) {
		mIndicators.put(MAPNAME, name);
	}
	
	public void Pause(Context ctx) {
		((LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE)).removeUpdates(mLocationListener);
	}
	
	public void Resume(Context ctx) {
		((LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE)).requestLocationUpdates(GPS, 0, 0, mLocationListener);
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
		//LinearLayout ll = (LinearLayout)LayoutInflater.from(context).inflate(R.layout.ind_test, null, false);
		LinearLayout ll = new BoardView(context);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		//LayoutInflater.from(context).inflate(R.layout.indicator_simple, ll, true);
		ll.addView((RelativeLayout)LayoutInflater.from(context).inflate(R.layout.indicator_simple, null, false), 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		ll.addView((RelativeLayout)LayoutInflater.from(context).inflate(R.layout.indicator_simple, null, false), 1, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		return ll;
		//return new BoardView(context, this);
	}
}
