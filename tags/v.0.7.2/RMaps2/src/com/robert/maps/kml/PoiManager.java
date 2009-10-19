package com.robert.maps.kml;

import java.util.ArrayList;
import java.util.List;

import org.andnav.osm.util.GeoPoint;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;


public class PoiManager {
	protected final Context mCtx;
	private GeoDatabase mGeoDatabase;

	public PoiManager(Context ctx) {
		super();
		mCtx = ctx;
		mGeoDatabase = new GeoDatabase(ctx);
	}

	public void FreeDatabases(){
		mGeoDatabase.FreeDatabases();
	}

	public void addPoi(GeoPoint point){
		mGeoDatabase.addPoi("testpoi", "Test POI 1", point.getLatitude(), point.getLongitude(), 0, 0, 0);
	}

	public void addPoi(final String title, final String descr, GeoPoint point){
		mGeoDatabase.addPoi(title, descr, point.getLatitude(), point.getLongitude(), 0, 0, 0);
	}

	public void updatePoi(final PoiPoint point){
		if(point.getId() < 0)
			mGeoDatabase.addPoi(point.Title, point.Descr, point.GeoPoint.getLatitude(), point.GeoPoint.getLongitude(), point.Alt, point.CategoryId, point.PointSourceId);
		else 
			mGeoDatabase.updatePoi(point.getId(), point.Title, point.Descr, point.GeoPoint.getLatitude(), point.GeoPoint.getLongitude(), point.Alt, point.CategoryId, point.PointSourceId);
	}

	public List<PoiPoint> getPoiList() {
		final ArrayList<PoiPoint> items = new ArrayList<PoiPoint>();
		final Cursor c = mGeoDatabase.getPoiListCursor();
		if (c != null) {
			if (c.moveToFirst()) {
				do {
					items.add(new PoiPoint(c.getInt(4), c.getString(2), c.getString(3), new GeoPoint(
							(int) (1E6 * c.getDouble(0)), (int) (1E6 * c.getDouble(1)))));
				} while (c.moveToNext());
			}
			c.close();
		}

		return items;
	}


	public void addPoiStartActivity(Context ctx, GeoPoint touchDownPoint) {
		ctx.startActivity((new Intent(ctx, PoiActivity.class)).putExtra("lat",
				touchDownPoint.getLatitude()).putExtra("lon",
				touchDownPoint.getLongitude()));
	}


	public PoiPoint getPoiPoint(int id) {
		PoiPoint point = null;
		final Cursor c = mGeoDatabase.getPoi(id);
		if (c != null) {
			if (c.moveToFirst())
				point = new PoiPoint(c.getInt(4), c.getString(2), c.getString(3), new GeoPoint(
						(int) (1E6 * c.getDouble(0)), (int) (1E6 * c.getDouble(1))));
			c.close();
		}

		return point;
	}
	
	public void deletePoi(final int id){
		mGeoDatabase.deletePoi(id);
	}
}
