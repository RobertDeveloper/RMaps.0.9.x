package com.robert.maps.overlays;

import java.util.ArrayList;
import java.util.HashMap;
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
	private Context mCtx;
	private PoiManager mPoiManager;
	private NinePatchDrawable mButton;
	private int mTapIndex;
	private GeoPoint mLastMapCenter;
	private int mLastZoom;
	private PoiListThread mThread;

	public int getTapIndex() {
		return mTapIndex;
	}

	public void setTapIndex(int mTapIndex) {
		this.mTapIndex = mTapIndex;
	}

	protected OnItemTapListener<PoiPoint> mOnItemTapListener;
	protected OnItemLongPressListener<PoiPoint> mOnItemLongPressListener;
	protected List<PoiPoint> mItemList;
	protected final Point mMarkerHotSpot;
	protected final int mMarkerWidth, mMarkerHeight;
	private boolean mCanUpdateList = true;
	protected HashMap<Integer, Drawable> mBtnMap;

	public PoiOverlay(Context ctx, PoiManager poiManager,
			OnItemTapListener<PoiPoint> onItemTapListener, boolean hidepoi)
	{
		mCtx = ctx;
		mPoiManager = poiManager;
		mCanUpdateList = !hidepoi;
		Bitmap mBubbleBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.popup_button);
		byte[] chunk = {8,8,31,28}; // left,top,right,bottom
		this.mButton = new NinePatchDrawable(new NinePatch(mBubbleBitmap, chunk, ""));
		mTapIndex = -1;

		Drawable marker = ctx.getResources().getDrawable(R.drawable.poi);
		this.mMarkerWidth = marker.getIntrinsicWidth();
		this.mMarkerHeight = marker.getIntrinsicHeight();

		mBtnMap = new HashMap<Integer, Drawable>();
		mBtnMap.put(new Integer(R.drawable.poi), marker);
		this.mMarkerHotSpot = new Point(0, 45);

        this.mOnItemTapListener = onItemTapListener;

        mLastMapCenter = null;
        mLastZoom = -1;
        mThread = new PoiListThread();

	}

	public void setGpsStatusGeoPoint(final GeoPoint geopoint, final String title, final String descr) {
		PoiPoint poi = new PoiPoint(title, descr, geopoint, R.drawable.poi_satttelite);
		if(mItemList == null)
			mItemList = new ArrayList<PoiPoint>();
		else
			mItemList.clear();

		mItemList.add(poi);
		mCanUpdateList = false;
	}

	@Override
	public void onDraw(Canvas c, OpenStreetMapView mapView) {
		final OpenStreetMapViewProjection pj = mapView.getProjection();
		final Point curScreenCoords = new Point();

		if (mCanUpdateList){
			boolean looseCenter = false;
			GeoPoint center = mapView.getMapCenter();
			GeoPoint lefttop = pj.fromPixels(0, 0);
			double deltaX = Math.abs(center.getLongitude() - lefttop.getLongitude());
			double deltaY = Math.abs(center.getLatitude() - lefttop.getLatitude());

			if (mLastMapCenter == null || mLastZoom != mapView.getZoomLevel())
				looseCenter = true;
			else if(0.7 * deltaX < Math.abs(center.getLongitude() - mLastMapCenter.getLongitude()) || 0.7 * deltaY < Math.abs(center.getLatitude() - mLastMapCenter.getLatitude()))
				looseCenter = true;

			if(looseCenter){
				mLastMapCenter = center;
				mLastZoom = mapView.getZoomLevel();

				mThread.setParams(1.5*deltaX, 1.5*deltaY);
				mThread.run();
			}
		}

		if (this.mItemList != null) {

			/*
			 * Draw in backward cycle, so the items with the least index are on
			 * the front.
			 */
			for (int i = this.mItemList.size() - 1; i >= 0; i--) {
				if (i != mTapIndex) {
					PoiPoint item = this.mItemList.get(i);
					pj.toPixels(item.GeoPoint, curScreenCoords);

					c.save();
					c.rotate(mapView.getBearing(), curScreenCoords.x,
							curScreenCoords.y);

					onDrawItem(c, i, curScreenCoords);

					c.restore();
				}
			}

			if (mTapIndex >= 0 && mTapIndex < this.mItemList.size()) {
				PoiPoint item = this.mItemList.get(mTapIndex);
				pj.toPixels(item.GeoPoint, curScreenCoords);

				c.save();
				c.rotate(mapView.getBearing(), curScreenCoords.x,
						curScreenCoords.y);

				onDrawItem(c, mTapIndex, curScreenCoords);

				c.restore();
			}
		}
	}

	protected void onDrawItem(Canvas c, int index, Point screenCoords) {
		final PoiPoint focusedItem = mItemList.get(index);

		if (index == mTapIndex) {
			int toUp = 8, toRight = 2; // int toUp = 25, toRight = 3;
			int textToRight = 34, widthRightCut = 2, textPadding = 4, maxButtonWidth = 240;
			int h0 = 40; // w0 = 40;// исходный размер

			Ut.TextWriter twTitle = new Ut.TextWriter(maxButtonWidth - textToRight, 14, focusedItem.Title);
			Ut.TextWriter twDescr = new Ut.TextWriter(maxButtonWidth - textToRight, 12, focusedItem.Descr);
			Ut.TextWriter twCoord = new Ut.TextWriter(maxButtonWidth - textToRight, 8, Ut.formatGeoPoint(focusedItem.GeoPoint));

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

		Integer key = new Integer(focusedItem.IconId);
		Drawable marker = null;
		if(mBtnMap.containsKey(key))
			marker = mBtnMap.get(key);
		else {
			try{
				marker = mCtx.getResources().getDrawable(focusedItem.IconId);
			} catch (Exception e) {
				marker = mCtx.getResources().getDrawable(R.drawable.poi);
			}
			mBtnMap.put(key, marker);
		}

		marker.setBounds(left, top, right, bottom);

		marker.draw(c);


//		final int pxUp = 2;
//		final int left2 = screenCoords.x + 5 - pxUp;
//		final int right2 = left + 38 + pxUp;
//		final int top2 = screenCoords.y - this.mMarkerHotSpot.y - pxUp;
//		final int bottom2 = top + 33 + pxUp;
//		Paint p = new Paint();
//		c.drawLine(left2, top2, right2, bottom2, p);
//		c.drawLine(right2, top2, left2, bottom2, p);
	}

	public PoiPoint getPoiPoint(final int index){
		return this.mItemList.get(index);
	}

	public int getMarkerAtPoint(final int eventX, final int eventY, OpenStreetMapView mapView){
		if(this.mItemList != null){
			final OpenStreetMapViewProjection pj = mapView.getProjection();

			final Rect curMarkerBounds = new Rect();
			final Point mCurScreenCoords = new Point();

			for(int i = 0; i < this.mItemList.size(); i++){
				final PoiPoint mItem = this.mItemList.get(i);
				pj.toPixels(mItem.GeoPoint, mapView.getBearing(), mCurScreenCoords);

				final int pxUp = 2;
				final int left = mCurScreenCoords.x + 5 - pxUp;
				final int right = left + 36 + pxUp;
				final int top = mCurScreenCoords.y - this.mMarkerHotSpot.y - pxUp;
				final int bottom = top + 33 + pxUp;

				curMarkerBounds.set(left, top, right, bottom);
				if(curMarkerBounds.contains(eventX, eventY))
					return i;
			}
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



	private class PoiListThread extends Thread {
		private double mdeltaX;
		private double mdeltaY;

		public void setParams(double deltaX, double deltaY){
			mdeltaX = deltaX;
			mdeltaY = deltaY;
		}

		@Override
		public void run() {
			mItemList = mPoiManager.getPoiListNotHidden(mLastZoom, mLastMapCenter, mdeltaX, mdeltaY);

			super.run();
		}

	}


}


