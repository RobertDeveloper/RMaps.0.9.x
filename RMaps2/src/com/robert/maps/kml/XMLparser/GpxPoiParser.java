package com.robert.maps.kml.XMLparser;

import org.andnav.osm.util.GeoPoint;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.PoiPoint;

public class GpxPoiParser extends DefaultHandler {
	private StringBuilder builder;
	private PoiManager mPoiManager;
	private PoiPoint mPoiPoint;
	private int mCategoryId;
	
	private static final String WPT = "wpt";
	private static final String LAT = "lat";
	private static final String LON = "lon";
	private static final String NAME = "name";
	private static final String CMT = "cmt";
	private static final String DESC = "desc";



	public GpxPoiParser(PoiManager poiManager, int CategoryId) {
		super();
		builder = new StringBuilder();
		mPoiManager = poiManager;
		mCategoryId = CategoryId;
		mPoiPoint = new PoiPoint();
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		builder. append(ch, start, length);
		super.characters(ch, start, length);
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes)
			throws SAXException {
		builder.delete(0, builder.length());
		if(localName.equalsIgnoreCase(WPT)){
			mPoiPoint = new PoiPoint();
			mPoiPoint.CategoryId = mCategoryId;
			mPoiPoint.GeoPoint = GeoPoint.from2DoubleString(attributes.getValue(LAT), attributes.getValue(LON));
		}
		super.startElement(uri, localName, name, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		if(localName.equalsIgnoreCase(WPT)){
			if(mPoiPoint.Title.equalsIgnoreCase("")) mPoiPoint.Title = "POI";
			mPoiManager.updatePoi(mPoiPoint);
		}
		else if(localName.equalsIgnoreCase(NAME))
			mPoiPoint.Title = builder.toString().trim();
		else if(localName.equalsIgnoreCase(CMT))
			mPoiPoint.Descr = builder.toString().trim();
		else if(localName.equalsIgnoreCase(DESC))
			if(mPoiPoint.Descr.equals(""))
				mPoiPoint.Descr = builder.toString().trim();
		super.endElement(uri, localName, name);
	}

}
