package com.robert.maps.applib.trackwriter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.robert.maps.R;
import com.robert.maps.applib.kml.TrackListActivity;
import com.robert.maps.applib.kml.TrackStatHelper;
import com.robert.maps.applib.utils.DistanceFormatter;
import com.robert.maps.applib.utils.Ut;

import org.andnav.osm.util.constants.OpenStreetMapConstants;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TrackWriterService extends Service implements OpenStreetMapConstants {
	private SQLiteDatabase db;
    NotificationManager mNM;
    Notification mNotification;
    PendingIntent mContentIntent;
    final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private TrackStatHelper mTrackStat = new TrackStatHelper();
    private DistanceFormatter mDf;
//    private String mLogFileName;

	protected LocationManager mLocationManager;
	protected SampleLocationListener mLocationListener;

    final RemoteCallbackList<ITrackWriterCallback> mCallbacks = new RemoteCallbackList<ITrackWriterCallback>();

    private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {
        public void registerCallback(ITrackWriterCallback cb) {
            if (cb != null) { 
            	mCallbacks.register(cb);
            	if(mTrackStat != null) {
            		try {
						cb.onTrackStatUpdate(mTrackStat.Cnt, mTrackStat.Distance, mTrackStat.Duration, mTrackStat.MaxSpeed, mTrackStat.AvgSpeed, mTrackStat.MoveTime, mTrackStat.AvgMoveSpeed);
					} catch (RemoteException e) {
					}
            	}
            }
        }
        public void unregisterCallback(ITrackWriterCallback cb) {
            if (cb != null) mCallbacks.unregister(cb);
        }
    };

    private final Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1: {
                    // Broadcast to all clients the new value.
                    final int N = mCallbacks.beginBroadcast();
                    for (int i=0; i<N; i++) {
                        try {
                        	final Location loc = (Location)msg.obj;
                            mCallbacks.getBroadcastItem(i).newPointWrited(loc.getLatitude(), loc.getLongitude());
                            mCallbacks.getBroadcastItem(i).onTrackStatUpdate(mTrackStat.Cnt, mTrackStat.Distance, mTrackStat.Duration, mTrackStat.MaxSpeed, mTrackStat.AvgSpeed, mTrackStat.MoveTime, mTrackStat.AvgMoveSpeed);
                        } catch (RemoteException e) {
                        }
                    }
                    mCallbacks.finishBroadcast();

               } break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

//    public class CrashReportHandler implements UncaughtExceptionHandler {
//
//		@Override
//		public void uncaughtException(Thread thread, Throwable ex) {
//			StringWriter stackTrace=new StringWriter();
//			ex.printStackTrace(new PrintWriter(stackTrace));
//
//			appendLog(stackTrace.toString());
//			Process.killProcess(Process.myPid());
//			System.exit(10);
//		}
//    	
//    }
//    
    
    @Override
	public void onCreate() {
		super.onCreate();
		
//		mLogFileName = Ut.getRMapsMainDir(this, "").getAbsolutePath()+"/trackwriter.log";
//		
//		Thread.setDefaultUncaughtExceptionHandler(new CrashReportHandler());
		
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		sdf.applyPattern("HH:mm:ss");
		
//		appendLog("onCreate");
		
		mDf = new DistanceFormatter(this);

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

	    try {
	        mStartForeground = getClass().getMethod("startForeground",
	                mStartForegroundSignature);
	        mStopForeground = getClass().getMethod("stopForeground",
	                mStopForegroundSignature);
	        return;
	    } catch (NoSuchMethodException e) {
	        // Running on an older platform.
	        mStartForeground = mStopForeground = null;
	    }
	    try {
	        mSetForeground = getClass().getMethod("setForeground",
	                mSetForegroundSignature);
	    } catch (NoSuchMethodException e) {
	        throw new IllegalStateException(
	                "OS doesn't have Service.startForeground OR Service.setForeground!");
	    }
		
	}

    //final RemoteCallbackList<IRemoteServiceCallback> mCallbacks = new RemoteCallbackList<IRemoteServiceCallback>();

	@Override
	public void onStart(Intent intent, int startId) {
		if(intent != null)
			handleCommand(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null)
			handleCommand(intent);
		return START_STICKY;
	}

	private void handleCommand(Intent intent) {
//		appendLog("handleCommand");
		final File folder = Ut.getRMapsMainDir(this, "data");
		if(folder.canRead()){
			try {
				db = new DatabaseHelper(this, folder.getAbsolutePath() + "/writedtrack.db").getWritableDatabase();
			} catch (Exception e) {
				db = null;
			}
		};

		if(db == null){
			Toast.makeText(this,
					getString(R.string.message_cantstarttrackwriter)+" "+folder.getAbsolutePath(),
					Toast.LENGTH_LONG).show();
			this.stopSelf();
			return;
		};

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final int minTime = Integer.parseInt(pref.getString("pref_trackwriter_mintime", "2000"));
		final int minDistance = Integer.parseInt(pref.getString("pref_trackwriter_mindistance", "10"));

		mLocationListener = new SampleLocationListener();
//		appendLog("requestLocationUpdates minTime="+minTime+" minDistance="+minDistance);
		getLocationManager().requestLocationUpdates(GPS, minTime, minDistance, this.mLocationListener);
		
		showNotification();
	}
	
	private void showNotification() {
		CharSequence text = getText(R.string.remote_service_started);

		mNotification = new Notification(R.drawable.track_writer_service, text, System.currentTimeMillis());
		mNotification.flags = mNotification.flags | Notification.FLAG_NO_CLEAR;

		mContentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, TrackListActivity.class), 0);

		mNotification.setLatestEventInfo(this, getText(R.string.remote_service_started), text, mContentIntent);

		startForegroundCompat(R.string.remote_service_started, mNotification);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
