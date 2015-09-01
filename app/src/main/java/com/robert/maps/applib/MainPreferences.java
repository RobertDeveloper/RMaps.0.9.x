package com.robert.maps.applib;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

import com.robert.maps.R;
import com.robert.maps.applib.constants.PrefConstants;
import com.robert.maps.applib.kml.XMLparser.PredefMapsParser;
import com.robert.maps.applib.preference.MixedMapsPreference;
import com.robert.maps.applib.preference.UserMapsPrefActivity;
import com.robert.maps.applib.tileprovider.TileSourceBase;
import com.robert.maps.applib.utils.CheckBoxPreferenceExt;
import com.robert.maps.applib.utils.Ut;

import org.openintents.filemanager.FileManagerActivity;
import org.openintents.filemanager.intents.FileManagerIntents;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class MainPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener, PrefConstants {
	private static String PNG = ".png";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		registerForContextMenu(getListView());

		final SharedPreferences aPref = PreferenceManager.getDefaultSharedPreferences(this);
		
		final ArrayList<String> arrEntry = new ArrayList<String>();
		arrEntry.add("Default");
		final ArrayList<String> arrEntryValues = new ArrayList<String>();
		arrEntryValues.add("");
		final File folderCursors = Ut.getRMapsMainDir(this, "icons/cursors");
		if(folderCursors != null) {
			File[] files = folderCursors.listFiles();
			if(files != null) {
				for (int i = 0; i < files.length; i++) {
					if(files[i].getName().toLowerCase().endsWith(PNG)) {
						arrEntry.add(files[i].getName());
						arrEntryValues.add(files[i].getName());
					}
				}
			}
		}
		
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

		((ListPreference) findPreference("pref_person_icon")).setEntries((String[])arrEntry.toArray(new String[arrEntry.size()]));
		((ListPreference) findPreference("pref_person_icon")).setEntryValues((String[])arrEntryValues.toArray(new String[arrEntry.size()]));
		((ListPreference) findPreference("pref_arrow_icon")).setEntries((String[])arrEntry.toArray(new String[arrEntry.size()]));
		((ListPreference) findPreference("pref_arrow_icon")).setEntryValues((String[])arrEntryValues.toArray(new String[arrEntry.size()]));
		
		findPreference("pref_dir_main").setSummary(aPref.getString("pref_dir_main", Ut.getExternalStorageDirectory()+"/rmaps/"));
		findPreference("pref_dir_maps").setSummary(aPref.getString("pref_dir_maps", Ut.getExternalStorageDirectory()+"/rmaps/maps/"));
		findPreference("pref_main_usermaps").setSummary("Maps from "+aPref.getString("pref_dir_maps", Ut.getExternalStorageDirectory()+"/rmaps/maps/"));
		findPreference("pref_dir_import").setSummary(aPref.getString("pref_dir_import", Ut.getExternalStorageDirectory()+"/rmaps/import/"));
		findPreference("pref_dir_export").setSummary(aPref.getString("pref_dir_export", Ut.getExternalStorageDirectory()+"/rmaps/export/"));
		
		findPreference("pref_dir_main").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				pickDir(R.string.pref_dir_main, Uri.parse(aPref.getString("pref_dir_main", Ut.getExternalStorageDirectory()+"/rmaps/")));
				return false;
			}
		});
		findPreference("pref_dir_maps").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				pickDir(R.string.pref_dir_maps, Uri.parse(aPref.getString("pref_dir_maps", Ut.getExternalStorageDirectory()+"/rmaps/maps/")));
				return false;
			}
		});
		findPreference("pref_dir_import").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				pickDir(R.string.pref_dir_import, Uri.parse(aPref.getString("pref_dir_import", Ut.getExternalStorageDirectory()+"/rmaps/import/")));
				return false;
			}
		});
		findPreference("pref_dir_export").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				pickDir(R.string.pref_dir_export, Uri.parse(aPref.getString("pref_dir_export", Ut.getExternalStorageDirectory()+"/rmaps/export/")));
				return false;
			}
		});
		

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
		
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	private void pickDir(int id, Uri uri) {
		Intent intent = new Intent(MainPreferences.this, FileManagerActivity.class);
		intent.setAction(FileManagerIntents.ACTION_PICK_DIRECTORY);
		intent.setData(uri);
		startActivityForResult(intent, id);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == R.string.pref_dir_main 
				|| requestCode == R.string.pref_dir_maps
				|| requestCode == R.string.pref_dir_import
				|| requestCode == R.string.pref_dir_export) {
			if (resultCode == RESULT_OK && data != null) {
				// obtain the filename
				String filename = Uri.decode(data.getDataString());
				if (filename != null) {
					// Get rid of URI prefix:
					if (filename.startsWith("file://")) {
						filename = filename.substring(7);
					}

					String prefName = "";
					if(requestCode == R.string.pref_dir_main) {
						prefName = "pref_dir_main";
					} else if(requestCode == R.string.pref_dir_maps) {
						prefName = "pref_dir_maps";
					} else if(requestCode == R.string.pref_dir_import) {
						prefName = "pref_dir_import";
					} else if(requestCode == R.string.pref_dir_export) {
						prefName = "pref_dir_export";
					}

					final SharedPreferences aPref = PreferenceManager.getDefaultSharedPreferences(this);
					final Editor editor = aPref.edit();
					editor.putString(prefName, filename);
					editor.commit();
					
					onSharedPreferenceChanged(aPref, prefName);
				}

			}
		}
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

					final CheckBoxPreferenceExt pref = new CheckBoxPreferenceExt(this, PREF_USERMAPS_ + name + "_enabled", false);
					pref.setKey(PREF_USERMAPS_ + name);
					pref.setTitle(aPref.getString(PREF_USERMAPS_ + name + "_name", files[i].getName()));
					pref.setSummary(files[i].getAbsolutePath());
					pref.setIntent(new Intent(this, UserMapsPrefActivity.class)
						.putExtra("Key", PREF_USERMAPS_ + name)
						.putExtra("ID", TileSourceBase.USERMAP_ + name)
						.putExtra("Name", files[i].getName())
						.putExtra("AbsolutePath", files[i].getAbsolutePath())
						);
					prefUserMapsgroup.addPreference(pref);

				}
			}
		
		prefEditor.commit();
	}

    @Override
    protected void onDestroy() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }

	public void onSharedPreferenceChanged(SharedPreferences aPref, String aKey) {
		Ut.w(aKey);

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
		else if (Ut.equalsIgnoreCase(aKey, 0, 14, PREF_USERMAPS_)) {
			if (aKey.endsWith("name") && findPreference(aKey.replace("_name", "")) != null) {
				findPreference(aKey.replace("_name", "")).setTitle(aPref.getString(aKey, ""));
			} else if (aKey.endsWith("_enabled") && findPreference(aKey.replace("_enabled", "")) != null) {
				((CheckBoxPreferenceExt) findPreference(aKey.replace("_enabled", ""))).setChecked(aPref.getBoolean(aKey, true));
			}
		} else if (Ut.equalsIgnoreCase(aKey, 0, 16, PREF_PREDEFMAPS_)) {
			final Preference pref = findPreference(aKey+"_screen");
			if(pref != null && pref instanceof CheckBoxPreferenceExt)
				((CheckBoxPreferenceExt) pref).setChecked(aPref.getBoolean(aKey, true));
				
		}
	}
	
}
