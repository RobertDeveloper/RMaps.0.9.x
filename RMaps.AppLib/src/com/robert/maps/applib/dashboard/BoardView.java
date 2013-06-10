package com.robert.maps.applib.dashboard;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class BoardView extends LinearLayout {
//	private IndicatorManager mIndicatorManager;
//
//	public BoardView(Context context, IndicatorManager indicatorManager) {
//		super(context);
//		
//		mIndicatorManager = indicatorManager;
//		
//		setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
//		setOrientation(LinearLayout.VERTICAL);
//		
//		LinearLayout msgbox = null;
//		if(msgbox == null) {
//			msgbox = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.error_message_box, this);
//			msgbox.setVisibility(View.VISIBLE);
//		};
//
//		((TextView) msgbox.findViewById(R.id.descr)).setText("jsdlksjdfhs");

//		TextView tv = new TextView(context);
//		tv.setText("sdfsdfsdf");
//		tv.setVisibility(View.VISIBLE);
//		addView(tv, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
//		LinearLayout ll = new LinearLayout(context);
//		ll.setOrientation(LinearLayout.HORIZONTAL);
//		
//		final LayoutInflater inflater = LayoutInflater.from(context);
//		RelativeLayout rel = (RelativeLayout) inflater.inflate(R.layout.indicator_simple, null, false);
//		((TextView) rel.findViewById(R.id.title)).setText("sdfsdf");
//		((TextView) rel.findViewById(R.id.units)).setText("sdfsdf");
//		((TextView) rel.findViewById(R.id.indicator)).setText("sdfsdf");
//		
//		ll.addView(rel);
//		
//		addView(ll, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
//	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

	}

	public BoardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public BoardView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

}
