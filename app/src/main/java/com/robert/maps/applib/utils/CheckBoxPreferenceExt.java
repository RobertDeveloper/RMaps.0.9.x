package com.robert.maps.applib.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;

public class CheckBoxPreferenceExt extends Preference implements CompoundButton.OnCheckedChangeListener,
		OnClickListener {

	private boolean mChecked;
	private boolean mDefaultValueChecked;
	private String mPrefKeyChecked;
	private Intent mIntent;
	private Checkable mCheckBox;

	public CheckBoxPreferenceExt(Context context, String keyChecked) {
		this(context, keyChecked, true);
	}
	
	public CheckBoxPreferenceExt(Context context, String keyChecked, boolean defValue) {
		super(context, null, android.R.attr.checkBoxPreferenceStyle);
		mDefaultValueChecked = defValue;
		mPrefKeyChecked = keyChecked;
		
		mChecked = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(mPrefKeyChecked, mDefaultValueChecked);
	}

	@Override
	public View getView(View convertView, ViewGroup parent) {
		View view = super.getView(convertView, parent);
		
        View checkboxView = view.findViewById(android.R.id.checkbox);
        if (checkboxView != null && checkboxView instanceof Checkable) {
        	checkboxView.setClickable(true);
            ((Checkable) checkboxView).setChecked(mChecked);
            ((CheckBox) checkboxView).setOnCheckedChangeListener(this);
        }

        view.setOnClickListener(this);
        view.setLongClickable(true);
        
		return view;
	}

	@Override
	public void onClick(View v) {
		Context context = getContext();
		if(context != null && mIntent != null) {
			context.startActivity(mIntent);
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
		editor.putBoolean(mPrefKeyChecked, isChecked);
		editor.commit();
	}

	@Override
	public void setIntent(Intent intent) {
		super.setIntent(intent);
		mIntent = intent;
	}

	public void setChecked(boolean isChecked) {
		mChecked = isChecked;
		if(mCheckBox != null)
			mCheckBox.setChecked(isChecked);
	}
	
}
