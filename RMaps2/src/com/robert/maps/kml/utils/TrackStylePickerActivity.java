package com.robert.maps.kml.utils;

import net.margaritov.preference.colorpicker.ColorPickerPanelView;
import net.margaritov.preference.colorpicker.ColorPickerView;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.robert.maps.R;
import com.robert.maps.kml.Track;

public class TrackStylePickerActivity extends Activity implements ColorPickerView.OnColorChangedListener,
		View.OnClickListener {
	
	private ColorPickerView mColorPicker;

	private ColorPickerPanelView mOldColor;
	private ColorPickerPanelView mNewColor;
	private SeekBar mWidthBar;

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
		mColorPicker.setOnColorChangedListener(this);
		mOldColor.setColor(color);
		mColorPicker.setColor(color, true);

		mColorPicker.setAlphaSliderVisible(true);
		mWidthBar.setProgress(extras.getInt(Track.WIDTH, 4));

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
	
}
