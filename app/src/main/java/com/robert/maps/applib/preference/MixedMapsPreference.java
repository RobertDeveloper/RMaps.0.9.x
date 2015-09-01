package com.robert.maps.applib.preference;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.robert.maps.R;
import com.robert.maps.applib.utils.CheckBoxPreferenceExt;

import org.json.JSONException;
import org.json.JSONObject;

public class MixedMapsPreference extends MMPreferenceActivity implements OnSharedPreferenceChangeListener {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.mixedmapspreference);
		registerForContextMenu(getListView());
		
		findPreference("pref_addmixmap").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				getListView().showContextMenu();
				return false;
			}
		});
		
		loadMixedMaps();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
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
			
			if(c.moveToFirst()) {
				do {
					switch(c.getInt(idType)) {
					case 3:
					case 2: {
						final JSONObject json = getMapCustomParams(c.getString(idParams));
						
						final CheckBoxPreferenceExt pref = new CheckBoxPreferenceExt(this, PREF_MIXMAPS_ + c.getInt(idMapid) + "_enabled", true);
						pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid));
						pref.setTitle(PreferenceManager.getDefaultSharedPreferences(this).getString(PREF_MIXMAPS_ + c.getInt(idMapid) + "_name", c.getString(idName)));
						pref.setSummary(R.string.menu_add_ownsourcemap);
						pref.setIntent(new Intent(this, CustomMapsPrefActivity.class)
							.putExtra("Key", PREF_MIXMAPS_ + c.getInt(idMapid))
							.putExtra(MAPID, "mixmap_"+c.getInt(idMapid))
							.putExtra(NAME, c.getString(idName))
							.putExtra(BASEURL, json.optString(BASEURL))
							.putExtra(MAPPROJECTION, json.optString(MAPPROJECTION))
							.putExtra(MINZOOM, json.optString(MINZOOM))
							.putExtra(MAXZOOM, json.optString(MAXZOOM))
							);
						prefGroup.addPreference(pref);

						} break;
					case 1: {
						final JSONObject json = getMapPairParams(c.getString(idParams));
						
						final CheckBoxPreferenceExt pref = new CheckBoxPreferenceExt(this, PREF_MIXMAPS_ + c.getInt(idMapid) + "_enabled", true);
						pref.setKey(PREF_MIXMAPS_ + c.getInt(idMapid));
						pref.setTitle(PreferenceManager.getDefaultSharedPreferences(this).getString(PREF_MIXMAPS_ + c.getInt(idMapid) + "_name", c.getString(idName)));
						pref.setSummary(R.string.menu_add_dualmap);
						pref.setIntent(new Intent(this, PairMapsPrefActivity.class)
							.putExtra("Key", PREF_MIXMAPS_ + c.getInt(idMapid))
							.putExtra(MAPID, c.getString(idMapid))
							.putExtra(NAME, c.getString(idName))
							.putExtra(OVERLAYID, json.optString(OVERLAYID))
							);
						prefGroup.addPreference(pref);

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
		} else if(menuInfo != null && ((AdapterView.AdapterContextMenuInfo) menuInfo).position != 0) {
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
				json.put(STRETCH, 1.0f);
			} catch (JSONException e1) {
			}
		}
		return json;
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.startsWith(PREF_MIXMAPS_)) {
			if(key.endsWith("_name")) {
				CheckBoxPreferenceExt pref = (CheckBoxPreferenceExt) findPreference(key.replace("_name", ""));
				if(pref != null)
					pref.setTitle(sharedPreferences.getString(key, ""));
			} else if(key.endsWith("_enabled")) {
				CheckBoxPreferenceExt pref = (CheckBoxPreferenceExt) findPreference(key.replace("_enabled", ""));
				if(pref != null)
					pref.setChecked(sharedPreferences.getBoolean(key, true));
			}
			
		}
	}
	
	@Override
	protected void onDestroy() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

}
