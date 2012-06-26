package com.robert.maps.overlays;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.util.OpenStreetMapTileFilesystemProvider;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;

import com.robert.maps.MainActivity;
import com.robert.maps.R;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.Track;
import com.robert.maps.utils.Ut;
import com.robert.maps.view.TileView;
import com.robert.maps.view.TileViewOverlay;

public class TrackOverlay extends TileViewOverlay {
	private Paint mPaint;
	private int mLastZoom;
	private Path mPath;
	private Track mTrack;
	private Point mBaseCoords;
	private GeoPoint mBaseLocation;
	private PoiManager mPoiManager;
	private TrackThread mThread;
	private boolean mThreadRunned = false;
	private TileView mOsmv;
	private Handler mMainMapActivityCallbackHandler;
	private boolean mStopDraw = false;
	private com.robert.maps.view.TileView.OpenStreetMapViewProjection mProjection;

	protected ExecutorService mThreadExecutor = Executors.newSingleThreadExecutor();

	private class TrackThread extends Thread {

		@Override
		public void run() {
			Ut.d("run TrackThread");

			mPath = null;

			if(mTrack == null){
				mTrack = mPoiManager.getTrackChecked();
				if(mTrack == null){
					Ut.d("Track is null. Stoped??");
					mThreadRunned = false;
					mStopDraw = true;
					return;
				}
				Ut.d("Track loaded");
			}

			mPath = mProjection.toPixelsTrackPoints(mTrack.getPoints(), mBaseCoords, mBaseLocation);

			Ut.d("Track maped");

			Message.obtain(mMainMapActivityCallbackHandler, OpenStreetMapTileFilesystemProvider.MAPTILEFSLOADER_SUCCESS_ID).sendToTarget();

			mThreadRunned = false;
		}
	}

	public TrackOverlay(MainActivity mainActivity, PoiManager poiManager, Handler aHandler) {
		mMainMapActivityCallbackHandler = aHandler;
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
		mPaint.setColor(mainActivity.getResources().getColor(R.color.track));
	}

	@Override
	public void Free() {
		if(mPoiManager != null)
			mPoiManager.StopProcessing();
		if(mProjection != null)
			mProjection.StopProcessing();
		mThreadExecutor.shutdown();
		super.Free();
	}

	public void setStopDraw(boolean stopdraw){
		mStopDraw = stopdraw;
	}

	@Override
	protected void onDraw(Canvas c, TileView osmv) {
		if(mStopDraw) return;

		if (!mThreadRunned && (mTrack == null || mLastZoom != osmv.getZoomLevel())) {
			mPath = null;
			mLastZoom = osmv.getZoomLevel();
			//mMainMapActivityCallbackHandler = osmv.getHandler();
			mOsmv = osmv;
			//mThread.run();
			Ut.d("mThreadExecutor.execute "+mThread.isAlive());
			mProjection = mOsmv.getProjection();
			mThreadRunned = true;
			mThreadExecutor.execute(mThread);
			return;
		}

		if(mPath == null)
			return;

		final com.robert.maps.view.TileView.OpenStreetMapViewProjection pj = osmv.getProjection();
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
	}

	@Override
	protected void onDrawFinished(Canvas c, TileView osmv) {
	}

	public void clearTrack(){
		mTrack = null;
	}

}
