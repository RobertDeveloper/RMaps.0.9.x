package com.robert.maps.applib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.robert.maps.applib.R;

public class ScreenCellView extends RelativeLayout {

	public ScreenCellView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		View.inflate(context, R.layout.screencell, this);
		
		//this.mT = (RelativeLayout) LayoutInflater.from(ctx).inflate(R.layout.poi_descr, null);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		
	}
	
}
