package com.robert.maps.applib.utils;

import android.content.SharedPreferences;

public class Units {
	public static final String KM = "km";
	public static final String ML = "ml";
	public static final String KMH = "km/h";
	public static final String MLH = "ml/h";
	public static final String M = "m";
	public static final String FT = "ft";
	public static final String MINKM = "min/km";
	public static final String MINML = "min/ml";
	
	public static final double ML_IN_KM = 0.621371192;
	public static final double FT_IN_M = 3.2808399;
	
	private boolean mMetricSystem;

	public Units(SharedPreferences aPref) {
		super();
		
		mMetricSystem = Integer.parseInt(aPref.getString("pref_units", "0")) == 0 ? true : false;
	}
	
	public String KM() {
		return mMetricSystem ? KM : ML;
	}
	
	public String KMH() {
		return mMetricSystem ? KMH : MLH;
	}
	
	public String M() {
		return mMetricSystem ? M : FT;
	}
	
	public String MINKM() {
		return mMetricSystem ? MINKM : MINML;
	}

	
	public double KM(final double aValue) {
		return mMetricSystem ? aValue : aValue * ML_IN_KM;
	}
	
	public double KMH(final double aValue) {
		return mMetricSystem ? aValue : aValue * ML_IN_KM;
	}
	
	public double M(final double aValue) {
		return mMetricSystem ? aValue : aValue * FT_IN_M;
	}
	
	public double MINKM(final double aValue) {
		return mMetricSystem ? aValue : aValue / ML_IN_KM;
	}
	
	
}
