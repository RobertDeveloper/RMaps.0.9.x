package com.robert.maps.kml;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.andnav.osm.util.GeoPoint;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

import com.robert.maps.kml.constants.PoiConstants;

public class Track implements PoiConstants {
	public static final String COLOR = "color";
	public static final String COLORSHADOW = "color_shadow";
	public static final String WIDTH = "width";
	public static final String SHADOWRADIUS = "shadowradius";

	private final int Id;
	public String Name;
	public String Descr;
	public TrackPoint LastTrackPoint;
	public boolean Show;
	public int Cnt;
	public double Distance;
	public double Duration;
	public int Category;
	public int Activity;
	public Date Date;
	public int Color;
	public int ColorShadow;
	public int Width;
	public double ShadowRadius;
	public String Style;
	public String DefaultStyle;

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
		this(EMPTY_ID, "", "", false, 0, 0, 0, 0, 0, new Date(0), "", "");
	}

	public Track(final String style) {
		this(EMPTY_ID, "", "", false, 0, 0, 0, 0, 0, new Date(0), style, style);
	}

	public Track(final int id, final String name, final String descr, final boolean show, final int cnt, final double distance, final double duration, final int category, final int activity, final Date date, final String style, final String defaultStyle) {
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
		Style = style;
		DefaultStyle = defaultStyle;
		
		try {
			final JSONObject json = new JSONObject(Style.equalsIgnoreCase("") ? DefaultStyle : Style);
			Color = json.optInt(COLOR, 0xffA565FE);
			Width = json.optInt(WIDTH, 4);
			ShadowRadius = json.optDouble(SHADOWRADIUS, 0);
			ColorShadow = json.optInt(COLORSHADOW, 0xffA565FE);
		} catch (Exception e) {
			Color = 0xffA565FE;
			Width = 4;
			ShadowRadius = 0;
			ColorShadow = 0xffA565FE;
		}
	}
	
	public String getStyle() {
		final JSONObject json = new JSONObject();
		try {
			json.put(COLOR, Color);
			json.put(COLORSHADOW, ColorShadow);
			json.put(WIDTH, Width);
			json.put(SHADOWRADIUS, ShadowRadius);
		} catch (JSONException e) {
		}
		return json.toString();
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
	
	public class Stat{
		public Date Date1;
		public Date Date2;
		public double MaxSpeed;
		public double AvgSpeed;
		public double AvgPace;
		public double MinEle;
		public double MaxEle;
		public int MoveTime;
		public double AvgMoveSpeed;
	};

	public Stat CalculateStatFull() {
		TrackPoint lastpt = null;
		final Stat stat = new Stat();
		
		if(Duration > 0)
			stat.AvgSpeed = (Distance / 1000) / (Duration/60/60);
		if(Distance > 0)
			stat.AvgPace = Duration / (Distance / 1000);
		
		for(TrackPoint pt : trackpoints){
			if(lastpt == null){
				stat.Date1 = pt.date;
				stat.MaxSpeed = 0.0;
				stat.MinEle = pt.alt;
				stat.MaxEle = pt.alt;
			} else {
				if(pt.speed > stat.MaxSpeed)
					stat.MaxSpeed = pt.speed;
				if(pt.alt > stat.MaxEle)
					stat.MaxEle = pt.alt;
				if(pt.alt < stat.MinEle)
					stat.MinEle = pt.alt;
				if(lastpt.speed > 0.5)
					stat.MoveTime += pt.date.getTime() - lastpt.date.getTime();
				
			}
			lastpt = pt;
		}
		if (lastpt != null)
			stat.Date2 = lastpt.date;
		if(stat.MoveTime > 0){
			stat.AvgMoveSpeed = (Distance / 1000) / (stat.MoveTime/1000/60.0/60.0);
		}
		
		stat.MaxSpeed *= 3.6;
		
		return stat;
	}
}
