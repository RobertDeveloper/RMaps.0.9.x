package com.robert.maps.applib.overlays;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.TypeConverter;
import org.andnav.osm.views.util.StreamUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.robert.maps.applib.R;
import com.robert.maps.applib.utils.CoordFormatter;
import com.robert.maps.applib.utils.DistanceFormatter;
import com.robert.maps.applib.utils.SimpleThreadFactory;
import com.robert.maps.applib.view.MapView;
import com.robert.maps.applib.view.TileView;
import com.robert.maps.applib.view.TileViewOverlay;

public class SearchResultOverlay extends TileViewOverlay {

	protected final Paint mPaint = new Paint();
	protected final Paint mPaintLine;
	protected GeoPoint mLocation;
	protected GeoPoint mCurrLocation;
	protected String mDescr;
	private TextView mT;
	private CoordFormatter mCf;
	private DistanceFormatter mDf;
	//private RequestQueue mRequestQueue;
	private double mElevation;
	private MapView mMapView;
	private boolean mSearchBubble;
	
	private ExecutorService mThreadPool = Executors.newFixedThreadPool(2, new SimpleThreadFactory("SearchResultOverlay"));
	
	private final String LAT;
	private final String LON;
	private final String ALT;
	private final String DIST;
	private final String AZIMUT;

	public SearchResultOverlay(final Context ctx, MapView mapView) {
		this.mDescr = "";
		this.mPaint.setAntiAlias(true);
		this.mT = (TextView) LayoutInflater.from(ctx).inflate(R.layout.search_bubble, null); //new Button(ctx);
		this.mT.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		mCf = new CoordFormatter(ctx);
		mDf = new DistanceFormatter(ctx);
		//mRequestQueue = Volley.newRequestQueue(ctx);
		mMapView = mapView;
	
		mPaintLine = new Paint();
		mPaintLine.setAntiAlias(true);
		mPaintLine.setStrokeWidth(2);
		mPaintLine.setStyle(Paint.Style.STROKE);
		mPaintLine.setColor(ctx.getResources().getColor(R.color.line_to_gps));
		
		LAT = ctx.getResources().getString(R.string.PoiLat);
		LON = ctx.getResources().getString(R.string.PoiLon);
		ALT = ctx.getResources().getString(R.string.PoiAlt);
		DIST = ctx.getResources().getString(R.string.dist);
		AZIMUT = ctx.getResources().getString(R.string.azimuth);
	}

	@Override
	public void Free() {
		mThreadPool.shutdown();
		super.Free();
	}

	public void setLocation(final Location loc){
		// Используется для сохранения текущего положения 
		this.mCurrLocation = TypeConverter.locationToGeoPoint(loc);
		if(!mSearchBubble)
			setDescr();
	}

	public void setLocation(final GeoPoint geopoint, final String aDescr){
		// Используется при поиске
		this.mLocation = geopoint;
		this.mDescr = aDescr;
		mSearchBubble = true;
	}

	public void Clear(){
		this.mLocation = null;
		this.mDescr = "";
	}

