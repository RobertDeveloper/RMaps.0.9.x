package com.robert.maps.applib.preference;

import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.robert.maps.R;
import com.robert.maps.applib.kml.PoiManager;
import com.robert.maps.applib.kml.XMLparser.PredefMapsParser;
import com.robert.maps.applib.kml.constants.PoiConstants;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class MMPreferenceActivity extends PreferenceActivity implements PoiConstants {
	public static final String MAPID = "mapid";
	public static final String MAPPROJECTION = "mapprojection";
	public static final String OVERLAYID = "overlayid";
	public static final String OVERLAYPROJECTION = "overlayprojection";
	public static final String OVERLAYOPAQUE = "overlayopaque";
	public static final String PREF_MIXMAPS_ = "PREF_MIXMAPS_";
	public static final String BASEURL = "baseurl";
	public static final String ISOVERLAY = "isoverlay";
	public static final String ONLINECACHE = "onlinecache";
	public static final String STRETCH = "stretch";

	protected PoiManager mPoiManager;
	protected MapHelper mMapHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPoiManager = new PoiManager(this);
		mMapHelper = new MapHelper();
	}
	
	@Override
	protected void onDestroy() {
		mPoiManager.FreeDatabases();
		super.onDestroy();
	}

	protected class MapHelper {
		public long ID;
		public String NAME;
		public int TYPE;
		public String PARAMS;
		
		public void getMap(long id) {
			ID = 0;
			NAME = "";
			TYPE = 0;
			PARAMS = "";
			
			Cursor c = MMPreferenceActivity.this.mPoiManager.getGeoDatabase().getMap(id);
			if(c != null) {
				if(c.moveToFirst()) {
					ID = id;
					NAME = c.getString(1);
					TYPE = c.getInt(2);
					PARAMS = c.getString(3);
				}
				c.close();
			}
		}
		
		public void updateMap() {
			MMPreferenceActivity.this.mPoiManager.getGeoDatabase().updateMap(ID, NAME, TYPE, PARAMS);
		}
	}

	protected String[][] getMaps(final boolean aGetMaps, final boolean aGetOverlays, final int aProjection) {
		ArrayList<String> arr1 = new ArrayList<String>();
		ArrayList<String> arr2 = new ArrayList<String>();
		
		final SAXParserFactory fac = SAXParserFactory.newInstance();
		SAXParser parser = null;
		try {
			parser = fac.newSAXParser();
			if(parser != null){
				final InputStream in = getResources().openRawResource(R.raw.predefmaps);
				parser.parse(in, new PredefMapsParser(arr1, arr2, aGetMaps, aGetOverlays, aProjection));
				String[][] arrayList = new String[2][arr2.size()];
				arr1.toArray(arrayList[0]);
				arr2.toArray(arrayList[1]);
				
				return arrayList;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
