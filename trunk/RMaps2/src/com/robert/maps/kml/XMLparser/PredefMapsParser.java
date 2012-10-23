package com.robert.maps.kml.XMLparser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

import com.robert.maps.MainPreferences;
import com.robert.maps.R;
import com.robert.maps.tileprovider.TileSourceBase;
import com.robert.maps.utils.OnlineCachePreference;

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
	private boolean mNeedOverlays;
	private int mNeedProjection;
	private PreferenceGroup mPrefMapsgroup;
	private PreferenceGroup mPrefOverlaysgroup;
	private Context mPrefActivity;
	private SharedPreferences mSharedPreferences;

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
					if(mNeedOverlays && isLayer && !timeDependent && mNeedProjection == Integer.parseInt(attributes.getValue(PROJECTION)) || !mNeedOverlays && !isLayer) {
						final MenuItem item = mSubmenu.add(R.id.isoverlay, Menu.NONE, Menu.NONE, attributes.getValue(NAME));
						item.setTitleCondensed(attributes.getValue(ID));
					}
				}
			}
			else if(mPrefMapsgroup != null && mPrefOverlaysgroup != null) {
				final int i = attributes.getIndex(LAYER);
				final PreferenceGroup prefGroup = (i == -1 || !attributes.getValue(LAYER).equalsIgnoreCase(TRUE)) ? mPrefMapsgroup : mPrefOverlaysgroup;
					
				final PreferenceScreen prefscr = ((PreferenceActivity) mPrefActivity).getPreferenceManager().createPreferenceScreen(mPrefActivity);
				prefscr.setKey(MainPreferences.PREF_PREDEFMAPS_ + attributes.getValue(ID) + "_screen");
				//PREF_USERMAPS_ + name);
				{
					final CheckBoxPreference pref = new CheckBoxPreference(mPrefActivity);
					pref.setKey(MainPreferences.PREF_PREDEFMAPS_ + attributes.getValue(ID));
					pref.setTitle(mPrefActivity.getString(R.string.pref_usermap_enabled));
					pref.setSummary(mPrefActivity.getString(R.string.pref_usermap_enabled_summary));
					pref.setDefaultValue(true);
					prefscr.addPreference(pref);
				}
				{
					final OnlineCachePreference pref = new OnlineCachePreference(mPrefActivity, attributes.getValue(ID));
					pref.setKey(MainPreferences.PREF_PREDEFMAPS_ + attributes.getValue(ID) + "_clearcache");
					prefscr.addPreference(pref);
				}
				final int j = attributes.getIndex(GOOGLESCALE);
				if(j > -1 && attributes.getValue(GOOGLESCALE).equalsIgnoreCase(TRUE)) {
					final ListPreference pref = new ListPreference(mPrefActivity);
					pref.setKey(MainPreferences.PREF_PREDEFMAPS_ + attributes.getValue(ID) + "_googlescale");
					pref.setDefaultValue("1");
					pref.setTitle(R.string.pref_googlescale);
					pref.setSummary(R.string.pref_googlescale_summary);
					pref.setEntries(R.array.googlescale_pref_title);
					pref.setEntryValues(R.array.googlescale_pref_values);
					prefscr.addPreference(pref);
				}
				{
					final Preference pref = new Preference(mPrefActivity);
					pref.setTitle(R.string.pref_usermap_projection);
					switch(Integer.parseInt(attributes.getValue(PROJECTION))) {
					case 1:
						pref.setSummary(R.string.mercator_spheroid);
						break;
					case 2:
						pref.setSummary(R.string.mercator_ellipsoid);
						break;
					case 3:
						pref.setSummary(R.string.osgb36);
						break;
					}
					prefscr.addPreference(pref);
				}
				{
					final Preference pref = new Preference(mPrefActivity);
					pref.setTitle(R.string.pref_tile_size);
					int size = Integer.parseInt(attributes.getValue(MAPTILE_SIZEPX));
					
					((PreferenceActivity) mPrefActivity).getPreferenceManager();
					final SharedPreferences sharpref = PreferenceManager.getDefaultSharedPreferences(mPrefActivity);
					final double GOOGLESCALE_SIZE_FACTOR = Double.parseDouble(sharpref.getString(MainPreferences.PREF_PREDEFMAPS_ + attributes.getValue(ID) + "_googlescale", "1"));
					size = (int) (size * GOOGLESCALE_SIZE_FACTOR);
					
					pref.setSummary(String.format("%d x %d px", size, size));
					prefscr.addPreference(pref);
				}
				
				prefscr.setTitle(attributes.getValue(NAME));
				prefscr.setSummary(attributes.getValue(DESCR));
				prefGroup.addPreference(prefscr);
			}
		}
		super.startElement(uri, localName, name, attributes);
	}

}
