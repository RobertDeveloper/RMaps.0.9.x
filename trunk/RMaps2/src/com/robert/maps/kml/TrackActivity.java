package com.robert.maps.kml;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.robert.maps.R;

public class TrackActivity extends Activity {
	EditText mName, mDescr;
	Spinner mActivity;
	private Track mTrack;
	private PoiManager mPoiManager;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.track);

		if(mPoiManager == null)
			mPoiManager = new PoiManager(this);

		mName = (EditText) findViewById(R.id.Name);
		mDescr = (EditText) findViewById(R.id.Descr);
		mActivity = (Spinner) findViewById(R.id.Activity);
		Cursor c = mPoiManager.getGeoDatabase().getActivityListCursor();
        startManagingCursor(c);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, c, 
                        new String[] { "name" }, 
                        new int[] { android.R.id.text1 });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mActivity.setAdapter(adapter);

        Bundle extras = getIntent().getExtras();
        if(extras == null) extras = new Bundle();
        int id = extras.getInt("id", PoiPoint.EMPTY_ID());

        if(id < 0){
        	mTrack = new Track();
			mName.setText(extras.getString("name"));
			mDescr.setText(extras.getString("descr"));
			mActivity.setSelection(0);
        }else{
        	mTrack = mPoiManager.getTrack(id);

        	if(mTrack == null)
        		finish();

        	mName.setText(mTrack.Name);
        	mDescr.setText(mTrack.Descr);
			mActivity.setSelection(mTrack.Activity);
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
				TrackActivity.this.finish();
			}
		});
	}

	@Override
	protected void onDestroy() {
		mPoiManager.FreeDatabases();
		super.onDestroy();
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
		mTrack.Name = mName.getText().toString();
		mTrack.Descr = mDescr.getText().toString();
		mTrack.Activity = mActivity.getSelectedItemPosition();

		mPoiManager.updateTrack(mTrack);
		finish();

		Toast.makeText(TrackActivity.this, R.string.message_saved, Toast.LENGTH_SHORT).show();
	}

}
