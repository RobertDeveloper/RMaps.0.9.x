package com.robert.maps.utils;

import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class Ut implements OpenStreetMapConstants, OpenStreetMapViewConstants {
	public static String getAppVersion(Context ctx) {
		PackageInfo pi;
		String res = "";
		try {
			pi = ctx.getPackageManager().getPackageInfo("com.robert.maps", 0);
			res = pi.versionName;
		} catch (NameNotFoundException e) {
		}

		return res;
	}

	public static void dd(String str){
		Log.d(DEBUGTAG, str);
	}

	public static void e(String str){
		if(DEBUGMODE)
			Log.e(DEBUGTAG, str);
	}
	public static void i(String str){
		if(DEBUGMODE)
			Log.i(DEBUGTAG, str);
	}
	public static void d(String str){
		if(DEBUGMODE)
			Log.d(DEBUGTAG, str);
	}
}
