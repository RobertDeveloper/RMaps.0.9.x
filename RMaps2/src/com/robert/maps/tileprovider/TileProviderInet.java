package com.robert.maps.tileprovider;

import org.andnav.osm.views.util.OpenStreetMapTileCache;

import android.content.Context;
import android.graphics.Bitmap;

public class TileProviderInet extends TileProviderBase {

	public TileProviderInet(Context ctx, TileURLGeneratorBase gen) {
		super(ctx);
		mTileURLGenerator = gen;
		mTileCache = new OpenStreetMapTileCache(20);
	}

	@Override
	public Bitmap getTile(int x, int y, int z) {
//		final String tileurl = mTileURLGenerator.Get(x, y, z);
//		
//		final Bitmap bmp = mTileCache.getMapTile(tileurl);
//		if(bmp != null)
//			return bmp;
//		
//		if (this.mPending.contains(tileurl))
//			return super.getTile(x, y, z);
//		
//		mPending.add(tileurl);
		
		
		
		return mLoadingMapTile;
	}

}
