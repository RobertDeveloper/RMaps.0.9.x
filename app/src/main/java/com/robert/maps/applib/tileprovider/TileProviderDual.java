package com.robert.maps.applib.tileprovider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;

public class TileProviderDual extends TileProviderBase {
	TileProviderBase mTileProviderMap;
	TileProviderBase mTileProviderLayer;
	Paint mPaint;

	public TileProviderDual(Context ctx, String aName, TileProviderBase aTileProviderMap, TileProviderBase aTileProviderLayer, MapTileMemCache aTileCache) {
		super(ctx);
		mTileCache = aTileCache;
		mTileProviderMap = aTileProviderMap;
		mTileProviderLayer = aTileProviderLayer;
		mTileURLGenerator = new TileURLGeneratorBase(aName);
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
			try {
				bmpDual = Bitmap.createBitmap(bmpMap.getWidth(), bmpMap.getHeight(), Config.ARGB_8888);
				Canvas canvas = new Canvas(bmpDual);
				canvas.drawBitmap(bmpMap, 0, 0, mPaint);

//				final Rect src = new Rect(0, 0, bmpLayer.getWidth(), bmpLayer.getHeight());
//				final Rect dst = new Rect(0, 0, bmpMap.getWidth(), bmpMap.getHeight());
//				canvas.drawBitmap(bmpLayer, src, dst, mPaint);
				
				if(bmpMap.getWidth() == bmpLayer.getWidth())
					canvas.drawBitmap(bmpLayer, 0, 0, mPaint);
				else {
				    float scaleWidth = ((float) bmpMap.getWidth()) / bmpLayer.getWidth();
				    final Matrix matrix = new Matrix();
				    matrix.postScale(scaleWidth, scaleWidth);
				    final Bitmap resizedBitmap = Bitmap.createBitmap(bmpLayer, 0, 0, bmpLayer.getWidth(), bmpLayer.getWidth(), matrix, false);
				    
				    if(resizedBitmap != null) {
				    	canvas.drawBitmap(resizedBitmap, 0, 0, mPaint);
				    	resizedBitmap.recycle();
				    }
					
				}
				
				mTileProviderMap.removeTile(tileurl);
				mTileProviderLayer.removeTile(tileurl);
				
				mTileCache.putTile(tileurl, bmpDual);
			} catch (OutOfMemoryError e) {
				bmpDual = bmpMap;
			} catch (Exception e) {
				bmpDual = bmpMap;
			}

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
		return super.needIndex(aCashTableName, aSizeFile, aLastModifiedFile, aBlockIndexing);
	}

	@Override
	public void Index() {
		super.Index();
	}

	@Override
	public void removeTileFromCashe(int x, int y, int z) {
		mTileProviderMap.removeTileFromCashe(x, y, z);
		mTileProviderLayer.removeTileFromCashe(x, y, z);
	}

}
