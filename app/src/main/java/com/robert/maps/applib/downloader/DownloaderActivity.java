package com.robert.maps.applib.downloader;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.robert.maps.applib.MainActivity;
import com.robert.maps.R;
import com.robert.maps.applib.tileprovider.TileSource;
import com.robert.maps.applib.tileprovider.TileSourceBase;
import com.robert.maps.applib.utils.Ut;
import com.robert.maps.applib.view.MapView;

import org.andnav.osm.util.GeoPoint;

import java.io.File;

public class DownloaderActivity extends Activity {
	private static final String CNT = "cnt";
	private static final String ERRCNT = "errcnt";
	private static final String TIME = "time";
	private static final String MAPID = "mapid";
	private static final String ZOOM = "zoom";
	private static final String LAT = "lat";
	private static final String LON = "lon";

	private MapView mMap;
	private TileSource mTileSource;
	private String mMapID;
	private GeoPoint mCenter;
	private DownloadedAreaOverlay mDownloadedAreaOverlay;
	private ServiceConnection mConnection;
	IRemoteService mService = null;
	private ProgressBar mProgress;
	private TextView mTextVwTileCnt;
	private TextView mTextVwError;
	private TextView mTextVwTime;
	private int mTileCntTotal;
	private long mStartTime;
	private String mFileName;
	private boolean mLoadToOnlineCache;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.downloaderactivity);

		SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);

		mMap = (MapView) findViewById(R.id.map);
		mCenter = new GeoPoint(uiState.getInt("Latitude", 0), uiState.getInt("Longitude", 0));
		mMap.getController().setCenter(mCenter);
		mMap.setLongClickable(false);

		mProgress = (ProgressBar) findViewById(R.id.progress);
		mTextVwTileCnt = (TextView) findViewById(R.id.textTileCnt);
		mTextVwTime = (TextView) findViewById(R.id.textTime);
		mTextVwError = (TextView) findViewById(R.id.textError);
		mTextVwError.setTextColor(mTextVwError.getLinkTextColors());
		mTextVwError.setPaintFlags(mTextVwError.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		
		mDownloadedAreaOverlay = new DownloadedAreaOverlay();
		mMap.getOverlays().add(mDownloadedAreaOverlay);
		
		findViewById(R.id.pause).setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				final Intent intent = new Intent("com.robert.maps.mapdownloader");
				stopService(intent);
			}
		});
		
		findViewById(R.id.open).setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				doOpenMap();
			}
		});
		
		mTextVwError.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final File file = new File(Ut.getRMapsMainDir(DownloaderActivity.this, "").getAbsolutePath()+"/cache/mapdownloaderlog.txt");
				final Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(file), "text/plain");
				startActivity(intent);
			}
		});

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

	protected void doOpenMap() {
		final String name = Ut.FileName2ID(mFileName+".sqlitedb");
		startActivity(new Intent(this, MainActivity.class)
		.setAction("SHOW_MAP_ID")
		.putExtra("MapName", mLoadToOnlineCache ? mTileSource.ID : "usermap_"+name)
		.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
		);
		finish();
	}

	private IDownloaderCallback mCallback = new IDownloaderCallback.Stub() {

		public void downloadDone() throws RemoteException {
			if (mHandler != null) {
				mHandler.sendMessage(mHandler.obtainMessage(R.id.done));
			}
		}

		public void downloadStart(int tileCnt, long startTime, String fileName, String mapid, int zoom, int lat0, int lon0, int lat1, int lon1) throws RemoteException {
			Bundle b = new Bundle();
			b.putInt(CNT, tileCnt);
			b.putLong(TIME, startTime);
			b.putString(MAPID, mapid);
			b.putInt(ZOOM, zoom);
			b.putInt(LAT, lat0 + (lat1 - lat0) / 2);
			b.putInt(LON, lon0 + (lon1 - lon0) / 2);
			mFileName = fileName;
			mLoadToOnlineCache = mFileName.equalsIgnoreCase("");
			mDownloadedAreaOverlay.Init(DownloaderActivity.this, lat0, lon0, lat1, lon1);
			mHandler.sendMessage(mHandler.obtainMessage(R.id.download_start, b));
			
			final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(DownloaderActivity.this);
			final Editor editor = pref.edit();
			final String name = Ut.FileName2ID(mFileName+".sqlitedb");
			editor.putBoolean(TileSourceBase.PREF_USERMAP_+name+"_enabled", true);
			editor.putString(TileSourceBase.PREF_USERMAP_+name+"_name", pref.getString(TileSourceBase.PREF_USERMAP_+name+"_name", mFileName));
			editor.putString(TileSourceBase.PREF_USERMAP_+name+"_projection", Integer.toString(mTileSource.PROJECTION));
			final File folder = Ut.getRMapsMapsDir(DownloaderActivity.this);
			editor.putString(TileSourceBase.PREF_USERMAP_+name+"_baseurl", folder.getAbsolutePath() + "/" + mFileName + ".sqlitedb");
			editor.putBoolean(TileSourceBase.PREF_USERMAP_+name+"_isoverlay", mTileSource.LAYER);
			editor.commit();
			
		}

		public void downloadTileDone(int tileCnt, int errorCnt, int x, int y, int z) throws RemoteException {
			Bundle b = new Bundle();
			b.putInt(CNT, tileCnt);
			b.putInt(ERRCNT, errorCnt);
			mDownloadedAreaOverlay.setLastDowloadedTile(x, y, z, mMap.getTileView());
			mHandler.sendMessage(mHandler.obtainMessage(R.id.tile_done, b));
		}

	};

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == R.id.done) {
				findViewById(R.id.open).setVisibility(View.VISIBLE);
				findViewById(R.id.progress).setVisibility(View.GONE);
				findViewById(R.id.pause).setVisibility(View.GONE);
				mTextVwTileCnt.setText(Integer.toString(mTileCntTotal));
				mTextVwTime.setText(Ut.formatTime(System.currentTimeMillis() - mStartTime));
				mDownloadedAreaOverlay.downloadDone();
				mMap.invalidate(); //postInvalidate();
				
			} else if(msg.what == R.id.download_start) {
				Bundle b = (Bundle) msg.obj;
				mTileCntTotal = b.getInt(CNT);
				mStartTime = b.getLong(TIME);
				mMapID = b.getString(MAPID);
				final int zoom =b.getInt(ZOOM);
				final int lat =b.getInt(LAT);
				final int lon =b.getInt(LON);
				
				setTitle();

				mProgress.setMax(mTileCntTotal);
				mTextVwTileCnt.setText(Integer.toString(mTileCntTotal));
				mTextVwTime.setText("00:00");

				boolean needChangeTileSource = true;
				if (mTileSource != null) {
					if(mMapID != mTileSource.ID)
						mTileSource.Free();
					else
						needChangeTileSource = false;
				}

				if(needChangeTileSource) {
					try {
						mTileSource = new TileSource(DownloaderActivity.this, mMapID);
					} catch (Exception e) {
					}
					mMap.setTileSource(mTileSource);
				}
				mMap.getController().setZoom(zoom);
				mMap.getController().setCenter(new GeoPoint(lat, lon));

			} else if(msg.what == R.id.tile_done) { 
				final int tileCnt = ((Bundle) msg.obj).getInt(CNT);
				final int errorCnt = ((Bundle) msg.obj).getInt(ERRCNT);
				mProgress.setProgress(tileCnt);
				
				mTextVwTileCnt.setText(String.format("%d/%d", tileCnt, mTileCntTotal));
				if(errorCnt > 0)
					mTextVwError.setText(String.format("ERRORS: %d", errorCnt));
				
				final long time = System.currentTimeMillis();
				if(time - mStartTime > 5 * 1000) 
					mTextVwTime.setText(String.format("%s / %s", Ut.formatTime(time - mStartTime), Ut.formatTime((long)((double)(time - mStartTime) / (1.0f * tileCnt / mTileCntTotal))) ));
				else
					mTextVwTime.setText(Ut.formatTime(time - mStartTime));
				
				mMap.invalidate(); //postInvalidate();
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

		bindService(new Intent(IRemoteService.class.getName()), mConnection, 0);
		super.onResume();
	}

	@Override
	protected void onPause() {
		unbindService(mConnection);
		
		if (mTileSource != null)
			mTileSource.Free();

		super.onPause();
	}

	private void setTitle(){
		try {
			final TextView leftText = (TextView) findViewById(R.id.left_text);
			if(leftText != null)
				leftText.setText(mFileName);
			
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