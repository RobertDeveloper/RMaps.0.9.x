package com.robert.maps.applib.kml;

import java.util.Date;
import java.util.HashMap;

import org.andnav.osm.util.GeoPoint;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.SparseArray;

import com.robert.maps.applib.R;
import com.robert.maps.applib.kml.Track.TrackPoint;
import com.robert.maps.applib.kml.constants.PoiConstants;


public class PoiManager implements PoiConstants {
	protected final Context mCtx;
	private GeoDatabase mGeoDatabase;
	private boolean mStopProcessing;

	public PoiManager(Context ctx) {
		super();
		mCtx = ctx;
		mGeoDatabase = new GeoDatabase(ctx);
	}

	public GeoDatabase getGeoDatabase(){
		return mGeoDatabase;
	}

	public void FreeDatabases(){
		mGeoDatabase.FreeDatabases();
	}
	
	public void StopProcessing() {
		mStopProcessing = true;
	}
	
	private boolean Stop() {
		if(mStopProcessing) {
			mStopProcessing = false;
			return true;
		}
		return false;
	}

	public void addPoi(final String title, final String descr, GeoPoint point){
		mGeoDatabase.addPoi(title, descr, point.getLatitude(), point.getLongitude(), ZERO, ZERO, ZERO, ZERO, R.drawable.poi);
	}

	public void updatePoi(final PoiPoint point){
		if(point.getId() < 0)
			mGeoDatabase.addPoi(point.Title, point.Descr, point.GeoPoint.getLatitude(), point.GeoPoint.getLongitude(), point.Alt, point.CategoryId, point.PointSourceId, point.Hidden == true ? ONE : ZERO, point.IconId);
		else
			mGeoDatabase.updatePoi(point.getId(), point.Title, point.Descr, point.GeoPoint.getLatitude(), point.GeoPoint.getLongitude(), point.Alt, point.CategoryId, point.PointSourceId, point.Hidden == true ? ONE : ZERO, point.IconId);
	}

	private SparseArray<PoiPoint> doCreatePoiListFromCursor(Cursor c){
		final SparseArray<PoiPoint> items = new SparseArray<PoiPoint>();
		if (c != null) {
			if (c.moveToFirst()) {
				do {
					items.put(c.getInt(4), new PoiPoint(c.getInt(4), c.getString(2), c.getString(3), new GeoPoint(
							(int) (1E6 * c.getDouble(0)), (int) (1E6 * c.getDouble(1))), c.getInt(7), c.getInt(8)));
				} while (c.moveToNext());
			}
			c.close();
		}

		return items;
	}

	public SparseArray<PoiPoint> getPoiList() {
		return doCreatePoiListFromCursor(mGeoDatabase.getPoiListCursor());
	}

	public SparseArray<PoiPoint> getPoiListNotHidden(int zoom, GeoPoint center, double deltaX, double deltaY){
		return doCreatePoiListFromCursor(mGeoDatabase.getPoiListNotHiddenCursor(zoom, center.getLongitude() - deltaX, center.getLongitude() + deltaX
				, center.getLatitude() + deltaY, center.getLatitude() - deltaY));
	}

	public void addPoiStartActivity(Context ctx, GeoPoint touchDownPoint) {
		ctx.startActivity((new Intent(ctx, PoiActivity.class)).putExtra(LAT,
				touchDownPoint.getLatitude()).putExtra(LON,
				touchDownPoint.getLongitude()));
	}


	public PoiPoint getPoiPoint(int id) {
		PoiPoint point = null;
		final Cursor c = mGeoDatabase.getPoi(id);
		if (c != null) {
			if (c.moveToFirst())
				point = new PoiPoint(c.getInt(4), c.getString(2), c
						.getString(3), new GeoPoint(
						(int) (1E6 * c.getDouble(0)), (int) (1E6 * c
								.getDouble(1))), c.getInt(9), c.getInt(7), c
						.getDouble(5), c.getInt(8), c.getInt(6));
			c.close();
		}

		return point;
	}

	public void deletePoi(final int id){
		mGeoDatabase.deletePoi(id);
	}

	public void deletePoiCategory(final int id){
		mGeoDatabase.deletePoiCategory(id);
	}

	public PoiCategory getPoiCategory(int id) {
		PoiCategory category = null;
		final Cursor c = mGeoDatabase.getPoiCategory(id);
		if (c != null) {
			if (c.moveToFirst())
				category = new PoiCategory(id, c.getString(0), c.getInt(2) == ONE ? true : false, c.getInt(3), c.getInt(4));
			c.close();
		}

		return category;
	}

	public void updatePoiCategory(PoiCategory poiCategory) {
		if(poiCategory.getId() < ZERO)
			mGeoDatabase.addPoiCategory(poiCategory.Title, poiCategory.Hidden == true ? ONE : ZERO, poiCategory.IconId);
		else
			mGeoDatabase.updatePoiCategory(poiCategory.getId(), poiCategory.Title, poiCategory.Hidden == true ? ONE : ZERO, poiCategory.IconId, poiCategory.MinZoom);
	}