	@Override
	protected void onDraw(Canvas c, TileView osmv) {
		if(this.mLocation != null){
			mT.setText(mDescr);
			mT.measure(0, 0);
			mT.layout(0, 0, mT.getMeasuredWidth(), mT.getMeasuredHeight());

			final com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection pj = osmv.getProjection();
			final Point screenCoords = new Point();
			pj.toPixels(this.mLocation, screenCoords);

			if(!mSearchBubble && mCurrLocation != null) {
				final Point currLocCoords = new Point();
				pj.toPixels(this.mCurrLocation, currLocCoords);
				c.drawLine(currLocCoords.x, currLocCoords.y, screenCoords.x, screenCoords.y, mPaintLine);
			}

			c.save();
			c.rotate(osmv.getBearing(), screenCoords.x, screenCoords.y);
			c.translate(screenCoords.x - mT.getMeasuredWidth() / 2, screenCoords.y - mT.getMeasuredHeight() + 2);
			mT.draw(c);
			
			c.restore();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event,
			TileView mapView) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
			if (mLocation != null) {
				mLocation = null;
				mapView.invalidate();
				return true;
			}

		return super.onKeyDown(keyCode, event, mapView);
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e, TileView mapView) {
		if (mLocation != null) {
			final com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection pj = mapView.getProjection();
			final Point screenCoords = new Point();
			pj.toPixels(this.mLocation, mapView.getBearing(), screenCoords);
			final Rect rect = new Rect(screenCoords.x - mT.getMeasuredWidth() / 2, screenCoords.y - mT.getMeasuredHeight() + 5, screenCoords.x + mT.getMeasuredWidth() / 2, screenCoords.y - 20);
			
			if(rect.contains((int)e.getX(), (int)e.getY())) {
				mapView.mPoiMenuInfo.EventGeoPoint = mLocation;
				mapView.mPoiMenuInfo.MarkerIndex = PoiOverlay.NO_TAP;
				mapView.mPoiMenuInfo.Elevation = mElevation;
				mapView.showContextMenu();
			}
			
			mLocation = null;
			mapView.invalidate();
			return true;
		}

		return super.onSingleTapUp(e, mapView);
	}

	@Override
	public int onLongPress(MotionEvent event, TileView mapView) {
		mLocation = mapView.getProjection().fromPixels((int)event.getX(), (int)event.getY(), mapView.getBearing());
		mElevation = 0.0;
		mSearchBubble = false;
		
		mThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				InputStream in = null;
				OutputStream out = null;
				
				try {
					in = new BufferedInputStream(new URL("http://maps.googleapis.com/maps/api/elevation/json?locations="+mLocation.toDoubleString()+"&sensor=true").openStream(), StreamUtils.IO_BUFFER_SIZE);
					
					final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
					out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
					StreamUtils.copy(in, out);
					out.flush();
					
					try {
						final JSONObject json = new JSONObject(dataStream.toString());
						mElevation = json.getJSONArray("results").getJSONObject(0).getDouble("elevation");
					} catch (JSONException e) {
						mElevation = 0.0;
					}
					setDescr();
					mMapView.postInvalidate();
					
				} catch (Exception e) {
				} catch (OutOfMemoryError e) {
					System.gc();
				} finally {
					StreamUtils.closeStream(in);
					StreamUtils.closeStream(out);
				}
			}
		});
		
		setDescr();
		mMapView.invalidate(); //postInvalidate();
		return 2;
	}
	
	private void setDescr() {
		if(mLocation != null)
			mDescr = new StringBuilder()
			.append(LAT).append(": ")
			.append(mCf.convertLat(mLocation.getLatitude()))
			.append("\n").append(LON).append(": ")
			.append(mCf.convertLon(mLocation.getLongitude()))
			.append("\n").append(ALT).append(": ")
			.append(mElevation == 0.0 ? "n/a" : mDf.formatElevation(mElevation))
			.append(mCurrLocation == null ? "" : String.format(Locale.UK, "\n%s: %.1f°", AZIMUT, mCurrLocation.bearingTo(mLocation))+"\n"+DIST+": "+mDf.formatDistance(mCurrLocation.distanceTo(mLocation)))
			.toString();				
	}

	@Override
	protected void onDrawFinished(Canvas c, TileView osmv) {

	}

	public void fromPref(SharedPreferences settings) {
		final String strlocation = settings.getString("SearchResultLocation", "");
		if(strlocation.length() > 0){
			mLocation = GeoPoint.fromDoubleString(strlocation);
			mDescr = settings.getString("SearchResultDescr", "");
		}
	}
	
	public void toPref(SharedPreferences.Editor editor){
		if(mLocation != null && mSearchBubble){
			editor.putString("SearchResultDescr", mDescr);
			editor.putString("SearchResultLocation", mLocation.toDoubleString());
		}else{
			editor.putString("SearchResultDescr", "");
			editor.putString("SearchResultLocation", "");
		}
	}

}
