package com.robert.maps.applib.view;

import java.util.ArrayList;
import java.util.List;

import org.andnav.osm.util.BoundingBoxE6;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.util.Util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.robert.maps.applib.overlays.TileOverlay;
import com.robert.maps.applib.reflection.OnExGestureListener;
import com.robert.maps.applib.reflection.RGestureHelper;
import com.robert.maps.applib.reflection.VerGestureDetector;
import com.robert.maps.applib.reflection.VerScaleGestureDetector;
import com.robert.maps.applib.tileprovider.TileSource;

public class TileView extends View {
	private static final int LATITUDE = 0;
	private static final int LONGITUDE = 1;

	public int mLatitudeE6 = 0, mLongitudeE6 = 0;
	private double mOffsetLat, mOffsetLon;
	private int mZoom = 0;
	private float mBearing = 0;
	final Paint mPaint = new Paint();
	final Matrix mMatrixBearing = new Matrix();
	final Rect mRectDraw = new Rect();
	public final boolean mDrawTileGrid;
	
	private boolean mStopProcessing;
	private boolean mSetOffsetMode;
	
	public double mTouchScale = 1;
	
	private TileSource mTileSource;
	//private TileMapHandler mTileMapHandler = new TileMapHandler();
	protected final List<TileViewOverlay> mOverlays = new ArrayList<TileViewOverlay>();
	private TileOverlay mTileOverlay;
	
	private GestureDetector mDetector = VerGestureDetector.newInstance().getGestureDetector(getContext(), new TouchListener()); 
	private VerScaleGestureDetector mScaleDetector = VerScaleGestureDetector.newInstance(getContext(), new ScaleListener());
	
	private class ScaleListener implements VerScaleGestureDetector.OnGestureListener {

		public void onScale(double aScaleFactor) {
			mTouchScale = aScaleFactor;
			if(mMoveListener != null)
				mMoveListener.onZoomDetected();
			
			postInvalidate();
		}

		public void onScaleEnd() {
			int zoom = 0;
			if(mTouchScale > 1)
				zoom = getZoomLevel()+(int)Math.round(mTouchScale)-1;
			else
				zoom = getZoomLevel()-(int)Math.round(1/mTouchScale)+1;
			
			mTouchScale = 1;
			setZoomLevel(zoom);
		}
		
	}
	
	private class TouchListener implements OnExGestureListener {
		public boolean onDown(MotionEvent e) {
			for (TileViewOverlay osmvo : mOverlays) {
				if(osmvo.onDown(e, TileView.this))
					break;
			}

			return true;
		}

		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}

		public boolean onSingleTapConfirmed(MotionEvent e) {
			for (TileViewOverlay osmvo : mOverlays)
				if (osmvo.onSingleTapUp(e, TileView.this)) {
					invalidate();
					return true;
				}
			
			invalidate();
			return false;
		}

		public void onLongPress(MotionEvent e) {
			int ret = 0;
			for (TileViewOverlay osmvo : mOverlays) {
				ret = osmvo.onLongPress(e, TileView.this);
				if(ret == 1) {
					break;
				} else if(ret == 2) {
					return;
				}
			}
			
			showContextMenu();
		}

		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			for (TileViewOverlay osmvo : mOverlays) {
				if(osmvo.onScroll(e1, e2, distanceX, distanceY, TileView.this))
					return false;
			}

