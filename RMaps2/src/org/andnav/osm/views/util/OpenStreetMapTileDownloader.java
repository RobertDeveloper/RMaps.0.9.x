// Created by plusminus on 21:31:36 - 25.09.2008
package org.andnav.osm.views.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import com.robert.maps.utils.SQLiteMapDatabase;
import com.robert.maps.utils.Ut;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class OpenStreetMapTileDownloader implements OpenStreetMapConstants, OpenStreetMapViewConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int MAPTILEDOWNLOADER_SUCCESS_ID = 0;
	public static final int MAPTILEDOWNLOADER_FAIL_ID = MAPTILEDOWNLOADER_SUCCESS_ID + 1;

	// ===========================================================
	// Fields
	// ===========================================================

	protected HashSet<String> mPending = new HashSet<String>();
	protected Context mCtx;
	protected OpenStreetMapTileFilesystemProvider mMapTileFSProvider;
	protected ExecutorService mThreadPool = Executors.newFixedThreadPool(5);
	protected SQLiteMapDatabase mCacheDatabase;

	// ===========================================================
	// Constructors
	// ===========================================================

	public void setCacheDatabase(final String aCacheDatabaseName) {
    	if(aCacheDatabaseName != null && aCacheDatabaseName != ""){
    		try {
				this.mCacheDatabase = new SQLiteMapDatabase();
				final File folder = Ut.getRMapsMainDir(mCtx, "cache");
				this.mCacheDatabase.setFile(folder.getAbsolutePath()+"/"+aCacheDatabaseName+".sqlitedb");
			} catch (Exception e) {
				e.printStackTrace();
				this.mCacheDatabase = null;
			}
    	} else
    		this.mCacheDatabase = null;
	}

	public OpenStreetMapTileDownloader(final Context ctx, final OpenStreetMapTileFilesystemProvider aMapTileFSProvider){
		this.mCtx = ctx;
		this.mMapTileFSProvider = aMapTileFSProvider;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	/** Sets the Child-ImageView of this to the URL passed. */
	public void getRemoteImageAsync(final String aURLString, final Handler callback, final int aX, final int aY, final int aZ) {
		this.mThreadPool.execute(new Runnable(){
			public void run() {
				InputStream in = null;
				OutputStream out = null;

				try {
					if(DEBUGMODE)
						Log.i(DEBUGTAG, "Downloading Maptile from url: " + aURLString);

					byte[] data = null;

					if(OpenStreetMapTileDownloader.this.mCacheDatabase != null)
						data = OpenStreetMapTileDownloader.this.mCacheDatabase.getTile(aX, aY, aZ);

					if(data == null) {
						Ut.w("FROM INTERNET "+aURLString);
						in = new BufferedInputStream(new URL(aURLString).openStream(), StreamUtils.IO_BUFFER_SIZE);

						final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
						out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
						StreamUtils.copy(in, out);
						out.flush();

						data = dataStream.toByteArray();

						if(OpenStreetMapTileDownloader.this.mCacheDatabase != null)
							OpenStreetMapTileDownloader.this.mCacheDatabase.putTile(aX, aY, aZ, data);
					} else
						Ut.w("FROM CACHE "+aURLString);

					OpenStreetMapTileDownloader.this.mMapTileFSProvider.saveFile(aURLString, data);
					if(DEBUGMODE)
						Log.i(DEBUGTAG, "Maptile saved to: " + aURLString);

					final Message successMessage = Message.obtain(callback, MAPTILEDOWNLOADER_SUCCESS_ID);
					successMessage.sendToTarget();
					OpenStreetMapTileDownloader.this.mPending.remove(aURLString);
				} catch (Exception e) {
					final Message failMessage = Message.obtain(callback, MAPTILEDOWNLOADER_FAIL_ID);
					failMessage.sendToTarget();
					if(DEBUGMODE)
						Log.e(DEBUGTAG, "Error Downloading MapTile. Exception: " + e.getClass().getSimpleName(), e);
					/* LATER What to do when downloading tile caused an error?
					 * Also remove it from the mPending?
					 * Doing not blocks it for the whole existence of this TileDownloder.
					 */
				} catch (OutOfMemoryError e) {
					Ut.w("OutOfMemoryError");
					final Message failMessage = Message.obtain(callback, MAPTILEDOWNLOADER_FAIL_ID);
					failMessage.sendToTarget();
					if(DEBUGMODE)
						Log.e(DEBUGTAG, "Error Downloading MapTile. Exception: " + e.getClass().getSimpleName(), e);
				} finally {
					StreamUtils.closeStream(in);
					StreamUtils.closeStream(out);
				}
			}
		});
	}

	public void requestMapTileAsync(final String aURLString, final Handler callback, final int aX, final int aY, final int aZ) {
		if(this.mPending.contains(aURLString))
			return;

		this.mPending.add(aURLString);
		getRemoteImageAsync(aURLString, callback, aX, aY, aZ);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
