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
import android.database.sqlite.SQLiteException;
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

import com.robert.maps.applib.R;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.XMLparser.PredefMapsParser;
import com.robert.maps.kml.constants.PoiConstants;
import com.robert.maps.tileprovider.TileSourceBase;
import com.robert.maps.utils.OnlineCachePreference;
import com.robert.maps.utils.RException;

public class MixedMapsPreference extends PreferenceActivity implements OnSharedPreferenceChangeListener, PoiConstants {
	public static final String MAPID = "mapid";
	public static final String MAPPROJECTION = "mapprojection";
	public static final String OVERLAYID = "overlayid";
	public static final String OVERLAYPROJECTION = "overlayprojection";
	public static final String OVERLAYOPAQUE = "overlayopaque";
	public static final String PREF_MIXMAPS_ = "PREF_MIXMAPS_";
	public static final String BASEURL = "baseurl";
	public static final String ISOVERLAY = "isoverlay";
	public static final String ONLINECACHE = "onlinecache";
	public static final String MINZOOM = "minzoom";
	public static final String MAXZOOM = "maxzoom";
	
	
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
			
			final String[][] listMap = getMaps(true, false, 0);
			final String[][] listOverlays = getMaps(false, true, 0);
			
			if(c.moveToFirst()) {
				do {
					switch(c.getInt(idType)) {
					case 3:
					case 2: {
						final JSONObject json = getMapCustomParams(c.getString(idParams));
						
						final PreferenceScreen prefscr = getPreferenceManager().createPreferenceScreen(this);
						prefscr.setKey(PREF_MIXMAPS_ + c.getInt(idMapid));
						
						prefscr.setTitle(prefscr.getSharedPreferences().getString(PREF_MIXMAPS_ + c.getInt(idMapid) + "_name", c.getString(idName)));
						prefscr.setSummary(R.string.menu_add_ownsourcemap);
						{
							final CheckBoxPreference pref = new CheckBoxPreference(this);
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_enabled");
							pref.setTitle(getString(R.string.pref_usermap_enabled));
							pref.setSummary(getString(R.string.pref_usermap_enabled_summary));
							pref.setDefaultValue(true);
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
							final EditTextPreference pref = new EditTextPreference(this);
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_baseurl");
							pref.setTitle(getString(R.string.pref_usermap_baseurl));
							pref.setSummary(json.optString(BASEURL));
							pref.setDefaultValue(json.optString(BASEURL));
							pref.setDialogMessage(R.string.pref_custommap_dialogmessage);
							prefscr.addPreference(pref);
						}
						{
							final ListPreference pref = new ListPreference(this);
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_projection");
							pref.setTitle(getString(R.string.pref_usermap_projection));
							pref.setEntryValues(R.array.projection_value);
							pref.setEntries(R.array.projection_title);
							pref.setValue(json.optString(MAPPROJECTION));
							pref.setSummary(pref.getEntry());
							prefscr.addPreference(pref);
						}
						{
							final CheckBoxPreference pref = new CheckBoxPreference(this);
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_isoverlay");
							pref.setTitle(getString(R.string.pref_usermap_overlay));
							pref.setSummary(getString(R.string.pref_usermap_overlay_summary));
							pref.setDefaultValue(false);
							prefscr.addPreference(pref);
						}
						{
							final ListPreference pref = new ListPreference(this);
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_minzoom");
							pref.setTitle(getString(R.string.pref_minzoom));
							pref.setEntryValues(R.array.zoomlevel_pref_value);
							pref.setEntries(R.array.zoomlevel_pref_title);
							pref.setValue(""+json.optInt(MINZOOM));
							pref.setSummary(pref.getEntry());
							prefscr.addPreference(pref);
						}
						{
							final ListPreference pref = new ListPreference(this);
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_maxzoom");
							pref.setTitle(getString(R.string.pref_maxzoom));
							pref.setEntryValues(R.array.zoomlevel_pref_value);
							pref.setEntries(R.array.zoomlevel_pref_title);
							pref.setValue(""+json.optInt(MAXZOOM));
							pref.setSummary(pref.getEntry());
							prefscr.addPreference(pref);
						}
						{
							final CheckBoxPreference pref = new CheckBoxPreference(this);
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_onlinecache");
							pref.setTitle(getString(R.string.pref_onlinecache));
							pref.setSummary(getString(R.string.pref_onlinecache_summary));
							pref.setDefaultValue(true);
							prefscr.addPreference(pref);
						}
						{
							final OnlineCachePreference pref = new OnlineCachePreference(this, "mixmap_"+c.getInt(idMapid));
							pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid) + "_clearcache");
							prefscr.addPreference(pref);
						}
						prefGroup.addPreference(prefscr);
						} break;
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
							pref.setDefaultValue(true);
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
		if(item.getItemId() == R.id.add_dualmap) {
			mPoiManager.addMap(1, getMapPairParams("").toString());
			loadMixedMaps();
		} else if(item.getItemId() == R.id.add_ownsourcemap) {
			mPoiManager.addMap(2, getMapCustomParams("").toString());
			loadMixedMaps();
		} else if(item.getItemId() == R.id.menu_deletepoi) {
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
			editor.remove(PREF_MIXMAPS_+params[2]+"_baseurl");
			editor.remove(PREF_MIXMAPS_+params[2]+"_projection");
			editor.remove(PREF_MIXMAPS_+params[2]+"_isoverlay");
			editor.commit();
			
			loadMixedMaps();
		}
		return super.onContextItemSelected(item);
	}

