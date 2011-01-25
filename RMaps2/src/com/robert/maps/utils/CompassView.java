package com.robert.maps.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.robert.maps.R;

public class CompassView extends View {
	private Drawable mCompass;
	private float mAzimuth = 0;
	private boolean mSideBottom;
	private int PADDING = 2;
	private Context mCtx;

	public CompassView(Context ctx, final boolean sideBottom) {
		super(ctx);

		mCtx = ctx;
		mSideBottom = sideBottom;
	}

	private boolean getCompassImg(){
		if(mCompass == null){
			try {
				this.mCompass = mCtx.getResources().getDrawable(R.drawable.arrow_n);
			} catch (OutOfMemoryError e) {
				Ut.w("OutOfMemoryError");
				e.printStackTrace();
				return false;
			}
			return true;
		} else
			return true;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if(getCompassImg()){
			canvas.save();
			if (mSideBottom) {
				canvas.rotate(360 - mAzimuth, PADDING + mCompass.getMinimumWidth() / 2, PADDING + mCompass.getMinimumHeight() / 2);
				mCompass.setBounds(PADDING, PADDING, PADDING + mCompass.getMinimumWidth(), PADDING
						+ mCompass.getMinimumHeight());
			} else {
				canvas.rotate(360 - mAzimuth, PADDING + mCompass.getMinimumWidth() / 2, this.getHeight() - mCompass.getMinimumHeight() /2 - PADDING);
				mCompass.setBounds(PADDING, this.getHeight() - mCompass.getMinimumHeight() - PADDING, PADDING
						+ mCompass.getMinimumWidth(), this.getHeight() - PADDING);
			}
			mCompass.draw(canvas);
			canvas.restore();
		}

		super.onDraw(canvas);
	}

	public void setAzimuth(float aAzimuth) {
		mAzimuth = aAzimuth;
	}

}
