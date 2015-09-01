package com.robert.maps.applib.downloader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;

import com.robert.maps.R;
import com.robert.maps.applib.view.TileView;
import com.robert.maps.applib.view.TileViewOverlay;

import org.andnav.osm.util.BoundingBoxE6;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.util.Util;

public class DownloadedAreaOverlay extends TileViewOverlay {
	private GeoPoint mPoint[];
	private GeoPoint mBasePoint[];
	private int X, Y, Z;
	private Paint mPaint = new Paint();
	private Paint mPaintFill = new Paint();
	private com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection mProj;
	
	public void Init(Context ctx, int lat0, int lon0, int lat1, int lon1) {
		mBasePoint = new GeoPoint[2];
		mBasePoint[0] = new GeoPoint(lat0, lon0);
		mBasePoint[1] = new GeoPoint(lat1, lon1);

		mPaint.setColor(ctx.getResources().getColor(R.color.download_area));
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(3);
		mPaint.setAntiAlias(true);
		mPaint.setAlpha(180);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setShadowLayer(10.0f, 0, 0, ctx.getResources().getColor(R.color.download_area));

		mPaintFill.setColor(ctx.getResources().getColor(R.color.download_area));
		mPaintFill.setStyle(Style.FILL);
		mPaintFill.setAntiAlias(true);
		mPaintFill.setAlpha(80);
	}
	
	public void setLastDowloadedTile(int x, int y, int z, TileView tileView) {
		if(Z != z && z >= 0) {
			if(mProj == null)
				mProj = tileView.getProjection();
			
			final int[] tile0 = Util.getMapTileFromCoordinates(mBasePoint[0].getLatitudeE6(), mBasePoint[0].getLongitudeE6(), z, null, tileView.getTileSource().PROJECTION);
			final int[] tile1 = Util.getMapTileFromCoordinates(mBasePoint[1].getLatitudeE6(), mBasePoint[1].getLongitudeE6(), z, null, tileView.getTileSource().PROJECTION);
			
			final BoundingBoxE6 bb0 = Util.getBoundingBoxFromMapTile(tile0, z, tileView.getTileSource().PROJECTION);
			final BoundingBoxE6 bb1 = Util.getBoundingBoxFromMapTile(tile1, z, tileView.getTileSource().PROJECTION);
			
			mPoint = new GeoPoint[2];
			mPoint[0] = new GeoPoint(bb0.getLatNorthE6(), bb0.getLonWestE6());
			mPoint[1] = new GeoPoint(bb1.getLatSouthE6(), bb1.getLonEastE6());
		}
		
		if(z > Z) {
			X = x;
			Y = y;
			Z = z;
		} else if(y > Y) {
			X = x;
			Y = y;
		} else if(x > X) {
			X = x;
		}
		
	}
	
	@Override
	protected void onDraw(Canvas c, TileView tileView) {
		if(mPoint == null || mProj == null) return;
		
		mProj = tileView.getProjection();
		
		final Point p0 = mProj.toPixels(mPoint[0], null);
		final Point p1 = mProj.toPixels(mPoint[1], null);
		
		final int[] tileCoord = {Y, X};
		final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(tileCoord, Z, tileView.getTileSource().PROJECTION);
		final Point p2 = mProj.toPixels(new GeoPoint(bb.getLatNorthE6(), bb.getLonWestE6()), null);
		final Point p3 = mProj.toPixels(new GeoPoint(bb.getLatSouthE6(), bb.getLonEastE6()), null);
		
		final Path path = new Path();
		path.moveTo(p1.x, p1.y);
		if(p1.x == p3.x && p1.y != p3.y) {
			path.lineTo(p1.x, p3.y);
			path.lineTo(p0.x, p3.y);
			path.lineTo(p0.x, p1.y);
		} else {
			path.lineTo(p1.x, p2.y);
			if(p1.y == p3.y) {
				path.lineTo(p3.x, p2.y);
				path.lineTo(p2.x, p2.y);
				path.lineTo(p2.x, p1.y);
			} else {
				path.lineTo(p3.x, p2.y);
				path.lineTo(p3.x, p3.y);
				path.lineTo(p0.x, p3.y);
				path.lineTo(p0.x, p1.y);
			}
		}
		path.close();
		c.drawPath(path, mPaintFill);
		c.drawPath(path, mPaint);
	}
	
	@Override
	protected void onDrawFinished(Canvas c, TileView tileView) {
		// TODO Auto-generated method stub
		
	}

	public void downloadDone() {
		mPoint = null;
	}
	
}
