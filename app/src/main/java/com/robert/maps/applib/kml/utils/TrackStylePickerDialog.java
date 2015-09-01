package com.robert.maps.applib.kml.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.robert.maps.R;
import com.robert.maps.applib.tileprovider.TileSource;
import com.robert.maps.applib.utils.Ut;
import com.robert.maps.applib.view.MapView;

import net.margaritov.preference.colorpicker.ColorPickerDialog;
import net.margaritov.preference.colorpicker.ColorPickerPanelView;
import net.margaritov.preference.colorpicker.ColorPickerView;

import org.andnav.osm.util.GeoPoint;

public class TrackStylePickerDialog extends Dialog implements ColorPickerView.OnColorChangedListener,
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
	private OnTrackStyleChangedListener mListener;
	
	public interface OnTrackStyleChangedListener {
		public void onTrackStyleChanged(int color, int width, int colorshadow, double shadowradius);
	}
	
	public void setOnTrackStyleChangedListener(OnTrackStyleChangedListener listener) {
		mListener = listener;
	}
	
	public TrackStylePickerDialog(Context context, int color, int width, int colorshadow, double shadowradius) {
		super(context);
		
		this.setContentView(R.layout.track_style_picker);

		setTitle(R.string.track_style_picker);
		
		mColorView = (ColorPickerPanelView) findViewById(R.id.color);
		mColorView.setColor(color);
		mColorView.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				mDialog = new ColorPickerDialog(getContext(), TrackStylePickerDialog.this.mColorView.getColor());
				mDialog.setOnColorChangedListener(TrackStylePickerDialog.this);
				mDialog.setAlphaSliderVisible(true);
				mDialog.show();
			}
		});

		mColorShadowView = (ColorPickerPanelView) findViewById(R.id.colorshadow);
		mColorShadowView.setColor(colorshadow);
		mColorShadowView.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				mDialog = new ColorPickerDialog(getContext(), TrackStylePickerDialog.this.mColorShadowView.getColor());
				mDialog.setOnColorChangedListener(TrackStylePickerDialog.this.mColorShadowListiner);
				mDialog.setAlphaSliderVisible(true);
				mDialog.show();
			}
		});

		mWidthBar = (SeekBar) findViewById(R.id.width);
		mShadowRadiusBar = (SeekBar) findViewById(R.id.shadowradius);
		
		mWidthBar.setProgress(width);
		mShadowRadiusBar.setProgress((int)(shadowradius * 10));
		
 		mMap = (MapView) findViewById(R.id.map);
		mMap.setLongClickable(false);

		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(width);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(color);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setShadowLayer((float) shadowradius, 0, 0, colorshadow);
		
		mTrackStyleOverlay = new TrackStyleOverlay();
		mTrackStyleOverlay.setPaint(mPaint);
		mMap.getOverlays().add(mTrackStyleOverlay);
		final SharedPreferences pref = getContext().getSharedPreferences("MapName", Activity.MODE_PRIVATE);
		
		if(mTileSource != null)
			mTileSource.Free();

		try {
			mTileSource = new TileSource(getContext(), pref.getString(MapView.MAPNAME, TileSource.MAPNIK));
		} catch (Exception e) {
			e.printStackTrace();
		}
		mMap.setTileSource(mTileSource);
 		mMap.getController().setZoom(pref.getInt("ZoomLevel", 0));
 		mMap.getController().setCenter(new GeoPoint(pref.getInt("Latitude", 0), pref.getInt("Longitude", 0)));

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
		mAddShadowBox.setOnClickListener(new View.OnClickListener() {
			
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
			if(mListener != null)
				mListener.onTrackStyleChanged(mColorView.getColor(), mWidthBar.getProgress(), mColorShadowView.getColor(), ((double)mShadowRadiusBar.getProgress()) / 10);
		}
		dismiss();
	}
	
	public void onColorChanged(int color) {
		mColorView.setColor(color);
		
		mPaint.setColor(color);
		
		mTrackStyleOverlay.setPaint(mPaint);
		mMap.invalidate();
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
