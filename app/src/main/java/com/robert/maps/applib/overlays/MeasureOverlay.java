package com.robert.maps.applib.overlays;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.robert.maps.R;
import com.robert.maps.applib.utils.CoordFormatter;
import com.robert.maps.applib.utils.DistanceFormatter;
import com.robert.maps.applib.view.TileView;
import com.robert.maps.applib.view.TileViewOverlay;

import org.andnav.osm.util.GeoPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class MeasureOverlay extends TileViewOverlay {
	
	private Paint mPaint = new Paint();
	private Paint mPaintText;
	private ArrayList<DistPoint> points = new ArrayList<DistPoint>();
	private Bitmap mCornerMarker = null;
	private float mDistance = 0;
	private TextView mT;
	private LinearLayout msgbox = null;
	private DistanceFormatter mDf;
	private CoordFormatter mCf;
	private DistPoint mLocation;
	private CharSequence mDescr;
	
	private final String LAT;
	private final String LON;
	private final String DIST_START;
	private final String DIST_END;
	private final String DIST_PREV;
	private final String AZI;
	private boolean mShowInfoBubble;
	private boolean mShowLineInfo;
	static private final String DIV = ": ";
	
	public MeasureOverlay(Context ctx, View bottomView) {
		super();
		
		mPaint.setColor(ctx.getResources().getColor(R.color.chart_graph_0));
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(3);
		mPaint.setAntiAlias(true);
		mPaint.setAlpha(180);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setShadowLayer(10.0f, 0, 0, ctx.getResources().getColor(R.color.chart_graph_0));
		
		mPaintText = new Paint();
		mPaintText.setAntiAlias(true);
		mPaintText.setAlpha(10);
		mPaintText.setColor(ctx.getResources().getColor(android.R.color.black));
		mPaintText.setShadowLayer(4.0f, 0, 0, ctx.getResources().getColor(android.R.color.white));
		mPaintText.setTextAlign(Paint.Align.CENTER);
		mPaintText.setTextSize(ctx.getResources().getDimensionPixelSize(R.dimen.measuretool_label_size));
		
		mDf = new DistanceFormatter(ctx);
		mCf = new CoordFormatter(ctx);

		msgbox = (LinearLayout) LayoutInflater.from(ctx).inflate(R.layout.measure_info_box, (ViewGroup) bottomView);
		msgbox.setVisibility(View.VISIBLE);
		
		this.mT = (TextView) LayoutInflater.from(ctx).inflate(R.layout.search_bubble, null);
		this.mT.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		LAT = ctx.getResources().getString(R.string.PoiLat);
		LON = ctx.getResources().getString(R.string.PoiLon);
		DIST_START = ctx.getResources().getString(R.string.tostart);
		DIST_END = ctx.getResources().getString(R.string.toend);
		DIST_PREV = ctx.getResources().getString(R.string.toprev);
		AZI = ctx.getResources().getString(R.string.azimuth);

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		mShowLineInfo = pref.getBoolean("pref_show_measure_line_info", true);
		mShowInfoBubble = pref.getBoolean("pref_show_measure_info", true);

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
			final Path path = new Path();
			Iterator<DistPoint> it = points.iterator();
			while(it.hasNext()) {
				pt = it.next();
				final Point p1 = pj.toPixels(pt.Point, null);
				
				if(p0 != null) {
					c.drawLine(p0.x, p0.y, p1.x, p1.y, mPaint);
					path.reset();
					if(p0.x < p1.x) {
						path.moveTo(p0.x, p0.y);
						path.lineTo(p1.x, p1.y);
					} else {
						path.moveTo(p1.x, p1.y);
						path.lineTo(p0.x, p0.y);
					}
					
					if(mShowLineInfo) {
						c.drawTextOnPath(mDf.formatDistance(pt.Dist2Prev), path, 0, -5, mPaintText);
						c.drawTextOnPath(String.format(Locale.UK, "%.1f°", pt.Bearing), path, 0, mPaintText.getTextSize(), mPaintText);
					}
					
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
		final int bounds = 12;
		Iterator<DistPoint> it = points.iterator();
		while(it.hasNext()) {
			pt = it.next();
			px = pj.toPixels(pt.Point, tileView.getBearing(), px);
			rect.set(px.x - bounds, px.y - bounds, px.x + bounds, px.y + bounds);
			if(rect.contains((int)e.getX(), (int)e.getY())) {
				if(mLocation != null && mLocation.Point.equals(pt.Point))
					mLocation = null;
				else
					mLocation = pt;
				setDescr();
				return true;
			}
		}
		
		addPoint(e.getX(), e.getY(), tileView);

		return true;
	}
	
	private void addPoint(float x, float y, TileView tileView) {
		final com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection pj = tileView.getProjection();

		DistPoint pt = null;
		pt = new DistPoint(pj.fromPixels(x, y, tileView.getBearing()));
		pt.Dist2Prev = points.size() > 0 ? points.get(points.size() - 1).Point.distanceTo(pt.Point) : 0;
		pt.Dist2Start = mDistance + pt.Dist2Prev;
		pt.Bearing = points.size() > 0 ? points.get(points.size() - 1).Point.bearingTo360(pt.Point) : 0;
		if(mShowInfoBubble)
			mLocation = pt;
		
		if(points.size() > 0) {
			mDistance += pt.Dist2Prev;
		}
		
		points.add(pt);
		
		setDescr();
		ShowDistance();
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
	
	private void setDescr() {
		if(mLocation != null)
			mDescr = new StringBuilder()
			.append(LAT).append(DIV).append(mCf.convertLat(mLocation.Point.getLatitude()))
			.append("\n").append(LON).append(DIV).append(mCf.convertLon(mLocation.Point.getLongitude()))
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

	public void setShowInfoBubble(boolean showInfo) {
		mShowInfoBubble = showInfo;
		if(!showInfo)
			mLocation = null;
	}

	public void setShowLineInfo(boolean showInfo) {
		mShowLineInfo = showInfo;
	}
	
	public void addPointOnCenter(TileView tileView) {
		addPoint(tileView.getWidth() / 2, tileView.getHeight() / 2, tileView);
	}

}
