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

import com.robert.maps.R;
import com.robert.maps.kml.XMLparser.PredefMapsParser;
import com.robert.maps.utils.RException;
import com.robert.maps.utils.Ut;

public class TileSource {
	private TileProviderBase mTileProvider;
	
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
	public boolean LAYER, mOnlineMapCacheEnabled;

	public TileSource(Context ctx, String aId) throws SQLiteException, RException {
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
			this.MAPTILE_SIZEPX = 256;
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
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		
		switch(TILE_SOURCE_TYPE) {
		case 0:
			TileURLGeneratorBase gen = null;
			switch(URL_BUILDER_TYPE) {
			case 0:
				gen = new TileURLGeneratorOSM(BASEURL, IMAGE_FILENAMEENDING);
				break;
			case 1:
				gen = new TileURLGeneratorGOOGLEMAP(BASEURL, GOOGLE_LANG_CODE);
				break;
			case 2:
				gen = new TileURLGeneratorYANDEX(BASEURL, IMAGE_FILENAMEENDING);
				break;
			case 3:
				gen = new TileURLGeneratorYANDEXTRAFFIC(BASEURL);
				break;
			case 4:
				gen = new TileURLGeneratorGOOGLESAT(BASEURL, GOOGLE_LANG_CODE);
				break;
			case 5:
				gen = new TileURLGeneratorOrdnanceSurveyMap(BASEURL, ZOOM_MINLEVEL);
				break;
			case 6:
				gen = new TileURLGeneratorMS(BASEURL, IMAGE_FILENAMEENDING);
				break;
			case 7:
				gen = new TileURLGeneratorDOCELUPL(BASEURL);
				break;
			case 8:
				gen = new TileURLGeneratorVFR(BASEURL);
				break;
			case 9:
				gen = new TileURLGeneratorAVC(BASEURL, IMAGE_FILENAMEENDING);
				break;
			case 10:
				gen = new TileURLGeneratorSovMilMap(BASEURL);
				break;
			case 11:
				gen = new TileURLGeneratorVFRCB(BASEURL, IMAGE_FILENAMEENDING);
				break;
			}
			if(LAYER)
				mTileProvider = new TileProviderInet(ctx, gen, CacheDatabaseName(), null);
			else
				mTileProvider = new TileProviderInet(ctx, gen, CacheDatabaseName());
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

	public void postIndex() {
		mTileProvider.updateMapParams(this);
	}

}
