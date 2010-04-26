package com.robert.maps.kml;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.robert.maps.R;
import com.robert.maps.kml.constants.PoiConstants;

public class PoiCategoryActivity extends Activity implements PoiConstants {
	EditText mTitle;
	CheckBox mHidden;
	ImageView mIcon;
	EditText mMinZoom;
	private PoiCategory mPoiCategory;
	private PoiManager mPoiManager;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.poicategory);

		if(mPoiManager == null)
			mPoiManager = new PoiManager(this);

		mTitle = (EditText) findViewById(R.id.Title);
		mHidden = (CheckBox) findViewById(R.id.Hidden);
		mIcon = (ImageView) findViewById(R.id.ImageIcon);
		mMinZoom = (EditText) findViewById(R.id.MinZoom);

        Bundle extras = getIntent().getExtras();
        if(extras == null) extras = new Bundle();
        int id = extras.getInt("id", PoiPoint.EMPTY_ID());
        
        if(id < 0){
        	mPoiCategory = new PoiCategory();
			mTitle.setText(extras.getString("title"));
			mHidden.setChecked(false);
			mIcon.setImageResource(mPoiCategory.IconId);
			mMinZoom.setText("14");
		} else {
        	mPoiCategory = mPoiManager.getPoiCategory(id);
        	
        	if(mPoiCategory == null)
        		finish();
        	
        	mTitle.setText(mPoiCategory.Title);
           	mHidden.setChecked(mPoiCategory.Hidden);
			mIcon.setImageResource(mPoiCategory.IconId);
			mMinZoom.setText(Integer.toString(mPoiCategory.MinZoom));
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
				PoiCategoryActivity.this.finish();
			}
		});
		mIcon.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				doSelectIcon();
			}
		});
	}

	protected void doSelectIcon() {
		startActivityForResult(new Intent(this, PoiIconSetActivity.class), R.id.ImageIcon);
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
		mPoiCategory.Title = mTitle.getText().toString();
		mPoiCategory.Hidden = mHidden.isChecked();
		mPoiCategory.MinZoom = Integer.parseInt(mMinZoom.getText().toString());
		
		mPoiManager.updatePoiCategory(mPoiCategory);
		finish();
		
		Toast.makeText(this, R.string.message_saved, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(resultCode == RESULT_OK){
			mPoiCategory.IconId = data.getIntExtra("iconid", R.drawable.poi);
			mIcon.setImageResource(mPoiCategory.IconId);
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}


}
