package com.robert.maps.applib.utils;


import org.andnav.osm.util.GeoPoint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.robert.maps.applib.view.MapView;
import com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection;

public class ScaleBarDrawable extends Drawable {

    private Paint mPaint = new Paint();
    private Paint mPaint2 = new Paint();
    private Paint mPaint3 = new Paint();
    private Paint mPaint4 = new Paint();
    private MapView mOsmv;
    private int mZoomLevel = -1;
    private double mTouchScale = 1;
    private String mDist = "";
    private int mWidth = 100;
    private int mUnits;
    private int mWidth2 = 100;

    private static final int SCALE[][] = {{25000000,15000000,8000000,4000000,2000000,1000000,500000,250000,100000,50000,25000,15000,8000,4000,2000,1000,500,250,100,50}
    ,{15000,8000,4000,2000,1000,500,250,100,50,25,15,8,21120,10560,5280,3000,1500,500,250,100}};

    public ScaleBarDrawable(Context ctx, MapView osmv, int units) {
    	mOsmv = osmv;
    	mUnits = units;

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
		final int h = 13, h2 = 6, margin = 7;

    	mZoomLevel = mOsmv.getZoomLevel();
    	mTouchScale = mOsmv.getTouchScale();
		final OpenStreetMapViewProjection pj = mOsmv.getTileView().getProjection();
		final GeoPoint center = mOsmv.getMapCenter();
		
		int dist = SCALE[mUnits][Math.max(0, Math.min(19, mZoomLevel + 1 + (int)(mTouchScale > 1 ? Math.round(mTouchScale)-1 : -Math.round(1/mTouchScale)+1)))];
		final GeoPoint c2 = center.calculateEndingGlobalCoordinates(center, 90, dist);
		final Point p = new Point();
		pj.toPixels(c2, p);
		mWidth = p.x - mOsmv.getWidth() / 2;
   		mWidth2 = (int) mWidth / 2;

		if(mUnits == 0){
    		if(dist > 999) {
    			mDist = ""+(dist/1000)+" km";
    		} else
    			mDist = ""+dist+" m";
		} else {
			if(mZoomLevel < 11){
	    		mDist = ""+dist+" ml";
			} else if(dist > 5280) {
	    		mDist = ""+dist+" ml";
    		} else {
	    		mDist = ""+dist+" ft";
    		}
		}
        
        canvas.drawRect(margin+0, 0, margin+mWidth+2, 4, mPaint2);
        canvas.drawRect(margin+0, 0, margin+4, h+2, mPaint2);
        canvas.drawRect(margin+mWidth+2-4, 0, margin+mWidth+2, h+2, mPaint2);
        canvas.drawRect(margin+mWidth2+2-4, 0, margin+mWidth2+2, h2+2, mPaint2);

        canvas.drawRect(margin+1, 1, margin+mWidth+1, 3, mPaint);
        canvas.drawRect(margin+1, 1, margin+3, h+1, mPaint);
        canvas.drawRect(margin+mWidth+1-2, 1, margin+mWidth+1, h+1, mPaint);
        canvas.drawRect(margin+mWidth2+1-2, 1, margin+mWidth2+1, h2+1, mPaint);

        canvas.drawText(mDist, margin+7-1, 2+mPaint3.getTextSize()-1, mPaint4);
        canvas.drawText(mDist, margin+7+1, 2+mPaint3.getTextSize()+1, mPaint4);
        canvas.drawText(mDist, margin+7, 2+mPaint3.getTextSize(), mPaint3);
    }

    @Override
    public int getIntrinsicWidth() {
    	return 300; // *320/240;
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
