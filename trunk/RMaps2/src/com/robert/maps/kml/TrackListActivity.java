package com.robert.maps.kml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
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
import com.robert.maps.kml.Track.TrackPoint;
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
			case R.id.menu_exporttogpxpoi:
				if (msg.arg1 == 0)
					Toast
							.makeText(TrackListActivity.this,
									getString(R.string.message_error) + " " + (String) msg.obj,
									Toast.LENGTH_LONG).show();
				else
					Toast.makeText(TrackListActivity.this,
							getString(R.string.message_trackexported) + " " + (String) msg.obj,
							Toast.LENGTH_LONG).show();
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
				SQLiteDatabase db = null;
				File folder = Ut.getRMapsMainDir(TrackListActivity.this, "data");
				if(folder.canRead()){
					try {
						db = new DatabaseHelper(TrackListActivity.this, folder.getAbsolutePath() + "/writedtrack.db").getWritableDatabase();
					} catch (Exception e) {
						db = null;
					}
				};
				int res = 0;
				if(db != null){
					try {
						res = mPoiManager.getGeoDatabase().saveTrackFromWriter(db);
					} catch (Exception e) {
					}
					db.close();
				};

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
		if(c != null){
	        startManagingCursor(c);

	        ListAdapter adapter = new SimpleCursorAdapter(this,
	                R.layout.list_item
	                , c,
	                        new String[] { "name", "descr", "image" },
	                        new int[] { android.R.id.text1, android.R.id.text2, R.id.ImageView01 });
	        setListAdapter(adapter);
		};
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
		menu.add(0, R.id.menu_exporttokmlpoi, 0, getText(R.string.menu_exporttokml));

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
			DoExportTrackGPX(id);
	        break;
		case R.id.menu_exporttokmlpoi:
			DoExportTrackKML(id);
	        break;
		}

		return super.onContextItemSelected(item);
	}

	private void DoExportTrackKML(int id) {
		showDialog(R.id.dialog_wait);
		final int trackid = id;

		this.mThreadPool.execute(new Runnable() {
			public void run() {
				final Track track = mPoiManager.getTrack(trackid);

				SimpleXML xml = new SimpleXML("kml");
				xml.setAttr("xmlns:gx", "http://www.google.com/kml/ext/2.2");
				xml.setAttr("xmlns", "http://www.opengis.net/kml/2.2");

				SimpleXML Placemark = xml.createChild("Placemark");
				Placemark.createChild("name").setText(track.Name);
				Placemark.createChild("description").setText(track.Descr);
				SimpleXML LineString = Placemark.createChild("LineString");
				SimpleXML coordinates = LineString.createChild("coordinates");
				StringBuilder builder = new StringBuilder();

				for (TrackPoint tp : track.getPoints()){
					builder.append(tp.lon).append(",").append(tp.lat).append(",").append(tp.alt).append(" ");
				}
				coordinates.setText(builder.toString().trim());

				File folder = Ut.getRMapsExportDir(TrackListActivity.this);
				String filename = folder.getAbsolutePath() + "/track" + trackid + ".kml";
				File file = new File(filename);
				FileOutputStream out;
				try {
					file.createNewFile();
					out = new FileOutputStream(file);
					OutputStreamWriter wr = new OutputStreamWriter(out);
					wr.write(SimpleXML.saveXml(xml));
					wr.close();
					Message.obtain(mHandler, R.id.menu_exporttogpxpoi, 1, 0, filename).sendToTarget();
				} catch (FileNotFoundException e) {
					Message.obtain(mHandler, R.id.menu_exporttogpxpoi, 0, 0, e.getMessage()).sendToTarget();
					e.printStackTrace();
				} catch (IOException e) {
					Message.obtain(mHandler, R.id.menu_exporttogpxpoi, 0, 0, e.getMessage()).sendToTarget();
					e.printStackTrace();
				}

				dlgWait.dismiss();
			}
		});


	}

	private void DoExportTrackGPX(int id) {
		showDialog(R.id.dialog_wait);
		final int trackid = id;

		this.mThreadPool.execute(new Runnable() {
			public void run() {
				final Track track = mPoiManager.getTrack(trackid);

				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				SimpleXML xml = new SimpleXML("gpx");
				xml.setAttr("xsi:schemaLocation", "http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd");
				xml.setAttr("xmlns", "http://www.topografix.com/GPX/1/0");
				xml.setAttr("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
				xml.setAttr("creator", "RMaps - http://code.google.com/p/robertprojects/");
				xml.setAttr("version", "1.0");

				xml.createChild("name").setText(track.Name);
				xml.createChild("desc").setText(track.Descr);

				SimpleXML trk = xml.createChild("trk");
				SimpleXML trkseg = trk.createChild("trkseg");
				SimpleXML trkpt = null;
				for (TrackPoint tp : track.getPoints()){
					trkpt = trkseg.createChild("trkpt");
					trkpt.setAttr("lat", Double.toString(tp.lat));
					trkpt.setAttr("lon", Double.toString(tp.lon));
					trkpt.createChild("ele").setText(Double.toString(tp.alt));
					trkpt.createChild("time").setText(formatter.format(tp.date));
				}

				File folder = Ut.getRMapsExportDir(TrackListActivity.this);
				String filename = folder.getAbsolutePath() + "/track" + trackid + ".gpx";
				File file = new File(filename);
				FileOutputStream out;
				try {
					file.createNewFile();
					out = new FileOutputStream(file);
					OutputStreamWriter wr = new OutputStreamWriter(out);
					wr.write(SimpleXML.saveXml(xml));
					wr.close();
					Message.obtain(mHandler, R.id.menu_exporttogpxpoi, 1, 0, filename).sendToTarget();
				} catch (FileNotFoundException e) {
					Message.obtain(mHandler, R.id.menu_exporttogpxpoi, 0, 0, e.getMessage()).sendToTarget();
					e.printStackTrace();
				} catch (IOException e) {
					Message.obtain(mHandler, R.id.menu_exporttogpxpoi, 0, 0, e.getMessage()).sendToTarget();
					e.printStackTrace();
				}

				dlgWait.dismiss();
			}
		});


	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.dialog_wait: {
			dlgWait = new ProgressDialog(this);
			dlgWait.setMessage("Please wait...");
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
