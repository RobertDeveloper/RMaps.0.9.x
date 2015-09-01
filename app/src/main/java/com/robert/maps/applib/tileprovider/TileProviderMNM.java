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

public class TileProviderMNM extends TileProviderFileBase {
	private ExecutorService mThreadPool = Executors.newSingleThreadExecutor(new SimpleThreadFactory("TileProviderTAR"));
	private File mMapFile;
	private String mMapID;
	private ProgressDialog mProgressDialog;
	private boolean mStopIndexing;

	public TileProviderMNM(Context ctx, final String filename, final String mapid, MapTileMemCache aTileCache) {
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
			
			CreateIndex();
			
			new IndexTask().execute(mMapFile.length(), mMapFile.lastModified());
		}
	}

	private void CreateIndex() {
		this.mIndexDatabase.execSQL("DROP TABLE IF EXISTS '" + mMapID + "'");
		this.mIndexDatabase.execSQL("CREATE TABLE IF NOT EXISTS '" + mMapID + "' (x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, offset INTEGER NOT NULL, size INTEGER NOT NULL, PRIMARY KEY(x, y, z) );");
		this.mIndexDatabase.delete("ListCashTables", "name = '" + mMapID + "'", null);
	}

	public void addMnmIndexRow(final int aX, final int aY, final int aZ, final long aOffset, final int aSize) {
		final ContentValues cv = new ContentValues();
		cv.put("x", aX);
		cv.put("y", aY);
		cv.put("z", aZ);
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
							addMnmIndexRow(tileX, tileY, tileZ, offset, tileSize);
						} catch (Exception e) {
							break;
						}
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

				if(!mStopIndexing)
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
		Ut.d("TileProviderMNM Free");
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

	public boolean findMnmIndex(final int aX, final int aY, final int aZ, Param4ReadData aData) {
		boolean ret  = false;
		final Cursor c = this.mIndexDatabase.rawQuery("SELECT offset, size FROM '" + mMapID + "' WHERE x = " + aX
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

	public void updateMapParams(TileSource tileSource) {
		tileSource.ZOOM_MINLEVEL = ZoomMinInCashFile(mMapID);
		tileSource.ZOOM_MAXLEVEL = ZoomMaxInCashFile(mMapID);
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
					final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
					if(findMnmIndex(x, y, z, Data)) {
						out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
	
						byte[] tmp = new byte[Data.size];
						in.skip(Data.offset);
						int read = in.read(tmp);
						if (read > 0) {
							out.write(tmp, 0, read);
						}
						out.flush();
	
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
