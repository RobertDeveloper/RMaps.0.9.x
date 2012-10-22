package com.robert.maps.tileprovider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;

public class TileProviderDual extends TileProviderBase {
	TileProviderBase mTileProviderMap;
	TileProviderBase mTileProviderLayer;
	Paint mPaint;

	public TileProviderDual(Context ctx, TileProviderBase aTileProviderMap, TileProviderBase aTileProviderLayer, MapTileMemCache aTileCache) {
		super(ctx);
		mTileCache = aTileCache;
		mTileProviderMap = aTileProviderMap;
		mTileProviderLayer = aTileProviderLayer;
		mTileURLGenerator = new TileURLGeneratorBase("MAP");
		mPaint = new Paint();
		
		mTileProviderMap.setLoadingMapTile(null);
		mTileProviderLayer.setLoadingMapTile(null);
	}

	@Override
	public void Free() {
		mTileProviderMap.Free();
		mTileProviderLayer.Free();
		super.Free();
	}

	@Override
	public Bitmap getTile(int x, int y, int z) {
		final String tileurl = mTileURLGenerator.Get(x, y, z);
		
		final Bitmap bmp = mTileCache.getMapTile(tileurl);
		if(bmp != null) {
			return bmp;
		}
		
		final Bitmap bmpMap = mTileProviderMap.getTile(x, y, z);
		final Bitmap bmpLayer = mTileProviderLayer.getTile(x, y, z);
		
		Bitmap bmpDual = null;
		if(bmpMap != null && bmpLayer == null) {
			bmpDual = bmpMap;
		} else if(bmpMap == null && bmpLayer != null) {
			bmpDual = bmpLayer;
		} else if(bmpMap != null && bmpLayer != null) {
			bmpDual = Bitmap.createBitmap(bmpMap.getWidth(), bmpMap.getHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(bmpDual);
			canvas.drawBitmap(bmpMap, 0, 0, mPaint);
			canvas.drawBitmap(bmpLayer, 0, 0, mPaint);
			
			mTileProviderMap.removeTile(tileurl);
			mTileProviderLayer.removeTile(tileurl);
			
			mTileCache.putTile(tileurl, bmpDual);

			bmpDual = bmpMap;
		} else {
			bmpDual = super.getTile(x, y, z);
		};

		return bmpDual;
	}

	@Override
	public void updateMapParams(TileSource tileSource) {
		mTileProviderMap.updateMapParams(tileSource);
		mTileProviderLayer.updateMapParams(tileSource);
		
		super.updateMapParams(tileSource);
	}

	@Override
	public void setHandler(Handler mTileMapHandler) {
		mTileProviderMap.setHandler(mTileMapHandler);
		mTileProviderLayer.setHandler(mTileMapHandler);
	}

	@Override
	public void ResizeCashe(int size) {
		super.ResizeCashe(size);
		mTileProviderMap.ResizeCashe(size);
		mTileProviderLayer.ResizeCashe(size);
	}

	@Override
	public boolean needIndex(String aCashTableName, long aSizeFile, long aLastModifiedFile, boolean aBlockIndexing) {
		// TODO Auto-generated method stub
		return super.needIndex(aCashTableName, aSizeFile, aLastModifiedFile, aBlockIndexing);
	}

	@Override
	public void Index() {
		// TODO Auto-generated method stub
		super.Index();
	}

}
