package com.robert.maps.applib.utils;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.robert.maps.applib.R;

public class DistanceFormatter {
	private int mUnits;
	private String mUnitM, mUnitKM;
	static final public int FT_IN_ML = 5280;
	static final public int M_IN_KM = 1000;
	static final String ELEV_FORMAT = "%.1f %s";
	static final double FT_IN_M = 3.281;

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
			if(dist < FT_IN_ML) {
				str[0] = String.format("%.0f", dist);
				str[1] = mUnitM;
			} else if(dist/FT_IN_ML < 100) {
				str[0] = String.format("%.2f", dist/FT_IN_ML);
				str[1] = mUnitKM;
			} else {
				str[0] = String.format("%.0f", dist/FT_IN_ML);
				str[1] = mUnitKM;
			}
		}
		return str;
	}
}
