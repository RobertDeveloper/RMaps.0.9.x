package com.robert.maps.view;

import java.util.ArrayList;
import java.util.List;

import org.andnav.osm.util.BoundingBoxE6;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.MyMath;
import org.andnav.osm.views.util.Util;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.robert.maps.kml.Track.TrackPoint;
import com.robert.maps.tileprovider.MessageHandlerConstants;
import com.robert.maps.tileprovider.TileSource;
import com.robert.maps.utils.Ut;

public class TileView extends View {
	private static final int LATITUDE = 0;
	private static final int LONGITUDE = 1;

	private int mLatitudeE6 = 0, mLongitudeE6 = 0;
	private int mZoom = 0;
	private float mBearing = 0;
	
	private boolean mStopProcessing;
	
	public double mTouchScale = 1;
	
	private TileSource mTileSource;
	private TileMapHandler mTileMapHandler = new TileMapHandler();
	protected final List<TileViewOverlay> mOverlays = new ArrayList<TileViewOverlay>();
	
	private GestureDetector mDetector = new GestureDetector(getContext(), new TouchListener(), null, false);
	private ScaleGestureDetector mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
	
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mTouchScale = detector.getScaleFactor();
			if(mMoveListener != null)
				mMoveListener.onZoomDetected();
			
			postInvalidate();
			
			return super.onScale(detector);
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			int zoom = 0;
			if(mTouchScale > 1)
				zoom = getZoomLevel()+(int)Math.round(mTouchScale)-1;
			else
				zoom = getZoomLevel()-(int)Math.round(1/mTouchScale)+1;
			
			mTouchScale = 1;
			setZoomLevel(zoom);
			
			super.onScaleEnd(detector);
		}
		
	}
	
	private class TouchListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			for (TileViewOverlay osmvo : mOverlays)
				if (osmvo.onSingleTapUp(e, TileView.this)) {
					invalidate();
					return true;
				}
			
			invalidate();
			return super.onSingleTapConfirmed(e);
		}

		@Override
		public void onLongPress(MotionEvent e) {
			for (TileViewOverlay osmvo : mOverlays) {
				osmvo.onLongPress(e, TileView.this);
			}
			
			showContextMenu();
			super.onLongPress(e);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			final float aRotateToAngle = 360 - mBearing;
			final int viewWidth_2 = TileView.this.getWidth() / 2;
			final int viewHeight_2 = TileView.this.getHeight() / 2;
			final int TouchMapOffsetX = (int) (Math.sin(Math.toRadians(aRotateToAngle)) * (distanceY / mTouchScale))
					+ (int) (Math.cos(Math.toRadians(aRotateToAngle)) * (distanceX / mTouchScale));
			final int TouchMapOffsetY = (int) (Math.cos(Math.toRadians(aRotateToAngle)) * (distanceY / mTouchScale))
					- (int) (Math.sin(Math.toRadians(aRotateToAngle)) * (distanceX / mTouchScale));
			final GeoPoint newCenter = TileView.this.getProjection().fromPixels(viewWidth_2 + TouchMapOffsetX,
					viewHeight_2 + TouchMapOffsetY);
			TileView.this.setMapCenter(newCenter);
			
			// if(count > 1){
			// final double DiagonalSize = Math.hypot((double)(x1 - x2),
			// (double)(y1 - y2));
			// mTouchScale = (DiagonalSize / mTouchDiagonalSize);
			// }
			if (mMoveListener != null)
				mMoveListener.onMoveDetected();
			
			//invalidate();
			
			return super.onScroll(e1, e2, distanceX, distanceY);
		}

		@Override
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
	}

	private class TileMapHandler extends Handler {
		
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case MessageHandlerConstants.MAPTILEFSLOADER_SUCCESS_ID:
				invalidate();
				break;
			case MessageHandlerConstants.MAPTILEFSLOADER_INDEXIND_SUCCESS_ID:
				mTileSource.postIndex();
				setZoomLevel(getZoomLevel());
				if(mMoveListener != null)
					mMoveListener.onZoomDetected();
				break;
//			case OpenStreetMapTileFilesystemProvider.ERROR_MESSAGE:
//				Message.obtain(mMainActivityCallbackHandler, OpenStreetMapTileFilesystemProvider.ERROR_MESSAGE, msg.obj)
//						.sendToTarget();
//				break;
//			case OpenStreetMapTileFilesystemProvider.INDEXIND_SUCCESS_ID:
//				if (mZoomLevel > mRendererInfo.ZOOM_MAXLEVEL)
//					mZoomLevel = mRendererInfo.ZOOM_MAXLEVEL;
//				if (mZoomLevel < mRendererInfo.ZOOM_MINLEVEL)
//					mZoomLevel = mRendererInfo.ZOOM_MINLEVEL;
//				
//				Message.obtain(mMainActivityCallbackHandler, R.id.set_title).sendToTarget();
//				
//				invalidate();
//				break;
			}
		}
	}

	public TileView(Context context) {
		super(context);

		setFocusable(true);
		setFocusableInTouchMode(true);
	}
	
	public PoiMenuInfo mPoiMenuInfo = new PoiMenuInfo(-1);
	
