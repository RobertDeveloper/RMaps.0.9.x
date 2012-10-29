package com.robert.maps;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.XMLparser.PredefMapsParser;
import com.robert.maps.kml.constants.PoiConstants;
import com.robert.maps.utils.Ut;

public class MixedMapsPreference extends PreferenceActivity implements OnSharedPreferenceChangeListener, PoiConstants {
	public static final String MAPID = "mapid";
	public static final String OVERLAYID = "overlayid";
	public static final String OVERLAYOPAQUE = "overlayopaque";
	public static final String PREF_MIXMAPS_ = "PREF_MIXMAPS_";
	
	
	private PoiManager mPoiManager;
	private MapHelper mMapHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.mixedmapspreference);
		registerForContextMenu(getListView());
		
		mPoiManager = new PoiManager(this);
		mMapHelper = new MapHelper();

		findPreference("pref_addmixmap").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				getListView().showContextMenu();
				return false;
			}
		});
		
		loadMixedMaps();
	}

	private void loadMixedMaps() {
		PreferenceGroup prefGroup = (PreferenceGroup) findPreference("pref_mixmaps_group");
		prefGroup.removeAll();
		
		final Cursor c = mPoiManager.getGeoDatabase().getMixedMaps();
		if(c != null) {
			final int idMapid = c.getColumnIndex(MAPID);
			final int idName = c.getColumnIndex(NAME);
			final int idType = c.getColumnIndex(TYPE);
			final int idParams = c.getColumnIndex(PARAMS);
			
			final String[] listMapIDs = null; //= new ArrayList<String>();
			final String[] listMapNames = null; //= new ArrayList<String>();
			final String[] listOverlayIDs = null; //= new ArrayList<String>();
			final String[] listOverlayNames = null; //= new ArrayList<String>();
			getMaps(listMapIDs, listMapNames, true, false);
			getMaps(listOverlayIDs, listOverlayNames, false, true);
			
			if(c.moveToFirst()) {
				do {
					switch(c.getInt(idType)) {
					case 1: {
						final PreferenceScreen prefscr = getPreferenceManager().createPreferenceScreen(this);
						prefscr.setKey(PREF_MIXMAPS_ + c.getInt(idMapid));
						
						prefscr.setTitle(prefscr.getSharedPreferences().getString(PREF_MIXMAPS_ + c.getInt(idMapid) + "_name", c.getString(idName)));
						prefscr.setSummary(R.string.menu_add_dualmap);
						{
							final CheckBoxPreference pref = new CheckBoxPreference(this);
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_enabled");
							pref.setTitle(getString(R.string.pref_usermap_enabled));
							pref.setSummary(getString(R.string.pref_usermap_enabled_summary));
							pref.setDefaultValue(false);
							prefscr.addPreference(pref);
						}
						{
							final EditTextPreference pref = new EditTextPreference(this);
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_name");
							pref.setTitle(getString(R.string.pref_usermap_name));
							pref.setSummary(c.getString(idName));
							pref.setDefaultValue(c.getString(idName));
							prefscr.addPreference(pref);
						}
						{
							final ListPreference pref = new ListPreference(this);
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_"+MAPID);
							pref.setTitle(getString(R.string.pref_mixmap_map));
							pref.setEntries(listMapNames);
							pref.setEntryValues(listMapIDs);
							prefscr.addPreference(pref);
						}
						{
							final ListPreference pref = new ListPreference(this);
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_"+OVERLAYID);
							pref.setTitle(getString(R.string.pref_mixmap_overlay));
							pref.setEntries(listOverlayNames);
							pref.setEntryValues(listOverlayIDs);
							prefscr.addPreference(pref);
						}
						prefGroup.addPreference(prefscr);
						} break;
					case 2: {
						} break;
					};
				} while(c.moveToNext());
			}
			c.close();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		Ut.w("onCreateContextMenu");
		menu.add(Menu.NONE, R.id.add_dualmap, Menu.NONE, R.string.menu_add_dualmap);
		menu.add(Menu.NONE, R.id.add_ownsourcemap, Menu.NONE, R.string.menu_add_ownsourcemap);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.add_dualmap:
			mPoiManager.addMap(1, getMapPairParams("").toString());
			loadMixedMaps();
			break;
		case R.id.add_ownsourcemap:
			Ut.w("add_ownsourcemap");
			break;
		}
		return super.onContextItemSelected(item);
	}

	private JSONObject getMapPairParams(String jsonstring) {
		JSONObject json;
		try {
			json = new JSONObject(jsonstring);
		} catch(Exception e) {
			json = new JSONObject();
			try {
				json.put(MAPID, "");
				json.put(OVERLAYID, "");
				json.put(OVERLAYOPAQUE, 0);
			} catch (JSONException e1) {
			}
		}
		return json;
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.startsWith(PREF_MIXMAPS_)) {
			if(key.endsWith("_name")) {
				final String params[] = key.split("_");
				mMapHelper.getMap(Long.parseLong(params[2]));
				mMapHelper.NAME = sharedPreferences.getString(key, "");
				mMapHelper.updateMap();
				findPreference(key).setSummary(mMapHelper.NAME);
				findPreference(PREF_MIXMAPS_+mMapHelper.ID).setTitle(mMapHelper.NAME);
			}
		}
	}
	
	private class MapHelper {
		public long ID;
		public String NAME;
		public int TYPE;
		public String PARAMS;
		
		public void getMap(long id) {
			ID = 0;
			NAME = "";
			TYPE = 0;
			PARAMS = "";
			
			Cursor c = MixedMapsPreference.this.mPoiManager.getGeoDatabase().getMap(id);
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
			MixedMapsPreference.this.mPoiManager.getGeoDatabase().updateMap(ID, NAME, TYPE, PARAMS);
		}
	}

	@Override
	protected void onResume() {
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		super.onResume();
	}

	@Override
	protected void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	private void getMaps(String[] arrayListID, String[] arrayListName, final boolean aGetMaps, final boolean aGetOverlays) {
		ArrayList<String> arr1 = new ArrayList<String>();
		ArrayList<String> arr2 = new ArrayList<String>();
		
		final SAXParserFactory fac = SAXParserFactory.newInstance();
		SAXParser parser = null;
		try {
			parser = fac.newSAXParser();
			if(parser != null){
				final InputStream in = getResources().openRawResource(R.raw.predefmaps);
				parser.parse(in, new PredefMapsParser(arr1, arr2, aGetMaps, aGetOverlays));
				arrayListID = new String[arr1.size()];
				arrayListName = new String[arr2.size()];
				arr1.toArray(arrayListID);
				arr2.toArray(arrayListName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
