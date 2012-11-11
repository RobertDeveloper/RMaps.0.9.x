package com.robert.maps.tileprovider;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.robert.maps.MainPreferences;
import com.robert.maps.applib.R;
import com.robert.maps.utils.RException;
import com.robert.maps.utils.Ut;

public class TileSource extends TileSourceBase {
	private TileProviderBase mTileProvider;
	private TileURLGeneratorBase mTileURLGenerator;
	private TileSourceBase mTileSourceOverlay;
	
	public TileSource(Context ctx, String aId) throws SQLiteException, RException {
		this(ctx, aId, true, true);
	}
	
	public TileSource(Context ctx, String aId, boolean aShowOverlay) throws SQLiteException, RException {
		this(ctx, aId, aShowOverlay, true);
	}
	
	public TileSource(Context ctx, String aId, boolean aShowOverlay, boolean aNeedTileProvider) throws SQLiteException, RException {
		super(ctx, aId);
		
		if(MAP_TYPE == MIXMAP_PAIR) {
			final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);

			final MapTileMemCache tileCache = new MapTileMemCache();
			mTileURLGenerator = initTileURLGenerator(this, pref);
			final TileProviderBase provider = initTileProvider(ctx, this, mTileURLGenerator, null);
			
			if(aShowOverlay) {
				mTileSourceOverlay = new TileSourceBase(ctx, OVERLAYID);
				final TileURLGeneratorBase layerURLGenerator = initTileURLGenerator(mTileSourceOverlay, pref);
				final TileProviderBase layerProvider = initTileProvider(ctx, mTileSourceOverlay, layerURLGenerator, null);
				
				mTileProvider = new TileProviderDual(ctx, this.ID, provider, layerProvider, tileCache);
			} else {
				mTileProvider = aNeedTileProvider ? initTileProvider(ctx, this, mTileURLGenerator, null) : null;
			}

		} else {
			final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
			mTileURLGenerator = initTileURLGenerator(this, pref);
			mTileProvider = aNeedTileProvider ? initTileProvider(ctx, this, mTileURLGenerator, null) : null;
			mTileSourceOverlay = null;
		}
		
	}
	
	public TileSource(Context ctx, String aId, String aLayerId) throws SQLiteException, RException {
		super(ctx, aId);
		
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);

		final MapTileMemCache tileCache = new MapTileMemCache();
		mTileURLGenerator = initTileURLGenerator(this, pref);
		final TileProviderBase provider = initTileProvider(ctx, this, mTileURLGenerator, null);
		
		mTileSourceOverlay = new TileSourceBase(ctx, aLayerId);
		final TileURLGeneratorBase layerURLGenerator = initTileURLGenerator(mTileSourceOverlay, pref);
		final TileProviderBase layerProvider = initTileProvider(ctx, mTileSourceOverlay, layerURLGenerator, null);
		
		mTileProvider = new TileProviderDual(ctx, this.ID, provider, layerProvider, tileCache);
		
	}
	
	public TileSourceBase getTileSourceOverlay() {
		return mTileSourceOverlay;
	}
	
	public String getOverlayName() {
		return mTileSourceOverlay == null ? "" : mTileSourceOverlay.ID;
	}
	
	private TileProviderBase initTileProvider(Context ctx, TileSourceBase tileSource, TileURLGeneratorBase aTileURLGenerator, MapTileMemCache aTileCache) throws SQLiteException, RException {
		TileProviderBase provider = null;
		
		switch(tileSource.TILE_SOURCE_TYPE) {
		case 0:
			if(tileSource.LAYER)
				provider = new TileProviderInet(ctx, aTileURLGenerator, CacheDatabaseName(tileSource), aTileCache, null);
			else
				provider = new TileProviderInet(ctx, aTileURLGenerator, CacheDatabaseName(tileSource), aTileCache);
			break;
		case 3:
			provider = new TileProviderMNM(ctx, tileSource.BASEURL, tileSource.ID, aTileCache);
			provider.updateMapParams(this);
			break;
		case 4:
			provider = new TileProviderTAR(ctx, tileSource.BASEURL, tileSource.ID, aTileCache);
			provider.updateMapParams(this);
			break;
		case 5:
			provider = new TileProviderSQLITEDB(ctx, tileSource.BASEURL, tileSource.ID, aTileCache);
			provider.updateMapParams(this);
			break;
		case 6:
			provider = new TileProviderBase(ctx);
			provider.setLoadingMapTile(BitmapFactory.decodeResource(ctx.getResources(), R.drawable.r_blank));
			break;
		default:
			provider = new TileProviderBase(ctx);
		}
			
		return provider;
	}
	
	private TileURLGeneratorBase initTileURLGenerator(TileSourceBase tileSource, SharedPreferences pref) {
		TileURLGeneratorBase generator = null;
		
		if(tileSource.TILE_SOURCE_TYPE == 0) {
			switch(tileSource.URL_BUILDER_TYPE) {
			case 0:
				generator = new TileURLGeneratorOSM(tileSource.BASEURL, tileSource.IMAGE_FILENAMEENDING);
				break;
			case 1:
				generator = new TileURLGeneratorGOOGLEMAP(tileSource.BASEURL, tileSource.GOOGLE_LANG_CODE, pref.getString(MainPreferences.PREF_PREDEFMAPS_ + tileSource.ID + "_googlescale", "1"));
				break;
			case 2:
				generator = new TileURLGeneratorYANDEX(tileSource.BASEURL, tileSource.IMAGE_FILENAMEENDING);
				break;
			case 3:
				generator = new TileURLGeneratorYANDEXTRAFFIC(tileSource.BASEURL);
				break;
			case 4:
				generator = new TileURLGeneratorGOOGLESAT(tileSource.BASEURL, tileSource.GOOGLE_LANG_CODE);
				break;
			case 5:
				generator = new TileURLGeneratorOrdnanceSurveyMap(tileSource.BASEURL, tileSource.ZOOM_MINLEVEL);
				break;
			case 6:
				generator = new TileURLGeneratorMS(tileSource.BASEURL, tileSource.IMAGE_FILENAMEENDING);
				break;
			case 7:
				generator = new TileURLGeneratorDOCELUPL(tileSource.BASEURL);
				break;
			case 8:
				generator = new TileURLGeneratorVFR(tileSource.BASEURL);
				break;
			case 9:
				generator = new TileURLGeneratorAVC(tileSource.BASEURL, tileSource.IMAGE_FILENAMEENDING);
				break;
			case 10:
				generator = new TileURLGeneratorSovMilMap(tileSource.BASEURL);
				break;
			case 11:
				generator = new TileURLGeneratorVFRCB(tileSource.BASEURL, tileSource.IMAGE_FILENAMEENDING);
				break;
			case 12:
				generator = new TileURLGeneratorCustom(tileSource.BASEURL);
				break;
			case 13:
				generator = null;
				break;
			}
		}
		
		return generator;
	}
	
	private String CacheDatabaseName(TileSourceBase aTileSource) {
		if(!aTileSource.mOnlineMapCacheEnabled && !aTileSource.LAYER) // Cache Enabled?
			return null;
		if(aTileSource.CACHE.trim().equalsIgnoreCase(""))
			return aTileSource.ID;
		else
			return aTileSource.CACHE;
	}

	public Bitmap getTile(final int x, final int y, final int z) {
		return mTileProvider.getTile(x, y, z);
	}

	public void Free() {
		if(mTileProvider != null) mTileProvider.Free();
	}

	protected void finalize() throws Throwable {
		Ut.d("TileSource finalize");
		super.finalize();
	}

	public int getZOOM_MINLEVEL() {
		return ZOOM_MINLEVEL;
	}

	public int getZOOM_MAXLEVEL() {
		return ZOOM_MAXLEVEL;
	}

	public int getTileSizePx(int mZoom) {
		return MAPTILE_SIZEPX;
	}
	
	public int getTileUpperBound(final int zoomLevel) {
//		if (this.URL_BUILDER_TYPE == 5) {
//			return OpenSpaceUpperBoundArray[zoomLevel - ZOOM_MINLEVEL];
//		} else
			return (int) Math.pow(2, zoomLevel);
	}

	public void setHandler(Handler mTileMapHandler) {
		mTileProvider.setHandler(mTileMapHandler);
		
	}
	
	public TileProviderBase getTileProvider() {
		return mTileProvider;
	}
	
	public TileURLGeneratorBase getTileURLGenerator () {
		return mTileURLGenerator;
	}

	public void postIndex() {
		mTileProvider.updateMapParams(this);
	}

}
