package com.robert.maps.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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

	public static String FileName2ID(String name) {
		return name.replace(".", "_").replace(" ", "_").replace("-", "_").trim();
	}

	public static File getRMapsFolder(String aName, boolean aShowAlertToast) {
		File folder = new File("/sdcard/rmaps/" + aName);
		if(!folder.exists()){
			if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
				folder.mkdirs();
			}else if(aShowAlertToast){
				//Toast.makeText(new Application(), "SD card is not available", Toast.LENGTH_LONG);
			}
		}

		return folder;
	}

	public static String readString(final InputStream in, final int size) throws IOException{
		byte b [] = new byte[size];

		int lenght = in.read(b);
		if(b[0] == 0)
			return "";
		else if(lenght > 0)
			return new String(b, 0, lenght);
		else
			return "";
	}

	public static int readInt(final InputStream in) throws IOException{
		int res = 0;
		byte b [] = new byte[4];

		if(in.read(b)>0)
			res = (((int)(b[0] & 0xFF)) << 24) +
			  + ((b[1] & 0xFF) << 16) +
			  + ((b[2] & 0xFF) << 8) +
			  + (b[3] & 0xFF);

		return res;
	}

}
