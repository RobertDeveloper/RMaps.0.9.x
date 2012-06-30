package com.robert.maps.tileprovider;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.andnav.osm.views.util.OpenStreetMapTileCache;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Message;

import com.robert.maps.R;
import com.robert.maps.utils.SQLiteMapDatabase;
import com.robert.maps.utils.Ut;

public class TileProviderSQLITEDB extends TileProviderFileBase {
	private ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
	private SQLiteMapDatabase mUserMapDatabase;
	private String mMapID;
	private ProgressDialog mProgressDialog;

	public TileProviderSQLITEDB(Context ctx, final String filename, final String mapid) {
		super(ctx);
		mTileURLGenerator = new TileURLGeneratorBase(filename);
		mTileCache = new OpenStreetMapTileCache(20);
		mUserMapDatabase = new SQLiteMapDatabase();
		mUserMapDatabase.setFile(filename);
		mMapID = mapid;
		
		final File file = new File(filename);
		if(needIndex(mapid, file.length(), file.lastModified(), false)) {
			mProgressDialog = Ut.ShowWaitDialog(ctx, R.string.message_updateminmax);
			new IndexTask().execute(file.length(), file.lastModified());
		}
		
		this.mThreadPool.execute(new Runnable() {
			public void run() {
				XYZ xyz = null;
				Collection<XYZ> col = null;
				Iterator<XYZ> it = null;
				byte[] data = null;
				Bitmap bmp = null;
				
				while(!mThreadPool.isShutdown()) {
					synchronized(mPending2) {
						col = mPending2.values();
						it = col.iterator();
						if (it.hasNext()) 
							xyz = it.next();
						else
							xyz = null;
					}
					
					if(xyz == null) {
						synchronized(mPending2) {
							try {
								SendMessageSuccess();
								mPending2.wait();
							} catch (InterruptedException e) {
							}
						}
					}
					else {
						data = mUserMapDatabase.getTile(xyz.X, xyz.Y, xyz.Z);
						
						if (data != null) {
							bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
							mTileCache.putTile(xyz.TILEURL, bmp);
						}
							
						synchronized(mPending2) {
							mPending2.remove(xyz.TILEURL);
						}
					}
					
				}
			}
		});
	}
	
	public void updateMapParams(TileSource tileSource) {
		tileSource.ZOOM_MINLEVEL = ZoomMinInCashFile(mMapID);
		tileSource.ZOOM_MAXLEVEL = ZoomMaxInCashFile(mMapID);
	}
	
	private class IndexTask extends AsyncTask<Long, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Long... params) {
			try {
				final long fileLength = params[0];
				final long fileModified = params[1];
				mUserMapDatabase.updateMinMaxZoom();
				final int minzoom = mUserMapDatabase.getMinZoom();
				final int maxzoom = mUserMapDatabase.getMaxZoom();
	
				CommitIndex(mMapID, fileLength, fileModified, minzoom, maxzoom);
			} catch (Exception e) {
				return false;
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if(result)
				Message.obtain(mCallbackHandler, MessageHandlerConstants.MAPTILEFSLOADER_INDEXIND_SUCCESS_ID).sendToTarget();
			if(mProgressDialog != null)
				mProgressDialog.dismiss();
		}
	}

	@Override
	public void Free() {
		Ut.d("TileProviderSQLITEDB Free");
		mUserMapDatabase.freeDatabases();
		mThreadPool.shutdown();
		super.Free();
	}

	private HashMap<String, XYZ> mPending2 = new HashMap<String, XYZ>();
	
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
	
	public Bitmap getTile(final int x, final int y, final int z) {
		final String tileurl = mTileURLGenerator.Get(x, y, z);
		
		final Bitmap bmp = mTileCache.getMapTile(tileurl);
		if(bmp != null)
			return bmp;
		
		synchronized(mPending2) {
			if (this.mPending2.containsKey(tileurl))
				return mLoadingMapTile;
		}
		
		synchronized(mPending2) {
			mPending2.put(tileurl, new XYZ(tileurl, x, y, z));
			mPending2.notify();
		}
		
		return mLoadingMapTile;
	}

}
