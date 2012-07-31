package com.robert.maps.downloader;

import org.andnav.osm.util.GeoPoint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.view.MotionEvent;

import com.robert.maps.R;
import com.robert.maps.utils.Ut;
import com.robert.maps.view.TileView;
import com.robert.maps.view.TileViewOverlay;

public class AreaSelectorOverlay extends TileViewOverlay {
	private Rect mRect = new Rect();
	private Paint mPaint = new Paint();
	private GeoPoint point[] = {new GeoPoint(0,0), new GeoPoint(0,0)};
	private int mPointHolded = -1; 
	
	public void Init(Context ctx, TileView tileView, int left, int top, int right, int bottom) {
		mRect.set(left, top, right, bottom);
		
		mPaint.setColor(ctx.getResources().getColor(R.color.chart_graph_0));
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(3);
		mPaint.setAntiAlias(true);
		mPaint.setAlpha(180);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setShadowLayer(10.0f, 0, 0, ctx.getResources().getColor(R.color.chart_graph_0));
	}
	
	public void Init(Context ctx, TileView tileView) {
		Init(ctx, tileView, (int)(tileView.getWidth()*(1-0.8)/2), (int)(tileView.getHeight()*(1-0.8)/2), (int)(tileView.getWidth()*(1+0.8)/2), (int)(tileView.getHeight()*(1+0.8)/2));
	}
	
	@Override
	protected void onDraw(Canvas c, TileView tileView) {
		final com.robert.maps.view.TileView.OpenStreetMapViewProjection pj = tileView.getProjection();
		
		if(point[0].getLatitudeE6()+point[0].getLongitudeE6() == 0) {
			point[0] = pj.fromPixels((float)(tileView.getWidth()*(1-0.7)/2), (float)(tileView.getHeight()*(1-0.7)/2));
			point[1] = pj.fromPixels((float)(tileView.getWidth()*(1+0.7)/2), (float)(tileView.getHeight()*(1+0.7)/2));
		}
		
		final Point p0 = pj.toPixels(point[0], null);
		final Point p1 = pj.toPixels(point[1], null);
		
		mRect.set(p0.x, p0.y, p1.x, p1.y);
		
		c.drawRect(mRect, mPaint);
	}
	
	@Override
	protected void onDrawFinished(Canvas c, TileView tileView) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY, TileView tileView) {
		if(mPointHolded >= 0) {
			Ut.d("distanceX = "+distanceX+" distanceY = "+distanceY);
			final com.robert.maps.view.TileView.OpenStreetMapViewProjection pj = tileView.getProjection();
			final Point p0 = pj.toPixels(point[0], null);
			final Point p1 = pj.toPixels(point[1], null);
			
			final GeoPoint g0 = pj.fromPixels(-distanceX + p0.x, -distanceY + p0.y);
			final GeoPoint g1 = pj.fromPixels(-distanceX + p1.x, -distanceY + p1.y);

			switch(mPointHolded) {
			case 0:
				point[0].setCoordsE6(g0.getLatitudeE6(), g0.getLongitudeE6());
				break;
			case 1:
				point[0].setLatitudeE6(g0.getLatitudeE6());
				point[1].setLongitudeE6(g1.getLongitudeE6());
				break;
			case 2:
				point[0].setCoordsE6(g1.getLatitudeE6(), g1.getLongitudeE6());
				break;
			case 3:
				point[0].setLatitudeE6(g1.getLatitudeE6());
				point[1].setLatitudeE6(g0.getLongitudeE6());
				break;
			}
		
			tileView.invalidate();
			return true;
		}
		
		return super.onScroll(e1, e2, distanceX, distanceY, tileView);
	}
	
	private void setAreaBound(Rect bound, int x, int y) {
		final int area = 20;
		bound.set(x - area, y - area, x + area, y + area);
	}

	@Override
	public boolean onDown(MotionEvent e, TileView tileView) {
		final com.robert.maps.view.TileView.OpenStreetMapViewProjection pj = tileView.getProjection();
		final Point p0 = pj.toPixels(point[0], null);
		final Point p1 = pj.toPixels(point[1], null);
		
		final Rect bouds[] = {new Rect(), new Rect(), new Rect(), new Rect()};
		setAreaBound(bouds[0], p0.x, p0.y);
		setAreaBound(bouds[1], p1.x, p0.y);
		setAreaBound(bouds[2], p1.x, p1.y);
		setAreaBound(bouds[3], p0.x, p1.y);
		for(int i = 0; i < 4; i++) {
			if(bouds[i].contains((int)e.getX(), (int)e.getY())) {
				mPointHolded = i;
				return true;
			}
		}

		return super.onDown(e, tileView);
	}
	
}
