package com.robert.maps.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class NinePatchDrawable extends Drawable
{
	private NinePatch mNinePatch;
	private Paint mPaint = new Paint();
	@SuppressWarnings("unused")
	private Context mContext;

	public NinePatchDrawable(NinePatch ninePatch)
	{
		mNinePatch = ninePatch;
		mPaint.setAlpha(0xff);
	}

	@Override
	public void draw(Canvas canvas)
	{
		mNinePatch.draw(canvas, getBounds(), mPaint);
	}

	@Override
	public int getOpacity()
	{
		return 0;
	}

	@Override
	public void setAlpha(int alpha)
	{
		mPaint.setAlpha(alpha);
	}

	/* (non-Javadoc)
	 * @see android.graphics.drawable.Drawable#setColorFilter(android.graphics.ColorFilter)
	 */
	@Override
	public void setColorFilter(ColorFilter cf)
	{
	}
	
	/* non-Android APIs */
	
	public void __setContext(Context context)
	{
		mContext = context;
	}
	
	public NinePatch __getNinePatch()
	{
		return mNinePatch;
	}
}