			final float aRotateToAngle = 360 - mBearing;
			final int viewWidth_2 = TileView.this.getWidth() / 2;
			final int viewHeight_2 = TileView.this.getHeight() / 2;
			final int TouchMapOffsetX = (int) (Math.sin(Math.toRadians(aRotateToAngle)) * (distanceY / mTouchScale))
					+ (int) (Math.cos(Math.toRadians(aRotateToAngle)) * (distanceX / mTouchScale));
			final int TouchMapOffsetY = (int) (Math.cos(Math.toRadians(aRotateToAngle)) * (distanceY / mTouchScale))
					- (int) (Math.sin(Math.toRadians(aRotateToAngle)) * (distanceX / mTouchScale));
			final GeoPoint newCenter = TileView.this.getProjection().fromPixels(viewWidth_2 + TouchMapOffsetX,
					viewHeight_2 + TouchMapOffsetY);
			if(mSetOffsetMode && ((RGestureHelper) mDetector).getPointerCount(e2) == 1) {
				mOffsetLat = mOffsetLat + (newCenter.getLatitudeE6() - mLatitudeE6) / 1E6;
				mOffsetLon = mOffsetLon + (newCenter.getLongitudeE6() - mLongitudeE6) / 1E6;
				mTileOverlay.setOffset(mOffsetLat, mOffsetLon);
				TileView.this.postInvalidate();
			} else {
				TileView.this.setMapCenter(newCenter);
			}
			
			if (mMoveListener != null)
				mMoveListener.onMoveDetected();
			
			return false;
		}

		public boolean onDoubleTap(MotionEvent e) {
			if (mBearing != 0) {
				mBearing = 0;
			} else {
				final GeoPoint newCenter = TileView.this.getProjection().fromPixels(e.getX(), e.getY());
				setMapCenter(newCenter);

				setZoomLevel(getZoomLevel() + 1);
			}

			return true;
		}

		public void onShowPress(MotionEvent e) {
		}

		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return false;
		}

		public void onUp(MotionEvent e) {
			for (TileViewOverlay osmvo : mOverlays) {
				osmvo.onUp(e, TileView.this);
			}
		}

		public boolean onDoubleTapEvent(MotionEvent e) {
			return false;
		}
	}

