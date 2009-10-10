package com.robert.maps.kml;

import org.andnav.osm.util.GeoPoint;

public class PoiPoint {
	
	public final String mTitle;
	public final String mDescr;
	public final GeoPoint mGeoPoint;
	
	public PoiPoint(String mTitle, String mDescr, GeoPoint mGeoPoint) {
		this.mTitle = mTitle;
		this.mDescr = mDescr;
		this.mGeoPoint = mGeoPoint;
	}

}
