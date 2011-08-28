// Created by plusminus on 22:01:11 - 29.09.2008
package com.robert.maps.overlays;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.TypeConverter;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
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
	private Drawable mArrow = null;
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
	private boolean mNeedCrosshair;
	private final Paint mPaintCross = new Paint();
	private final static int mCrossSize = 7;

	// ===========================================================
	// Constructors
	// ===========================================================

	public MyLocationOverlay(final Context ctx){
		mCtx = ctx.getApplicationContext();

		mPaintAccurasyFill = new Paint();
		mPaintAccurasyFill.setAntiAlias(true);
		mPaintAccurasyFill.setStrokeWidth(2);
		mPaintAccurasyFill.setStyle(Paint.Style.FILL);
		mPaintAccurasyFill.setColor(0x4490B8D8);

		mPaintAccurasyBorder = new Paint(mPaintAccurasyFill);
		mPaintAccurasyBorder.setStyle(Paint.Style.STROKE);
		mPaintAccurasyBorder.setColor(0xFF90B8D8);

		mPaintCross.setAntiAlias(true);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		mPrefAccuracy = Integer.parseInt(pref.getString("pref_accuracy", "1").replace("\"", ""));
		mNeedCrosshair = pref.getBoolean("pref_crosshair", true);
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

	private boolean getArrowIcon(){
		if(mArrow == null)
			try {
				this.mArrow = mCtx.getResources().getDrawable(R.drawable.arrow);
			} catch (Exception e) {
				mArrow = null;
			} catch (OutOfMemoryError e) {
				mArrow = null;
			}

		return mArrow == null ? false : true;
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
			
			if(OpenStreetMapViewConstants.DEBUGMODE){
//				mSpeed = 10;
				mAccuracy = 50;
			}

			if (mPrefAccuracy != 0
					&& ((mAccuracy > 0 && mPrefAccuracy == 1) || (mPrefAccuracy > 1 && mAccuracy >= mPrefAccuracy))) {
				int PixelRadius = (int) (osmv.mTouchScale * mAccuracy / ((float)METER_IN_PIXEL / (1 << osmv.getZoomLevel())));
				c.drawCircle(screenCoords.x, screenCoords.y, PixelRadius, mPaintAccurasyFill);
				c.drawCircle(screenCoords.x, screenCoords.y, PixelRadius, mPaintAccurasyBorder);
			}

			c.save();
			if (mSpeed == 0) {
				c.rotate(osmv.getBearing(), screenCoords.x, screenCoords.y);
				if(getPersonIcon()){
					final Rect r = new Rect(screenCoords.x - (int)(PERSON_ICON2.getWidth()/2)
							, screenCoords.y - (int)(PERSON_HOTSPOT.y * PERSON_ICON2.getHeight() / 48)
							, screenCoords.x - (int)(PERSON_ICON2.getWidth()/2) + PERSON_ICON2.getWidth()
							, screenCoords.y - (int)(PERSON_HOTSPOT.y * PERSON_ICON2.getHeight() / 48) + PERSON_ICON2.getHeight()
							);
					c.drawBitmap(PERSON_ICON2, null, r, this.mPaint);
					//final Rect r = new Rect(screenCoords.x - PERSON_HOTSPOT.x, screenCoords.y - PERSON_HOTSPOT.y, screenCoords.x - PERSON_HOTSPOT.x + 48, screenCoords.y - PERSON_HOTSPOT.y + 48);
					//c.drawBitmap(PERSON_ICON2, null, r, this.mPaint);
//					c.drawBitmap(PERSON_ICON2, screenCoords.x - PERSON_HOTSPOT.x * PERSON_ICON2.getDensity() / 160
//							, screenCoords.y - PERSON_HOTSPOT.y * PERSON_ICON2.getDensity() / 160
//							, this.mPaint);
				};
//				mStop.setBounds(screenCoords.x - mArrow.getMinimumWidth() / 2, screenCoords.y
//						- mArrow.getMinimumHeight() / 2 - 3, screenCoords.x + mArrow.getMinimumWidth() / 2, screenCoords.y
//						+ mArrow.getMinimumHeight() / 2 - 3);
//				mStop.draw(c);
			} else {
				if(getArrowIcon()){
					c.rotate(mBearing, screenCoords.x, screenCoords.y);
					mArrow.setBounds(screenCoords.x - mArrow.getMinimumWidth() / 2, screenCoords.y
							- mArrow.getMinimumHeight() / 2, screenCoords.x + mArrow.getMinimumWidth() / 2, screenCoords.y
							+ mArrow.getMinimumHeight() / 2);
					mArrow.draw(c);
				}
			}
			c.restore();

//			c.drawText("Speed=" + mSpeed, ((float) screenCoords.x + 30), ((float) screenCoords.y + 30), paint);
//			c.drawText("Accuracy=" + mAccuracy, ((float) screenCoords.x + 30), ((float) screenCoords.y + 45), paint);
		}
		
		if(mNeedCrosshair){
			final int x = osmv.getWidth() / 2;
			final int y = osmv.getHeight() / 2;
			c.drawLine(x - mCrossSize, y, x + mCrossSize, y, this.mPaintCross);
			c.drawLine(x, y - mCrossSize, x, y + mCrossSize, this.mPaintCross);
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
