// Created by plusminus on 17:53:07 - 25.09.2008
package org.andnav.osm.views.util;

import org.andnav.osm.util.BoundingBoxE6;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import com.robert.maps.utils.OSGB36;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class Util implements OpenStreetMapViewConstants{
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public static int[] getMapTileFromCoordinates(final int aLat, final int aLon, final int zoom, final int[] reuse, final int aProjection) {
		return getMapTileFromCoordinates(aLat / 1E6, aLon / 1E6, zoom, reuse, aProjection);
	}

	public static int[] getMapTileFromCoordinates(final double aLat, final double aLon, final int zoom, final int[] aUseAsReturnValue, final int aProjection) {
		final int[] out = (aUseAsReturnValue != null) ? aUseAsReturnValue : new int[2];

		if (aProjection == 3) {
			final double[] OSRef = OSGB36.LatLon2OSGB(aLat, aLon);
			out[0] = (int) ((1 - OSRef[0] / 1000000)*OpenSpaceUpperBoundArray[zoom - 7]);
			out[1] = (int) ((OSRef[1] / 1000000)*OpenSpaceUpperBoundArray[zoom - 7]);
		} else {
			if (aProjection == 1)
				out[MAPTILE_LATITUDE_INDEX] = (int) Math.floor((1 - Math
						.log(Math.tan(aLat * Math.PI / 180) + 1
								/ Math.cos(aLat * Math.PI / 180))
						/ Math.PI)
						/ 2 * (1 << zoom));
			else {
				final double E2 = (double) aLat * Math.PI / 180;
				final long sradiusa = 6378137;
				final long sradiusb = 6356752;
				final double J2 = (double) Math.sqrt(sradiusa * sradiusa
						- sradiusb * sradiusb)
						/ sradiusa;
				final double M2 = (double) Math.log((1 + Math.sin(E2))
						/ (1 - Math.sin(E2)))
						/ 2
						- J2
						* Math.log((1 + J2 * Math.sin(E2))
								/ (1 - J2 * Math.sin(E2))) / 2;
				final double B2 = (double) (1 << zoom);
				out[MAPTILE_LATITUDE_INDEX] = (int) Math.floor(B2 / 2 - M2 * B2
						/ 2 / Math.PI);
			}

			out[MAPTILE_LONGITUDE_INDEX] = (int) Math.floor((aLon + 180) / 360
					* (1 << zoom));
		}

		return out;
	}

	// Conversion of a MapTile to a BoudingBox

	public static BoundingBoxE6 getBoundingBoxFromMapTile(final int[] aMapTile, final int zoom, final int aProjection) {
		final int y = aMapTile[MAPTILE_LATITUDE_INDEX];
		final int x = aMapTile[MAPTILE_LONGITUDE_INDEX];

		if(aProjection == 3){
			final double[] LatLon0 = OSGB36.OSGB2LatLon(
					(double)((OpenSpaceUpperBoundArray[zoom - 7] - y - 1) * 1000000
							/ OpenSpaceUpperBoundArray[zoom - 7]), (double)(x * 1000000
							/ OpenSpaceUpperBoundArray[zoom - 7]));
			final double[] LatLon1 = OSGB36.OSGB2LatLon(
					(double)((OpenSpaceUpperBoundArray[zoom - 7] - y - 1 + 1) * 1000000
							/ OpenSpaceUpperBoundArray[zoom - 7]), (double)((x + 1) * 1000000
							/ OpenSpaceUpperBoundArray[zoom - 7]));
			return new BoundingBoxE6(LatLon1[0], LatLon1[1], LatLon0[0], LatLon0[1]);
		} else
			return new BoundingBoxE6(tile2lat(y, zoom, aProjection), tile2lon(x + 1, zoom), tile2lat(y + 1, zoom, aProjection), tile2lon(x, zoom));
	}

	private static double tile2lon(int x, int aZoom) {
		return (x / Math.pow(2.0, aZoom) * 360.0) - 180;
	}

	private static double tile2lat(int y, int aZoom, final int aProjection) {

		if (aProjection == 1) {
			final double n = Math.PI
					- ((2.0 * Math.PI * y) / Math.pow(2.0, aZoom));
			return 180.0 / Math.PI
					* Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));
		} else {
			final double MerkElipsK = 0.0000001;
			final long sradiusa = 6378137;
			final long sradiusb = 6356752;
			final double FExct = (double) Math.sqrt(sradiusa * sradiusa
					- sradiusb * sradiusb)
					/ sradiusa;
			final int TilesAtZoom = 1 << aZoom;
			double result = (y - TilesAtZoom / 2)
					/ -(TilesAtZoom / (2 * Math.PI));
			result = (2 * Math.atan(Math.exp(result)) - Math.PI / 2) * 180
					/ Math.PI;
			double Zu = result / (180 / Math.PI);
			double yy = ((y) - TilesAtZoom / 2);

			double Zum1 = Zu;
			Zu = Math
					.asin(1
							- ((1 + Math.sin(Zum1)) * Math.pow(1 - FExct
									* Math.sin(Zum1), FExct))
							/ (Math.exp((2 * yy)
									/ -(TilesAtZoom / (2 * Math.PI))) * Math
									.pow(1 + FExct * Math.sin(Zum1), FExct)));
			while (Math.abs(Zum1 - Zu) >= MerkElipsK) {
				Zum1 = Zu;
				Zu = Math
						.asin(1
								- ((1 + Math.sin(Zum1)) * Math.pow(1 - FExct
										* Math.sin(Zum1), FExct))
								/ (Math.exp((2 * yy)
										/ -(TilesAtZoom / (2 * Math.PI))) * Math
										.pow(1 + FExct * Math.sin(Zum1), FExct)));
			}

			result = Zu * 180 / Math.PI;

			return result;
		}
	}

	public static int x2lon(int x, int aZoom, final int MAPTILE_SIZEPX) {
		int px = MAPTILE_SIZEPX * (1 << aZoom);
		if (x < 0)
			x = px + x;
		if (x > px)
			x = x - px;
		return (int) (1E6 * (((double)x / px * 360.0) - 180));
	}

	public static double y2lat(int y, int aZoom, final int MAPTILE_SIZEPX) {
//		final int aProjection = 1;

//		if (aProjection == 1) {
			final double n = Math.PI
					- ((2.0 * Math.PI * y) / MAPTILE_SIZEPX * Math.pow(2.0, aZoom));
			return 180.0 / Math.PI
					* Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));
