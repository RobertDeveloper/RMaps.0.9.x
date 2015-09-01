package com.robert.maps.applib.dashboard;

public interface IndicatorConst {
	public static final String GPS = "gps";
	public static final String OFF = "off";
	public static final String EMPTY = "";

	public static final String JNAME = "name";
	public static final String JINDICATORS = "indicators";
	public static final String JMAIN = "main";
	public static final String JMAINLANDSCAPE = "main_landscape";
	public static final String JTAG = "tag";
	public static final String JINDEX = "index";
	public static final String DASHBOARD_DIR = "data/dashboards";
	public static final String DASHBOARD_FILE = "%s/%s.json";

	public static final String GPSSPEED = "gpsspeed";
	public static final String GPSLAT = "gpslat";
	public static final String GPSLON = "gpslon";
	public static final String GPSBEARING = "gpsbearing";
	public static final String GPSELEV = "gpselev";
	public static final String GPSACCURACY = "gpsaccuracy";
	public static final String GPSTIME = "gpstime"; // UTC time of this fix, in milliseconds since January 1, 1970
	public static final String GPSPROVIDER = "gpsprovider";

	public static final String MAPNAME = "mapname";
	public static final String MAPZOOM = "mapzoom";
	public static final String MAPCENTERLAT = "mapcenterlat";
	public static final String MAPCENTERLON = "mapcenterlon";
	
	public static final String TRCNT = "trcnt";
	public static final String TRDIST = "trdist";
	public static final String TRDURATION = "trduration";
	public static final String TRMAXSPEED = "trmaxspeed";
	public static final String TRAVGSPEED = "travgspeed";
	public static final String TRMOVETIME = "trmovetime";
	public static final String TRAVGMOVESPEED = "travgmovespeed";

	public static final String TARGETDISTANCE = "targetdistance";
	public static final String TARGETBEARING = "targetbearing";
}
