package com.robert.maps.overlays;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.TypeConverter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.preference.PreferenceManager;

import com.robert.maps.R;
import com.robert.maps.view.TileView;
import com.robert.maps.view.TileView.OpenStreetMapViewProjection;
import com.robert.maps.view.TileViewOverlay;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class MyLocationOverlay extends TileViewOverlay {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected final Paint mPaint = new Paint();

	protected Bitmap PERSON_ICON2 = null;
	private Bitmap mArrow = null;

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
	private Location mLoc;

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
				this.mArrow = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.arrow);
			} catch (Exception e) {
				mArrow = null;
			} catch (OutOfMemoryError e) {
				mArrow = null;
			}

		return mArrow == null ? false : true;
	}
	
	public GeoPoint getLastGeoPoint() {
		return mLocation;
	}
	
	public Location getLastLocation() {
		return mLoc;
	}

	public void setLocation(final Location loc){
		this.mLocation = TypeConverter.locationToGeoPoint(loc);
		this.mAccuracy = loc.getAccuracy();
		this.mBearing = loc.getBearing();
		this.mSpeed = loc.getSpeed();
		mLoc = loc;
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
	protected void onDrawFinished(Canvas c, TileView osmv) {
		return;
	}

	@Override
	public void onDraw(final Canvas c, final TileView osmv) {
		if(this.mLocation != null){
			final OpenStreetMapViewProjection pj = osmv.getProjection();
			final Point screenCoords = new Point();
			pj.toPixels(this.mLocation, screenCoords);
			

			if (mPrefAccuracy != 0
					&& mSpeed <= 0.278
					&& ((mAccuracy > 0 && mPrefAccuracy == 1) || (mPrefAccuracy > 1 && mAccuracy >= mPrefAccuracy))) {
				int PixelRadius = (int) (osmv.mTouchScale * mAccuracy / ((float)METER_IN_PIXEL / (1 << osmv.getZoomLevel())));
				c.drawCircle(screenCoords.x, screenCoords.y, PixelRadius, mPaintAccurasyFill);
				c.drawCircle(screenCoords.x, screenCoords.y, PixelRadius, mPaintAccurasyBorder);
			}

			c.save();
			if (mSpeed <= 0.278) {
				c.rotate(osmv.getBearing(), screenCoords.x, screenCoords.y);
				if(getPersonIcon()){
					c.drawBitmap(PERSON_ICON2, screenCoords.x - (int)(PERSON_ICON2.getWidth()/2), screenCoords.y - (int)(PERSON_ICON2.getHeight() / 2), mPaint);
				};
			} else {
				if(getArrowIcon()){
					c.rotate(mBearing, screenCoords.x, screenCoords.y);
					c.drawBitmap(mArrow, screenCoords.x - (int)(mArrow.getWidth()/2), screenCoords.y - (int)(mArrow.getHeight() / 2), mPaint);
				}
			}
			c.restore();

		}
		
		if(mNeedCrosshair){
			final int x = osmv.getWidth() / 2;
			final int y = osmv.getHeight() / 2;
			c.drawLine(x - mCrossSize, y, x + mCrossSize, y, this.mPaintCross);
			c.drawLine(x, y - mCrossSize, x, y + mCrossSize, this.mPaintCross);
		}
	}

}
