package com.robert.maps.kml;

import org.andnav.osm.util.GeoPoint;

import com.robert.maps.R;
import com.robert.maps.kml.constants.PoiConstants;

public class PoiPoint implements PoiConstants {

	private final int Id;
	public String Title;
	public String Descr;
	public GeoPoint GeoPoint;
	public int pictureid;
	public double Alt;
	public int CategoryId;
	public int PointSourceId;

	public PoiPoint(int id, String mTitle, String mDescr, GeoPoint mGeoPoint,
			int picid) {
		this.Id = id;
		this.Title = mTitle;
		this.Descr = mDescr;
		this.GeoPoint = mGeoPoint;
		this.pictureid = picid;
		this.Alt = 0;
		this.CategoryId = 0;
		this.PointSourceId = 0;
	}
	
	public PoiPoint(){
		this(EMPTY_ID, "", "", null, R.drawable.poi);
	}

	public PoiPoint(int id, String mTitle, String mDescr, GeoPoint mGeoPoint) {
		this(id, mTitle, mDescr, mGeoPoint, R.drawable.poi);

	}

	public PoiPoint(String mTitle, String mDescr, GeoPoint mGeoPoint, int picid) {
		this(EMPTY_ID, mTitle, mDescr, mGeoPoint, picid);
	}

	public int getId() {
		return Id;
	}
	
	public static int EMPTY_ID(){
		return EMPTY_ID;
	}

}
