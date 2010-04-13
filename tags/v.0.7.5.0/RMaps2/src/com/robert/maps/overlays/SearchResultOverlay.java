package com.robert.maps.overlays;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.TypeConverter;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.view.KeyEvent;

import com.robert.maps.R;
import com.robert.maps.utils.NinePatch;
import com.robert.maps.utils.NinePatchDrawable;

public class SearchResultOverlay extends OpenStreetMapViewOverlay {

	protected final Paint mPaint = new Paint();
	protected GeoPoint mLocation;
	protected NinePatchDrawable mButton;
	protected String mDescr;

	public SearchResultOverlay(final Context ctx) {
		Bitmap mBubbleBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.bubble2);
		byte[] chunk = {21,10,26,17}; // left,top,right,bottom
		this.mButton = new NinePatchDrawable(new NinePatch(mBubbleBitmap, chunk, ""));
		this.mDescr = "";
		this.mPaint.setAntiAlias(true);
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
	protected void onDraw(Canvas c, OpenStreetMapView osmv) {
		if(this.mLocation != null){

			final OpenStreetMapViewProjection pj = osmv.getProjection();
			final Point screenCoords = new Point();
			pj.toPixels(this.mLocation, screenCoords);
			final int DESCRIPTION_MAXWIDTH = 155;
			final int TEXT_SIZE = 12;

			c.save();
			c.rotate(osmv.getBearing(), screenCoords.x, screenCoords.y);

			final float[] widths = new float[mDescr.length()];
			this.mPaint.setTextSize(TEXT_SIZE);
			this.mPaint.getTextWidths(mDescr, widths);

			final StringBuilder sb = new StringBuilder();
			int maxWidth = 0;
			int curLineWidth = 0;
			int lastStop = 0;
			int i;
			int lastwhitespace = 0;
			/* Loop through the charwidth array and harshly insert a linebreak,
			 * when the width gets bigger than DESCRIPTION_MAXWIDTH. */
			for (i = 0; i < widths.length; i++) {
				if(!Character.isLetter(mDescr.charAt(i)) && mDescr.charAt(i) != ',')
					lastwhitespace = i;

				float charwidth = widths[i];

				if(curLineWidth + charwidth> DESCRIPTION_MAXWIDTH){
					if(lastStop == lastwhitespace)
						i--;
					else
						i = lastwhitespace;


					sb.append(mDescr.subSequence(lastStop, i));
					sb.append('\n');

					lastStop = i;
					maxWidth = Math.max(maxWidth, curLineWidth);
					curLineWidth = 0;
				}

				curLineWidth += charwidth;
			}
			/* Add the last line to the rest to the buffer. */
			if(i != lastStop){
				final String rest = mDescr.substring(lastStop, i);

				maxWidth = Math.max(maxWidth, (int)this.mPaint.measureText(rest));

				sb.append(rest);
			}
			final String[] lines = sb.toString().split("\n");
			final int descWidth = Math.min(maxWidth, DESCRIPTION_MAXWIDTH);
			final int descHeight = lines.length * TEXT_SIZE;

			mButton.setBounds(screenCoords.x - 12, screenCoords.y - (descHeight + 23) + 2, screenCoords.x + descWidth + 15 - 12, screenCoords.y + 2);
			mButton.draw(c);

			/* Draw all the lines of the description. */
			for(int j = 0; j < lines.length; j++){
				c.drawText(lines[j].trim(), screenCoords.x - 12 + 8, 7 + TEXT_SIZE + screenCoords.y - (descHeight + 23) + 2 + j * TEXT_SIZE,
						mPaint);
			}
			
			c.restore();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event,
			OpenStreetMapView mapView) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
			if (mLocation != null) {
				mLocation = null;
				mapView.invalidate();
				return true;
			}

		return super.onKeyDown(keyCode, event, mapView);
	}

	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
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
