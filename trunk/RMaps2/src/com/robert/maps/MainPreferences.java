/**
 *
 */
package com.robert.maps;

import java.io.File;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import com.robert.maps.constants.PrefConstants;
import com.robert.maps.kml.XMLparser.PredefMapsParser;
import com.robert.maps.utils.Ut;

public class MainPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener, PrefConstants {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.mainpreferences);

		final PreferenceGroup prefMapsgroup = (PreferenceGroup) findPreference("pref_predefmaps_mapsgroup");

		final SAXParserFactory fac = SAXParserFactory.newInstance();
		SAXParser parser = null;
		try {
			parser = fac.newSAXParser();
			if(parser != null){
				final InputStream in = getResources().openRawResource(R.raw.predefmaps);
				parser.parse(in, new PredefMapsParser(prefMapsgroup, this));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		final File folder = Ut.getRMapsMapsDir(this);
		LoadUserMaps(folder);
	}

	private void LoadUserMaps(final File folder) {
		// Cash file preferences
		final PreferenceGroup prefUserMapsgroup = (PreferenceGroup) findPreference("pref_usermaps_mapsgroup");
		prefUserMapsgroup.removeAll();

		final File[] files = folder.listFiles();
		if (files != null)
			for (int i = 0; i < files.length; i++) {
				if (files[i].getName().toLowerCase().endsWith(
						getString(R.string.mnm))
						|| files[i].getName().toLowerCase().endsWith(
								getString(R.string.tar))
						|| files[i].getName().toLowerCase().endsWith(
								getString(R.string.sqlitedb))) {
					final String name = Ut.FileName2ID(files[i].getName());

					final PreferenceScreen prefscr = getPreferenceManager().createPreferenceScreen(this);
					prefscr.setKey(PREF_USERMAPS_ + name);
					{
						final CheckBoxPreference pref = new CheckBoxPreference(this);
						pref.setKey(PREF_USERMAPS_ + name + "_enabled");
						pref.setTitle(getString(R.string.pref_usermap_enabled));
						pref.setSummary(getString(R.string.pref_usermap_enabled_summary));
						pref.setDefaultValue(false);
						prefscr.addPreference(pref);
					}
					{
						final EditTextPreference pref = new EditTextPreference(this);
						pref.setKey(PREF_USERMAPS_ + name + "_name");
						pref.setTitle(getString(R.string.pref_usermap_name));
						pref.setSummary(files[i].getName());
						pref.setDefaultValue(files[i].getName());
						prefscr.addPreference(pref);
					}
					{
						final EditTextPreference pref = new EditTextPreference(this);
						pref.setKey(PREF_USERMAPS_ + name + "_baseurl");
						pref.setTitle(getString(R.string.pref_usermap_baseurl));
						pref.setSummary(files[i].getAbsolutePath());
						pref.setDefaultValue(files[i].getAbsolutePath());
						pref.setEnabled(false);
						prefscr.addPreference(pref);
					}
					{
						final ListPreference pref = new ListPreference(this);
						pref.setKey(PREF_USERMAPS_ + name + "_projection");
						pref.setTitle(getString(R.string.pref_usermap_projection));
						pref.setEntries(R.array.projection_title);
						pref.setEntryValues(R.array.projection_value);
						pref.setDefaultValue("1");
						prefscr.addPreference(pref);
						pref.setSummary(pref.getEntry());
					}
					{
						final CheckBoxPreference pref = new CheckBoxPreference(this);
						pref.setKey(PREF_USERMAPS_ + name + "_traffic");
						pref.setTitle(getString(R.string.pref_usermap_traffic));
						pref.setSummary(getString(R.string.pref_usermap_traffic_summary));
						pref.setDefaultValue(false);
						prefscr.addPreference(pref);
					}

					prefscr.setTitle(prefscr.getSharedPreferences().getString(PREF_USERMAPS_ + name + "_name",
							files[i].getName()));
					if (prefscr.getSharedPreferences().getBoolean(PREF_USERMAPS_ + name + "_enabled", false))
						prefscr.setSummary("Enabled  " + files[i].getAbsolutePath());
					else
						prefscr.setSummary("Disabled  " + files[i].getAbsolutePath());
					prefUserMapsgroup.addPreference(prefscr);
				}
			}
	}

	@Override
	    protected void onResume() {
	        super.onResume();

	        // Set up a listener whenever a key changes
	        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	    }

	    @Override
	    protected void onPause() {
	        super.onPause();

	        // Unregister the listener whenever a key changes
	        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	    }

	public void onSharedPreferenceChanged(SharedPreferences aPref, String aKey) {

		if(aKey.equalsIgnoreCase("pref_dir_maps")){
			findPreference("pref_main_usermaps").setSummary("Maps from "+aPref.getString("pref_dir_maps", "/sdcard/rmaps/maps/"));
			findPreference(aKey).setSummary(aPref.getString("pref_dir_maps", "/sdcard/rmaps/maps/"));


			final File dir = new File(aPref.getString("pref_dir_maps", "/sdcard/rmaps/maps/").concat("/").replace("//", "/"));
			if(!dir.exists()){
				if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
					dir.mkdirs();
				}
			}
			if(dir.exists())
				LoadUserMaps(dir);
		}
		else if(aKey.substring(0, 9).equalsIgnoreCase("pref_dir_")) {
			findPreference("pref_dir_main").setSummary(aPref.getString("pref_dir_main", "/sdcard/rmaps/"));
			findPreference("pref_dir_import").setSummary(aPref.getString("pref_dir_import", "/sdcard/rmaps/import/"));
			findPreference("pref_dir_export").setSummary(aPref.getString("pref_dir_export", "/sdcard/rmaps/export/"));
		}
		else if (aKey.length() > 14)
			if (aKey.substring(0, 14).equalsIgnoreCase(PREF_USERMAPS_)) {
				if (aKey.endsWith("name") && findPreference(aKey) != null) {
					findPreference(aKey).setSummary(aPref.getString(aKey, ""));
					findPreference(aKey.replace("_name", "")).setTitle(aPref.getString(aKey, ""));
				} else if (aKey.endsWith("enabled") && findPreference(aKey.replace("_enabled", "")) != null) {
					if (aPref.getBoolean(aKey, false))
						findPreference(aKey.replace("_enabled", "")).setSummary(
								"Enabled  " + aPref.getString(aKey.replace("_enabled", "_baseurl"), ""));
					else
						findPreference(aKey.replace("_enabled", "")).setSummary(
								"Disabled  " + aPref.getString(aKey.replace("_enabled", "_baseurl"), ""));
				} else if (aKey.endsWith("projection") && findPreference(aKey) != null) {
					ListPreference pref = (ListPreference) findPreference(aKey);
					findPreference(aKey).setSummary(pref.getEntry());
				}
			}
		onContentChanged();
	}

}
