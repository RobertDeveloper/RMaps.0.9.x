package com.robert.maps;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.TypeConverter;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;

public class SearchResultOverlay extends OpenStreetMapViewOverlay {

	protected final Paint mPaint = new Paint();
	protected final Bitmap mBubbleBitmap;
	protected GeoPoint mLocation;

	public SearchResultOverlay(final Context ctx) {
		this.mBubbleBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.bubble);
	}

	public void setLocation(final Location loc){
		this.mLocation = TypeConverter.locationToGeoPoint(loc);
	}

	public void setLocation(final GeoPoint geopoint){
		this.mLocation = geopoint;
	}

	@Override
	protected void onDraw(Canvas c, OpenStreetMapView osmv) {
		if(this.mLocation != null){
			final OpenStreetMapViewProjection pj = osmv.getProjection();
			final Point screenCoords = new Point();
			pj.toPixels(this.mLocation, screenCoords);

			c.drawBitmap(mBubbleBitmap, screenCoords.x - 2, screenCoords.y - 30, this.mPaint);
		}
	}

	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
		// Auto-generated method stub

	}

}
