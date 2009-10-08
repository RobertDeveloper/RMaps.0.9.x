package com.robert.maps.overlays;

import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.OpenStreetMapViewItemizedOverlay;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlayItem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;

import com.robert.maps.R;
import com.robert.maps.kml.PoiManager;

public class PoiOverlay extends OpenStreetMapViewItemizedOverlay<OpenStreetMapViewOverlayItem> {
	private PoiManager mPoiManager;

	public PoiOverlay(Context ctx, PoiManager poiManager,
			OnItemTapListener<OpenStreetMapViewOverlayItem> onItemTapListener) {
		super(ctx, poiManager.getPoiList(), ctx.getResources().getDrawable(R.drawable.poi), new Point(0, 38), onItemTapListener);
		mPoiManager = poiManager;
	}

	@Override
	public void onDraw(Canvas c, OpenStreetMapView mapView) {
		super.mItemList = mPoiManager.getPoiList();
		super.onDraw(c, mapView);
	}


}
