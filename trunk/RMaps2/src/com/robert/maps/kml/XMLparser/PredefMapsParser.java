package com.robert.maps.kml.XMLparser;

import org.andnav.osm.views.util.OpenStreetMapRendererInfo;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceGroup;
import android.view.Menu;
import android.view.MenuItem;

import com.robert.maps.MainPreferences;

public class PredefMapsParser extends DefaultHandler {
	private final OpenStreetMapRendererInfo mRendererInfo;
	private final String mMapId;

	private static final String MAP = "map";
	private static final String LAYER = "layer";
	private static final String CACHE = "cache";
	private static final String TRUE = "true";
	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String DESCR = "descr";
	private static final String BASEURL = "baseurl";
	private static final String IMAGE_FILENAMEENDING = "IMAGE_FILENAMEENDING";
	private static final String ZOOM_MINLEVEL = "ZOOM_MINLEVEL";
	private static final String ZOOM_MAXLEVEL = "ZOOM_MAXLEVEL";
	private static final String MAPTILE_SIZEPX = "MAPTILE_SIZEPX";
	private static final String URL_BUILDER_TYPE = "URL_BUILDER_TYPE";
	private static final String TILE_SOURCE_TYPE = "TILE_SOURCE_TYPE";
	private static final String PROJECTION = "PROJECTION";
	private static final String YANDEX_TRAFFIC_ON = "YANDEX_TRAFFIC_ON";

	private Menu mSubmenu;
	private PreferenceGroup mPrefMapsgroup;
	private Context mPrefActivity;
	private SharedPreferences mSharedPreferences;

	public PredefMapsParser(final PreferenceGroup aPrefMapsgroup, final Context aPrefActivity) {
		super();
		mSubmenu = null;
		mRendererInfo = null;
		mMapId = null;
		mPrefMapsgroup = aPrefMapsgroup;
		mPrefActivity = aPrefActivity;
	}

	public PredefMapsParser(final Menu aSubmenu, final SharedPreferences pref) {
		super();
		mSubmenu = aSubmenu;
		mSharedPreferences = pref;
		mRendererInfo = null;
		mMapId = null;
		mPrefMapsgroup = null;
	}

	public PredefMapsParser(final OpenStreetMapRendererInfo aRendererInfo, final String aMapId) {
		super();
		mSubmenu = null;
		mRendererInfo = aRendererInfo;
		mMapId = aMapId;
		mPrefMapsgroup = null;
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		if(localName.equalsIgnoreCase(MAP)){
			if (mRendererInfo != null) {
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

					mRendererInfo.LAYER = false;
					if(attributes.getIndex(LAYER)>-1)
						mRendererInfo.LAYER = Boolean.parseBoolean(attributes.getValue(LAYER));

					mRendererInfo.CACHE = "";
					if(attributes.getIndex(CACHE)>-1)
						mRendererInfo.CACHE = attributes.getValue(CACHE);

				}
			}
			else if(mSubmenu != null) {
				final int i = attributes.getIndex(LAYER);
				if(mSharedPreferences.getBoolean(MainPreferences.PREF_PREDEFMAPS_+attributes.getValue(ID), true) && (i == -1 || !attributes.getValue(LAYER).equalsIgnoreCase(TRUE))){
					final MenuItem item = mSubmenu.add(attributes.getValue(NAME));
					item.setTitleCondensed(attributes.getValue(ID));
				}
			}
			else if(mPrefMapsgroup != null) {
				final int i = attributes.getIndex(LAYER);
				if(i == -1 || !attributes.getValue(LAYER).equalsIgnoreCase(TRUE)){
					CheckBoxPreference pref = new CheckBoxPreference(mPrefActivity);
					pref.setKey(MainPreferences.PREF_PREDEFMAPS_ + attributes.getValue(ID));
					pref.setTitle(attributes.getValue(NAME));
					pref.setSummary(attributes.getValue(DESCR));
					pref.setDefaultValue(true);
					mPrefMapsgroup.addPreference(pref);
				}
			}
		}
		super.startElement(uri, localName, name, attributes);
	}

}
