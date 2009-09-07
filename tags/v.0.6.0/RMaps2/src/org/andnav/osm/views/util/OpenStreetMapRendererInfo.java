// Created by plusminus on 18:23:16 - 25.09.2008
package org.andnav.osm.views.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.robert.maps.R;

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
	public String ID, BASEURL, NAME, IMAGE_FILENAMEENDING;
	public int ZOOM_MINLEVEL, ZOOM_MAXLEVEL, MAPTILE_SIZEPX,
	URL_BUILDER_TYPE, // 0 - OSM, 1 - Google, 2 - Yandex
	TILE_SOURCE_TYPE, // 0 - internet, 1 - AndNav ZIP file, 2 - SASGIS ZIP file, 3 - MapNav file, 4 - TAR
	YANDEX_TRAFFIC_ON,
	PROJECTION; // 1-меркатор на сфероид, 2- на эллипсоид

	// ===========================================================
	// Constructors
	// ===========================================================

	public void set(Context aCtx){

	}

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
	}

	public void LoadFromResources(String aId, SharedPreferences pref) {
		if (aId.equalsIgnoreCase(""))
			aId = "mapnik";

		if (aId.contains("usermap_")) {
			String prefix = "pref_usermaps_" + aId.substring(8);
			this.ID = aId;
			this.NAME = pref.getString(prefix + "_name", aId);
			this.BASEURL = pref.getString(prefix + "_baseurl", "no_baseurl");
			this.ZOOM_MINLEVEL = 0;
			this.ZOOM_MAXLEVEL = 24;
			this.MAPTILE_SIZEPX = 256;
			this.URL_BUILDER_TYPE = 0;
			if (aId.toLowerCase().endsWith("mnm")) {
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
			InputStream in = mResources.openRawResource(R.raw.predefmaps);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = null;
			try {
				db = dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e1) {
				e1.printStackTrace();
			}
			Document doc = null;
			try {
				doc = db.parse(in);
				Element el = doc.getElementById(aId);
				if(el == null)
					el = doc.getElementById("mapnik");

				this.ID = el.getAttribute("id");
				this.NAME = el.getAttribute("name");
				this.BASEURL = el.getAttribute("baseurl");
				this.ZOOM_MINLEVEL = 0; // Integer.parseInt(el.getAttribute("ZOOM_MINLEVEL"));
				this.ZOOM_MAXLEVEL = Integer.parseInt(el.getAttribute("ZOOM_MAXLEVEL"));
				this.IMAGE_FILENAMEENDING = el.getAttribute("IMAGE_FILENAMEENDING");
				this.MAPTILE_SIZEPX = Integer.parseInt(el.getAttribute("MAPTILE_SIZEPX"));
				this.URL_BUILDER_TYPE = Integer.parseInt(el.getAttribute("URL_BUILDER_TYPE"));
				this.TILE_SOURCE_TYPE = Integer.parseInt(el.getAttribute("TILE_SOURCE_TYPE"));
				this.PROJECTION = Integer.parseInt(el.getAttribute("PROJECTION"));
				this.YANDEX_TRAFFIC_ON = Integer.parseInt(el.getAttribute("YANDEX_TRAFFIC_ON"));
			} catch (SAXException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
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
				in = new BufferedInputStream(new URL("http://trf.maps.yandex.net/trf/stat.js").openStream(), StreamUtils.IO_BUFFER_SIZE);

				final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
				out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
				StreamUtils.copy(in, out);
				out.flush();

				String str = dataStream.toString();
				int start = str.indexOf("timestamp: \"")+12;
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

	public String getTileURLString(final int[] tileID, final int zoomLevel){
		final String strGalileo = new String("Galileo");
		switch(this.TILE_SOURCE_TYPE){
		case 0: // 0 - internet
			switch(this.URL_BUILDER_TYPE){
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
					.append("&hl=ru&x=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
					.append("&y=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])
					.append("&zoom=")
					.append(18-zoomLevel-1)
					.append("&s=")
					.append(strGalileo.substring(0, (tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX]*3+tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])% 8))
					.toString();
					// ResultURL:=GetUrlBase+'&x='+inttostr(GetX)+'&y='+inttostr(GetY)+'&zoom='+inttostr(18-GetZ)+'&s='+copy('Galileo',1,(GetX*3+GetY)mod 8);
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

}
