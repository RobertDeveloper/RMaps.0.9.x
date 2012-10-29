package com.robert.maps;

import com.robert.maps.utils.Ut;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;

public class MixedMapsPreference extends PreferenceActivity {

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
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		Ut.w("onCreateContextMenu");
		menu.add(Menu.NONE, R.id.add_dualmap, Menu.NONE, R.string.menu_add_dualmap);
		menu.add(Menu.NONE, R.id.add_ownsourcemap, Menu.NONE, R.string.menu_add_ownsourcemap);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.add_dualmap:
			Ut.w("add_dualmap");
			break;
		case R.id.add_ownsourcemap:
			Ut.w("add_ownsourcemap");
			break;
		}
		return super.onContextItemSelected(item);
	}

}
