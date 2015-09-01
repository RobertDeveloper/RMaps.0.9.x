package com.robert.maps.applib.downloader;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.widget.Toast;

import com.robert.maps.R;
import com.robert.maps.applib.tileprovider.TileProviderFileBase;
import com.robert.maps.applib.tileprovider.TileSource;
import com.robert.maps.applib.utils.SQLiteMapDatabase;
import com.robert.maps.applib.utils.SimpleThreadFactory;
import com.robert.maps.applib.utils.Ut;

import org.andnav.osm.views.util.StreamUtils;
import org.andnav.osm.views.util.Util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MapDownloaderService extends Service {
	private final int THREADCOUNT = 1;
	
	private NotificationManager mNM;
	private Notification mNotification;
	private PendingIntent mContentIntent = null;
	private int mZoomArr[];
	private int mCoordArr[];
	private String mMapID;
	private int mZoom;
	private String mOfflineMapName;
	private boolean mOverwriteFile;
	private boolean mOverwriteTiles;
	private boolean mLoadToOnlineCache;
	private TileIterator mTileIterator;
	private SQLiteMapDatabase mMapDatabase;
	private TileSource mTileSource;
	private ExecutorService mThreadPool = Executors.newFixedThreadPool(THREADCOUNT, new SimpleThreadFactory(
			"MapDownloaderService"));
	private Handler mHandler = new DownloaderHanler();
	final RemoteCallbackList<IDownloaderCallback> mCallbacks = new RemoteCallbackList<IDownloaderCallback>();
	private int mTileCntTotal = 0, mTileCnt = 0, mErrorCnt = 0;
	private long mStartTime = 0;
	private String mLogFileName;
	
	private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {
		public void registerCallback(IDownloaderCallback cb) {
			if (cb != null) {
				mCallbacks.register(cb);
				if (mStartTime > 0)
					try {
						cb.downloadStart(mTileCntTotal, mStartTime, mLoadToOnlineCache ? "" : mOfflineMapName, mMapID, mZoom, mCoordArr[0], mCoordArr[1],
								mCoordArr[2], mCoordArr[3]);
					} catch (RemoteException e) {
					}
			}
		}
		
		public void unregisterCallback(IDownloaderCallback cb) {
			if (cb != null)
				mCallbacks.unregister(cb);
		}
	};
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mLogFileName = Ut.getRMapsMainDir(this, "").getAbsolutePath()+"/cache/mapdownloaderlog.txt";
		final File file = new File(mLogFileName);
		if(file.exists())
			file.delete();
		
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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
		if(mStartTime > 0) {
			Toast.makeText(this, R.string.downloader_notif_text, Toast.LENGTH_LONG).show();
			return;
		}
		
		checkLimitation();
		
		mZoomArr = intent.getIntArrayExtra("ZOOM");
		mCoordArr = intent.getIntArrayExtra("COORD");
		mMapID = intent.getStringExtra("MAPID");
		mZoom = intent.getIntExtra("ZOOMCUR", 0);
		mOfflineMapName = intent.getStringExtra("OFFLINEMAPNAME");
		mOverwriteFile = intent.getBooleanExtra("overwritefile", true);
		mOverwriteTiles = intent.getBooleanExtra("overwritetiles", false);
		mLoadToOnlineCache = intent.getBooleanExtra("online_cache", false);
		
		mContentIntent = PendingIntent.getActivity(
				this,
				0,
				new Intent(this, DownloaderActivity.class)
						.putExtra("MAPID", mMapID)
						.putExtra("Latitude", mCoordArr[2] - mCoordArr[0])
						.putExtra("Longitude", mCoordArr[3] - mCoordArr[1])
						.putExtra("ZoomLevel", mZoomArr[0])
						.putExtra("OFFLINEMAPNAME", mOfflineMapName), 0);
		showNotification();
		
		try {
			mTileSource = new TileSource(this, mMapID, true, false);
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
		
		final SQLiteMapDatabase cacheDatabase = new SQLiteMapDatabase();
		
		if(mLoadToOnlineCache) {
			if(mTileSource.CACHE.trim().equalsIgnoreCase(""))
				mOfflineMapName = mTileSource.ID;
			else
				mOfflineMapName = mTileSource.CACHE;
		}

		final File folder = mLoadToOnlineCache ? Ut.getRMapsMainDir(this, "cache") : Ut.getRMapsMapsDir(this);
		final File file = new File(folder.getAbsolutePath() + "/" + mOfflineMapName + ".sqlitedb");
		if (mOverwriteFile && !mLoadToOnlineCache) {
			File[] files = folder.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].getName().startsWith(file.getName()))
						files[i].delete();
				}
			}
		}
		
		try {
			cacheDatabase.setFile(file.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
		mMapDatabase = cacheDatabase;
		
		mTileCnt = 0;
		mTileCntTotal = getTileCount(mZoomArr, mCoordArr);
		mTileIterator = new TileIterator(mZoomArr, mCoordArr);
		mStartTime = System.currentTimeMillis();
		
		final int N = mCallbacks.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mCallbacks.getBroadcastItem(i).downloadStart(mTileCntTotal, mStartTime, mOfflineMapName, mMapID, mZoom, mCoordArr[0],
						mCoordArr[1], mCoordArr[2], mCoordArr[3]);
			} catch (RemoteException e) {
			}
		}
		mCallbacks.finishBroadcast();
		
		for (int i = 0; i < THREADCOUNT; i++)
			mThreadPool.execute(new Downloader());
	}
	
	private void checkLimitation() {
//		InputStream in = null;
//		OutputStream out = null;
//
//		try {
//			in = new BufferedInputStream(new URL("https://sites.google.com/site/robertk506/limits.txt").openStream(), StreamUtils.IO_BUFFER_SIZE);
//
//			final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
//			out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
//			StreamUtils.copy(in, out);
//			out.flush();
//
//			String str = dataStream.toString();
//			Ut.w("checkLimitation: "+str);
//			//JSONObject json = new JSONObject(str.replace("YMaps.TrafficLoader.onLoad(\"stat\",", "").replace("});", "}"));
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			StreamUtils.closeStream(in);
//			StreamUtils.closeStream(out);
//		}
	}

	@Override
	public void onDestroy() {
		if (mThreadPool != null) {
			mThreadPool.shutdown();
			try {
				if (!mThreadPool.awaitTermination(5L, TimeUnit.SECONDS)) {
					mThreadPool.shutdownNow();
				}
			} catch (InterruptedException e) {
			}
		}
		
		try {
			TileProviderFileBase provider = new TileProviderFileBase(this);
			provider.CommitIndex(mMapDatabase.getID("usermap_"), 0, 0,
					mMapDatabase.getMinZoom(), mMapDatabase.getMaxZoom());
			provider.Free();
		} catch (Exception e1) {
		}
		
		final int N = mCallbacks.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mCallbacks.getBroadcastItem(i).downloadDone();
			} catch (RemoteException e) {
			}
		}
		mCallbacks.finishBroadcast();
		
		if (mMapDatabase != null) {
			mMapDatabase.setParams(mMapID, mTileSource.NAME, mCoordArr, mZoomArr, mZoom);
			mMapDatabase.Free();
		}
		
		if (mTileSource != null)
			mTileSource.Free();
		
		//mNM.cancel(R.id.downloader_service);
		stopForegroundCompat(R.id.downloader_service);
		mNM = null;
		
		mStartTime = 0;
		
		super.onDestroy();
	}
	
	@TargetApi(Build.VERSION_CODES.ECLAIR)
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.downloader_notif_ticket);
		
		// Set the icon, scrolling text and timestamp
		mNotification = new Notification(R.drawable.r_download, text, System.currentTimeMillis());
		mNotification.flags = mNotification.flags | Notification.FLAG_NO_CLEAR;
		
		// The PendingIntent to launch our activity if the user selects this
		// notification
		// mContentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
		// MainActivity.class), 0);
		// Set the info for the views that show in the notification panel.
		mNotification.setLatestEventInfo(this, getText(R.string.downloader_notif_title),
				getText(R.string.downloader_notif_text), mContentIntent);
		
		// Send the notification.
		// We use a string id because it is a unique number. We use it later to
		// cancel.
		//mNM.notify(R.id.downloader_service, mNotification);
		startForegroundCompat(R.id.downloader_service, mNotification);
	}
	
	private void downloadDone() {
		stopSelf();
		
		// ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(R.string.remote_service_started);
		//
		// CharSequence text = getText(R.string.auto_follow_enabled);
		//
		// // Set the icon, scrolling text and timestamp
		// Notification notification = new
		// Notification(R.drawable.track_writer_service, text,
		// System.currentTimeMillis());
		// //notification.flags = notification.flags |
		// Notification.FLAG_NO_CLEAR;
		//
		// // The PendingIntent to launch our activity if the user selects this
		// notification
		// PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		// new Intent(this, AreaSelectorActivity.class), 0);
		//
		// // Set the info for the views that show in the notification panel.
		// notification.setLatestEventInfo(this,
		// getText(R.string.auto_follow_enabled), text, contentIntent);
		//
		// // Send the notification.
		// // We use a string id because it is a unique number. We use it later
		// to cancel.
		// ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(R.string.auto_follow_enabled,
		// notification);
		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	private class DownloaderHanler extends Handler {
		private int doneCounter = 0;
		
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == R.id.done) {
				doneCounter++;
				if (doneCounter >= THREADCOUNT)
					downloadDone();
			} else if(msg.what == R.id.tile_done || msg.what == R.id.tile_error) {
				mTileCnt++;
				if (msg.what == R.id.tile_error)
					mErrorCnt++;
				
				mNotification.setLatestEventInfo(
						MapDownloaderService.this,
						getText(R.string.downloader_notif_title),
						getText(R.string.downloader_notif_text)
								+ String.format(": %d%% (%d/%d)", (mTileCnt * 100 / mTileCntTotal), mTileCnt,
										mTileCntTotal), mContentIntent);
				if (mNM != null)
					mNM.notify(R.id.downloader_service, mNotification);
				
				final int N = mCallbacks.beginBroadcast();
				final XYZ tileParam = (XYZ) msg.obj;
				for (int i = 0; i < N; i++) {
					try {
						if (tileParam == null)
							mCallbacks.getBroadcastItem(i).downloadTileDone(mTileCnt, mErrorCnt, -1, -1, -1);
						else
							mCallbacks.getBroadcastItem(i).downloadTileDone(mTileCnt, mErrorCnt, tileParam.X,
									tileParam.Y, tileParam.Z);
					} catch (RemoteException e) {
					}
				}
				mCallbacks.finishBroadcast();
			}
		}
		
	}
	
	private class Downloader implements Runnable {
		public void run() {
			
			XYZ tileParam = null;
			boolean continueExecute = true;
			
			while (continueExecute && !mThreadPool.isShutdown()) {
				synchronized (mTileIterator) {
					if (mTileIterator.hasNext()) {
						tileParam = mTileIterator.next();
					} else {
						continueExecute = false;
						tileParam = null;
					}
				}
				
				if (tileParam != null) {
					tileParam.TILEURL = mTileSource.getTileURLGenerator().Get(tileParam.X, tileParam.Y, tileParam.Z);
					InputStream in = null;
					OutputStream out = null;
					
					try {
						if (mOverwriteFile || mOverwriteTiles || !mOverwriteTiles
								&& !mMapDatabase.existsTile(tileParam.X, tileParam.Y, tileParam.Z)) {
							
							byte[] data = null;
							final URL url = new URL(tileParam.TILEURL);
				        	final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				            connection.connect();

				            if(connection.getResponseCode() != 200)
								Ut.appendLog(mLogFileName, String.format("%tc %s\n	Response: %d %s", System.currentTimeMillis(), tileParam.TILEURL, connection.getResponseCode(), connection.getResponseMessage()));

							in = new BufferedInputStream(url.openStream(), StreamUtils.IO_BUFFER_SIZE);
							
							final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
							out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
							StreamUtils.copy(in, out);
							out.flush();
							
							data = dataStream.toByteArray();
							
							if (data != null) {
								if (mOverwriteTiles)
									mMapDatabase.deleteTile(tileParam.TILEURL, tileParam.X, tileParam.Y, tileParam.Z);
								
								mMapDatabase.putTile(tileParam.X, tileParam.Y, tileParam.Z, data);
							}
						}
						
						if (mHandler != null)
							Message.obtain(mHandler, R.id.tile_done, tileParam).sendToTarget();
					} catch (Exception e) {
						Ut.appendLog(mLogFileName, String.format("%tc %s\n	Error: %s", System.currentTimeMillis(), tileParam.TILEURL, e.getMessage()));
						if (mHandler != null)
							Message.obtain(mHandler, R.id.tile_error, tileParam).sendToTarget();
					} catch (OutOfMemoryError e) {
						Ut.appendLog(mLogFileName, String.format("%tc %s\n	Error: %s", System.currentTimeMillis(), tileParam.TILEURL, e.getMessage()));
						if (mHandler != null)
							Message.obtain(mHandler, R.id.tile_error, tileParam).sendToTarget();
						System.gc();
					} finally {
						StreamUtils.closeStream(in);
						StreamUtils.closeStream(out);
					}
					
				}
//				try {
//					Thread.sleep(400);
//				} catch (InterruptedException e) {
//				}
			}
			
			if (mHandler != null)
				Message.obtain(mHandler, R.id.done).sendToTarget();
		}
		
	}
	
	private int getTileCount(int[] zArr, int[] coordArr) {
		int xMin = 0, xMax = 0;
		int yMin = 0, yMax = 0;
		int cnt = 0;
		
		for (int i = 0; i < zArr.length; i++) {
			final int c0[] = Util.getMapTileFromCoordinates(coordArr[0], coordArr[1], zArr[i], null,
					mTileSource.PROJECTION);
			final int c1[] = Util.getMapTileFromCoordinates(coordArr[2], coordArr[3], zArr[i], null,
					mTileSource.PROJECTION);
			xMin = Math.min(c0[0], c1[0]);
			xMax = Math.max(c0[0], c1[0]);
			yMin = Math.min(c0[1], c1[1]);
			yMax = Math.max(c0[1], c1[1]);
			cnt += (xMax - xMin + 1) * (yMax - yMin + 1);
		}
		return cnt;
	}
	
	private class TileIterator {
		private int zInd = -1, zArr[];
		private int x, xMin = 0, xMax = 0;
		private int y, yMin = 0, yMax = 0;
		private int coordArr[];
		
		public TileIterator(int zarr[], int coordarr[]) {
			zArr = zarr;
			zInd = -1;
			x = 1;
			y = 1;
			coordArr = coordarr;
		}
		
		public boolean hasNext() {
			x++;
			if (x > xMax) {
				y++;
				x = xMin;
				if (y > yMax) {
					zInd++;
					y = yMin;
					if (zInd > zArr.length - 1) {
						return false;
					}
					final int c0[] = Util.getMapTileFromCoordinates(coordArr[0], coordArr[1], zArr[zInd], null,
							mTileSource.PROJECTION);
					final int c1[] = Util.getMapTileFromCoordinates(coordArr[2], coordArr[3], zArr[zInd], null,
							mTileSource.PROJECTION);
					yMin = Math.min(c0[0], c1[0]);
					yMax = Math.max(c0[0], c1[0]);
					xMin = Math.min(c0[1], c1[1]);
					xMax = Math.max(c0[1], c1[1]);
					x = xMin;
					y = yMin;
				}
			}
			
			return true;
		}
		
		public XYZ next() {
			try {
				return new XYZ(null, x, y, zArr[zInd]);
			} catch (Exception e) {
				return null;
			}
		}
		
	}
	
	private class XYZ {
		public String TILEURL;
		public int X;
		public int Y;
		public int Z;
		
		public XYZ(final String tileurl, final int x, final int y, final int z) {
			TILEURL = tileurl;
			X = x;
			Y = y;
			Z = z;
		}
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
