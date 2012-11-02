package com.robert.maps.downloader;

import org.andnav.osm.util.GeoPoint;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.robert.maps.R;
import com.robert.maps.tileprovider.TileSource;
import com.robert.maps.utils.Ut;
import com.robert.maps.view.MapView;

public class DownloaderActivity extends Activity {
	private static final String CNT = "cnt";
	private static final String TIME = "time";

	private MapView mMap;
	private TileSource mTileSource;
	private ServiceConnection mConnection;
	IRemoteService mService = null;
	private ProgressBar mProgress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.downloaderactivity);

		// final SharedPreferences pref =
		// PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);

		mMap = (MapView) findViewById(R.id.map);
		// mMap.setMoveListener(mMoveListener);
		// mMap.displayZoomControls(Integer.parseInt(pref.getString("pref_zoomctrl",
		// "1")));
		mMap.getController().setCenter(new GeoPoint(uiState.getInt("Latitude", 0), uiState.getInt("Longitude", 0)));
		mMap.setLongClickable(false);

		mProgress = (ProgressBar) findViewById(R.id.progress);

		mConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className, IBinder service) {
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
	}

	private IDownloaderCallback mCallback = new IDownloaderCallback.Stub() {

		public void downloadDone() throws RemoteException {
			if (mHandler != null) {
				mHandler.sendMessage(mHandler.obtainMessage(R.id.done));
			}
		}

		public void downloadStart(int tileCnt, long startTime) throws RemoteException {
			Bundle b = new Bundle();
			b.putInt(CNT, tileCnt);
			b.putLong(TIME, startTime);
			mHandler.sendMessage(mHandler.obtainMessage(R.id.download_start, b));
		}

		public void downloadTileDone(int tileCnt) throws RemoteException {
			Bundle b = new Bundle();
			b.putInt(CNT, tileCnt);
			mHandler.sendMessage(mHandler.obtainMessage(R.id.tile_done, b));
		}

	};

	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case R.id.done:
				findViewById(R.id.open).setVisibility(View.VISIBLE);
				findViewById(R.id.progress).setVisibility(View.GONE);
				break;
			case R.id.download_start:
				Bundle b = (Bundle) msg.obj;
				final int tileCnt = b.getInt(CNT);
				final long startTime = b.getLong(TIME);

				mProgress.setMax(tileCnt);
				Ut.w("download_start = "+tileCnt);

				break;
			case R.id.tile_done:
				mProgress.setProgress(((Bundle) msg.obj).getInt(CNT));
				Ut.w("setProgress = "+((Bundle) msg.obj).getInt(CNT));
				break;
			}
		}
	};

	protected void onResume() {
		Intent intent = getIntent();
		if (intent != null) {
			if (mTileSource != null)
				mTileSource.Free();

			try {
				mTileSource = new TileSource(this, intent.getStringExtra("MAPID"));
			} catch (Exception e) {
			}
			mMap.setTileSource(mTileSource);
			mMap.getController().setZoom(intent.getIntExtra("ZoomLevel", 0));
			mMap.getController().setCenter(new GeoPoint(intent.getIntExtra("Latitude", 0), intent.getIntExtra("Longitude", 0)));
			setTitle();
		}

		Ut.w("bindService");
		bindService(new Intent(IRemoteService.class.getName()), mConnection, 0);

		super.onResume();
	}

	@Override
	protected void onPause() {
		unbindService(mConnection);
		
		super.onPause();
	}

	private void setTitle(){
		try {
			final TextView leftText = (TextView) findViewById(R.id.left_text);
			if(leftText != null)
				leftText.setText(mMap.getTileSource().NAME);
			
			final TextView gpsText = (TextView) findViewById(R.id.gps_text);
			if(gpsText != null){
				gpsText.setText("");
			}

			final TextView rightText = (TextView) findViewById(R.id.right_text);
			if(rightText != null){
				final double zoom = mMap.getZoomLevelScaled();
				if(zoom > mMap.getTileSource().ZOOM_MAXLEVEL)
					rightText.setText(""+(mMap.getTileSource().ZOOM_MAXLEVEL+1)+"+");
				else
					rightText.setText(""+(1 + Math.round(zoom)));
			}
		} catch (Exception e) {
		}
	}


}