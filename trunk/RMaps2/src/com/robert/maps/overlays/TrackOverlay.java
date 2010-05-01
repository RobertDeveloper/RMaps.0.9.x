package com.robert.maps.overlays;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

import com.robert.maps.MainMapActivity;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.Track;
import com.robert.maps.kml.Track.TrackPoint;

public class TrackOverlay extends OpenStreetMapViewOverlay {
	private Paint mPaint;
	private float[] pts = null;
	private Track mTrack;
	private Point mBaseCoords;
	private GeoPoint mBaseLocation;

	public TrackOverlay(MainMapActivity mainMapActivity, PoiManager poiManager) {
		mTrack = poiManager.getTrack(1);
		mBaseCoords = new Point();
		mBaseLocation = new GeoPoint(0, 0);

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(4);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(0xFF90B8D8);
	}

	@Override
	protected void onDraw(Canvas c, OpenStreetMapView osmv) {
		final OpenStreetMapViewProjection pj = osmv.getProjection();
		final Point screenCoords = new Point();
		final Point screenCoords2 = new Point();
		final GeoPoint loc = new GeoPoint(0, 0);

		if (pts == null) {
			pj.toPixels(mBaseLocation, mBaseCoords);

			pts = new float[mTrack.trackpoints.size() * 4 - 2];

			int i = -4;
			for (TrackPoint trackpoint : mTrack.trackpoints) {
				loc.setCoordsE6((int) (trackpoint.lat * 1E6), (int) (trackpoint.lon * 1E6));
				pj.toPixels(loc, screenCoords);

				if (i > -4) {
					pts[i] = screenCoords2.x;
					pts[i + 1] = screenCoords2.y;
					pts[i + 2] = screenCoords.x;
					pts[i + 3] = screenCoords.y;
				}
				i = i + 4;
				screenCoords2.x = screenCoords.x;
				screenCoords2.y = screenCoords.y;
			}
		}

		pj.toPixels(mBaseLocation, screenCoords);
		if(screenCoords.x != mBaseCoords.x && screenCoords.y != mBaseCoords.y){
			c.save();
			c.translate(screenCoords.x - mBaseCoords.x, screenCoords.y - mBaseCoords.y);
			c.drawLines(pts, mPaint);
			c.restore();
		} else
			c.drawLines(pts, mPaint);

	}

	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
		// TODO Auto-generated method stub

	}

}
