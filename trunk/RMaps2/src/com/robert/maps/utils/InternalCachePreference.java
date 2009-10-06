package com.robert.maps.utils;

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.robert.maps.R;

public class InternalCachePreference extends Preference {
    private Button btnClear;
    private Context mCtx;
    private File mDbFile;

	public InternalCachePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mCtx = context;
		setWidgetLayoutResource(R.layout.preference_widget_btn_clear);
		mDbFile = new File("/data/data/com.robert.maps/databases/osmaptilefscache_db");
		setSummary(String.format(mCtx.getString(R.string.pref_internalcache_summary), (int) mDbFile
				.length() / 1024));
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		btnClear = (Button) view.findViewById(R.id.btnClear);
		btnClear.setOnClickListener(new OnClickListener() {
			// @Override
			public void onClick(View v) {
				SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
						"/data/data/com.robert.maps/databases/osmaptilefscache_db", null);
				if (db != null) {
					db.execSQL("DELETE FROM 't_fscache'");
					InternalCachePreference.this.setSummary(String.format(
							InternalCachePreference.this.mCtx
									.getString(R.string.pref_internalcache_summary),
							(int) InternalCachePreference.this.mDbFile.length() / 1024));
					db.close();
				}
			}
		});

	}

}
