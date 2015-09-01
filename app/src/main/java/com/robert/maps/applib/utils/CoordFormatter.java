package com.robert.maps.applib.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Locale;
import java.util.StringTokenizer;

public class CoordFormatter {
	private int mFormat = 0;
	public static final String HINT0 = "-00.00000";
	public static final String HINT1 = "-00 00.0000";
	public static final String HINT2 = "-00 00 00.00";

	public CoordFormatter(int format) {
		super();
		mFormat = format;
	}
	
	public CoordFormatter(Context ctx) {
		super();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		mFormat = Integer.valueOf(pref.getString("pref_coordformat", "0"));
	}
	
	public String getHint() {
		if(mFormat == 0 || mFormat == 3)
			return HINT0;
		else if(mFormat == 1 || mFormat == 4)
			return HINT1;
		else if(mFormat == 2 || mFormat == 5)
			return HINT2;
		else
			return "";
	}
	
	public String convertLat(double coord) {
		return convert(coord, 'S', 'N');
	}
	
	public String convertLon(double coord) {
		return convert(coord, 'W', 'E');
	}
	
	private String convert(double coord, char minus, char plus) {
		String ret = "";
		
		if(mFormat == 1) {
			ret = String.format(Locale.UK, "%s%d°%.4f'", coord<0?"-":"", Math.abs((int)coord), Math.abs((coord-(int)coord)*60.0));
		} else if(mFormat == 2) {
			final double min = (coord-(int)coord)*60.0;
			ret = String.format(Locale.UK, "%s%d°%d'%.2f\"", coord<0?"-":"", Math.abs((int)coord), Math.abs((int)min), Math.abs((min-(int)min)*60.0));
		} else if(mFormat == 3) {
			ret = String.format(Locale.UK, "%s%.5f°", coord<0?minus:plus, Math.abs(coord));
		} else if(mFormat == 4) {
			ret = String.format(Locale.UK, "%s%d°%.4f'", coord<0?minus:plus, Math.abs((int)coord), Math.abs((coord-(int)coord)*60.0));
		} else if(mFormat == 5) {
			final double min = (coord-(int)coord)*60.0;
			ret = String.format(Locale.UK, "%s%d°%d'%.2f\"", coord<0?minus:plus, Math.abs((int)coord), Math.abs((int)min), Math.abs((min-(int)min)*60.0));
		} else {
			ret = String.format(Locale.UK, "%.5f°", coord);
		} 
		
		return ret;
	}

	public static double convert(String coordinate) {
		try {
			return convertTrowable(coordinate);
		} catch (Exception e) {
			return 0.0;
		}
	}

	public static double convertTrowable(String coordinate) {
	    // IllegalArgumentException if bad syntax
	    if (coordinate == null) {
	        throw new NullPointerException("coordinate");
	    }
	    
	    coordinate = coordinate.replace(',', '.').replace('°', ' ').replace('\'', ' ').replace('"', ' ').trim();

	    boolean negative = false;
	    final char sign = coordinate.charAt(0);
	    if (sign == '-' || sign == 'S' || sign == 's' || sign == 'W' || sign == 'w') {
	        coordinate = coordinate.substring(1).trim();
	        negative = true;
	    } else {
	    	coordinate = coordinate.replace('+', ' ').replace('N', ' ').replace('n', ' ').replace('E', ' ').replace('e', ' ').trim();
	    }
	    while(coordinate.indexOf("  ") >= 0) {
	    	coordinate = coordinate.replace("  ", " ");
	    }
	    coordinate = coordinate.replace(' ', ':').replace(',', '.');

	    StringTokenizer st = new StringTokenizer(coordinate, ":");
	    int tokens = st.countTokens();
	    if (tokens < 1) {
	        throw new IllegalArgumentException("coordinate=" + coordinate);
	    }
	    try {
	        String degrees = st.nextToken();
	        double val;
	        if (tokens == 1) {
	            val = Double.parseDouble(degrees);
		        boolean isNegative180 = negative && ((int)val*1E6 == 180*1E6);

				// deg must be in [0, 179] except for the case of -180 degrees
				if ((val < 0.0) || (val > 180.0 && !isNegative180)) {
					throw new IllegalArgumentException("coordinate=" + coordinate);
				}

	            val = negative ? -val : val;
				
				return val;
			}

	        String minutes = st.nextToken();
	        int deg = Integer.parseInt(degrees);
	        double min;
	        double sec = 0.0;

	        if (st.hasMoreTokens()) {
	            min = Integer.parseInt(minutes);
	            String seconds = st.nextToken();
	            sec = Double.parseDouble(seconds);
	        } else {
	            min = Double.parseDouble(minutes);
	        }

	        boolean isNegative180 = negative && (deg == 180) &&
	            (min == 0) && (sec == 0);

	        // deg must be in [0, 179] except for the case of -180 degrees
	        if ((deg < 0.0) || (deg > 180 && !isNegative180)) {
	            throw new IllegalArgumentException("coordinate=" + coordinate);
	        }
	        if (min < 0 || min > 60) {
	            throw new IllegalArgumentException("coordinate=" +
	                    coordinate);
	        }
	        if (sec < 0 || sec > 60) {
	            throw new IllegalArgumentException("coordinate=" +
	                    coordinate);
	        }

	        val = deg*3600.0 + min*60.0 + sec;
	        val /= 3600.0;
	        return negative ? -val : val;
	    } catch (NumberFormatException nfe) {
	        throw new IllegalArgumentException("coordinate=" + coordinate);
	    }
	}
}
