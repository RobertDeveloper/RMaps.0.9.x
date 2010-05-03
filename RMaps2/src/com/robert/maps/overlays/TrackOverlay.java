package com.robert.maps.overlays;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.robert.maps.MainMapActivity;
import com.robert.maps.R;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.Track;
import com.robert.maps.kml.Track.TrackPoint;

public class TrackOverlay extends OpenStreetMapViewOverlay {
	private Paint mPaint;
	private int mLastZoom;
	private Path mPath;
	private Track mTrack;
	private Point mBaseCoords;
	private GeoPoint mBaseLocation;

	public TrackOverlay(MainMapActivity mainMapActivity, PoiManager poiManager) {
		mTrack = poiManager.getTrack(1);
		mBaseCoords = new Point();
		mBaseLocation = new GeoPoint(0, 0);
		mLastZoom = -1;

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(4);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(mainMapActivity.getResources().getColor(R.color.track));
	}

	@Override
	protected void onDraw(Canvas c, OpenStreetMapView osmv) {
		if(mTrack == null)
			return;

		final OpenStreetMapViewProjection pj = osmv.getProjection();
		final Point screenCoords = new Point();
		final Point screenCoords2 = new Point();
		final GeoPoint loc = new GeoPoint(0, 0);

		if (mLastZoom != osmv.getZoomLevel()) {
			mLastZoom = osmv.getZoomLevel();
			pj.toPixels(mBaseLocation, mBaseCoords);

			mPath = new Path();

			boolean isFirstPoint = true;
			for (TrackPoint trackpoint : mTrack.trackpoints) {
				loc.setCoordsE6((int) (trackpoint.lat * 1E6), (int) (trackpoint.lon * 1E6));
				pj.toPixels(loc, screenCoords);

				if(!isFirstPoint){
					if (Math.abs(screenCoords2.x - screenCoords.x) > 5
							|| Math.abs(screenCoords2.y - screenCoords.y) > 5) {
						mPath.lineTo(screenCoords.x, screenCoords.y);
						screenCoords2.x = screenCoords.x;
						screenCoords2.y = screenCoords.y;
					}
				} else {
					mPath.setLastPoint(screenCoords.x, screenCoords.y);
					screenCoords2.x = screenCoords.x;
					screenCoords2.y = screenCoords.y;
					isFirstPoint = false;
				}
			}
		}
		pj.toPixels(mBaseLocation, screenCoords);

		//final long startMs = System.currentTimeMillis();

		if(screenCoords.x != mBaseCoords.x && screenCoords.y != mBaseCoords.y){
			c.save();
			c.translate(screenCoords.x - mBaseCoords.x, screenCoords.y - mBaseCoords.y);
			c.drawPath(mPath, mPaint);
			c.restore();
		} else
			c.drawPath(mPath, mPaint);

		if(osmv.getBearing() != 0.0){
			final int viewWidth = osmv.getWidth();
			final int viewHeight = osmv.getHeight();
			final float aRotateToAngle = 360 - osmv.getBearing();
			c.save();
			c.rotate(aRotateToAngle, viewWidth/2, viewHeight/2);
			c.restore();
		}

//		final long endMs = System.currentTimeMillis();
//		Ut.dd("Rendering overall: " + (endMs - startMs) + "ms");

	}

	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
		// TODO Auto-generated method stub

	}

}
