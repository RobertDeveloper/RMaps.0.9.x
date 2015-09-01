package com.robert.maps.applib.kml;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.robert.maps.R;
import com.robert.maps.applib.utils.CoordFormatter;

import org.andnav.osm.util.GeoPoint;

import java.util.Locale;

public class PoiActivity extends Activity {
	EditText mTitle, mLat, mLon, mDescr, mAlt;
	Spinner mSpinner;
	CheckBox mHidden;
	private PoiPoint mPoiPoint;
	private PoiManager mPoiManager;
	private CoordFormatter mCf;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.poi);

		if(mPoiManager == null)
			mPoiManager = new PoiManager(this);
		mCf = new CoordFormatter(this);
		
		mTitle = (EditText) findViewById(R.id.Title);
		mLat = (EditText) findViewById(R.id.Lat);
		mLon = (EditText) findViewById(R.id.Lon);
		mAlt = (EditText) findViewById(R.id.Alt);
		mDescr = (EditText) findViewById(R.id.Descr);
		mHidden = (CheckBox) findViewById(R.id.Hidden);
		
		mLat.setHint(mCf.getHint());
		mLat.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus) {
					try {
						mLat.setText(mCf.convertLat(CoordFormatter.convertTrowable(mLat.getText().toString())));
					} catch (Exception e) {
						mLat.setText("");
					}
				}
			}
		});

		mLon.setHint(mCf.getHint());
		mLon.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus) {
					try {
						mLon.setText(mCf.convertLon(CoordFormatter.convertTrowable(mLon.getText().toString())));
					} catch (Exception e) {
						mLon.setText("");
					}
				}
			}
		});

		mSpinner = (Spinner) findViewById(R.id.spinnerCategory);
		Cursor c = mPoiManager.getGeoDatabase().getPoiCategoryListCursor();
        startManagingCursor(c);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                R.layout.poicategory_spinner, //android.R.layout.simple_spinner_item, 
                c, 
                        new String[] { "name", "iconid"}, 
                        new int[] { android.R.id.text1, R.id.pic });
        adapter.setDropDownViewResource(R.layout.poicategory_spinner_dropdown);
        mSpinner.setAdapter(adapter);
		
        Bundle extras = getIntent().getExtras();
        if(extras == null) extras = new Bundle();
        int id = extras.getInt("pointid", PoiPoint.EMPTY_ID());
        
        if(id < 0){
        	mPoiPoint = new PoiPoint();
			mTitle.setText(extras.getString("title"));
			mSpinner.setSelection(0);
			mLat.setText(mCf.convertLat(extras.getDouble("lat")));
			mLon.setText(mCf.convertLon(extras.getDouble("lon")));
			mAlt.setText(String.format(Locale.UK, "%.1f", extras.getDouble("alt", 0.0)));
			mDescr.setText(extras.getString("descr"));
			mHidden.setChecked(false);
        }else{
        	mPoiPoint = mPoiManager.getPoiPoint(id);
        	
        	if(mPoiPoint == null)
        		finish();
        	
        	mTitle.setText(mPoiPoint.Title);
        	for(int pos = 0; pos < mSpinner.getCount(); pos++){
        		if(mSpinner.getItemIdAtPosition(pos) == mPoiPoint.CategoryId){
        			mSpinner.setSelection(pos);
        			break;
        		}
        	}
         	mLat.setText(mCf.convertLat(mPoiPoint.GeoPoint.getLatitude()));
        	mLon.setText(mCf.convertLon(mPoiPoint.GeoPoint.getLongitude()));
        	mAlt.setText(String.format(Locale.UK, "%.1f", mPoiPoint.Alt));
        	mDescr.setText(mPoiPoint.Descr);
        	mHidden.setChecked(mPoiPoint.Hidden);
        }
		
		((Button) findViewById(R.id.saveButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doSaveAction();
			}
		});
		((Button) findViewById(R.id.discardButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PoiActivity.this.finish();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mPoiManager.FreeDatabases();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK: {
			doSaveAction();
			return true;
		}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void doSaveAction() {
		mPoiPoint.Title = mTitle.getText().toString();
		mPoiPoint.CategoryId = (int)mSpinner.getSelectedItemId();
		mPoiPoint.Descr = mDescr.getText().toString();
		mPoiPoint.GeoPoint = GeoPoint.fromDouble(CoordFormatter.convert(mLat.getText().toString()), CoordFormatter.convert(mLon.getText().toString()));
		mPoiPoint.Hidden = mHidden.isChecked();
		try {
			mPoiPoint.Alt = Double.parseDouble(mAlt.getText().toString());
		} catch (NumberFormatException e) {
		}
		
		mPoiManager.updatePoi(mPoiPoint);
		finish();
		
		Toast.makeText(PoiActivity.this, R.string.message_saved, Toast.LENGTH_SHORT).show();
	}

}
