package com.robert.maps.tileprovider;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;

import com.robert.maps.MainPreferences;
import com.robert.maps.R;
import com.robert.maps.kml.XMLparser.PredefMapsParser;
import com.robert.maps.utils.RException;

public class TileSourceBase {

	protected static final String EMPTY = "";
	public static final String MAPNIK = "mapnik";
	protected static final String EN = "en";
	protected static final String PREF_ONLINECACHE = "pref_onlinecache";
	protected static final String PREF_GOOGLELANG = "pref_googlelanguagecode";
	protected static final String USERMAP_ = "usermap_";
	protected static final String PREF_USERMAP_ = "pref_usermaps_";
	protected static final String NAME_ = "_name";
	protected static final String BASEURL_ = "_baseurl";
	protected static final String NO_BASEURL = "no_baseurl";
	protected static final String SQLITEDB = "sqlitedb";
	protected static final String MNM = "mnm";
	protected static final String PROJECTION_ = "_projection";
	protected static final String TRAFFIC_ = "_traffic";
	
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

	public TileSourceBase(Context ctx, String aId, boolean aNeedTileProvider) throws SQLiteException, RException {
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
	}
}
