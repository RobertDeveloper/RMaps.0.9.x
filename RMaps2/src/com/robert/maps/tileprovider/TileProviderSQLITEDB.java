package com.robert.maps.tileprovider;

import java.io.File;
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

	public Bitmap getTile(final int x, final int y, final int z) {
		final String tileurl = mTileURLGenerator.Get(x, y, z);
		
		final Bitmap bmp = mTileCache.getMapTile(tileurl);
		if(bmp != null)
			return bmp;
		
		if (this.mPending.contains(tileurl))
			return super.getTile(x, y, z);
		
		mPending.add(tileurl);
		
		this.mThreadPool.execute(new Runnable() {
			public void run() {
				try {
					// File exists, otherwise a FileNotFoundException would have been thrown
					//OpenStreetMapTileFilesystemProvider.this.mDatabase.incrementUse(formattedTileURLString);

					final byte[] data = mUserMapDatabase.getTile(x, y, z);

					if(data != null){
						final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
						mTileCache.putTile(tileurl, bmp);
						SendMessageSuccess();
					}
				} catch (OutOfMemoryError e) {
					SendMessageFail();
					System.gc();
				} catch (Exception e) {
					SendMessageFail();
				}

				mPending.remove(tileurl);
			}
		});
		
		
		return mLoadingMapTile;
	}

}