	public void DeleteAllPoi() {
		mGeoDatabase.DeleteAllPoi();
	}

	public void beginTransaction(){
		mGeoDatabase.beginTransaction();
	}

	public void rollbackTransaction(){
		mGeoDatabase.rollbackTransaction();
	}

	public void commitTransaction(){
		mGeoDatabase.commitTransaction();
	}

	public void updateTrack(Track track) {
		if(track.getId() < 0){
			long newId = mGeoDatabase.addTrack(track.Name, track.Descr, track.Show ? ONE : ZERO, track.Cnt, track.Distance, track.Duration, track.Category, track.Activity, track.Date, track.Style);

			for(TrackPoint trackpoint: track.getPoints()){
				mGeoDatabase.addTrackPoint(newId, trackpoint.lat, trackpoint.lon, trackpoint.alt, trackpoint.speed, trackpoint.date);
			}
		} else
			mGeoDatabase.updateTrack(track.getId(), track.Name, track.Descr, track.Show ? ONE : ZERO, track.Cnt, track.Distance, track.Duration, track.Category, track.Activity, track.Date, track.Style);
	}
	
	

	public boolean haveTrackChecked(){
		boolean ret = false;
		Cursor c = mGeoDatabase.getTrackChecked();
		if (c != null) {
			if (c.moveToFirst())
				ret = true;
			c.close();
		}

		return ret;
	}

	public Track[] getTrackChecked(){
		return getTrackChecked(true);
	}

	public Track[] getTrackChecked(final boolean aNeedPoints){
		mStopProcessing = false;
		Track tracks[] = null;
		Cursor c = mGeoDatabase.getTrackChecked();
		if (c != null) {
			tracks = new Track[c.getCount()];
			final String defStyle = PreferenceManager.getDefaultSharedPreferences(mCtx).getString("pref_track_style", "");
			
			if (c.moveToFirst())
				do {
					final int pos = c.getPosition();
					String style = c.getString(10);
					if(style == null || style.equalsIgnoreCase(""))
						style = "";

					tracks[pos] = new Track(c.getInt(3), c.getString(0), c.getString(1), c.getInt(2) == ONE ? true : false, c.getInt(4), c.getDouble(5), c.getDouble(6), c.getInt(7), c.getInt(8), new Date(c.getLong(9)*1000), style, defStyle);
					
					if (aNeedPoints) {
						Cursor cpoints = mGeoDatabase.getTrackPoints(tracks[pos].getId());
						if (cpoints != null) {
							if (cpoints.moveToFirst()) {
								do {
									if (Stop()) {
										tracks[pos] = null;
										break;
									}
									tracks[pos].AddTrackPoint(); //track.trackpoints.size()
									tracks[pos].LastTrackPoint.lat = cpoints.getDouble(0);
									tracks[pos].LastTrackPoint.lon = cpoints.getDouble(1);
									tracks[pos].LastTrackPoint.alt = cpoints.getDouble(2);
									tracks[pos].LastTrackPoint.speed = cpoints.getDouble(3);
									tracks[pos].LastTrackPoint.date.setTime(cpoints.getLong(4) * 1000); // System.currentTimeMillis()
								} while (cpoints.moveToNext());
							}
							cpoints.close();
						}
					}
				} while (c.moveToNext());
			else {
				c.close();
				return null;
			}
			c.close();


		}
		return tracks;
	}

	public Track getTrack(int id){
		Track track = null;
		Cursor c = mGeoDatabase.getTrack(id);
		if (c != null) {
			if (c.moveToFirst()) {
				final String defStyle = PreferenceManager.getDefaultSharedPreferences(mCtx).getString("pref_track_style", "");
				String style = c.getString(9);
				if(style == null || style.equalsIgnoreCase(""))
					style = "";

				track = new Track(id, c.getString(0), c.getString(1), c.getInt(2) == ONE ? true : false, c.getInt(3), c.getDouble(4), c.getDouble(5), c.getInt(6), c.getInt(7), new Date(c.getLong(8)*1000), style, defStyle);
			};
			c.close();
			c = null;

			c = mGeoDatabase.getTrackPoints(id);
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						track.AddTrackPoint();
						track.LastTrackPoint.lat = c.getDouble(0);
						track.LastTrackPoint.lon = c.getDouble(1);
						track.LastTrackPoint.alt = c.getDouble(2);
						track.LastTrackPoint.speed = c.getDouble(3);
						track.LastTrackPoint.date.setTime(c.getLong(4) * 1000); // System.currentTimeMillis()
					} while (c.moveToNext());
				}
				c.close();
			}

		}

		return track;
	}

	public void setTrackChecked(int id) {
		mGeoDatabase.setTrackChecked(id);
	}

	public void deleteTrack(int id) {
		mGeoDatabase.deleteTrack(id);

	}
	
	public long addMap(int type, String params) {
		return mGeoDatabase.addMap(type, params);
	}


}
