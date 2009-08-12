// Created by plusminus on 18:23:16 - 25.09.2008
package org.andnav.osm.views.util;

import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

/**
 * 
 * @author Nicolas Gramlich
 *
 */
public enum OpenStreetMapRendererInfo {
	OSMARENDER("http://tah.openstreetmap.org/Tiles/tile/", "OsmaRender", ".png", 17, 256, 0, 0),
	MAPNIK("http://tile.openstreetmap.org/", "Mapnik", ".png", 18, 256, 0, 0),
	CYCLEMAP("http://b.andy.sandbox.cloudmade.com/tiles/cycle/", "Cycle Map", ".png", 17, 256, 0, 0),
	OPENARIELMAP("http://tile.openaerialmap.org/tiles/1.0.0/openaerialmap-900913/", "OpenArialMap (Satellite)", ".jpg", 13, 256, 0, 0),
	CLOUDMADESMALLTILES("http://tile.cloudmade.com/BC9A493B41014CAABB98F0471D759707/2/64/", "Cloudmade (Small tiles)", ".jpg", 13, 64, 0, 0),
	//CLOUDMADESTANDARDTILES("http://tile.cloudmade.com/BC9A493B41014CAABB98F0471D759707/2/256/", "Cloudmade (Standard tiles)", ".jpg", 18, 256);
	CLOUDMADESTANDARDTILES("http://mt.google.com/mt?v=w2.95&hl=ru/", "Cloudmade (Standard tiles)", ".png", 17, 256, 1, 0),
	FILEMAPNIK("/sdcard/rmaps/maps/mapnik.zip", "Mapnik (cash .zip)", ".png.andnav", 18, 256, 0, 1),
	YANDEXMAP("http://vec.maps.yandex.net/tiles?l=map&v=2.6.0&x=", "Yandex.Maps", "", 18, 256, 2, 0);
	
	
	// ===========================================================
	// Fields
	// ===========================================================
	
	public final String BASEURL, NAME, IMAGE_FILENAMEENDING;
	public final int ZOOM_MAXLEVEL, MAPTILE_SIZEPX, 
	URL_BUILDER_TYPE, // 0 - OSM, 1 - Google, 2 - Yandex 
	TILE_SOURCE_TYPE; // 0 - internet, 1 - AndNav ZIP file
	
	// ===========================================================
	// Constructors
	// ===========================================================
	
	private OpenStreetMapRendererInfo(final String aBaseUrl, final String aName, final String aImageFilenameEnding, final int aZoomMax, final int aTileSizePX, final int aURLBuilderType, final int aTileSourceType){
		this.BASEURL = aBaseUrl;
		this.NAME = aName;
		this.ZOOM_MAXLEVEL = aZoomMax;
		this.IMAGE_FILENAMEENDING = aImageFilenameEnding;
		this.MAPTILE_SIZEPX = aTileSizePX;
		this.URL_BUILDER_TYPE = aURLBuilderType;
		this.TILE_SOURCE_TYPE = aTileSourceType;
	}
	
	public static OpenStreetMapRendererInfo getDefault() {
		return MAPNIK;
	}
	
	// ===========================================================
	// Methods
	// ===========================================================
	
	public String getTileURLString(final int[] tileID, final int zoomLevel){
		switch(this.TILE_SOURCE_TYPE){
		case 0:
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
				case 1: // Google
					final String strGalileo = new String("Galileo");
					return new StringBuilder().append(this.BASEURL)
					.append("&x=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
					.append("&y=")
					.append(tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])
					.append("&zoom=")
					.append(18-zoomLevel +1)
					.append("&s=")
					.append(strGalileo.substring(0, (tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX]*3+tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])% 8))
					.toString();
					// ResultURL:=GetUrlBase+'&x='+inttostr(GetX)+'&y='+inttostr(GetY)+'&zoom='+inttostr(18-GetZ)+'&s='+copy('Galileo',1,(GetX*3+GetY)mod 8);
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
		case 1: 
			return new StringBuilder().append(zoomLevel)
			.append("/")
			.append(tileID[OpenStreetMapViewConstants.MAPTILE_LONGITUDE_INDEX])
			.append("/")
			.append(tileID[OpenStreetMapViewConstants.MAPTILE_LATITUDE_INDEX])
			.append(this.IMAGE_FILENAMEENDING)
			.toString();
		default:
			return "";
		}
	}
}
