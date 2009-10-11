package com.robert.maps.kml;

import java.io.File;
import java.text.SimpleDateFormat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.robert.maps.R;
import com.robert.maps.utils.Ut;

public class GeoDatabase {
	protected final Context mCtx;
	private final SQLiteDatabase mDatabase;
	protected final SimpleDateFormat DATE_FORMAT_ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	public GeoDatabase(Context ctx) {
		super();
		mCtx = ctx;
		mDatabase = getDatabase();
	}

	public void addPoi(String aName, String aDescr, double aLat, double aLon, double aAlt, int aCategoryId,
			int aPointSourceId) {
		if (isDatabaseReady()) {
			final ContentValues cv = new ContentValues();
			cv.put("name", aName);
			cv.put("descr", aDescr);
			cv.put("lat", aLat);
			cv.put("lon", aLon);
			cv.put("alt", aAlt);
			cv.put("categoryid", aCategoryId);
			cv.put("pointsourceid", aPointSourceId);
			this.mDatabase.insert("points", null, cv);
		}
	}

	public Cursor getPoiListCursor() {
		if (isDatabaseReady()) {
			return mDatabase.rawQuery("SELECT lat, lon, name, descr FROM points ORDER BY lat, lon", null);
		}

		return null;
	}

	private boolean isDatabaseReady() {
		boolean ret = true;

		if(mDatabase == null)
			ret = false;

		if(ret == false)
			Toast.makeText(mCtx, mCtx.getText(R.string.message_geodata_notavailable), Toast.LENGTH_LONG).show();

		return ret;
	}

	protected SQLiteDatabase getDatabase() {
		File folder = Ut.getRMapsFolder("data", false);
		if(!folder.exists()) // no sdcard
			return null;

		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(folder.getAbsolutePath() + "/geodata.db", null);

		// for ver.1
		if(db.needUpgrade(1)) {
			Ut.dd("Upgrade data.db from ver.0 to ver.1");
			db.execSQL("CREATE TABLE IF NOT EXISTS 'points' (name VARCHAR, descr VARCHAR, lat FLOAT DEFAULT '0', lon FLOAT DEFAULT '0', alt FLOAT DEFAULT '0', categoryid INTEGER, pointsourceid INTEGER);");
			db.execSQL("CREATE TABLE IF NOT EXISTS 'pointsource' (pointsourceid INTEGER, name VARCHAR);");
			db.execSQL("CREATE TABLE IF NOT EXISTS 'category' (categoryid INTEGER, name VARCHAR);");
			db.setVersion(1);
		}

		return db;
	}
}
