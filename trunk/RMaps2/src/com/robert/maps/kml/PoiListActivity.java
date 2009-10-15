package com.robert.maps.kml;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class PoiListActivity extends ListActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		GeoDatabase db = new GeoDatabase(this);
		Cursor c = db.getPoiListCursor();
        startManagingCursor(c);

        ListAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_2, c, 
                        new String[] { "name", "descr" }, 
                        new int[] { android.R.id.text1, android.R.id.text2 });
        setListAdapter(adapter);
	}
}
