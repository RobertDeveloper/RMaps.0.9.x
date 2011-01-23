package com.robert.maps.utils;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class CashDatabase {
	private SQLiteDatabase mDatabase;

	public void setFile(final File aFile) throws SQLiteException {
		if (mDatabase != null)
			mDatabase.close();

		mDatabase = new CashDatabaseHelper(null, aFile.getAbsolutePath()).getWritableDatabase();
		Ut.d("CashDatabase: Open SQLITEDB Database");

	}

	protected class CashDatabaseHelper extends RSQLiteOpenHelper {
		public CashDatabaseHelper(final Context context, final String name) {
			super(context, name, null, 3);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}

	}

	public void updateMinMaxZoom() throws SQLiteException {
		if(mDatabase != null){
			Ut.dd("Update min max");
			this.mDatabase.execSQL("DROP TABLE IF EXISTS info");
			this.mDatabase.execSQL("CREATE TABLE IF NOT EXISTS info AS SELECT MIN(z) AS minzoom, MAX(z) AS maxzoom FROM tiles");
		};
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

	public int getMaxZoom() {
		int ret = 99;
		if(mDatabase != null){
			final Cursor c = this.mDatabase.rawQuery("SELECT 17-minzoom AS ret FROM info", null);
			if (c != null) {
				if (c.moveToFirst()) {
					ret = c.getInt(c.getColumnIndexOrThrow("ret"));
				}
				c.close();
			}
		};
		return ret;
	}

	public int getMinZoom() {
		int ret = 0;
		if(mDatabase != null){
			final Cursor c = this.mDatabase.rawQuery("SELECT 17-maxzoom AS ret FROM info", null);
			if (c != null) {
				if (c.moveToFirst()) {
					ret = c.getInt(c.getColumnIndexOrThrow("ret"));
				}
				c.close();
			}
		}
		return ret;
	}

	@Override
	protected void finalize() throws Throwable {
		Ut.dd("finalize: Close SQLITEDB Database database");
		if(mDatabase != null)
			mDatabase.close();
		super.finalize();
	}

	public void freeDatabases() {
		if(mDatabase != null)
			if(mDatabase.isOpen())
			{
				mDatabase.close();
				Ut.dd("Close SQLITEDB Database");
			}
	}


}
