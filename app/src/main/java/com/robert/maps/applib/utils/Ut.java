package com.robert.maps.applib.utils;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.robert.maps.R;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Ut implements OpenStreetMapConstants, OpenStreetMapViewConstants {
	public static final int MAPTILEFSLOADER_SUCCESS_ID = 1000;
	public static final int MAPTILEFSLOADER_FAIL_ID = MAPTILEFSLOADER_SUCCESS_ID + 1;
	public static final int INDEXIND_SUCCESS_ID = MAPTILEFSLOADER_SUCCESS_ID + 2;
	public static final int INDEXIND_FAIL_ID = MAPTILEFSLOADER_SUCCESS_ID + 3;
	public static final int ERROR_MESSAGE = MAPTILEFSLOADER_SUCCESS_ID + 4;
	public static final int SEARCH_OK_MESSAGE = MAPTILEFSLOADER_SUCCESS_ID + 5;

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
		final String str = aTileURLString.substring(7).replace("/", "_").replace("?", "_");
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

	@SuppressLint("SimpleDateFormat")
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
			pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			res = pi.versionName;
		} catch (NameNotFoundException e) {
		}

		return res;
	}

	public static String getPackVersion(Context ctx) {
		return ctx.getPackageName().endsWith("ext") ? "Ext" : "Free";
	}
	
	public static void toLogFile(String str) {
//	    PrintWriter pw;
//		try {
//			pw = new PrintWriter(new File(Environment.getExternalStorageDirectory().getPath()+"/rmaps.log"));
//			pw.println(str);
//			pw.flush();
//			pw.close();
//		} catch (FileNotFoundException e) {
//		}     
		
	}

	public static void dd(String str){
		Log.d(DEBUGTAG, str);
	}

	public static void e(String str){
		if(DEBUGMODE)
			Log.e(DEBUGTAG, str);
		toLogFile(str);
	}
	public static void i(String str){
		if(DEBUGMODE)
			Log.i(DEBUGTAG, str);
		toLogFile(str);
	}
	public static void w(String str){
		if(DEBUGMODE)
			Log.w(DEBUGTAG, str);
		toLogFile(str);
	}

	public static void d(String str){
		if(DEBUGMODE)
			Log.d(DEBUGTAG, str);
		toLogFile(str);
	}

	public static String FileName2ID(String name) {
		return name.replace(".", "_").replace(" ", "_").replace("-", "_").trim();
	}

	private static File getDir(final Context mCtx, final String aPref, final String aDefaultDirName, final String aFolderName) {
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mCtx);
		final String dirName = pref.getString(aPref, aDefaultDirName)+"/"+aFolderName+"/";

		final File dir = new File(dirName.replace("//", "/").replace("//", "/"));
		if(!dir.exists()){
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				dir.mkdirs();
			}
		}

		return dir;
	}
	
	private static String EXTERNAL_SD = "/storage/extSdCard";
	private static String SIGNAL_FILE_NAME = "/RMapsOnSDCard";
	
	public static String getExternalStorageDirectory() {
		final File signalFile = new File(Environment.getExternalStorageDirectory().getPath()+SIGNAL_FILE_NAME);
		if(signalFile.exists())
			return (EXTERNAL_SD);
		else
			return Environment.getExternalStorageDirectory().getPath();
	}

	public static File getRMapsMainDir(final Context mCtx, final String aFolderName) {
		return getDir(mCtx, "pref_dir_main", Ut.getExternalStorageDirectory()+"/rmaps/", aFolderName);
	}

	public static File getRMapsMapsDir(final Context mCtx) {
		return getDir(mCtx, "pref_dir_maps", getRMapsMainDir(mCtx, "maps").getAbsolutePath(), "");
	}

	public static File getRMapsImportDir(final Context mCtx) {
		return getDir(mCtx, "pref_dir_import", getRMapsMainDir(mCtx, "import").getAbsolutePath(), "");
	}

	public static File getRMapsExportDir(final Context mCtx) {
		return getDir(mCtx, "pref_dir_export", getRMapsMainDir(mCtx, "export").getAbsolutePath(), "");
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

	public static String formatGeoPoint(GeoPoint point, Context ctx){
		final CoordFormatter cf = new CoordFormatter(ctx);
		return cf.convertLat(point.getLatitude())+", "+cf.convertLon(point.getLongitude());
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


	public static class Algorithm
	{
	 
	    /**
	     * @param args the command line arguments
	     */
	    private static int RIGHT = 2;
	    private static int TOP = 8;
	    private static int BOTTOM = 4;
	    private static int LEFT = 1;
	 
	    public static int ComputeOutCode (int x, int y, int xmin, int ymin, int xmax, int ymax)
	    {
	        int code = 0;
	        if (y > ymax)
	            code |= TOP;
	        else if (y < ymin)
	            code |= BOTTOM;
	        if (x > xmax)
	            code |= RIGHT;
	        else if (x < xmin)
	            code |= LEFT;
	        return code;
	    }
	 
	    public static boolean cohenSutherland( int x1, int y1, int x2, int y2, int xmin, int ymin, int xmax, int ymax)
	    {
	        //Outcodes for P0, P1, and whatever point lies outside the clip rectangle
	        int outcode0, outcode1, outcodeOut, hhh = 0;
	        boolean accept = false, done = false;
	 
	        //compute outcodes
	        outcode0 = ComputeOutCode (x1, y1, xmin, ymin, xmax, ymax);
	        outcode1 = ComputeOutCode (x2, y2, xmin, ymin, xmax, ymax);
	 
	        //System.out.println( outcode0 + " " + outcode1 );
	 
	        do{
	            if ((outcode0 | outcode1) == 0 )
	            {
	                accept = true;
	                done = true;
	            }
	            else if ( (outcode0 & outcode1) > 0 )
	            {
	                done = true;
	            }
	 
	            else
	            {
	                //failed both tests, so calculate the line segment to clip
	                //from an outside point to an intersection with clip edge
	                int x = 0, y = 0;
	                //At least one endpoint is outside the clip rectangle; pick it.
	                outcodeOut = outcode0 != 0 ? outcode0: outcode1;
	                //Now find the intersection point;
	                //use formulas y = y0 + slope * (x - x0), x = x0 + (1/slope)* (y - y0)
	                if ( (outcodeOut & TOP) > 0 )
	                {
	                    x = x1 + (x2 - x1) * (ymax - y1)/(y2 - y1);
	                    y = ymax;
	                }
	                else if ((outcodeOut & BOTTOM) > 0 )
	                {
	                    x = x1 + (x2 - x1) * (ymin - y1)/(y2 - y1);
	                    y = ymin;
	                }
	                else if ((outcodeOut & RIGHT)> 0)
	                {
	                    y = y1 + (y2 - y1) * (xmax - x1)/(x2 - x1);
	                    x = xmax;
	                }
	                else if ((outcodeOut & LEFT) > 0)
	                {
	                    y = y1 + (y2 - y1) * (xmin - x1)/(x2 - x1);
	                    x = xmin;
	                }
	                //Now we move outside point to intersection point to clip
	                //and get ready for next pass.
	                if (outcodeOut == outcode0)
	                {
	                    x1 = x;
	                    y1 = y;
	                    outcode0 = ComputeOutCode (x1, y1, xmin, ymin, xmax, ymax);
	                }
	                else
	                {
	                    x2 = x;
	                    y2 = y;
	                    outcode1 = ComputeOutCode (x2, y2, xmin, ymin, xmax, ymax);
	                }
	            }
	            hhh ++;
	        }
	        while (done  != true && hhh < 5000);
	 
//	        if(accept)
//	        {
//	            set( x1, y1, x2, y2);
//	        }
	        
	        return accept;
	    }
	    
	    public static boolean isIntersected(int left, int top, int right, int bottom, float arr[]) {
	    	boolean ret = false;
	    	
	    	for(int i = 0; i < 8; i = i + 2) {
	    		ret = cohenSutherland((int)arr[i], (int)arr[i+1], (int)arr[i+2], (int)arr[i+3], left, top, right, bottom);
	    		if(ret) break;
	    	}
	    	
	    	return ret;
	    }
	 
	 
	}

	public static String formatDistance(Context ctx, float dist, int units) {
		final String[] str = formatDistance2(ctx, dist, units);
		return str[0]+" "+str[1];
	}

	public static String[] formatDistance2(Context ctx, float dist, int units) {
		final String[] str = new String[2];
		if(units == 0) {
			if(dist < 1000) {
				str[0] = String.format("%.0f", dist);
				str[1] = ctx.getResources().getString(R.string.m);
			} else if(dist/1000 < 100) {
				str[0] = String.format("%.1f", dist/1000);
				str[1] = ctx.getResources().getString(R.string.km);
			} else {
				str[0] = String.format("%.0f", dist/1000);
				str[1] = ctx.getResources().getString(R.string.km);
			}
		} else {
			if(dist < 5280) {
				str[0] = String.format("%.0f", dist);
				str[1] = ctx.getResources().getString(R.string.ft);
			} else if(dist/5280 < 100) {
				str[0] = String.format("%.1f", dist/5280);
				str[1] = ctx.getResources().getString(R.string.ml);
			} else {
				str[0] = String.format("%.0f", dist/5280);
				str[1] = ctx.getResources().getString(R.string.ml);
			}
		}
		return str;
	}
	
	public static CharSequence formatTime(long time) {
		return String.format("%tM:%tM:%tS", time / 60, time, time);
	}
	
	public static String formatSize(double size) {
		if(size / 1024 > 1024)
			return String.format("%.1f MB", size / 1024 / 1024);
		else
			return String.format("%.1f KB", size / 1024);
				
	}

	public static String loadStringFromResourceFile(Context ctx, int id) {
        try {           
             Resources res = ctx.getResources();
             InputStream fis = res.openRawResource(id);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            fis.close();
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    public static void appendLog(final String fileName, final String text)
    {       
       final File logFile = new File(fileName);
       if (!logFile.exists())
       {
          try
          {
             logFile.createNewFile();
          } 
          catch (IOException e)
          {
             e.printStackTrace();
          }
       }
       try
       {
          //BufferedWriter for performance, true to set append to file flag
          BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
          buf.append(text);
          buf.newLine();
          buf.close();
       }
       catch (IOException e)
       {
          e.printStackTrace();
       }
    }

}
