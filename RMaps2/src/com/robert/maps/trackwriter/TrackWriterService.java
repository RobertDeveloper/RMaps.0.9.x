package com.robert.maps.trackwriter;

import java.io.File;

import org.andnav.osm.util.constants.OpenStreetMapConstants;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import com.robert.maps.R;
import com.robert.maps.kml.TrackListActivity;
import com.robert.maps.utils.Ut;

public class TrackWriterService extends Service implements OpenStreetMapConstants {

	protected LocationManager mLocationManager;
	protected SampleLocationListener mLocationListener;

	@Override
	public void onCreate() {
		super.onCreate();

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		File folder = Ut.getRMapsFolder("data", false);
		db = new DatabaseHelper(this, folder.getAbsolutePath() + "/writedtrack.db").getWritableDatabase();

		mLocationListener = new SampleLocationListener();
		final int minTime = 1000;
		final int minDistance = 5;
		getLocationManager().requestLocationUpdates(GPS, minTime, minDistance, this.mLocationListener);


		showNotification();
}

	private SQLiteDatabase db;
    NotificationManager mNM;
    //final RemoteCallbackList<IRemoteServiceCallback> mCallbacks = new RemoteCallbackList<IRemoteServiceCallback>();

	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the expanded notification
		CharSequence text = getText(R.string.remote_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.track_writer_service, text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, TrackListActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.remote_service_started), text, contentIntent);

		// Send the notification.
		// We use a string id because it is a unique number. We use it later to cancel.
		mNM.notify(R.string.remote_service_started, notification);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
	       // Cancel the persistent notification.
        mNM.cancel(R.string.remote_service_started);

        getLocationManager().removeUpdates(mLocationListener);

        // Tell the user we stopped.
        //Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();

        // Unregister all callbacks.
        //mCallbacks.kill();

        // Remove the next pending message to increment the counter, stopping
        // the increment loop.
        //mHandler.removeMessages(REPORT_MSG);
	}

    private final IBinder mBinder = new Binder() {
        @Override
		protected boolean onTransact(int code, Parcel data, Parcel reply,
		        int flags) throws RemoteException {
            return super.onTransact(code, data, reply, flags);
        }
    };




	private LocationManager getLocationManager() {
		if(this.mLocationManager == null)
			this.mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		return this.mLocationManager;
	}

	private class SampleLocationListener implements LocationListener {

		public void onLocationChanged(final Location loc) {
			if (loc != null){
				addPoint(loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), loc.getSpeed(), System.currentTimeMillis());
			}
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
		cv.put("date", currentTimeMillis);
		this.db.insert("trackpoints", null, cv);
	}

}
