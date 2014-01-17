package com.robert.maps.applib.data;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;
import com.robert.maps.applib.R;
import com.robert.maps.applib.kml.constants.PoiConstants;
import com.robert.maps.applib.utils.Ut;

public class GeoData {
	private static GeoData mInstance = null;
	
	private Context mContext;
	private GeoDataDatabaseOpenHelper mSQLiteOpenHelper;
	
	
	public static GeoData getInstance(Context context) {
		if(mInstance == null) {
			mInstance = new GeoData(context);
		}
		
		return mInstance;
	}

	public GeoData(Context context) {
		mContext = context;

		File folder = Ut.getRMapsMainDir(context, PoiConstants.DATA);
		mSQLiteOpenHelper = new GeoDataDatabaseOpenHelper(context, folder.getAbsolutePath() + PoiConstants.GEODATA_FILENAME);
	}
	
	
	
	




	protected class GeoDataDatabaseOpenHelper extends SQLiteSDOpenHelper {
		private final static int mCurrentVersion = 22;
		
		public GeoDataDatabaseOpenHelper(final Context context, final String name) {
			super(context, name, null, mCurrentVersion);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			db.execSQL(PoiConstants.SQL_CREATE_points);
			db.execSQL(PoiConstants.SQL_CREATE_pointsource);
			db.execSQL(PoiConstants.SQL_CREATE_category);
			db.execSQL(PoiConstants.SQL_ADD_category);
			db.execSQL(PoiConstants.SQL_CREATE_tracks);
			db.execSQL(PoiConstants.SQL_CREATE_trackpoints);
			db.execSQL(PoiConstants.SQL_CREATE_maps);
			db.execSQL(PoiConstants.SQL_CREATE_routes);
			LoadActivityListFromResource(db);
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {

			if (oldVersion < 2) {
				db.execSQL(PoiConstants.SQL_UPDATE_1_1);
				db.execSQL(PoiConstants.SQL_UPDATE_1_2);
				db.execSQL(PoiConstants.SQL_UPDATE_1_3);
				db.execSQL(PoiConstants.SQL_CREATE_points);
				db.execSQL(PoiConstants.SQL_UPDATE_1_5);
				db.execSQL(PoiConstants.SQL_UPDATE_1_6);
				db.execSQL(PoiConstants.SQL_UPDATE_1_7);
				db.execSQL(PoiConstants.SQL_UPDATE_1_8);
				db.execSQL(PoiConstants.SQL_UPDATE_1_9);
				db.execSQL(PoiConstants.SQL_CREATE_category);
				db.execSQL(PoiConstants.SQL_ADD_category);
				//db.execSQL(PoiConstants.SQL_UPDATE_1_11);
				//db.execSQL(PoiConstants.SQL_UPDATE_1_12);
			}
			if (oldVersion < 3) {
				db.execSQL(PoiConstants.SQL_UPDATE_2_7);
				db.execSQL(PoiConstants.SQL_UPDATE_2_8);
				db.execSQL(PoiConstants.SQL_UPDATE_2_9);
				db.execSQL(PoiConstants.SQL_CREATE_category);
				db.execSQL(PoiConstants.SQL_UPDATE_2_11);
				db.execSQL(PoiConstants.SQL_UPDATE_2_12);
			}
			if (oldVersion < 5) {
				db.execSQL(PoiConstants.SQL_CREATE_tracks);
				db.execSQL(PoiConstants.SQL_CREATE_trackpoints);
			}
			if (oldVersion < 18) {
				db.execSQL(PoiConstants.SQL_UPDATE_6_1);
				db.execSQL(PoiConstants.SQL_UPDATE_6_2);
				db.execSQL(PoiConstants.SQL_UPDATE_6_3);
				db.execSQL(PoiConstants.SQL_CREATE_tracks);
				db.execSQL(PoiConstants.SQL_UPDATE_6_4);
				db.execSQL(PoiConstants.SQL_UPDATE_6_5);
				LoadActivityListFromResource(db);
			}
			if (oldVersion < 20) {
				db.execSQL(PoiConstants.SQL_UPDATE_6_1);
				db.execSQL(PoiConstants.SQL_UPDATE_6_2);
				db.execSQL(PoiConstants.SQL_UPDATE_6_3);
				db.execSQL(PoiConstants.SQL_CREATE_tracks);
				db.execSQL(PoiConstants.SQL_UPDATE_20_1);
				db.execSQL(PoiConstants.SQL_UPDATE_6_5);
			}
			if (oldVersion < 21) {
				db.execSQL(PoiConstants.SQL_CREATE_maps);
			}
			if (oldVersion < 22) {
				db.execSQL(PoiConstants.SQL_CREATE_routes);
			}
		}

		public void LoadActivityListFromResource(final SQLiteDatabase db) {
			db.execSQL(PoiConstants.SQL_CREATE_drop_activity);
			db.execSQL(PoiConstants.SQL_CREATE_activity);
	    	String[] act = mContext.getResources().getStringArray(R.array.track_activity);
	    	for(int i = 0; i < act.length; i++){
	    		db.execSQL(String.format(PoiConstants.SQL_CREATE_insert_activity, i, act[i]));
	    	}
		}

	}






	public SQLiteCursorLoader getPoiListCursorLoader() {
		return getPoiListCursorLoader(PoiConstants.LATLON);
	}

	public SQLiteCursorLoader getPoiListCursorLoader(String sortColNames) {
		// не менять порядок полей
		return new SQLiteCursorLoader(mContext, mSQLiteOpenHelper, PoiConstants.STAT_GET_POI_LIST + sortColNames, null);
	}


}
