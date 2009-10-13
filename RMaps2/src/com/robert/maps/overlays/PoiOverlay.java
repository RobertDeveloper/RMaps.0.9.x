package com.robert.maps.overlays;

import java.util.ArrayList;
import java.util.List;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import com.robert.maps.R;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.PoiPoint;
import com.robert.maps.utils.NinePatch;
import com.robert.maps.utils.NinePatchDrawable;
import com.robert.maps.utils.Ut;

public class PoiOverlay extends OpenStreetMapViewOverlay {
//	private Context mCtx;
	private PoiManager mPoiManager;
	private NinePatchDrawable mButton;
	private int mTapIndex;

	protected OnItemTapListener<PoiPoint> mOnItemTapListener;
	protected OnItemLongPressListener<PoiPoint> mOnItemLongPressListener;
	protected List<PoiPoint> mItemList;
	protected final Point mMarkerHotSpot;
	protected final Drawable mMarker;
	protected final int mMarkerWidth, mMarkerHeight;
	private boolean mCanUpdateList = true;

	public PoiOverlay(Context ctx, PoiManager poiManager,
			OnItemTapListener<PoiPoint> onItemTapListener) 
	{
//		mCtx = ctx;
		mPoiManager = poiManager;
		Bitmap mBubbleBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.popup_button);
		byte[] chunk = {8,8,31,28}; // left,top,right,bottom
		this.mButton = new NinePatchDrawable(new NinePatch(mBubbleBitmap, chunk, ""));
		mTapIndex = -1;

		this.mMarker = ctx.getResources().getDrawable(R.drawable.poi);
		this.mMarkerHotSpot = new Point(0, 38);

        this.mOnItemTapListener = onItemTapListener;

		this.mMarkerWidth = this.mMarker.getIntrinsicWidth();
		this.mMarkerHeight = this.mMarker.getIntrinsicHeight();
	}
	
	public void setGpsStatusGeoPoint(final GeoPoint geopoint, final String title, final String descr) {
		PoiPoint poi = new PoiPoint(title, descr, geopoint);
		if(mItemList == null)
			mItemList = new ArrayList<PoiPoint>();
		else
			mItemList.clear();
		
		mItemList.add(poi);
		mCanUpdateList = false;
	}
	
	@Override
	public void onDraw(Canvas c, OpenStreetMapView mapView) {
		if(mCanUpdateList)
			this.mItemList = mPoiManager.getPoiList();

		final OpenStreetMapViewProjection pj = mapView.getProjection();

		final Point curScreenCoords = new Point();

		/* Draw in backward cycle, so the items with the least index are on the front. */
		for (int i = this.mItemList.size() - 1; i >= 0; i--) {
			if (i != mTapIndex) {
				PoiPoint item = this.mItemList.get(i);
				pj.toPixels(item.mGeoPoint, curScreenCoords);

				c.save();
				c.rotate(mapView.getBearing(), curScreenCoords.x, curScreenCoords.y);
				
				onDrawItem(c, i, curScreenCoords);

				c.restore();
			}
		}
		
		if(mTapIndex >= 0){
			PoiPoint item = this.mItemList.get(mTapIndex);
			pj.toPixels(item.mGeoPoint, curScreenCoords);

			c.save();
			c.rotate(mapView.getBearing(), curScreenCoords.x, curScreenCoords.y);

			onDrawItem(c, mTapIndex, curScreenCoords);

			c.restore();
		}
	}

	protected void onDrawItem(Canvas c, int index, Point screenCoords) {
		if (index == mTapIndex) {
			int toUp = 1, toRight = 4; // int toUp = 25, toRight = 3;
			int textToRight = 24, widthRightCut = 2, textPadding = 4, maxButtonWidth = 240;
			int h0 = 40; // w0 = 40;// исходный размер
			final PoiPoint focusedItem = mItemList.get(index);

			Ut.TextWriter twTitle = new Ut.TextWriter(maxButtonWidth - textToRight, 14, focusedItem.mTitle);
			Ut.TextWriter twDescr = new Ut.TextWriter(maxButtonWidth - textToRight, 12, focusedItem.mDescr);
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

		final int left = screenCoords.x - this.mMarkerHotSpot.x;
		final int right = left + this.mMarkerWidth;
		final int top = screenCoords.y - this.mMarkerHotSpot.y;
		final int bottom = top + this.mMarkerHeight;

		this.mMarker.setBounds(left, top, right, bottom);

		this.mMarker.draw(c);
		
		
//		final int left2 = screenCoords.x + 5;
//		final int right2 = left + 30;
//		final int top2 = screenCoords.y - this.mMarkerHotSpot.y;
//		final int bottom2 = top + 24;
//		Paint p = new Paint();
//		c.drawLine(left2, top2, right2, bottom2, p);
//		c.drawLine(right2, top2, left2, bottom2, p);
	}
	
	public int getMarkerAtPoint(final int eventX, final int eventY, OpenStreetMapView mapView){
		final OpenStreetMapViewProjection pj = mapView.getProjection();

		final Rect curMarkerBounds = new Rect();
		final Point mCurScreenCoords = new Point();

		for(int i = 0; i < this.mItemList.size(); i++){
			final PoiPoint mItem = this.mItemList.get(i);
			pj.toPixels(mItem.mGeoPoint, mapView.getBearing(), mCurScreenCoords);

//			final int left = mCurScreenCoords.x - this.mMarkerHotSpot.x;
//			final int right = left + markerWidth;
//			final int top = mCurScreenCoords.y - this.mMarkerHotSpot.y;
//			final int bottom = top + markerHeight;
			final int pxUp = 2;
			final int left = mCurScreenCoords.x + 5 - pxUp;
			final int right = left + 30 + pxUp;
			final int top = mCurScreenCoords.y - this.mMarkerHotSpot.y - pxUp;
			final int bottom = top + 24 + pxUp;

			curMarkerBounds.set(left, top, right, bottom);
			if(curMarkerBounds.contains(eventX, eventY))
				return i;
		}
		
		return -1;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event, OpenStreetMapView mapView) {
		final int index = getMarkerAtPoint((int)event.getX(), (int)event.getY(), mapView);
		if (index >= 0)
			if (onTap(index))
				return true;

		return super.onSingleTapUp(event, mapView);
	}

	@Override
	public boolean onLongPress(MotionEvent event, OpenStreetMapView mapView) {
		final int index = getMarkerAtPoint((int)event.getX(), (int)event.getY(), mapView);
		if (index >= 0)
			if (onLongLongPress(index))
				return true;

		return super.onLongPress(event, mapView);
	}

	private boolean onLongLongPress(int index) {
		return false;
//		if(this.mOnItemLongPressListener != null)
//			return this.mOnItemLongPressListener.onItemLongPress(index, this.mItemList.get(index));
//		else
//			return false;
	}

	protected boolean onTap(int index) {
		if(mTapIndex == index)
			mTapIndex = -1;
		else
			mTapIndex = index;

		if(this.mOnItemTapListener != null)
			return this.mOnItemTapListener.onItemTap(index, this.mItemList.get(index));
		else
			return false;
	}

	@SuppressWarnings("hiding")
	public static interface OnItemTapListener<PoiPoint>{
		public boolean onItemTap(final int aIndex, final PoiPoint aItem);
	}

	@SuppressWarnings("hiding")
	public static interface OnItemLongPressListener<PoiPoint>{
		public boolean onItemLongPress(final int aIndex, final PoiPoint aItem);
	}

	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
	}

}


