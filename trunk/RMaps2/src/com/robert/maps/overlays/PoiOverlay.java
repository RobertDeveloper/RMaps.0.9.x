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
import com.robert.maps.utils.Ut;

public class PoiOverlay extends OpenStreetMapViewItemizedOverlay<OpenStreetMapViewOverlayItem> {
//	private Context mCtx;
	private PoiManager mPoiManager;
	private NinePatchDrawable mButton;
	private int mTapIndex;

	public PoiOverlay(Context ctx, PoiManager poiManager,
			OnItemTapListener<OpenStreetMapViewOverlayItem> onItemTapListener) {
		super(ctx, poiManager.getPoiList(), ctx.getResources().getDrawable(R.drawable.poi), new Point(0, 38), onItemTapListener);

//		mCtx = ctx;
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
		//FIXME Активная точка должна рисоваться последней
		if (mTapIndex == index) {
			int toUp = 1, toRight = 4; // int toUp = 25, toRight = 3;
			int textToRight = 24, widthRightCut = 2, textPadding = 4, maxButtonWidth = 240;
			int h0 = 40; // w0 = 40;// исходный размер
			final OpenStreetMapViewOverlayItem focusedItem = super.mItemList.get(index);

			Ut.TextWriter twTitle = new Ut.TextWriter(maxButtonWidth - textToRight, 14, focusedItem.mTitle);
			Ut.TextWriter twDescr = new Ut.TextWriter(maxButtonWidth - textToRight, 12, focusedItem.mDescription);
			Ut.TextWriter twCoord = new Ut.TextWriter(maxButtonWidth - textToRight, 8, Ut.formatGeoPoint(focusedItem.mGeoPoint));

			final int buttonHegth = 10 + twTitle.getHeight() + twDescr.getHeight() + twCoord.getHeight() + 3*textPadding;
			final int buttonWidth = Math.max(twCoord.getWidth(), Math.max(twTitle.getWidth(),twDescr.getWidth())) + textToRight + widthRightCut + 2 * textPadding;// конечный размер

			mButton.setBounds(screenCoords.x + toRight, screenCoords.y - h0 - toUp, screenCoords.x + buttonWidth + toRight,
					screenCoords.y -h0 + buttonHegth - toUp);
			mButton.draw(c);

			twTitle.Draw(c, screenCoords.x + toRight + textToRight + textPadding, screenCoords.y - h0 - toUp + textPadding - 1);
			twDescr.Draw(c, screenCoords.x + toRight + textToRight + textPadding, twTitle.getHeight() + screenCoords.y - h0 - toUp + 2*textPadding - 1);
			twCoord.Draw(c, screenCoords.x + toRight + textToRight + textPadding, twTitle.getHeight() + twDescr.getHeight() + screenCoords.y - h0 - toUp + 3*textPadding - 1);
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
