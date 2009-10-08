package com.robert.maps.kml;

import java.util.ArrayList;
import java.util.List;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlayItem;

import android.content.Context;
import android.database.Cursor;

public class PoiManager {
	protected final Context mCtx;
	private GeoDatabase mGeoDatabase;

	public PoiManager(Context ctx) {
		super();
		mCtx = ctx;
		mGeoDatabase = new GeoDatabase(ctx);
	}


	public void addPoi(GeoPoint point){
		mGeoDatabase.addPoi("testpoi", "Test POI 1", point.getLatitude(), point.getLongitude(), 0, 0, 0);
	}

	public List<OpenStreetMapViewOverlayItem> getPoiList() {
		final ArrayList<OpenStreetMapViewOverlayItem> items = new ArrayList<OpenStreetMapViewOverlayItem>();
		final Cursor c = mGeoDatabase.getPoiListCursor();
		if (c != null) {
			if (c.moveToFirst()) {
				do {
					items.add(new OpenStreetMapViewOverlayItem(c.getString(2), c.getString(3), new GeoPoint(
							(int) (1E6 * c.getDouble(0)), (int) (1E6 * c.getDouble(1)))));
				} while (c.moveToNext());
			}
			c.close();
		}

		return items;
	}
}
