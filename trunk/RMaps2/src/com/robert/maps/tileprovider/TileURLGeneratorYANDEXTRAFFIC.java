package com.robert.maps.tileprovider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;

import org.andnav.osm.views.util.StreamUtils;

public class TileURLGeneratorYANDEXTRAFFIC extends TileURLGeneratorBase {
	private Long mLastUpdateTime;
	private String mTimeStamp;

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
			InputStream in = null;
			OutputStream out = null;

			try {
				in = new BufferedInputStream(new URL("http://jgo.maps.yandex.net/trf/stat.js").openStream(), StreamUtils.IO_BUFFER_SIZE);

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
		}

		return mTimeStamp;
	}
}