//		} else {
//			final double MerkElipsK = 0.0000001;
//			final long sradiusa = 6378137;
//			final long sradiusb = 6356752;
//			final double FExct = (double) Math.sqrt(sradiusa * sradiusa
//					- sradiusb * sradiusb)
//					/ sradiusa;
//			final int TilesAtZoom = 1 << aZoom;
//			double result = (y - TilesAtZoom / 2)
//					/ -(TilesAtZoom / (2 * Math.PI));
//			result = (2 * Math.atan(Math.exp(result)) - Math.PI / 2) * 180
//					/ Math.PI;
//			double Zu = result / (180 / Math.PI);
//			double yy = ((y) - TilesAtZoom / 2);
//
//			double Zum1 = Zu;
//			Zu = Math
//					.asin(1
//							- ((1 + Math.sin(Zum1)) * Math.pow(1 - FExct
//									* Math.sin(Zum1), FExct))
//							/ (Math.exp((2 * yy)
//									/ -(TilesAtZoom / (2 * Math.PI))) * Math
//									.pow(1 + FExct * Math.sin(Zum1), FExct)));
//			while (Math.abs(Zum1 - Zu) >= MerkElipsK) {
//				Zum1 = Zu;
//				Zu = Math
//						.asin(1
//								- ((1 + Math.sin(Zum1)) * Math.pow(1 - FExct
//										* Math.sin(Zum1), FExct))
//								/ (Math.exp((2 * yy)
//										/ -(TilesAtZoom / (2 * Math.PI))) * Math
//										.pow(1 + FExct * Math.sin(Zum1), FExct)));
//			}
//
//			result = Zu * 180 / Math.PI;
//
//			return result;
//		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
