package com.robert.maps.applib.utils;

import java.io.File;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.robert.maps.applib.tileprovider.TileSource;

public class SQLiteMapDatabase implements ICacheProvider {
	private static final String SQL_CREATE_tiles = "CREATE TABLE IF NOT EXISTS tiles (x int, y int, z int, s int, image blob, PRIMARY KEY (x,y,z,s));";
	private static final String SQL_CREATE_info = "CREATE TABLE IF NOT EXISTS info (id Int, maxzoom Int, minzoom Int, params VARCHAR, PRIMARY KEY (id));";
	private static final String SQL_SELECT_MINZOOM = "SELECT 17-minzoom AS ret FROM info";
	private static final String SQL_SELECT_MAXZOOM = "SELECT 17-maxzoom AS ret FROM info";
	private static final String SQL_SELECT_PARAMS = "SELECT params FROM info";
	private static final String SQL_UPDATE_PARAMS = "UPDATE info SET params = ?";
	private static final String SQL_SELECT_IMAGE = "SELECT image as ret FROM tiles WHERE x = ? AND y = ? AND z = ?";
	private static final String SQL_DROP_tiles = "DROP TABLE IF EXISTS tiles";
	private static final String SQL_DROP_info = "DROP TABLE IF EXISTS info";
	private static final String SQL_tiles_count = "SELECT COUNT(*) cnt FROM tiles";
	private static final String RET = "ret";
	private static final long MAX_DATABASE_SIZE = 1945 * 1024 * 1024; // 1.9GB
	private static final String JOURNAL = "-journal";
	private static final String SQLITEDB = "sqlitedb";

	private SQLiteDatabase[] mDatabase = new SQLiteDatabase[0];
	private SQLiteDatabase mDatabaseWritable;
	private int mCurrentIndex = 0;
	private File mBaseFile = null;
	private int mBaseFileIndex = 0;

	private void initDatabaseFiles(final String aFileName, final boolean aCreateNewDatabaseFile) throws RException {
		for(int i = 0; i < mDatabase.length; i++)
			if (mDatabase[i] != null)
				mDatabase[i].close();
		
		//RException aException = null;
		
		mBaseFile = new File(aFileName);
		final File folder = mBaseFile.getParentFile();
		if(folder != null) {
			File[] files = folder.listFiles();
			if(files != null) {
				int j = 0;
				mBaseFileIndex = 0;
				// Подсчитаем количество подходящих файлов
				for (int i = 0; i < files.length; i++) {
					if(files[i].getName().startsWith(mBaseFile.getName()) && !files[i].getName().endsWith(JOURNAL)) {
						j = j + 1;
						
						try {
							final int index = Integer.getInteger(files[i].getName().replace(mBaseFile.getName(), ""));
							if(index > mBaseFileIndex)
								mBaseFileIndex = index;
						} catch (Exception e) {
						}
					}
				}
				final int dbFilesCnt = j;
				// Если нужно создать еще один, то резервируем для него место
				if(aCreateNewDatabaseFile || j == 0)
					j = j + 1;
				// Создаем массив определенного размера
				mDatabase = new SQLiteDatabase[j];
				// Заполняем массив 
				j = 0; long minsize = 0;
				for (int i = 0; i < files.length; i++) {
					if(files[i].getName().startsWith(mBaseFile.getName()) && !files[i].getName().endsWith(JOURNAL)) {
						try {
							mDatabase[j] = new CashDatabaseHelper(null, files[i].getAbsolutePath()).getWritableDatabase();
							mDatabase[j].setMaximumSize(MAX_DATABASE_SIZE);
							if(mDatabaseWritable == null) {
								mDatabaseWritable = mDatabase[j];
								minsize = files[i].length();
							} else {
								if(files[i].length() < minsize) {
									mDatabaseWritable = mDatabase[j];
									minsize = files[i].length();
								}
							}
							j = j + 1;
						} catch (Throwable e) {
							//aException = new RException(R.string.error_diskio, files[i].getAbsolutePath());
						}
					}
				}
				if(dbFilesCnt == 0) {
					mDatabase[0] = new CashDatabaseHelper(null, mBaseFile.getAbsolutePath()).getWritableDatabase();
					mDatabaseWritable = mDatabase[0];
				}
				if(aCreateNewDatabaseFile) {
					mDatabase[j] = new CashDatabaseHelper(null, mBaseFile.getAbsolutePath() + (mBaseFileIndex + 1)).getWritableDatabase();
					mDatabaseWritable = mDatabase[j];
				}
			}
		}
		
//		if(aException != null)
//			throw aException;
	}
	
	public synchronized void setFile(final String aFileName) throws SQLiteException, RException {
		initDatabaseFiles(aFileName, false);
	}

