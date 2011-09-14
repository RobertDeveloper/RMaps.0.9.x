package com.robert.maps.kml;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.robert.maps.kml.Track.TrackPoint;

public class ChartView extends View {

	private int zoomLevel = 1;
	private double maxX;
	private int leftBorder = -1;
	private int topBorder = 0;
	private int bottomBorder = 0;

	private int w = 0;
	private int h = 0;
	private int effectiveWidth = 0;
	private int effectiveHeight = 0;

	private Path path;

	private final Paint borderPaint = new Paint();
	private final Paint labelPaint = new Paint();
	private final Paint gridPaint = new Paint();
	private final Paint gridBarPaint = new Paint();
	private final Paint clearPaint = new Paint();

	private static final int TOP_BORDER = 15;
	private static final float BOTTOM_BORDER = 40;
	private static final int RIGHT_BORDER = 17;
	private static final int UNIT_BORDER = 15;
	private static final int FONT_HEIGHT = 10;
	private static final int MAX_INTERVALS = 5;

	public ChartView(Context context, AttributeSet attrs) {
		super(context, attrs);

		labelPaint.setStyle(Style.STROKE);
		labelPaint.setColor(Color.BLACK);
		labelPaint.setAntiAlias(true);

		borderPaint.setStyle(Style.STROKE);
		borderPaint.setColor(Color.BLACK);
		borderPaint.setAntiAlias(true);

		gridPaint.setStyle(Style.STROKE);
		gridPaint.setColor(Color.GRAY);
		gridPaint.setAntiAlias(false);

		gridBarPaint.set(gridPaint);
		gridBarPaint.setPathEffect(new DashPathEffect(new float[] { 3, 2 }, 0));

		clearPaint.setStyle(Style.FILL);
		clearPaint.setColor(Color.WHITE);
		clearPaint.setAntiAlias(false);
		
//		path = new Path();
//		path.moveTo(0, 0);
//		path.lineTo(100, 100);
//		path.lineTo(100, 200);
//		path.lineTo(200, 300);
//		path.close();
        
		
	    
		updateDimensions();
	}

	public ChartView(Context context) {
		super(context);
	}
	
	public void setTrack(final Track tr){
		path = new Path();
		TrackPoint frstpt = null;
		for(TrackPoint pt : tr.getPoints()){
			if(frstpt == null){
				path.moveTo(pt.date.getTime()/1000, (float) pt.speed);
				frstpt = pt;
			} else {
				path.lineTo(pt.date.getTime()/1000, (float) pt.speed);
			}
		}

		final Matrix m = new Matrix();
		m.setTranslate(-frstpt.date.getTime()/1000, (float) -frstpt.speed);
		path.transform(m);
	}

	@Override
	protected void onDraw(Canvas c) {
		updateEffectiveDimensionsIfChanged(c);
		
		c.save();

		c.drawColor(Color.WHITE);
		
		drawXAxis(c);
		drawYAxis(c);
		
//		Path p = new Path();;
//		Matrix m = new Matrix();
//		m.setScale(0.5f, 0.5f);
//		path.transform(m, p);
//		
//		c.drawPath(p, borderPaint);
		
		if(path != null){
			RectF r = new RectF();
			path.computeBounds(r, true);
			final Matrix m = new Matrix();
			m.setScale(effectiveWidth / r.width(), - effectiveHeight / r.height());
			path.transform(m);
			m.setTranslate(RIGHT_BORDER, effectiveHeight + BOTTOM_BORDER);
			path.transform(m);
			c.drawPath(path, borderPaint);
		}

		c.restore();
	}

	private int getX(double distance) {
		return leftBorder
				+ (int) ((distance * effectiveWidth / maxX) * zoomLevel);
	}

	private void updateDimensions() {
		// maxX = xMonitor.getMax();
		// if (data.size() <= 1) {
		// maxX = 1;
		// }
		// for (ChartValueSeries cvs : series) {
		// cvs.updateDimension();
		// }
		// // TODO: This is totally broken. Make sure that we calculate based on
		// measureText for each
		// // grid line, as the labels may vary across intervals.
		// int maxLength = 0;
		// for (ChartValueSeries cvs : series) {
		// if (cvs.isEnabled() && cvs.hasData()) {
		// maxLength += cvs.getMaxLabelLength();
		// }
		// }
		final float density = getContext().getResources().getDisplayMetrics().density;
		// maxLength = Math.max(maxLength, 1);
		final int maxLength = 1;
		leftBorder = (int) (density * (4 + 8 * maxLength));
		bottomBorder = (int) (density * BOTTOM_BORDER);
		topBorder = (int) (density * TOP_BORDER);
		updateEffectiveDimensions();
	}

	private void updateEffectiveDimensions() {
		effectiveWidth = Math.max(0, w - 2 * RIGHT_BORDER);
		effectiveHeight = (int) Math.max(0, h - 2 * BOTTOM_BORDER);

		
		//		effectiveWidth = Math.max(0, w - leftBorder - RIGHT_BORDER);
//		effectiveHeight = Math.max(0, h - topBorder - bottomBorder);
	}

	private void updateEffectiveDimensionsIfChanged(Canvas c) {
		if (w != c.getWidth() || h != c.getHeight()) {
			// Dimensions have changed (for example due to orientation change).
			w = getWidth();
			h = getHeight();
			updateEffectiveDimensions();
			//setUpPath();
		}
	}

	/** Draws the actual X axis line and its label. */
	private void drawXAxis(Canvas canvas) {
		canvas.drawLine(RIGHT_BORDER, effectiveHeight + BOTTOM_BORDER, effectiveWidth + RIGHT_BORDER, effectiveHeight + BOTTOM_BORDER, borderPaint);
		
//		float rightEdge = getX(maxX);
//		final int y = effectiveHeight + topBorder;
		//canvas.drawLine(leftBorder, y, rightEdge, y, borderPaint);
		// Context c = getContext();
		// String s = mode == Mode.BY_DISTANCE
		// ? (metricUnits ? c.getString(R.string.kilometer) :
		// c.getString(R.string.mile))
		// : c.getString(R.string.min);
		// canvas.drawText(s, rightEdge, effectiveHeight + .2f * UNIT_BORDER +
		// topBorder, labelPaint);
	}

	/** Draws the actual Y axis line and its label. */
	private void drawYAxis(Canvas canvas) {
		canvas.drawLine(RIGHT_BORDER, BOTTOM_BORDER, RIGHT_BORDER,
		effectiveHeight + BOTTOM_BORDER, borderPaint);
		
//		canvas.drawRect(0, 0, leftBorder - 1, effectiveHeight + topBorder
//				+ UNIT_BORDER + 1, clearPaint);
//		canvas.drawLine(leftBorder, UNIT_BORDER + topBorder, leftBorder,
//				effectiveHeight + topBorder, borderPaint);
//		for (int i = 1; i < MAX_INTERVALS; ++i) {
//			int y = i * effectiveHeight / MAX_INTERVALS + topBorder;
//			canvas.drawLine(leftBorder - 5, y, leftBorder, y, gridPaint);
//		}

	}
}