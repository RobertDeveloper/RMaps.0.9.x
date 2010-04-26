package com.robert.maps.kml;

import java.util.ArrayList;
import java.util.List;

import org.andnav.osm.util.GeoPoint;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.robert.maps.R;


public class PoiManager {
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
		mGeoDatabase.addPoi(title, descr, point.getLatitude(), point.getLongitude(), 0, 0, 0, 0, R.drawable.poi);
	}

	public void updatePoi(final PoiPoint point){
		if(point.getId() < 0)
			mGeoDatabase.addPoi(point.Title, point.Descr, point.GeoPoint.getLatitude(), point.GeoPoint.getLongitude(), point.Alt, point.CategoryId, point.PointSourceId, point.Hidden == true ? 1 : 0, point.IconId);
		else
			mGeoDatabase.updatePoi(point.getId(), point.Title, point.Descr, point.GeoPoint.getLatitude(), point.GeoPoint.getLongitude(), point.Alt, point.CategoryId, point.PointSourceId, point.Hidden == true ? 1 : 0, point.IconId);
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

	public List<PoiPoint> getPoiListNotHidden(int zoom, GeoPoint center, GeoPoint lefttop){
		return doCreatePoiListFromCursor(mGeoDatabase.getPoiListNotHiddenCursor(zoom, lefttop.getLongitude(), lefttop
				.getLongitude()
				+ 2 * (center.getLongitude() - lefttop.getLongitude()), lefttop.getLatitude(), lefttop
				.getLatitude()
				+ 2 * (center.getLatitude() - lefttop.getLatitude())));
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
				category = new PoiCategory(id, c.getString(0), c.getInt(2) == 1 ? true : false, c.getInt(3), c.getInt(4));
			c.close();
		}

		return category;
	}

	public void updatePoiCategory(PoiCategory poiCategory) {
		if(poiCategory.getId() < 0)
			mGeoDatabase.addPoiCategory(poiCategory.Title, poiCategory.Hidden == true ? 1 : 0, poiCategory.IconId);
		else
			mGeoDatabase.updatePoiCategory(poiCategory.getId(), poiCategory.Title, poiCategory.Hidden == true ? 1 : 0, poiCategory.IconId, poiCategory.MinZoom);
	}

	public void DeleteAllPoi() {
		mGeoDatabase.DeleteAllPoi();
	}
}
