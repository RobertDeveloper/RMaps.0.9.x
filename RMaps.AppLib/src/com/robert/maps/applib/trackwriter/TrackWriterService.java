package com.robert.maps.applib.trackwriter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.andnav.osm.util.constants.OpenStreetMapConstants;

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

import com.robert.maps.applib.R;
import com.robert.maps.applib.kml.Track;
import com.robert.maps.applib.kml.TrackListActivity;
import com.robert.maps.applib.utils.Ut;

public class TrackWriterService extends Service implements OpenStreetMapConstants {
	private SQLiteDatabase db;
    NotificationManager mNM;
    Notification mNotification;
    PendingIntent mContentIntent;
    final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

	protected LocationManager mLocationManager;
	protected SampleLocationListener mLocationListener;

    final RemoteCallbackList<ITrackWriterCallback> mCallbacks = new RemoteCallbackList<ITrackWriterCallback>();

    private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {
        public void registerCallback(ITrackWriterCallback cb) {
            if (cb != null) mCallbacks.register(cb);
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
                        	Ut.dd("mCallbacks.getBroadcastItem(i).newPointWrited");
                            mCallbacks.getBroadcastItem(i).newPointWrited(loc.getLatitude(), loc.getLongitude());
                        } catch (RemoteException e) {
                            // The RemoteCallbackList will take care of removing
                            // the dead object for us.
                        	Ut.dd("RemoteException: The RemoteCallbackList will take care of removing");
                        }
                    }
                    mCallbacks.finishBroadcast();

