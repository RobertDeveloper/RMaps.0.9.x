package com.robert.maps.utils;

import java.io.File;

import com.robert.maps.tileprovider.TileSource;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class SQLiteMapDatabase implements ICacheProvider {
	private static final String SQL_CREATE_tiles = "CREATE TABLE IF NOT EXISTS tiles (x int, y int, z int, s int, image blob, PRIMARY KEY (x,y,z,s));";
	private static final String SQL_CREATE_info = "CREATE TABLE IF NOT EXISTS info (maxzoom Int, minzoom Int);";
	private static final String SQL_SELECT_MINZOOM = "SELECT 17-minzoom AS ret FROM info";
	private static final String SQL_SELECT_MAXZOOM = "SELECT 17-maxzoom AS ret FROM info";
	private static final String SQL_SELECT_IMAGE = "SELECT image as ret FROM tiles WHERE s = 0 AND x = ? AND y = ? AND z = ?";
	private static final String RET = "ret";
	private static final long MAX_DATABASE_SIZE = 2 * 1024 * 1024 * 1024;
	private static final String JOURNAL = "-journal";

	private SQLiteDatabase[] mDatabase = new SQLiteDatabase[0];
	private SQLiteDatabase mDatabaseWritable;
	private int mCurrentIndex = 0;
	private File mBaseFile = null;
	private int mBaseFileIndex = 0;

	private void initDatabaseFiles(final String aFileName, final boolean aCreateNewDatabaseFile) {
		for(int i = 0; i < mDatabase.length; i++)
			if (mDatabase[i] != null)
				mDatabase[i].close();

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
	}
	
	public void setFile(final String aFileName) throws SQLiteException {
		initDatabaseFiles(aFileName, false);
	}

	public void setFile(final File aFile) throws SQLiteException {
		setFile(aFile.getAbsolutePath());
	}

	protected class CashDatabaseHelper extends RSQLiteOpenHelper {
		public CashDatabaseHelper(final Context context, final String name) {
			super(context, name, null, 3);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_tiles);
			db.execSQL(SQL_CREATE_info);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}

	}
	
	public void updateMapParams(TileSource tileSource) {
		tileSource.ZOOM_MINLEVEL = getMinZoom();
		tileSource.ZOOM_MAXLEVEL = getMaxZoom();
	}
	
	private static final String SQL_UPDZOOM_DROP = "DROP TABLE IF EXISTS info";
	private static final String SQL_UPDZOOM_CREATE = "CREATE TABLE info As SELECT 0 As minzoom, 0 As maxzoom;";
	private static final String SQL_UPDZOOM_UPDMIN = "UPDATE info SET minzoom = (SELECT DISTINCT z FROM tiles ORDER BY z ASC LIMIT 1);";
	private static final String SQL_UPDZOOM_UPDMAX = "UPDATE info SET maxzoom = (SELECT DISTINCT z FROM tiles ORDER BY z DESC LIMIT 1);";

	public void updateMinMaxZoom() throws SQLiteException {
		for(int i = 0; i < mDatabase.length; i++)
			if(mDatabase[i] != null){
				this.mDatabase[i].execSQL(SQL_UPDZOOM_DROP);
				this.mDatabase[i].execSQL(SQL_UPDZOOM_CREATE);
				this.mDatabase[i].execSQL(SQL_UPDZOOM_UPDMIN);
				this.mDatabase[i].execSQL(SQL_UPDZOOM_UPDMAX);
			}
	}

	public synchronized void putTile(final int aX, final int aY, final int aZ, final byte[] aData) {
		if (this.mDatabaseWritable != null) {
			final ContentValues cv = new ContentValues();
			cv.put("x", aX);
			cv.put("y", aY);
			cv.put("z", 17 - aZ);
			cv.put("s", 0);
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

	public /*synchronized*/ byte[] getTile(final int aX, final int aY, final int aZ) {
		byte[] ret = null;

		int j = 0;
		for(int i = 0; i < mDatabase.length; i++) {
			j = mCurrentIndex + i;
			if(j >= mDatabase.length)
				j = j - mDatabase.length;
			
			if (this.mDatabase[j] != null) {
				final String[] args = {""+aX, ""+aY, ""+(17 - aZ)};
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
			}
			
		}
		
		return ret;
	}

	public int getMaxZoom() {
		int ret = 0;
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
		return ret;
	}

	public int getMinZoom() {
		int ret = 99;
		
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

	public void freeDatabases() {
		for (int i = 0; i < mDatabase.length; i++) {
			if (mDatabase[i] != null)
				if (mDatabase[i].isOpen()) {
					mDatabase[i].close();
				}
		}
	}

	public byte[] getTile(String aURLstring, int aX, int aY, int aZ) {
		return getTile(aX, aY, aZ);
	}

	public void putTile(String aURLstring, int aX, int aY, int aZ, byte[] aData) {
		putTile(aX, aY, aZ, aData);
	}

	public void Free() {
		freeDatabases();
	}

}
