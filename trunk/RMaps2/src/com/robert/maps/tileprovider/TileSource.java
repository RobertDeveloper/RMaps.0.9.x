package com.robert.maps.tileprovider;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.robert.maps.MainPreferences;
import com.robert.maps.R;
import com.robert.maps.kml.XMLparser.PredefMapsParser;
import com.robert.maps.utils.RException;
import com.robert.maps.utils.Ut;

public class TileSource {
	private TileProviderBase mTileProvider;
	private TileURLGeneratorBase mTileURLGenerator;
	
	private static final String EMPTY = "";
	public static final String MAPNIK = "mapnik";
	private static final String EN = "en";
	private static final String PREF_ONLINECACHE = "pref_onlinecache";
	private static final String PREF_GOOGLELANG = "pref_googlelanguagecode";
	private static final String USERMAP_ = "usermap_";
	private static final String PREF_USERMAP_ = "pref_usermaps_";
	private static final String NAME_ = "_name";
	private static final String BASEURL_ = "_baseurl";
	private static final String NO_BASEURL = "no_baseurl";
	private static final String SQLITEDB = "sqlitedb";
	private static final String MNM = "mnm";
	private static final String PROJECTION_ = "_projection";
	private static final String TRAFFIC_ = "_traffic";
	
	public final static int PREDEF_ONLINE = 0;
	public final static int USERMAP_OFFLINE = 1;
	
	public String ID, BASEURL, NAME, IMAGE_FILENAMEENDING, GOOGLE_LANG_CODE, CACHE;
	public int MAPTILE_SIZEPX, ZOOM_MINLEVEL, ZOOM_MAXLEVEL,
	URL_BUILDER_TYPE, // 0 - OSM, 1 - Google, 2 - Yandex, 3 - Yandex.Traffic, 4 - Google.Sattelite, 5 - openspace, 6 - microsoft, 8 - VFR Chart
	TILE_SOURCE_TYPE, // 0 - internet, 3 - MapNav file, 4 - TAR, 5 - sqlitedb
	YANDEX_TRAFFIC_ON,
	MAP_TYPE,
	PROJECTION; // 1-меркатор на сфероид, 2- на эллипсоид, 3- OSGB 36 British national grid reference system
	public boolean LAYER, mOnlineMapCacheEnabled, GOOGLESCALE;
	public double MAPTILE_SIZE_FACTOR = 1;

	public TileSource(Context ctx, String aId) throws SQLiteException, RException {
		this(ctx, aId, true);
	}
	
