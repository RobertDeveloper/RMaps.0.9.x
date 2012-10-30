package com.robert.maps;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

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
			
			final String[][] listMap = getMaps(true, false);
			final String[][] listOverlays = getMaps(false, true);
			
			if(c.moveToFirst()) {
				do {
					switch(c.getInt(idType)) {
					case 1: {
						final JSONObject json = getMapPairParams(c.getString(idParams));
						
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
							if(listMap != null) {
								pref.setEntryValues(listMap[0]);
								pref.setEntries(listMap[1]);
							}
							pref.setValue(json.optString(MAPID));
							pref.setSummary(pref.getEntry());
							prefscr.addPreference(pref);
						}
						{
							final ListPreference pref = new ListPreference(this);
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_"+OVERLAYID);
							pref.setTitle(getString(R.string.pref_mixmap_overlay));
							if(listOverlays != null) {
								pref.setEntryValues(listOverlays[0]);
								pref.setEntries(listOverlays[1]);
							}
							pref.setValue(json.optString(OVERLAYID));
							pref.setSummary(pref.getEntry());
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
		if(menuInfo == null || menuInfo != null && ((AdapterView.AdapterContextMenuInfo) menuInfo).id == 0) {
			menu.add(Menu.NONE, R.id.add_dualmap, Menu.NONE, R.string.menu_add_dualmap);
			menu.add(Menu.NONE, R.id.add_ownsourcemap, Menu.NONE, R.string.menu_add_ownsourcemap);
		} else {
			menu.add(Menu.NONE, R.id.menu_deletepoi, Menu.NONE, R.string.menu_delete);
			((AdapterView.AdapterContextMenuInfo) menuInfo).id = 0;
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	public PrefMenuInfo mPrefMenuInfo = new PrefMenuInfo();
	
	public class PrefMenuInfo implements ContextMenuInfo {
		public int MenuId;
		public long MapId;
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
		case R.id.menu_deletepoi:
			PreferenceGroup prefGroup = (PreferenceGroup) findPreference("pref_mixmaps_group");
			final String key = prefGroup.getPreference(((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position - 2).getKey();
			final String params[] = key.split("_");
			mPoiManager.getGeoDatabase().deleteMap(Long.parseLong(params[2]));
			
			final SharedPreferences aPref = PreferenceManager.getDefaultSharedPreferences(this);
			final Editor editor = aPref.edit();
			editor.remove(PREF_MIXMAPS_+params[2]+"_enabled");
			editor.remove(PREF_MIXMAPS_+params[2]+"_name");
			editor.remove(PREF_MIXMAPS_+params[2]+MAPID);
			editor.remove(PREF_MIXMAPS_+params[2]+OVERLAYID);
			editor.commit();
			
			loadMixedMaps();
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
			final String params[] = key.split("_");
			mMapHelper.getMap(Long.parseLong(params[2]));
			if(key.endsWith("_name")) {
				mMapHelper.NAME = sharedPreferences.getString(key, "");
				if(findPreference(key) != null)
					findPreference(key).setSummary(mMapHelper.NAME);
				if(findPreference(PREF_MIXMAPS_+mMapHelper.ID) != null)
					findPreference(PREF_MIXMAPS_+mMapHelper.ID).setTitle(mMapHelper.NAME);
			} else if(key.endsWith(MAPID)) {
				final JSONObject json = getMapPairParams(mMapHelper.PARAMS);
				try {
					json.put(MAPID, sharedPreferences.getString(key, ""));
					mMapHelper.PARAMS = json.toString();
					findPreference(key).setSummary(((ListPreference)findPreference(key)).getEntry());
				} catch (JSONException e) {
				}
				
			} else if(key.endsWith(OVERLAYID)) {
				final JSONObject json = getMapPairParams(mMapHelper.PARAMS);
				try {
					json.put(OVERLAYID, sharedPreferences.getString(key, ""));
					mMapHelper.PARAMS = json.toString();
					findPreference(key).setSummary(((ListPreference)findPreference(key)).getEntry());
				} catch (JSONException e) {
				}
				
			}
			mMapHelper.updateMap();
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

	private String[][] getMaps(final boolean aGetMaps, final boolean aGetOverlays) {
		ArrayList<String> arr1 = new ArrayList<String>();
		ArrayList<String> arr2 = new ArrayList<String>();
		
		final SAXParserFactory fac = SAXParserFactory.newInstance();
		SAXParser parser = null;
		try {
			parser = fac.newSAXParser();
			if(parser != null){
				final InputStream in = getResources().openRawResource(R.raw.predefmaps);
				parser.parse(in, new PredefMapsParser(arr1, arr2, aGetMaps, aGetOverlays));
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
