package com.robert.maps.applib.preference;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;

import com.robert.maps.R;
import com.robert.maps.applib.tileprovider.TileSourceBase;
import com.robert.maps.applib.utils.RException;

import org.json.JSONObject;

public class PairMapsPrefActivity extends MMPreferenceActivity implements OnSharedPreferenceChangeListener {
	private String mKey;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
		
		Intent intent = getIntent();
		if(intent == null)
			finish();
		
		final String[][] listMap = getMaps(true, false, 0);
		final String[][] listOverlays = getMaps(false, true, 0);
		Bundle bundle = intent.getExtras();
		mKey = bundle.getString("Key");
		
		final PreferenceCategory prefscr = new PreferenceCategory(this);
		prefscr.setKey(mKey);
		prefscr.setTitle(bundle.getString(NAME));
		getPreferenceScreen().addPreference(prefscr);
		
		prefscr.setTitle(getPreferenceScreen().getSharedPreferences().getString(mKey + "_name", bundle.getString(NAME)));
		prefscr.setSummary(R.string.menu_add_dualmap);
		{
			final CheckBoxPreference pref = new CheckBoxPreference(this);
			pref.setKey(mKey + "_enabled");
			pref.setTitle(getString(R.string.pref_usermap_enabled));
			pref.setSummary(getString(R.string.pref_usermap_enabled_summary));
			pref.setDefaultValue(true);
			prefscr.addPreference(pref);
		}
		{
			final EditTextPreference pref = new EditTextPreference(this);
			pref.setKey(mKey + "_name");
			pref.setTitle(getString(R.string.pref_usermap_name));
			pref.setSummary(bundle.getString(NAME));
			pref.setDefaultValue(bundle.getString(NAME));
			prefscr.addPreference(pref);
		}
		{
			final ListPreference pref = new ListPreference(this);
			pref.setKey(mKey + "_"+MAPID);
			pref.setTitle(getString(R.string.pref_mixmap_map));
			if(listMap != null) {
				pref.setEntryValues(listMap[0]);
				pref.setEntries(listMap[1]);
			}
			pref.setValue(bundle.getString(MAPID));
			prefscr.addPreference(pref);
			pref.setSummary(pref.getEntry());
		}
		{
			final ListPreference pref = new ListPreference(this);
			pref.setKey(mKey + "_"+OVERLAYID);
			pref.setTitle(getString(R.string.pref_mixmap_overlay));
			if(listOverlays != null) {
				pref.setEntryValues(listOverlays[0]);
				pref.setEntries(listOverlays[1]);
			}
			pref.setValue(bundle.getString(OVERLAYID));
			prefscr.addPreference(pref);
			pref.setSummary(pref.getEntry());
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

	@Override
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
			} else if(key.endsWith(OVERLAYID)) {
				final JSONObject json = MixedMapsPreference.getMapPairParams(mMapHelper.PARAMS);
				try {
					json.put(OVERLAYID, sharedPreferences.getString(key, ""));
					mMapHelper.PARAMS = json.toString();
					if(findPreference(key) != null)
						findPreference(key).setSummary(((ListPreference)findPreference(key)).getEntry());
				} catch (Exception e) {
				}
				
			} else if(key.endsWith(MAPID)) {
				final JSONObject json = MixedMapsPreference.getMapPairParams(mMapHelper.PARAMS);
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
				
			}
			mMapHelper.updateMap();
		}
		
	}
	
}
