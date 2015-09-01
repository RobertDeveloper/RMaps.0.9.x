package com.robert.maps.applib.kml.XMLparser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceGroup;
import android.view.Menu;
import android.view.MenuItem;

import com.robert.maps.applib.MainPreferences;
import com.robert.maps.R;
import com.robert.maps.applib.preference.PredefMapsPrefActivity;
import com.robert.maps.applib.tileprovider.TileSourceBase;
import com.robert.maps.applib.utils.CheckBoxPreferenceExt;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

public class PredefMapsParser extends DefaultHandler {
	private final TileSourceBase mRendererInfo;
	private final String mMapId;

	private static final String MAP = "map";
	private static final String LAYER = "layer";
	private static final String TIMEDEPENDENT = "timedependent";
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
	private static final String GOOGLESCALE = "GOOGLESCALE";

	private Menu mSubmenu;
	private boolean mNeedMaps;
	private boolean mNeedOverlays;
	private int mNeedProjection;
	private PreferenceGroup mPrefMapsgroup;
	private PreferenceGroup mPrefOverlaysgroup;
	private Context mPrefActivity;
	private SharedPreferences mSharedPreferences;
	private ArrayList<String> mID;
	private ArrayList<String> mName;
	
	public PredefMapsParser(final ArrayList<String> arrayListID, final ArrayList<String> arrayListName, final boolean aGetMaps, final boolean aGetOverlays, final int aProjection) {
		super();
		
		mID = arrayListID;
		mName = arrayListName;
		mNeedMaps = aGetMaps;
		mNeedOverlays = aGetOverlays;
		mNeedProjection = aProjection;
		
		mSubmenu = null;
		mRendererInfo = null;
		mMapId = null;
		mPrefMapsgroup = null;
		mPrefOverlaysgroup = null;
		mPrefActivity = null;
	}

	public PredefMapsParser(final PreferenceGroup aPrefMapsgroup, final PreferenceGroup aPrefOverlaysgroup, final Context aPrefActivity) {
		super();
		mSubmenu = null;
		mRendererInfo = null;
		mMapId = null;
		mPrefMapsgroup = aPrefMapsgroup;
		mPrefOverlaysgroup = aPrefOverlaysgroup;
		mPrefActivity = aPrefActivity;
	}

	public PredefMapsParser(final Menu aSubmenu, final SharedPreferences pref, boolean aNeedOverlays, int aProjection) {
		super();
		mSubmenu = aSubmenu;
		mNeedOverlays = aNeedOverlays;
		mNeedProjection = aProjection;
		mSharedPreferences = pref;
		mRendererInfo = null;
		mMapId = null;
		mPrefMapsgroup = null;
		mPrefOverlaysgroup = null;
	}

	public PredefMapsParser(final Menu aSubmenu, final SharedPreferences pref) {
		this(aSubmenu, pref, false, 0);
	}

