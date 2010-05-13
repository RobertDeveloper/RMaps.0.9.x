package com.robert.maps.overlays;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

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
	private PoiManager mPoiManager;
//	private TrackThread mThread;
//	private boolean mThreadRunned = false;
	private OpenStreetMapView mOsmv;
//	private Handler mMainMapActivityCallbackHandler;
	private boolean mStopDraw = false;
	private Context mContext;

    IRemoteService mService = null;
    private boolean mIsBound;

	public CurrentTrackOverlay(MainMapActivity mainMapActivity, PoiManager poiManager, OpenStreetMapView osmv) {
		mTrack = new Track();
		mContext = mainMapActivity;
		mPoiManager = poiManager;
		mBaseCoords = new Point();
		mBaseLocation = new GeoPoint(0, 0);
		mLastZoom = -1;

		mOsmv = osmv;
//		mThread = new TrackThread();
//		mThread.setName("Track thread");


		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(4);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(mainMapActivity.getResources().getColor(R.color.currenttrack));

		mContext.bindService(new Intent(IRemoteService.class.getName()), mConnection, 0 /*Context.BIND_AUTO_CREATE*/);
		mIsBound = true;
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
//		if (!mThreadRunned && (mTrack == null || mLastZoom != osmv.getZoomLevel())) {
//			mPath = null;
//			mLastZoom = osmv.getZoomLevel();
//			mMainMapActivityCallbackHandler = osmv.getHandler();
//			mOsmv = osmv;
//			//mThread.run();
//			Ut.d("mThreadExecutor.execute "+mThread.isAlive());
//			mThreadRunned = true;
//			mThreadExecutor.execute(mThread);
//			return;
//		}

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
        	Ut.dd("newPointWrited "+lat+" "+lon);

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
