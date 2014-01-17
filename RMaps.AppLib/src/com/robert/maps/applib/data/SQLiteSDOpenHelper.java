package com.robert.maps.applib.data;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class SQLiteSDOpenHelper extends SQLiteOpenHelper {

	public SQLiteSDOpenHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(new ContextWrapper(context) {
			@Override
			public SQLiteDatabase openOrCreateDatabase(String name, int mode,
					SQLiteDatabase.CursorFactory factory) {

				return SQLiteDatabase.openDatabase(name, null,
						SQLiteDatabase.CREATE_IF_NECESSARY);
			}
		}, name, factory, version);
	}

}
