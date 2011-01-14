// Created by plusminus on 17:58:57 - 25.09.2008
package org.andnav.osm.views.util;

import java.util.HashMap;
import java.util.HashSet;

import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

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
	
	protected HashMap<String, Bitmap> mCachedTiles;
	protected HashSet<String> mNeedUpdates;

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
		this.mNeedUpdates = new HashSet<String>();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	
	public synchronized Bitmap getMapTile(final String aTileURLString) {
		return this.mCachedTiles.get(aTileURLString);
	}

	public synchronized void putTile(final String aTileURLString, final Bitmap aTile, boolean replaceExisting) {
		if (replaceExisting || !this.mCachedTiles.containsKey(aTileURLString)) {
			this.mCachedTiles.put(aTileURLString, aTile);
			
			if (!replaceExisting)
				this.mNeedUpdates.add(aTileURLString);
			else
				this.mNeedUpdates.remove(aTileURLString);
		}
	}

	public synchronized boolean needsTileUpdate(final String aTileURLString) {
		return this.mNeedUpdates.contains(aTileURLString);
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
