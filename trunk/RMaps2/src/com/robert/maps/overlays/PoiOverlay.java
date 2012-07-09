package com.robert.maps.overlays;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.robert.maps.R;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.PoiPoint;
import com.robert.maps.utils.Ut;
import com.robert.maps.view.TileView;
import com.robert.maps.view.TileViewOverlay;

public class PoiOverlay extends TileViewOverlay {
	private Context mCtx;
	private PoiManager mPoiManager;
	private int mTapIndex;
	private GeoPoint mLastMapCenter;
	private int mLastZoom;
	private PoiListThread mThread;
	private RelativeLayout mT;
	private float mDensity;
	private boolean mNeedUpdateList = false;

	protected OnItemTapListener<PoiPoint> mOnItemTapListener;
	protected OnItemLongPressListener<PoiPoint> mOnItemLongPressListener;
	protected List<PoiPoint> mItemList;
	protected final Point mMarkerHotSpot;
	protected final int mMarkerWidth, mMarkerHeight;
	private boolean mCanUpdateList = true;
	protected HashMap<Integer, Drawable> mBtnMap;

	public int getTapIndex() {
		return mTapIndex;
	}

	public void setTapIndex(int mTapIndex) {
		this.mTapIndex = mTapIndex;
	}
	
	public void UpdateList() {
		mNeedUpdateList = true;
	}

	public PoiOverlay(Context ctx, PoiManager poiManager,
			OnItemTapListener<PoiPoint> onItemTapListener, boolean hidepoi)
	{
		mCtx = ctx;
		mPoiManager = poiManager;
		mCanUpdateList = !hidepoi;
		mTapIndex = -1;

		Drawable marker = ctx.getResources().getDrawable(R.drawable.poi);
		this.mMarkerWidth = marker.getIntrinsicWidth();
		this.mMarkerHeight = marker.getIntrinsicHeight();

		mBtnMap = new HashMap<Integer, Drawable>();
		mBtnMap.put(new Integer(R.drawable.poi), marker);
		this.mMarkerHotSpot = new Point(0, mMarkerHeight);

        this.mOnItemTapListener = onItemTapListener;

        mLastMapCenter = null;
        mLastZoom = -1;
        mThread = new PoiListThread();

		this.mT = (RelativeLayout) LayoutInflater.from(ctx).inflate(R.layout.poi_descr, null);
		this.mT.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		DisplayMetrics metrics = new DisplayMetrics();
		((Activity) ctx).getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mDensity = metrics.density;
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
	public void onDraw(Canvas c, TileView mapView) {
		final com.robert.maps.view.TileView.OpenStreetMapViewProjection pj = mapView.getProjection();
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

			if(looseCenter || mNeedUpdateList){
				mLastMapCenter = center;
				mLastZoom = mapView.getZoomLevel();
				mNeedUpdateList = false;

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
			final ImageView pic = (ImageView) mT.findViewById(R.id.pic);
			final TextView title = (TextView) mT.findViewById(R.id.poi_title);
			final TextView descr = (TextView) mT.findViewById(R.id.descr);
			final TextView coord = (TextView) mT.findViewById(R.id.coord);
			
			pic.setImageResource(focusedItem.IconId);
			title.setText(focusedItem.Title);
			descr.setText(focusedItem.Descr);
			coord.setText(Ut.formatGeoPoint(focusedItem.GeoPoint));

			mT.measure(0, 0);
			mT.layout(0, 0, mT.getMeasuredWidth(), mT.getMeasuredHeight());
			
			c.save();
			c.translate(screenCoords.x, screenCoords.y - pic.getMeasuredHeight() - pic.getTop());
			mT.draw(c);
			c.restore();
			
		} else {

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

		if(OpenStreetMapViewConstants.DEBUGMODE){
			final int pxUp = 2;
			final int left2 = (int)(screenCoords.x + mDensity*(5 - pxUp));
			final int right2 = (int)(screenCoords.x + mDensity*(38 + pxUp));
			final int top2 = (int)(screenCoords.y - this.mMarkerHotSpot.y - mDensity*(pxUp));
			final int bottom2 = (int)(top2 + mDensity*(33 + pxUp));
			Paint p = new Paint();
			c.drawLine(left2, top2, right2, bottom2, p);
			c.drawLine(right2, top2, left2, bottom2, p);
			
			c.drawLine(screenCoords.x - 5, screenCoords.y - 5, screenCoords.x + 5, screenCoords.y + 5, p);
			c.drawLine(screenCoords.x - 5, screenCoords.y + 5, screenCoords.x + 5, screenCoords.y - 5, p);
			}
		}
	}

	public PoiPoint getPoiPoint(final int index){
		return this.mItemList.get(index);
	}

	public int getMarkerAtPoint(final int eventX, final int eventY, TileView mapView){
		if(this.mItemList != null){
			final com.robert.maps.view.TileView.OpenStreetMapViewProjection pj = mapView.getProjection();

			final Rect curMarkerBounds = new Rect();
			final Point mCurScreenCoords = new Point();

			 
			for(int i = 0; i < this.mItemList.size(); i++){
				final PoiPoint mItem = this.mItemList.get(i);
				pj.toPixels(mItem.GeoPoint, mapView.getBearing(), mCurScreenCoords);

				final int pxUp = 2;
				final int left = (int)(mCurScreenCoords.x + mDensity*(5 - pxUp));
				final int right = (int)(mCurScreenCoords.x + mDensity*(38 + pxUp));
				final int top = (int)(mCurScreenCoords.y - this.mMarkerHotSpot.y - mDensity*(pxUp));
				final int bottom = (int)(top + mDensity*(33 + pxUp));
				Ut.d("event "+eventX+" "+eventY);
				Ut.d("bounds "+left+"-"+right+" "+top+"-"+bottom);

				curMarkerBounds.set(left, top, right, bottom);
				if(curMarkerBounds.contains(eventX, eventY))
					return i;
			}
		}

		return -1;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event, TileView mapView) {
		final int index = getMarkerAtPoint((int)event.getX(), (int)event.getY(), mapView);
		if (index >= 0)
			if (onTap(index))
				return true;

		return super.onSingleTapUp(event, mapView);
	}

	@Override
	public boolean onLongPress(MotionEvent event, TileView mapView) {
		final int index = getMarkerAtPoint((int)event.getX(), (int)event.getY(), mapView);
		mapView.mPoiMenuInfo.MarkerIndex = index;
		mapView.mPoiMenuInfo.EventGeoPoint = mapView.getProjection().fromPixels((int)event.getX(), (int)event.getY(), mapView.getBearing());
		if (index >= 0)
			if (onLongLongPress(index))
				return true;

		return super.onLongPress(event, mapView);
	}

	private boolean onLongLongPress(int index) {
		return false;
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
	protected void onDrawFinished(Canvas c, TileView osmv) {
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


