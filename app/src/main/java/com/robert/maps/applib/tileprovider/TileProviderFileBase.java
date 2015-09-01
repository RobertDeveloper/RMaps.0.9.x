package com.robert.maps.applib.tileprovider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.robert.maps.applib.utils.RSQLiteOpenHelper;
import com.robert.maps.applib.utils.Ut;

import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import java.io.File;

public class TileProviderFileBase extends TileProviderBase {
	protected final SQLiteDatabase mIndexDatabase;
	private static final String DELETE_FROM_ListCashTables = "DELETE FROM 'ListCashTables' WHERE name LIKE ('%sqlitedb')";
	private static final String INDEX_DB = "/index.db";
	
	public TileProviderFileBase(Context ctx) {
		super(ctx);
		this.mIndexDatabase = getIndexDatabase(ctx);
	}
	
	@Override
	public void Free() {
		if(mIndexDatabase != null)
			mIndexDatabase.close();
		super.Free();
	}

	public boolean needIndex(final String aCashTableName, final long aSizeFile, final long aLastModifiedFile, final boolean aBlockIndexing) {
		if(this.mIndexDatabase == null)
			return false;
		
		this.mIndexDatabase.execSQL("CREATE TABLE IF NOT EXISTS ListCashTables (name VARCHAR(100), lastmodified LONG NOT NULL, size LONG NOT NULL, minzoom INTEGER NOT NULL, maxzoom INTEGER NOT NULL, PRIMARY KEY(name) );");

		Cursor cur = null;
		cur = this.mIndexDatabase.rawQuery("SELECT COUNT(*) FROM ListCashTables", null);
		if (cur != null) {
			if (cur.getCount() > 0) {
				Ut.d("In table ListCashTables " + cur.getCount() + " records");
				cur.close();
				
				if(OpenStreetMapViewConstants.DEBUGMODE) {
					Ut.d("ListCashTables:");
					cur = this.mIndexDatabase.rawQuery("SELECT name, minzoom, maxzoom, size, lastmodified FROM ListCashTables", null);
					if(cur != null) {
						if(cur.moveToFirst()) {
							do {
								Ut.d(""+cur.getString(0)+" "
										+cur.getInt(1)+" "
										+cur.getInt(2)+" "
										+cur.getLong(3)+" "
										+cur.getLong(4)+" "
										);
							} while (cur.moveToNext());
						}
						cur.close();
					}
				}

				Ut.d("Check for aCashTableName = " + aCashTableName);
				cur = this.mIndexDatabase.rawQuery("SELECT size, lastmodified FROM ListCashTables WHERE lower(name) = lower('"
						+ aCashTableName + "') OR lower(name) = lower('" + aCashTableName.replace("usermap_", "cahs_") + "')", null);
				if (cur.getCount() > 0) {
					cur.moveToFirst();
					Ut.d("Record " + aCashTableName 
							+ " size = " + cur.getLong(cur.getColumnIndexOrThrow("size")) 
							+ " AND lastmodified = " + cur.getLong(cur.getColumnIndexOrThrow("lastmodified")));
					Ut.d("File " + aCashTableName + " size = " + aSizeFile + " AND lastmodified = "
							+ aLastModifiedFile);
				} else
					Ut.d("In table ListCashTables NO records for " + aCashTableName);
				cur.close();
			} else {
				Ut.d("In table ListCashTables NO records");
				cur.close();
			}
		} else
			Ut.d("NO table ListCashTables in database");

		Cursor c = null;
		c = this.mIndexDatabase.rawQuery("SELECT * FROM ListCashTables WHERE lower(name) = lower('" + aCashTableName + "') OR lower(name) = lower('" + aCashTableName.replace("usermap_", "cahs_") + "')", null);
		boolean res = false;
		if(c == null)
			return true;
		else if(c.moveToFirst() == false)
			res = true;
		else if(aBlockIndexing)
			res = false;
		else if (c.getLong(c.getColumnIndex("size")) != aSizeFile
				/*|| c.getLong(c.getColumnIndex("lastmodified")) != aLastModifiedFile*/)
			res = true;

		c.close();
		return res;
	}

	public void CommitIndex(final String aCashTableName, long aSizeFile, long aLastModifiedFile, int zoomMinInCashFile, int zoomMaxInCashFile) {
		this.mIndexDatabase.execSQL("CREATE TABLE IF NOT EXISTS ListCashTables (name VARCHAR(100), lastmodified LONG NOT NULL, size LONG NOT NULL, minzoom INTEGER NOT NULL, maxzoom INTEGER NOT NULL, PRIMARY KEY(name) );");
		this.mIndexDatabase.delete("ListCashTables", "lower(name) = lower('" + aCashTableName + "') OR lower(name) = lower('" + aCashTableName.replace("usermap_", "cahs_") + "')", null);
		final ContentValues cv = new ContentValues();
		cv.put("name", aCashTableName);
		cv.put("lastmodified", aLastModifiedFile);
		cv.put("size", aSizeFile);
		cv.put("minzoom", zoomMinInCashFile);
		cv.put("maxzoom", zoomMaxInCashFile);
		this.mIndexDatabase.insert("ListCashTables", null, cv);
	}

	public int ZoomMaxInCashFile(final String mapid) {
		int ret = 24;
		try {
			final Cursor c = this.mIndexDatabase.rawQuery("SELECT maxzoom FROM ListCashTables WHERE lower(name) = lower('"
					+ mapid + "') OR lower(name) = lower('" + mapid.replace("usermap_", "cahs_") + "')", null);
			if (c != null) {
				if (c.moveToFirst()) {
					ret = c.getInt(c.getColumnIndexOrThrow("maxzoom"));
				}
				c.close();
			}
		} catch (Exception e) {
		}

		return ret;
	}

	public int ZoomMinInCashFile(final String mapid) {
		int ret  = 0;
		try {
			final Cursor c = this.mIndexDatabase.rawQuery("SELECT minzoom FROM ListCashTables WHERE lower(name) = lower('"
					+ mapid + "') OR lower(name) = lower('" + mapid.replace("usermap_", "cahs_") + "')", null);
			if (c != null) {
				if (c.moveToFirst()) {
					ret = c.getInt(c.getColumnIndexOrThrow("minzoom"));
				}
				c.close();
			}
		} catch (Exception e) {
		}

		return ret;
	}

	private SQLiteDatabase getIndexDatabase(Context ctx) {
		File folder = Ut.getRMapsMainDir(ctx, "data");
		if(!folder.exists()) // no sdcard // TODO
			return null;

		Ut.d("OpenStreetMapTileFilesystemProvider: Open INDEX database");
		return new IndexDatabaseHelper(ctx, folder.getAbsolutePath() + INDEX_DB).getWritableDatabase();
	}
	
	protected class IndexDatabaseHelper extends RSQLiteOpenHelper {
		public IndexDatabaseHelper(final Context context, final String name) {
			super(context, name, null, 3);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if(oldVersion < 2){
				try {
					Ut.dd("Upgrade IndexDatabase ver."+oldVersion+" to ver."+newVersion);
					db.execSQL(DELETE_FROM_ListCashTables);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
