package com.robert.maps.applib.preference;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.robert.maps.R;
import com.robert.maps.applib.utils.OnlineCachePreference;

public class PredefMapsPrefActivity extends PreferenceActivity {
	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String GOOGLESCALE = "GOOGLESCALE";
	private static final String PROJECTION = "PROJECTION";
	private static final String MAPTILE_SIZEPX = "MAPTILE_SIZEPX";

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
		
		{
			final CheckBoxPreference pref = new CheckBoxPreference(this);
			pref.setKey(mKey);
			pref.setTitle(getString(R.string.pref_usermap_enabled));
			pref.setSummary(getString(R.string.pref_usermap_enabled_summary));
			pref.setDefaultValue(true);
			prefscr.addPreference(pref);
		}
		{
			final OnlineCachePreference pref = new OnlineCachePreference(this, bundle.getString(ID));
			pref.setKey(mKey + "_clearcache");
			prefscr.addPreference(pref);
		}
		if(bundle.containsKey(GOOGLESCALE) && bundle.getBoolean(GOOGLESCALE)) {
			final ListPreference pref = new ListPreference(this);
			pref.setKey(mKey + "_googlescale");
			pref.setDefaultValue("1");
			pref.setTitle(R.string.pref_googlescale);
			pref.setSummary(R.string.pref_googlescale_summary);
			pref.setEntries(R.array.googlescale_pref_title);
			pref.setEntryValues(R.array.googlescale_pref_values);
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
			final Preference pref = new Preference(this);
			pref.setTitle(R.string.pref_usermap_projection);
			switch(bundle.getInt(PROJECTION)) {
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
			final Preference pref = new Preference(this);
			pref.setTitle(R.string.pref_tile_size);
			int size = bundle.getInt(MAPTILE_SIZEPX);
			
			final SharedPreferences sharpref = PreferenceManager.getDefaultSharedPreferences(this);
			final double GOOGLESCALE_SIZE_FACTOR = Double.parseDouble(sharpref.getString(mKey + "_googlescale", "1"));
			size = (int) (size * GOOGLESCALE_SIZE_FACTOR);
			
			pref.setSummary(String.format("%d x %d px", size, size));
			prefscr.addPreference(pref);
		}
		{
			final OffsetPreference pref = new OffsetPreference(this, bundle.getString(ID));
			pref.setKey(bundle.getString(ID) + "_offset");
			pref.setTitle(R.string.pref_mapoffset);
			pref.setSummary(R.string.pref_mapoffset_summary);
			prefscr.addPreference(pref);
		}
	}
	
}
