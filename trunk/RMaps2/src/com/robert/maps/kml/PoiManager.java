package com.robert.maps.kml;

import java.util.ArrayList;
import java.util.List;

import org.andnav.osm.util.GeoPoint;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.robert.maps.R;
import com.robert.maps.kml.Track.TrackPoint;
import com.robert.maps.kml.constants.PoiConstants;


public class PoiManager implements PoiConstants {
	protected final Context mCtx;
	private GeoDatabase mGeoDatabase;

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

	public void addPoi(final String title, final String descr, GeoPoint point){
		mGeoDatabase.addPoi(title, descr, point.getLatitude(), point.getLongitude(), ZERO, ZERO, ZERO, ZERO, R.drawable.poi);
	}

	public void updatePoi(final PoiPoint point){
		if(point.getId() < 0)
			mGeoDatabase.addPoi(point.Title, point.Descr, point.GeoPoint.getLatitude(), point.GeoPoint.getLongitude(), point.Alt, point.CategoryId, point.PointSourceId, point.Hidden == true ? ONE : ZERO, point.IconId);
		else
			mGeoDatabase.updatePoi(point.getId(), point.Title, point.Descr, point.GeoPoint.getLatitude(), point.GeoPoint.getLongitude(), point.Alt, point.CategoryId, point.PointSourceId, point.Hidden == true ? ONE : ZERO, point.IconId);
	}

	private List<PoiPoint> doCreatePoiListFromCursor(Cursor c){
		final ArrayList<PoiPoint> items = new ArrayList<PoiPoint>();
		if (c != null) {
			if (c.moveToFirst()) {
				do {
					items.add(new PoiPoint(c.getInt(4), c.getString(2), c.getString(3), new GeoPoint(
							(int) (1E6 * c.getDouble(0)), (int) (1E6 * c.getDouble(1))), c.getInt(7), c.getInt(8)));
				} while (c.moveToNext());
			}
			c.close();
		}

		return items;
	}

	public List<PoiPoint> getPoiList() {
		return doCreatePoiListFromCursor(mGeoDatabase.getPoiListCursor());
	}

	public List<PoiPoint> getPoiListNotHidden(int zoom, GeoPoint center, double deltaX, double deltaY){
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
						.getInt(5), c.getInt(8), c.getInt(6));
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
			long newId = mGeoDatabase.addTrack(track.Name, track.Descr, track.Show ? ONE : ZERO);

			for(TrackPoint trackpoint: track.getPoints()){
				//Ut.dd("lat="+trackpoint.lat);
				mGeoDatabase.addTrackPoint(newId, trackpoint.lat, trackpoint.lon, trackpoint.alt, trackpoint.speed, trackpoint.date);
			}
		} else
			mGeoDatabase.updateTrack(track.getId(), track.Name, track.Descr, track.Show ? ONE : ZERO);
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

	public Track getTrackChecked(){
		Track track = null;
		Cursor c = mGeoDatabase.getTrackChecked();
		if (c != null) {
			if (c.moveToFirst())
				track = new Track(c.getInt(3), c.getString(0), c.getString(1), c.getInt(2) == ONE ? true : false);
			else {
				c.close();
				return null;
			}
			c.close();

			c = mGeoDatabase.getTrackPoints(track.getId());
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						track.AddTrackPoint(); //track.trackpoints.size()
						track.LastTrackPoint.lat = c.getDouble(0);
						track.LastTrackPoint.lon = c.getDouble(1);
					} while (c.moveToNext());
				}
				c.close();
			}

		}
		return track;
	}

	public Track getTrack(int id){
		Track track = null;
		Cursor c = mGeoDatabase.getTrack(id);
		if (c != null) {
			if (c.moveToFirst())
				track = new Track(id, c.getString(0), c.getString(1), c.getInt(2) == ONE ? true : false);
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

}
