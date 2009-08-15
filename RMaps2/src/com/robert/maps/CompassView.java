package com.robert.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

public class CompassView extends View {
	private Drawable mCompass;
	private float mAzimuth = 0;
	private boolean mSideBottom;
	private int PADDING = 2;

	public CompassView(Context ctx, final boolean sideBottom) {
		super(ctx);

		this.mCompass = ctx.getResources().getDrawable(R.drawable.arrow_n);
		mSideBottom = sideBottom;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.save();
		canvas.rotate(360 - mAzimuth, mCompass.getMinimumWidth()/2, mCompass.getMinimumHeight()/2);
		if(mSideBottom)
			mCompass.setBounds(PADDING, PADDING, PADDING+mCompass.getMinimumWidth(), PADDING+mCompass.getMinimumHeight());
		else
			mCompass.setBounds(PADDING, this.getHeight() - mCompass.getMinimumHeight() - PADDING, PADDING+mCompass.getMinimumWidth(), this.getHeight()-PADDING);
		mCompass.draw(canvas);
		canvas.restore();

		super.onDraw(canvas);
	}

	public void setAzimuth(float aAzimuth) {
		mAzimuth = aAzimuth;
	}

}
