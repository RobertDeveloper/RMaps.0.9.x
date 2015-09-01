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

public class GpxPoiParser extends DefaultHandler {
	private StringBuilder builder;
	private PoiManager mPoiManager;
	private PoiPoint mPoiPoint;
	private int mCategoryId;
	private HashMap<String, Integer> mCategoryMap;
	
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
		if(localName.equalsIgnoreCase(WPT)){
			mPoiPoint = new PoiPoint();
			mPoiPoint.CategoryId = mCategoryId;
			mPoiPoint.GeoPoint = GeoPoint.from2DoubleString(attributes.getValue(LAT), attributes.getValue(LON));
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
		if(localName.equalsIgnoreCase(WPT)){
			if(mPoiPoint.Title.equalsIgnoreCase("")) mPoiPoint.Title = "POI";
			mPoiManager.updatePoi(mPoiPoint);
		} else if(localName.equalsIgnoreCase(NAME)) {
			if(mPoiPoint != null)
				mPoiPoint.Title = builder.toString().trim();
		} else if(localName.equalsIgnoreCase(CMT)) {
			if(mPoiPoint != null)
				mPoiPoint.Descr = builder.toString().trim();
		} else if(localName.equalsIgnoreCase(DESC)) {
			if(mPoiPoint != null)
				if(mPoiPoint.Descr.equals(""))
					mPoiPoint.Descr = builder.toString().trim();
		}
		super.endElement(uri, localName, name);
	}

}
