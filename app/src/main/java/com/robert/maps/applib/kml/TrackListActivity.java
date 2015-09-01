package com.robert.maps.applib.kml;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.robert.maps.R;
import com.robert.maps.applib.kml.Track.TrackPoint;
import com.robert.maps.applib.kml.XMLparser.SimpleXML;
import com.robert.maps.applib.trackwriter.DatabaseHelper;
import com.robert.maps.applib.utils.SimpleThreadFactory;
import com.robert.maps.applib.utils.Ut;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackListActivity extends ListActivity {
	private PoiManager mPoiManager;

	private ProgressDialog dlgWait;
	private SimpleInvalidationHandler mHandler;
	private boolean mNeedTracksStatUpdate = false;
    private ExecutorService mThreadExecutor = null;
    private int mUnits = 0;
	private String mSortOrder;

	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {
			if(msg.what == R.id.about) {
				((SimpleCursorAdapter) getListAdapter()).getCursor().requery();
			} else if(msg.what == R.id.tracks) {
				if(msg.arg1 == 0)
					Toast.makeText(TrackListActivity.this, R.string.trackwriter_nothing, Toast.LENGTH_LONG).show();
				else
					Toast.makeText(TrackListActivity.this, R.string.trackwriter_saved, Toast.LENGTH_LONG).show();

				((SimpleCursorAdapter) getListAdapter()).getCursor().requery();
			} else if(msg.what == R.id.menu_exporttogpxpoi) {
				if (msg.arg1 == 0)
					Toast
							.makeText(TrackListActivity.this,
									getString(R.string.message_error) + " " + (String) msg.obj,
									Toast.LENGTH_LONG).show();
				else
					Toast.makeText(TrackListActivity.this,
							getString(R.string.message_trackexported) + " " + (String) msg.obj,
							Toast.LENGTH_LONG).show();
			}
		}
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.track_list);
        registerForContextMenu(getListView());
        mPoiManager = new PoiManager(this);
        mSortOrder = "trackid DESC";

        mHandler = new SimpleInvalidationHandler();

		((Button) findViewById(R.id.startButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startService(new Intent("com.robert.maps.trackwriter"));
			}
		});
		((Button) findViewById(R.id.pauseButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				stopService(new Intent("com.robert.maps.trackwriter"));
			}
		});
		((Button) findViewById(R.id.stopButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				stopService(new Intent("com.robert.maps.trackwriter"));
				doSaveTrack();
			}
		});
		
		SharedPreferences settings = getPreferences(Activity.MODE_PRIVATE);
		final int versionDataUpdate = settings.getInt("versionDataUpdate", 0);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		mUnits = Integer.parseInt(pref.getString("pref_units", "0"));

		if(versionDataUpdate < 8){
			mNeedTracksStatUpdate = true;
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("versionDataUpdate", 8);
			editor.commit();
		}
		
	}

	@Override
	protected void onDestroy() {
		if(mThreadExecutor != null)
			mThreadExecutor.shutdown();
		super.onDestroy();
		mPoiManager.FreeDatabases();
	}

	private void doSaveTrack(){
		dlgWait = Ut.ShowWaitDialog(this, 0);
		if(mThreadExecutor == null)
			mThreadExecutor = Executors.newSingleThreadExecutor(new SimpleThreadFactory("doSaveTrack"));

		this.mThreadExecutor.execute(new Runnable() {
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
					
					if(res > 0){
						Track tr = mPoiManager.getTrack(res);
						tr.CalculateStat();
						mPoiManager.updateTrack(tr);
					}
				};

				dlgWait.dismiss();
				Message.obtain(mHandler, R.id.tracks, res, 0).sendToTarget();
			}
		});
	}

	private void doJoinTracks() {
		dlgWait = Ut.ShowWaitDialog(this, 0);
		if(mThreadExecutor == null)
			mThreadExecutor = Executors.newSingleThreadExecutor(new SimpleThreadFactory("doSaveTrack"));
		
		this.mThreadExecutor.execute(new Runnable() {
			public void run() {
				int res = -1;
				try {
					res = (int)mPoiManager.getGeoDatabase().JoinTracks();
				} catch (Exception e) {
				};
					
				if(res > 0){
					Track tr = mPoiManager.getTrack(res);
					tr.CalculateStat();
					mPoiManager.updateTrack(tr);
				} else {
					res = 0; // Nothing to save
				}
				
				dlgWait.dismiss();
				Message.obtain(mHandler, R.id.tracks, res, 0).sendToTarget();
			}
		});
	}

	@Override
	protected void onPause() {
		SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = uiState.edit();
		editor.putString("sortOrder", mSortOrder);
		editor.commit();
		super.onPause();
	}

	@Override
	protected void onResume() {
		final SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
		mSortOrder = uiState.getString("sortOrder", mSortOrder);
		
		FillData();
		super.onResume();
	}

	private void FillData() {
		Cursor c = mPoiManager.getGeoDatabase().getTrackListCursor(mUnits == 0 ? getResources().getString(R.string.km) : getResources().getString(R.string.ml), mSortOrder);
		
		if(mNeedTracksStatUpdate){
			mNeedTracksStatUpdate = false;
			if(c != null){
				if(c.moveToFirst()){
					if(c.getInt(8) == -1){
						UpdateTracksStat();
					}
				}
			}
		}
		
		if(c != null){
	        startManagingCursor(c);

	        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
	                R.layout.tracklist_item
	                , c,
	                        new String[] { "name", "title2", "show", "cnt", "distance" + mUnits, "duration", "units"/*, "descr"*/ },
	                        new int[] { R.id.title1, R.id.title2, R.id.checkbox, R.id.data_value1, R.id.data_value2, R.id.data_value3, R.id.data_unit2 /*, R.id.descr*/ });
	        adapter.setViewBinder(mViewBinder);
	        setListAdapter(adapter);
		};
	}
	
	private SimpleCursorAdapter.ViewBinder mViewBinder  = new CheckBoxViewBinder();

	private class CheckBoxViewBinder implements SimpleCursorAdapter.ViewBinder {
		private static final String SHOW = "show";

		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if(cursor.getColumnName(columnIndex).equalsIgnoreCase(SHOW)) {
				((CheckBox)view.findViewById(R.id.checkbox)).setChecked(cursor.getInt(columnIndex) == 1);
				return true;
			}
			return false;
		}
		
	}

	private void UpdateTracksStat() {
		if(mThreadExecutor == null)
			mThreadExecutor = Executors.newSingleThreadExecutor(new SimpleThreadFactory("UpdateTracksStat"));
		dlgWait = Ut.ShowWaitDialog(this, 0);
		mThreadExecutor.execute(new Runnable(){

			public void run() {
				Cursor c = mPoiManager.getGeoDatabase().getTrackListCursor("");
				if(c != null){
					if (c.moveToFirst()) {
						Track tr = null;
						do {
							tr = mPoiManager.getTrack(c.getInt(3));
							if(tr != null){
								tr.Category = 0;
								tr.Activity = 0;
								final List<Track.TrackPoint> tps = tr.getPoints();
								if(tps.size() > 0){
									tr.Date = tps.get(0).date;
								}
								tr.CalculateStat();
								mPoiManager.updateTrack(tr);
							}
						} while (c.moveToNext());
					}
					c.close();
				}
				
				TrackListActivity.this.dlgWait.dismiss();
				Message.obtain(mHandler, R.id.about, 0, 0).sendToTarget();
			}});
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

		if(item.getItemId() == R.id.menu_importpoi) {
			startActivity((new Intent(this, ImportTrackActivity.class)));
			return true;
		} else if(item.getItemId() == R.id.menu_sort_name) {
			if(mSortOrder.contains("tracks.name")) {
				if(mSortOrder.contains("asc"))
					mSortOrder = "tracks.name desc";
				else
					mSortOrder = "tracks.name asc";
			} else {
				mSortOrder = "tracks.name asc";
			}
			((SimpleCursorAdapter) getListAdapter()).changeCursor(mPoiManager.getGeoDatabase().getTrackListCursor(mUnits == 0 ? getResources().getString(R.string.km) : getResources().getString(R.string.ml), mSortOrder));
		} else if(item.getItemId() == R.id.menu_sort_category) {
			if(mSortOrder.contains("activity.name")) {
				if(mSortOrder.contains("asc"))
					mSortOrder = "activity.name desc";
				else
					mSortOrder = "activity.name asc";
			} else {
				mSortOrder = "activity.name asc";
			}
			((SimpleCursorAdapter) getListAdapter()).changeCursor(mPoiManager.getGeoDatabase().getTrackListCursor(mUnits == 0 ? getResources().getString(R.string.km) : getResources().getString(R.string.ml), mSortOrder));
		} else if(item.getItemId() == R.id.menu_sort_date) {
			if(mSortOrder.contains("date")) {
				if(mSortOrder.contains("asc"))
					mSortOrder = "date desc";
				else
					mSortOrder = "date asc";
			} else {
				mSortOrder = "date asc";
			}
			((SimpleCursorAdapter) getListAdapter()).changeCursor(mPoiManager.getGeoDatabase().getTrackListCursor(mUnits == 0 ? getResources().getString(R.string.km) : getResources().getString(R.string.ml), mSortOrder));
		} else if(item.getItemId() == R.id.menu_join) {
			doJoinTracks();
		}

		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		menu.add(0, R.id.menu_gotopoi, 0, getText(R.string.menu_goto_track));
		menu.add(0, R.id.menu_stat, 0, getText(R.string.menu_stat));
		menu.add(0, R.id.menu_editpoi, 0, getText(R.string.menu_edit));
		menu.add(0, R.id.menu_deletepoi, 0, getText(R.string.menu_delete));
		menu.add(0, R.id.menu_exporttogpxpoi, 0, getText(R.string.menu_exporttogpx));
		menu.add(0, R.id.menu_exporttokmlpoi, 0, getText(R.string.menu_exporttokml));

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int id = (int) ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).id;

		if(item.getItemId() == R.id.menu_stat) {
			startActivity((new Intent(this, TrackStatActivity.class)).putExtra("id", id));
		} else if(item.getItemId() == R.id.menu_editpoi) {
			startActivity((new Intent(this, TrackActivity.class)).putExtra("id", id));
		} else if(item.getItemId() == R.id.menu_gotopoi) {
			setResult(RESULT_OK, (new Intent()).putExtra("trackid", id));
			finish();
		} else if(item.getItemId() == R.id.menu_deletepoi) {
			new AlertDialog.Builder(this) 
			.setTitle(R.string.app_name)
			.setMessage(getResources().getString(R.string.question_delete, getText(R.string.track)) )
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					mPoiManager.deleteTrack(id);
					((SimpleCursorAdapter) getListAdapter()).getCursor().requery();
				}
			}).setNegativeButton(R.string.no, null).create().show();

		} else if(item.getItemId() == R.id.menu_exporttogpxpoi) {
			DoExportTrackGPX(id);
		} else if(item.getItemId() == R.id.menu_exporttokmlpoi) {
			DoExportTrackKML(id);
		}

		return super.onContextItemSelected(item);
	}

	private void DoExportTrackKML(int id) {
		dlgWait = Ut.ShowWaitDialog(this, 0);
		final int trackid = id;
		if(mThreadExecutor == null)
			mThreadExecutor = Executors.newSingleThreadExecutor(new SimpleThreadFactory("DoExportTrackKML"));

		this.mThreadExecutor.execute(new Runnable() {
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
		dlgWait = Ut.ShowWaitDialog(this, 0);
		final int trackid = id;
		if(mThreadExecutor == null)
			mThreadExecutor = Executors.newSingleThreadExecutor(new SimpleThreadFactory("DoExportTrackGPX"));

		this.mThreadExecutor.execute(new Runnable() {
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
	protected void onListItemClick(ListView l, View v, int position, long id) {
		mPoiManager.setTrackChecked((int)id);

		final CheckBox ch = (CheckBox) v.findViewById(R.id.checkbox);
		ch.setChecked(!ch.isChecked());
		((SimpleCursorAdapter) getListAdapter()).getCursor().requery();
		
		super.onListItemClick(l, v, position, id);
	}

}