	public static JSONObject getMapPairParams(String jsonstring) {
		JSONObject json;
		try {
			json = new JSONObject(jsonstring);
		} catch(Exception e) {
			json = new JSONObject();
			try {
				json.put(MAPID, "");
				json.put(OVERLAYID, "");
				json.put(OVERLAYOPAQUE, 0);
				json.put(MAPPROJECTION, 0);
				json.put(OVERLAYPROJECTION, 0);
			} catch (JSONException e1) {
			}
		}
		return json;
	}

	public static JSONObject getMapCustomParams(String jsonstring) {
		JSONObject json;
		try {
			json = new JSONObject(jsonstring);
		} catch(Exception e) {
			json = new JSONObject();
			try {
				json.put(BASEURL, "");
				json.put(MAPPROJECTION, 1);
				json.put(ISOVERLAY, false);
				json.put(ONLINECACHE, true);
				json.put(MINZOOM, 0);
				json.put(MAXZOOM, 19);
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
					
					try {
						final TileSourceBase tileSouce = new TileSourceBase(this, sharedPreferences.getString(key, ""));
						json.put(MAPPROJECTION, tileSouce.PROJECTION);
						if(tileSouce.PROJECTION != json.optInt(OVERLAYPROJECTION)) {
							json.put(OVERLAYID, "");
							json.put(OVERLAYPROJECTION, tileSouce.PROJECTION);
							final String overlaykey = key.replace(MAPID, OVERLAYID);
							final ListPreference pref = (ListPreference) findPreference(overlaykey);
							pref.setSummary("");
							
							final String[][] listOverlays = getMaps(false, true, tileSouce.PROJECTION);
							if(listOverlays != null) {
								pref.setEntryValues(listOverlays[0]);
								pref.setEntries(listOverlays[1]);
							}
	
						}
					} catch (SQLiteException e) {
					} catch (RException e) {
					}
					
					mMapHelper.PARAMS = json.toString();
					if(findPreference(key) != null)
						findPreference(key).setSummary(((ListPreference)findPreference(key)).getEntry());
				} catch (Exception e) {
				}
				
			} else if(key.endsWith(OVERLAYID)) {
				final JSONObject json = getMapPairParams(mMapHelper.PARAMS);
				try {
					json.put(OVERLAYID, sharedPreferences.getString(key, ""));
					mMapHelper.PARAMS = json.toString();
					if(findPreference(key) != null)
						findPreference(key).setSummary(((ListPreference)findPreference(key)).getEntry());
				} catch (Exception e) {
				}
				
			} else if(key.endsWith(BASEURL)) {
				final JSONObject json = getMapCustomParams(mMapHelper.PARAMS);
				try {
					json.put(BASEURL, sharedPreferences.getString(key, ""));
					mMapHelper.PARAMS = json.toString();
					if(findPreference(key) != null)
						findPreference(key).setSummary(sharedPreferences.getString(key, ""));
				} catch (Exception e) {
				}
				
			} else if(key.endsWith("_projection")) {
				final JSONObject json = getMapCustomParams(mMapHelper.PARAMS);
				try {
					json.put(MAPPROJECTION, Integer.parseInt(sharedPreferences.getString(key, "")));
					mMapHelper.PARAMS = json.toString();
					if(findPreference(key) != null)
						findPreference(key).setSummary(((ListPreference)findPreference(key)).getEntry());
				} catch (Exception e) {
				}
				
			} else if(key.endsWith("_isoverlay")) {
				final JSONObject json = getMapCustomParams(mMapHelper.PARAMS);
				try {
					json.put(ISOVERLAY, sharedPreferences.getBoolean(key, false));
					mMapHelper.PARAMS = json.toString();
					mMapHelper.TYPE = sharedPreferences.getBoolean(key, false) ? 3 : 2;
				} catch (Exception e) {
				}
				
			} else if(key.endsWith("_onlinecache")) {
				final JSONObject json = getMapCustomParams(mMapHelper.PARAMS);
				try {
					json.put(ONLINECACHE, sharedPreferences.getBoolean(key, false));
					mMapHelper.PARAMS = json.toString();
				} catch (Exception e) {
				}
				
			} else if(key.endsWith("_minzoom")) {
				final JSONObject json = getMapCustomParams(mMapHelper.PARAMS);
				try {
					json.put(MINZOOM, Integer.parseInt(sharedPreferences.getString(key, "1")));
					mMapHelper.PARAMS = json.toString();
					if(findPreference(key) != null)
						findPreference(key).setSummary(((ListPreference)findPreference(key)).getEntry());
				} catch (Exception e) {
				}
				
			} else if(key.endsWith("_maxzoom")) {
				final JSONObject json = getMapCustomParams(mMapHelper.PARAMS);
				try {
					json.put(MAXZOOM, Integer.parseInt(sharedPreferences.getString(key, "20")));
					mMapHelper.PARAMS = json.toString();
					if(findPreference(key) != null)
						findPreference(key).setSummary(((ListPreference)findPreference(key)).getEntry());
				} catch (Exception e) {
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

	private String[][] getMaps(final boolean aGetMaps, final boolean aGetOverlays, final int aProjection) {
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
