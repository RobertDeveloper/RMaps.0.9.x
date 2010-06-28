package com.robert.maps.kml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.robert.maps.R;
import com.robert.maps.kml.XMLparser.SimpleXML;
import com.robert.maps.trackwriter.DatabaseHelper;
import com.robert.maps.utils.Ut;

public class TrackListActivity extends ListActivity {
	private PoiManager mPoiManager;

	private ProgressDialog dlgWait;
	protected ExecutorService mThreadPool = Executors.newFixedThreadPool(2);
	private SimpleInvalidationHandler mHandler;

	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case R.id.tracks:
				if(msg.arg1 == 0)
					Toast.makeText(TrackListActivity.this, R.string.trackwriter_nothing, Toast.LENGTH_LONG).show();
				else
					Toast.makeText(TrackListActivity.this, R.string.trackwriter_saved, Toast.LENGTH_LONG).show();

				FillData();
				break;
			}
		}
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.track_list);
        registerForContextMenu(getListView());
        mPoiManager = new PoiManager(this);

        mHandler = new SimpleInvalidationHandler();

		((Button) findViewById(R.id.startButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startService(new Intent("com.robert.maps.trackwriter"));
			}
		});
		((Button) findViewById(R.id.stopButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				stopService(new Intent("com.robert.maps.trackwriter"));
				doSaveTrack();
			}
		});
	}
	
	private void doSaveTrack(){
		showDialog(R.id.dialog_wait);

		this.mThreadPool.execute(new Runnable() {
			public void run() {
				final SQLiteDatabase db;
				File folder = Ut.getRMapsFolder("data", false);
				db = new DatabaseHelper(TrackListActivity.this, folder.getAbsolutePath() + "/writedtrack.db").getWritableDatabase();
				final int res = mPoiManager.getGeoDatabase().saveTrackFromWriter(db);
				db.releaseReference();

				dlgWait.dismiss();
				Message.obtain(mHandler, R.id.tracks, res, 0).sendToTarget();
			}
		});
	}

	@Override
	protected void onResume() {
		FillData();
		super.onResume();
	}

	private void FillData() {
		Cursor c = mPoiManager.getGeoDatabase().getTrackListCursor();
        startManagingCursor(c);

        ListAdapter adapter = new SimpleCursorAdapter(this,
                R.layout.list_item
                , c,
                        new String[] { "name", "descr", "image" },
                        new int[] { android.R.id.text1, android.R.id.text2, R.id.ImageView01 });
        setListAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tracklist, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch(item.getItemId()){
		case R.id.menu_importpoi:
			startActivity((new Intent(this, ImportTrackActivity.class)));
			return true;
		}

		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
//		int pointid = (int) ((AdapterView.AdapterContextMenuInfo)menuInfo).id;
//		PoiPoint poi = mPoiManager.getPoiPoint(pointid);
//
		menu.add(0, R.id.menu_gotopoi, 0, getText(R.string.menu_goto_track));
		menu.add(0, R.id.menu_editpoi, 0, getText(R.string.menu_edit));
		menu.add(0, R.id.menu_deletepoi, 0, getText(R.string.menu_delete));
		menu.add(0, R.id.menu_exporttogpxpoi, 0, getText(R.string.menu_exporttogpx));

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int id = (int) ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).id;
//		PoiPoint poi = mPoiManager.getPoiPoint(pointid);
//
		switch(item.getItemId()){
		case R.id.menu_editpoi:
			startActivity((new Intent(this, TrackActivity.class)).putExtra("id", id));
			break;
		case R.id.menu_gotopoi:
			setResult(RESULT_OK, (new Intent()).putExtra("trackid", id));
			finish();
			break;
		case R.id.menu_deletepoi:
			mPoiManager.deleteTrack(id);
			FillData();
	        break;
		case R.id.menu_exporttogpxpoi:
			DoExportTrack(id);
	        break;
		}

		return super.onContextItemSelected(item);
	}

	private void DoExportTrack(int id) {
		final Track track = mPoiManager.getTrack(id);

		SimpleXML xml = new SimpleXML("gpx");
		
		File folder = Ut.getRMapsFolder("export", false);
		File file = new File(folder.getAbsolutePath() + "/track" + id + ".gpx");
		FileOutputStream out;
		try {
			file.createNewFile();
			out = new FileOutputStream(file);
			OutputStreamWriter wr = new OutputStreamWriter(out);
			wr.write(SimpleXML.saveXml(xml));
			wr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.dialog_wait: {
			dlgWait = new ProgressDialog(this);
			dlgWait.setMessage("Please wait while loading...");
			dlgWait.setIndeterminate(true);
			dlgWait.setCancelable(false);
			return dlgWait;
		}
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Ut.dd("pos="+position);
		Ut.dd("id="+id);
		mPoiManager.setTrackChecked((int)id);
		FillData();
		super.onListItemClick(l, v, position, id);
	}

}
