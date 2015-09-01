package com.robert.maps.applib.overlays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;

import com.robert.maps.applib.tileprovider.MessageHandlerConstants;
import com.robert.maps.applib.tileprovider.TileSource;
import com.robert.maps.applib.utils.Ut;
import com.robert.maps.applib.view.TileView;
import com.robert.maps.applib.view.TileViewOverlay;

import org.andnav.osm.util.BoundingBoxE6;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.MyMath;
import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.util.Util;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

public class YandexTrafficOverlay extends TileViewOverlay implements OpenStreetMapConstants,
		OpenStreetMapViewConstants {
	private TileView mMapView;
	private final Paint mPaint = new Paint();
	private TileSource mTileSource;
	private SimpleInvalidationHandler mHandler = new SimpleInvalidationHandler();

	public YandexTrafficOverlay(Context ctx, TileView mapView) {
		mMapView = mapView;
		try {
			mTileSource = new TileSource(ctx, "yandextraffic");
		} catch (Exception e) {
		}
		mTileSource.setHandler(mHandler);
	}
	
	public void Free() {
		mTileSource.Free();
	}

	private static final int LATITUDE = 0;
	private static final int LONGITUDE = 1;

	@Override
	protected void onDraw(Canvas c, TileView osmv) {
		final int zoomLevel = mMapView.getZoomLevel();
		final int viewWidth = mMapView.getWidth();
		final int viewHeight = mMapView.getHeight();
		final GeoPoint centerGeoPoint = mMapView.getMapCenter();

		if (mTileSource != null) {
		}
			
		final int tileSizePxNotScale = mTileSource.getTileSizePx(zoomLevel);
		final int tileSizePx = (int)(this.mTileSource.getTileSizePx(zoomLevel) * mMapView.mTouchScale);
		final int[] centerMapTileCoords = Util.getMapTileFromCoordinates(centerGeoPoint.getLatitudeE6(), centerGeoPoint.getLongitudeE6(), zoomLevel, null,
				mTileSource.PROJECTION);

		/*
		 * Calculate the Latitude/Longitude on the left-upper ScreenCoords
		 * of the center MapTile. So in the end we can determine which
		 * MapTiles we additionally need next to the centerMapTile.
		 */
		final Point upperLeftCornerOfCenterMapTileNotScale = getUpperLeftCornerOfCenterMapTileInScreen(
				centerMapTileCoords, tileSizePxNotScale, null);

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

		final int iDelta = 0;
		final int additionalTilesNeededToLeftOfCenter = iDelta
				+ (int) Math.ceil((float) centerMapTileScreenLeftNotScale
						/ tileSizePxNotScale); // i.e.
		final int additionalTilesNeededToRightOfCenter = iDelta
				+ (int) Math
						.ceil((float) (viewWidth - centerMapTileScreenRightNotScale)
								/ tileSizePxNotScale);
		final int additionalTilesNeededToTopOfCenter = iDelta
				+ (int) Math.ceil((float) centerMapTileScreenTopNotScale
						/ tileSizePxNotScale); // i.e.
		final int additionalTilesNeededToBottomOfCenter = iDelta
				+ (int) Math
						.ceil((float) (viewHeight - centerMapTileScreenBottomNotScale)
								/ tileSizePxNotScale);

		final int mapTileUpperBound = mTileSource.getTileUpperBound(zoomLevel);
		final int[] mapTileCoords = new int[] {
				centerMapTileCoords[LATITUDE],
				centerMapTileCoords[LONGITUDE] };
		
		mTileSource.getTileProvider().ResizeCashe((additionalTilesNeededToTopOfCenter+additionalTilesNeededToBottomOfCenter+1)*(additionalTilesNeededToLeftOfCenter+additionalTilesNeededToRightOfCenter+1));


		/* Draw all the MapTiles (from the upper left to the lower right). */
		for (int y = -additionalTilesNeededToTopOfCenter; y <= additionalTilesNeededToBottomOfCenter; y++) {
			for (int x = -additionalTilesNeededToLeftOfCenter; x <= additionalTilesNeededToRightOfCenter; x++) {
				mapTileCoords[LATITUDE] = MyMath.mod(
						centerMapTileCoords[LATITUDE] + y,
						mapTileUpperBound);
				mapTileCoords[LONGITUDE] = MyMath.mod(
						centerMapTileCoords[LONGITUDE] + x,
						mapTileUpperBound);

				final Bitmap currentMapTile = this.mTileSource.getTile(
						mapTileCoords[LONGITUDE], mapTileCoords[LATITUDE],
						zoomLevel);
				if (currentMapTile != null) {
					final int tileLeft = centerMapTileScreenLeft + (x * tileSizePx);
					final int tileTop = centerMapTileScreenTop + (y * tileSizePx);
					final Rect r = new Rect(tileLeft, tileTop, tileLeft
							+ tileSizePx, tileTop + tileSizePx);
					if (!currentMapTile.isRecycled())
						c.drawBitmap(currentMapTile, null, r, mPaint);

				}

			}
		}

		mTileSource.getTileProvider().CommitCashe();
	}

	@Override
	protected void onDrawFinished(Canvas c, TileView osmv) {
	}

	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case MessageHandlerConstants.MAPTILEFSLOADER_SUCCESS_ID:
			case Ut.MAPTILEFSLOADER_SUCCESS_ID:
				mMapView.invalidate();
				break;
			}
		}
	}

	private Point getUpperLeftCornerOfCenterMapTileInScreen(final int[] centerMapTileCoords, final int tileSizePx,
			final Point reuse) {
		final GeoPoint centerGeoPoint = mMapView.getMapCenter();
		final Point out = (reuse != null) ? reuse : new Point();

		final int viewWidth = mMapView.getWidth();
		final int viewWidth_2 = viewWidth / 2;
		final int viewHeight = mMapView.getHeight();
		final int viewHeight_2 = viewHeight / 2;

		/*
		 * Calculate the Latitude/Longitude on the left-upper ScreenCoords of
		 * the center MapTile. So in the end we can determine which MapTiles we
		 * additionally need next to the centerMapTile.
		 */
		final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(centerMapTileCoords,
				mMapView.getZoomLevel(), mTileSource.PROJECTION);
		final float[] relativePositionInCenterMapTile = bb
				.getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(
						centerGeoPoint.getLatitudeE6(), centerGeoPoint.getLongitudeE6(), null);

		final int centerMapTileScreenLeft = viewWidth_2
				- (int) (0.5f + (relativePositionInCenterMapTile[LONGITUDE] * tileSizePx));
		final int centerMapTileScreenTop = viewHeight_2
				- (int) (0.5f + (relativePositionInCenterMapTile[LATITUDE] * tileSizePx));

		out.set(centerMapTileScreenLeft, centerMapTileScreenTop);
		return out;
	}

}
