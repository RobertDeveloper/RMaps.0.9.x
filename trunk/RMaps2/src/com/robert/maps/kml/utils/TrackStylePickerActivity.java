package com.robert.maps.kml.utils;

import net.margaritov.preference.colorpicker.ColorPickerPanelView;
import net.margaritov.preference.colorpicker.ColorPickerView;

import org.andnav.osm.util.GeoPoint;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.robert.maps.R;
import com.robert.maps.kml.Track;
import com.robert.maps.tileprovider.TileSource;
import com.robert.maps.view.MapView;

public class TrackStylePickerActivity extends Activity implements ColorPickerView.OnColorChangedListener,
		View.OnClickListener, OnSeekBarChangeListener {
	
	private ColorPickerView mColorPicker;

	private ColorPickerPanelView mOldColor;
	private ColorPickerPanelView mNewColor;
	private SeekBar mWidthBar;
	private MapView mMap;
	private TrackStyleOverlay mTrackStyleOverlay;
	private TileSource mTileSource;
	private Paint mPaint = new Paint();

	public interface OnColorChangedListener {
		public void onColorChanged(int color);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.track_style_picker);

        Bundle extras = getIntent().getExtras();
        if(extras == null) extras = new Bundle();
		final int color = extras.getInt(Track.COLOR, getResources().getColor(R.color.track));
		
		// To fight color banding.
		getWindow().setFormat(PixelFormat.RGBA_8888);
		
		setTitle(R.string.dialog_color_picker);
		
		mColorPicker = (ColorPickerView) findViewById(R.id.color_picker_view);
		mOldColor = (ColorPickerPanelView) findViewById(R.id.old_color_panel);
		mNewColor = (ColorPickerPanelView) findViewById(R.id.new_color_panel);
		mWidthBar = (SeekBar) findViewById(R.id.width);
		
		((LinearLayout) mOldColor.getParent()).setPadding(
			Math.round(mColorPicker.getDrawingOffset()), 
			0, 
			Math.round(mColorPicker.getDrawingOffset()), 
			0
		);	
		
		mOldColor.setOnClickListener(this);
		mNewColor.setOnClickListener(this);
		mOldColor.setColor(color);
		mColorPicker.setColor(color, true);

		mColorPicker.setAlphaSliderVisible(true);
		mWidthBar.setProgress(extras.getInt(Track.WIDTH, 4));
		
 		mMap = (MapView) findViewById(R.id.map);
		mMap.setLongClickable(false);

		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(extras.getInt(Track.WIDTH, 4));
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(color);
		mTrackStyleOverlay = new TrackStyleOverlay();
		mTrackStyleOverlay.setPaint(mPaint);
		mMap.getOverlays().add(mTrackStyleOverlay);

		mColorPicker.setOnColorChangedListener(this);
		mWidthBar.setOnSeekBarChangeListener(this);
	}

	public void onClick(View v) {
		if (v.getId() == R.id.new_color_panel) {
			setResult(RESULT_OK, (new Intent())
					.putExtra(Track.COLOR, mNewColor.getColor())
					.putExtra(Track.WIDTH, mWidthBar.getProgress())
					);
		}
		finish();
	}
	
	public void onColorChanged(int color) {
		mNewColor.setColor(color);
		
		mPaint.setColor(color);
		mTrackStyleOverlay.setPaint(mPaint);
		mMap.invalidate();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
//		Bundle state = super.onSaveInstanceState();
//		state.putInt("old_color", mOldColor.getColor());
//		state.putInt("new_color", mNewColor.getColor());
//		return state;
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		mOldColor.setColor(savedInstanceState.getInt("old_color"));
		mColorPicker.setColor(savedInstanceState.getInt("new_color"), true);
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		final SharedPreferences pref = getSharedPreferences("MapName", Activity.MODE_PRIVATE);
		
		if(mTileSource != null)
			mTileSource.Free();

		try {
			mTileSource = new TileSource(this, pref.getString(MapView.MAPNAME, TileSource.MAPNIK));
		} catch (Exception e) {
			e.printStackTrace();
		}
		mMap.setTileSource(mTileSource);
 		mMap.getController().setZoom(pref.getInt("ZoomLevel", 0));
 		mMap.getController().setCenter(new GeoPoint(pref.getInt("Latitude", 0), pref.getInt("Longitude", 0)));

 		super.onResume();
	}

	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		mPaint.setStrokeWidth(progress);
		mTrackStyleOverlay.setPaint(mPaint);
		mMap.invalidate();
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
	}
	
}
