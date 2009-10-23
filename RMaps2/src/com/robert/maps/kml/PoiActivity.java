package com.robert.maps.kml;

import org.andnav.osm.util.GeoPoint;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.robert.maps.R;
import com.robert.maps.utils.Ut;

public class PoiActivity extends Activity {
	EditText mTitle, mLat, mLon, mDescr;
	Spinner mSpinner;
	private PoiPoint mPoiPoint;
	private PoiManager mPoiManager;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.poi);

		if(mPoiManager == null)
			mPoiManager = new PoiManager(this);

		mTitle = (EditText) findViewById(R.id.Title);
		mLat = (EditText) findViewById(R.id.Lat);
		mLon = (EditText) findViewById(R.id.Lon);
		mDescr = (EditText) findViewById(R.id.Descr);

		mSpinner = (Spinner) findViewById(R.id.spinnerCategory);
		Cursor c = mPoiManager.getGeoDatabase().getPoiCategoryListCursor();
        startManagingCursor(c);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, c, 
                        new String[] { "name"}, 
                        new int[] { android.R.id.text1 });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
		
        Bundle extras = getIntent().getExtras();
        if(extras == null) extras = new Bundle();
        int id = extras.getInt("pointid", PoiPoint.EMPTY_ID());
        
        if(id < 0){
        	mPoiPoint = new PoiPoint();
			mTitle.setText(extras.getString("title"));
			mSpinner.setSelection(0);
			mLat.setText(Ut.formatGeoCoord(extras.getDouble("lat")));
			mLon.setText(Ut.formatGeoCoord(extras.getDouble("lon")));
			mDescr.setText(extras.getString("descr"));
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
         	mLat.setText(Ut.formatGeoCoord(mPoiPoint.GeoPoint.getLatitude()));
        	mLon.setText(Ut.formatGeoCoord(mPoiPoint.GeoPoint.getLongitude()));
        	mDescr.setText(mPoiPoint.Descr);
        }
		
		((Button) findViewById(R.id.saveButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mPoiPoint.Title = mTitle.getText().toString();
				mPoiPoint.CategoryId = (int)mSpinner.getSelectedItemId();
				mPoiPoint.Descr = mDescr.getText().toString();
				mPoiPoint.GeoPoint = GeoPoint.from2DoubleString(mLat.getText().toString(), mLon.getText().toString());
				
				mPoiManager.updatePoi(mPoiPoint);
				PoiActivity.this.finish();
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
		mPoiManager.FreeDatabases();
		super.onDestroy();
	}

}
