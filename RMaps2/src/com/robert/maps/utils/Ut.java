package com.robert.maps.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.robert.maps.R;

public class Ut implements OpenStreetMapConstants, OpenStreetMapViewConstants {
	public static final int MAPTILEFSLOADER_SUCCESS_ID = 1000;
	public static final int MAPTILEFSLOADER_FAIL_ID = MAPTILEFSLOADER_SUCCESS_ID + 1;
	public static final int INDEXIND_SUCCESS_ID = MAPTILEFSLOADER_SUCCESS_ID + 2;
	public static final int INDEXIND_FAIL_ID = MAPTILEFSLOADER_SUCCESS_ID + 3;
	public static final int ERROR_MESSAGE = MAPTILEFSLOADER_SUCCESS_ID + 4;

	public static final int IO_BUFFER_SIZE = 8 * 1024;
	
	public static long copy(final InputStream in, final OutputStream out) throws IOException {
		long length = 0;
		final byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
			length += read;
		}
		return length;
	}

	public static String formatToFileName(final String aTileURLString) {
		final String str = aTileURLString.substring(7).replace("/", "_");
		if (str.length() > 255) {
			return str.substring(str.length() - 255);
		} else
			return str;
	}

	final static String[] formats = new String[] { 
			"yyyy-MM-dd'T'HH:mm:ss.SSSZ",
			"yyyy-MM-dd'T'HH:mm:ssZ",
			"yyyy-MM-dd'T'HH:mmZ",
			"yyyy-MM-dd'T'HH:mm:ss'Z'",
			"yyyy-MM-dd HH:mm:ss.SSSZ",
			"yyyy-MM-dd HH:mmZ", 
			"yyyy-MM-dd HH:mm",
			"yyyy-MM-dd", 
			};

	public static Date ParseDate(final String str){
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date(0);
		for (String format : formats) {
			sdf.applyPattern(format);

			try {
				date = sdf.parse(str);
				break;
			} catch (ParseException e) {
			}
		}

		return date;
	}
	
	public static boolean equalsIgnoreCase(String string, int start, int end, String string2){
		try {
			return string.substring(start, end).equalsIgnoreCase(string2);
		} catch (Exception e) {
			return false;
		}
	}

	public static ProgressDialog ShowWaitDialog(final Context mCtx) {
		return ShowWaitDialog(mCtx, 0);
	}

	public static ProgressDialog ShowWaitDialog(final Context mCtx, final int ResourceId) {
		final ProgressDialog dialog = new ProgressDialog(mCtx);
		dialog.setMessage(mCtx.getString(ResourceId == 0 ? R.string.message_wait : ResourceId));
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);

		dialog.show();

		return dialog;
	}

	public static String getAppVersion(Context ctx) {
		PackageInfo pi;
		String res = "";
		try {
			pi = ctx.getPackageManager().getPackageInfo("com.robert.maps", 0);
			res = pi.versionName;
		} catch (NameNotFoundException e) {
		}

		return res;
	}

	public static void dd(String str){
		Log.d(DEBUGTAG, str);
	}

	public static void e(String str){
		if(DEBUGMODE)
			Log.e(DEBUGTAG, str);
	}
	public static void i(String str){
		if(DEBUGMODE)
			Log.i(DEBUGTAG, str);
	}
	public static void w(String str){
		if(DEBUGMODE)
			Log.w(DEBUGTAG, str);
	}

	public static void d(String str){
		if(DEBUGMODE)
			Log.d(DEBUGTAG, str);
	}

	public static String FileName2ID(String name) {
		return name.replace(".", "_").replace(" ", "_").replace("-", "_").trim();
	}

	private static File getDir(final Context mCtx, final String aPref, final String aDefaultDirName, final String aFolderName) {
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mCtx);
		final String dirName = pref.getString(aPref, aDefaultDirName)+"/"+aFolderName+"/";

		final File dir = new File(dirName.replace("//", "/").replace("//", "/"));
		if(!dir.exists()){
			if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
				dir.mkdirs();
			}
		}

		return dir;
	}
	
	private static String EXTERNAL_SD = "/external_sd";
	public static String getExternalStorageDirectory() {
		final File dir = new File(Environment.getExternalStorageDirectory().getPath()+EXTERNAL_SD);
		if(dir.exists())
			return (Environment.getExternalStorageDirectory().getPath()+EXTERNAL_SD);
		else
			return Environment.getExternalStorageDirectory().getPath();
	}

	public static File getRMapsMainDir(final Context mCtx, final String aFolderName) {
		return getDir(mCtx, "pref_dir_main", Ut.getExternalStorageDirectory()+"/rmaps/", aFolderName);
	}

	public static File getRMapsMapsDir(final Context mCtx) {
		return getRMapsMainDir(mCtx, "maps");
	}

	public static File getRMapsImportDir(final Context mCtx) {
		return getRMapsMainDir(mCtx, "import");
	}

	public static File getRMapsExportDir(final Context mCtx) {
		return getRMapsMainDir(mCtx, "export");
	}

	public static File getRMapsCacheTilesDir(final Context mCtx) {
		return getRMapsMainDir(mCtx, "cache/tiles");
	}

	public static String readString(final InputStream in, final int size) throws IOException{
		byte b [] = new byte[size];

		int lenght = in.read(b);
		if(b[0] == 0)
			return "";
		else if(lenght > 0)
			return new String(b, 0, lenght);
		else
			return "";
	}

	public static String formatGeoPoint(GeoPoint point){
		return point.toDoubleString();
	}

	public static CharSequence formatGeoCoord(double double1) {
		return new StringBuilder().append(double1).toString();
	}

	public static int readInt(final InputStream in) throws IOException{
		int res = 0;
		byte b [] = new byte[4];

		if(in.read(b)>0)
			res = (((int)(b[0] & 0xFF)) << 24) +
			  + ((b[1] & 0xFF) << 16) +
			  + ((b[2] & 0xFF) << 8) +
			  + (b[3] & 0xFF);

		return res;
	}

	public static class TextWriter {
		private String mText;
		private int mMaxWidth;
		private int mMaxHeight;
		private int mTextSize;
		private Paint mPaint;
		private String[] mLines;

		public TextWriter(int aMaxWidth, int aTextSize, String aText) {
			mMaxWidth = aMaxWidth;
			mTextSize = aTextSize;
			mText = aText;
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			//mPaint.setTypeface(Typeface.create((Typeface)null, Typeface.BOLD));

			final float[] widths = new float[mText.length()];
			this.mPaint.setTextSize(mTextSize);
			this.mPaint.getTextWidths(mText, widths);

			final StringBuilder sb = new StringBuilder();
			int maxWidth = 0;
			int curLineWidth = 0;
			int lastStop = 0;
			int i;
			int lastwhitespace = 0;
			/*
			 * Loop through the charwidth array and harshly insert a linebreak, when the width gets bigger than
			 * DESCRIPTION_MAXWIDTH.
			 */
			for (i = 0; i < widths.length; i++) {
				if (!Character.isLetter(mText.charAt(i)) && mText.charAt(i) != ',')
					lastwhitespace = i;

				float charwidth = widths[i];

				if (curLineWidth + charwidth > mMaxWidth) {
					if (lastStop == lastwhitespace)
						i--;
					else
						i = lastwhitespace;

					sb.append(mText.subSequence(lastStop, i));
					sb.append('\n');

					lastStop = i;
					maxWidth = Math.max(maxWidth, curLineWidth);
					curLineWidth = 0;
				}

				curLineWidth += charwidth;
			}
			/* Add the last line to the rest to the buffer. */
			if (i != lastStop) {
				final String rest = mText.substring(lastStop, i);

				maxWidth = Math.max(maxWidth, (int) this.mPaint.measureText(rest));

				sb.append(rest);
			}
			mLines = sb.toString().split("\n");

			mMaxWidth = maxWidth;
			mMaxHeight = mLines.length * mTextSize;
		}

		public void Draw(final Canvas c, final int x, final int y) {
			for (int j = 0; j < mLines.length; j++) {
				c.drawText(mLines[j].trim(), x, y + mTextSize * (j+1), mPaint);
			}
		}

		public int getWidth() {
			return mMaxWidth;
		}

		public int getHeight() {
			return mMaxHeight;
		}
	}

	public static Intent SendMail(String subject, String text) {
		final String[] email = {"robertk506@gmail.com"};
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, text);
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
		sendIntent.putExtra(Intent.EXTRA_EMAIL, email);
		sendIntent.setType("message/rfc822");
		return Intent.createChooser(sendIntent, "Error report to the author");
	}

}
