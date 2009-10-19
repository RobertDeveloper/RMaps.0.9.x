package com.robert.maps.utils;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class CashDatabase {
	private SQLiteDatabase mDatabase;

	public void setFile(final File aFile) {
		if (mDatabase != null)
			mDatabase.close();

		Ut.dd("CashDatabase: Open SQLITEDB Database");
		//mDatabase = SQLiteDatabase.openOrCreateDatabase(aFile, null);
		mDatabase = new CashDatabaseHelper(null, aFile.getAbsolutePath()).getWritableDatabase();
	}

	protected class CashDatabaseHelper extends RSQLiteOpenHelper {
		public CashDatabaseHelper(final Context context, final String name) {
			super(context, name, null, 2);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if(oldVersion < 2){
				db.execSQL("DROP TABLE IF EXISTS info");
				db.execSQL("CREATE TABLE IF NOT EXISTS info AS SELECT 17-MAX(z) AS minzoom, 17-MIN(z) AS maxzoom FROM tiles");
			}
		}
		
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
				"SELECT maxzoom AS ret FROM info", null);
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
				"SELECT minzoom AS ret FROM info", null);
		if (c != null) {
			if (c.moveToFirst()) {
				ret = c.getInt(c.getColumnIndexOrThrow("ret"));
			}
			c.close();
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
