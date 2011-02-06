package com.robert.maps.overlays;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;
import org.andnav.osm.views.util.OpenStreetMapTileFilesystemProvider;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

import com.robert.maps.MainMapActivity;
import com.robert.maps.R;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.Track;
import com.robert.maps.trackwriter.IRemoteService;
import com.robert.maps.trackwriter.ITrackWriterCallback;
import com.robert.maps.utils.Ut;

public class CurrentTrackOverlay extends OpenStreetMapViewOverlay {
	private Paint mPaint;
	private OpenStreetMapViewProjection mBasePj;
	private int mLastZoom;
	private Path mPath;
	private Track mTrack;
	private Point mBaseCoords;
	private GeoPoint mBaseLocation;
	//private PoiManager mPoiManager;
	private TrackThread mThread;
	private boolean mThreadRunned = false;
	protected ExecutorService mThreadExecutor = Executors.newSingleThreadExecutor();
	private OpenStreetMapView mOsmv;
//	private Handler mMainMapActivityCallbackHandler;
	private Context mContext;

    IRemoteService mService = null;
    private boolean mIsBound;

	public CurrentTrackOverlay(MainMapActivity mainMapActivity, PoiManager poiManager, OpenStreetMapView osmv) {
		mTrack = new Track();
		mContext = mainMapActivity;
		//mPoiManager = poiManager;
		mBaseCoords = new Point();
		mBaseLocation = new GeoPoint(0, 0);
		mLastZoom = -1;
		mBasePj = null;

		mOsmv = osmv;
		mThread = new TrackThread();
		mThread.setName("Current Track thread");


		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(4);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(mainMapActivity.getResources().getColor(R.color.currenttrack));

		mContext.bindService(new Intent(IRemoteService.class.getName()), mConnection, 0 /*Context.BIND_AUTO_CREATE*/);
		mIsBound = true;
	}

	private class TrackThread extends Thread {

		@Override
		public void run() {
			Ut.d("run CurrentTrackThread");

			mPath = null;
			if(mTrack == null)
				mTrack = new Track();
			else
				mTrack.getPoints().clear();

			final File folder = Ut.getRMapsMainDir(mContext, "data");
			if(folder.canRead()){
				SQLiteDatabase db = null;
				try {
					db = new com.robert.maps.trackwriter.DatabaseHelper(mContext, folder.getAbsolutePath() + "/writedtrack.db").getReadableDatabase();
				} catch (Exception e) {
					db = null;
				}

				if(db != null){
					final Cursor c = db.rawQuery("SELECT lat, lon FROM trackpoints ORDER BY id", null);

					if(c != null){
						if (c.moveToFirst()) {
							do {
								mTrack.AddTrackPoint();
								mTrack.LastTrackPoint.lat = c.getDouble(0);
								mTrack.LastTrackPoint.lon = c.getDouble(1);
							} while (c.moveToNext());
						}
						c.close();
					}
					db.close();
				};
			};

			mBasePj = mOsmv.getProjection();
			mPath = mBasePj.toPixelsTrackPoints(mTrack.getPoints(), mBaseCoords, mBaseLocation);

			Ut.d("Track maped");

			Message.obtain(mOsmv.getHandler(), OpenStreetMapTileFilesystemProvider.MAPTILEFSLOADER_SUCCESS_ID).sendToTarget();

			mThreadRunned = false;
		}
	}

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            mService = IRemoteService.Stub.asInterface(service);

            try {
                mService.registerCallback(mCallback);
            } catch (RemoteException e) {
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

	@Override
	protected void onDraw(Canvas c, OpenStreetMapView osmv) {
		if (!mThreadRunned && (mTrack == null || mLastZoom != osmv.getZoomLevel())) {
			//mPath = null;
			mLastZoom = osmv.getZoomLevel();
			//mMainMapActivityCallbackHandler = osmv.getHandler();
			Ut.d("mThreadExecutor.execute "+mThread.isAlive());
			mThreadRunned = true;
			mThreadExecutor.execute(mThread);
			return;
		}

		if(mPath == null)
			return;

		Ut.d("Draw track");
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
	}

	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
		// TODO Auto-generated method stub

	}

	public void onResume(){

	}

	public void onPause(){
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    mService.unregisterCallback(mCallback);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
	}

    private ITrackWriterCallback mCallback = new ITrackWriterCallback.Stub() {
        public void newPointWrited(double lat, double lon) {
        	Ut.dd("newPointWrited "+lat+" "+lon+" mThreadRunned="+mThreadRunned);

        	if(mThreadRunned)
        		return;

        	Ut.dd("hello");

        	if(mPath == null){
        		mPath = new Path();
        		mBaseLocation = new GeoPoint((int)(lat*1E6), (int)(lon*1E6));
        		mBasePj = mOsmv.getProjection();
        		mBaseCoords = mBasePj.toPixels2(mBaseLocation);
        		mPath.setLastPoint(mBaseCoords.x, mBaseCoords.y);
        		Ut.dd("setLastPoint "+mBaseCoords.x+" "+mBaseCoords.y);
        	} else {
           		final GeoPoint geopoint = new GeoPoint((int)(lat*1E6), (int)(lon*1E6));
           	    final Point point = mBasePj.toPixels2(geopoint);
        		mPath.lineTo(point.x, point.y);
        		Ut.dd("lineTo "+point.x+" "+point.y);
        	}

        }
    };

}
