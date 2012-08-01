package com.robert.maps.downloader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.robert.maps.R;

public class MapDownloaderService extends Service {
	
	@Override
	public void onCreate() {
		super.onCreate();

		showNotification();
	}

	@Override
	public void onDestroy() {
		((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(R.string.remote_service_started);
		
		super.onDestroy();
	}

	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the expanded notification
		CharSequence text = getText(R.string.remote_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.track_writer_service, text, System.currentTimeMillis());
		notification.flags = notification.flags | Notification.FLAG_NO_CLEAR;

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, AreaSelectorActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.remote_service_started), text, contentIntent);

		// Send the notification.
		// We use a string id because it is a unique number. We use it later to cancel.
		((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(R.string.remote_service_started, notification);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
}
