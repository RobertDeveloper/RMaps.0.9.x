// Created by plusminus on 21:46:22 - 25.09.2008
package org.andnav.osm.views.util;

import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.robert.maps.R;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class OpenStreetMapTileProvider implements OpenStreetMapConstants, OpenStreetMapViewConstants{
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected Bitmap mLoadingMapTile;
	protected Context mCtx;
	protected OpenStreetMapTileCache mTileCache;
	public OpenStreetMapTileFilesystemProvider mFSTileProvider;
	protected OpenStreetMapTileDownloader mTileDownloader;
	private Handler mLoadCallbackHandler = new LoadCallbackHandler();
	private Handler mDownloadFinishedListenerHander;
	protected OpenStreetMapRendererInfo mRendererInfo;

	// ===========================================================
	// Constructors
	// ===========================================================

	public OpenStreetMapTileProvider(final Context ctx, final Handler aDownloadFinishedListener, final OpenStreetMapRendererInfo aRendererInfo, final int size) {
		this.mCtx = ctx;
		try {
			this.mLoadingMapTile = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.maptile_loading);
		} catch (OutOfMemoryError e) {
			this.mLoadingMapTile = null;
			e.printStackTrace();
		}
		this.mTileCache = new OpenStreetMapTileCache(size);
		this.mFSTileProvider = new OpenStreetMapTileFilesystemProvider(ctx, 4 * 1024 * 1024, this.mTileCache); // 4MB FSCache
		this.mTileDownloader = new OpenStreetMapTileDownloader(ctx, this.mFSTileProvider);
		this.mDownloadFinishedListenerHander = aDownloadFinishedListener;
		this.mRendererInfo = aRendererInfo;

		switch(aRendererInfo.TILE_SOURCE_TYPE){
		case 1: // AndNav ZIP file
		case 2:
		case 3:
		case 4:
		case 5:
			mFSTileProvider.setCashFile(aRendererInfo.BASEURL, aRendererInfo.TILE_SOURCE_TYPE, aDownloadFinishedListener);
			aRendererInfo.ZOOM_MAXLEVEL = mFSTileProvider.getZoomMaxInCashFile();
			aRendererInfo.ZOOM_MINLEVEL = mFSTileProvider.getZoomMinInCashFile();
			break;
		}
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

	public boolean setRender(final OpenStreetMapRendererInfo aRenderer, final Handler callback){
		boolean ret = true;
		this.mRendererInfo = aRenderer;
		switch(aRenderer.TILE_SOURCE_TYPE){
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
			ret = mFSTileProvider.setCashFile(aRenderer.BASEURL, aRenderer.TILE_SOURCE_TYPE, new SimpleInvalidationHandler());
			aRenderer.ZOOM_MAXLEVEL = mFSTileProvider.getZoomMaxInCashFile();
			aRenderer.ZOOM_MINLEVEL = mFSTileProvider.getZoomMinInCashFile();
			break;
		}
		return ret;
	}

	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
				case OpenStreetMapTileFilesystemProvider.INDEXIND_SUCCESS_ID:
					mRendererInfo.ZOOM_MAXLEVEL = mFSTileProvider.getZoomMaxInCashFile();
					mRendererInfo.ZOOM_MINLEVEL = mFSTileProvider.getZoomMinInCashFile();

					final Message successMessage = Message.obtain(mDownloadFinishedListenerHander, OpenStreetMapTileFilesystemProvider.INDEXIND_SUCCESS_ID);
					successMessage.sendToTarget();

					break;
			}
		}
	}

	public Bitmap getMapTile(final int aTypeCash, final int x, final int y, final int z){
		//Log.d(DEBUGTAG, "getMapTile "+aTileURLString);

		int[] tileID = new int[2];
		tileID[MAPTILE_LATITUDE_INDEX] = y;
		tileID[MAPTILE_LONGITUDE_INDEX] = x;
		
		String aTileURLString = this.mRendererInfo.getTileURLString(tileID, z);
		Bitmap ret = this.mTileCache.getMapTile(aTileURLString);
		if(ret != null){
			if(DEBUGMODE)
				Log.i(DEBUGTAG, "MapTileCache succeded for: " + aTileURLString);
		}

		if (ret == null || this.mTileCache.needsTileUpdate(aTileURLString)){
			if(DEBUGMODE)
				Log.i(DEBUGTAG, "Cache needs update, trying from FS.");
			try {
				if(aTypeCash == 5)  // sqlitedb files
					this.mFSTileProvider.loadMapTileFromSQLite(aTileURLString, this.mLoadCallbackHandler, x, y, z);
				else if(aTypeCash == 4) // TAR files
					this.mFSTileProvider.loadMapTileFromTAR(aTileURLString, this.mLoadCallbackHandler, this.mRendererInfo, x, y, z);
				else if(aTypeCash == 3) { // MapNav files
					this.mFSTileProvider.loadMapTileFromMNM(aTileURLString, this.mLoadCallbackHandler, x, y, z);
				} else
					this.mFSTileProvider.loadMapTileToMemCacheAsync(aTileURLString, this.mLoadCallbackHandler, this.mRendererInfo, x, y, z);
			} catch (Exception e) {
				if(DEBUGMODE)
					Log.d(DEBUGTAG, "Error(" + e.getClass().getSimpleName() + ") loading MapTile from Filesystem: " + OpenStreetMapTileNameFormatter.format(aTileURLString));
			}
			if(ret == null){ /* FS did not contain the MapTile, we need to download it asynchronous. */
				if(DEBUGMODE)
					Log.i(DEBUGTAG, "Requesting Maptile for download.");
				ret = mLoadingMapTile;

				this.mTileDownloader.requestMapTileAsync(aTileURLString, this.mLoadCallbackHandler);
			}
		}
		return ret;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
	private class LoadCallbackHandler extends Handler{
		@Override
		public void handleMessage(final Message msg) {
			final int what = msg.what;
			switch(what){
				case OpenStreetMapTileDownloader.MAPTILEDOWNLOADER_SUCCESS_ID:
					OpenStreetMapTileProvider.this.mDownloadFinishedListenerHander.sendEmptyMessage(OpenStreetMapTileDownloader.MAPTILEDOWNLOADER_SUCCESS_ID);
					if(DEBUGMODE)
						Log.i(DEBUGTAG, "MapTile download success.");
					break;
				case OpenStreetMapTileDownloader.MAPTILEDOWNLOADER_FAIL_ID:
					if(DEBUGMODE)
						Log.e(DEBUGTAG, "MapTile download error.");
					break;

				case OpenStreetMapTileFilesystemProvider.MAPTILEFSLOADER_SUCCESS_ID:
					OpenStreetMapTileProvider.this.mDownloadFinishedListenerHander.sendEmptyMessage(OpenStreetMapTileFilesystemProvider.MAPTILEFSLOADER_SUCCESS_ID);
					if(DEBUGMODE)
						Log.i(DEBUGTAG, "MapTile fs->cache success.");
					break;
				case OpenStreetMapTileFilesystemProvider.MAPTILEFSLOADER_FAIL_ID:
					if(DEBUGMODE)
						Log.e(DEBUGTAG, "MapTile download error.");
					break;
			}
		}
	}

	public void preCacheTile(String aTileURLString) {
		try {
			this.mFSTileProvider.loadMapTileToMemCacheAsync(aTileURLString, mLoadCallbackHandler, null, 0, 0, 0);
		} catch (Exception e) {
			this.mTileDownloader.requestMapTileAsync(aTileURLString, this.mLoadCallbackHandler);
		}
	}

	public void freeDatabases() {
		mFSTileProvider.freeDatabases();
	}

	public void setOverzoom(int overzoom) {
		mFSTileProvider.setOverzoom(overzoom);
	}
}
