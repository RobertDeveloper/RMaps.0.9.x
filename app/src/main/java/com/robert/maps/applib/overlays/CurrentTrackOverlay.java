package com.robert.maps.applib.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import com.robert.maps.applib.MainActivity;
import com.robert.maps.applib.kml.PoiManager;
import com.robert.maps.applib.kml.Track;
import com.robert.maps.applib.trackwriter.IRemoteService;
import com.robert.maps.applib.trackwriter.ITrackWriterCallback;
import com.robert.maps.applib.utils.SimpleThreadFactory;
import com.robert.maps.applib.utils.Ut;
import com.robert.maps.applib.view.TileView;
import com.robert.maps.applib.view.TileViewOverlay;

import org.andnav.osm.util.GeoPoint;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurrentTrackOverlay extends TileViewOverlay {
	private Paint mPaint;
	private int mLastZoom;
	private Path mPath;
	private Track mTrack;
	private Point mBaseCoords;
	private GeoPoint mBaseLocation;
	private TrackThread mThread;
	private com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection mBasePj;
	private boolean mThreadRunned = false;
	protected ExecutorService mThreadExecutor = Executors.newSingleThreadExecutor(new SimpleThreadFactory("CurrentTrack"));
	private TileView mOsmv;
	private Context mContext;

    IRemoteService mService = null;
    private boolean mIsBound;
    private ServiceConnection mConnection;

	public CurrentTrackOverlay(MainActivity mainMapActivity, PoiManager poiManager) {
		mConnection = new ServiceConnection() {
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
		
		final String defStyle = PreferenceManager.getDefaultSharedPreferences(mainMapActivity).getString("pref_track_style_current", "");
		mTrack = new Track(defStyle);
		mContext = mainMapActivity;
		mBaseCoords = new Point();
		mBaseLocation = new GeoPoint(0, 0);
		mLastZoom = -1;
		mBasePj = null;

		mOsmv = null;
		mThread = new TrackThread();
		mThread.setName("Current Track thread");

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setColor(mTrack.Color);
		mPaint.setStrokeWidth(mTrack.Width);
		mPaint.setAlpha(Color.alpha(mTrack.ColorShadow));
		mPaint.setShadowLayer((float) mTrack.ShadowRadius, 0, 0, mTrack.ColorShadow);

		mIsBound = false;
	}

	@Override
	public void Free() {
		if(mBasePj != null)
			mBasePj.StopProcessing();
		mThreadExecutor.shutdown();
		super.Free();
	}

	private class TrackThread extends Thread {

		@Override
		public void run() {
			mPath = null;
			if(mTrack == null)
				mTrack = new Track();
			else
				mTrack.getPoints().clear();

			final File folder = Ut.getRMapsMainDir(mContext, "data");
			if(folder.canRead()){
				SQLiteDatabase db = null;
				try {
					db = new com.robert.maps.applib.trackwriter.DatabaseHelper(mContext, folder.getAbsolutePath() + "/writedtrack.db").getReadableDatabase();
				} catch (Exception e) {
					db = null;
				}

				if(db != null){
					final Cursor c = db.rawQuery("SELECT lat, lon FROM trackpoints ORDER BY id", null);
					
					mPath = mBasePj.toPixelsTrackPoints(c, mBaseCoords, mBaseLocation);
					if(mPath != null && mPath.isEmpty())
						mPath = null;
					
					db.close();
				};
			};

			try {
				Message.obtain(mOsmv.getHandler(), Ut.MAPTILEFSLOADER_SUCCESS_ID).sendToTarget();
			} catch (Exception e) {
			}

			mThreadRunned = false;
		}
	}


	@Override
	protected void onDraw(Canvas c, TileView osmv) {
		if (!mThreadRunned && (mTrack == null || mLastZoom != osmv.getZoomLevel())) {
			mOsmv = osmv;
			mLastZoom = osmv.getZoomLevel();
			mBasePj = mOsmv.getProjection();
			mThreadRunned = true;
			mThreadExecutor.execute(mThread);
			return;
		}

		if(mPath == null)
			return;

		final com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection pj = osmv.getProjection();
		final Point screenCoords = new Point();

		pj.toPixels(mBaseLocation, screenCoords);

		c.save();
		if(screenCoords.x != mBaseCoords.x && screenCoords.y != mBaseCoords.y){
			c.translate(screenCoords.x - mBaseCoords.x, screenCoords.y - mBaseCoords.y);
			c.scale((float)osmv.mTouchScale, (float)osmv.mTouchScale, mBaseCoords.x, mBaseCoords.y);
		};
		if(mPath != null)
			c.drawPath(mPath, mPaint);
		c.restore();
	}

	@Override
	protected void onDrawFinished(Canvas c, TileView osmv) {

	}

	public void onResume(){
		mTrack = null;
		mContext.bindService(new Intent(IRemoteService.class.getName()), mConnection, 0 /*Context.BIND_AUTO_CREATE*/);
		mIsBound = true;
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

        	if(mThreadRunned)
        		return;

        	if(mPath == null){
        		mPath = new Path();
        		mBaseLocation = new GeoPoint((int)(lat*1E6), (int)(lon*1E6));
        		mBasePj = mOsmv.getProjection();
        		mBaseCoords = mBasePj.toPixels2(mBaseLocation);
        		mPath.setLastPoint(mBaseCoords.x, mBaseCoords.y);
        	} else {
           		final GeoPoint geopoint = new GeoPoint((int)(lat*1E6), (int)(lon*1E6));
           	    final Point point = mBasePj.toPixels2(geopoint);
        		mPath.lineTo(point.x, point.y);
        	}

        }

		@Override
		public void onTrackStatUpdate(int Cnt, double Distance, long Duration, double MaxSpeed, double AvgSpeed, long MoveTime, double AvgMoveSpeed)
				throws RemoteException {
			
		}

    };

}
