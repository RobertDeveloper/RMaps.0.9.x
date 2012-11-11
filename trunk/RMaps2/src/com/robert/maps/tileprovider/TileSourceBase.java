package com.robert.maps.tileprovider;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;

import com.robert.maps.MainPreferences;
import com.robert.maps.MixedMapsPreference;
import com.robert.maps.applib.R;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.XMLparser.PredefMapsParser;
import com.robert.maps.utils.RException;

public class TileSourceBase {

	protected static final String EMPTY = "";
	public static final String MAPNIK = "mapnik";
	protected static final String EN = "en";
	protected static final String PREF_ONLINECACHE = "pref_onlinecache";
	protected static final String PREF_GOOGLELANG = "pref_googlelanguagecode";
	protected static final String USERMAP_ = "usermap_";
	public static final String PREF_USERMAP_ = "pref_usermaps_";
	protected static final String NAME_ = "_name";
	protected static final String BASEURL_ = "_baseurl";
	protected static final String NO_BASEURL = "no_baseurl";
	protected static final String SQLITEDB = "sqlitedb";
	protected static final String MNM = "mnm";
	protected static final String PROJECTION_ = "_projection";
	protected static final String TRAFFIC_ = "_traffic";
	protected static final String MIXMAP_ = "mixmap_";
	protected static final String GOOGLESCALE_ = "_googlescale";
	protected static final String STRING_1 = "1";
	protected static final String UNDERLINE = "_";
	
	public final static int PREDEF_ONLINE = 0;
	public final static int USERMAP_OFFLINE = 1;
	public final static int MIXMAP_PAIR = 2;
	public final static int MIXMAP_CUSTOM = 3;
	
	public String ID, BASEURL, NAME, IMAGE_FILENAMEENDING, GOOGLE_LANG_CODE, CACHE, MAPID, OVERLAYID;
	public int MAPTILE_SIZEPX, ZOOM_MINLEVEL, ZOOM_MAXLEVEL,
	URL_BUILDER_TYPE, // 0 - OSM, 1 - Google, 2 - Yandex, 3 - Yandex.Traffic, 4 - Google.Sattelite, 5 - openspace, 6 - microsoft, 8 - VFR Chart
	TILE_SOURCE_TYPE, // 0 - internet, 3 - MapNav file, 4 - TAR, 5 - sqlitedb
	YANDEX_TRAFFIC_ON,
	MAP_TYPE,
	PROJECTION; // 1-меркатор на сфероид, 2- на эллипсоид, 3- OSGB 36 British national grid reference system
	public boolean LAYER, mOnlineMapCacheEnabled, GOOGLESCALE;
	public double MAPTILE_SIZE_FACTOR = 1;

	public TileSourceBase(Context ctx, String aId) throws SQLiteException, RException {
		if (aId.equalsIgnoreCase(EMPTY))
			aId = MAPNIK;
		
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		mOnlineMapCacheEnabled = pref.getBoolean(PREF_ONLINECACHE, true);
		GOOGLE_LANG_CODE = pref.getString(PREF_GOOGLELANG, EN);
		this.OVERLAYID = EMPTY;
		String mixMapName = EMPTY;
		String mixMapId = EMPTY;

		if (aId.startsWith(MIXMAP_)) {
			final String[] params = aId.split(UNDERLINE);
			final PoiManager poiman = new PoiManager(ctx);
			mixMapId = aId;
			aId = MAPNIK;
			MAP_TYPE = PREDEF_ONLINE;
			Cursor c = poiman.getGeoDatabase().getMap(Long.parseLong(params[1]));
			if(c != null) {
				if(c.moveToFirst()) {
					mixMapName = c.getString(1);
					if(c.getInt(2) == 1) { // Pair maps
						final JSONObject json = MixedMapsPreference.getMapPairParams(c.getString(3));
						try {
							aId = json.getString(MixedMapsPreference.MAPID);
							this.OVERLAYID = json.getString(MixedMapsPreference.OVERLAYID);
							this.MAP_TYPE = MIXMAP_PAIR;
						} catch (JSONException e) {
						}
					} else if(c.getInt(2) == 2 || c.getInt(2) == 3) { // Custom source
						final JSONObject json = MixedMapsPreference.getMapCustomParams(c.getString(3));
						aId = mixMapId;
						this.ID = mixMapId;
						this.NAME = c.getString(1);
						this.BASEURL = json.optString(MixedMapsPreference.BASEURL, "");
						this.PROJECTION = json.optInt(MixedMapsPreference.MAPPROJECTION, 1);
						this.LAYER = c.getInt(2) == 2 ? false : true;
						this.MAP_TYPE = MIXMAP_CUSTOM;
						this.URL_BUILDER_TYPE = 12;
						this.ZOOM_MINLEVEL = json.optInt(MixedMapsPreference.MINZOOM, 1)-1;
						this.ZOOM_MAXLEVEL = json.optInt(MixedMapsPreference.MAXZOOM, 20)-1;
						this.MAPTILE_SIZEPX = 256;
						this.CACHE = EMPTY;
						this.mOnlineMapCacheEnabled = json.optBoolean(MixedMapsPreference.ONLINECACHE, true);
						return;
					}
				}
				c.close();
			}
		} else if (aId.contains(USERMAP_)) {
			MAP_TYPE = USERMAP_OFFLINE;
		} else {
			MAP_TYPE = PREDEF_ONLINE;
		}
		
		if (aId.contains(USERMAP_)) {
			String prefix = PREF_USERMAP_ + aId.substring(8);
			this.ID = aId;
			this.MAPID = aId;
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
			this.PROJECTION = Integer.parseInt(pref.getString(prefix + PROJECTION_, STRING_1));
			if (pref.getBoolean(prefix + TRAFFIC_, false))
				this.YANDEX_TRAFFIC_ON = 1;
			else
				this.YANDEX_TRAFFIC_ON = 0;
		} else {
			final SAXParserFactory fac = SAXParserFactory.newInstance();
			SAXParser parser = null;
			try {
				parser = fac.newSAXParser();
				if(parser != null){
					final InputStream in = ctx.getResources().openRawResource(R.raw.predefmaps);
					parser.parse(in, new PredefMapsParser(this, aId));
					this.MAPTILE_SIZEPX = (int) (this.MAPTILE_SIZEPX * MAPTILE_SIZE_FACTOR);
					if(this.GOOGLESCALE) {
						final double GOOGLESCALE_SIZE_FACTOR = Double.parseDouble(pref.getString(MainPreferences.PREF_PREDEFMAPS_ + this.ID + GOOGLESCALE_, STRING_1));
						this.MAPTILE_SIZEPX = (int) (this.MAPTILE_SIZEPX * GOOGLESCALE_SIZE_FACTOR);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		
		if(!mixMapName.equals(EMPTY)) {
			this.NAME = mixMapName;
			this.ID = mixMapId;
		}
	}
}
