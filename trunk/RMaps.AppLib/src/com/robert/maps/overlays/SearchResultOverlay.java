package com.robert.maps.overlays;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.TypeConverter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.robert.maps.applib.R;
import com.robert.maps.view.TileView;
import com.robert.maps.view.TileViewOverlay;

public class SearchResultOverlay extends TileViewOverlay {

	protected final Paint mPaint = new Paint();
	protected GeoPoint mLocation;
	protected String mDescr;
	private TextView mT;

	public SearchResultOverlay(final Context ctx) {
		this.mDescr = "";
		this.mPaint.setAntiAlias(true);
		this.mT = (TextView) LayoutInflater.from(ctx).inflate(R.layout.search_bubble, null); //new Button(ctx);
		this.mT.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

	public void setLocation(final Location loc){
		this.mLocation = TypeConverter.locationToGeoPoint(loc);
	}

	public void setLocation(final GeoPoint geopoint, final String aDescr){
		this.mLocation = geopoint;
		this.mDescr = aDescr;
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

			final com.robert.maps.view.TileView.OpenStreetMapViewProjection pj = osmv.getProjection();
			final Point screenCoords = new Point();
			pj.toPixels(this.mLocation, screenCoords);

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
			mLocation = null;
			mapView.invalidate();
			return true;
		}

		return super.onSingleTapUp(e, mapView);
	}

	@Override
	protected void onDrawFinished(Canvas c, TileView osmv) {
		// Auto-generated method stub

	}

	public void fromPref(SharedPreferences settings) {
		final String strlocation = settings.getString("SearchResultLocation", "");
		if(strlocation.length() > 0){
			mLocation = GeoPoint.fromDoubleString(strlocation);
			mDescr = settings.getString("SearchResultDescr", "");
		}
	}
	
	public void toPref(SharedPreferences.Editor editor){
		if(mLocation != null){
			editor.putString("SearchResultDescr", mDescr);
			editor.putString("SearchResultLocation", mLocation.toDoubleString());
		}else{
			editor.putString("SearchResultDescr", "");
			editor.putString("SearchResultLocation", "");
		}
	}

}
