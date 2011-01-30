package com.robert.maps.kml.XMLparser;

import org.andnav.osm.views.util.OpenStreetMapRendererInfo;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.robert.maps.utils.Ut;

public class PredefMapsParser extends DefaultHandler {
	private final OpenStreetMapRendererInfo mRendererInfo;
	private final String mMapId;

	private static final String MAP = "map";
	private static final String ID = "id";
	private static final String NAME = "name";
	//private static final String DESCR = "descr";
	private static final String BASEURL = "baseurl";
	private static final String IMAGE_FILENAMEENDING = "IMAGE_FILENAMEENDING";
	private static final String ZOOM_MINLEVEL = "ZOOM_MINLEVEL";
	private static final String ZOOM_MAXLEVEL = "ZOOM_MAXLEVEL";
	private static final String MAPTILE_SIZEPX = "MAPTILE_SIZEPX";
	private static final String URL_BUILDER_TYPE = "URL_BUILDER_TYPE";
	private static final String TILE_SOURCE_TYPE = "TILE_SOURCE_TYPE";
	private static final String PROJECTION = "PROJECTION";
	private static final String YANDEX_TRAFFIC_ON = "YANDEX_TRAFFIC_ON";

	public PredefMapsParser(final OpenStreetMapRendererInfo aRendererInfo, final String aMapId) {
		super();
		mRendererInfo = aRendererInfo;
		mMapId = aMapId;
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		Ut.dd(localName);
		if(localName.equalsIgnoreCase(MAP))
			if(attributes.getValue(ID).equalsIgnoreCase(mMapId)){
				mRendererInfo.ID = attributes.getValue(ID);
				mRendererInfo.NAME = attributes.getValue(NAME);
				mRendererInfo.BASEURL = attributes.getValue(BASEURL);
				mRendererInfo.ZOOM_MINLEVEL = Integer.parseInt(attributes.getValue(ZOOM_MINLEVEL));
				mRendererInfo.ZOOM_MAXLEVEL = Integer.parseInt(attributes.getValue(ZOOM_MAXLEVEL));
				mRendererInfo.IMAGE_FILENAMEENDING = attributes.getValue(IMAGE_FILENAMEENDING);
				mRendererInfo.MAPTILE_SIZEPX = Integer.parseInt(attributes.getValue(MAPTILE_SIZEPX));
				mRendererInfo.URL_BUILDER_TYPE = Integer.parseInt(attributes.getValue(URL_BUILDER_TYPE));
				mRendererInfo.TILE_SOURCE_TYPE = Integer.parseInt(attributes.getValue(TILE_SOURCE_TYPE));
				mRendererInfo.PROJECTION = Integer.parseInt(attributes.getValue(PROJECTION));
				mRendererInfo.YANDEX_TRAFFIC_ON = Integer.parseInt(attributes.getValue(YANDEX_TRAFFIC_ON));
			}
		super.startElement(uri, localName, name, attributes);
	}

}
