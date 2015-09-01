package com.robert.maps.applib.tileprovider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

import com.robert.maps.R;

import java.util.HashSet;

public class TileProviderBase {
	protected Bitmap mLoadingMapTile;
	protected TileURLGeneratorBase mTileURLGenerator;
	protected HashSet<String> mPending = new HashSet<String>();
	protected MapTileMemCache mTileCache;
	protected Handler mCallbackHandler;
	protected boolean mReloadTileMode = false;

	
	public TileProviderBase(Context ctx) {
		super();
		mLoadingMapTile = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.maptile_loading);
	}
	
	public void Free() {
		mPending.clear();
		if(mTileURLGenerator != null)
			mTileURLGenerator.Free();
		if(mLoadingMapTile != null)
			mLoadingMapTile.recycle();
		mCallbackHandler = null;
		if(mTileCache != null)
			mTileCache.Free();
	}

	protected void finalize() throws Throwable {
		super.finalize();
	}

	public Bitmap getTile(final int x, final int y, final int z) {
		return mLoadingMapTile;
	}
	
	public void removeTile(final String aTileURLString) {
		if(mTileCache != null)
			mTileCache.removeTile(aTileURLString);
	}
	
	public void removeTileFromCashe(final int x, final int y, final int z) {
	}
	
	protected void SendMessageSuccess() {
		if(mCallbackHandler != null)
			Message.obtain(mCallbackHandler, MessageHandlerConstants.MAPTILEFSLOADER_SUCCESS_ID).sendToTarget();
	}
	
	protected void SendMessageFail() {
		if(mCallbackHandler != null)
			Message.obtain(mCallbackHandler, MessageHandlerConstants.MAPTILEFSLOADER_FAIL_ID).sendToTarget();
	}

	public void setHandler(Handler mTileMapHandler) {
		mCallbackHandler = mTileMapHandler;
	}
	
	public void ResizeCashe(final int size) {
		if(mTileCache != null)
			mTileCache.Resize(size);
	}
	
	public void CommitCashe() {
		if(mTileCache != null)
			mTileCache.Commit();
	}

	public void updateMapParams(TileSource tileSource) {
	}
	
	public boolean needIndex(final String aCashTableName, final long aSizeFile, final long aLastModifiedFile, final boolean aBlockIndexing) {
		return false;
	}
	
	public void Index() {
		
	}
	
	public void setLoadingMapTile(Bitmap aLoadingMapTile) {
		if(mLoadingMapTile != null)
			mLoadingMapTile.recycle();
		mLoadingMapTile = aLoadingMapTile;
	}
	
	public double getTileLength() {
		return 0;
	}

}
