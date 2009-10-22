package com.robert.maps.kml;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.robert.maps.R;
import com.robert.maps.kml.constants.PoiConstants;

public class PoiCategoryActivity extends Activity implements PoiConstants {
	EditText mTitle;
	private PoiCategory mPoiCategory;
	private PoiManager mPoiManager;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.poicategory);

		if(mPoiManager == null)
			mPoiManager = new PoiManager(this);

		mTitle = (EditText) findViewById(R.id.Title);

        Bundle extras = getIntent().getExtras();
        if(extras == null) extras = new Bundle();
        int id = extras.getInt("id", PoiPoint.EMPTY_ID());
        
        if(id < 0){
        	mPoiCategory = new PoiCategory();
			mTitle.setText(extras.getString("title"));
        }else{
        	mPoiCategory = mPoiManager.getPoiCategory(id);
        	
        	if(mPoiCategory == null)
        		finish();
        	
        	mTitle.setText(mPoiCategory.Title);
        }
		
		((Button) findViewById(R.id.saveButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mPoiCategory.Title = mTitle.getText().toString();
				
				mPoiManager.updatePoiCategory(mPoiCategory);
				PoiCategoryActivity.this.finish();
			}
		});
		((Button) findViewById(R.id.discardButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PoiCategoryActivity.this.finish();
			}
		});
	}

}
