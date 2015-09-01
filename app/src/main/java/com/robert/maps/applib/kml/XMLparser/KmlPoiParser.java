package com.robert.maps.applib.kml.XMLparser;

import android.database.Cursor;

import com.robert.maps.applib.kml.PoiManager;
import com.robert.maps.applib.kml.PoiPoint;
import com.robert.maps.applib.kml.constants.PoiConstants;

import org.andnav.osm.util.GeoPoint;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;

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

	private HashMap<String, Integer> mCategoryMap;

	public KmlPoiParser(PoiManager poiManager, int CategoryId) {
		super();
		builder = new StringBuilder();
		mPoiManager = poiManager;
		mCategoryId = CategoryId;
		mPoiPoint = new PoiPoint();
		mItIsPoint = false;
		
		mCategoryMap = new HashMap<String, Integer>();
		Cursor c = mPoiManager.getGeoDatabase().getPoiCategoryListCursor();
		if(c != null) {
			if(c.moveToFirst()) {
				do {
					mCategoryMap.put(c.getString(0), c.getInt(2));
				} while(c.moveToNext());
			}
			c.close();
		}
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
		} else if(localName.equalsIgnoreCase("categoryid") && mPoiPoint != null) {
			final String attrName = attributes.getValue(PoiConstants.NAME);
			if(mCategoryMap.containsKey(attrName)) {
				mPoiPoint.CategoryId = mCategoryMap.get(attrName);
			} else {
				mPoiPoint.CategoryId = (int) mPoiManager.getGeoDatabase().addPoiCategory(attrName, 0, Integer.parseInt(attributes.getValue(PoiConstants.ICONID)));
				mCategoryMap.put(attrName, mPoiPoint.CategoryId);
			}
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
		} else if(localName.equalsIgnoreCase(NAME)) {
			if(mPoiPoint != null)
				mPoiPoint.Title = builder.toString().trim();
		} else if(localName.equalsIgnoreCase(description)) {
			if(mPoiPoint != null)
				mPoiPoint.Descr = builder.toString().trim();
		} else if(localName.equalsIgnoreCase(coordinates)) {
			mStrArray = builder.toString().split(",");
			if(mPoiPoint != null)
				mPoiPoint.GeoPoint = GeoPoint.from2DoubleString(mStrArray[1], mStrArray[0]);
		} else if(localName.equalsIgnoreCase(Point)) {
			mItIsPoint = true;
		}
		super.endElement(uri, localName, name);
	}

}
