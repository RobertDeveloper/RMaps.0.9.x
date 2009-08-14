package com.robert.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

public class CompassView extends View {
	private Drawable mCompass;
	private float mAzimuth = 0;

	public CompassView(Context ctx) {
		super(ctx);

		this.mCompass = ctx.getResources().getDrawable(R.drawable.arrow_n);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.save();
		canvas.rotate(360 - mAzimuth, mCompass.getMinimumWidth()/2, mCompass.getMinimumHeight()/2);
		mCompass.setBounds(0, 0, mCompass.getMinimumWidth(), mCompass.getMinimumHeight());
		mCompass.draw(canvas);
		canvas.restore();

		super.onDraw(canvas);
	}

	public void setAzimuth(float aAzimuth) {
		mAzimuth = aAzimuth;
	}

}
