package com.robert.maps.applib.downloader;

import android.app.ListActivity;
import android.os.Bundle;

public class FileDownloadListActivity extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		FileDownloadListAdapter adapter = new FileDownloadListAdapter(this);
		
		setListAdapter(adapter);
	}
	
}
