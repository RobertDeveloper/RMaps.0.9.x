package com.robert.maps.applib.preference;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;

import com.robert.maps.R;
import com.robert.maps.applib.utils.OnlineCachePreference;

import org.json.JSONObject;

public class CustomMapsPrefActivity extends MMPreferenceActivity implements OnSharedPreferenceChangeListener {
	private String mKey;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
		
		Intent intent = getIntent();
		if(intent == null)
			finish();
		
		Bundle bundle = intent.getExtras();
		mKey = bundle.getString("Key");
		
		final PreferenceCategory prefscr = new PreferenceCategory(this);
		prefscr.setKey(mKey);
		prefscr.setTitle(bundle.getString(NAME));
		getPreferenceScreen().addPreference(prefscr);
		
		prefscr.setTitle(getPreferenceScreen().getSharedPreferences().getString(mKey + "_name", bundle.getString(NAME)));
		prefscr.setSummary(R.string.menu_add_ownsourcemap);
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
			final EditTextPreference pref = new EditTextPreference(this);
			pref.setKey(mKey + "_baseurl");
			pref.setTitle(getString(R.string.pref_usermap_baseurl));
			pref.setSummary(bundle.getString(BASEURL));
			pref.setDefaultValue(bundle.getString(BASEURL));
			pref.setDialogMessage(R.string.pref_custommap_dialogmessage);
			prefscr.addPreference(pref);
		}
		{
			final ListPreference pref = new ListPreference(this);
			pref.setKey(mKey + "_projection");
			pref.setTitle(getString(R.string.pref_usermap_projection));
			pref.setEntryValues(R.array.projection_value);
			pref.setEntries(R.array.projection_title);
			pref.setValue(bundle.getString(MAPPROJECTION));
			prefscr.addPreference(pref);
			pref.setSummary(pref.getEntry());
		}
		{
			final CheckBoxPreference pref = new CheckBoxPreference(this);
			pref.setKey(mKey + "_isoverlay");
			pref.setTitle(getString(R.string.pref_usermap_overlay));
			pref.setSummary(getString(R.string.pref_usermap_overlay_summary));
			pref.setDefaultValue(false);
			prefscr.addPreference(pref);
		}
		{
			final ListPreference pref = new ListPreference(this);
			pref.setKey(mKey + "_stretch");
			pref.setDefaultValue("1");
			pref.setTitle(R.string.pref_stretchtile);
			pref.setSummary(R.string.pref_stretchtile_summary);
			pref.setEntries(R.array.googlescale_pref_title);
			pref.setEntryValues(R.array.googlescale_pref_values);
			prefscr.addPreference(pref);

		}
		{
			final ListPreference pref = new ListPreference(this);
			pref.setKey(mKey + "_minzoom");
			pref.setTitle(getString(R.string.pref_minzoom));
			pref.setEntryValues(R.array.zoomlevel_pref_value);
			pref.setEntries(R.array.zoomlevel_pref_title);
			pref.setValue(""+bundle.getInt(MINZOOM));
			prefscr.addPreference(pref);
			pref.setSummary(pref.getEntry());
		}
		{
			final ListPreference pref = new ListPreference(this);
			pref.setKey(mKey + "_maxzoom");
			pref.setTitle(getString(R.string.pref_maxzoom));
			pref.setEntryValues(R.array.zoomlevel_pref_value);
			pref.setEntries(R.array.zoomlevel_pref_title);
			pref.setValue(""+bundle.getInt(MAXZOOM));
			prefscr.addPreference(pref);
			pref.setSummary(pref.getEntry());
		}
		{
			final CheckBoxPreference pref = new CheckBoxPreference(this);
			pref.setKey(mKey + "_onlinecache");
			pref.setTitle(getString(R.string.pref_onlinecache));
			pref.setSummary(getString(R.string.pref_onlinecache_summary));
			pref.setDefaultValue(true);
			prefscr.addPreference(pref);
		}
		{
			final OnlineCachePreference pref = new OnlineCachePreference(this, bundle.getString(MAPID));
			pref.setKey(mKey + "_clearcache");
			prefscr.addPreference(pref);
		}
		{
			final OffsetPreference pref = new OffsetPreference(this, bundle.getString(MAPID));
			pref.setKey(bundle.getString(MAPID) + "_offset");
			pref.setTitle(R.string.pref_mapoffset);
			pref.setSummary(R.string.pref_mapoffset_summary);
			prefscr.addPreference(pref);
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
			} else if(key.endsWith(BASEURL)) {
				final JSONObject json = MixedMapsPreference.getMapCustomParams(mMapHelper.PARAMS);
				try {
					json.put(BASEURL, sharedPreferences.getString(key, ""));
					mMapHelper.PARAMS = json.toString();
					if(findPreference(key) != null)
						findPreference(key).setSummary(sharedPreferences.getString(key, ""));
				} catch (Exception e) {
				}
				
			} else if(key.endsWith("_projection")) {
				final JSONObject json = MixedMapsPreference.getMapCustomParams(mMapHelper.PARAMS);
				try {
					json.put(MAPPROJECTION, Integer.parseInt(sharedPreferences.getString(key, "")));
					mMapHelper.PARAMS = json.toString();
					if(findPreference(key) != null)
						findPreference(key).setSummary(((ListPreference)findPreference(key)).getEntry());
				} catch (Exception e) {
				}
				
			} else if(key.endsWith("_stretch")) {
				final JSONObject json = MixedMapsPreference.getMapCustomParams(mMapHelper.PARAMS);
				try {
					json.put(STRETCH, Double.parseDouble(sharedPreferences.getString(key, "")));
					mMapHelper.PARAMS = json.toString();
				} catch (Exception e) {
				}
				
			} else if(key.endsWith("_isoverlay")) {
				final JSONObject json = MixedMapsPreference.getMapCustomParams(mMapHelper.PARAMS);
				try {
					json.put(ISOVERLAY, sharedPreferences.getBoolean(key, false));
					mMapHelper.PARAMS = json.toString();
					mMapHelper.TYPE = sharedPreferences.getBoolean(key, false) ? 3 : 2;
				} catch (Exception e) {
				}
				
			} else if(key.endsWith("_onlinecache")) {
				final JSONObject json = MixedMapsPreference.getMapCustomParams(mMapHelper.PARAMS);
				try {
					json.put(ONLINECACHE, sharedPreferences.getBoolean(key, false));
					mMapHelper.PARAMS = json.toString();
				} catch (Exception e) {
				}
				
			} else if(key.endsWith("_minzoom")) {
				final JSONObject json = MixedMapsPreference.getMapCustomParams(mMapHelper.PARAMS);
				try {
					json.put(MINZOOM, Integer.parseInt(sharedPreferences.getString(key, "1")));
					mMapHelper.PARAMS = json.toString();
					if(findPreference(key) != null)
						findPreference(key).setSummary(((ListPreference)findPreference(key)).getEntry());
				} catch (Exception e) {
				}
				
			} else if(key.endsWith("_maxzoom")) {
				final JSONObject json = MixedMapsPreference.getMapCustomParams(mMapHelper.PARAMS);
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
	

}
