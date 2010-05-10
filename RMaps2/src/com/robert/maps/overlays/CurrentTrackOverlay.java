package com.robert.maps.overlays;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
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

	public CurrentTrackOverlay(MainMapActivity mainMapActivity, PoiManager poiManager) {
		mTrack = null;
		mContext = mainMapActivity;
		mPoiManager = poiManager;
		mBaseCoords = new Point();
		mBaseLocation = new GeoPoint(0, 0);
		mLastZoom = -1;
//		mThread = new TrackThread();
//		mThread.setName("Track thread");


		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(4);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(mainMapActivity.getResources().getColor(R.color.currenttrack));

		final boolean res = mContext.bindService(new Intent(IRemoteService.class.getName()), mConnection, 0 /*Context.BIND_AUTO_CREATE*/);
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
		// TODO Auto-generated method stub

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
            //mHandler.sendMessage(mHandler.obtainMessage(BUMP_MSG, value, 0));
        	Ut.dd("newPointWrited "+lat+" "+lon);
        }
    };

}
