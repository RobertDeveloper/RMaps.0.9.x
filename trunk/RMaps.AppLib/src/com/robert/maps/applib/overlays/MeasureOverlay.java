package com.robert.maps.applib.overlays;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import org.andnav.osm.util.GeoPoint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.robert.maps.applib.R;
import com.robert.maps.applib.utils.CoordFormatter;
import com.robert.maps.applib.utils.DistanceFormatter;
import com.robert.maps.applib.view.TileView;
import com.robert.maps.applib.view.TileViewOverlay;

public class MeasureOverlay extends TileViewOverlay {
	
	private Paint mPaint = new Paint();
	private ArrayList<DistPoint> points = new ArrayList<DistPoint>();
	//private int mPointHolded = -1;
	private Bitmap mCornerMarker = null;
	private float mDistance = 0;
	private TextView mT;
	private LinearLayout msgbox = null;
	private DistanceFormatter mDf;
	private CoordFormatter mCf;
	private DistPoint mLocation;
	private CharSequence mDescr;
	
	public MeasureOverlay(Context ctx, View bottomView) {
		super();
		
		mPaint.setColor(ctx.getResources().getColor(R.color.chart_graph_0));
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(3);
		mPaint.setAntiAlias(true);
		mPaint.setAlpha(180);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setShadowLayer(10.0f, 0, 0, ctx.getResources().getColor(R.color.chart_graph_0));
		
		mDf = new DistanceFormatter(ctx);
		mCf = new CoordFormatter(ctx);

		msgbox = (LinearLayout) LayoutInflater.from(ctx).inflate(R.layout.measure_info_box, (ViewGroup) bottomView);
		msgbox.setVisibility(View.VISIBLE);
		this.mT = (TextView) LayoutInflater.from(ctx).inflate(R.layout.search_bubble, null);
		this.mT.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		ShowDistance();
	}

	private Bitmap getPic(TileView tileView) {
		if(mCornerMarker == null)
			mCornerMarker = BitmapFactory.decodeResource(tileView.getContext().getResources(), R.drawable.r_mark);
		
		return mCornerMarker;
	}
	
	private void ShowDistance() {
		((TextView) msgbox.findViewById(R.id.value)).setText(mDf.formatDistance(mDistance));
	}
	
	@Override
	protected void onDraw(Canvas c, TileView tileView) {
		final com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection pj = tileView.getProjection();
		final Bitmap pic = getPic(tileView);

		if(points.size() > 0) {
			Point p0 = null;
			DistPoint pt = null;
			Iterator<DistPoint> it = points.iterator();
			while(it.hasNext()) {
				pt = it.next();
				final Point p1 = pj.toPixels(pt.Point, null);
				
				if(p0 != null) {
					c.drawLine(p0.x, p0.y, p1.x, p1.y, mPaint);
					c.drawBitmap(pic, p0.x - (int)(pic.getWidth()/2), p0.y - (int)(pic.getHeight() / 2), mPaint);
				}
				
				p0 = p1;
			}
			
			c.drawBitmap(pic, p0.x - (int)(pic.getWidth()/2), p0.y - (int)(pic.getHeight() / 2), mPaint);
			
		}
		
		if(this.mLocation != null){
			mT.setText(mDescr);
			mT.measure(0, 0);
			mT.layout(0, 0, mT.getMeasuredWidth(), mT.getMeasuredHeight());

			final Point screenCoords = new Point();
			pj.toPixels(this.mLocation.Point, screenCoords);

			c.save();
			c.rotate(tileView.getBearing(), screenCoords.x, screenCoords.y);
			c.translate(screenCoords.x - mT.getMeasuredWidth() / 2, screenCoords.y - mT.getMeasuredHeight() + 2);
			mT.draw(c);
			
			c.restore();
			
			c.drawBitmap(pic, screenCoords.x - (int)(pic.getWidth()/2), screenCoords.y - (int)(pic.getHeight() / 2), mPaint);
		}

	}
	
	@Override
	protected void onDrawFinished(Canvas c, TileView tileView) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e, TileView tileView) {
		final com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection pj = tileView.getProjection();
		
		DistPoint pt = null;
		Point px = new Point();
		Rect rect = new Rect();
		final int bounds = 8;
		Iterator<DistPoint> it = points.iterator();
		while(it.hasNext()) {
			pt = it.next();
			px = pj.toPixels(pt.Point, tileView.getBearing(), px);
			rect.set(px.x - bounds, px.y - bounds, px.x + bounds, px.y + bounds);
			if(rect.contains((int)e.getX(), (int)e.getY())) {
				mLocation = pt;
				setDescr();
				return true;
			}
		}
		
		pt = new DistPoint(pj.fromPixels(e.getX(), e.getY(), tileView.getBearing()));
		pt.Dist2Prev = points.size() > 0 ? points.get(points.size() - 1).Point.distanceTo(pt.Point) : 0;
		pt.Dist2Start = mDistance + pt.Dist2Prev;
		pt.Bearing = points.size() > 0 ? points.get(points.size() - 1).Point.bearingTo(pt.Point) : 0;
		mLocation = pt;
		
		if(points.size() > 0) {
			mDistance += pt.Dist2Prev;
		}
		
		points.add(pt);
		
		setDescr();
		ShowDistance();

		return true;
	}
	
	private class DistPoint {
		double Dist2Start;
		double Dist2Prev;
		double Bearing;
		GeoPoint Point;
		
		DistPoint(GeoPoint point) {
			Point = point;
		}
	}
	
	static private final String DIST_START = "to Start";
	static private final String DIST_END = "to End";
	static private final String DIST_PREV = "to Previous";
	static private final String AZI = "Azimut";
	static private final String DIV = ": ";
	
	private void setDescr() {
		if(mLocation != null)
			mDescr = new StringBuilder()
			.append(mCf.convertLat(mLocation.Point.getLatitude()))
			.append("\n")
			.append(mCf.convertLon(mLocation.Point.getLongitude()))
			.append("\n").append(DIST_PREV).append(DIV).append(mDf.formatDistance(mLocation.Dist2Prev))
			.append("\n").append(DIST_START).append(DIV).append(mDf.formatDistance(mLocation.Dist2Start))
			.append("\n").append(DIST_END).append(DIV).append(mDf.formatDistance(mDistance - mLocation.Dist2Start))
			.append("\n").append(AZI).append(DIV).append(String.format(Locale.UK, "%.1f°", mLocation.Bearing))
			.toString();				
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
		points.clear();
		mDistance = 0;
		mLocation = null;
		ShowDistance();
	}

	public void Undo() {
		if(points.size() > 2) {
			mDistance -= points.get(points.size() - 1).Point.distanceTo(points.get(points.size() - 2).Point);
			points.remove(points.size() - 1);
		} else if(points.size() > 0) {
			mDistance = 0;
			points.remove(points.size() - 1);
		}
		if(points.size() > 0) {
			mLocation = points.get(points.size() - 1);
			setDescr();
		} else {
			mLocation = null;
		}
		ShowDistance();
	}

}
