package com.robert.maps.applib.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.robert.maps.R;
import com.robert.maps.applib.kml.Track;
import com.robert.maps.applib.kml.utils.TrackStyleDrawable;
import com.robert.maps.applib.kml.utils.TrackStylePickerDialog;
import com.robert.maps.applib.kml.utils.TrackStylePickerDialog.OnTrackStyleChangedListener;

import org.json.JSONException;
import org.json.JSONObject;

public class TrackStylePreference extends Preference implements Preference.OnPreferenceClickListener, OnTrackStyleChangedListener {

	private String mValue;
	private String mDefaultValue = "{\"color\":-5937666,\"shadowradius\":0,\"width\":10,\"color_shadow\":-5937666}";
	private TrackStylePickerDialog mDialog;
	private View mView;
	private static final String androidns = "http://schemas.android.com/apk/res/android";

	public TrackStylePreference(Context context) {
		super(context);
		init(null);
	}

	public TrackStylePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public TrackStylePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	private void init(AttributeSet attrs) {
		setOnPreferenceClickListener(this);
		if (attrs != null) {
			final String val = attrs.getAttributeValue(androidns, "defaultValue");
			if(!val.equalsIgnoreCase(""))
				mDefaultValue = val;
		}
		mValue = mDefaultValue;
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		mView = view;
		
		int Color, ColorShadow, Width;
		double ShadowRadius;
		try {
			final JSONObject json = new JSONObject(getValue());
			Color = json.optInt(Track.COLOR, 0xffA565FE);
			Width = json.optInt(Track.WIDTH, 4);
			ShadowRadius = json.optDouble(Track.SHADOWRADIUS, 0);
			ColorShadow = json.optInt(Track.COLORSHADOW, 0xffA565FE);
		} catch (Exception e) {
			Color = 0xffA565FE;
			Width = 4;
			ShadowRadius = 0;
			ColorShadow = 0xffA565FE;
		}
		
		setPreviewColor(Color, Width, ColorShadow, ShadowRadius);
	}

	private void setPreviewColor(int Color, int Width, int ColorShadow, double ShadowRadius) {
		if (mView == null) return;
		
		ImageView iView = new ImageView(getContext());
		LinearLayout widgetFrameView = ((LinearLayout) mView.findViewById(android.R.id.widget_frame));
		if (widgetFrameView == null) return;
		widgetFrameView.setVisibility(View.VISIBLE);
		int count = widgetFrameView.getChildCount();
		if (count > 0) {
			widgetFrameView.removeViews(0, count);
		}
		widgetFrameView.addView(iView);
		widgetFrameView.setMinimumWidth(0);

		final Drawable dr = new TrackStyleDrawable(Color, Width, ColorShadow, ShadowRadius);
		final Drawable[] d = {getContext().getResources().getDrawable(R.drawable.r_home_other1), dr};
		LayerDrawable ld = new LayerDrawable(d);
		iView.setBackgroundDrawable(ld);
}

	public boolean onPreferenceClick(Preference preference) {
		int Color, ColorShadow, Width;
		double ShadowRadius;
		try {
			final JSONObject json = new JSONObject(getValue());
			Color = json.optInt(Track.COLOR, 0xffA565FE);
			Width = json.optInt(Track.WIDTH, 4);
			ShadowRadius = json.optDouble(Track.SHADOWRADIUS, 0);
			ColorShadow = json.optInt(Track.COLORSHADOW, 0xffA565FE);
		} catch (Exception e) {
			Color = 0xffA565FE;
			Width = 4;
			ShadowRadius = 0;
			ColorShadow = 0xffA565FE;
		}
		
		mDialog = new TrackStylePickerDialog(getContext(), Color, Width, ColorShadow, ShadowRadius);
		mDialog.setOnTrackStyleChangedListener(this);
		mDialog.show();
		
		return false;
	}
	
	public String getValue() {
		try {
			if (isPersistent()) {
				mValue = getPersistedString(mDefaultValue);
			}
		} catch (ClassCastException e) {
			mValue = mDefaultValue;
		}

		return mValue;
	}

	public void onTrackStyleChanged(int color, int width, int colorshadow, double shadowradius) {
		final JSONObject json = new JSONObject();
		try {
			json.put(Track.COLOR, color);
			json.put(Track.COLORSHADOW, colorshadow);
			json.put(Track.WIDTH, width);
			json.put(Track.SHADOWRADIUS, shadowradius);
		} catch (JSONException e) {
		}
		
		if (isPersistent()) {
			persistString(json.toString());
		}
		mValue = json.toString();
		setPreviewColor(color, width, colorshadow, shadowradius);
		try {
			getOnPreferenceChangeListener().onPreferenceChange(this, color);
		} catch (NullPointerException e) {

		}
	}

}
