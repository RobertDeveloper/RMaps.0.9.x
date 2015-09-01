package com.robert.maps.applib.tileprovider;

import android.graphics.Bitmap;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class MapTileMemCache {
	private static final int CACHE_MAPTILECOUNT_DEFAULT = 5;
	private int mSize;

	protected LinkedHashMap<String, Bitmap> mHardCachedTiles;

	public MapTileMemCache(){
		this(CACHE_MAPTILECOUNT_DEFAULT);
	}

	public MapTileMemCache(final int aMaximumCacheSize){
		this.mHardCachedTiles = new LinkedHashMap<String, Bitmap>(aMaximumCacheSize, 0.75f, true);
		mSize = aMaximumCacheSize;
	}

	public synchronized Bitmap getMapTile(final String aTileURLString) {
		if(aTileURLString != null) {
			final Bitmap bmpHard = this.mHardCachedTiles.get(aTileURLString);
			if(bmpHard != null){
				if(!bmpHard.isRecycled()) {
					return bmpHard;
				}
			}
		}
		return null;
	}

	public synchronized void putTile(final String aTileURLString, final Bitmap aTile) {
		this.mHardCachedTiles.put(aTileURLString, aTile);
		
		if(mHardCachedTiles.size() > mSize) {
			Iterator<String> it = mHardCachedTiles.keySet().iterator();
			if(it.hasNext()) {
				final String key = it.next();
				final Bitmap bmpHard = this.mHardCachedTiles.get(key);
				if(bmpHard != null){
					if(!bmpHard.isRecycled()) {
						bmpHard.recycle();
					}
				}
				mHardCachedTiles.remove(key);
			}
		}
	}
	
	public void removeTile(final String aTileURLString) {
		if(mHardCachedTiles.containsKey(aTileURLString)) {
			final Bitmap bmpHard = this.mHardCachedTiles.get(aTileURLString);
			if(bmpHard != null){
				if(!bmpHard.isRecycled()) {
					bmpHard.recycle();
				}
			}
			mHardCachedTiles.remove(mHardCachedTiles);
		}
	}

	public synchronized void Commit() {
	}
	
	public synchronized void Resize(final int size) {
		if(size > mSize){
			mSize = size;
			final LinkedHashMap<String, Bitmap> hardCache = new LinkedHashMap<String, Bitmap>(size, 0.75f, true);
			hardCache.putAll(mHardCachedTiles);
			mHardCachedTiles = hardCache;
		}
	}

	public void Free() {
		try {
			Iterator<Entry<String, Bitmap>> it = mHardCachedTiles.entrySet().iterator();
			while(it.hasNext()) {
				final Bitmap bmpHard = it.next().getValue();
				if(bmpHard != null){
					if(!bmpHard.isRecycled()) {
						bmpHard.recycle();
					}
				}
			}
		} catch (ConcurrentModificationException e) {
			// TODO It's need other iteration code
		}
		mHardCachedTiles.clear();
		mSize = 0;
	}
}
