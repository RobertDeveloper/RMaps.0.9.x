package com.robert.maps.kml.constants;

import com.robert.maps.R;

public interface PoiConstants {
	public static final int EMPTY_ID = -777;

	public static final String SQL_CREATE_points = "CREATE TABLE 'points' (pointid INTEGER NOT NULL PRIMARY KEY UNIQUE,name VARCHAR,descr VARCHAR,lat FLOAT DEFAULT '0',lon FLOAT DEFAULT '0',alt FLOAT DEFAULT '0',hidden INTEGER DEFAULT '0',categoryid INTEGER,pointsourceid INTEGER,iconid INTEGER DEFAULT NULL);";
	public static final String SQL_CREATE_category = "CREATE TABLE 'category' (categoryid INTEGER NOT NULL PRIMARY KEY UNIQUE, name VARCHAR, hidden INTEGER DEFAULT '0', iconid INTEGER DEFAULT NULL, minzoom INTEGER DEFAULT '14');";
	public static final String SQL_CREATE_pointsource = "CREATE TABLE IF NOT EXISTS 'pointsource' (pointsourceid INTEGER NOT NULL PRIMARY KEY UNIQUE, name VARCHAR);";
	public static final String SQL_CREATE_tracks = "CREATE TABLE IF NOT EXISTS 'tracks' (trackid INTEGER NOT NULL PRIMARY KEY UNIQUE, name VARCHAR, descr VARCHAR, date DATETIME);";
	public static final String SQL_CREATE_trackpoints = "CREATE TABLE IF NOT EXISTS 'trackpoints' (trackid INTEGER NOT NULL, id INTEGER NOT NULL PRIMARY KEY UNIQUE, lat FLOAT, lon FLOAT, alt FLOAT, speed FLOAT, date DATETIME);";

	public static final String SQL_ADD_category = "INSERT INTO 'category' (categoryid, name, hidden, iconid) VALUES (0, 'My POI', 0, "
			+ R.drawable.poiblue + ");";

	public static final String SQL_UPDATE_1_1 = "DROP TABLE IF EXISTS 'points_45392250'; ";
	public static final String SQL_UPDATE_1_2 = "CREATE TABLE 'points_45392250' AS SELECT * FROM 'points';";
	public static final String SQL_UPDATE_1_3 = "DROP TABLE 'points';";
	public static final String SQL_UPDATE_1_5 = "INSERT INTO 'points' (pointid, name, descr, lat, lon, alt, hidden, categoryid, pointsourceid, iconid) SELECT pointid, name, descr, lat, lon, alt, hidden, categoryid, pointsourceid, "
			+ R.drawable.poi + " FROM 'points_45392250';";
	public static final String SQL_UPDATE_1_6 = "DROP TABLE 'points_45392250';";

	public static final String SQL_UPDATE_1_7 = "DROP TABLE IF EXISTS 'category_46134312'; ";
	public static final String SQL_UPDATE_1_8 = "CREATE TABLE 'category_46134312' AS SELECT * FROM 'category';";
	public static final String SQL_UPDATE_1_9 = "DROP TABLE 'category';";
	public static final String SQL_UPDATE_1_11 = "INSERT INTO 'category' (categoryid, name) SELECT categoryid, name FROM 'category_46134312';";
	public static final String SQL_UPDATE_1_12 = "DROP TABLE 'category_46134312';";

	public static final String SQL_UPDATE_2_7 = "DROP TABLE IF EXISTS 'category_46134313'; ";
	public static final String SQL_UPDATE_2_8 = "CREATE TABLE 'category_46134313' AS SELECT * FROM 'category';";
	public static final String SQL_UPDATE_2_9 = "DROP TABLE 'category';";
	public static final String SQL_UPDATE_2_11 = "INSERT INTO 'category' (categoryid, name, hidden, iconid) SELECT categoryid, name, hidden, iconid FROM 'category_46134313';";
	public static final String SQL_UPDATE_2_12 = "DROP TABLE 'category_46134313';";
}
