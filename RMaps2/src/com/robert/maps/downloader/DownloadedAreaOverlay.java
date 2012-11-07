package com.robert.maps.downloader;

import org.andnav.osm.util.BoundingBoxE6;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.util.Util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;

import com.robert.maps.R;
import com.robert.maps.utils.Ut;
import com.robert.maps.view.TileView;
import com.robert.maps.view.TileViewOverlay;

public class DownloadedAreaOverlay extends TileViewOverlay {
	private GeoPoint mPoint[];
	private int X, Y, Z;
	private Paint mPaint = new Paint();
	private com.robert.maps.view.TileView.OpenStreetMapViewProjection mProj;
	
	public void Init(Context ctx, int lat0, int lon0, int lat1, int lon1) {
		mPoint = new GeoPoint[2];
		mPoint[0] = new GeoPoint(lat0, lon0);
		mPoint[1] = new GeoPoint(lat1, lon1);

		mPaint.setColor(ctx.getResources().getColor(R.color.chart_graph_0));
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(3);
		mPaint.setAntiAlias(true);
		mPaint.setAlpha(180);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setShadowLayer(10.0f, 0, 0, ctx.getResources().getColor(R.color.chart_graph_0));
	}
	
	public void setLastDowloadedTile(int x, int y, int z, TileView tileView) {
		if(Z != z && z >= 0)
			mProj = tileView.getProjection(tileView.getZoomLevel(), 1L / Math.pow(2, tileView.getZoomLevel() - z));
		
		X = x;
		Y = y;
		Z = z;
		
	}
	
	@Override
	protected void onDraw(Canvas c, TileView tileView) {
		if(mPoint == null || mProj == null) return;
		
		final Point p0 = mProj.toPixels(mPoint[0], null);
		final Point p1 = mProj.toPixels(mPoint[1], null);
		final int[] tileCoord = {Y, X};
		
		final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(tileCoord, tileView.getZoomLevel(), tileView.getTileSource().PROJECTION);
		final Point p2 = mProj.toPixels(new GeoPoint(bb.getLatSouthE6(), bb.getLonEastE6()), null);
		final Point p3 = mProj.toPixels(new GeoPoint(bb.getLatNorthE6(), bb.getLonEastE6()), null);
		
		final Path path = new Path();
		path.moveTo(p2.x, p2.y);
		//path.setLastPoint(dx, dy)
//		path.lineTo(p2.x, p2.y);
		path.lineTo(p3.x, p3.y);
		path.lineTo(p1.x, p3.y);
		path.lineTo(p1.x, p1.y);
		path.lineTo(p0.x, p1.y);
		path.lineTo(p0.x, p2.y);
		c.drawPath(path, mPaint);
		
		c.drawCircle(p0.x, p0.y, 10L, mPaint);
		c.drawCircle(p1.x, p1.y, 10L, mPaint);
		c.drawCircle(p2.x, p2.y, 10L, mPaint);
		c.drawCircle(p3.x, p3.y, 10L, mPaint);

		//final Rect rect = new Rect(p0.x, p0.y, p1.x, p1.y);
		//c.drawRect(rect, mPaint);
	}
	
	@Override
	protected void onDrawFinished(Canvas c, TileView tileView) {
		// TODO Auto-generated method stub
		
	}
	
}
