package com.robert.maps;

import org.andnav.osm.util.GeoPoint;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.PoiPoint;
import com.robert.maps.utils.Ut;

public class PoiActivity extends Activity {
	EditText mTitle, mLat, mLon, mDescr;
	private PoiPoint mPoiPoint;
	private PoiManager mPoiManager;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.poi);

		mPoiManager = new PoiManager(this);

		mTitle = (EditText) findViewById(R.id.Title);
		mLat = (EditText) findViewById(R.id.Lat);
		mLon = (EditText) findViewById(R.id.Lon);
		mDescr = (EditText) findViewById(R.id.Descr);

        Bundle extras = getIntent().getExtras();
        int id = extras.getInt("pointid", PoiPoint.EMPTY_ID());
        
        if(id < 0){
        	mPoiPoint = new PoiPoint();
			mTitle.setText(extras.getString("title"));
			mLat.setText(Ut.formatGeoCoord(extras.getDouble("lat")));
			mLon.setText(Ut.formatGeoCoord(extras.getDouble("lon")));
			mDescr.setText(extras.getString("descr"));
        }else{
        	mPoiPoint = mPoiManager.getPoiPoint(id);
        	
        	if(mPoiPoint == null)
        		finish();
        	
        	mTitle.setText(mPoiPoint.Title);
        	mLat.setText(Ut.formatGeoCoord(mPoiPoint.GeoPoint.getLatitude()));
        	mLon.setText(Ut.formatGeoCoord(mPoiPoint.GeoPoint.getLongitude()));
        	mDescr.setText(mPoiPoint.Descr);
        }
		
		((Button) findViewById(R.id.saveButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mPoiPoint.Title = mTitle.getText().toString();
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
}
