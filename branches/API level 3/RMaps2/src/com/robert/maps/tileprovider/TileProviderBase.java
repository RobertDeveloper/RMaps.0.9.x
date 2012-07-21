package com.robert.maps.tileprovider;

import java.util.HashSet;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

import com.robert.maps.R;
import com.robert.maps.utils.Ut;

public class TileProviderBase {
	protected Bitmap mLoadingMapTile;
	protected TileURLGeneratorBase mTileURLGenerator;
	protected HashSet<String> mPending = new HashSet<String>();
	protected MapTileMemCache mTileCache;
	protected Handler mCallbackHandler;

	
	public TileProviderBase(Context ctx) {
		super();
		Ut.d("TileProviderBase Created");
		mLoadingMapTile = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.maptile_loading);
	}
	
	public void Free() {
		Ut.d("TileProviderBase Free");
		mPending.clear();
		if(mTileURLGenerator != null)
			mTileURLGenerator.Free();
		if(mLoadingMapTile != null)
			mLoadingMapTile.recycle();
		mCallbackHandler = null;
	}

	protected void finalize() throws Throwable {
		Ut.d("TileProviderBase finalize");
		super.finalize();
	}

	public Bitmap getTile(final int x, final int y, final int z) {
		return mLoadingMapTile;
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

}
