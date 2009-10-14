package com.robert.maps.kml;

import org.andnav.osm.util.GeoPoint;

public class PoiPoint {
	
	private final int mId;
	public final String mTitle;
	public final String mDescr;
	public final GeoPoint mGeoPoint;
	
	public PoiPoint(String mTitle, String mDescr, GeoPoint mGeoPoint) {
		this.mId = -1;
		this.mTitle = mTitle;
		this.mDescr = mDescr;
		this.mGeoPoint = mGeoPoint;
	}

	public PoiPoint(int id, String mTitle, String mDescr, GeoPoint mGeoPoint) {
		this.mId = id;
		this.mTitle = mTitle;
		this.mDescr = mDescr;
		this.mGeoPoint = mGeoPoint;
	}

	public int getId() {
		return mId;
	}

}
