package com.robert.maps.overlays;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;
import org.andnav.osm.views.util.OpenStreetMapTileFilesystemProvider;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;

import com.robert.maps.MainMapActivity;
import com.robert.maps.R;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.Track;
import com.robert.maps.kml.Track.TrackPoint;
import com.robert.maps.utils.Ut;

public class TrackOverlay extends OpenStreetMapViewOverlay {
	private Paint mPaint;
	private int mLastZoom;
	private Path mPath;
	private Track mTrack;
	private Point mBaseCoords;
	private GeoPoint mBaseLocation;
	private PoiManager mPoiManager;
	private TrackThread mThread;
	private boolean mThreadRunned = false;
	private OpenStreetMapView mOsmv;
	private Handler mMainMapActivityCallbackHandler;

	protected ExecutorService mThreadExecutor = Executors.newSingleThreadExecutor();

	private class TrackThread extends Thread {

		@Override
		public void run() {
			Ut.dd("run TrackThread");

			mPath = null;
			final Path path = new Path();

			if(mTrack == null){
				mTrack = mPoiManager.getTrack(1);
				Ut.dd("Track loaded");
			}

			final OpenStreetMapViewProjection pj = mOsmv.getProjection();
//			pj.toPixelsTrackPoints(mTrack.trackpoints, path);
			mPath =path;

			final Point screenCoords = new Point();
			final Point screenCoords2 = new Point();
			final GeoPoint loc = new GeoPoint(0, 0);

			pj.toPixels(mBaseLocation, mBaseCoords);
			int i = 0;
			boolean isFirstPoint = true;
			for (TrackPoint trackpoint : mTrack.trackpoints) {
				loc.setCoordsE6((int) (trackpoint.lat * 1E6), (int) (trackpoint.lon * 1E6));
				pj.toPixels(loc, screenCoords);

				if(!isFirstPoint){
					if (Math.abs(screenCoords2.x - screenCoords.x) > 5
							|| Math.abs(screenCoords2.y - screenCoords.y) > 5) {
						path.lineTo(screenCoords.x, screenCoords.y);
						screenCoords2.x = screenCoords.x;
						screenCoords2.y = screenCoords.y;
						i++;
						if(i<10) Ut.dd("test coord "+screenCoords.x+" "+screenCoords.y);
					}
				} else {
					path.setLastPoint(screenCoords.x, screenCoords.y);
					screenCoords2.x = screenCoords.x;
					screenCoords2.y = screenCoords.y;
					isFirstPoint = false;
					i++;
				}
			}

			mPath = path;

			Ut.dd("Track maped");

			Message.obtain(mMainMapActivityCallbackHandler, OpenStreetMapTileFilesystemProvider.MAPTILEFSLOADER_SUCCESS_ID).sendToTarget();

			mThreadRunned = false;
			//super.run();
		}
	}

	public TrackOverlay(MainMapActivity mainMapActivity, PoiManager poiManager) {
		mTrack = null;
		mPoiManager = poiManager;
		mBaseCoords = new Point();
		mBaseLocation = new GeoPoint(0, 0);
		mLastZoom = -1;
		mThread = new TrackThread();
		mThread.setName("Track thread");


		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(4);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(mainMapActivity.getResources().getColor(R.color.track));
	}

	@Override
	protected void onDraw(Canvas c, OpenStreetMapView osmv) {
		if (!mThreadRunned && (mTrack == null || mLastZoom != osmv.getZoomLevel())) {
			mPath = null;
			mLastZoom = osmv.getZoomLevel();
			mMainMapActivityCallbackHandler = osmv.getHandler();
			mOsmv = osmv;
			//mThread.run();
			Ut.dd("mThreadExecutor.execute "+mThread.isAlive());
			mThreadRunned = true;
			mThreadExecutor.execute(mThread);
			return;
		}

		if(mPath == null)
			return;

		Ut.dd("Draw track");
		final OpenStreetMapViewProjection pj = osmv.getProjection();
		final Point screenCoords = new Point();

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
