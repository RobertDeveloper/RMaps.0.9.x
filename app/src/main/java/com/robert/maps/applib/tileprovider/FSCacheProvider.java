package com.robert.maps.applib.tileprovider;

import android.os.Handler;
import android.os.Message;

import com.robert.maps.applib.utils.ICacheProvider;
import com.robert.maps.applib.utils.Ut;

import org.andnav.osm.views.util.StreamUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

public class FSCacheProvider implements ICacheProvider {
	public static final String TILE_PATH_EXTENSION = ".tile";
	public static final long TILE_MAX_CACHE_SIZE_BYTES = 4L * 1024 * 1024;
	public static final long TILE_TRIM_CACHE_SIZE_BYTES = 4L * 1024 * 1024;

	private File mCachePath;
	private long mUsedCacheSpace = 0L;
	private Handler mHandler = null;

	public FSCacheProvider(final File aCachePath) {
		this(aCachePath, null);
	}
	
	public FSCacheProvider(final File aCachePath, final Handler aHandler) {
		super();
		this.mCachePath = aCachePath;
		this.mHandler = aHandler;

		// do this in the background because it takes a long time
		final Thread t = new Thread() {
			@Override
			public void run() {
				mUsedCacheSpace = 0; // because it's static
				calculateDirectorySize(aCachePath);
				if (mUsedCacheSpace > TILE_MAX_CACHE_SIZE_BYTES) {
					cutCurrentCache();
				}
				Ut.d("Finished init thread");
				if(mHandler != null)
					Message.obtain(mHandler).sendToTarget();
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	public long getUsedCacheSpace() {
		return mUsedCacheSpace;
	}

	public byte[] getTile(String aURLstring, int aX, int aY, int aZ) {
		if(mCachePath == null)
			return null;
		
		final File file = new File(mCachePath, Ut.formatToFileName(aURLstring) + TILE_PATH_EXTENSION);
		if(!file.exists())
			return null;
		
		OutputStream out = null;
		InputStream in = null;
		final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
		try {
			in = new BufferedInputStream(new FileInputStream(file), StreamUtils.IO_BUFFER_SIZE);
			out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
			Ut.copy(in, out);
			out.flush();
		} catch (IOException e) {
		} finally {
			StreamUtils.closeStream(in);
			StreamUtils.closeStream(out);
		}

		final byte[] data = dataStream.toByteArray();
		
		return data;
	}

	public void putTile(String aURLstring, int aX, int aY, int aZ, byte[] aData) {
		if(mCachePath == null)
			return;
		
		final File file = new File(mCachePath, Ut.formatToFileName(aURLstring)
				+ TILE_PATH_EXTENSION);

		final File parent = file.getParentFile();
		if (!parent.exists()) {
			return;
		}

		BufferedOutputStream outputStream = null;
		ByteArrayInputStream byteStream = null;
		try {
			byteStream = new ByteArrayInputStream(aData);
			outputStream = new BufferedOutputStream(new FileOutputStream(file.getPath()),
					Ut.IO_BUFFER_SIZE);
			final long length = Ut.copy(byteStream, outputStream);

			mUsedCacheSpace += length;
			if (mUsedCacheSpace > TILE_MAX_CACHE_SIZE_BYTES) {
				cutCurrentCache();
			}
		} catch (final IOException e) {
			e.printStackTrace();
			
			return;
		} finally {
			StreamUtils.closeStream(outputStream);
			StreamUtils.closeStream(byteStream);
		}
		
	}

	public void Free() {
		// TODO Auto-generated method stub
		
	}
	
	public void clearCache() {
		cutCurrentCacheToSize(0L);
	}

	private void cutCurrentCache() {
		cutCurrentCacheToSize(TILE_TRIM_CACHE_SIZE_BYTES);
	}

	private void cutCurrentCacheToSize(final long aTrimSizeBytes) {

		synchronized (mCachePath) {

			if (mUsedCacheSpace > aTrimSizeBytes) {

				Ut.d("Trimming tile cache from " + mUsedCacheSpace + " to "
						+ aTrimSizeBytes);

				final List<File> z = getDirectoryFileList(mCachePath);

				final File[] files = z.toArray(new File[0]);
				Arrays.sort(files, new Comparator<File>() {
					public int compare(final File f1, final File f2) {
						return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
					}
				});

				for (final File file : files) {
					if (mUsedCacheSpace <= aTrimSizeBytes) {
						break;
					}

					final long length = file.length();
					if (file.delete()) {
						mUsedCacheSpace -= length;
					}
				}

				Ut.d("Finished trimming tile cache");
			}
		}
	}

	private List<File> getDirectoryFileList(final File aDirectory) {
		final List<File> files = new ArrayList<File>();

		final File[] z = aDirectory.listFiles();
		if (z != null) {
			for (final File file : z) {
				if (file.isFile()) {
					files.add(file);
				}
				if (file.isDirectory()) {
					files.addAll(getDirectoryFileList(file));
				}
			}
		}

		return files;
	}

	private void calculateDirectorySize(final File pDirectory) {
		if(pDirectory == null)
			return;
		
		final File[] z = pDirectory.listFiles();
		if (z != null) {
			for (final File file : z) {
				if (file.isFile()) {
					mUsedCacheSpace += file.length();
				}
				if (file.isDirectory() && !isSymbolicDirectoryLink(pDirectory, file)) {
					calculateDirectorySize(file); // *** recurse ***
				}
			}
		}
	}

	private boolean isSymbolicDirectoryLink(final File pParentDirectory, final File pDirectory) {
		try {
			final String canonicalParentPath1 = pParentDirectory.getCanonicalPath();
			final String canonicalParentPath2 = pDirectory.getCanonicalFile().getParent();
			return !canonicalParentPath1.equals(canonicalParentPath2);
		} catch (final IOException e) {
			return true;
		} catch (final NoSuchElementException e) {
			// See: http://code.google.com/p/android/issues/detail?id=4961
			// See: http://code.google.com/p/android/issues/detail?id=5807
			return true;
		}

	}

	public double getTileLenght() {
		return 0;
	}

	@Override
	public void deleteTile(String aURLstring, int aX, int aY, int aZ) {
		// TODO Auto-generated method stub
		
	}

	
}