                    //sendMessageDelayed(obtainMessage(1), 1*1000);
               } break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

	@Override
	public void onCreate() {
		super.onCreate();
		
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		sdf.applyPattern("HH:mm:ss");
		

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

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		mLocationListener = new SampleLocationListener();
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		mLocationListener.Init(Integer.parseInt(pref.getString("pref_trackwriter_mintime", "2000")), Integer.parseInt(pref.getString("pref_trackwriter_mindistance", "10")));
		final int minTime = 1000;
		final int minDistance = 1;
		getLocationManager().requestLocationUpdates(GPS, minTime, minDistance, this.mLocationListener);


		showNotification();
        //mHandler.sendEmptyMessage(1) ;
	}

    //final RemoteCallbackList<IRemoteServiceCallback> mCallbacks = new RemoteCallbackList<IRemoteServiceCallback>();

	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the expanded notification
		CharSequence text = getText(R.string.remote_service_started);

		// Set the icon, scrolling text and timestamp
		mNotification = new Notification(R.drawable.track_writer_service, text, System.currentTimeMillis());
		mNotification.flags = mNotification.flags | Notification.FLAG_NO_CLEAR;

		// The PendingIntent to launch our activity if the user selects this notification
		mContentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, TrackListActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		mNotification.setLatestEventInfo(this, getText(R.string.remote_service_started), text, mContentIntent);

		// Send the notification.
		// We use a string id because it is a unique number. We use it later to cancel.
		mNM.notify(R.string.remote_service_started, mNotification);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
	       // Cancel the persistent notification.
		if(mNM != null)
			mNM.cancel(R.string.remote_service_started);

		if(mLocationListener != null)
			getLocationManager().removeUpdates(mLocationListener);

		if(db != null)
			db.close();

        // Tell the user we stopped.
        //Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();

        // Unregister all callbacks.
        if(mCallbacks != null)
        	mCallbacks.kill();

        // Remove the next pending message to increment the counter, stopping
        // the increment loop.
        //mHandler.removeMessages(REPORT_MSG);
	}

	private LocationManager getLocationManager() {
		if(this.mLocationManager == null)
			this.mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		return this.mLocationManager;
	}

	private class SampleLocationListener implements LocationListener {
		private Location mLastWritedLocation = null;
		private Location mLastLocation = null;
		private long mMinTime = 2000;
		private long mMaxTime = 2000;
		private int mMinDistance = 10;
		private double mDistanceFromLastWriting = 0;
		private long mTimeFromLastWriting = 0;

		//public Track.TrackPoint lastpt = null;
		float[] results = {0};
		public Date lastDate = new Date();
		public double Distance = 0;
		public double Duration = 0;
		public Date Date1 = new Date();
		public Date Date2 = new Date();
		public double MaxSpeed = 0;
		public double AvgSpeed = 0;
		public double AvgPace = 0;
		public double MinEle = 99999;
		public double MaxEle = -99999;
		public int MoveTime = 0;
		public double AvgMoveSpeed = 0;
		
		public void onLocationChanged(final Location loc) {
			if (loc != null){
				boolean needWrite = false;
				if(mLastLocation != null)
					mDistanceFromLastWriting =+ loc.distanceTo(mLastLocation);
				if(mLastWritedLocation != null)
					mTimeFromLastWriting = loc.getTime() - mLastWritedLocation.getTime();

				if(mLastWritedLocation == null || mLastLocation == null)
					needWrite = true;
				else if (mTimeFromLastWriting > mMaxTime)
					needWrite = true;
				else if(mDistanceFromLastWriting > mMinDistance && mTimeFromLastWriting > mMinTime)
					needWrite = true;

				if(needWrite){
					final long time = System.currentTimeMillis();

					if(mLastWritedLocation != null) {
						results[0] = 0;
						Location.distanceBetween(mLastWritedLocation.getLatitude(), mLastWritedLocation.getLongitude(), loc.getLatitude(), loc.getLongitude(), results);
						Distance += results[0];
					} else {
						Date1.setTime(time);
						lastDate.setTime(time);
					}

					mLastWritedLocation = loc;
					mLastLocation = loc;
					mDistanceFromLastWriting = 0;
					addPoint(loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), loc.getSpeed(), time);

					mHandler.sendMessage(mHandler.obtainMessage(1, loc));
					
					// Statistic
					Duration = (time - Date1.getTime()) / 1000;
					if(Duration > 0)
						AvgSpeed = (Distance / 1000) / (Duration/60/60);
					if(Distance > 0)
						AvgPace = Duration / (Distance / 1000);
					
					if(loc.getSpeed() > MaxSpeed)
						MaxSpeed = loc.getSpeed();
					if(loc.getAltitude() > MaxEle)
						MaxEle = loc.getAltitude();
					if(loc.getAltitude() < MinEle)
						MinEle = loc.getAltitude();
					if(loc.getSpeed() > 0.5)
						MoveTime += time - lastDate.getTime();
					Date2.setTime(time);
					if(MoveTime > 0)
						AvgMoveSpeed = (Distance / 1000) / (MoveTime/1000/60.0/60.0);
					
					lastDate.setTime(time);
					
					final String text = ""
							+sdf.format(new Date((long) (Duration*1000)))
							+" | "+Ut.formatDistance(TrackWriterService.this, (float) Distance, 0)
							+" | "+String.format("%.1f", AvgSpeed)+" km/h"
							;
					mNotification.setLatestEventInfo(TrackWriterService.this, getText(R.string.remote_service_started), text, mContentIntent);
					mNM.notify(R.string.remote_service_started, mNotification);

					
				} else {
					//Ut.dd("NOT addPoint mDistanceFromLastWriting="+mDistanceFromLastWriting+" mTimeFromLastWriting="+(mTimeFromLastWriting/1000));
					mLastLocation = loc;
				}
			}
		}

		public void Init(int mintime, int mindistance) {
			mMinTime = mintime;
			mMinDistance = mindistance;
			Ut.d("mintime="+mintime+" mindistance="+mindistance);
		}

		public void onStatusChanged(String a, int status, Bundle b) {
		}

		public void onProviderEnabled(String a) {
		}
		public void onProviderDisabled(String a) {
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

}