//	@Override
//	protected ContextMenuInfo getContextMenuInfo() {
//		return mPoiMenuInfo;
//	}
//	
	public class PoiMenuInfo implements ContextMenuInfo {
		public int MarkerIndex;
		public GeoPoint EventGeoPoint;

		public PoiMenuInfo(int markerIndex) {
			super();
			MarkerIndex = markerIndex;
		}
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		
		boolean result = mDetector.onTouchEvent(event);
		if (!result) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				//stopScrolling();
				result = true;
			}
		}
		return result;
		
//		this.mGestureDetector.onTouchEvent(event);
//		this.mDetector.onTouchEvent(event);
//
//		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		for (TileViewOverlay osmvo : this.mOverlays)
			if (osmvo.onKeyDown(keyCode, event, this))
				return true;

		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDraw(Canvas c) {
		final Paint paint = new Paint();
		paint.setAntiAlias(true);

		c.save();

		final float aRotateToAngle = 360 - mBearing;
		c.rotate(aRotateToAngle, this.getWidth() / 2, this.getHeight() / 2);

		c.drawRGB(255, 255, 255);

		if (mTileSource != null) {
			final int tileSizePxNotScale = mTileSource.getTileSizePx(mZoom);
			final int tileSizePx = (int) (tileSizePxNotScale * mTouchScale);
			final int[] centerMapTileCoords = Util.getMapTileFromCoordinates(
					this.mLatitudeE6, this.mLongitudeE6, mZoom, null,
					mTileSource.PROJECTION);

			/*
			 * Calculate the Latitude/Longitude on the left-upper ScreenCoords
			 * of the center MapTile. So in the end we can determine which
			 * MapTiles we additionally need next to the centerMapTile.
			 */
			final Point upperLeftCornerOfCenterMapTileNotScale = getUpperLeftCornerOfCenterMapTileInScreen(
					centerMapTileCoords, tileSizePxNotScale, null);
			Ut.d("Map: "+upperLeftCornerOfCenterMapTileNotScale.x+", "+upperLeftCornerOfCenterMapTileNotScale.y);

			final int centerMapTileScreenLeftNotScale = upperLeftCornerOfCenterMapTileNotScale.x;
			final int centerMapTileScreenTopNotScale = upperLeftCornerOfCenterMapTileNotScale.y;
			final int centerMapTileScreenRightNotScale = centerMapTileScreenLeftNotScale
					+ tileSizePxNotScale;
			final int centerMapTileScreenBottomNotScale = centerMapTileScreenTopNotScale
					+ tileSizePxNotScale;

			final Point upperLeftCornerOfCenterMapTile = getUpperLeftCornerOfCenterMapTileInScreen(
					centerMapTileCoords, tileSizePx, null);
			final int centerMapTileScreenLeft = upperLeftCornerOfCenterMapTile.x;
			final int centerMapTileScreenTop = upperLeftCornerOfCenterMapTile.y;

			/*
			 * Calculate the amount of tiles needed for each side around the
			 * center one.
			 */
			// TODO Нужен адекватный алгоритм для отбора необходимых тайлов,
			// попадающих в экран при повороте карты
			final int iDelta = 1; //mBearing > 0 && mTileSource.YANDEX_TRAFFIC_ON == 0 ? 1 : 0;
			final int additionalTilesNeededToLeftOfCenter = iDelta
					+ (int) Math.ceil((float) centerMapTileScreenLeftNotScale
							/ tileSizePxNotScale); // i.e.
			final int additionalTilesNeededToRightOfCenter = iDelta
					+ (int) Math
							.ceil((float) (this.getWidth() - centerMapTileScreenRightNotScale)
									/ tileSizePxNotScale);
			final int additionalTilesNeededToTopOfCenter = iDelta
					+ (int) Math.ceil((float) centerMapTileScreenTopNotScale
							/ tileSizePxNotScale); // i.e.
			final int additionalTilesNeededToBottomOfCenter = iDelta
					+ (int) Math
							.ceil((float) (this.getHeight() - centerMapTileScreenBottomNotScale)
									/ tileSizePxNotScale);

			final int mapTileUpperBound = mTileSource.getTileUpperBound(mZoom);
			final int[] mapTileCoords = new int[] {
					centerMapTileCoords[LATITUDE],
					centerMapTileCoords[LONGITUDE] };
			
			mTileSource.getTileProvider().ResizeCashe((additionalTilesNeededToTopOfCenter+additionalTilesNeededToBottomOfCenter+1)*(additionalTilesNeededToLeftOfCenter+additionalTilesNeededToRightOfCenter+1));

			/* Draw all the MapTiles (from the upper left to the lower right). */
			for (int y = -additionalTilesNeededToTopOfCenter; y <= additionalTilesNeededToBottomOfCenter; y++) {
				for (int x = -additionalTilesNeededToLeftOfCenter; x <= additionalTilesNeededToRightOfCenter; x++) {
					/*
					 * Add/substract the difference of the tile-position to the
					 * one of the center.
					 */
					mapTileCoords[LATITUDE] = MyMath.mod(
							centerMapTileCoords[LATITUDE] + y,
							mapTileUpperBound);
					mapTileCoords[LONGITUDE] = MyMath.mod(
							centerMapTileCoords[LONGITUDE] + x,
							mapTileUpperBound);
					/* Construct a URLString, which represents the MapTile. */
					// final String tileURLString =
					// this.mTileSource.getTileURLString(mapTileCoords,
					// mZoom);
					// Ut.dd("onDraw: " + tileURLString);

					/*
					 * Draw the MapTile 'i tileSizePx' above of the
					 * centerMapTile
					 */
					// final Bitmap currentMapTile =
					// this.mTileProvider.getMapTile(tileURLString,
					// this.mRendererInfo.TILE_SOURCE_TYPE,
					// mapTileCoords[MAPTILE_LONGITUDE_INDEX],
					// mapTileCoords[MAPTILE_LATITUDE_INDEX], zoomLevel);
					final Bitmap currentMapTile = this.mTileSource.getTile(
							mapTileCoords[LONGITUDE], mapTileCoords[LATITUDE],
							mZoom);
					if (currentMapTile != null) {
						final int tileLeft = centerMapTileScreenLeft + (x * tileSizePx);
						final int tileTop = centerMapTileScreenTop + (y * tileSizePx);
						final Rect r = new Rect(tileLeft, tileTop, tileLeft
								+ tileSizePx, tileTop + tileSizePx);
						if (!currentMapTile.isRecycled())
							c.drawBitmap(currentMapTile, null, r, paint);

						if (OpenStreetMapViewConstants.DEBUGMODE) {
							c.drawLine(tileLeft, tileTop,
									tileLeft + tileSizePx, tileTop, paint);
							c.drawLine(tileLeft, tileTop, tileLeft, tileTop
									+ tileSizePx, paint);
							c.drawText("y x = " + mapTileCoords[LATITUDE] + " "
									+ mapTileCoords[LONGITUDE] + " zoom "
									+ mZoom + " ", tileLeft + 5, tileTop + 15,
									paint);
						}

					}

				}
			}

			mTileSource.getTileProvider().CommitCashe();

			/* Draw all Overlays. */
			for (TileViewOverlay osmvo : this.mOverlays)
				osmvo.onManagedDraw(c, this);

			c.restore();

			// c.drawLine(viewWidth/2, 0, viewWidth/2, viewHeight, this.mPaint);
			// c.drawLine(0, viewHeight/2, viewWidth, viewHeight/2,
			// this.mPaint);
			// c.drawCircle(viewWidth/2, viewHeight/2, 100, this.mPaint);
			// c.drawLine(viewWidth/2-100, viewHeight/2-100, viewWidth/2+100,
			// viewHeight/2+100, this.mPaint);
			// c.drawLine(viewWidth/2+100, viewHeight/2-100, viewWidth/2-100,
			// viewHeight/2+100, this.mPaint);
		}

		c.restore();

		super.onDraw(c);
	}

	public List<TileViewOverlay> getOverlays() {
		return mOverlays;
	}
	
	public void setTileSource(TileSource tilesource) {
		if(mTileSource != null)
			mTileSource.Free();
		mTileSource = tilesource;
		mTileSource.setHandler(mTileMapHandler);
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

	public class OpenStreetMapViewProjection {

		final int viewWidth;
		final int viewHeight;
		final BoundingBoxE6 bb;
		final int zoomLevel;
		final int tileSizePx;
		final int[] centerMapTileCoords;
		final Point upperLeftCornerOfCenterMapTile;

		public OpenStreetMapViewProjection() {
			viewWidth = getWidth();
			viewHeight = getHeight();

			/*
			 * Do some calculations and drag attributes to local variables to
			 * save some performance.
			 */
			zoomLevel = mZoom; // LATER Draw to
															// attributes and so
															// make it only
															// 'valid' for a
															// short time.
			tileSizePx = (int)(mTileSource.getTileSizePx(zoomLevel)*mTouchScale);

			/*
			 * Get the center MapTile which is above this.mLatitudeE6 and
			 * this.mLongitudeE6 .
			 */
			centerMapTileCoords = Util.getMapTileFromCoordinates(
					mLatitudeE6, mLongitudeE6,
					zoomLevel, null, mTileSource.PROJECTION);
			upperLeftCornerOfCenterMapTile = getUpperLeftCornerOfCenterMapTileInScreen(
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
					* mTileSource.getTileSizePx(mZoom);
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

		public Path toPixelsTrackPoints(List<TrackPoint> in, Point baseCoord, GeoPoint baseLocation) throws IllegalArgumentException {
			if (in.size() < 2)
				return null;
				//throw new IllegalArgumentException("List of GeoPoints needs to be at least 2.");

			mStopProcessing = false;
			final Path out = new Path();
			final boolean doGudermann = true;

			int i = 0;
			int lastX = 0, lastY = 0;
			for (TrackPoint tp : in) {
				if(Stop()) {
					return null;
				}
				final int[] underGeopointTileCoords = Util.getMapTileFromCoordinates(tp.getLatitudeE6(), tp
						.getLongitudeE6(), zoomLevel, null, mTileSource.PROJECTION);

				/*
				 * Calculate the Latitude/Longitude on the left-upper ScreenCoords of the MapTile.
				 */
				final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(underGeopointTileCoords, zoomLevel,
						mTileSource.PROJECTION);

				final float[] relativePositionInCenterMapTile;
				if (doGudermann && zoomLevel < 7)
					relativePositionInCenterMapTile = bb
							.getRelativePositionOfGeoPointInBoundingBoxWithExactGudermannInterpolation(tp
									.getLatitudeE6(), tp.getLongitudeE6(), null);
				else
					relativePositionInCenterMapTile = bb
							.getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(tp.getLatitudeE6(), tp
									.getLongitudeE6(), null);

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
					baseLocation.setCoordsE6(tp.getLatitudeE6(), tp.getLongitudeE6());
					i++;
				} else {
					if (Math.abs(lastX - x) > 5 || Math.abs(lastY - y) > 5) {
						out.lineTo(x, y);
						lastX = x;
						lastY = y;
						i++;
					}
				}
			}

			return out;
		}
	}

	private IMoveListener mMoveListener;

	// TODO След процедуры под вопросом о переделке
	private Point getUpperLeftCornerOfCenterMapTileInScreen(final int[] centerMapTileCoords,
			final int tileSizePx, final Point reuse) {
		final Point out = (reuse != null) ? reuse : new Point();

		final int viewWidth = this.getWidth();
		final int viewWidth_2 = viewWidth / 2;
		final int viewHeight = this.getHeight();
		final int viewHeight_2 = viewHeight / 2;

		/*
		 * Calculate the Latitude/Longitude on the left-upper ScreenCoords of
		 * the center MapTile. So in the end we can determine which MapTiles we
		 * additionally need next to the centerMapTile.
		 */
		final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(centerMapTileCoords,
				this.mZoom, mTileSource.PROJECTION);
		final float[] relativePositionInCenterMapTile = bb
				.getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(
						this.mLatitudeE6, this.mLongitudeE6, null);

		final int centerMapTileScreenLeft = viewWidth_2
				- (int) (0.5f + (relativePositionInCenterMapTile[LONGITUDE] * tileSizePx));
		final int centerMapTileScreenTop = viewHeight_2
				- (int) (0.5f + (relativePositionInCenterMapTile[LATITUDE] * tileSizePx));

		out.set(centerMapTileScreenLeft, centerMapTileScreenTop);
		return out;
	}

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
	}

}
