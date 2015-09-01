package com.robert.maps.applib.kml.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.robert.maps.applib.view.TileView;
import com.robert.maps.applib.view.TileViewOverlay;

public class TrackStyleOverlay extends TileViewOverlay {
	private Paint mPaint = null;
	private Path mPath = null;
	
	public void setPaint(final Paint paint) {
		mPaint = paint;
	}
	
	@Override
	protected void onDraw(Canvas c, TileView tileView) {
		if(mPaint == null) return;
		if(mPath == null) {
			final int left = tileView.getWidth() / 10;
			final int step = (tileView.getWidth() - 2 * left) / 3;
			final int top = tileView.getHeight() / 4;
			final int cent_v = tileView.getHeight() / 2;

			mPath = new Path();
			mPath.setLastPoint(left, cent_v);
			mPath.lineTo(left + step, top);
			mPath.lineTo(left + 2 * step, tileView.getHeight() - top);
			mPath.lineTo(left + 3 * step, cent_v);
		}
		
		c.drawPath(mPath, mPaint);
	}
	
	@Override
	protected void onDrawFinished(Canvas c, TileView tileView) {
	}
	
}
