package com.robert.maps.applib;

import java.util.Locale;

import org.andnav.osm.util.GeoPoint;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.preference.PreferenceManager;

public class MapApplication extends Application {
	private Locale locale = null;
	private Locale defLocale = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
	
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration config = getBaseContext().getResources().getConfiguration();
        defLocale = config.locale;
        locale = defLocale;
        
        String lang = pref.getString("pref_locale", "");
		if(lang.equalsIgnoreCase("zh_CN")) {
			locale = Locale.SIMPLIFIED_CHINESE;
		} else if(lang.equalsIgnoreCase("zh_TW")) {
			locale = Locale.TRADITIONAL_CHINESE;
		} else if(!lang.equalsIgnoreCase("") && !lang.equalsIgnoreCase(" ")) {
            locale = new Locale(lang);
		} 
        Locale.setDefault(locale);
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
	
	public Locale getDefLocale() {
		return defLocale;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration config = getBaseContext().getResources().getConfiguration();
        defLocale = config.locale;
        locale = defLocale;
        
        String lang = pref.getString("pref_locale", "");
		if(lang.equalsIgnoreCase("zh_CN")) {
			locale = Locale.SIMPLIFIED_CHINESE;
		} else if(lang.equalsIgnoreCase("zh_TW")) {
			locale = Locale.TRADITIONAL_CHINESE;
		} else if(!lang.equalsIgnoreCase("") && !lang.equalsIgnoreCase(" ")) {
            locale = new Locale(lang);
		} 
        Locale.setDefault(locale);
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
	}
	
}