	public TileSource(Context ctx, String aId, boolean aNeedTileProvider) throws SQLiteException, RException {
		if (aId.equalsIgnoreCase(EMPTY))
			aId = MAPNIK;

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		mOnlineMapCacheEnabled = pref.getBoolean(PREF_ONLINECACHE, true);
		GOOGLE_LANG_CODE = pref.getString(PREF_GOOGLELANG, EN);

		if (aId.contains(USERMAP_)) {
			MAP_TYPE = USERMAP_OFFLINE;
			String prefix = PREF_USERMAP_ + aId.substring(8);
			this.ID = aId;
			this.NAME = pref.getString(prefix + NAME_, aId);
			this.BASEURL = pref.getString(prefix + BASEURL_, NO_BASEURL);
			this.ZOOM_MINLEVEL = 0;
			this.ZOOM_MAXLEVEL = 24;
			this.MAPTILE_SIZEPX = (int) (256 * MAPTILE_SIZE_FACTOR);
			this.URL_BUILDER_TYPE = 0;
			if (aId.toLowerCase().endsWith(SQLITEDB)) {
				this.TILE_SOURCE_TYPE = 5;
				this.IMAGE_FILENAMEENDING = EMPTY;
			}
			else if (aId.toLowerCase().endsWith(MNM)) {
				this.TILE_SOURCE_TYPE = 3;
				this.IMAGE_FILENAMEENDING = EMPTY;
			} else {
				this.TILE_SOURCE_TYPE = 4;
				this.IMAGE_FILENAMEENDING = EMPTY;
			}
			this.PROJECTION = Integer.parseInt(pref.getString(prefix + PROJECTION_, "1"));
			if (pref.getBoolean(prefix + TRAFFIC_, false))
				this.YANDEX_TRAFFIC_ON = 1;
			else
				this.YANDEX_TRAFFIC_ON = 0;
		} else {
			MAP_TYPE = PREDEF_ONLINE;
			final SAXParserFactory fac = SAXParserFactory.newInstance();
			SAXParser parser = null;
			try {
				parser = fac.newSAXParser();
				if(parser != null){
					final InputStream in = ctx.getResources().openRawResource(R.raw.predefmaps);
					parser.parse(in, new PredefMapsParser(this, aId));
					this.MAPTILE_SIZEPX = (int) (this.MAPTILE_SIZEPX * MAPTILE_SIZE_FACTOR);
					if(this.GOOGLESCALE) {
						final double GOOGLESCALE_SIZE_FACTOR = Double.parseDouble(pref.getString(MainPreferences.PREF_PREDEFMAPS_ + this.ID + "_googlescale", "1"));
						this.MAPTILE_SIZEPX = (int) (this.MAPTILE_SIZEPX * GOOGLESCALE_SIZE_FACTOR);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		
		if(TILE_SOURCE_TYPE == 0) {
			switch(URL_BUILDER_TYPE) {
			case 0:
				mTileURLGenerator = new TileURLGeneratorOSM(BASEURL, IMAGE_FILENAMEENDING);
				break;
			case 1:
				mTileURLGenerator = new TileURLGeneratorGOOGLEMAP(BASEURL, GOOGLE_LANG_CODE, pref.getString(MainPreferences.PREF_PREDEFMAPS_ + this.ID + "_googlescale", "1"));
				break;
			case 2:
				mTileURLGenerator = new TileURLGeneratorYANDEX(BASEURL, IMAGE_FILENAMEENDING);
				break;
			case 3:
				mTileURLGenerator = new TileURLGeneratorYANDEXTRAFFIC(BASEURL);
				break;
			case 4:
				mTileURLGenerator = new TileURLGeneratorGOOGLESAT(BASEURL, GOOGLE_LANG_CODE);
				break;
			case 5:
				mTileURLGenerator = new TileURLGeneratorOrdnanceSurveyMap(BASEURL, ZOOM_MINLEVEL);
				break;
			case 6:
				mTileURLGenerator = new TileURLGeneratorMS(BASEURL, IMAGE_FILENAMEENDING);
				break;
			case 7:
				mTileURLGenerator = new TileURLGeneratorDOCELUPL(BASEURL);
				break;
			case 8:
				mTileURLGenerator = new TileURLGeneratorVFR(BASEURL);
				break;
			case 9:
				mTileURLGenerator = new TileURLGeneratorAVC(BASEURL, IMAGE_FILENAMEENDING);
				break;
			case 10:
				mTileURLGenerator = new TileURLGeneratorSovMilMap(BASEURL);
				break;
			case 11:
				mTileURLGenerator = new TileURLGeneratorVFRCB(BASEURL, IMAGE_FILENAMEENDING);
				break;
			}
		}
		
		if(aNeedTileProvider) {
			switch(TILE_SOURCE_TYPE) {
			case 0:
				if(LAYER)
					mTileProvider = new TileProviderInet(ctx, mTileURLGenerator, CacheDatabaseName(), null);
				else
					mTileProvider = new TileProviderInet(ctx, mTileURLGenerator, CacheDatabaseName());
				break;
			case 3:
				mTileProvider = new TileProviderMNM(ctx, BASEURL, ID);
				mTileProvider.updateMapParams(this);
				break;
			case 4:
				mTileProvider = new TileProviderTAR(ctx, BASEURL, ID);
				mTileProvider.updateMapParams(this);
				break;
			case 5:
				mTileProvider = new TileProviderSQLITEDB(ctx, BASEURL, ID);
				mTileProvider.updateMapParams(this);
				break;
			default:
				mTileProvider = new TileProviderBase(ctx);
			}
			
		}
		
	}
	
	public boolean CacheEnabled() {
		return mOnlineMapCacheEnabled && !LAYER;
	}

	public String CacheDatabaseName() {
		if(!CacheEnabled())
			return null;
		if(CACHE.trim().equalsIgnoreCase(""))
			return ID;
		else
			return CACHE;
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
