package com.robert.maps.downloader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;

import com.robert.maps.R;
import com.robert.maps.view.TileView;
import com.robert.maps.view.TileViewOverlay;

public class AreaSelectorOverlay extends TileViewOverlay {
	private Rect mRect = new Rect();
	private Paint mPaint = new Paint();
	
	public void Init(Context ctx, TileView tileView, int left, int top, int right, int bottom) {
		mRect.set((int)(tileView.getWidth()*(1-0.8)/2), (int)(tileView.getHeight()*(1-0.8)/2), (int)(tileView.getWidth()*(1+0.8)/2), (int)(tileView.getHeight()*(1+0.8)/2));
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
		c.drawRect(mRect, mPaint);
	}
	
	@Override
	protected void onDrawFinished(Canvas c, TileView tileView) {
		// TODO Auto-generated method stub
		
	}
	
}