//		appendLog("onDestroy");
		stopForegroundCompat(R.string.remote_service_started);

		if(mLocationListener != null)
			getLocationManager().removeUpdates(mLocationListener);

		if(db != null)
			db.close();

        if(mCallbacks != null)
        	mCallbacks.kill();
        
	}

	private LocationManager getLocationManager() {
		if(this.mLocationManager == null)
			this.mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		return this.mLocationManager;
	}

	private class SampleLocationListener implements LocationListener {
		public void onLocationChanged(final Location loc) {
			if (loc != null){
//				appendLog("onLocationChanged "+loc.toString());
				
				addPoint(loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), loc.getSpeed(), loc.getTime());
				
				mTrackStat.addPoint(loc);

				mHandler.sendMessage(mHandler.obtainMessage(1, loc));
				
				final String text = ""
						+sdf.format(new Date(mTrackStat.Duration))
						+" | " + mDf.formatDistance(mTrackStat.Distance)
						+" | " + mDf.formatSpeed(mTrackStat.AvgSpeed)
						;
				mNotification.setLatestEventInfo(TrackWriterService.this, getText(R.string.remote_service_started), text, mContentIntent);
				mNM.notify(R.string.remote_service_started, mNotification);
			}
		}

		public void onStatusChanged(String a, int status, Bundle b) {
//			appendLog("onStatusChanged provider="+a+" status="
//					+ (status == LocationProvider.OUT_OF_SERVICE ? "OUT_OF_SERVICE" : status == LocationProvider.TEMPORARILY_UNAVAILABLE ? "TEMPORARILY_UNAVAILABLE" : status == LocationProvider.AVAILABLE ? "AVAILABLE" : "UNKNOWN")
//					+" satellites="+b.getInt("satellites", -1)
//					);
		}

		public void onProviderEnabled(String a) {
//			appendLog("onProviderEnabled provider="+a);
		}

		public void onProviderDisabled(String a) {
//			appendLog("onProviderDisabled provider="+a);
		}
	}

	public void addPoint(double latitude, double longitude, double altitude, float speed, long currentTimeMillis) {
		final ContentValues cv = new ContentValues();
		cv.put("trackid", 0);
		cv.put("lat", latitude);
		cv.put("lon", longitude);
		cv.put("alt", altitude);
		cv.put("speed", speed);
		cv.put("date", currentTimeMillis / 1000);
		this.db.insert("trackpoints", null, cv);
	}

	private static final Class<?>[] mSetForegroundSignature = new Class[] {
	    boolean.class};
	private static final Class<?>[] mStartForegroundSignature = new Class[] {
	    int.class, Notification.class};
	private static final Class<?>[] mStopForegroundSignature = new Class[] {
	    boolean.class};

	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mSetForegroundArgs = new Object[1];
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	void invokeMethod(Method method, Object[] args) {
	    try {
	        method.invoke(this, args);
	    } catch (InvocationTargetException e) {
	    } catch (IllegalAccessException e) {
	    }
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	void startForegroundCompat(int id, Notification notification) {
	    // If we have the new startForeground API, then use it.
	    if (mStartForeground != null) {
	        mStartForegroundArgs[0] = Integer.valueOf(id);
	        mStartForegroundArgs[1] = notification;
	        invokeMethod(mStartForeground, mStartForegroundArgs);
	        return;
	    }

	    // Fall back on the old API.
	    mSetForegroundArgs[0] = Boolean.TRUE;
	    invokeMethod(mSetForeground, mSetForegroundArgs);
	    mNM.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	void stopForegroundCompat(int id) {
	    // If we have the new stopForeground API, then use it.
	    if (mStopForeground != null) {
	        mStopForegroundArgs[0] = Boolean.TRUE;
	        invokeMethod(mStopForeground, mStopForegroundArgs);
	        return;
	    }

	    // Fall back on the old API.  Note to cancel BEFORE changing the
	    // foreground state, since we could be killed at that point.
	    mNM.cancel(id);
	    mSetForegroundArgs[0] = Boolean.FALSE;
	    invokeMethod(mSetForeground, mSetForegroundArgs);
	}
}
