package com.robert.maps.applib.preference;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.view.View;
import android.widget.EditText;

import com.robert.maps.applib.R;
import com.robert.maps.applib.utils.Ut;

public class OffsetPreference extends DialogPreference {
    private EditText mEditTextLat;
    private EditText mEditTextLon;
    private String mMapID;

    public OffsetPreference(Context context, String mapID) {
    	super(context, null);
    	setDialogLayoutResource(R.layout.pref_offset);
    	mMapID = mapID;
    	setDialogTitle(getTitle());
    }

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		mEditTextLat = (EditText) view.findViewById(R.id.Lat);
		mEditTextLon = (EditText) view.findViewById(R.id.Lon);
		
		Ut.w(getKey()+"lat");
		mEditTextLat.setText(String.format("%.9f", getSharedPreferences().getFloat(getKey()+"lat", 0f)));
		mEditTextLon.setText(String.format("%.9f", getSharedPreferences().getFloat(getKey()+"lon", 0f)));
		
		view.findViewById(android.R.id.copy).setOnClickListener(mClickCopy);
		view.findViewById(android.R.id.paste).setOnClickListener(mClickPaste);
		view.findViewById(R.id.clear).setOnClickListener(mClickClear);
		view.findViewById(R.id.map).setOnClickListener(mClickMap);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if (positiveResult) {
			Editor editor = getSharedPreferences().edit();
			try {
				editor.putFloat(getKey()+"lat", Float.parseFloat(mEditTextLat.getText().toString()));
			} catch (NumberFormatException e) {
				editor.putFloat(getKey()+"lat", 0f);
			}
			try {
				editor.putFloat(getKey()+"lon", Float.parseFloat(mEditTextLon.getText().toString()));
			} catch (NumberFormatException e) {
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
			Ut.w("set offset="+mMapID);
			getContext().startActivity(intent);
			getDialog().dismiss();
		}
	};

	private View.OnClickListener mClickCopy = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
		}
	};

	private View.OnClickListener mClickPaste = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
		}
	};

	private View.OnClickListener mClickClear = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			mEditTextLat.setText("0.0");
			mEditTextLon.setText("0.0");
		}
	};
}
