package com.robert.maps.kml;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.andnav.osm.util.GeoPoint;

import com.robert.maps.kml.constants.PoiConstants;

public class Track implements PoiConstants {

	private final int Id;
	public String Name;
	public String Descr;
	public TrackPoint LastTrackPoint;
	public boolean Show;

	private List<TrackPoint> trackpoints = null;

	public class TrackPoint{
		public double lat;
		public double lon;
		public double alt;
		public double speed;
		public Date date;

		public TrackPoint() {
			lat = 0;
			lon = 0;
			alt = 0;
			speed = 0;
			date = new Date();
			//trackpoints = new ArrayList<TrackPoint>(1);
		}

		public int getLatitudeE6() {
			return (int) (lat * 1E6);
		}

		public int getLongitudeE6() {
			return (int) (lon * 1E6);
		}
	};
	
	public List<TrackPoint> getPoints(){
		if(trackpoints == null)
			return new ArrayList<TrackPoint>(0);
		
		return trackpoints;
	}

	public void AddTrackPoint(){
		LastTrackPoint = new TrackPoint();
		if(trackpoints == null)
			trackpoints = new ArrayList<TrackPoint>(1);
		trackpoints.add(LastTrackPoint);
	}

	public Track() {
		this(EMPTY_ID, "", "", false);
	}

	public Track(int id, String name, String descr, boolean show) {
		Id = id;
		Name = name;
		Descr = descr;
		Show = show;
	}

	public int getId() {
		return Id;
	}

	public GeoPoint getBeginGeoPoint() {
		if(trackpoints.size()>0)
			return new GeoPoint(trackpoints.get(0).getLatitudeE6(), trackpoints.get(0).getLongitudeE6());
		return null;
	}
}
