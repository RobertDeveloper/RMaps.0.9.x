package org.andnav.osm.views.util;

import java.util.LinkedHashMap;

import android.graphics.Bitmap;

public class LRUMapTileCache extends LinkedHashMap<String, Bitmap> {

        private static final long serialVersionUID = -541142277575493335L;

        private final int mCapacity;
       
        public LRUMapTileCache(final int pCapacity) {
                super(pCapacity + 2, 0.1f, true);
                mCapacity = pCapacity;
        }

        @Override
        public Bitmap remove(Object pKey) {
                final Bitmap bm = super.remove(pKey);
                if (bm != null) {
                        bm.recycle();
                }
                return bm;
        }

        @Override
        protected boolean removeEldestEntry(Entry<String, Bitmap> pEldest) {
                return size() > mCapacity;
        }

}

