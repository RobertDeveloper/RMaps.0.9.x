/**
 *
 */
package com.robert.maps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.andnav.osm.views.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

public class MainPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.mainpreferences);

		PreferenceGroup prefMapsgroup = (PreferenceGroup) findPreference("pref_predefmaps_mapsgroup");

		InputStream in = getResources().openRawResource(R.raw.predefmaps);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
		Document doc = null;
		try {
			doc = db.parse(in);

			NodeList nl = doc.getDocumentElement().getElementsByTagName("map");
			for (int i = 0; i < nl.getLength(); i++) {
				NamedNodeMap nnm = nl.item(i).getAttributes();

				Node nodeLayer = nnm.getNamedItem("layer");
				boolean ItIsLayer = false;
				if (nodeLayer != null)
					ItIsLayer = nodeLayer.getNodeValue().equalsIgnoreCase("true");

				if (!ItIsLayer) {
					CheckBoxPreference pref = new CheckBoxPreference(this);
					pref.setKey("pref_predefmaps_" + nnm.getNamedItem("id").getNodeValue());
					pref.setTitle(nnm.getNamedItem("name").getNodeValue());
					pref.setSummary(nnm.getNamedItem("descr").getNodeValue());
					pref.setDefaultValue(true);
					prefMapsgroup.addPreference(pref);
				}
			}
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		LoadUserMaps();
	}

	private void LoadUserMaps() {
		// Cash file preferences
		PreferenceGroup prefUserMapsgroup = (PreferenceGroup) findPreference("pref_usermaps_mapsgroup");
		prefUserMapsgroup.removeAll();

		File folder = Util.getRMapsFolder("maps", true);
		File[] files = folder.listFiles();
		if (files != null)
			for (int i = 0; i < files.length; i++) {
				if (files[i].getName().toLowerCase().endsWith(".mnm")
						|| files[i].getName().toLowerCase().endsWith(".tar")) {
					String name = Util.FileName2ID(files[i].getName());

					PreferenceScreen prefscr = getPreferenceManager().createPreferenceScreen(this);
					prefscr.setKey("pref_usermaps_" + name);
					{
						CheckBoxPreference pref = new CheckBoxPreference(this);
						pref.setKey("pref_usermaps_" + name + "_enabled");
						pref.setTitle("Enabled map");
						pref.setSummary("Add map to maps list");
						pref.setDefaultValue(false);
						prefscr.addPreference(pref);
					}
					{
						EditTextPreference pref = new EditTextPreference(this);
						pref.setKey("pref_usermaps_" + name + "_name");
						pref.setTitle("Name");
						pref.setSummary(files[i].getName());
						pref.setDefaultValue(files[i].getName());
						prefscr.addPreference(pref);
					}
					{
						EditTextPreference pref = new EditTextPreference(this);
						pref.setKey("pref_usermaps_" + name + "_baseurl");
						pref.setTitle("Base URL");
						pref.setSummary(files[i].getAbsolutePath());
						pref.setDefaultValue(files[i].getAbsolutePath());
						pref.setEnabled(false);
						prefscr.addPreference(pref);
					}
					{
						ListPreference pref = new ListPreference(this);
						pref.setKey("pref_usermaps_" + name + "_projection");
						pref.setTitle("Projection");
						pref.setEntries(R.array.projection_title);
						pref.setEntryValues(R.array.projection_value);
						pref.setDefaultValue("1");
						prefscr.addPreference(pref);
						pref.setSummary(pref.getEntry());
					}
					{
						CheckBoxPreference pref = new CheckBoxPreference(this);
						pref.setKey("pref_usermaps_" + name + "_traffic");
						pref.setTitle("Yandex.Traffic");
						pref.setSummary("Добавить слой пробок на карту");
						pref.setDefaultValue(false);
						prefscr.addPreference(pref);
					}

					prefscr.setTitle(prefscr.getSharedPreferences().getString("pref_usermaps_" + name + "_name",
							files[i].getName()));
					if (prefscr.getSharedPreferences().getBoolean("pref_usermaps_" + name + "_enabled", false))
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
		if (aKey.length() > 14)
			if (aKey.substring(0, 14).equalsIgnoreCase("pref_usermaps_")) {
				if (aKey.endsWith("name")) {
					findPreference(aKey).setSummary(aPref.getString(aKey, ""));
					findPreference(aKey.replace("_name", "")).setTitle(aPref.getString(aKey, ""));
				} else if (aKey.endsWith("enabled")) {
					if (aPref.getBoolean(aKey, false))
						findPreference(aKey.replace("_enabled", "")).setSummary(
								"Enabled  " + aPref.getString(aKey.replace("_enabled", "_baseurl"), ""));
					else
						findPreference(aKey.replace("_enabled", "")).setSummary(
								"Disabled  " + aPref.getString(aKey.replace("_enabled", "_baseurl"), ""));
				} else if (aKey.endsWith("projection")) {
					ListPreference pref = (ListPreference) findPreference(aKey);
					findPreference(aKey).setSummary(pref.getEntry());
				}
			}
	}
}
