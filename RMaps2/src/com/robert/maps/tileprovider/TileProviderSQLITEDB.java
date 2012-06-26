package com.robert.maps.tileprovider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.andnav.osm.views.util.OpenStreetMapTileCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.robert.maps.utils.SQLiteMapDatabase;
import com.robert.maps.utils.Ut;

public class TileProviderSQLITEDB extends TileProviderBase {
	private ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
	private SQLiteMapDatabase mUserMapDatabase;

	public TileProviderSQLITEDB(Context ctx, final String filename) {
		super(ctx);
		mTileURLGenerator = new TileURLGeneratorBase(filename);
		mTileCache = new OpenStreetMapTileCache(20);
		mUserMapDatabase = new SQLiteMapDatabase();
		mUserMapDatabase.setFile(filename);
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
					}
				} catch (Exception e) {
					SendMessageFail();
				} catch (OutOfMemoryError e) {
					SendMessageFail();
					System.gc();
				}

				SendMessageSuccess();
				mPending.remove(tileurl);
			}
		});
		
		
		return mLoadingMapTile;
	}

}
