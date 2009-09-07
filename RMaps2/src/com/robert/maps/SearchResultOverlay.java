package com.robert.maps;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.TypeConverter;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;

import com.robert.maps.utils.NinePatch;
import com.robert.maps.utils.NinePatchDrawable;
import com.robert.maps.utils.Ut;

public class SearchResultOverlay extends OpenStreetMapViewOverlay {

	protected final Paint mPaint = new Paint();
	protected final Bitmap mBubbleBitmap;
	protected GeoPoint mLocation;
	protected NinePatchDrawable mButton;

	public SearchResultOverlay(final Context ctx) {
		this.mBubbleBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.bubble2);
		byte[] chunk = {20,10,30,20};
		NinePatch nine = new NinePatch(mBubbleBitmap, chunk, "");
		this.mButton = new NinePatchDrawable(nine);
	}

	public void setLocation(final Location loc){
		this.mLocation = TypeConverter.locationToGeoPoint(loc);
	}

	public void setLocation(final GeoPoint geopoint){
		this.mLocation = geopoint;
	}

	@Override
	protected void onDraw(Canvas c, OpenStreetMapView osmv) {
		if(this.mLocation != null){
			final OpenStreetMapViewProjection pj = osmv.getProjection();
			final Point screenCoords = new Point();
			pj.toPixels(this.mLocation, screenCoords);

			mButton.setBounds(screenCoords.x - 12, screenCoords.y - 80 + 3, screenCoords.x + 80 - 12, screenCoords.y + 3);
			mButton.draw(c);
			
			Paint p = new Paint();
			p.setAntiAlias(true);
			Rect r = new Rect();
			String str = new String("HELLO HELLO\nHELLO HELLO HELLO HELLO");
			p.getTextBounds(str, 0, str.length(), r);
			Ut.dd("r :: "+r.left+"-"+r.right+" : "+r.top+"-"+r.bottom);
		}
	}

	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
		// Auto-generated method stub

	}

}
