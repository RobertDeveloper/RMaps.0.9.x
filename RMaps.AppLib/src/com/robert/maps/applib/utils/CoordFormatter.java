package com.robert.maps.applib.utils;

import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class CoordFormatter {
	private int mFormat = 0;

	public CoordFormatter(int format) {
		super();
		mFormat = format;
	}
	
	public CoordFormatter(Context ctx) {
		super();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		mFormat = Integer.valueOf(pref.getString("pref_coordformat", "0"));
	}
	
	public String convertLat(double coord) {
		return convert(coord, 'S', 'N');
	}
	
	public String convertLon(double coord) {
		return convert(coord, 'W', 'E');
	}
	
	private String convert(double coord, char minus, char plus) {
		String ret = "";
		
		if(mFormat == 1) {
			ret = String.format(Locale.UK, "%s%d°%.4f'", coord<0?"-":"", Math.abs((int)coord), Math.abs((coord-(int)coord)*60.0));
		} else if(mFormat == 2) {
			final double min = (coord-(int)coord)*60.0;
			ret = String.format(Locale.UK, "%s%d°%d'%.2f\"", coord<0?"-":"", Math.abs((int)coord), Math.abs((int)min), Math.abs((min-(int)min)*60.0));
		} else if(mFormat == 3) {
			ret = String.format(Locale.UK, "%s%.5f°", coord<0?minus:plus, Math.abs(coord));
		} else if(mFormat == 4) {
			ret = String.format(Locale.UK, "%s%d°%.4f'", coord<0?minus:plus, Math.abs((int)coord), Math.abs((coord-(int)coord)*60.0));
		} else if(mFormat == 5) {
			final double min = (coord-(int)coord)*60.0;
			ret = String.format(Locale.UK, "%s%d°%d'%.2f\"", coord<0?minus:plus, Math.abs((int)coord), Math.abs((int)min), Math.abs((min-(int)min)*60.0));
		} else {
			ret = String.format(Locale.UK, "%.5f°", coord);
		} 
		
		return ret;
	}
}
