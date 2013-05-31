package com.robert.maps.applib.overlays;

import java.util.ArrayList;
import java.util.Iterator;

import org.andnav.osm.util.GeoPoint;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.robert.maps.applib.R;
import com.robert.maps.applib.utils.Ut;
import com.robert.maps.applib.view.TileView;
import com.robert.maps.applib.view.TileViewOverlay;

public class MeasureOverlay extends TileViewOverlay {
	
	private Paint mPaint = new Paint();
	private ArrayList<GeoPoint> point = new ArrayList<GeoPoint>();
	//private int mPointHolded = -1;
	private Bitmap mCornerMarker = null;
	private float mDistance = 0;
	private Context mCtx;
	private int mUnits;
	private LinearLayout msgbox = null;
	
	public MeasureOverlay(Context ctx, View bottomView) {
		super();
		
		mCtx = ctx;
		mPaint.setColor(ctx.getResources().getColor(R.color.chart_graph_0));
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(3);
		mPaint.setAntiAlias(true);
		mPaint.setAlpha(180);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setShadowLayer(10.0f, 0, 0, ctx.getResources().getColor(R.color.chart_graph_0));

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		mUnits = Integer.parseInt(pref.getString("pref_units", "0"));
		
		msgbox = (LinearLayout) LayoutInflater.from(mCtx).inflate(R.layout.measure_info_box, (ViewGroup) bottomView);
		msgbox.setVisibility(View.VISIBLE);
		ShowDistance();
	}

	private Bitmap getPic(TileView tileView) {
		if(mCornerMarker == null)
			mCornerMarker = BitmapFactory.decodeResource(tileView.getContext().getResources(), R.drawable.r_mark);
		
		return mCornerMarker;
	}
	
	private void ShowDistance() {
		final String lbl = Ut.formatDistance(mCtx, mDistance, mUnits);
		((TextView) msgbox.findViewById(R.id.value)).setText(lbl);
	}
	
	@Override
	protected void onDraw(Canvas c, TileView tileView) {
		if(point.size() > 0) {
			final com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection pj = tileView.getProjection();
			
			final Bitmap pic = getPic(tileView);
			Point p0 = null;
			GeoPoint g = null;
			Iterator<GeoPoint> it = point.iterator();
			while(it.hasNext()) {
				g = it.next();
				final Point p1 = pj.toPixels(g, null);
				
				if(p0 != null) {
					c.drawLine(p0.x, p0.y, p1.x, p1.y, mPaint);
					c.drawBitmap(pic, p0.x - (int)(pic.getWidth()/2), p0.y - (int)(pic.getHeight() / 2), mPaint);
				}
				
				p0 = p1;
			}
			
			c.drawBitmap(pic, p0.x - (int)(pic.getWidth()/2), p0.y - (int)(pic.getHeight() / 2), mPaint);
			
		}
		
	}
	
	@Override
	protected void onDrawFinished(Canvas c, TileView tileView) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e, TileView tileView) {
		final com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection pj = tileView.getProjection();
		final GeoPoint g = pj.fromPixels(e.getX(), e.getY());
		
		if(point.size() > 0) {
			mDistance += point.get(point.size() - 1).distanceTo(g);
		}
		
		point.add(g);
		
		ShowDistance();

		return true;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event, TileView mapView) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Undo();
			
			mapView.invalidate();
			return true;
			}

		return super.onKeyDown(keyCode, event, mapView);
	}

	public void Clear() {
		point.clear();
		mDistance = 0;
		ShowDistance();
	}

	public void Undo() {
		if(point.size() > 2) {
			mDistance -= point.get(point.size() - 1).distanceTo(point.get(point.size() - 2));
			point.remove(point.size() - 1);
		} else if(point.size() > 0) {
			mDistance = 0;
			point.remove(point.size() - 1);
		}
		ShowDistance();
	}

}
