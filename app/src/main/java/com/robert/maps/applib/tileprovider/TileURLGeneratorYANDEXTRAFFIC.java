package com.robert.maps.applib.tileprovider;

import android.os.Handler;
import android.os.Message;

import com.robert.maps.applib.utils.SimpleThreadFactory;

import org.andnav.osm.views.util.StreamUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TileURLGeneratorYANDEXTRAFFIC extends TileURLGeneratorBase {
	private static final String YANDEX_STAT_URL = "http://jgo.maps.yandex.net/trf/stat.js";
	private Long mLastUpdateTime;
	private String mTimeStamp;
	private ExecutorService mThreadPool = Executors.newSingleThreadExecutor(new SimpleThreadFactory("TileURLGeneratorYANDEXTRAFFIC"));
	private Handler mCallbackHandler = null;

	public TileURLGeneratorYANDEXTRAFFIC(String mName) {
		super(mName);
		mLastUpdateTime = 0L;
		mTimeStamp = "";
	}

	@Override
	public String Get(int x, int y, int z) {
		return new StringBuilder().append(mName)
		.append("&x=")
		.append(x)
		.append("&y=")
		.append(y)
		.append("&z=")
		.append(z)
		.append("&tm=")
		.append(get_ts(60))
		.toString();
	}
	
	public void setCallbackHandler(final Handler aCallbackHandler) {
		mCallbackHandler = aCallbackHandler;
	}
	
	public void Free() {
		mCallbackHandler = null;
		mThreadPool.shutdown();
	}
	
	private boolean ts_update_needed(int delta){
		Date d = new Date();
		Long now = d.getTime();
		if(now - mLastUpdateTime > delta*1000){
			mLastUpdateTime = now;
			return true;
		}
		return false;
	}

	private String get_ts(int delta){

		if (ts_update_needed(delta)) {
			mThreadPool.execute(new Runnable() {
				
				public void run() {
					InputStream in = null;
					OutputStream out = null;

					try {
						in = new BufferedInputStream(new URL(YANDEX_STAT_URL).openStream(), StreamUtils.IO_BUFFER_SIZE);

						final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
						out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
						StreamUtils.copy(in, out);
						out.flush();

						String str = dataStream.toString();
						//JSONObject json = new JSONObject(str.replace("YMaps.TrafficLoader.onLoad(\"stat\",", "").replace("});", "}"));
						int start = str.indexOf("timestamp:");
						start = str.indexOf("\"", start) + 1;
						int end = str.indexOf("\"", start);
						mTimeStamp = str.substring(start, end);

					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						StreamUtils.closeStream(in);
						StreamUtils.closeStream(out);
					}
					
					if(mCallbackHandler != null)
						Message.obtain(mCallbackHandler, MessageHandlerConstants.MAPTILEFSLOADER_SUCCESS_ID).sendToTarget();
				}
			});
		}

		return mTimeStamp;
	}
}
