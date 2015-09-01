package com.robert.maps.applib.preference;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;

import com.robert.maps.R;

public class UserMapsPrefActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
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
		getPreferenceScreen().addPreference(prefscr);
		
		{
			final CheckBoxPreference pref = new CheckBoxPreference(this);
			pref.setKey(mKey + "_enabled");
			pref.setTitle(getString(R.string.pref_usermap_enabled));
			pref.setSummary(getString(R.string.pref_usermap_enabled_summary));
			pref.setDefaultValue(false);
			prefscr.addPreference(pref);
		}
		{
			final EditTextPreference pref = new EditTextPreference(this);
			pref.setKey(mKey + "_name");
			pref.setTitle(getString(R.string.pref_usermap_name));
			pref.setDefaultValue(bundle.getString("Name"));
			prefscr.addPreference(pref);
			
			pref.setSummary(pref.getText());
			prefscr.setTitle(pref.getText());
		}
		{
			final EditTextPreference pref = new EditTextPreference(this);
			pref.setKey(mKey + "_baseurl");
			pref.setTitle(getString(R.string.pref_usermap_baseurl));
			pref.setSummary(bundle.getString("AbsolutePath"));
			pref.setDefaultValue(bundle.getString("AbsolutePath"));
			pref.setEnabled(false);
			prefscr.addPreference(pref);
		}
		{
			final ListPreference pref = new ListPreference(this);
			pref.setKey(mKey + "_projection");
			pref.setTitle(getString(R.string.pref_usermap_projection));
			pref.setEntries(R.array.projection_title);
			pref.setEntryValues(R.array.projection_value);
			pref.setDefaultValue("1");
			prefscr.addPreference(pref);
			pref.setSummary(pref.getEntry());
		}
		{
			final CheckBoxPreference pref = new CheckBoxPreference(this);
			pref.setKey(mKey + "_traffic");
			pref.setTitle(getString(R.string.pref_usermap_traffic));
			pref.setSummary(getString(R.string.pref_usermap_traffic_summary));
			pref.setDefaultValue(false);
			prefscr.addPreference(pref);
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
			final OffsetPreference pref = new OffsetPreference(this, bundle.getString("ID"));
			pref.setKey(bundle.getString("ID") + "_offset");
			pref.setTitle(R.string.pref_mapoffset);
			pref.setSummary(R.string.pref_mapoffset_summary);
			prefscr.addPreference(pref);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences aPref, String aKey) {
		if (aKey.endsWith("name") && findPreference(aKey) != null) {
			findPreference(aKey).setSummary(aPref.getString(aKey, ""));
			findPreference(aKey.replace("_name", "")).setTitle(aPref.getString(aKey, ""));
			
		} else if (aKey.endsWith("projection") && findPreference(aKey) != null) {
			ListPreference pref = (ListPreference) findPreference(aKey);
			findPreference(aKey).setSummary(pref.getEntry());
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	
}
