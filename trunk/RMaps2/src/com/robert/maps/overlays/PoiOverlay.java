package com.robert.maps.overlays;

import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.OpenStreetMapViewItemizedOverlay;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlayItem;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;

import com.robert.maps.R;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.utils.NinePatch;
import com.robert.maps.utils.NinePatchDrawable;

public class PoiOverlay extends OpenStreetMapViewItemizedOverlay<OpenStreetMapViewOverlayItem> {
	private PoiManager mPoiManager;
	private NinePatchDrawable mButton;
	private int mTapIndex;

	public PoiOverlay(Context ctx, PoiManager poiManager,
			OnItemTapListener<OpenStreetMapViewOverlayItem> onItemTapListener) {
		super(ctx, poiManager.getPoiList(), ctx.getResources().getDrawable(R.drawable.poi), new Point(0, 38), onItemTapListener);
		mPoiManager = poiManager;
		Bitmap mBubbleBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.popup_button);
		byte[] chunk = {8,8,31,28}; // left,top,right,bottom
		this.mButton = new NinePatchDrawable(new NinePatch(mBubbleBitmap, chunk, ""));
		mTapIndex = -1;
	}

	@Override
	public void onDraw(Canvas c, OpenStreetMapView mapView) {
		super.mItemList = mPoiManager.getPoiList();
		super.onDraw(c, mapView);
	}

	@Override
	protected void onDrawItem(Canvas c, int index, Point screenCoords) {
		if (mTapIndex == index) {
			int toUp = 1, toRight = 4; // int toUp = 25, toRight = 3;
			int h = 70, w = 160;// конечный размер
			int h0 = 40; // w0 = 40;// исходный размер
			mButton.setBounds(screenCoords.x + toRight, screenCoords.y - h0 - toUp, screenCoords.x + w + toRight,
					screenCoords.y -h0 + h - toUp);
			mButton.draw(c);
		}

		super.onDrawItem(c, index, screenCoords);
	}

	@Override
	protected boolean onTap(int index) {
		if(mTapIndex == index)
			mTapIndex = -1;
		else
			mTapIndex = index;
		return super.onTap(index);
	}


}
