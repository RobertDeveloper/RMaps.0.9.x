package com.robert.maps.applib.tileprovider;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Message;

import com.robert.maps.applib.utils.SimpleThreadFactory;
import com.robert.maps.applib.utils.Ut;

import org.andnav.osm.views.util.StreamUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TileProviderTAR extends TileProviderFileBase {
	private ExecutorService mThreadPool = Executors.newSingleThreadExecutor(new SimpleThreadFactory("TileProviderTAR"));
	private File mMapFile;
	private String mMapID;
	private ProgressDialog mProgressDialog;
	private boolean mStopIndexing;

	public TileProviderTAR(Context ctx, final String filename, final String mapid, MapTileMemCache aTileCache) {
		super(ctx);
		mTileURLGenerator = new TileURLGeneratorTAR(filename);
		mTileCache = aTileCache == null ? new MapTileMemCache() : aTileCache;
		mMapFile = new File(filename);
		mMapID = mapid;
		
		if(needIndex(mapid, mMapFile.length(), mMapFile.lastModified(), false)) {
			mProgressDialog = new ProgressDialog(ctx);
			mProgressDialog.setTitle("Indexing");
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setMax((int)(mMapFile.length()/1024));
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
			
			CreateTarIndex();
			
			new IndexTask().execute(mMapFile.length(), mMapFile.lastModified());
		}
	}
	
	private void CreateTarIndex() {
		this.mIndexDatabase.execSQL("DROP TABLE IF EXISTS '" + mMapID + "'");
		this.mIndexDatabase.execSQL("CREATE TABLE IF NOT EXISTS '" + mMapID + "' (name VARCHAR(100), offset INTEGER NOT NULL, size INTEGER NOT NULL, PRIMARY KEY(name) );");
		this.mIndexDatabase.delete("ListCashTables", "name = '" + mMapID + "'", null);
	}

	private void addTarIndexRow(String aName, int aOffset, int aSize) {
		final ContentValues cv = new ContentValues();
		cv.put("name", aName);
		cv.put("offset", aOffset);
		cv.put("size", aSize);
		this.mIndexDatabase.insert("'" + mMapID + "'", null, cv);
	}

	private void CommitIndex(long aSizeFile, long aLastModifiedFile, int zoomMinInCashFile, int zoomMaxInCashFile) {
		this.mIndexDatabase.delete("ListCashTables", "name = '" + mMapID + "'", null);
		final ContentValues cv = new ContentValues();
		cv.put("name", mMapID);
		cv.put("lastmodified", aLastModifiedFile);
		cv.put("size", aSizeFile);
		cv.put("minzoom", zoomMinInCashFile);
		cv.put("maxzoom", zoomMaxInCashFile);
		this.mIndexDatabase.insert("ListCashTables", null, cv);
	}

	private boolean findTarIndex(final String aName, Param4ReadData aData) {
		boolean ret  = false;
		final Cursor c = this.mIndexDatabase.rawQuery("SELECT offset, size FROM '" + mMapID + "' WHERE name = '"
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

	public void updateMapParams(TileSource tileSource) {
		tileSource.ZOOM_MINLEVEL = ZoomMinInCashFile(mMapID);
		tileSource.ZOOM_MAXLEVEL = ZoomMaxInCashFile(mMapID);
	}
	
	private class IndexTask extends AsyncTask<Long, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Long... params) {
			try {
				mStopIndexing = false;

				long fileLength = mMapFile.length();
				long fileModified = mMapFile.lastModified();
				int minzoom = 24, maxzoom = 0;

				InputStream in = null;
				in = new BufferedInputStream(new FileInputStream(mMapFile), 8192);
				String name; // 100 name of file
//				int mode; // file mode
//				int uid; // owner user ID
//				int gid; // owner group ID
				int tileSize; // 12 length of file in bytes
//				int mtime; // 12 modify time of file
//				int chksum; // checksum for header
//				byte[] link = new byte[1]; // indicator for links
//				String linkname; // 100 name of linked file
				int offset = 0, skip = 0;

				while (in.available() > 0) {
					name = Ut.readString(in, 100).trim().replace('\\', '/');

//					mode = Integer.decode("0" + Util.readString(in, 8).trim());
//					uid = Integer.decode("0" + Util.readString(in, 8).trim());
//					gid = Integer.decode("0" + Util.readString(in, 8).trim());
					in.skip(24);
					tileSize = Integer.decode("0" + Ut.readString(in, 12).trim());
//					mtime = Integer.decode("0" + Util.readString(in, 12).trim());
//					in.read(link);
//					linkname = Util.readString(in, 100);
					in.skip(12 + 1 + 100);
					in.skip(512 - 100 - 8 - 8 - 8 - 12 - 12 - 1 - 100);
					offset += 512;

					if (tileSize > 0) {
						addTarIndexRow(name, offset, tileSize);
						Ut.d(name);

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
				
				if (!mStopIndexing)
					CommitIndex(fileLength, fileModified, minzoom, maxzoom);

			} catch (Exception e) {
				Ut.e(e.getLocalizedMessage());
				return false;
			}

			return !mStopIndexing;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if(result) 
				Message.obtain(mCallbackHandler, MessageHandlerConstants.MAPTILEFSLOADER_INDEXIND_SUCCESS_ID).sendToTarget();
			
			if(mProgressDialog != null)
				mProgressDialog.dismiss();
		}
	}

	@Override
	public void Free() {
		Ut.d("TileProviderTAR Free");
		mThreadPool.shutdown();
		super.Free();
	}

	private class Param4ReadData {
		public int offset, size;

		Param4ReadData(int offset, int size) {
			this.offset = offset;
			this.size = size;
		}
	}

	public Bitmap getTile(final int x, final int y, final int z) {
		final String tileurl = mTileURLGenerator.Get(x, y, z);
		FileInputStream stream;
		try {
			stream = new FileInputStream(mMapFile);
		} catch (FileNotFoundException e1) {
			return mLoadingMapTile;
		}
		final InputStream in = new BufferedInputStream(stream, 8192);
		
		final Bitmap bmp = mTileCache.getMapTile(tileurl);
		if(bmp != null)
			return bmp;
		
		if (this.mPending.contains(tileurl))
			return super.getTile(x, y, z);
		
		mPending.add(tileurl);
		
		this.mThreadPool.execute(new Runnable() {
			public void run() {
				OutputStream out = null;
				try {
					Param4ReadData Data = new Param4ReadData(0, 0);
					if(findTarIndex(tileurl, Data)) {
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
						final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
						mTileCache.putTile(tileurl, bmp);

						SendMessageSuccess();
					}

				} catch (OutOfMemoryError e) {
					SendMessageFail();
					System.gc();
				} catch (Exception e) {
					SendMessageFail();
				} finally {
					StreamUtils.closeStream(in);
					StreamUtils.closeStream(out);
				}

				mPending.remove(tileurl);
			}
		});
		
		
		return mLoadingMapTile;
	}



}
