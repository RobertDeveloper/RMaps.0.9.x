// Created by plusminus on 21:46:41 - 25.09.2008
package org.andnav.osm.views.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.andnav.osm.exceptions.EmptyCacheException;
import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.robert.maps.R;
import com.robert.maps.utils.RSQLiteOpenHelper;
import com.robert.maps.utils.SQLiteMapDatabase;
import com.robert.maps.utils.Ut;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class OpenStreetMapTileFilesystemProvider implements OpenStreetMapConstants, OpenStreetMapViewConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int MAPTILEFSLOADER_SUCCESS_ID = 1000;
	public static final int MAPTILEFSLOADER_FAIL_ID = MAPTILEFSLOADER_SUCCESS_ID + 1;
	public static final int INDEXIND_SUCCESS_ID = MAPTILEFSLOADER_SUCCESS_ID + 2;
	public static final int INDEXIND_FAIL_ID = MAPTILEFSLOADER_SUCCESS_ID + 3;
	public static final int ERROR_MESSAGE = MAPTILEFSLOADER_SUCCESS_ID + 4;

	// public static final Options BITMAPLOADOPTIONS = new Options(){
	// {
	// inPreferredConfig = Config.RGB_565;
	// }
	// };

	// ===========================================================
	// Fields
	// ===========================================================

	protected final Context mCtx;
	protected final OpenStreetMapTileFilesystemProviderDataBase mDatabase;
	protected final int mMaxFSCacheByteSize;
	protected int mCurrentFSCacheByteSize;
	protected ExecutorService mThreadPool = Executors.newFixedThreadPool(2);
	protected final OpenStreetMapTileCache mCache;

	protected HashSet<String> mPending = new HashSet<String>();

	protected File mCashFile;
	protected SQLiteMapDatabase mUserMapDatabase;

    private ProgressDialog mProgressDialog;
    private boolean mStopIndexing = false;
    private boolean mBlockIndexing = false;

    // ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * @param ctx
	 * @param aMaxFSCacheByteSize
	 *            the size of the cached MapTiles will not exceed this size.
	 * @param aCache
	 *            to load fs-tiles to.
	 */
	public OpenStreetMapTileFilesystemProvider(final Context ctx, final int aMaxFSCacheByteSize,
			final OpenStreetMapTileCache aCache, final String aCacheDatabaseName) {
		this.mCtx = ctx;
		this.mMaxFSCacheByteSize = aMaxFSCacheByteSize;
		this.mDatabase = new OpenStreetMapTileFilesystemProviderDataBase(ctx);
		this.mCurrentFSCacheByteSize = this.mDatabase.getCurrentFSCacheByteSize();
		this.mCache = aCache;
		this.mUserMapDatabase = new SQLiteMapDatabase();

		if (DEBUGMODE)
			Log.i(DEBUGTAG, "Currently used cache-size is: " + this.mCurrentFSCacheByteSize + " of "
					+ this.mMaxFSCacheByteSize + " Bytes");

    	SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
    	mBlockIndexing = pref.getBoolean("pref_turnoffautoreindex", false);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public int getZoomMinInCashFile() {
		return mDatabase.ZoomMinInCashFile();
	}

	public int getZoomMaxInCashFile() {
		return mDatabase.ZoomMaxInCashFile();
	}

	public SQLiteMapDatabase getUserMapDatabase(){
		return mUserMapDatabase;
	}

	public boolean setUserMapFile(final String aFileName, final int aTileSourceType, final Handler callback) {
		mCashFile = new File(aFileName);
		if (mCashFile.exists() == false) {
			Ut.i("File " + aFileName + " not found");
			Toast.makeText(mCtx, "File " + aFileName + " not found", Toast.LENGTH_LONG).show();
			return false;
		}



		switch (aTileSourceType ) {
		case 5:
			try {
				IndexSQLiteFile(callback);
			} catch (NumberFormatException e) {
			} catch (IOException e) {
			}
			break;
		case 3:
			try {
				IndexMnmFile(callback);
			} catch (NumberFormatException e) {
			} catch (IOException e) {
			}
			break;
		case 4:
			try {
					IndexTarFile(callback);
				} catch (NumberFormatException e) {
				} catch (IOException e) {
				}
			break;
		}
		return true;
	}

	private void IndexTarFile(final Handler callback) throws NumberFormatException, IOException {
		mDatabase.setCashTable("cahs_" + mCashFile.getName().replace(".", "_").trim());

		long fileLength = mCashFile.length();
		long fileModified = mCashFile.lastModified();
		if (mDatabase.NeedIndex(fileLength, fileModified, mBlockIndexing))
		{
			mStopIndexing = false;
			ShowIndexingProgressDialog(fileLength);

			mDatabase.CreateTarIndex(fileLength, fileModified);

			this.mThreadPool.execute(new Runnable() {
				public void run() {
					try {
						final long startMs = System.currentTimeMillis();

						long fileLength = mCashFile.length();
						long fileModified = mCashFile.lastModified();
						int minzoom = 24, maxzoom = 0;

						InputStream in = null;
						in = new BufferedInputStream(new FileInputStream(mCashFile), 8192);
						String name; // 100 name of file
//						int mode; // file mode
//						int uid; // owner user ID
//						int gid; // owner group ID
						int tileSize; // 12 length of file in bytes
//						int mtime; // 12 modify time of file
//						int chksum; // checksum for header
//						byte[] link = new byte[1]; // indicator for links
//						String linkname; // 100 name of linked file
						int offset = 0, skip = 0;

						while (in.available() > 0) {
							name = Ut.readString(in, 100).trim();

//							mode = Integer.decode("0" + Util.readString(in, 8).trim());
//							uid = Integer.decode("0" + Util.readString(in, 8).trim());
//							gid = Integer.decode("0" + Util.readString(in, 8).trim());
							in.skip(24);
							tileSize = Integer.decode("0" + Ut.readString(in, 12).trim());
//							mtime = Integer.decode("0" + Util.readString(in, 12).trim());
//							in.read(link);
//							linkname = Util.readString(in, 100);
							in.skip(12 + 1 + 100);
							in.skip(512 - 100 - 8 - 8 - 8 - 12 - 12 - 1 - 100);
							offset += 512;

							if (tileSize > 0) {
								mDatabase.addTarIndexRow(name, offset, tileSize);

								if(tileSize % 512 == 0)
									skip = tileSize;
								else
									skip = tileSize + 512 - tileSize % 512;

								in.skip(skip);
								offset += skip;

								int zoom = Integer.parseInt(name.substring(0, 2)) - 1;
								if (zoom > maxzoom)
									maxzoom = zoom;
								if (zoom < minzoom)
									minzoom = zoom;
							}


							mProgressDialog.setProgress((int) (offset/1024));

							if(mStopIndexing)
								break;
						}

						final long endMs = System.currentTimeMillis();
						Log.i(DEBUGTAG, "Indexing time: " + (endMs - startMs) + "ms");

						mProgressDialog.dismiss();

						if (!mStopIndexing){
							mDatabase.CommitIndex(fileLength, fileModified, minzoom, maxzoom);

							final Message successMessage = Message.obtain(callback, INDEXIND_SUCCESS_ID);
							successMessage.sendToTarget();
						} else
							mDatabase.ClearIndex();

					} catch (NumberFormatException e) {
						e.printStackTrace();
						mDatabase.ClearIndex();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						mDatabase.ClearIndex();
					} catch (IOException e) {
						e.printStackTrace();
						mDatabase.ClearIndex();
					} catch (Exception e) {
					} catch (OutOfMemoryError e) {
						Ut.w("OutOfMemoryError");
					} finally {
					}
				}
			});
		}
	}

	private void IndexSQLiteFile(final Handler callback) throws IOException {
		try {
			mUserMapDatabase.setFile(mCashFile);
		} catch (SQLiteException e) {
			Toast.makeText(mCtx, "Error: "+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			return;
		}

		mDatabase.setCashTable("cahs_" + Ut.FileName2ID(mCashFile.getName()));

		if (mDatabase.NeedIndex(mCashFile.length(), mCashFile.lastModified(), mBlockIndexing))
		{
			mStopIndexing = false;
			ShowWaitDialog(mCtx.getString(R.string.message_updateminmax));

			this.mThreadPool.execute(new Runnable() {
				public void run() {
					try {
						final long fileLength = mCashFile.length();
						final long fileModified = mCashFile.lastModified();
						mUserMapDatabase.updateMinMaxZoom();
						final int minzoom = mUserMapDatabase.getMinZoom();
						final int maxzoom = mUserMapDatabase.getMaxZoom();

						mDatabase.CommitIndex(fileLength, fileModified, minzoom, maxzoom);
					} catch (Exception e) {
						Message.obtain(callback, ERROR_MESSAGE, "Error: "+e.getLocalizedMessage()).sendToTarget();
					}

					mProgressDialog.dismiss();

					Message.obtain(callback, INDEXIND_SUCCESS_ID).sendToTarget();
				}
			});
		}

	}

	private void IndexMnmFile(final Handler callback) throws IOException {
		try {
			mDatabase.setCashTable("cahs_" + Ut.FileName2ID(mCashFile.getName()));
		} catch (Exception e1) {
			return;
		}

		long fileLength = mCashFile.length();
		long fileModified = mCashFile.lastModified();
		if (mDatabase.NeedIndex(fileLength, fileModified, mBlockIndexing))
		{
			mStopIndexing = false;
			ShowIndexingProgressDialog(fileLength);

			try {
				mDatabase.CreateMnmIndex(fileLength, fileModified);
			} catch (Exception e1) {
				mProgressDialog.dismiss();
				return;
			}

			this.mThreadPool.execute(new Runnable() {
				public void run() {
					try {
						long fileLength = mCashFile.length();
						long fileModified = mCashFile.lastModified();
						int minzoom = 24, maxzoom = 0;
						InputStream in = null;
						in = new BufferedInputStream(new FileInputStream(mCashFile), 8192);

						byte b[] = new byte[5];
						in.read(b);
						int tilescount = Ut.readInt(in);

						int tileX = 0, tileY = 0, tileZ = 0, tileSize = 0;
						long offset = 9;
						byte mapType[] = new byte[1];

						for (int i = 0; i < tilescount; i++) {
							tileX = Ut.readInt(in);
							tileY = Ut.readInt(in);
							tileZ = Ut.readInt(in) - 1;
							in.read(mapType);
							tileSize = Ut.readInt(in);
							offset += 17;

							if (tileSize > 0) {
								try {
									mDatabase.addMnmIndexRow(tileX, tileY, tileZ, offset, tileSize);
								} catch (Exception e) {
									break;
								}
								//Log.e(DEBUGTAG, tileX + " " + tileY + " " + tileZ + " size=" + tileSize + " offset=" + offset);
								in.skip(tileSize);
								offset += tileSize;

								if (tileZ > maxzoom)
									maxzoom = tileZ;
								if (tileZ < minzoom)
									minzoom = tileZ;
							}

							mProgressDialog.setProgress((int) (offset/1024));

							if(mStopIndexing)
								break;
						}

						mProgressDialog.dismiss();

						if(!mStopIndexing){
							mDatabase.CommitIndex(fileLength, fileModified, minzoom, maxzoom);

							final Message successMessage = Message.obtain(callback, INDEXIND_SUCCESS_ID);
							successMessage.sendToTarget();
						} else
							mDatabase.ClearIndex();

					} catch (Exception e) {
						e.printStackTrace();
						mDatabase.ClearIndex();
					} finally {
					}
				}
			});
		}

	}

	private void ShowIndexingProgressDialog(long fileLength) {
		mProgressDialog = new ProgressDialog(mCtx);
		mProgressDialog.setTitle("Indexing");
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setMax((int)(fileLength/1024));
		mProgressDialog.setCancelable(true);
		mProgressDialog.setButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
			public void onCancel(DialogInterface dialog) {
				mStopIndexing = true;
			}

		});
		mProgressDialog.show();
		mProgressDialog.setProgress(0);
	}

	private void ShowWaitDialog(String message) {
		mProgressDialog = new ProgressDialog(mCtx);
		mProgressDialog.setMessage(message);
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setCancelable(false);

		mProgressDialog.show();
	}

	public int getCurrentFSCacheByteSize() {
		return this.mCurrentFSCacheByteSize;
	}

	public int getCurrentPendingCount() {
		return mPending.size();
	}

	public void loadMapTileFromMNM(final String aTileURLString, final Handler callback, final int x, final int y,
			final int z) throws IOException {
		if (this.mPending.contains(aTileURLString))
			return;

		final String formattedTileURLString = aTileURLString.replace("/", "_");
		final InputStream in = new BufferedInputStream(new FileInputStream(mCashFile), 8192);

		this.mPending.add(aTileURLString);

		this.mThreadPool.execute(new Runnable() {
			public void run() {
				OutputStream out = null;
				try {
					// File exists, otherwise a FileNotFoundException would have been thrown
					OpenStreetMapTileFilesystemProvider.this.mDatabase.incrementUse(formattedTileURLString);

					Param4ReadData Data = new Param4ReadData(0, 0);
					if(mDatabase.findMnmIndex(x, y, z, Data)) {
						final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
						out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);

						byte[] tmp = new byte[Data.size];
						in.skip(Data.offset);
						int read = in.read(tmp);
						if (read > 0) {
							out.write(tmp, 0, read);
						}
						out.flush();

						final byte[] data = dataStream.toByteArray();
						try {
							final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);

							OpenStreetMapTileFilesystemProvider.this.mCache.putTile(aTileURLString, bmp);
						} catch (OutOfMemoryError e) {
							Ut.w("OutOfMemoryError");
							// TODO Попытка отловить OutOfMemory
							e.printStackTrace();
						}

						final Message successMessage = Message.obtain(callback, MAPTILEFSLOADER_SUCCESS_ID);
						successMessage.sendToTarget();
					}
				} catch (Exception e) {
					Message.obtain(callback, MAPTILEFSLOADER_FAIL_ID).sendToTarget();
					if (DEBUGMODE)
						Log.e(DEBUGTAG, "Error Loading MapTile from FS. Exception: " + e.getClass().getSimpleName(), e);
				} finally {
					StreamUtils.closeStream(in);
					StreamUtils.closeStream(out);
				}

				OpenStreetMapTileFilesystemProvider.this.mPending.remove(aTileURLString);
			}
		});

	}

	public void loadMapTileFromSQLite(final String aTileURLString, final Handler callback, final int x, final int y,
			final int z) throws IOException {
		if (this.mPending.contains(aTileURLString))
			return;

		final String formattedTileURLString = aTileURLString.replace("/", "_");

		this.mPending.add(aTileURLString);

		this.mThreadPool.execute(new Runnable() {
			public void run() {
				try {
					// File exists, otherwise a FileNotFoundException would have been thrown
					OpenStreetMapTileFilesystemProvider.this.mDatabase.incrementUse(formattedTileURLString);

					final byte[] data = OpenStreetMapTileFilesystemProvider.this.mUserMapDatabase.getTile(x, y, z);

					if(data != null){
						final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
						OpenStreetMapTileFilesystemProvider.this.mCache.putTile(aTileURLString, bmp);
					}
				} catch (Exception e) {
					Message.obtain(callback, MAPTILEFSLOADER_FAIL_ID).sendToTarget();
				} catch (OutOfMemoryError e) {
					Ut.w("OutOfMemoryError");
					Message.obtain(callback, MAPTILEFSLOADER_FAIL_ID).sendToTarget();
				}

				final Message successMessage = Message.obtain(callback, MAPTILEFSLOADER_SUCCESS_ID);
				successMessage.sendToTarget();

				OpenStreetMapTileFilesystemProvider.this.mPending.remove(aTileURLString);
			}
		});

	}

	public void loadMapTileFromTAR(final String aTileURLString, final Handler callback) throws IOException {
		if (this.mPending.contains(aTileURLString))
			return;

		final String formattedTileURLString = aTileURLString.replace("/", "_");
		final InputStream in = new BufferedInputStream(new FileInputStream(mCashFile), 8192);

		this.mPending.add(aTileURLString);

		this.mThreadPool.execute(new Runnable() {
			public void run() {
				OutputStream out = null;
				try {
					OpenStreetMapTileFilesystemProvider.this.mDatabase.incrementUse(formattedTileURLString);

					Param4ReadData Data = new Param4ReadData(0, 0);
					if(mDatabase.findTarIndex(aTileURLString, Data)) {
						final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
						out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);

						byte[] tmp = new byte[Data.size];
						in.skip(Data.offset);
						int read = in.read(tmp);
						if (read > 0) {
							out.write(tmp, 0, read);
						}
						out.flush();
						in.skip(Data.size % 512);

						final byte[] data = dataStream.toByteArray();
						Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
						OpenStreetMapTileFilesystemProvider.this.mCache.putTile(aTileURLString, bmp);

						final Message successMessage = Message.obtain(callback, MAPTILEFSLOADER_SUCCESS_ID);
						successMessage.sendToTarget();
					}

					if (DEBUGMODE)
						Log.d(DEBUGTAG, "Loaded: " + aTileURLString + " to MemCache.");
				} catch (OutOfMemoryError e) {
					Ut.w("OutOfMemoryError");
					Message.obtain(callback, MAPTILEFSLOADER_FAIL_ID).sendToTarget();
				} catch (Exception e) {
					Message.obtain(callback, MAPTILEFSLOADER_FAIL_ID).sendToTarget();
					if (DEBUGMODE)
						Log.e(DEBUGTAG, "Error Loading MapTile from FS. Exception: " + e.getClass().getSimpleName(), e);
				} finally {
					StreamUtils.closeStream(in);
					StreamUtils.closeStream(out);
				}

				OpenStreetMapTileFilesystemProvider.this.mPending.remove(aTileURLString);
			}
		});

	}

	public void loadMapTileToMemCacheAsync(final String aTileURLString, final Handler callback)
			throws FileNotFoundException {
		if (this.mPending.contains(aTileURLString))
			return;


		//try {
			final String formattedTileURLString = OpenStreetMapTileNameFormatter.format(aTileURLString);
			final InputStream in = new BufferedInputStream(OpenStreetMapTileFilesystemProvider.this.mCtx
					.openFileInput(formattedTileURLString), 8192);
			this.mPending.add(aTileURLString);


			this.mThreadPool.execute(new Runnable() {
				public void run() {
					OutputStream out = null;
					try {
						// File exists, otherwise a FileNotFoundException would have been thrown
						OpenStreetMapTileFilesystemProvider.this.mDatabase.incrementUse(formattedTileURLString);

						final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
						out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
						StreamUtils.copy(in, out);
						out.flush();

						final byte[] data = dataStream.toByteArray();

						//final BitmapFactory.Options options = new BitmapFactory.Options();
						//options.inSampleSize = 2;
						final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length); // , BITMAPLOADOPTIONS);

						OpenStreetMapTileFilesystemProvider.this.mCache.putTile(aTileURLString, bmp);

						final Message successMessage = Message.obtain(callback, MAPTILEFSLOADER_SUCCESS_ID);
						successMessage.sendToTarget();

						if (DEBUGMODE)
							Log.d(DEBUGTAG, "Loaded: " + aTileURLString + " to MemCache.");
					} catch (OutOfMemoryError e) {
						Ut.w("OutOfMemoryError");
						Message.obtain(callback, MAPTILEFSLOADER_FAIL_ID).sendToTarget();
					} catch (Exception e) {
						Message.obtain(callback, MAPTILEFSLOADER_FAIL_ID).sendToTarget();
						if (DEBUGMODE)
							Log.e(DEBUGTAG, "Error Loading MapTile from FS. Exception: " + e.getClass().getSimpleName(), e);
					} finally {
						StreamUtils.closeStream(in);
						StreamUtils.closeStream(out);
					}

					OpenStreetMapTileFilesystemProvider.this.mPending.remove(aTileURLString);
				}
			});


		//} catch (Exception e) {
		//	Ut.dd("catch catch catch catch");
		//};

	}

	public void saveFile(final String aURLString, final byte[] someData) throws IOException {
		final String filename = OpenStreetMapTileNameFormatter.format(aURLString);

		final FileOutputStream fos = this.mCtx.openFileOutput(filename, Context.MODE_WORLD_READABLE);
		final BufferedOutputStream bos = new BufferedOutputStream(fos, StreamUtils.IO_BUFFER_SIZE);
		bos.write(someData);

		bos.flush();
		bos.close();

		synchronized (this) {
			final int bytesGrown = this.mDatabase.addTileOrIncrement(filename, someData.length);
			this.mCurrentFSCacheByteSize += bytesGrown;

			if (DEBUGMODE)
				Log.i(DEBUGTAG, "FSCache Size is now: " + this.mCurrentFSCacheByteSize + " Bytes");

			/* If Cache is full... */
			try {

				if (this.mCurrentFSCacheByteSize > this.mMaxFSCacheByteSize) {
					if (DEBUGMODE)
						Log.d(DEBUGTAG, "Freeing FS cache...");
					this.mCurrentFSCacheByteSize -= this.mDatabase
							.deleteOldest((int) (this.mMaxFSCacheByteSize * 0.05f)); // Free 5% of cache
				}
			} catch (EmptyCacheException e) {
				if (DEBUGMODE)
					Log.e(DEBUGTAG, "Cache empty", e);
			}
		}
	}

	public void clearCurrentFSCache() {
		cutCurrentFSCacheBy(Integer.MAX_VALUE); // Delete all
	}

	public void cutCurrentFSCacheBy(final int bytesToCut) {
		try {
			this.mDatabase.deleteOldest(Integer.MAX_VALUE); // Delete all
			this.mCurrentFSCacheByteSize = 0;
		} catch (EmptyCacheException e) {
			if (DEBUGMODE)
				Log.e(DEBUGTAG, "Cache empty", e);
		}
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	private interface OpenStreetMapTileFilesystemProviderDataBaseConstants {
		public static final String DATABASE_NAME = "osmaptilefscache_db";
		public static final int DATABASE_VERSION = 2;

		public static final String T_FSCACHE = "t_fscache";
		public static final String T_FSCACHE_NAME = "name_id";
		public static final String T_FSCACHE_TIMESTAMP = "timestamp";
		public static final String T_FSCACHE_USAGECOUNT = "countused";
		public static final String T_FSCACHE_FILESIZE = "filesize";

		public static final String T_FSCACHE_CREATE_COMMAND = "CREATE TABLE IF NOT EXISTS " + T_FSCACHE + " ("
				+ T_FSCACHE_NAME + " VARCHAR(255)," + T_FSCACHE_TIMESTAMP + " DATE NOT NULL," + T_FSCACHE_USAGECOUNT
				+ " INTEGER NOT NULL DEFAULT 1," + T_FSCACHE_FILESIZE + " INTEGER NOT NULL," + " PRIMARY KEY("
				+ T_FSCACHE_NAME + ")" + ");";

//		public static final String T_FSCACHE_SELECT_LEAST_USED = "SELECT " + T_FSCACHE_NAME + "," + T_FSCACHE_FILESIZE
//				+ " FROM " + T_FSCACHE + " WHERE " + T_FSCACHE_USAGECOUNT + " = (SELECT MIN(" + T_FSCACHE_USAGECOUNT
//				+ ") FROM " + T_FSCACHE + ")";
		public static final String T_FSCACHE_SELECT_OLDEST = "SELECT " + T_FSCACHE_NAME + "," + T_FSCACHE_FILESIZE
				+ " FROM " + T_FSCACHE + " ORDER BY " + T_FSCACHE_TIMESTAMP + " ASC";
	}

	private class Param4ReadData {
		public int offset, size;

		Param4ReadData(int offset, int size) {
			this.offset = offset;
			this.size = size;
		}
	}

	private class OpenStreetMapTileFilesystemProviderDataBase implements
			OpenStreetMapTileFilesystemProviderDataBaseConstants, OpenStreetMapViewConstants {
		// ===========================================================
		// Fields
		// ===========================================================

		protected final Context mCtx;
		protected final SQLiteDatabase mDatabase;
		protected final SimpleDateFormat DATE_FORMAT_ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

		protected String mCashTableName;
		protected final SQLiteDatabase mIndexDatabase;

		// ===========================================================
		// Constructors
		// ===========================================================

		public OpenStreetMapTileFilesystemProviderDataBase(final Context context) {
			this.mCtx = context;
			this.mCashTableName = "";
			this.mIndexDatabase = getIndexDatabase();
			this.mDatabase = new AndNavDatabaseHelper(context).getWritableDatabase();
		}

		public int ZoomMaxInCashFile() {
			int ret = 24;
			try {
				final Cursor c = this.mIndexDatabase.rawQuery("SELECT maxzoom FROM ListCashTables WHERE name = '"
						+ mCashTableName + "'", null);
				if (c != null) {
					if (c.moveToFirst()) {
						ret = c.getInt(c.getColumnIndexOrThrow("maxzoom"));
					}
					c.close();
				}
			} catch (Exception e) {
				Toast.makeText(mCtx, R.string.message_corruptindex, Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}

			return ret;
		}

		public int ZoomMinInCashFile() {
			int ret  = 0;
			try {
				final Cursor c = this.mIndexDatabase.rawQuery("SELECT minzoom FROM ListCashTables WHERE name = '"
						+ mCashTableName + "'", null);
				if (c != null) {
					if (c.moveToFirst()) {
						ret = c.getInt(c.getColumnIndexOrThrow("minzoom"));
					}
					c.close();
				}
			} catch (Exception e) {
				Toast.makeText(mCtx, R.string.message_corruptindex, Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}

			return ret;
		}

		public void setCashTable(final String aTableName) {
			mCashTableName = aTableName;
		}

		public boolean NeedIndex(final long aSizeFile, final long aLastModifiedFile, final boolean aBlockIndexing) {
			this.mIndexDatabase.execSQL("CREATE TABLE IF NOT EXISTS ListCashTables (name VARCHAR(100), lastmodified LONG NOT NULL, size LONG NOT NULL, minzoom INTEGER NOT NULL, maxzoom INTEGER NOT NULL, PRIMARY KEY(name) );");

			Cursor cur = null;
			cur = this.mIndexDatabase.rawQuery("SELECT COUNT(*) FROM ListCashTables", null);
			if (cur != null) {
				if (cur.getCount() > 0) {
					Log.e(DEBUGTAG, "In table ListCashTables " + cur.getCount() + " records");
					cur.close();

					cur = this.mIndexDatabase.rawQuery("SELECT size, lastmodified FROM ListCashTables WHERE name = '"
							+ mCashTableName + "'", null);
					if (cur.getCount() > 0) {
						cur.moveToFirst();
						Log.e(DEBUGTAG, "Record " + mCashTableName + " size = "
								+ cur.getLong(cur.getColumnIndexOrThrow("size")) + " AND lastmodified = "
								+ cur.getLong(cur.getColumnIndexOrThrow("lastmodified")));
						Log.e(DEBUGTAG, "File " + mCashTableName + " size = " + aSizeFile + " AND lastmodified = "
								+ aLastModifiedFile);
					} else
						Log.e(DEBUGTAG, "In table ListCashTables NO records for " + mCashTableName);
					cur.close();
				} else {
					Log.e(DEBUGTAG, "In table ListCashTables NO records");
					cur.close();
				}
			} else
				Log.e(DEBUGTAG, "NO table ListCashTables in database");

			Cursor c = null;
			c = this.mIndexDatabase.rawQuery("SELECT * FROM ListCashTables WHERE name = '" + mCashTableName + "'", null);
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

		public void ClearIndex(){
			Ut.dd("DROP " + mCashTableName);
			this.mIndexDatabase.execSQL("DROP TABLE IF EXISTS '" + mCashTableName + "'");
			this.mIndexDatabase.delete("ListCashTables", "name = '" + mCashTableName + "'", null);
		}

		public void CommitIndex(long aSizeFile, long aLastModifiedFile, int zoomMinInCashFile, int zoomMaxInCashFile) {
			this.mIndexDatabase.delete("ListCashTables", "name = '" + mCashTableName + "'", null);
			final ContentValues cv = new ContentValues();
			cv.put("name", mCashTableName);
			cv.put("lastmodified", aLastModifiedFile);
			cv.put("size", aSizeFile);
			cv.put("minzoom", zoomMinInCashFile);
			cv.put("maxzoom", zoomMaxInCashFile);
			this.mIndexDatabase.insert("ListCashTables", null, cv);
		}

		public void CreateTarIndex(long aSizeFile, long aLastModifiedFile) {
			this.mIndexDatabase.execSQL("DROP TABLE IF EXISTS '" + mCashTableName + "'");
			this.mIndexDatabase.execSQL("CREATE TABLE IF NOT EXISTS '" + mCashTableName + "' (name VARCHAR(100), offset INTEGER NOT NULL, size INTEGER NOT NULL, PRIMARY KEY(name) );");
			this.mIndexDatabase.delete("ListCashTables", "name = '" + mCashTableName + "'", null);
		}

		public void CreateMnmIndex(long aSizeFile, long aLastModifiedFile) {
			this.mIndexDatabase.execSQL("DROP TABLE IF EXISTS '" + mCashTableName + "'");
			this.mIndexDatabase.execSQL("CREATE TABLE IF NOT EXISTS '" + mCashTableName + "' (x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, offset INTEGER NOT NULL, size INTEGER NOT NULL, PRIMARY KEY(x, y, z) );");
			this.mIndexDatabase.delete("ListCashTables", "name = '" + mCashTableName + "'", null);
		}

		public void addTarIndexRow(final String aName, final int aOffset, final int aSize) {
			final ContentValues cv = new ContentValues();
			cv.put("name", aName);
			cv.put("offset", aOffset);
			cv.put("size", aSize);
			this.mIndexDatabase.insert("'" + mCashTableName + "'", null, cv);
		}

		public void addMnmIndexRow(final int aX, final int aY, final int aZ, final long aOffset, final int aSize) {
			final ContentValues cv = new ContentValues();
			cv.put("x", aX);
			cv.put("y", aY);
			cv.put("z", aZ);
			cv.put("offset", aOffset);
			cv.put("size", aSize);
			this.mIndexDatabase.insert("'" + mCashTableName + "'", null, cv);
		}

		public boolean findTarIndex(final String aName, Param4ReadData aData) {
			boolean ret  = false;
			final Cursor c = this.mIndexDatabase.rawQuery("SELECT offset, size FROM '" + mCashTableName + "' WHERE name = '"
					+ aName + ".jpg' OR name = '" + aName + ".png'", null);
			if (c != null) {
				if (c.moveToFirst()) {
					aData.offset = c.getInt(c.getColumnIndexOrThrow("offset"));
					aData.size = c.getInt(c.getColumnIndexOrThrow("size"));
					ret = true;
				}
				c.close();
			}
			return ret;
		}

		public boolean findMnmIndex(final int aX, final int aY, final int aZ, Param4ReadData aData) {
			boolean ret  = false;
			final Cursor c = this.mIndexDatabase.rawQuery("SELECT offset, size FROM '" + mCashTableName + "' WHERE x = " + aX
					+ " AND y = " + aY + " AND z = " + aZ, null);
			if (c != null) {
				if (c.moveToFirst()) {
					aData.offset = c.getInt(c.getColumnIndexOrThrow("offset"));
					aData.size = c.getInt(c.getColumnIndexOrThrow("size"));
					ret = true;
				}
				c.close();
			}
			return ret;
		}

		public void incrementUse(final String aFormattedTileURLString) {
			try {
				final Cursor c = this.mDatabase.rawQuery("UPDATE " + T_FSCACHE + " SET " + T_FSCACHE_USAGECOUNT + " = "
						+ T_FSCACHE_USAGECOUNT + " + 1 , " + T_FSCACHE_TIMESTAMP + " = '" + getNowAsIso8601() + "' WHERE "
						+ T_FSCACHE_NAME + " = '" + aFormattedTileURLString + "'", null);
				c.close();
			} catch (Exception e) {
			}
		}

		public int addTileOrIncrement(final String aFormattedTileURLString, final int aByteFilesize) {
			final Cursor c = this.mDatabase.rawQuery("SELECT * FROM " + T_FSCACHE + " WHERE " + T_FSCACHE_NAME + " = '"
					+ aFormattedTileURLString + "'", null);
			final boolean existed = c.getCount() > 0;
			c.close();
			if (DEBUGMODE)
				Log.d(DEBUGTAG, "Tile existed=" + existed);
			if (existed) {
				incrementUse(aFormattedTileURLString);
				return 0;
			} else {
				insertNewTileInfo(aFormattedTileURLString, aByteFilesize);
				return aByteFilesize;
			}
		}

		private void insertNewTileInfo(final String aFormattedTileURLString, final int aByteFilesize) {
			final ContentValues cv = new ContentValues();
			cv.put(T_FSCACHE_NAME, aFormattedTileURLString);
			cv.put(T_FSCACHE_TIMESTAMP, getNowAsIso8601());
			cv.put(T_FSCACHE_FILESIZE, aByteFilesize);
			this.mDatabase.insert(T_FSCACHE, null, cv);
		}

		private int deleteOldest(final int pSizeNeeded) throws EmptyCacheException {
			final Cursor c = this.mDatabase.rawQuery(T_FSCACHE_SELECT_OLDEST, null);
			final ArrayList<String> deleteFromDB = new ArrayList<String>();
			int sizeGained = 0;
			if (c != null) {
				String fileNameOfDeleted;
				if (c.moveToFirst()) {
					do {
						final int sizeItem = c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_FILESIZE));
						sizeGained += sizeItem;
						fileNameOfDeleted = c.getString(c.getColumnIndexOrThrow(T_FSCACHE_NAME));

						deleteFromDB.add(fileNameOfDeleted);
						this.mCtx.deleteFile(fileNameOfDeleted);

						if (DEBUGMODE)
							Log.i(DEBUGTAG, "Deleted from FS: " + fileNameOfDeleted + " for " + sizeItem + " Bytes");
					} while (c.moveToNext() && sizeGained < pSizeNeeded);
				} else {
					c.close();
					throw new EmptyCacheException("Cache seems to be empty.");
				}
				c.close();

				for (String fn : deleteFromDB)
					this.mDatabase.delete(T_FSCACHE, T_FSCACHE_NAME + "='" + fn + "'", null);
			}
			return sizeGained;
		}

		// ===========================================================
		// Methods
		// ===========================================================
		private String TMP_COLUMN = "tmp";

		public int getCurrentFSCacheByteSize() {
			final Cursor c = this.mDatabase.rawQuery("SELECT SUM(" + T_FSCACHE_FILESIZE + ") AS " + TMP_COLUMN
					+ " FROM " + T_FSCACHE, null);
			final int ret;
			if (c != null) {
				if (c.moveToFirst()) {
					ret = c.getInt(c.getColumnIndexOrThrow(TMP_COLUMN));
				} else {
					ret = 0;
				}
			} else {
				ret = 0;
			}
			c.close();

			return ret;
		}

		/**
		 * Get at the moment within ISO8601 format.
		 *
		 * @return Date and time in ISO8601 format.
		 */
		private String getNowAsIso8601() {
			return DATE_FORMAT_ISO8601.format(new Date(System.currentTimeMillis()));
		}

		// ===========================================================
		// Inner and Anonymous Classes
		// ===========================================================

		private class AndNavDatabaseHelper extends SQLiteOpenHelper {
			AndNavDatabaseHelper(final Context context) {
				super(context, DATABASE_NAME, null, DATABASE_VERSION);
			}

			@Override
			public void onCreate(SQLiteDatabase db) {
				db.execSQL(T_FSCACHE_CREATE_COMMAND);
			}

			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				if (DEBUGMODE)
					Log.w(DEBUGTAG, "Upgrading database from version " + oldVersion + " to " + newVersion
							+ ", which will destroy all old data");

				db.execSQL("DROP TABLE IF EXISTS " + T_FSCACHE);

				onCreate(db);
			}
		}

		@Override
		protected void finalize() throws Throwable {
			Ut.dd("finalize: Close INDEX database");
			if(mIndexDatabase != null)
				mIndexDatabase.close();
			super.finalize();
		}

		public void freeDatabases() {
			if(mDatabase != null)
				if(mDatabase.isOpen())
				{
					mDatabase.close();
					Ut.dd("Close AndNavDatabase");
				}

			if(mIndexDatabase != null)
				if(mIndexDatabase.isOpen())
				{
					mIndexDatabase.close();
					Ut.dd("Close INDEX Database");
				}
		}

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
					db.execSQL("DELETE FROM 'ListCashTables' WHERE name LIKE ('%sqlitedb')");
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public SQLiteDatabase getIndexDatabase() {
		File folder = Ut.getRMapsMainDir(mCtx, "data");
		if(!folder.exists()) // no sdcard // TODO Проверить как это работает без карты? если вытащить карту во время работы программы?
			return null;

		Ut.dd("OpenStreetMapTileFilesystemProvider: Open INDEX database");
		return new IndexDatabaseHelper(mCtx, folder.getAbsolutePath() + "/index.db").getWritableDatabase();
	}

	public void freeDatabases() {
		mDatabase.freeDatabases();
		mUserMapDatabase.freeDatabases();
	}

}
