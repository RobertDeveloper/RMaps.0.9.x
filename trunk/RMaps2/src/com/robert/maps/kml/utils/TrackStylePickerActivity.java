package com.robert.maps.kml.utils;

import net.margaritov.preference.colorpicker.ColorPickerDialog;
import net.margaritov.preference.colorpicker.ColorPickerPanelView;
import net.margaritov.preference.colorpicker.ColorPickerView;

import org.andnav.osm.util.GeoPoint;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.robert.maps.R;
import com.robert.maps.kml.Track;
import com.robert.maps.tileprovider.TileSource;
import com.robert.maps.utils.Ut;
import com.robert.maps.view.MapView;

public class TrackStylePickerActivity extends Activity implements ColorPickerView.OnColorChangedListener,
		View.OnClickListener, OnSeekBarChangeListener, ColorPickerDialog.OnColorChangedListener {
	
	private SeekBar mWidthBar;
	private SeekBar mShadowRadiusBar;
	private MapView mMap;
	private TrackStyleOverlay mTrackStyleOverlay;
	private TileSource mTileSource;
	private Paint mPaint = new Paint();

	private ColorPickerPanelView mColorView;
	private ColorPickerPanelView mColorShadowView;
	private ColorPickerDialog mDialog;
	private CheckBox mAddShadowBox;
	
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
		final int colorshadow = extras.getInt(Track.COLORSHADOW, getResources().getColor(R.color.track));
		
		// To fight color banding.
		getWindow().setFormat(PixelFormat.RGBA_8888);
		
		setTitle(R.string.dialog_color_picker);
		
		mColorView = (ColorPickerPanelView) findViewById(R.id.color);
		//mColorView.setOnClickListener(this);
		mColorView.setColor(color);
		mColorView.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				mDialog = new ColorPickerDialog(TrackStylePickerActivity.this, TrackStylePickerActivity.this.mColorView.getColor());
				mDialog.setOnColorChangedListener(TrackStylePickerActivity.this);
				mDialog.setAlphaSliderVisible(true);
//				if (state != null) {
//					mDialog.onRestoreInstanceState(state);
//				}
				mDialog.show();
			}
		});

		mColorShadowView = (ColorPickerPanelView) findViewById(R.id.colorshadow);
		//mColorShadowView.setOnClickListener(this);
		mColorShadowView.setColor(colorshadow);
		mColorShadowView.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				mDialog = new ColorPickerDialog(TrackStylePickerActivity.this, TrackStylePickerActivity.this.mColorShadowView.getColor());
				mDialog.setOnColorChangedListener(TrackStylePickerActivity.this.mColorShadowListiner);
				mDialog.setAlphaSliderVisible(true);
//				if (state != null) {
//					mDialog.onRestoreInstanceState(state);
//				}
				mDialog.show();
			}
		});

		mWidthBar = (SeekBar) findViewById(R.id.width);
		mShadowRadiusBar = (SeekBar) findViewById(R.id.shadowradius);
		
		mWidthBar.setProgress(extras.getInt(Track.WIDTH, 4));
		final double shadowradius = extras.getDouble(Track.SHADOWRADIUS, 4);
		mShadowRadiusBar.setProgress((int)(shadowradius * 10));
		
 		mMap = (MapView) findViewById(R.id.map);
		mMap.setLongClickable(false);

		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(extras.getInt(Track.WIDTH, 4));
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(color);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setShadowLayer((float) shadowradius, 0, 0, colorshadow);
		
		mTrackStyleOverlay = new TrackStyleOverlay();
		mTrackStyleOverlay.setPaint(mPaint);
		mMap.getOverlays().add(mTrackStyleOverlay);

		mWidthBar.setOnSeekBarChangeListener(this);
		mShadowRadiusBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mPaint.setShadowLayer(progress / 10, 0, 0, mColorShadowView.getColor());
				mMap.invalidate();
			}
		});
		
		((Button) findViewById(R.id.saveButton)).setOnClickListener(this);
		((Button) findViewById(R.id.discardButton)).setOnClickListener(this);
		mAddShadowBox = (CheckBox) findViewById(R.id.add_shadow_box);
		mAddShadowBox.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				setShadowVisible(mAddShadowBox.isChecked() ? View.VISIBLE : View.INVISIBLE);
			}
		});
		mAddShadowBox.setChecked(shadowradius > 0);
		setShadowVisible(mAddShadowBox.isChecked() ? View.VISIBLE : View.INVISIBLE);
	}
	
	void setShadowVisible(final int visible) {
		findViewById(R.id.shadow_color).setVisibility(visible);
		findViewById(R.id.text_shadow_width).setVisibility(visible);
		findViewById(R.id.shadowradius).setVisibility(visible);
		if(visible == View.INVISIBLE)
			mShadowRadiusBar.setProgress(0);
	}
	
	ColorPickerDialog.OnColorChangedListener mColorShadowListiner = new ColorPickerDialog.OnColorChangedListener() {
		
		public void onColorChanged(int color) {
			mColorShadowView.setColor(color);
			mPaint.setAlpha(Color.alpha(color));
			Ut.dd("Color.alpha(color)="+Color.alpha(color));
			mPaint.setShadowLayer(mShadowRadiusBar.getProgress() / 10, 0, 0, color);
			mMap.invalidate();
		}
	};

	public void onClick(View v) {
		if (v.getId() == R.id.saveButton) {
			setResult(RESULT_OK, (new Intent())
					.putExtra(Track.COLOR, mColorView.getColor())
					.putExtra(Track.COLORSHADOW, mColorShadowView.getColor())
					.putExtra(Track.WIDTH, mWidthBar.getProgress())
					.putExtra(Track.SHADOWRADIUS, ((double)mShadowRadiusBar.getProgress()) / 10)
					);
		}
		finish();
	}
	
	public void onColorChanged(int color) {
		mColorView.setColor(color);
		
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
