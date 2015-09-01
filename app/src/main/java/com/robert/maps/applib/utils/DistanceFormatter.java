package com.robert.maps.applib.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.robert.maps.R;

import java.util.Locale;

public class DistanceFormatter {
	private int mUnits;
	private String mUnitM, mUnitKM;
	static final public int FT_IN_ML = 5280;
	static final public int M_IN_KM = 1000;
	static final String ELEV_FORMAT = "%.1f %s";
	static final double FT_IN_M = 3.2808399;

	public DistanceFormatter(Context ctx) {
		super();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		mUnits = Integer.parseInt(pref.getString("pref_units", "0"));
		
		if(mUnits == 0) {
			mUnitM = ctx.getResources().getString(R.string.m);
			mUnitKM = ctx.getResources().getString(R.string.km);
		} else {
			mUnitM = ctx.getResources().getString(R.string.ft);
			mUnitKM = ctx.getResources().getString(R.string.ml);
		}
	}
	
	@SuppressLint("DefaultLocale")
	public String formatElevation(double mElevation) {
		if(mUnits == 0)
			return String.format(Locale.UK, ELEV_FORMAT, mElevation, mUnitM);
		else
			return String.format(Locale.UK, ELEV_FORMAT, (mElevation * FT_IN_M), mUnitM);
	}

	public String formatDistance(double dist) {
		final String[] str = formatDistance2(dist);
		return str[0]+" "+str[1];
	}

	public String[] formatDistance2(double dist) {
		final String[] str = new String[2];
		if(mUnits == 0) {
			if(dist < M_IN_KM) {
				str[0] = String.format("%.0f", dist);
				str[1] = mUnitM;
			} else if(dist/M_IN_KM < 100) {
				str[0] = String.format("%.1f", dist/M_IN_KM);
				str[1] = mUnitKM;
			} else {
				str[0] = String.format("%.0f", dist/M_IN_KM);
				str[1] = mUnitKM;
			}
		} else {
			if(dist * FT_IN_M < FT_IN_ML) {
				str[0] = String.format("%.0f", dist * FT_IN_M);
				str[1] = mUnitM;
			} else if(dist * FT_IN_M / FT_IN_ML < 100) {
				str[0] = String.format("%.2f", dist * FT_IN_M / FT_IN_ML);
				str[1] = mUnitKM;
			} else {
				str[0] = String.format("%.0f", dist * FT_IN_M / FT_IN_ML);
				str[1] = mUnitKM;
			}
		}
		return str;
	}
	
	public static final String KM = "km";
	public static final String ML = "ml";
	public static final String KMH = "km/h";
	public static final String MLH = "ml/h";
	public static final String M = "m";
	public static final String FT = "ft";
	public static final String MINKM = "min/km";
	public static final String MINML = "min/ml";
	
	public static final double ML_IN_KM = 0.621371192;
	public static final double KMH_IN_MS = 3.6;
	public static final double MLH_IN_MS = 2.237;

	public String formatSpeed(double speed) { // m/s
		final String[] str = formatSpeed2(speed);
		return str[0]+" "+str[1];
	}
	
	public String[] formatSpeed2(double speed) { // m/s
		final String[] str = new String[2];
		if(mUnits == 0) {
			str[0] = String.format("%.1f", speed * KMH_IN_MS);
			str[1] = KMH;
		} else {
			str[0] = String.format("%.1f", speed * MLH_IN_MS);
			str[1] = MLH;
		}
		return str;
	}

}
