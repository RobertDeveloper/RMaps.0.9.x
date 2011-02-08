// Created by plusminus on 22:01:11 - 29.09.2008
package com.robert.maps.overlays;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.TypeConverter;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.preference.PreferenceManager;

import com.robert.maps.R;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class MyLocationOverlay extends OpenStreetMapViewOverlay {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected final Paint mPaint = new Paint();

	protected Bitmap PERSON_ICON2 = null;
	private Drawable mArrow;
	//private Drawable mStop;
	/** Coordinates the feet of the person are located. */
	protected final android.graphics.Point PERSON_HOTSPOT = new android.graphics.Point(24,39);

	private Context mCtx;
	protected GeoPoint mLocation;
	private float mAccuracy;
	private int mPrefAccuracy;
	private float mBearing;
	private float mSpeed;
	private int METER_IN_PIXEL = 156412;
	private Paint mPaintAccurasyFill;
	private Paint mPaintAccurasyBorder;

	// ===========================================================
	// Constructors
	// ===========================================================

	public MyLocationOverlay(final Context ctx){
		mCtx = ctx;
		this.mArrow = ctx.getResources().getDrawable(R.drawable.arrow);
		//this.mStop = ctx.getResources().getDrawable(R.drawable.arrow_stop);

		mPaintAccurasyFill = new Paint();
		mPaintAccurasyFill.setAntiAlias(true);
		mPaintAccurasyFill.setStrokeWidth(2);
		mPaintAccurasyFill.setStyle(Paint.Style.FILL);
		mPaintAccurasyFill.setColor(0x4490B8D8);

		mPaintAccurasyBorder = new Paint(mPaintAccurasyFill);
		mPaintAccurasyBorder.setStyle(Paint.Style.STROKE);
		mPaintAccurasyBorder.setColor(0xFF90B8D8);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		mPrefAccuracy = Integer.parseInt(pref.getString("pref_accuracy", "1").replace("\"", ""));
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	private boolean getPersonIcon(){
		if(PERSON_ICON2 == null)
			try {
				this.PERSON_ICON2 = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.person);
			} catch (Exception e) {
				PERSON_ICON2 = null;
			} catch (OutOfMemoryError e) {
				PERSON_ICON2 = null;
			}

		return PERSON_ICON2 == null ? false : true;
	}

	public void setLocation(final Location loc){
		this.mLocation = TypeConverter.locationToGeoPoint(loc);
		this.mAccuracy = loc.getAccuracy();
		this.mBearing = loc.getBearing();
		this.mSpeed = loc.getSpeed();
	}

	public void setLocation(final GeoPoint geopoint){
		this.mLocation = geopoint;
		this.mAccuracy = 0;
		this.mBearing = 0;
		this.mSpeed = 0;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
		return;
	}

	@Override
	public void onDraw(final Canvas c, final OpenStreetMapView osmv) {
		if(this.mLocation != null){
			final OpenStreetMapViewProjection pj = osmv.getProjection();
			final Point screenCoords = new Point();
			pj.toPixels(this.mLocation, screenCoords);

			if (mPrefAccuracy != 0
					&& ((mAccuracy > 0 && mPrefAccuracy == 1) || (mPrefAccuracy > 1 && mAccuracy >= mPrefAccuracy))) {
				int PixelRadius = (int) (osmv.mTouchScale * mAccuracy / ((float)METER_IN_PIXEL / (1 << osmv.getZoomLevel())));
				c.drawCircle(screenCoords.x, screenCoords.y, PixelRadius, mPaintAccurasyFill);
				c.drawCircle(screenCoords.x, screenCoords.y, PixelRadius, mPaintAccurasyBorder);
			}

			c.save();
			if (mSpeed == 0) {
				c.rotate(osmv.getBearing(), screenCoords.x, screenCoords.y);
				if(getPersonIcon())
					c.drawBitmap(PERSON_ICON2, screenCoords.x - PERSON_HOTSPOT.x, screenCoords.y - PERSON_HOTSPOT.y,
							this.mPaint);
//				mStop.setBounds(screenCoords.x - mArrow.getMinimumWidth() / 2, screenCoords.y
//						- mArrow.getMinimumHeight() / 2 - 3, screenCoords.x + mArrow.getMinimumWidth() / 2, screenCoords.y
//						+ mArrow.getMinimumHeight() / 2 - 3);
//				mStop.draw(c);
			} else {
				c.rotate(mBearing, screenCoords.x, screenCoords.y);
				mArrow.setBounds(screenCoords.x - mArrow.getMinimumWidth() / 2, screenCoords.y
						- mArrow.getMinimumHeight() / 2, screenCoords.x + mArrow.getMinimumWidth() / 2, screenCoords.y
						+ mArrow.getMinimumHeight() / 2);
				mArrow.draw(c);
			}
			c.restore();

//			c.drawText("Speed=" + mSpeed, ((float) screenCoords.x + 30), ((float) screenCoords.y + 30), paint);
//			c.drawText("Accuracy=" + mAccuracy, ((float) screenCoords.x + 30), ((float) screenCoords.y + 45), paint);
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
