package com.robert.maps.tileprovider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.andnav.osm.views.util.StreamUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.robert.maps.utils.ICacheProvider;
import com.robert.maps.utils.SQLiteMapDatabase;
import com.robert.maps.utils.SimpleThreadFactory;
import com.robert.maps.utils.Ut;

public class TileProviderInet extends TileProviderBase {
	private ICacheProvider mCacheProvider = null;
	private ExecutorService mThreadPool = Executors.newFixedThreadPool(5, new SimpleThreadFactory("TileProviderInet"));

	public TileProviderInet(Context ctx, TileURLGeneratorBase gen, final String cacheDatabaseName) {
		super(ctx);
		mTileURLGenerator = gen;
		mTileCache = new MapTileMemCache();
		if(cacheDatabaseName != null) {
			final SQLiteMapDatabase cacheDatabase = new SQLiteMapDatabase();
			final File folder = Ut.getRMapsMainDir(ctx, "cache");
			cacheDatabase.setFile(folder.getAbsolutePath()+"/"+cacheDatabaseName+".sqlitedb");
			mCacheProvider = cacheDatabase;
		} else {
			final File folder = Ut.getRMapsCacheTilesDir(ctx);
			mCacheProvider = new FSCacheProvider(folder);
		}
	}

	@Override
	public void Free() {
		mThreadPool.shutdown();
		if(mCacheProvider != null)
			mCacheProvider.Free();
		super.Free();
	}

	@Override
	public Bitmap getTile(final int x, final int y, final int z) {
		final String tileurl = mTileURLGenerator.Get(x, y, z);
		
		final Bitmap bmp = mTileCache.getMapTile(tileurl);
		if(bmp != null)
			return bmp;
		
		if (this.mPending.contains(tileurl))
			return super.getTile(x, y, z);
		
		mPending.add(tileurl);
		
		mThreadPool.execute(new Runnable() {
			public void run() {
				InputStream in = null;
				OutputStream out = null;

				try {
					Ut.i("Downloading Maptile from url: " + tileurl);

					byte[] data = null;

					if(mCacheProvider != null)
						data = mCacheProvider.getTile(tileurl, x, y, z);

					if(data == null) {
						Ut.w("FROM INTERNET "+tileurl);
						in = new BufferedInputStream(new URL(tileurl).openStream(), StreamUtils.IO_BUFFER_SIZE);

						final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
						out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
						StreamUtils.copy(in, out);
						out.flush();

						data = dataStream.toByteArray();

						if(mCacheProvider != null)
							mCacheProvider.putTile(tileurl, x, y, z, data);
					}

					if(data != null) {
						final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
						mTileCache.putTile(tileurl, bmp);
					}

					
					SendMessageSuccess();
				} catch (Exception e) {
					SendMessageFail();
				} catch (OutOfMemoryError e) {
					Ut.w("OutOfMemoryError");
					SendMessageFail();
					System.gc();
				} finally {
					StreamUtils.closeStream(in);
					StreamUtils.closeStream(out);
				}
				mPending.remove(tileurl);
			}
		});
		
		return mLoadingMapTile;
	}

}
