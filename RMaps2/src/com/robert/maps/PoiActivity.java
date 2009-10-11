package com.robert.maps;

import org.andnav.osm.util.GeoPoint;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.robert.maps.kml.PoiManager;
import com.robert.maps.utils.Ut;

public class PoiActivity extends Activity {
	EditText mTitle, mLat, mLon, mDescr;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.poi);
		
		mTitle = (EditText) findViewById(R.id.Title);
		mLat = (EditText) findViewById(R.id.Lat);
		mLon = (EditText) findViewById(R.id.Lon);
		mDescr = (EditText) findViewById(R.id.Descr);

        Bundle extras = getIntent().getExtras();
		mTitle.setText(extras.getString("title"));
		mLat.setText(Ut.formatGeoCoord(extras.getDouble("lat")));
		mLon.setText(Ut.formatGeoCoord(extras.getDouble("lon")));
		mDescr.setText(extras.getString("descr"));
		
		((Button) findViewById(R.id.saveButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PoiManager poiManager = new PoiManager(PoiActivity.this);
				poiManager.addPoi(mTitle.getText().toString(), mDescr.getText().toString(), GeoPoint.from2DoubleString(mLat.getText().toString(), mLon.getText().toString()));
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
