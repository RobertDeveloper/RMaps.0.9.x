// Created by plusminus on 21:28:12 - 25.09.2008
package org.andnav.osm.util;

import java.util.Locale;

import org.andnav.osm.util.constants.GeoConstants;
import org.andnav.osm.views.util.constants.MathConstants;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class GeoPoint implements MathConstants, GeoConstants{
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private int mLongitudeE6;
	private int mLatitudeE6;

	// ===========================================================
	// Constructors
	// ===========================================================

	public GeoPoint(final int aLatitudeE6, final int aLongitudeE6) {
		this.mLatitudeE6 = aLatitudeE6;
		this.mLongitudeE6 = aLongitudeE6;
	}

	protected static GeoPoint fromDoubleString(final String s, final char spacer) {
		final int spacerPos = s.indexOf(spacer);
		return new GeoPoint((int) (Double.parseDouble(s.substring(0,
				spacerPos - 1)) * 1E6), (int) (Double.parseDouble(s.substring(
				spacerPos + 1, s.length())) * 1E6));
	}

	public static GeoPoint fromDoubleString(final String s){
		//final int commaPos = s.indexOf(',');
		final String[] f = s.split(",");
		return new GeoPoint((int)(Double.parseDouble(f[0])* 1E6),
				(int)(Double.parseDouble(f[1])* 1E6));
		//return new GeoPoint((int)(Double.parseDouble(s.substring(0,commaPos-1))* 1E6),
		//		(int)(Double.parseDouble(s.substring(commaPos+1,s.length()))* 1E6));
	}

	public static GeoPoint from2DoubleString(final String lat, final String lon) {
		try {
			return new GeoPoint((int) (Double.parseDouble(lat) * 1E6),
					(int) (Double.parseDouble(lon) * 1E6));
		} catch (NumberFormatException e) {
			return new GeoPoint(0,0);
		}
	}

	public static GeoPoint fromIntString(final String s){
		final String word[] = s.split(",");
		int lat = 0, lon = 0;
		try {
			lat = Integer.parseInt(word[0]);
		} catch (Exception e) {
		}
		try {
			lon = Integer.parseInt(word[1]);
		} catch (Exception e) {
		}
		return new GeoPoint(lat, lon);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public int getLongitudeE6() {
		return this.mLongitudeE6;
	}

	public int getLatitudeE6() {
		return this.mLatitudeE6;
	}

	public double getLongitude() {
		return this.mLongitudeE6/1E6;
	}

	public double getLatitude() {
		return this.mLatitudeE6/1E6;
	}

	public void setLongitudeE6(final int aLongitudeE6) {
		this.mLongitudeE6 = aLongitudeE6;
	}

	public void setLatitudeE6(final int aLatitudeE6) {
		this.mLatitudeE6 = aLatitudeE6;
	}

	public void setCoordsE6(final int aLatitudeE6, final int aLongitudeE6) {
		this.mLatitudeE6 = aLatitudeE6;
		this.mLongitudeE6 = aLongitudeE6;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	public String toString(){
		return new StringBuilder().append(this.mLatitudeE6).append(",").append(this.mLongitudeE6).toString();
	}

	public String toDoubleString() {
		return String.format(Locale.UK, "%f,%f", this.mLatitudeE6 / 1E6, this.mLongitudeE6  / 1E6);
		//return new StringBuilder().append(this.mLatitudeE6 / 1E6).append(",").append(this.mLongitudeE6  / 1E6).toString();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof GeoPoint))
			return false;
		GeoPoint g = (GeoPoint)obj;
		return g.mLatitudeE6 == this.mLatitudeE6 && g.mLongitudeE6 == this.mLongitudeE6;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * @see Source@ http://www.geocities.com/DrChengalva/GPSDistance.html
	 * @param gpA
	 * @param gpB
	 * @return distance in meters
	 */
	public int distanceTo(final GeoPoint other) {

		final double a1 = DEG2RAD * (this.mLatitudeE6 / 1E6);
		final double a2 = DEG2RAD * (this.mLongitudeE6 / 1E6);
		final double b1 = DEG2RAD * (other.mLatitudeE6 / 1E6);
		final double b2 = DEG2RAD * (other.mLongitudeE6 / 1E6);

		final double cosa1 = Math.cos(a1);
		final double cosb1 = Math.cos(b1);

		final double t1 = cosa1*Math.cos(a2)*cosb1*Math.cos(b2);

		final double t2 = cosa1*Math.sin(a2)*cosb1*Math.sin(b2);

		final double t3 = Math.sin(a1)*Math.sin(b1);

		final double tt = Math.acos( t1 + t2 + t3 );

		return (int)(RADIUS_EARTH_METERS*tt);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================


	static private final double PiOver180 = Math.PI / 180.0;
	
	static public double toRadians(double degrees) {
		return degrees * PiOver180;
	}
	
	static public double toDegrees(double radians) {
		return radians / PiOver180;
	}

	public GeoPoint calculateEndingGlobalCoordinates(GeoPoint start,
			double startBearing, double distance /*, double[] endBearing*/) {

		double mSemiMajorAxis = 6378137.0;
		double mSemiMinorAxis = (1.0 - 1.0 / 298.257223563) * 6378137.0;
		double mFlattening = 1.0 / 298.257223563;
		//double mInverseFlattening = 298.257223563;

	    double a = mSemiMajorAxis;
		double b = mSemiMinorAxis;
		double aSquared = a * a;
		double bSquared = b * b;
		double f = mFlattening;
		double phi1 = toRadians(start.getLatitude());
		double alpha1 = toRadians(startBearing);
		double cosAlpha1 = Math.cos(alpha1);
		double sinAlpha1 = Math.sin(alpha1);
		double s = distance;
		double tanU1 = (1.0 - f) * Math.tan(phi1);
		double cosU1 = 1.0 / Math.sqrt(1.0 + tanU1 * tanU1);
		double sinU1 = tanU1 * cosU1;
		
		// eq. 1
		double sigma1 = Math.atan2(tanU1, cosAlpha1);
		
		// eq. 2
		double sinAlpha = cosU1 * sinAlpha1;
		
		double sin2Alpha = sinAlpha * sinAlpha;
		double cos2Alpha = 1 - sin2Alpha;
		double uSquared = cos2Alpha * (aSquared - bSquared) / bSquared;
		
		// eq. 3
		double A = 1 + (uSquared / 16384) * (4096 + uSquared * (-768 + uSquared * (320 - 175 * uSquared)));
		
		// eq. 4
		double B = (uSquared / 1024) * (256 + uSquared * (-128 + uSquared * (74 - 47 * uSquared)));
		
		// iterate until there is a negligible change in sigma
		double deltaSigma;
		double sOverbA = s / (b * A);
		double sigma = sOverbA;
		double sinSigma;
		double prevSigma = sOverbA;
		double sigmaM2;
		double cosSigmaM2;
		double cos2SigmaM2;
		
		for (;;) {
			// eq. 5
			sigmaM2 = 2.0 * sigma1 + sigma;
			cosSigmaM2 = Math.cos(sigmaM2);
			cos2SigmaM2 = cosSigmaM2 * cosSigmaM2;
			sinSigma = Math.sin(sigma);
			double cosSignma = Math.cos(sigma);
			
			// eq. 6
			deltaSigma = B
					* sinSigma
					* (cosSigmaM2 + (B / 4.0)
							* (cosSignma * (-1 + 2 * cos2SigmaM2) - (B / 6.0) * cosSigmaM2
									* (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM2)));
			
			// eq. 7
			sigma = sOverbA + deltaSigma;
			
			// break after converging to tolerance
			if (Math.abs(sigma - prevSigma) < 0.0000000000001)
				break;
			
			prevSigma = sigma;
		}
		
		sigmaM2 = 2.0 * sigma1 + sigma;
		cosSigmaM2 = Math.cos(sigmaM2);
		cos2SigmaM2 = cosSigmaM2 * cosSigmaM2;
		
		double cosSigma = Math.cos(sigma);
		sinSigma = Math.sin(sigma);
		
		// eq. 8
		double phi2 = Math.atan2(sinU1 * cosSigma + cosU1 * sinSigma * cosAlpha1,
				(1.0 - f) * Math.sqrt(sin2Alpha + Math.pow(sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1, 2.0)));
		
		// eq. 9
		// This fixes the pole crossing defect spotted by Matt Feemster. When a
		// path passes a pole and essentially crosses a line of latitude twice -
		// once in each direction - the longitude calculation got messed up.
		// Using
		// atan2 instead of atan fixes the defect. The change is in the next 3
		// lines.
		// double tanLambda = sinSigma * sinAlpha1 / (cosU1 * cosSigma - sinU1 *
		// sinSigma * cosAlpha1);
		// double lambda = Math.atan(tanLambda);
		double lambda = Math.atan2(sinSigma * sinAlpha1, (cosU1 * cosSigma - sinU1 * sinSigma * cosAlpha1));
		
		// eq. 10
		double C = (f / 16) * cos2Alpha * (4 + f * (4 - 3 * cos2Alpha));
		
		// eq. 11
		double L = lambda - (1 - C) * f * sinAlpha
				* (sigma + C * sinSigma * (cosSigmaM2 + C * cosSigma * (-1 + 2 * cos2SigmaM2)));
		
		// eq. 12
//		double alpha2 = Math.atan2(sinAlpha, -sinU1 * sinSigma + cosU1 * cosSigma * cosAlpha1);
		
		// build result
		double latitude = toDegrees(phi2);
		double longitude = start.getLongitude() + toDegrees(L);
		
//		if ((endBearing != null) && (endBearing.length > 0)) {
//			endBearing[0] = toDegrees(alpha2);
//		}
		
		return new GeoPoint((int)(1E6 * latitude), (int)(1E6 * longitude));
	}

}
