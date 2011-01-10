package com.robert.maps.overlays;

import org.andnav.osm.util.BoundingBoxE6;
import org.andnav.osm.util.MyMath;
import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;
import org.andnav.osm.views.util.OpenStreetMapRendererInfo;
import org.andnav.osm.views.util.OpenStreetMapTileDownloader;
import org.andnav.osm.views.util.OpenStreetMapTileFilesystemProvider;
import org.andnav.osm.views.util.OpenStreetMapTileProvider;
import org.andnav.osm.views.util.Util;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;

public class YandexTrafficOverlay extends OpenStreetMapViewOverlay implements OpenStreetMapConstants,
		OpenStreetMapViewConstants {
	private OpenStreetMapView mMapView;
	protected OpenStreetMapRendererInfo mRendererInfo;
	protected final OpenStreetMapTileProvider mTileProvider;
	protected final Paint mPaint = new Paint();

	public YandexTrafficOverlay(Context ctx, OpenStreetMapView mapView) {
		mMapView = mapView;
		mRendererInfo = new OpenStreetMapRendererInfo(ctx.getResources(), "yandextraffic");
		mRendererInfo.LoadFromResources("yandextraffic", null);

		DisplayMetrics metrics = new DisplayMetrics();
		((Activity) ctx).getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int OpenStreetMapTileCacheSize = 10;
		if (metrics.heightPixels > 480 || metrics.widthPixels > 480)
			OpenStreetMapTileCacheSize = 20;

		mTileProvider = new OpenStreetMapTileProvider(ctx, new SimpleInvalidationHandler(), mRendererInfo,
				OpenStreetMapTileCacheSize);
	}

	@Override
	protected void onDraw(Canvas c, OpenStreetMapView osmv) {
		// final long startMs = System.currentTimeMillis();

		/*
		 * Do some calculations and drag attributes to local variables to save some performance.
		 */
		final int zoomLevel = mMapView.getZoomLevel();
		final int viewWidth = mMapView.getWidth();
		final int viewHeight = mMapView.getHeight();
		final int tileSizePx = (int)(this.mRendererInfo.getTileSizePx(zoomLevel)*osmv.mTouchScale);

		/*
		 * Get the center MapTile which is above this.mLatitudeE6 and this.mLongitudeE6 .
		 */
		final int[] centerMapTileCoords = Util.getMapTileFromCoordinates(mMapView.getMapCenterLatitudeE6(), mMapView
				.getMapCenterLongitudeE6(), zoomLevel, null, mRendererInfo.PROJECTION);

		/*
		 * Calculate the Latitude/Longitude on the left-upper ScreenCoords of the center MapTile. So in the end we can
		 * determine which MapTiles we additionally need next to the centerMapTile.
		 */
		final Point upperLeftCornerOfCenterMapTile = getUpperLeftCornerOfCenterMapTileInScreen(centerMapTileCoords,
				tileSizePx, null);

		final int centerMapTileScreenLeft = upperLeftCornerOfCenterMapTile.x;
		final int centerMapTileScreenTop = upperLeftCornerOfCenterMapTile.y;

		final int centerMapTileScreenRight = centerMapTileScreenLeft + tileSizePx;
		final int centerMapTileScreenBottom = centerMapTileScreenTop + tileSizePx;

		/*
		 * Calculate the amount of tiles needed for each side around the center one.
		 */
		final int additionalTilesNeededToLeftOfCenter = (int) Math.ceil((float) centerMapTileScreenLeft / tileSizePx); // i.e.
		// "30 / 256"
		// = 1;
		final int additionalTilesNeededToRightOfCenter = (int) Math.ceil((float) (viewWidth - centerMapTileScreenRight)
				/ tileSizePx);
		final int additionalTilesNeededToTopOfCenter = (int) Math.ceil((float) centerMapTileScreenTop / tileSizePx); // i.e.
		// "30 / 256"
		// = 1;
		final int additionalTilesNeededToBottomOfCenter = (int) Math
				.ceil((float) (viewHeight - centerMapTileScreenBottom) / tileSizePx);

		final int mapTileUpperBound = (int) Math.pow(2, zoomLevel);
		final int[] mapTileCoords = new int[] { centerMapTileCoords[0], centerMapTileCoords[1] };

		/* Draw all the MapTiles (from the upper left to the lower right). */
		for (int y = -additionalTilesNeededToTopOfCenter; y <= additionalTilesNeededToBottomOfCenter; y++) {
			for (int x = -additionalTilesNeededToLeftOfCenter; x <= additionalTilesNeededToRightOfCenter; x++) {
				/*
				 * Add/substract the difference of the tile-position to the one of the center.
				 */
				mapTileCoords[0] = MyMath.mod(centerMapTileCoords[0] + y, mapTileUpperBound);
				mapTileCoords[1] = MyMath.mod(centerMapTileCoords[1] + x, mapTileUpperBound);
				/* Construct a URLString, which represents the MapTile. */
				final String tileURLString = this.mRendererInfo.getTileURLString(mapTileCoords, zoomLevel);

//				Log.i(DEBUGTAG, tileURLString);

				/* Draw the MapTile 'i tileSizePx' above of the centerMapTile */
				final Bitmap currentMapTile = this.mTileProvider.getMapTile(tileURLString,
						this.mRendererInfo.TILE_SOURCE_TYPE, null, 0, 0, 0);
				if (currentMapTile != null) {
					final int tileLeft = mMapView.mTouchMapOffsetX + centerMapTileScreenLeft + (x * tileSizePx);
					final int tileTop = mMapView.mTouchMapOffsetY + centerMapTileScreenTop + (y * tileSizePx);
					final Rect r = new Rect(tileLeft, tileTop, tileLeft+tileSizePx, tileTop+tileSizePx);
					c.drawBitmap(currentMapTile, null, r, this.mPaint);
					if (DEBUGMODE) {
						c.drawText("y x = " + mapTileCoords[0] + " " + mapTileCoords[1] + " zoom " + zoomLevel,
								tileLeft + 5, tileTop + 45, this.mPaint);
					}
				}
			}
		}
	}

	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
	}

	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case OpenStreetMapTileDownloader.MAPTILEDOWNLOADER_SUCCESS_ID:
			case OpenStreetMapTileFilesystemProvider.MAPTILEFSLOADER_SUCCESS_ID:
				mMapView.invalidate();
				break;
			}
		}
	}

	private Point getUpperLeftCornerOfCenterMapTileInScreen(final int[] centerMapTileCoords, final int tileSizePx,
			final Point reuse) {
		final Point out = (reuse != null) ? reuse : new Point();

		final int viewWidth = mMapView.getWidth();
		final int viewWidth_2 = viewWidth / 2;
		final int viewHeight = mMapView.getHeight();
		final int viewHeight_2 = viewHeight / 2;

		/*
		 * Calculate the Latitude/Longitude on the left-upper ScreenCoords of the center MapTile. So in the end we can
		 * determine which MapTiles we additionally need next to the centerMapTile.
		 */
		final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(centerMapTileCoords, mMapView.getZoomLevel(),
				mRendererInfo.PROJECTION);
		final float[] relativePositionInCenterMapTile = bb
				.getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(mMapView.getMapCenterLatitudeE6(),
						mMapView.getMapCenterLongitudeE6(), null);

		final int centerMapTileScreenLeft = viewWidth_2
				- (int) (0.5f + (relativePositionInCenterMapTile[1] * tileSizePx));
		final int centerMapTileScreenTop = viewHeight_2
				- (int) (0.5f + (relativePositionInCenterMapTile[0] * tileSizePx));

		out.set(centerMapTileScreenLeft, centerMapTileScreenTop);
		return out;
	}

}
