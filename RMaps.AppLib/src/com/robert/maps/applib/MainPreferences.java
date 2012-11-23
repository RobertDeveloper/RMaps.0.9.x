/**
 *
 */
package com.robert.maps.applib;

import java.io.File;
import java.io.InputStream;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.robert.maps.applib.R;
import com.robert.maps.applib.constants.PrefConstants;
import com.robert.maps.applib.kml.XMLparser.PredefMapsParser;
import com.robert.maps.applib.utils.Ut;

public class MainPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener, PrefConstants {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final SharedPreferences aPref = PreferenceManager.getDefaultSharedPreferences(this);
		
		final String sdf = aPref.getString("pref_dir_main", "NO");
		if(sdf.equalsIgnoreCase("NO")) {
			final Editor editor = aPref.edit();
			editor.putString("pref_dir_main", Ut.getExternalStorageDirectory()+"/rmaps/");
			editor.putString("pref_dir_maps", Ut.getExternalStorageDirectory()+"/rmaps/maps/");
			editor.putString("pref_dir_import", Ut.getExternalStorageDirectory()+"/rmaps/import/");
			editor.putString("pref_dir_export", Ut.getExternalStorageDirectory()+"/rmaps/export/");
			editor.commit();
		}
		
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.mainpreferences);
		
		findPreference("pref_dir_main").setSummary(aPref.getString("pref_dir_main", Ut.getExternalStorageDirectory()+"/rmaps/"));
		findPreference("pref_dir_maps").setSummary(aPref.getString("pref_dir_maps", Ut.getExternalStorageDirectory()+"/rmaps/maps/"));
		findPreference("pref_main_usermaps").setSummary("Maps from "+aPref.getString("pref_dir_maps", Ut.getExternalStorageDirectory()+"/rmaps/maps/"));
		findPreference("pref_dir_import").setSummary(aPref.getString("pref_dir_import", Ut.getExternalStorageDirectory()+"/rmaps/import/"));
		findPreference("pref_dir_export").setSummary(aPref.getString("pref_dir_export", Ut.getExternalStorageDirectory()+"/rmaps/export/"));

		final PreferenceGroup prefMapsgroup = (PreferenceGroup) findPreference("pref_predefmaps_mapsgroup");
		final PreferenceGroup prefOverlaysgroup = (PreferenceGroup) findPreference("pref_predefmaps_overlaysgroup");

		final SAXParserFactory fac = SAXParserFactory.newInstance();
		SAXParser parser = null;
		try {
			parser = fac.newSAXParser();
			if(parser != null){
				final InputStream in = getResources().openRawResource(R.raw.predefmaps);
				parser.parse(in, new PredefMapsParser(prefMapsgroup, prefOverlaysgroup, this));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		final File folder = Ut.getRMapsMapsDir(this);
		LoadUserMaps(folder);
		
		findPreference("pref_main_mixmaps").setIntent(new Intent(getApplicationContext(), MixedMapsPreference.class));
		
	}

	private void LoadUserMaps(final File folder) {
		// Cash file preferences
		final PreferenceGroup prefUserMapsgroup = (PreferenceGroup) findPreference("pref_usermaps_mapsgroup");
		prefUserMapsgroup.removeAll();

		final SharedPreferences aPref = PreferenceManager.getDefaultSharedPreferences(this);
		final Editor prefEditor = aPref.edit();
		
		final File[] files = folder.listFiles();
		if (files != null)
			for (int i = 0; i < files.length; i++) {
				if (files[i].getName().toLowerCase().endsWith(getString(R.string.mnm))
						|| files[i].getName().toLowerCase().endsWith(getString(R.string.tar))
						|| files[i].getName().toLowerCase().endsWith(getString(R.string.sqlitedb))) {
					final String name = Ut.FileName2ID(files[i].getName());
					
					prefEditor.putString(PREF_USERMAPS_ + name + "_baseurl", files[i].getAbsolutePath());

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
					{
						final CheckBoxPreference pref = new CheckBoxPreference(this);
						pref.setKey(PREF_USERMAPS_ + name + "_isoverlay");
						pref.setTitle(getString(R.string.pref_usermap_overlay));
						pref.setSummary(getString(R.string.pref_usermap_overlay_summary));
						pref.setDefaultValue(false);
						prefscr.addPreference(pref);
					}
					{
						final ListPreference pref = new ListPreference(this);
						pref.setKey(PREF_USERMAPS_ + name + "_stretch");
						pref.setDefaultValue("1");
						pref.setTitle(R.string.pref_stretchtile);
						pref.setSummary(R.string.pref_stretchtile_summary);
						pref.setEntries(R.array.googlescale_pref_title);
						pref.setEntryValues(R.array.googlescale_pref_values);
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
		
		prefEditor.commit();
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
			findPreference("pref_main_usermaps").setSummary("Maps from "+aPref.getString("pref_dir_maps", Ut.getExternalStorageDirectory()+"/rmaps/maps/"));
			findPreference(aKey).setSummary(aPref.getString("pref_dir_maps", Ut.getExternalStorageDirectory()+"/rmaps/maps/"));


			final File dir = new File(aPref.getString("pref_dir_maps", Ut.getExternalStorageDirectory()+"/rmaps/maps/").concat("/").replace("//", "/"));
			if(!dir.exists()){
				if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
					dir.mkdirs();
				}
			}
			if(dir.exists())
				LoadUserMaps(dir);
		}
		else if(Ut.equalsIgnoreCase(aKey, 0, 9, "pref_dir_")) {
			findPreference("pref_dir_main").setSummary(aPref.getString("pref_dir_main", Ut.getExternalStorageDirectory()+"/rmaps/"));
			findPreference("pref_dir_import").setSummary(aPref.getString("pref_dir_import", Ut.getExternalStorageDirectory()+"/rmaps/import/"));
			findPreference("pref_dir_export").setSummary(aPref.getString("pref_dir_export", Ut.getExternalStorageDirectory()+"/rmaps/export/"));
		}
		else if(aKey.equalsIgnoreCase("pref_locale")) {
			Locale locale = ((MapApplication) getApplication()).getDefLocale();
			final String lang = aPref.getString("pref_locale", " ");
			if(lang.equalsIgnoreCase("zh_CN")) {
				locale = Locale.SIMPLIFIED_CHINESE;
			} else if(lang.equalsIgnoreCase("zh_TW")) {
				locale = Locale.TRADITIONAL_CHINESE;
			} else if(!lang.equalsIgnoreCase("") && !lang.equalsIgnoreCase(" ")) {
	            locale = new Locale(lang);
			} 
			Locale.setDefault(locale);
            Configuration config = getBaseContext().getResources().getConfiguration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
            
            finish();
            Intent myIntent = new Intent(MainPreferences.this, MainPreferences.class);
            startActivity(myIntent);
		}
		else if (Ut.equalsIgnoreCase(aKey, 0, 14, PREF_USERMAPS_))
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

}
