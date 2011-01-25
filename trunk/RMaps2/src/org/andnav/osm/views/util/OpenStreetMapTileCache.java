// Created by plusminus on 17:58:57 - 25.09.2008
package org.andnav.osm.views.util;

import java.lang.ref.SoftReference;
import java.util.HashMap;

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
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public synchronized Bitmap getMapTile(final String aTileURLString) {
		final SoftReference<Bitmap> ref = this.mCachedTiles.get(aTileURLString);
		if(ref == null)
			return null;
		final Bitmap bmp = ref.get();
		if(bmp == null){
			Ut.w("EMPTY SoftReference");
			this.mCachedTiles.remove(ref);
		}
		return bmp;
	}

	public synchronized void putTile(final String aTileURLString, final Bitmap aTile) {
		this.mCachedTiles.put(aTileURLString, new SoftReference<Bitmap>(aTile));
		Ut.w("OpenStreetMapTileCache size = "+this.mCachedTiles.size());
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
