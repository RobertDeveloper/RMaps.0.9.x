package org.andnav.osm.views.util;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;

import android.graphics.Bitmap;

public class LRUMapTileCache extends LinkedHashMap<String, SoftReference<Bitmap>> {

        private static final long serialVersionUID = -541142277575493335L;

        private final int mCapacity;

        public LRUMapTileCache(final int pCapacity) {
                super(pCapacity + 2, 0.1f, true);
                mCapacity = pCapacity;
        }

        @Override
        public SoftReference<Bitmap> remove(Object pKey) {
        		final SoftReference<Bitmap> ref = super.remove(pKey);
        		if(ref != null){
	                final Bitmap bm = ref.get();
	                if (bm != null) {
	                        bm.recycle();
	                }
        		}
                return ref;
        }

        @Override
        protected boolean removeEldestEntry(Entry<String, SoftReference<Bitmap>> pEldest) {
                return size() > mCapacity;
        }

}

