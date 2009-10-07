package com.robert.maps.kml;

import org.andnav.osm.util.GeoPoint;

import android.content.Context;

public class PoiManager {
	protected final Context mCtx;
	private GeoDatabase mGeoDatabase;

	public PoiManager(Context ctx) {
		super();
		mCtx = ctx;
		mGeoDatabase = new GeoDatabase(ctx);
	}


	public void addPoi(GeoPoint point){
		mGeoDatabase.addPoi("testpoi", "Test POI 1", point.getLatitude(), point.getLongitude(), 0, 0, 0);
	}
}
