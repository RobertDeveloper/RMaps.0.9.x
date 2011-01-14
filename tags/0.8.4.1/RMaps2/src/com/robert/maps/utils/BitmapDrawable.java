package com.robert.maps.utils;


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

    public BitmapDrawable(Context ctx) {
        mPaint.setColor(ctx.getResources().getColor(android.R.color.black));
        mPaint2.setColor(ctx.getResources().getColor(android.R.color.white));

        mPaint3.setColor(ctx.getResources().getColor(android.R.color.black));
        mPaint3.setAntiAlias(true);
        mPaint3.setTextSize(15);
        mPaint4.setColor(ctx.getResources().getColor(android.R.color.white));
        mPaint4.setAntiAlias(true);
        mPaint4.setTextSize(15);
        //mPaint4.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
//		mPaint4.setStrokeWidth(4);
//		mPaint4.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
    }

    @Override
    public void draw(Canvas canvas) {
        final int w = 100, h = 13, margin = 7;
        canvas.drawRect(margin+0, 0, margin+w+2, 4, mPaint2);
        canvas.drawRect(margin+0, 0, margin+4, h+2, mPaint2);
        canvas.drawRect(margin+w+2-4, 0, margin+w+2, h+2, mPaint2);

        canvas.drawRect(margin+1, 1, margin+w+1, 3, mPaint);
        canvas.drawRect(margin+1, 1, margin+3, h+1, mPaint);
        canvas.drawRect(margin+w+1-2, 1, margin+w+1, h+1, mPaint);

        canvas.drawText("5000 km", margin+7-1, 2+mPaint3.getTextSize()-1, mPaint4);
        canvas.drawText("5000 km", margin+7+1, 2+mPaint3.getTextSize()+1, mPaint4);
        canvas.drawText("5000 km", margin+7, 2+mPaint3.getTextSize(), mPaint3);
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
