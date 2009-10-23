package com.robert.maps.kml;

import org.andnav.osm.util.GeoPoint;

import com.robert.maps.R;
import com.robert.maps.kml.constants.PoiConstants;

public class PoiPoint implements PoiConstants {

	private final int Id;
	public String Title;
	public String Descr;
	public GeoPoint GeoPoint;
	public int IconId;
	public double Alt;
	public int CategoryId;
	public int PointSourceId;
	public boolean Hidden;

	public PoiPoint(int id, String mTitle, String mDescr, GeoPoint mGeoPoint,
			int iconid, int categoryid, double alt, int sourseid, int hidden) {
		this.Id = id;
		this.Title = mTitle;
		this.Descr = mDescr;
		this.GeoPoint = mGeoPoint;
		this.IconId = iconid;
		this.Alt = alt;
		this.CategoryId = categoryid;
		this.PointSourceId = sourseid;
		this.Hidden = hidden == 1 ? true : false;
	}
	
	public PoiPoint(){
		this(EMPTY_ID, "", "", null, R.drawable.poi, 0, 0, 0, 0);
	}

	public PoiPoint(int id, String mTitle, String mDescr, GeoPoint mGeoPoint, int categoryid) {
		this(id, mTitle, mDescr, mGeoPoint, R.drawable.poi, categoryid, 0, 0, 0);

	}

	public PoiPoint(String mTitle, String mDescr, GeoPoint mGeoPoint, int iconid) {
		this(EMPTY_ID, mTitle, mDescr, mGeoPoint, iconid, 0, 0, 0, 0);
	}

	public int getId() {
		return Id;
	}
	
	public static int EMPTY_ID(){
		return EMPTY_ID;
	}

}
