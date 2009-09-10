package com.robert.maps.utils;

import java.io.File;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class CashDatabase {
	private SQLiteDatabase mDatabase;

	public void setFile(final File aFile) {
		if (mDatabase != null)
			mDatabase.close();

		mDatabase = SQLiteDatabase.openOrCreateDatabase(aFile, null);
	}

	public byte[] getTile(final int aX, final int aY, final int aZ) {
		byte[] ret = null;

		if (this.mDatabase != null) {
			final Cursor c = this.mDatabase.rawQuery("SELECT image FROM tiles WHERE s = 0 AND x = " + aX + " AND y = "
					+ aY + " AND z = " + (17 - aZ), null);
			if (c != null) {
				if (c.moveToFirst()) {
					ret = c.getBlob(c.getColumnIndexOrThrow("image"));
				}
				c.close();
			}
		}

		return ret;
	}

	public int getMaxZoom(){
		int ret = 99;
		final Cursor c = this.mDatabase.rawQuery(
				"SELECT 17-MIN(z) AS ret FROM tiles", null);
		if (c != null) {
			if (c.moveToFirst()) {
				ret = c.getInt(c.getColumnIndexOrThrow("ret"));
			}
			c.close();
		}
		return ret;
	}

	public int getMinZoom(){
		int ret = 0;
		final Cursor c = this.mDatabase.rawQuery(
				"SELECT 17-MAX(z) AS ret FROM tiles", null);
		if (c != null) {
			if (c.moveToFirst()) {
				ret = c.getInt(c.getColumnIndexOrThrow("ret"));
			}
			c.close();
		}
		return ret;
	}


}