	public synchronized void setFile(final File aFile) throws SQLiteException, RException {
		setFile(aFile.getAbsolutePath());
	}

	protected class CashDatabaseHelper extends RSQLiteOpenHelper {
		public CashDatabaseHelper(final Context context, final String name) {
			super(context, name, null, 5);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_tiles);
			db.execSQL(SQL_CREATE_info);
			db.execSQL(SQL_INIT_INFO);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if(oldVersion < 5) {
				db.execSQL(SQL_DROP_info);
				db.execSQL(SQL_CREATE_info);
				db.execSQL(SQL_INIT_INFO);
			}
		}

	}
	
	public void updateMapParams(TileSource tileSource) {
		tileSource.ZOOM_MINLEVEL = getMinZoom();
		tileSource.ZOOM_MAXLEVEL = getMaxZoom();
	}
	
	private static final String SQL_INIT_INFO = "INSERT OR IGNORE INTO info (id, minzoom, maxzoom) SELECT 0, 0, 0;";
	private static final String SQL_UPDZOOM_UPDMIN = "UPDATE info SET minzoom = (SELECT DISTINCT z FROM tiles ORDER BY z ASC LIMIT 1);";
	private static final String SQL_UPDZOOM_UPDMAX = "UPDATE info SET maxzoom = (SELECT DISTINCT z FROM tiles ORDER BY z DESC LIMIT 1);";

	public synchronized void updateMinMaxZoom() throws SQLiteException {
		for(int i = 0; i < mDatabase.length; i++)
			if(mDatabase[i] != null){
				this.mDatabase[i].execSQL(SQL_CREATE_info);
				this.mDatabase[i].execSQL(SQL_INIT_INFO);
				this.mDatabase[i].execSQL(SQL_UPDZOOM_UPDMIN);
				this.mDatabase[i].execSQL(SQL_UPDZOOM_UPDMAX);
			}
	}

	public synchronized void putTile(final int aX, final int aY, final int aZ, final byte[] aData) throws RException {
		if (this.mDatabaseWritable != null) {
			final ContentValues cv = new ContentValues();
			cv.put("x", aX);
			cv.put("y", aY);
			cv.put("z", 17 - aZ);
			cv.put("s", System.currentTimeMillis() / 1000);
			cv.put("image", aData);
			try {
				this.mDatabaseWritable.insertOrThrow(TILES, null, cv);
			} catch (SQLException e) {
				initDatabaseFiles(mBaseFile.getAbsolutePath(), true);
			}
		}
	}
	
	private static final String SQL_DELTILE_WHERE = "s = 0 AND x = ? AND y = ? AND z = ?";
	private static final String TILES = "tiles";

	public synchronized byte[] getTile(final int aX, final int aY, final int aZ) {
		byte[] ret = null;

		int j = 0;
		for(int i = 0; i < mDatabase.length; i++) {
			j = mCurrentIndex + i;
			if(j >= mDatabase.length)
				j = j - mDatabase.length;
			
			if (this.mDatabase[j] != null && this.mDatabase[j].isOpen() && !this.mDatabase[j].isDbLockedByOtherThreads()) {
				final String[] args = {""+aX, ""+aY, ""+(17 - aZ)};
				try {
					final Cursor c = this.mDatabase[j].rawQuery(SQL_SELECT_IMAGE, args);
					if (c != null) {
						if (c.moveToFirst()) {
							ret = c.getBlob(c.getColumnIndexOrThrow(RET));
							c.close();
							
							if(ret != null)
								if(ret.length == 0) {
									mDatabase[j].delete(TILES, SQL_DELTILE_WHERE, args);
									ret = null;
								}

							mCurrentIndex = j;
							break;
						} else
							c.close();
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			
		}
		
		return ret;
	}
	
	public synchronized void deleteTile(final int aX, final int aY, final int aZ) {
		final String[] args = {""+aX, ""+aY, ""+(17 - aZ)};
		for(int i = 0; i < mDatabase.length; i++) {
			if(mDatabase[i] != null)
				mDatabase[i].delete(TILES, SQL_DELTILE_WHERE, args);
		}
	}

	public synchronized boolean existsTile(final int aX, final int aY, final int aZ) {
		final String[] args = {""+aX, ""+aY, ""+(17 - aZ)};
		boolean ret = false;
		for(int i = 0; i < mDatabase.length; i++) {
			if(mDatabase[i] != null) {
				final Cursor c = this.mDatabase[i].rawQuery(SQL_SELECT_IMAGE, args);
				if(c != null) {
					if(c.moveToFirst())
						ret = true;
					c.close();
				}
			}
			if(ret) break;
		}
		return ret;
	}

	public synchronized int getMaxZoom() {
		int ret = 0;
		
		try {
			for(int i = 0; i < mDatabase.length; i++) {
				if(mDatabase[i] != null){
					final Cursor c = this.mDatabase[i].rawQuery(SQL_SELECT_MINZOOM, null);
					if (c != null) {
						if (c.moveToFirst()) {
							final int zoom = c.getInt(c.getColumnIndexOrThrow(RET));
							if(zoom > ret)
								ret = zoom;
						}
						c.close();
					}
				};
			};
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return ret;
	}

	public synchronized int getMinZoom() {
		int ret = 22;
		
		try {
			for(int i = 0; i < mDatabase.length; i++) {
				if(mDatabase[i] != null){
					final Cursor c = this.mDatabase[i].rawQuery(SQL_SELECT_MAXZOOM, null);
					if (c != null) {
						if (c.moveToFirst()) {
							final int zoom = c.getInt(c.getColumnIndexOrThrow(RET));
							if(zoom < ret)
								ret = zoom;
						}
						c.close();
					}
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return ret;
	}

	@Override
	protected void finalize() throws Throwable {
		for(int i = 0; i < mDatabase.length; i++) {
			if(mDatabase[i] != null)
				mDatabase[i].close();
		}
		super.finalize();
	}

	public synchronized void freeDatabases() {
		for (int i = 0; i < mDatabase.length; i++) {
			if (mDatabase[i] != null)
				if (mDatabase[i].isOpen()) {
					mDatabase[i].close();
				}
		}
	}

	public synchronized byte[] getTile(String aURLstring, int aX, int aY, int aZ) {
		return getTile(aX, aY, aZ);
	}

	public synchronized void putTile(String aURLstring, int aX, int aY, int aZ, byte[] aData) throws RException {
		putTile(aX, aY, aZ, aData);
	}

	public synchronized void Free() {
		freeDatabases();
	}

	public void clearTiles() {
		for (int i = 0; i < mDatabase.length; i++) {
			if (mDatabase[i] != null) {
				mDatabase[i].execSQL(SQL_DROP_tiles);
				mDatabase[i].execSQL(SQL_CREATE_tiles);
			}
		}
	}

	public double getTileLenght() {
		double ret = 0L;
		if(mDatabase[0] != null) {
			final Cursor c = mDatabase[0].rawQuery(SQL_tiles_count, null); 
			if(c != null) {
				if(c.moveToFirst()) {
					if(c.getInt(0) > 0) {
						final File file = new File(mDatabase[0].getPath()); 
						ret = file.length()/c.getInt(0);
					};
				};
				c.close();
			}
		}
		return ret;
	}
	
	public JSONObject getParams() {
		JSONObject json = null;
		
		for (int i = 0; i < mDatabase.length; i++) {
			if(mDatabase[i].getPath().toLowerCase(Locale.US).endsWith(SQLITEDB)) {
				Cursor c = this.mDatabase[i].rawQuery(SQL_SELECT_PARAMS, null);
				if(c != null) {
					if(c.moveToFirst()) {
						try {
							if(c.getString(0) != null)
								json = new JSONObject(c.getString(0));
						} catch (Exception e) {
						}
						c.close();
						break;
					}
					c.close();
				}
			}
		}
		
		if(json == null)
			json = new JSONObject();
		
		return json;
	}
	
	public static final String MAPID = "mapid";
	public static final String MAPNAME = "mapname";
	public static final String ZOOM = "zoom";
	public static final String COORDS = "coords";
	public static final String ZOOMS = "zooms";

	public void setParams(String mapID, String mapName, int[] coordArr, int[] zoomArr, int zoom) {
		final JSONObject json = getParams();
		try {
			json.put(MAPID, mapID);
			json.put(MAPNAME, mapName);
			json.put(ZOOM, zoom);
			{
				final JSONArray jarr = new JSONArray();
				for(int i = 0; i < coordArr.length; i++)
					jarr.put(coordArr[i]);
				json.put(COORDS, jarr);
			}
			{
				final JSONArray jarr = new JSONArray();
				for(int i = 0; i < zoomArr.length; i++)
					jarr.put(zoomArr[i]);
				json.put(ZOOMS, jarr);
			}
			
		} catch (JSONException e) {
		}
		for (int i = 0; i < mDatabase.length; i++) {
			if(mDatabase[i].getPath().toLowerCase(Locale.US).endsWith(SQLITEDB)) {
				
				final String[] arg = {json.toString()};
				this.mDatabase[i].execSQL(SQL_UPDATE_PARAMS, arg);
				break;
			}
		}
		
	}

}
