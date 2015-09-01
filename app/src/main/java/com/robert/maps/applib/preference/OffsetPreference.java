package com.robert.maps.applib.preference;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.view.View;
import android.widget.EditText;

import com.robert.maps.R;

import org.andnav.osm.util.GeoPoint;

public class OffsetPreference extends DialogPreference {
    private EditText mEditTextLat;
    private EditText mEditTextLon;
    private String mMapID;

    public OffsetPreference(Context context, String mapID) {
    	super(context, null);
    	setDialogLayoutResource(R.layout.pref_offset);
    	mMapID = mapID;
    }

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		mEditTextLat = (EditText) view.findViewById(R.id.Lat);
		mEditTextLon = (EditText) view.findViewById(R.id.Lon);
		
		final float offsetLat = getSharedPreferences().getFloat(getKey()+"lat", 0f);
		final float offsetLon = getSharedPreferences().getFloat(getKey()+"lon", 0f);
		final GeoPoint geoPoint0 = new GeoPoint(0, 0);
		final int lat = (offsetLat < 0 ? -1 : 1) * geoPoint0.distanceTo(new GeoPoint((int) (1E6 * offsetLat), 0));
		final int lon = (offsetLon < 0 ? -1 : 1) * geoPoint0.distanceTo(new GeoPoint(0, (int) (1E6 * offsetLon)));

		mEditTextLat.setText(String.format("%d", lat));
		mEditTextLon.setText(String.format("%d", lon));
		
		view.findViewById(R.id.map).setOnClickListener(mClickMap);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if (positiveResult) {
			Editor editor = getSharedPreferences().edit();
			final GeoPoint geoPoint0 = new GeoPoint(0, 0);
			
			try {
				final int lat = Integer.parseInt(mEditTextLat.getText().toString());
				final GeoPoint geolat = geoPoint0.calculateEndingGlobalCoordinates(geoPoint0, 0, lat);
				editor.putFloat(getKey()+"lat", (float) geolat.getLatitude());
			} catch (NumberFormatException e1) {
				editor.putFloat(getKey()+"lat", 0f);
			}
			
			try {
				final int lon = Integer.parseInt(mEditTextLon.getText().toString());
				final GeoPoint geolat = geoPoint0.calculateEndingGlobalCoordinates(geoPoint0, 90, lon);
				editor.putFloat(getKey()+"lon", (float) geolat.getLongitude());
			} catch (NumberFormatException e1) {
				editor.putFloat(getKey()+"lon", 0f);
			}

			editor.commit();
		}
	}

	private View.OnClickListener mClickMap = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getContext(), OffsetActivity.class);
			intent.putExtra("MAPID", mMapID);
			getContext().startActivity(intent);
			getDialog().dismiss();
		}
	};

}
