package com.robert.maps.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class Ut implements OpenStreetMapConstants, OpenStreetMapViewConstants {
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
	public static void d(String str){
		if(DEBUGMODE)
			Log.d(DEBUGTAG, str);
	}

	public static String FileName2ID(String name) {
		return name.replace(".", "_").replace(" ", "_").replace("-", "_").trim();
	}

	public static File getRMapsFolder(String aName, boolean aShowAlertToast) {
		File folder = new File("/sdcard/rmaps/" + aName);
		if(!folder.exists()){
			if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
				folder.mkdirs();
			}else if(aShowAlertToast){
				//Toast.makeText(new Application(), "SD card is not available", Toast.LENGTH_LONG);
			}
		}

		return folder;
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

	public static double sinSquared(double x) {
		return Math.sin(x) * Math.sin(x);
	}

	public static double cosSquared(double x) {
		return Math.cos(x) * Math.cos(x);
	}

	public static double tanSquared(double x) {
		return Math.tan(x) * Math.tan(x);
	}

	public static double sec(double x) {
		return 1.0 / Math.cos(x);
	}

	public static double[] OSRef2LatLon(final double latOSRef, final double lonOSRef){
		class RefEll {
			private double maj;
			private double min;
			private double ecc;

			public RefEll(double maj, double min) {
				this.maj = maj;
				this.min = min;
				this.ecc = ((maj * maj) - (min * min)) / (maj * maj);
			}

			public double getMaj() {
				return maj;
			}

			public double getMin() {
				return min;
			}

			public double getEcc() {
				return ecc;
			}
		}

	    double OSGB_F0 = 0.9996012717;
	    double N0 = -100000.0;
	    double E0 = 400000.0;
	    double phi0 = Math.toRadians(49.0);
	    double lambda0 = Math.toRadians(-2.0);
	    RefEll AIRY_1830 = new RefEll(6377563.396, 6356256.909);
	    double a = AIRY_1830.getMaj();
	    double b = AIRY_1830.getMin();
	    double eSquared = AIRY_1830.getEcc();
	    double phi = 0.0;
	    double lambda = 0.0;
	    double E = lonOSRef;
	    double N = latOSRef;
	    double n = (a - b) / (a + b);
	    double M = 0.0;
	    double phiPrime = ((N - N0) / (a * OSGB_F0)) + phi0;
	    do {
	      M =
	          (b * OSGB_F0)
	              * (((1 + n + ((5.0 / 4.0) * n * n) + ((5.0 / 4.0) * n * n * n)) * (phiPrime - phi0))
	                  - (((3 * n) + (3 * n * n) + ((21.0 / 8.0) * n * n * n))
	                      * Math.sin(phiPrime - phi0) * Math.cos(phiPrime + phi0))
	                  + ((((15.0 / 8.0) * n * n) + ((15.0 / 8.0) * n * n * n))
	                      * Math.sin(2.0 * (phiPrime - phi0)) * Math
	                      .cos(2.0 * (phiPrime + phi0))) - (((35.0 / 24.0) * n * n * n)
	                  * Math.sin(3.0 * (phiPrime - phi0)) * Math
	                  .cos(3.0 * (phiPrime + phi0))));
	      phiPrime += (N - N0 - M) / (a * OSGB_F0);
	    } while ((N - N0 - M) >= 0.001);
	    double v =
	        a * OSGB_F0
	            * Math.pow(1.0 - eSquared * Ut.sinSquared(phiPrime), -0.5);
	    double rho =
	        a * OSGB_F0 * (1.0 - eSquared)
	            * Math.pow(1.0 - eSquared * Ut.sinSquared(phiPrime), -1.5);
	    double etaSquared = (v / rho) - 1.0;
	    double VII = Math.tan(phiPrime) / (2 * rho * v);
	    double VIII =
	        (Math.tan(phiPrime) / (24.0 * rho * Math.pow(v, 3.0)))
	            * (5.0 + (3.0 * Ut.tanSquared(phiPrime)) + etaSquared - (9.0 * Ut
	                .tanSquared(phiPrime) * etaSquared));
	    double IX =
	        (Math.tan(phiPrime) / (720.0 * rho * Math.pow(v, 5.0)))
	            * (61.0 + (90.0 * Ut.tanSquared(phiPrime)) + (45.0 * Ut
	                .tanSquared(phiPrime) * Ut.tanSquared(phiPrime)));
	    double X = Ut.sec(phiPrime) / v;
	    double XI =
	        (Ut.sec(phiPrime) / (6.0 * v * v * v))
	            * ((v / rho) + (2 * Ut.tanSquared(phiPrime)));
	    double XII =
	        (Ut.sec(phiPrime) / (120.0 * Math.pow(v, 5.0)))
	            * (5.0 + (28.0 * Ut.tanSquared(phiPrime)) + (24.0 * Ut
	                .tanSquared(phiPrime) * Ut.tanSquared(phiPrime)));
	    double XIIA =
	        (Ut.sec(phiPrime) / (5040.0 * Math.pow(v, 7.0)))
	            * (61.0
	                + (662.0 * Ut.tanSquared(phiPrime))
	                + (1320.0 * Ut.tanSquared(phiPrime) * Ut
	                    .tanSquared(phiPrime)) + (720.0 * Ut.tanSquared(phiPrime)
	                * Ut.tanSquared(phiPrime) * Ut.tanSquared(phiPrime)));
	    phi =
	        phiPrime - (VII * Math.pow(E - E0, 2.0))
	            + (VIII * Math.pow(E - E0, 4.0)) - (IX * Math.pow(E - E0, 6.0));
	    lambda =
	        lambda0 + (X * (E - E0)) - (XI * Math.pow(E - E0, 3.0))
	            + (XII * Math.pow(E - E0, 5.0)) - (XIIA * Math.pow(E - E0, 7.0));

	    double[] ret = new double[2];
	    ret[0] = Math.toDegrees(phi);
	    ret[1] = Math.toDegrees(lambda);
	    return ret;
	}

	public static double[] LatLon2OSRef(final double lat, final double lng){
		class RefEll {
			private double maj;
			private double min;
			private double ecc;

			public RefEll(double maj, double min) {
				this.maj = maj;
				this.min = min;
				this.ecc = ((maj * maj) - (min * min)) / (maj * maj);
			}

			public double getMaj() {
				return maj;
			}

			public double getMin() {
				return min;
			}

			public double getEcc() {
				return ecc;
			}
		}

		RefEll airy1830 = new RefEll(6377563.396, 6356256.909);
	    double OSGB_F0 = 0.9996012717;
	    double N0 = -100000.0;
	    double E0 = 400000.0;
	    double phi0 = Math.toRadians(49.0);
	    double lambda0 = Math.toRadians(-2.0);
	    double a = airy1830.getMaj();
	    double b = airy1830.getMin();
	    double eSquared = airy1830.getEcc();
	    double phi = Math.toRadians(lat);
	    double lambda = Math.toRadians(lng);
	    double E = 0.0;
	    double N = 0.0;
	    double n = (a - b) / (a + b);
	    double v =
	        a * OSGB_F0 * Math.pow(1.0 - eSquared * Ut.sinSquared(phi), -0.5);
	    double rho =
	        a * OSGB_F0 * (1.0 - eSquared)
	            * Math.pow(1.0 - eSquared * Ut.sinSquared(phi), -1.5);
	    double etaSquared = (v / rho) - 1.0;
	    double M =
	        (b * OSGB_F0)
	            * (((1 + n + ((5.0 / 4.0) * n * n) + ((5.0 / 4.0) * n * n * n)) * (phi - phi0))
	                - (((3 * n) + (3 * n * n) + ((21.0 / 8.0) * n * n * n))
	                    * Math.sin(phi - phi0) * Math.cos(phi + phi0))
	                + ((((15.0 / 8.0) * n * n) + ((15.0 / 8.0) * n * n * n))
	                    * Math.sin(2.0 * (phi - phi0)) * Math
	                    .cos(2.0 * (phi + phi0))) - (((35.0 / 24.0) * n * n * n)
	                * Math.sin(3.0 * (phi - phi0)) * Math.cos(3.0 * (phi + phi0))));
	    double I = M + N0;
	    double II = (v / 2.0) * Math.sin(phi) * Math.cos(phi);
	    double III =
	        (v / 24.0) * Math.sin(phi) * Math.pow(Math.cos(phi), 3.0)
	            * (5.0 - Ut.tanSquared(phi) + (9.0 * etaSquared));
	    double IIIA =
	        (v / 720.0)
	            * Math.sin(phi)
	            * Math.pow(Math.cos(phi), 5.0)
	            * (61.0 - (58.0 * Ut.tanSquared(phi)) + Math.pow(Math.tan(phi),
	                4.0));
	    double IV = v * Math.cos(phi);
	    double V =
	        (v / 6.0) * Math.pow(Math.cos(phi), 3.0)
	            * ((v / rho) - Ut.tanSquared(phi));
	    double VI =
	        (v / 120.0)
	            * Math.pow(Math.cos(phi), 5.0)
	            * (5.0 - (18.0 * Ut.tanSquared(phi))
	                + (Math.pow(Math.tan(phi), 4.0)) + (14 * etaSquared) - (58 * Ut
	                .tanSquared(phi) * etaSquared));

	    N =
	        I + (II * Math.pow(lambda - lambda0, 2.0))
	            + (III * Math.pow(lambda - lambda0, 4.0))
	            + (IIIA * Math.pow(lambda - lambda0, 6.0));
	    E =
	        E0 + (IV * (lambda - lambda0)) + (V * Math.pow(lambda - lambda0, 3.0))
	            + (VI * Math.pow(lambda - lambda0, 5.0));

	    double[] ret = new double[2];
	    ret[0] = N;
	    ret[1] = E;
	    return ret;
	}



	public static double[] LatLon2OSGB36(final double lat, final double lng){
//	    uk.me.jstott.jcoord.ellipsoid.WGS84Ellipsoid wgs84 = uk.me.jstott.jcoord.ellipsoid.WGS84Ellipsoid.getInstance();
		double semiMajorAxis = 6378137;
		double semiMinorAxis = 6356752.3142;
	    double semiMajorAxisSquared = semiMajorAxis * semiMajorAxis;
	    double semiMinorAxisSquared = semiMinorAxis * semiMinorAxis;
	    double eccentricitySquared = (semiMajorAxisSquared - semiMinorAxisSquared)
	        / semiMajorAxisSquared;

		double a = semiMajorAxis; //wgs84.getSemiMajorAxis();

		double eSquared = eccentricitySquared; //wgs84.getEccentricitySquared();
		double phi = Math.toRadians(lat);
		double lambda = Math.toRadians(lng);
		double v = a / (Math.sqrt(1 - eSquared * Ut.sinSquared(phi)));
		double H = 0; // height
		double x = (v + H) * Math.cos(phi) * Math.cos(lambda);
		double y = (v + H) * Math.cos(phi) * Math.sin(lambda);
		double z = ((1 - eSquared) * v + H) * Math.sin(phi);

		double tx = -446.448;
		// ty : Incorrect value in v1.0 (124.157). Corrected in v1.1.
		double ty = 125.157;
		double tz = -542.060;
		double s = 0.0000204894;
		double rx = Math.toRadians(-0.00004172222);
		double ry = Math.toRadians(-0.00006861111);
		double rz = Math.toRadians(-0.00023391666);

		double xB = tx + (x * (1 + s)) + (-rx * y) + (ry * z);
		double yB = ty + (rz * x) + (y * (1 + s)) + (-rx * z);
		double zB = tz + (-ry * x) + (rx * y) + (z * (1 + s));

//		a = Airy1830Ellipsoid.getInstance().getSemiMajorAxis();
//		eSquared = Airy1830Ellipsoid.getInstance().getEccentricitySquared();
		semiMajorAxis = 6377563.396;
		semiMinorAxis = 6356256.909;
	    semiMajorAxisSquared = semiMajorAxis * semiMajorAxis;
	    semiMinorAxisSquared = semiMinorAxis * semiMinorAxis;
	    eccentricitySquared = (semiMajorAxisSquared - semiMinorAxisSquared)
	        / semiMajorAxisSquared;
		a = semiMajorAxis;
		eSquared = eccentricitySquared;

		double lambdaB = Math.toDegrees(Math.atan(yB / xB));
		double p = Math.sqrt((xB * xB) + (yB * yB));
		double phiN = Math.atan(zB / (p * (1 - eSquared)));
		for (int i = 1; i < 10; i++) {
			v = a / (Math.sqrt(1 - eSquared * Ut.sinSquared(phiN)));
			double phiN1 = Math.atan((zB + (eSquared * v * Math.sin(phiN))) / p);
			phiN = phiN1;
		}

		double phiB = Math.toDegrees(phiN);

	    double[] ret = new double[2];
	    ret[0] = phiB;
	    ret[1] = lambdaB;
	    return ret;
	}
}
