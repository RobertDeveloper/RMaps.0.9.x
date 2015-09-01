package com.robert.maps.applib.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import com.robert.maps.R;

import java.io.File;

public class IconManager {
	private static IconManager mIconManager = null;
	
	public static IconManager getInstance(Context ctx) {
		if(mIconManager == null)
			mIconManager = new IconManager(ctx);
		
		return mIconManager;
	}
	
	private Context mAppContext;

	public IconManager(Context ctx) {
		super();
		mAppContext = ctx.getApplicationContext();
	}
	
	public Bitmap getLocationIcon() {
		final Bitmap bmp = getBitmapFileFromProp("pref_person_icon", "icons/cursors");
		if(bmp != null)
			return bmp;
		else
			return getBitmap(R.drawable.person);
	}
	
	public Bitmap getArrowIcon() {
		final Bitmap bmp = getBitmapFileFromProp("pref_arrow_icon", "icons/cursors");
		if(bmp != null)
			return bmp;
		else
			return getBitmap(R.drawable.arrow);
	}

	public Bitmap getTargetIcon() {
		final Bitmap bmp = getBitmapFileFromProp("pref_target_icon", "icons/cursors");
		if(bmp != null)
			return bmp;
		else
			return getBitmap(R.drawable.r_mark);
	}
	
	
	
	private Bitmap getBitmapFileFromProp(String propName, String folderName) {
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mAppContext);
		final String prefPersonFileName = pref.getString(propName, "");
		
		if(!prefPersonFileName.equalsIgnoreCase("")) {
			final File folder = Ut.getRMapsMainDir(mAppContext, folderName);
			if(folder.exists()) {
				final File file = new File(folder.getAbsolutePath()+"/"+prefPersonFileName);
				if(file.exists()) {
					try {
						final Bitmap bmp = BitmapFactory.decodeFile(folder.getAbsolutePath()+"/"+prefPersonFileName);
						if(bmp != null)
							return bmp;
					} catch (Exception e) {
					} catch (OutOfMemoryError e) {
					}
				}
			}
		}
		
		return null;
	}
	
	private Drawable getDrawable(int resId) {
		return mAppContext.getResources().getDrawable(resId);
	}
	
	private Bitmap getBitmap(int resId) {
		try {
			return BitmapFactory.decodeResource(mAppContext.getResources(), resId);
		} catch (Exception e) {
			return null;
		} catch (OutOfMemoryError e) {
			return null;
		}
	}
}
