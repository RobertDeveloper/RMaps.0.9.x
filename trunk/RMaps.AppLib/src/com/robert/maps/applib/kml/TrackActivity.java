package com.robert.maps.applib.kml;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.robert.maps.applib.R;
import com.robert.maps.applib.kml.utils.TrackStyleDrawable;
import com.robert.maps.applib.kml.utils.TrackStylePickerDialog;
import com.robert.maps.applib.kml.utils.TrackStylePickerDialog.OnTrackStyleChangedListener;

public class TrackActivity extends Activity implements OnTrackStyleChangedListener{
	EditText mName, mDescr;
	Spinner mActivity;
	private Track mTrack;
	private PoiManager mPoiManager;
	TrackStylePickerDialog mDialog;


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
        
        findViewById(R.id.trackstyle).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mDialog = new TrackStylePickerDialog(TrackActivity.this, mTrack.Color, mTrack.Width, mTrack.ColorShadow, mTrack.ShadowRadius);
				mDialog.setOnTrackStyleChangedListener(TrackActivity.this);
				mDialog.show();
			}
		});

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
		
		final Drawable dr = new TrackStyleDrawable(mTrack.Color, mTrack.Width, mTrack.ColorShadow, mTrack.ShadowRadius);
		final Drawable[] d = {getResources().getDrawable(R.drawable.r_home_other1), dr};
		LayerDrawable ld = new LayerDrawable(d);
		((Button) findViewById(R.id.trackstyle)).setCompoundDrawablesWithIntrinsicBounds(null, null, ld, null);
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
		mTrack.Name = mName.getText().toString();
		mTrack.Descr = mDescr.getText().toString();
		mTrack.Activity = mActivity.getSelectedItemPosition();

		mPoiManager.updateTrack(mTrack);
		finish();

		Toast.makeText(TrackActivity.this, R.string.message_saved, Toast.LENGTH_SHORT).show();
	}

	public void onTrackStyleChanged(int color, int width, int colorshadow, double shadowradius) {
		mTrack.Color = color;
		mTrack.Width = width;
		mTrack.ColorShadow = colorshadow;
		mTrack.ShadowRadius = shadowradius;
		mTrack.Style = mTrack.getStyle();
		
		final Drawable dr = new TrackStyleDrawable(mTrack.Color, mTrack.Width, mTrack.ColorShadow, mTrack.ShadowRadius);
		final Drawable[] d = {getResources().getDrawable(R.drawable.r_home_other1), dr};
		LayerDrawable ld = new LayerDrawable(d);
		((Button) findViewById(R.id.trackstyle)).setCompoundDrawablesWithIntrinsicBounds(null, null, ld, null);
	}

}