	public PredefMapsParser(final TileSourceBase aRendererInfo, final String aMapId) {
		super();
		mSubmenu = null;
		mRendererInfo = aRendererInfo;
		mMapId = aMapId;
		mPrefMapsgroup = null;
		mPrefOverlaysgroup = null;
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		if(localName.equalsIgnoreCase(MAP)){
			if (mRendererInfo != null) {
				if(attributes.getValue(ID).equalsIgnoreCase(mMapId)){
					mRendererInfo.ID = attributes.getValue(ID);
					mRendererInfo.MAPID = attributes.getValue(ID);
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
					
					mRendererInfo.TIMEDEPENDENT = false;
					if(attributes.getIndex(TIMEDEPENDENT)>-1)
						mRendererInfo.TIMEDEPENDENT = Boolean.parseBoolean(attributes.getValue(TIMEDEPENDENT));

					mRendererInfo.LAYER = false;
					if(attributes.getIndex(LAYER)>-1)
						mRendererInfo.LAYER = Boolean.parseBoolean(attributes.getValue(LAYER));

					mRendererInfo.CACHE = "";
					if(attributes.getIndex(CACHE)>-1)
						mRendererInfo.CACHE = attributes.getValue(CACHE);

					mRendererInfo.GOOGLESCALE = false;
					if(attributes.getIndex(GOOGLESCALE)>-1)
						mRendererInfo.GOOGLESCALE = Boolean.parseBoolean(attributes.getValue(GOOGLESCALE));

				}
			}
			else if(mSubmenu != null) {
				final int i = attributes.getIndex(LAYER);
				boolean timeDependent = false;
				final int j = attributes.getIndex(TIMEDEPENDENT);
				if(j != -1)
					timeDependent = Boolean.parseBoolean(attributes.getValue(TIMEDEPENDENT));
				
				if(mSharedPreferences.getBoolean(MainPreferences.PREF_PREDEFMAPS_+attributes.getValue(ID), true)) {
					final boolean isLayer = !(i == -1 || !attributes.getValue(LAYER).equalsIgnoreCase(TRUE));
					if(mNeedOverlays && isLayer && !timeDependent 
							//&& (mNeedProjection == 0 || mNeedProjection == Integer.parseInt(attributes.getValue(PROJECTION))) 
							|| !mNeedOverlays && !isLayer) {
						final MenuItem item = mSubmenu.add(R.id.isoverlay, Menu.NONE, Menu.NONE, attributes.getValue(NAME));
						item.setTitleCondensed(attributes.getValue(ID));
					}
				}
			}
			else if(mPrefMapsgroup != null && mPrefOverlaysgroup != null) {
				final int i = attributes.getIndex(LAYER);
				final PreferenceGroup prefGroup = (i == -1 || !attributes.getValue(LAYER).equalsIgnoreCase(TRUE)) ? mPrefMapsgroup : mPrefOverlaysgroup;

				final CheckBoxPreferenceExt pref = new CheckBoxPreferenceExt(mPrefActivity, MainPreferences.PREF_PREDEFMAPS_ + attributes.getValue(ID));
				pref.setKey(MainPreferences.PREF_PREDEFMAPS_ + attributes.getValue(ID) + "_screen");
				final Intent intent = new Intent(mPrefActivity, PredefMapsPrefActivity.class)
					.putExtra("Key", MainPreferences.PREF_PREDEFMAPS_ + attributes.getValue(ID))
					.putExtra(ID, attributes.getValue(ID))
					.putExtra(NAME, attributes.getValue(NAME))
					.putExtra(PROJECTION, Integer.parseInt(attributes.getValue(PROJECTION)))
					.putExtra(MAPTILE_SIZEPX, Integer.parseInt(attributes.getValue(MAPTILE_SIZEPX)));
				final int j = attributes.getIndex(GOOGLESCALE);
				if(j > -1 && attributes.getValue(GOOGLESCALE).equalsIgnoreCase(TRUE))
					intent.putExtra(GOOGLESCALE, true);
					
				pref.setIntent(intent);
				pref.setTitle(attributes.getValue(NAME));
				pref.setSummary(attributes.getValue(DESCR));
				prefGroup.addPreference(pref);
			}
			else if(mID != null) {
				final int i = attributes.getIndex(LAYER);
				boolean timeDependent = false;
				final int j = attributes.getIndex(TIMEDEPENDENT);
				if(j != -1)
					timeDependent = Boolean.parseBoolean(attributes.getValue(TIMEDEPENDENT));
				
				final boolean isLayer = !(i == -1 || !attributes.getValue(LAYER).equalsIgnoreCase(TRUE));
				final int proj = Integer.parseInt(attributes.getValue(PROJECTION));
				
				if(mNeedMaps && !isLayer || mNeedOverlays && isLayer && !timeDependent && (mNeedProjection == 0 || mNeedProjection == proj)) {
					mID.add(attributes.getValue(ID));
					mName.add(attributes.getValue(NAME));
				}
			}
		}
		super.startElement(uri, localName, name, attributes);
	}

}
