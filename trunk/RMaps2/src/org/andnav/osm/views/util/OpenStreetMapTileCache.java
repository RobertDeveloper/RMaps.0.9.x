// Created by plusminus on 17:58:57 - 25.09.2008
package org.andnav.osm.views.util;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import com.robert.maps.utils.Ut;

import android.graphics.Bitmap;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class OpenStreetMapTileCache implements OpenStreetMapViewConstants{
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected HashMap<String, SoftReference<Bitmap>> mCachedTiles;
	protected LinkedHashMap<String, Bitmap> mHardCachedTiles;
	protected LinkedHashMap<String, Bitmap> mHardCachedTiles2;

	// ===========================================================
	// Constructors
	// ===========================================================

	public OpenStreetMapTileCache(){
		this(CACHE_MAPTILECOUNT_DEFAULT);
	}

	/**
	 * @param aMaximumCacheSize Maximum amount of MapTiles to be hold within.
	 */
	public OpenStreetMapTileCache(final int aMaximumCacheSize){
		this.mCachedTiles = new LRUMapTileCache(aMaximumCacheSize);
		this.mHardCachedTiles = new LinkedHashMap<String, Bitmap>(aMaximumCacheSize);
		this.mHardCachedTiles2 = new LinkedHashMap<String, Bitmap>(aMaximumCacheSize);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public synchronized Bitmap getMapTile(final String aTileURLString) {
		final Bitmap bmpHard = this.mHardCachedTiles.get(aTileURLString);
		if(bmpHard != null){
			if(!bmpHard.isRecycled()) {
				this.mHardCachedTiles2.put(aTileURLString, bmpHard);
				return bmpHard;
			}
		}
		final SoftReference<Bitmap> ref = this.mCachedTiles.get(aTileURLString);
		if(ref == null)
			return null;
		final Bitmap bmp = ref.get();
		if(bmp == null){
			Ut.w("EMPTY SoftReference");
			this.mCachedTiles.remove(ref);
		} else if(bmp.isRecycled()){
			this.mCachedTiles.remove(ref);
		}
		this.mHardCachedTiles2.put(aTileURLString, bmp);
		return bmp;
	}

	public synchronized void putTile(final String aTileURLString, final Bitmap aTile) {
		this.mCachedTiles.put(aTileURLString, new SoftReference<Bitmap>(aTile));
		this.mHardCachedTiles2.put(aTileURLString, aTile);
		Ut.w("OpenStreetMapTileCache size = "+this.mCachedTiles.size());
	}

	public synchronized void Commit() {
		final LinkedHashMap<String, Bitmap> tmp = this.mHardCachedTiles;
		this.mHardCachedTiles = this.mHardCachedTiles2;
		this.mHardCachedTiles2 = tmp;
		this.mHardCachedTiles2.clear();
		Ut.w("mHardCachedTiles size = "+this.mHardCachedTiles.size());
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
