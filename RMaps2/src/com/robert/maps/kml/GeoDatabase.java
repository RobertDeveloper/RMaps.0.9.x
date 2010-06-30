package com.robert.maps.kml;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.robert.maps.R;
import com.robert.maps.kml.constants.PoiConstants;
import com.robert.maps.utils.RSQLiteOpenHelper;
import com.robert.maps.utils.Ut;

public class GeoDatabase implements PoiConstants{
	protected final Context mCtx;
	private SQLiteDatabase mDatabase;
	protected final SimpleDateFormat DATE_FORMAT_ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	public GeoDatabase(Context ctx) {
		super();
		mCtx = ctx;
		mDatabase = getDatabase();
	}

	public void addPoi(String aName, String aDescr, double aLat, double aLon, double aAlt, int aCategoryId,
			int aPointSourceId, int hidden, int iconid) {
		if (isDatabaseReady()) {
			final ContentValues cv = new ContentValues();
			cv.put("name", aName);
			cv.put("descr", aDescr);
			cv.put("lat", aLat);
			cv.put("lon", aLon);
			cv.put("alt", aAlt);
			cv.put("categoryid", aCategoryId);
			cv.put("pointsourceid", aPointSourceId);
			cv.put("hidden", hidden);
			cv.put("iconid", iconid);
			this.mDatabase.insert("points", null, cv);
		}
	}

