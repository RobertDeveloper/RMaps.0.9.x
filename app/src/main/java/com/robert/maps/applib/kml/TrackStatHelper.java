package com.robert.maps.applib.kml;

import android.location.Location;

import java.util.Date;

public class TrackStatHelper {
	public Date Date1;
	public Date Date2;
	public double MaxSpeed;
	public double AvgSpeed;
	public double AvgPace;
	public double MinEle;
	public double MaxEle;
	public long MoveTime;
	public double AvgMoveSpeed;
	
	public int Cnt;
	public double Distance;
	public long Duration;
	
	private Location lastpt = null;
	private float[] results = { 0 };
	
	public void addPoint(Location pt) {
		addPoint(pt.getLatitude(), pt.getLongitude(), pt.getAltitude(), pt.getSpeed(), new Date(pt.getTime()));
	}
	
	public void finalCalc() {
		if (lastpt != null)
			Date2 = new Date(lastpt.getTime());
		if (MoveTime > 0) {
			AvgMoveSpeed = Distance / (MoveTime / 1000);
		}
		
		if (Duration > 0)
			AvgSpeed = Distance / (Duration / 1000);
		if (Distance > 0)
			AvgPace = Duration / 1000 / (Distance / 1000);
	}
	
	public void addPoint(double lat, double lon, double alt, double speed, Date date) {
		Cnt++;
		
		if (lastpt == null) {
			lastpt = new Location("");
			Date1 = date;
			MaxSpeed = 0.0;
			MinEle = alt;
			MaxEle = alt;
			
			Distance = 0;
			Duration = 0;
			MoveTime = 0;
		} else {
			if (speed > MaxSpeed)
				MaxSpeed = speed;
			if (alt > MaxEle)
				MaxEle = alt;
			if (alt < MinEle)
				MinEle = alt;
			if (lastpt.getSpeed() > 0.5)
				MoveTime += date.getTime() - lastpt.getTime();
			
			results[0] = 0;
			try {
				Location.distanceBetween(lastpt.getLatitude(), lastpt.getLongitude(), lat, lon, results);
				Distance += results[0];
			} catch (Exception e) {
			}
			
			Duration = date.getTime() - Date1.getTime();

			Date2 = new Date(date.getTime());
			
			if (MoveTime > 0) 
				AvgMoveSpeed = Distance / (MoveTime / 1000);
			if (Duration > 0)
				AvgSpeed = Distance / (Duration / 1000);
			if (Distance > 0)
				AvgPace = Duration / 1000 / (Distance / 1000);
		}
		
		lastpt.setLatitude(lat);
		lastpt.setLongitude(lon);
		lastpt.setAltitude(alt);
		lastpt.setSpeed((float) speed);
		lastpt.setTime(date.getTime());
	}
}
