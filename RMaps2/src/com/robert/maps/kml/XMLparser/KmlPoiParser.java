package com.robert.maps.kml.XMLparser;

import org.andnav.osm.util.GeoPoint;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.PoiPoint;

public class KmlPoiParser extends DefaultHandler {
	private StringBuilder builder;
	private PoiManager mPoiManager;
	private PoiPoint mPoiPoint;
	private int mCategoryId;
	private String [] mStrArray;
	private boolean mItIsPoint;
	
	private static final String Placemark = "Placemark";
	private static final String Point = "Point";
	private static final String NAME = "name";
	private static final String coordinates = "coordinates";
	private static final String description = "description";



	public KmlPoiParser(PoiManager poiManager, int CategoryId) {
		super();
		builder = new StringBuilder();
		mPoiManager = poiManager;
		mCategoryId = CategoryId;
		mPoiPoint = new PoiPoint();
		mItIsPoint = false;
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
		if(localName.equalsIgnoreCase(Placemark)){
			mPoiPoint = new PoiPoint();
			mPoiPoint.CategoryId = mCategoryId;
			mItIsPoint = false;
		}
		super.startElement(uri, localName, name, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		if(localName.equalsIgnoreCase(Placemark)){
			if(mItIsPoint){
				if(mPoiPoint.Title.equalsIgnoreCase("")) mPoiPoint.Title = "POI";
				mPoiManager.updatePoi(mPoiPoint);
			}
		}
		else if(localName.equalsIgnoreCase(NAME))
			mPoiPoint.Title = builder.toString().trim();
		else if(localName.equalsIgnoreCase(description))
			mPoiPoint.Descr = builder.toString().trim();
		else if(localName.equalsIgnoreCase(coordinates)){
			mStrArray = builder.toString().split(",");
			mPoiPoint.GeoPoint = GeoPoint.from2DoubleString(mStrArray[1], mStrArray[0]);
		}
		else if(localName.equalsIgnoreCase(Point))
			mItIsPoint = true;
		super.endElement(uri, localName, name);
	}

}
