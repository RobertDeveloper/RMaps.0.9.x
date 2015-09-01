package com.robert.maps.applib.utils;

import android.content.Context;

import com.robert.maps.R;

public class RException extends Exception {

	private static final long serialVersionUID = -8636414892868856061L;
	private int mStringResID;
	
    public RException() {}

    public RException(String error) {
        super(error);
    }
    
    public RException(final int aStringResID, String error) {
        super(error);
        mStringResID = aStringResID == 0 ? R.string.error_other : aStringResID;
    }
    
    public String getStringRes(Context context) {
    	return context.getResources().getString(mStringResID, getMessage());
    }
    
    public int getID() {
    	return mStringResID;
    }
}