//	private class TileMapHandler extends Handler {
//		
//		@Override
//		public void handleMessage(final Message msg) {
//			switch (msg.what) {
//			case MessageHandlerConstants.MAPTILEFSLOADER_SUCCESS_ID:
//				invalidate();
//				break;
//			case MessageHandlerConstants.MAPTILEFSLOADER_INDEXIND_SUCCESS_ID:
//				mTileSource.postIndex();
//				setZoomLevel(getZoomLevel());
//				if(mMoveListener != null)
//					mMoveListener.onZoomDetected();
//				break;
//			}
//		}
//	}

	public TileView(Context context) {
		super(context);

		mPaint.setFilterBitmap(true);
		mPaint.setAntiAlias(true);
		
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		mDrawTileGrid = pref.getBoolean("pref_drawtilegrid", false);
		
		mSetOffsetMode = false;

		setFocusable(true);
		setFocusableInTouchMode(true);
		
		mTileOverlay = new TileOverlay(this, false);
	}
	
	public PoiMenuInfo mPoiMenuInfo = new PoiMenuInfo(-1);
	
	public class PoiMenuInfo implements ContextMenuInfo {
		public int MarkerIndex;
		public GeoPoint EventGeoPoint;
		public double Elevation;

		public PoiMenuInfo(int markerIndex) {
			super();
			MarkerIndex = markerIndex;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		for (TileViewOverlay osmvo : mOverlays) {
			if(osmvo.onKeyDown(keyCode, event, TileView.this))
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		boolean result = false;
		
		if(!mDisableControl) {
			mScaleDetector.onTouchEvent(event);
			result = mDetector.onTouchEvent(event);
			if (!result) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					result = true;
				}
			}
		}
		return result;
	}

	@Override
	protected void onDraw(Canvas c) {
		c.save();

		final float aRotateToAngle = 360 - mBearing;
		c.rotate(aRotateToAngle, this.getWidth() / 2, this.getHeight() / 2);

		c.drawRGB(255, 255, 255);
		
		if (mTileSource != null) {
			mTileOverlay.onManagedDraw(c, this);

			/* Draw all Overlays. */
			for (TileViewOverlay osmvo : this.mOverlays)
				osmvo.onManagedDraw(c, this);
		}

		c.restore();
		
		super.onDraw(c);
	}

	public List<TileViewOverlay> getOverlays() {
		return mOverlays;
	}
	
	public void setTileSource(TileSource tileSource) {
		if(mTileSource != null)
			mTileSource.Free();
		mTileSource = tileSource;
		//mTileSource.setHandler(mTileMapHandler);
		mOffsetLat = mTileSource.OFFSET_LAT;
		mOffsetLon = mTileSource.OFFSET_LON;
		
		mTileOverlay.setTileSource(tileSource);
		
		setZoomLevel(getZoomLevel());
		invalidate();
	}
	
	public TileSource getTileSource() {
		return mTileSource;
	}
	
	public void setMapCenter(final GeoPoint aCenter) {
		this.setMapCenter(aCenter.getLatitudeE6(), aCenter.getLongitudeE6());
	}

	public void setMapCenter(final double aLatitude, final double aLongitude) {
		this.setMapCenter((int) (aLatitude * 1E6), (int) (aLongitude * 1E6));
	}

	public void setMapCenter(final int aLatitudeE6, final int aLongitudeE6) {
		this.setMapCenter(aLatitudeE6, aLongitudeE6, true);
	}

	public GeoPoint	getMapCenter() {
		return new GeoPoint(this.mLatitudeE6, this.mLongitudeE6);
	}
	
	protected void setMapCenter(final int aLatitudeE6, final int aLongitudeE6,
			final boolean doPassFurther) {
		this.mLatitudeE6 = aLatitudeE6;
		this.mLongitudeE6 = aLongitudeE6;
		
		if(mMoveListener != null)
			mMoveListener.onCenterDetected();

		this.postInvalidate();
	}
	
	public int getZoomLevel() {
		return mZoom;
	}
	
	public double getZoomLevelScaled() {
		if(mTouchScale == 1)
			return getZoomLevel();
		else if(mTouchScale > 1)
			return getZoomLevel()+Math.round(mTouchScale)-1;
		else
			return getZoomLevel()-Math.round(1/mTouchScale)+1;
	}
	
	public void setZoomLevel(final int zoom) {
		if(mTileSource == null) 
			mZoom = zoom;
		else
			mZoom = Math.max(mTileSource.ZOOM_MINLEVEL, Math.min(mTileSource.ZOOM_MAXLEVEL, zoom));
		
		if(mMoveListener != null)
			mMoveListener.onZoomDetected();
		
		this.postInvalidate();
	}
	
	public void setBearing(final float aBearing){
		this.mBearing = aBearing;
	}

	public float getBearing() {
		return this.mBearing;
	}

	public OpenStreetMapViewProjection getProjection() {
		return new OpenStreetMapViewProjection();
	}

	public OpenStreetMapViewProjection getProjection(int zoom, double touchScale) {
		return new OpenStreetMapViewProjection(zoom, touchScale);
	}

	public class OpenStreetMapViewProjection {

		final int viewWidth;
		final int viewHeight;
		final BoundingBoxE6 bb;
		final int zoomLevel;
		final int tileSizePx;
		final int[] centerMapTileCoords;
		final Point upperLeftCornerOfCenterMapTile;

		public OpenStreetMapViewProjection() {
			this(mZoom, mTouchScale);
		}
		
		public OpenStreetMapViewProjection(int zoom, double touchScale) {
			viewWidth = getWidth();
			viewHeight = getHeight();

			/*
			 * Do some calculations and drag attributes to local variables to
			 * save some performance.
			 */
			zoomLevel = zoom; // LATER Draw to
															// attributes and so
															// make it only
															// 'valid' for a
															// short time.
			tileSizePx = (int)(mTileSource.getTileSizePx(zoomLevel) * touchScale);

			/*
			 * Get the center MapTile which is above this.mLatitudeE6 and
			 * this.mLongitudeE6 .
			 */
			centerMapTileCoords = Util.getMapTileFromCoordinates(
					mLatitudeE6, mLongitudeE6,
					zoomLevel, null, mTileSource.PROJECTION);
			upperLeftCornerOfCenterMapTile = mTileOverlay.getUpperLeftCornerOfCenterMapTileInScreen(TileView.this, 
					centerMapTileCoords, tileSizePx, null);

			bb = getDrawnBoundingBoxE6();
		}

		/**
		 * Converts x/y ScreenCoordinates to the underlying GeoPoint.
		 *
		 * @param x
		 * @param y
		 * @return GeoPoint under x/y.
		 */
		public GeoPoint fromPixels(float x, float y) {
			/* Subtract the offset caused by touch. */
			//Log.d(DEBUGTAG, "x = "+x+" mTouchMapOffsetX = "+mTouchMapOffsetX+"   ");

			x -= 0;
			y -= 0;

			//int xx = centerMapTileCoords[0]*tileSizePx+(int)x-upperLeftCornerOfCenterMapTile.x;
			//int asd = Util.x2lon(xx, zoomLevel, tileSizePx);
			GeoPoint p = bb.getGeoPointOfRelativePositionWithLinearInterpolation(x / viewWidth, y
					/ viewHeight);

			//Log.d(DEBUGTAG, "lon "+p.getLongitudeE6()+" "+xx+" "+asd+" OffsetX = "+mTouchMapOffsetX);
			//Log.d(DEBUGTAG, "	"+centerMapTileCoords[0]+" "+tileSizePx+" "+x+" "+upperLeftCornerOfCenterMapTile.x);
			//p.setLongitudeE6(asd);

			//for(int i =0; i<=tileSizePx*(1<<zoomLevel); i++){int Q = Util.x2lon(i, zoomLevel, tileSizePx);Log.d(DEBUGTAG, "lon "+i+" "+Q);}

			return p;
		}

		public GeoPoint fromPixels(float x, float y, double bearing){
			final int x1 = (int) (x - getWidth()/2);
			final int y1 = (int) (y - getHeight()/2);
			final double hypot = Math.hypot(x1, y1);
			final double angle = -1 * Math.signum(y1) * Math.toDegrees(Math.acos(x1/hypot));
			final double angle2 = angle - bearing;
			final int x2 = (int)(Math.cos(Math.toRadians(angle2))*hypot);
			final int y2 = (int)(Math.sin(Math.toRadians(angle2-180))*hypot);

			return fromPixels((float)(getWidth()/2 + x2), (float)(getHeight()/2 + y2));
		}

		private static final int EQUATORCIRCUMFENCE = 40075676; //40075004;

		public float metersToEquatorPixels(final float aMeters) {
			return aMeters / EQUATORCIRCUMFENCE
					* mTileSource.getTileSizePx(zoomLevel);
		}

		/**
		 * Converts a GeoPoint to its ScreenCoordinates. <br/>
		 * <br/>
		 * <b>CAUTION</b> ! Conversion currently has a large error on
		 * <code>zoomLevels <= 7</code>.<br/>
		 * The Error on ZoomLevels higher than 7, the error is below
		 * <code>1px</code>.<br/>
		 * LATER: Add a linear interpolation to minimize this error.
		 *
		 * <PRE>
		 * Zoom 	Error(m) 	Error(px)
		 * 11 	6m 	1/12px
		 * 10 	24m 	1/6px
		 * 8 	384m 	1/2px
		 * 6 	6144m 	3px
		 * 4 	98304m 	10px
		 * </PRE>
		 *
		 * @param in
		 *            the GeoPoint you want the onScreenCoordinates of.
		 * @param reuse
		 *            just pass null if you do not have a Point to be
		 *            'recycled'.
		 * @return the Point containing the approximated ScreenCoordinates of
		 *         the GeoPoint passed.
		 */
		public Point toPixels(final GeoPoint in, final Point reuse) {
			return toPixels(in, reuse, true);
		}

		public Point toPixels(final GeoPoint in, final double bearing, final Point reuse){
			final Point point = toPixels(in, reuse, true);
			final Point out = (reuse != null) ? reuse : new Point();

			final int x1 = point.x - getWidth()/2;
			final int y1 = point.y - getHeight()/2;
			final double hypot = Math.hypot(x1, y1);
			final double angle = -1 * Math.signum(y1) * Math.toDegrees(Math.acos(x1/hypot));
			final double angle2 = angle + bearing;
			final int x2 = (int)(Math.cos(Math.toRadians(angle2))*hypot);
			final int y2 = (int)(Math.sin(Math.toRadians(angle2-180))*hypot);

			out.set(getWidth()/2 + x2, getHeight()/2 + y2);
			return out;
		}

		protected Point toPixels(final GeoPoint in, final Point reuse, final boolean doGudermann) {

			final Point out = (reuse != null) ? reuse : new Point();

			final int[] underGeopointTileCoords = Util.getMapTileFromCoordinates(
					in.getLatitudeE6(), in.getLongitudeE6(), zoomLevel, null, mTileSource.PROJECTION);

			/*
			 * Calculate the Latitude/Longitude on the left-upper ScreenCoords
			 * of the MapTile.
			 */
			final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(underGeopointTileCoords,
					zoomLevel, mTileSource.PROJECTION);

			final float[] relativePositionInCenterMapTile;
			if (doGudermann && zoomLevel < 7)
				relativePositionInCenterMapTile = bb
						.getRelativePositionOfGeoPointInBoundingBoxWithExactGudermannInterpolation(
								in.getLatitudeE6(), in.getLongitudeE6(), null);
			else
				relativePositionInCenterMapTile = bb
						.getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(in
								.getLatitudeE6(), in.getLongitudeE6(), null);

			final int tileDiffX = centerMapTileCoords[LONGITUDE]
					- underGeopointTileCoords[LONGITUDE];
			final int tileDiffY = centerMapTileCoords[LATITUDE]
					- underGeopointTileCoords[LATITUDE];
			final int underGeopointTileScreenLeft = upperLeftCornerOfCenterMapTile.x
					- (tileSizePx * tileDiffX);
			final int underGeopointTileScreenTop = upperLeftCornerOfCenterMapTile.y
					- (tileSizePx * tileDiffY);

			final int x = underGeopointTileScreenLeft
					+ (int) (relativePositionInCenterMapTile[LONGITUDE] * tileSizePx);
			final int y = underGeopointTileScreenTop
					+ (int) (relativePositionInCenterMapTile[LATITUDE] * tileSizePx);

			/* Add up the offset caused by touch. */
			out.set(x + 0, y
					+ 0);
			return out;
		}

		public Point toPixels2(final GeoPoint in) {

			final Point out = new Point();
			final boolean doGudermann = true;

			final int[] underGeopointTileCoords = Util.getMapTileFromCoordinates(
					in.getLatitudeE6(), in.getLongitudeE6(), zoomLevel, null, mTileSource.PROJECTION);

			/*
			 * Calculate the Latitude/Longitude on the left-upper ScreenCoords
			 * of the MapTile.
			 */
			final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(underGeopointTileCoords,
					zoomLevel, mTileSource.PROJECTION);

			final float[] relativePositionInCenterMapTile;
			if (doGudermann && zoomLevel < 7)
				relativePositionInCenterMapTile = bb
						.getRelativePositionOfGeoPointInBoundingBoxWithExactGudermannInterpolation(
								in.getLatitudeE6(), in.getLongitudeE6(), null);
			else
				relativePositionInCenterMapTile = bb
						.getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(in
								.getLatitudeE6(), in.getLongitudeE6(), null);

			final int tileDiffX = centerMapTileCoords[LONGITUDE]
					- underGeopointTileCoords[LONGITUDE];
			final int tileDiffY = centerMapTileCoords[LATITUDE]
					- underGeopointTileCoords[LATITUDE];
			final int underGeopointTileScreenLeft = upperLeftCornerOfCenterMapTile.x
					- (tileSizePx * tileDiffX);
			final int underGeopointTileScreenTop = upperLeftCornerOfCenterMapTile.y
					- (tileSizePx * tileDiffY);

			final int x = underGeopointTileScreenLeft
					+ (int) (relativePositionInCenterMapTile[LONGITUDE] * tileSizePx);
			final int y = underGeopointTileScreenTop
					+ (int) (relativePositionInCenterMapTile[LATITUDE] * tileSizePx);

			/* Add up the offset caused by touch. */
			out.set(x, y);
			return out;
		}

		public Path toPixels(final List<GeoPoint> in, final Path reuse) {
			return toPixels(in, reuse, true);
		}

		protected Path toPixels(final List<GeoPoint> in, final Path reuse, final boolean doGudermann)
				throws IllegalArgumentException {
			if (in.size() < 2)
				throw new IllegalArgumentException("List of GeoPoints needs to be at least 2.");

			final Path out = (reuse != null) ? reuse : new Path();

			int i = 0;
			for (GeoPoint gp : in) {
				i++;
				final int[] underGeopointTileCoords = Util.getMapTileFromCoordinates(gp
						.getLatitudeE6(), gp.getLongitudeE6(), zoomLevel, null, mTileSource.PROJECTION);

				/*
				 * Calculate the Latitude/Longitude on the left-upper
				 * ScreenCoords of the MapTile.
				 */
				final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(underGeopointTileCoords,
						zoomLevel, mTileSource.PROJECTION);

				final float[] relativePositionInCenterMapTile;
				if (doGudermann && zoomLevel < 7)
					relativePositionInCenterMapTile = bb
							.getRelativePositionOfGeoPointInBoundingBoxWithExactGudermannInterpolation(
									gp.getLatitudeE6(), gp.getLongitudeE6(), null);
				else
					relativePositionInCenterMapTile = bb
							.getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(gp
									.getLatitudeE6(), gp.getLongitudeE6(), null);

				final int tileDiffX = centerMapTileCoords[LONGITUDE]
						- underGeopointTileCoords[LONGITUDE];
				final int tileDiffY = centerMapTileCoords[LATITUDE]
						- underGeopointTileCoords[LATITUDE];
				final int underGeopointTileScreenLeft = upperLeftCornerOfCenterMapTile.x
						- (tileSizePx * tileDiffX);
				final int underGeopointTileScreenTop = upperLeftCornerOfCenterMapTile.y
						- (tileSizePx * tileDiffY);

				final int x = underGeopointTileScreenLeft
						+ (int) (relativePositionInCenterMapTile[LONGITUDE] * tileSizePx);
				final int y = underGeopointTileScreenTop
						+ (int) (relativePositionInCenterMapTile[LATITUDE] * tileSizePx);

				/* Add up the offset caused by touch. */
				if (i == 0)
					out.moveTo(x, y);
				else
					out.lineTo(x, y);
			}

			return out;
		}
		
		public void StopProcessing() {
			mStopProcessing = true;
		}
		
		private boolean Stop() {
			if(mStopProcessing) {
				mStopProcessing = false;
				return true;
			}
			return false;
		}

		public Path toPixelsTrackPoints(Cursor cursor, Point baseCoord, GeoPoint baseLocation) throws IllegalArgumentException {
			mStopProcessing = false;
			Path out = new Path();
			final boolean doGudermann = true;
			int lat, lon;

			int i = 0;
			int lastX = 0, lastY = 0;
			if(cursor != null) {
				if(cursor.moveToFirst()) {
					do {
						if(Stop()) {
							out = null;
							break;
						}
						
						lat = (int) (cursor.getDouble(0) * 1E6);
						lon = (int) (cursor.getDouble(1) * 1E6);
						
						final int[] underGeopointTileCoords = Util.getMapTileFromCoordinates(lat, lon, zoomLevel, null, mTileSource.PROJECTION);

						/*
						 * Calculate the Latitude/Longitude on the left-upper ScreenCoords of the MapTile.
						 */
						final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(underGeopointTileCoords, zoomLevel,
								mTileSource.PROJECTION);

						final float[] relativePositionInCenterMapTile;
						if (doGudermann && zoomLevel < 7)
							relativePositionInCenterMapTile = bb
									.getRelativePositionOfGeoPointInBoundingBoxWithExactGudermannInterpolation(lat, lon, null);
						else
							relativePositionInCenterMapTile = bb
									.getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(lat, lon, null);

						final int tileDiffX = centerMapTileCoords[LONGITUDE]
								- underGeopointTileCoords[LONGITUDE];
						final int tileDiffY = centerMapTileCoords[LATITUDE]
								- underGeopointTileCoords[LATITUDE];
						final int underGeopointTileScreenLeft = upperLeftCornerOfCenterMapTile.x - (tileSizePx * tileDiffX);
						final int underGeopointTileScreenTop = upperLeftCornerOfCenterMapTile.y - (tileSizePx * tileDiffY);

						final int x = underGeopointTileScreenLeft
								+ (int) (relativePositionInCenterMapTile[LONGITUDE] * tileSizePx);
						final int y = underGeopointTileScreenTop
								+ (int) (relativePositionInCenterMapTile[LATITUDE] * tileSizePx);

						/* Add up the offset caused by touch. */
						if (i == 0) {
							out.setLastPoint(x, y);
							lastX = x;
							lastY = y;
							baseCoord.x = x;
							baseCoord.y = y;
							baseLocation.setCoordsE6(lat, lon);
							i++;
						} else {
							if (Math.abs(lastX - x) > 5 || Math.abs(lastY - y) > 5) {
								out.lineTo(x, y);
								lastX = x;
								lastY = y;
								i++;
							}
						}
					} while(cursor.moveToNext());
				}
				cursor.close();
			}
			
			return out;
		}
	}

	private IMoveListener mMoveListener;
	private boolean mDisableControl;

	// TODO След процедуры под вопросом о переделке

	public BoundingBoxE6 getVisibleBoundingBoxE6() {
//		final ViewParent parent = this.getParent();
//		if(parent instanceof RotateView){
//			final RotateView par = (RotateView)parent;
//			return getBoundingBox(par.getMeasuredWidth(), par.getMeasuredHeight());
//		}else{
			return getBoundingBox(this.getWidth(), this.getHeight());
//		}
	}

	private BoundingBoxE6 getBoundingBox(final int pViewWidth, final int pViewHeight){
		/* Get the center MapTile which is above this.mLatitudeE6 and this.mLongitudeE6 .*/
		final int[] centerMapTileCoords = Util.getMapTileFromCoordinates(this.mLatitudeE6, this.mLongitudeE6, this.mZoom, null, this.mTileSource.PROJECTION);

		final BoundingBoxE6 tmp = Util.getBoundingBoxFromMapTile(centerMapTileCoords, this.mZoom, mTileSource.PROJECTION);

		final int mLatitudeSpan_2 = (int)(1.0f * tmp.getLatitudeSpanE6() * pViewHeight / this.mTileSource.getTileSizePx(this.mZoom)) / 2;
		final int mLongitudeSpan_2 = (int)(1.0f * tmp.getLongitudeSpanE6() * pViewWidth / this.mTileSource.getTileSizePx(this.mZoom)) / 2;

		final int north = this.mLatitudeE6 + mLatitudeSpan_2;
		final int south = this.mLatitudeE6 - mLatitudeSpan_2;
		final int west = this.mLongitudeE6 - mLongitudeSpan_2;
		final int east = this.mLongitudeE6 + mLongitudeSpan_2;

		return new BoundingBoxE6(north, east, south, west);
	}

	public BoundingBoxE6 getDrawnBoundingBoxE6() {
		return getBoundingBox(this.getWidth(), this.getHeight());
	}

	public void setMoveListener(IMoveListener moveListener) {
		mMoveListener = moveListener;
		mTileOverlay.setMoveListener(moveListener);
	}

	public void setDisableControl(boolean b) {
		mDisableControl = true;
	}

	public double[] getCurrentOffset() {
		final double[] offset = {mOffsetLat, mOffsetLon};
		return offset;
	}

	public void setOffsetMode(boolean mode) {
		mSetOffsetMode = mode;
	}
}
