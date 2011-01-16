package com.robert.maps.utils;


import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class BitmapDrawable extends Drawable {

    private Paint mPaint = new Paint();
    private Paint mPaint2 = new Paint();
    private Paint mPaint3 = new Paint();
    private Paint mPaint4 = new Paint();
    private OpenStreetMapView mOsmv;
    private int mZoomLevel = -1;
    private double mTouchScale = 1;
    private String mDist = "";
    private int mWidth = 100;

    public BitmapDrawable(Context ctx, OpenStreetMapView osmv) {
    	mOsmv = osmv;

        mPaint.setColor(ctx.getResources().getColor(android.R.color.black));
        mPaint2.setColor(ctx.getResources().getColor(android.R.color.white));
        mPaint3.setColor(ctx.getResources().getColor(android.R.color.black));
        mPaint3.setAntiAlias(true);
        mPaint3.setTextSize(15);
        mPaint4.setColor(ctx.getResources().getColor(android.R.color.white));
        mPaint4.setAntiAlias(true);
        mPaint4.setTextSize(15);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
    }

    @Override
    public void draw(Canvas canvas) {
		final int h = 13, margin = 7;

        if(mZoomLevel != mOsmv.getZoomLevel() || mTouchScale != mOsmv.mTouchScale){
        	mZoomLevel = mOsmv.getZoomLevel();
        	mTouchScale = mOsmv.mTouchScale;

    		final OpenStreetMapViewProjection pr = mOsmv.getProjection();
    		final GeoPoint geop = pr.fromPixels(0, 0);
    		final GeoPoint geop2 = pr.fromPixels((float) (100 / mTouchScale), 0);
    		final int dist1 = geop.distanceTo(geop2);
    		int dist = 0;
    		for(int i = 10; i > 0; i -= 1){
    			final double div = Math.pow(10, i);
    			if(dist1 > div){
    				dist = (int) (div*((int)(dist1/div)));
    				mWidth = (int) (100 * dist1 / dist);
    				break;
    			}
    		}
    		if(dist > 999)
    			mDist = ""+(dist/1000)+" km";
    		else
    			mDist = ""+dist+" m";
    	}

        canvas.drawRect(margin+0, 0, margin+mWidth+2, 4, mPaint2);
        canvas.drawRect(margin+0, 0, margin+4, h+2, mPaint2);
        canvas.drawRect(margin+mWidth+2-4, 0, margin+mWidth+2, h+2, mPaint2);

        canvas.drawRect(margin+1, 1, margin+mWidth+1, 3, mPaint);
        canvas.drawRect(margin+1, 1, margin+3, h+1, mPaint);
        canvas.drawRect(margin+mWidth+1-2, 1, margin+mWidth+1, h+1, mPaint);

        canvas.drawText(mDist, margin+7-1, 2+mPaint3.getTextSize()-1, mPaint4);
        canvas.drawText(mDist, margin+7+1, 2+mPaint3.getTextSize()+1, mPaint4);
        canvas.drawText(mDist, margin+7, 2+mPaint3.getTextSize(), mPaint3);
    }

    @Override
    public int getIntrinsicWidth() {
    	return 200;
    }

    @Override
    public int getIntrinsicHeight() {
    	return 22;
    }

	@Override
	public int getOpacity() {
		return 0;
	}

	@Override
	public void setAlpha(int alpha) {

	}

	@Override
	public void setColorFilter(ColorFilter cf) {

	}
}