	public void updatePoi(int id, String aName, String aDescr, double aLat, double aLon, double aAlt, int aCategoryId,
			int aPointSourceId, int hidden, int iconid) {
		if (isDatabaseReady()) {
			final ContentValues cv = new ContentValues();
			cv.put("name", aName);
			cv.put("descr", aDescr);
			cv.put("lat", aLat);
			cv.put("lon", aLon);
			cv.put("alt", aAlt);
			cv.put("categoryid", aCategoryId);
			cv.put("pointsourceid", aPointSourceId);
			cv.put("hidden", hidden);
			cv.put("iconid", iconid);
			String[] args = {"" + id};
			this.mDatabase.update("points", cv, "pointid = @1", args);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if(mDatabase != null){
			if(mDatabase.isOpen()){
				mDatabase.close();
				Ut.dd("finalize: Close database");
				mDatabase = null;
			}
		}
		super.finalize();
	}

	public Cursor getPoiListCursor() {
		if (isDatabaseReady()) {
			// не менять порядок полей
			return mDatabase.rawQuery("SELECT lat, lon, name, descr, pointid, pointid _id, pointid ID FROM points ORDER BY lat, lon", null);
		}

		return null;
	}

	public Cursor getPoiListNotHiddenCursor(int zoom, double left, double right, double top, double bottom) {
		if (isDatabaseReady()) {
			// не менять порядок полей
			return mDatabase
					.rawQuery(
							"SELECT poi.lat, poi.lon, poi.name, poi.descr, poi.pointid, poi.pointid _id, poi.pointid ID, poi.categoryid, cat.iconid FROM points poi LEFT JOIN category cat ON cat.categoryid = poi.categoryid WHERE poi.hidden = 0 AND cat.hidden = 0 "
							+"AND cat.minzoom <= " + (zoom + 1)
							+ " AND poi.lon BETWEEN " + left + " AND " + right
							+ " AND poi.lat BETWEEN " + bottom + " AND " + top
							+ " ORDER BY lat, lon", null);
		}

		return null;
	}

	public Cursor getPoiCategoryListCursor() {
		if (isDatabaseReady()) {
			// не менять порядок полей
			return mDatabase.rawQuery("SELECT name, categoryid _id FROM category ORDER BY name", null);
		}

		return null;
	}

	public Cursor getPoi(final int id) {
		if (isDatabaseReady()) {
			// не менять порядок полей
			return mDatabase
					.rawQuery(
							"SELECT lat, lon, name, descr, pointid, alt, hidden, categoryid, pointsourceid, iconid FROM points WHERE pointid = "
									+ id, null);
		}

		return null;
	}

	public void deletePoi(final int id) {
		if (isDatabaseReady()) {
			mDatabase.execSQL("DELETE FROM points WHERE pointid = " + id);
		}
	}

	public void deletePoiCategory(final int id) {
		if (isDatabaseReady() && id != 0) { // predef category My POI never delete
			mDatabase.execSQL("DELETE FROM category WHERE categoryid = " + id);
		}
	}

	private boolean isDatabaseReady() {
		boolean ret = true;

		if(mDatabase == null)
			mDatabase = getDatabase();

		if(mDatabase == null)
			ret = false;
		else if(!mDatabase.isOpen())
			mDatabase = getDatabase();

		if(ret == false)
			Toast.makeText(mCtx, mCtx.getText(R.string.message_geodata_notavailable), Toast.LENGTH_LONG).show();

		return ret;
	}

	public void FreeDatabases(){
		if(mDatabase != null){
			if(mDatabase.isOpen()){
				mDatabase.close();
				Ut.dd("Close database");
				mDatabase = null;
			}
		}
	}

	protected SQLiteDatabase getDatabase() {
		File folder = Ut.getRMapsFolder("data", false);
		if(!folder.exists()) // no sdcard
			return null;

		SQLiteDatabase db = new GeoDatabaseHelper(mCtx, folder.getAbsolutePath() + "/geodata.db").getWritableDatabase();

		return db;
	}

	protected class GeoDatabaseHelper extends RSQLiteOpenHelper {
		public GeoDatabaseHelper(final Context context, final String name) {
			super(context, name, null, 5);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(PoiConstants.SQL_CREATE_points);
			db.execSQL(PoiConstants.SQL_CREATE_pointsource);
			db.execSQL(PoiConstants.SQL_CREATE_category);
			db.execSQL(PoiConstants.SQL_ADD_category);
			db.execSQL(PoiConstants.SQL_CREATE_tracks);
			db.execSQL(PoiConstants.SQL_CREATE_trackpoints);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Ut.dd("Upgrade data.db from ver." + oldVersion + " to ver."
					+ newVersion);

			if (oldVersion < 2) {
				db.execSQL(PoiConstants.SQL_UPDATE_1_1);
				db.execSQL(PoiConstants.SQL_UPDATE_1_2);
				db.execSQL(PoiConstants.SQL_UPDATE_1_3);
				db.execSQL(PoiConstants.SQL_CREATE_points);
				db.execSQL(PoiConstants.SQL_UPDATE_1_5);
				db.execSQL(PoiConstants.SQL_UPDATE_1_6);
				db.execSQL(PoiConstants.SQL_UPDATE_1_7);
				db.execSQL(PoiConstants.SQL_UPDATE_1_8);
				db.execSQL(PoiConstants.SQL_UPDATE_1_9);
				db.execSQL(PoiConstants.SQL_CREATE_category);
				db.execSQL(PoiConstants.SQL_ADD_category);
				//db.execSQL(PoiConstants.SQL_UPDATE_1_11);
				//db.execSQL(PoiConstants.SQL_UPDATE_1_12);
			}
			if (oldVersion < 3) {
				db.execSQL(PoiConstants.SQL_UPDATE_2_7);
				db.execSQL(PoiConstants.SQL_UPDATE_2_8);
				db.execSQL(PoiConstants.SQL_UPDATE_2_9);
				db.execSQL(PoiConstants.SQL_CREATE_category);
				db.execSQL(PoiConstants.SQL_UPDATE_2_11);
				db.execSQL(PoiConstants.SQL_UPDATE_2_12);
			}
			if (oldVersion < 5) {
				db.execSQL(PoiConstants.SQL_CREATE_tracks);
				db.execSQL(PoiConstants.SQL_CREATE_trackpoints);
			}
		}

	}

	public Cursor getPoiCategory(int id) {
		if (isDatabaseReady()) {
			// не менять порядок полей
			return mDatabase.rawQuery("SELECT name, categoryid, hidden, iconid, minzoom FROM category WHERE categoryid = " + id, null);
		}

		return null;
	}

	public void addPoiCategory(String title, int hidden, int iconid) {
		if (isDatabaseReady()) {
			final ContentValues cv = new ContentValues();
			cv.put("name", title);
			cv.put("hidden", hidden);
			cv.put("iconid", iconid);
			this.mDatabase.insert("category", null, cv);
		}
	}

	public void updatePoiCategory(int id, String title, int hidden, int iconid, int minzoom) {
		if (isDatabaseReady()) {
			final ContentValues cv = new ContentValues();
			cv.put("name", title);
			cv.put("hidden", hidden);
			cv.put("iconid", iconid);
			cv.put("minzoom", minzoom);
			String[] args = {"" + id};
			this.mDatabase.update("category", cv, "categoryid = @1", args);
		}
	}

	public void DeleteAllPoi() {
		if (isDatabaseReady()) {
			mDatabase.execSQL("DELETE FROM points");
		}
	}

	public void beginTransaction(){
		mDatabase.beginTransaction();
	}

	public void rollbackTransaction(){
		mDatabase.endTransaction();
	}

	public void commitTransaction(){
		mDatabase.setTransactionSuccessful();
		mDatabase.endTransaction();
	}

	public Cursor getTrackListCursor() {
		if (isDatabaseReady()) {
			// не менять порядок полей
			return mDatabase.rawQuery("SELECT name, descr, trackid _id, CASE WHEN show=1 THEN "
					+ R.drawable.btn_check_buttonless_on + " ELSE " + R.drawable.btn_check_buttonless_off
					+ " END as image FROM tracks ORDER BY trackid DESC;", null);
		}

		return null;
	}

	public long addTrack(String name, String descr, int show) {
		long newId = -1;

		if (isDatabaseReady()) {
			final ContentValues cv = new ContentValues();
			cv.put("name", name);
			cv.put("descr", descr);
			cv.put("show", show);
			newId = this.mDatabase.insert("tracks", null, cv);
		}

		return newId;
	}

	public void updateTrack(int id, String name, String descr, int show) {
		if (isDatabaseReady()) {
			final ContentValues cv = new ContentValues();
			cv.put("name", name);
			cv.put("descr", descr);
			cv.put("show", show);
			String[] args = {"" + id};
			this.mDatabase.update("tracks", cv, "trackid = @1", args);
		}
	}

	public void addTrackPoint(long trackid, double lat, double lon, double alt, double speed, Date date) {
		if (isDatabaseReady()) {
			final ContentValues cv = new ContentValues();
			cv.put("trackid", trackid);
			cv.put("lat", lat);
			cv.put("lon", lon);
			cv.put("alt", alt);
			cv.put("speed", speed);
			//cv.put("date", date.getTime());
			this.mDatabase.insert("trackpoints", null, cv);
		}
	}

	public Cursor getTrackChecked() {
		if (isDatabaseReady()) {
			// не менять порядок полей
			return mDatabase.rawQuery("SELECT name, descr, show, trackid FROM tracks WHERE show = 1 LIMIT 1",
					null);
		}

		return null;
	}

	public Cursor getTrack(long trackid) {
		if (isDatabaseReady()) {
			// не менять порядок полей
			return mDatabase.rawQuery("SELECT name, descr, show FROM tracks WHERE trackid = " + trackid,
					null);
		}

		return null;
	}

	public Cursor getTrackPoints(long trackid) {
		if (isDatabaseReady()) {
			// не менять порядок полей
			return mDatabase.rawQuery("SELECT lat, lon, alt, speed, date FROM trackpoints WHERE trackid = " + trackid + " ORDER BY id",
					null);
		}

		return null;
	}

	public void setTrackChecked(int id){
		if (isDatabaseReady()) {
			mDatabase.execSQL("UPDATE tracks SET show = 1 - show * 1 WHERE trackid = " + id);
			mDatabase.execSQL("UPDATE tracks SET show = 0 WHERE trackid <> " + id);
		}
	}

	public void deleteTrack(int id) {
		if (isDatabaseReady()) {
			beginTransaction();
			mDatabase.execSQL("DELETE FROM trackpoints WHERE trackid = " + id);
			mDatabase.execSQL("DELETE FROM tracks WHERE trackid = " + id);
			commitTransaction();
		}
	}

	public int saveTrackFromWriter(SQLiteDatabase db){
		int res = 0;
		if (isDatabaseReady()) {
			final Cursor c = db.rawQuery("SELECT lat, lon, alt, speed, date FROM trackpoints ORDER BY id;", null);
			if(c != null){
				if(c.getCount() > 1){
					beginTransaction();

					res = c.getCount();
					long newId = -1;

					final ContentValues cv = new ContentValues();
					cv.put("name", "Track");
					cv.put("show", 0);
					newId = mDatabase.insert("tracks", null, cv);

					cv.put("name", "Track "+newId);
					final String[] args = {"" + newId};
					mDatabase.update("tracks", cv, "trackid = @1", args);

					if (c.moveToFirst()) {
						do {
							cv.clear();
							cv.put("trackid", newId);
							cv.put("lat", c.getDouble(0));
							cv.put("lon", c.getDouble(1));
							cv.put("alt", c.getDouble(2));
							cv.put("speed", c.getDouble(3));
							cv.put("date", c.getInt(4));
							mDatabase.insert("trackpoints", null, cv);
						} while (c.moveToNext());
					}

					commitTransaction();
				}
				c.close();

				db.execSQL("DELETE FROM 'trackpoints';");
			}

		}

		return res;
	}


}
