// Created by plusminus on 18:23:16 - 25.09.2008
package org.andnav.osm.views.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.content.SharedPreferences;
import android.content.res.Resources;

import com.robert.maps.R;
import com.robert.maps.kml.XMLparser.PredefMapsParser;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class OpenStreetMapRendererInfo {
	// ===========================================================
	// Fields
	// ===========================================================

	private final Resources mResources;
	public int MAPTILE_SIZEPX;
	private static final int OpenSpaceUpperBoundArray[] = { 2, 5, 10, 25 , 50, 100, 200, 500, 1000, 2000, 4000};
	private static final int OpenSpaceLayersArray[] = {2500, 1000, 500, 200, 100, 50, 25, 10, 5, 2, 1};

	public String ID, BASEURL, NAME, IMAGE_FILENAMEENDING, GOOGLE_LANG_CODE, CACHE;
	public int ZOOM_MINLEVEL, ZOOM_MAXLEVEL,
	URL_BUILDER_TYPE, // 0 - OSM, 1 - Google, 2 - Yandex, 3 - Yandex.Traffic, 4 - Google.Sattelite, 5 - openspace, 6 - microsoft, 8 - VFR Chart
	TILE_SOURCE_TYPE, // 0 - internet, 3 - MapNav file, 4 - TAR, 5 - sqlitedb
	YANDEX_TRAFFIC_ON,
	PROJECTION; // 1-меркатор на сфероид, 2- на эллипсоид, 3- OSGB 36 British national grid reference system
	public boolean LAYER;
	private boolean mOnlineMapCacheEnabled;

	// ===========================================================
	// Constructors
	// ===========================================================

	public OpenStreetMapRendererInfo(Resources aRes, String aId){
		mResources = aRes;
		mLastUpdateTime = 0L;
		mTimeStamp = "";
		this.ID = "";
		this.NAME = "";
		this.BASEURL = "";
		this.ZOOM_MINLEVEL = 0;
		this.ZOOM_MAXLEVEL = 0;
		this.IMAGE_FILENAMEENDING = "";
		this.MAPTILE_SIZEPX = 256;
		this.URL_BUILDER_TYPE = 1;
		this.TILE_SOURCE_TYPE = 0;
		this.PROJECTION = 1;
		this.YANDEX_TRAFFIC_ON = 0;
		this.GOOGLE_LANG_CODE = "";
		this.LAYER = false;
		this.CACHE = "";
	}

	public void LoadFromResources(String aId, SharedPreferences pref) {
		if (aId.equalsIgnoreCase(""))
			aId = "mapnik";

		if(pref != null)
			mOnlineMapCacheEnabled = pref.getBoolean("pref_onlinecache", true);
		else
			mOnlineMapCacheEnabled = false;

		if(pref != null)
			this.GOOGLE_LANG_CODE = pref.getString("pref_googlelanguagecode", "en");

		if (aId.contains("usermap_")) {
			String prefix = "pref_usermaps_" + aId.substring(8);
			this.ID = aId;
			this.NAME = pref.getString(prefix + "_name", aId);
			this.BASEURL = pref.getString(prefix + "_baseurl", "no_baseurl");
			this.ZOOM_MINLEVEL = 0;
			this.ZOOM_MAXLEVEL = 24;
			this.MAPTILE_SIZEPX = 256;
			this.URL_BUILDER_TYPE = 0;
			if (aId.toLowerCase().endsWith("sqlitedb")) {
				this.TILE_SOURCE_TYPE = 5;
				this.IMAGE_FILENAMEENDING = "";
			}
			else if (aId.toLowerCase().endsWith("mnm")) {
				this.TILE_SOURCE_TYPE = 3;
				this.IMAGE_FILENAMEENDING = "";
			} else {
				this.TILE_SOURCE_TYPE = 4;
				this.IMAGE_FILENAMEENDING = "";
			}
			this.PROJECTION = Integer.parseInt(pref.getString(prefix + "_projection", "1"));
			if (pref.getBoolean(prefix + "_traffic", false))
				this.YANDEX_TRAFFIC_ON = 1;
			else
				this.YANDEX_TRAFFIC_ON = 0;
		} else {
			final SAXParserFactory fac = SAXParserFactory.newInstance();
			SAXParser parser = null;
			try {
				parser = fac.newSAXParser();
				if(parser != null){
					final InputStream in = mResources.openRawResource(R.raw.predefmaps);
					parser.parse(in, new PredefMapsParser(this, aId));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	public boolean CacheEnabled() {
		return mOnlineMapCacheEnabled && !LAYER;
	}

	public String CacheDatabaseName() {
		if(!CacheEnabled())
			return "";
		if(CACHE.trim().equalsIgnoreCase(""))
			return ID;
		else
			return CACHE;
	}

	public String getQRTS(int x, int y, int zoomLevel){
		final char[][] M_TSQR = {{'q','t'},{'r','s'}};
		int i;
		int mask;

		String result = "t";
		mask = 1 << zoomLevel;
		x = x % mask;
		if (x < 0) x += mask;
		for (i = 2; i <= zoomLevel+1; i++){
			mask = mask >> 1;
		    result += M_TSQR[((x & mask) > 0)? 1 : 0][((y & mask) > 0)? 1 : 0];
		}
		return result;
	}

	private Long mLastUpdateTime;
	private String mTimeStamp;

	private boolean ts_update_needed(int delta){
		Date d = new Date();
		Long now = d.getTime();
		if(now - mLastUpdateTime > delta*1000){
			mLastUpdateTime = now;
			return true;
		}
		return false;
	}

	private String get_ts(int delta){

		if (ts_update_needed(delta)) {
			InputStream in = null;
			OutputStream out = null;

			try {
				in = new BufferedInputStream(new URL("http://jgo.maps.yandex.net/trf/stat.js").openStream(), StreamUtils.IO_BUFFER_SIZE);

				final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
				out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
				StreamUtils.copy(in, out);
				out.flush();

				String str = dataStream.toString();
				//JSONObject json = new JSONObject(str.replace("YMaps.TrafficLoader.onLoad(\"stat\",", "").replace("});", "}"));
				int start = str.indexOf("timestamp:");
				start = str.indexOf("\"", start) + 1;
				int end = str.indexOf("\"", start);
				mTimeStamp = str.substring(start, end);

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				StreamUtils.closeStream(in);
				StreamUtils.closeStream(out);
			}
		}

		return mTimeStamp;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public int getTileUpperBound(final int zoomLevel) {
		if (this.URL_BUILDER_TYPE == 5) {
			return OpenSpaceUpperBoundArray[zoomLevel - ZOOM_MINLEVEL];
		} else
			return (int) Math.pow(2, zoomLevel);
	}

	public String getTileURLString(final int[] tileID, final int zoomLevel){
		final String strGalileo = new String("Galileo");
		switch(this.TILE_SOURCE_TYPE){
		case 0: // 0 - internet
			switch(this.URL_BUILDER_TYPE){
				case 7: // docelu.pl
					String sy = String.format("%06x", tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX]);
					String sx = String.format("%06x", tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX]);
					char[] cx = sx.toCharArray();
					char[] cy = sy.toCharArray();
					String szoom = Integer.toHexString(zoomLevel);

					String s = "http://i.wp.pl/m/tiles004/" + szoom + "/" + cx[4] + cy[4] + "/" + cx[3]
							+ cy[3] + "/" + cx[2] + cy[2] + "/" + cx[1] + cy[1] + "/" + cx[0] + cy[0]
							+ "/z" + szoom + "x" + sx + "y" + sy + ".png";
					return s;
				case 6: // Microsoft
					return new StringBuilder().append(this.BASEURL)
					.append(encodeQuadTree(zoomLevel, tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX], tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX]))
					.append(this.IMAGE_FILENAMEENDING)
					.toString();
				case 5: // openspace
					final int million = 1000000 / getTileUpperBound(zoomLevel);
					final int size = OpenSpaceLayersArray[zoomLevel-ZOOM_MINLEVEL] < 5 ? 250 : 200;
					return new StringBuilder()
					.append(this.BASEURL)
					.append("LAYERS=").append(OpenSpaceLayersArray[zoomLevel-ZOOM_MINLEVEL])
					.append("&SRS=EPSG%3A27700&BBOX=")
					.append(million*tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
					.append(",")
					.append(million*(getTileUpperBound(zoomLevel)-1-tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX]))
					.append(",")
					.append(million*(1+tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX]))
					.append(",")
					.append(million*(1+(getTileUpperBound(zoomLevel)-1-tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])))
					.append("&WIDTH=").append(size).append("&HEIGHT=").append(size)
					.toString();
				case 9: // http://www.avcharts.com/
					return new StringBuilder().append(this.BASEURL)
					.append(zoomLevel)
					.append("/")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
					.append("/")
					.append((1<<zoomLevel)-tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX]-1)
					.append(this.IMAGE_FILENAMEENDING)
					.toString();
				case 0: // OSM
					return new StringBuilder().append(this.BASEURL)
					.append(zoomLevel)
					.append("/")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
					.append("/")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])
					.append(this.IMAGE_FILENAMEENDING)
					.toString();
				case 2: // Yandex
					return new StringBuilder().append(this.BASEURL)
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
					.append("&y=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])
					.append("&z=")
					.append(zoomLevel)
					.append(this.IMAGE_FILENAMEENDING)
					.toString();
					// ResultURL:=GetURLBase+inttostr(GetX)+'&y='+inttostr(GetY)+'&z='+inttostr(GetZ-1);
				case 3: // Yandex.Traffic
					return new StringBuilder().append(this.BASEURL)
					.append("&x=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
					.append("&y=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])
					.append("&z=")
					.append(zoomLevel)
					.append("&tm=")
					.append(get_ts(60))
					.toString();
					// ResultURL:=GetURLBase+inttostr(GetX)+'&y='+inttostr(GetY)+'&z='+inttostr(GetZ-1)+'&tm='+inttostr(get_ts(60));
				case 1: // Google.Map
					return new StringBuilder().append(this.BASEURL)
					.append("hl=")
					.append(GOOGLE_LANG_CODE)
					.append("&x=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
					.append("&y=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])
					.append("&zoom=")
					.append(18-zoomLevel-1)
					.append("&s=")
					.append(strGalileo.substring(0, (tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX]*3+tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])% 8))
					.toString();
					// ResultURL:=GetUrlBase+'&x='+inttostr(GetX)+'&y='+inttostr(GetY)+'&zoom='+inttostr(18-GetZ)+'&s='+copy('Galileo',1,(GetX*3+GetY)mod 8);
				case 8: // VFR Chart
					return new StringBuilder().append(this.BASEURL)
					.append("x=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
					.append("&y=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])
					.append("&z=")
					.append(18-zoomLevel-1)
					.toString();
					// http://www.runwayfinder.com/media/charts/?x=0&y=0&z=17
				case 4: // Google.Sattelite
					return new StringBuilder().append(this.BASEURL)
					//http://khm0.google.com/maptilecompress?t=2&q=80&hl=ru
					.append("&q=80&hl=ru&x=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
					.append("&y=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])
					.append("&z=")
					.append(zoomLevel)
					.append("&s=")
					.append(strGalileo.substring(0, (tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX]*3+tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])% 8))
					.toString();
					// ResultURL:=GetUrlBase+'&x='+inttostr(GetX)+'&y='+inttostr(GetY)+'&z='+inttostr(GetZ-1)+'&s='+copy('Galileo',1,(GetX*3+GetY)mod 8);
				default: // OSM
					return new StringBuilder().append(this.BASEURL)
					.append(zoomLevel)
					.append("/")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
					.append("/")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])
					.append(this.IMAGE_FILENAMEENDING)
					.toString();
			}
		case 1: // 1 - AndNav ZIP file
			return new StringBuilder().append(zoomLevel)
			.append("/")
			.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
			.append("/")
			.append(tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])
			.append(this.IMAGE_FILENAMEENDING)
			.toString();
		case 2: // 2 - SASGIS ZIP file
		case 4: // TAR file
			return new StringBuilder()
			.append(0).append(zoomLevel+1).reverse().delete(2, 3).reverse()
			.append("/")
			.append(getQRTS(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX], tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX], zoomLevel))
			.append(this.IMAGE_FILENAMEENDING)
			.toString();
		default:
			return new StringBuilder().append(this.BASEURL)
			.append("/")
			.append(zoomLevel)
			.append("/")
			.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
			.append("/")
			.append(tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])
			.append(this.IMAGE_FILENAMEENDING)
			.toString();
		}
	}

	protected static final char[] NUM_CHAR = { '0', '1', '2', '3' };

	private Object encodeQuadTree(int zoom, int tilex, int tiley) {
		char[] tileNum = new char[zoom];
		for (int i = zoom - 1; i >= 0; i--) {
			// Binary encoding using ones for tilex and twos for tiley. if a bit
			// is set in tilex and tiley we get a three.
			int num = (tilex % 2) | ((tiley % 2) << 1);
			tileNum[i] = NUM_CHAR[num];
			tilex >>= 1;
			tiley >>= 1;
		}
		return new String(tileNum);
	}

	public int getTileSizePx(final int zoomLevel){
		if(this.URL_BUILDER_TYPE == 5)
			return zoomLevel-ZOOM_MINLEVEL >= 9 ? 250 : 200;
		else
			return MAPTILE_SIZEPX;
	}
}
