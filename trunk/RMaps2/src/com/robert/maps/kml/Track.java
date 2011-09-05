package com.robert.maps.kml;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.andnav.osm.util.GeoPoint;

import android.location.Location;

import com.robert.maps.kml.constants.PoiConstants;

public class Track implements PoiConstants {

	private final int Id;
	public String Name;
	public String Descr;
	public TrackPoint LastTrackPoint;
	public boolean Show;
	public int Cnt;
	public double Distance;
	public double Duration;
	public int Category;
	public String Activity;
	public Date Date;

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
		this(EMPTY_ID, "", "", false, 0, 0, 0, 0, UNKNOWN, new Date(0));
	}

	public Track(final int id, final String name, final String descr, final boolean show, final int cnt, final double distance, final double duration, final int category, final String activity, final Date date) {
		Id = id;
		Name = name;
		Descr = descr;
		Show = show;
		Cnt = cnt;
		Distance = distance;
		Duration = duration;
		Category = category;
		Activity = activity;
		Date = date;
	}

	public int getId() {
		return Id;
	}

	public GeoPoint getBeginGeoPoint() {
		if(trackpoints.size()>0)
			return new GeoPoint(trackpoints.get(0).getLatitudeE6(), trackpoints.get(0).getLongitudeE6());
		return null;
	}
	
	public void CalculateStat() {
		Cnt = trackpoints.size();
		Duration = 0;
		if (trackpoints.size() > 0)
			Duration = (double) ((trackpoints.get(trackpoints.size() - 1).date.getTime() - trackpoints.get(0).date.getTime())/1000);
		TrackPoint lastpt = null;
		Distance = 0;
		float[] results = {0};
		
		for(TrackPoint pt : trackpoints){
			if(lastpt != null){
				results[0] = 0;
				try {
					Location.distanceBetween(lastpt.lat, lastpt.lon, pt.lat, pt.lon, results);
					Distance += results[0];
				} catch (Exception e) {
				}
			}
			lastpt = pt;
		}
	}
}
