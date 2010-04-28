package com.robert.maps.kml;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openintents.filemanager.IconifiedText;
import org.openintents.filemanager.IconifiedTextListAdapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;

import com.robert.maps.R;

public class PoiListActivity extends ListActivity {
	private PoiManager mPoiManager;
	List<IconifiedText> mListPoi = new ArrayList<IconifiedText>();
	static final public int MESSAGE_SHOW_DIRECTORY_CONTENTS = 500;	// List of contents is ready, obj = DirectoryContents
    private Handler currentHandler;
	protected ExecutorService mThreadPool = Executors.newFixedThreadPool(1);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		currentHandler = new Handler() {
			public void handleMessage(Message msg) {
				PoiListActivity.this.handleMessage(msg);
			}
		};

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		registerForContextMenu(getListView());

		//getListView().setEmptyView(findViewById(R.id.empty));
		getListView().setTextFilterEnabled(true);
		getListView().requestFocus();
		getListView().requestFocusFromTouch();

		mPoiManager = new PoiManager(this);
		FillData();
	}

	protected void handleMessage(Message msg) {
   	 switch (msg.what) {
	 case MESSAGE_SHOW_DIRECTORY_CONTENTS:
		 showContents();
		 break;
	 }
	}

	private void showContents() {
		IconifiedTextListAdapter itla = new IconifiedTextListAdapter(this);
		itla.setListItems(mListPoi, getListView().hasTextFilter());
		setListAdapter(itla);
		getListView().setTextFilterEnabled(true);
		setProgressBarIndeterminateVisibility(false);
	}

	@Override
	protected void onStart() {
		mPoiManager = new PoiManager(this);
		super.onStart();
	}

	@Override
	protected void onStop() {
		mPoiManager.FreeDatabases();
		mPoiManager = null;
		super.onStop();
	}

	@Override
	protected void onResume() {
		FillData();
		super.onResume();
	}

	private void FillData() {
		mListPoi.clear();
		setProgressBarIndeterminateVisibility(true);
		setListAdapter(null);

		mThreadPool.execute(new Runnable(){

			public void run() {
				mListPoi = mPoiManager.getPoiListLikeIconifiedText();

				Message msg = currentHandler.obtainMessage(MESSAGE_SHOW_DIRECTORY_CONTENTS);
				msg.sendToTarget();
			}});

//		Cursor c = mPoiManager.getGeoDatabase().getPoiListCursor();
//        startManagingCursor(c);
//
//        ListAdapter adapter = new SimpleCursorAdapter(this,
//                android.R.layout.simple_list_item_2, c,
//                        new String[] { "name", "descr" },
//                        new int[] { android.R.id.text1, android.R.id.text2 });
//        setListAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.poilist_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch(item.getItemId()){
		case R.id.menu_addpoi:
			startActivity((new Intent(this, PoiActivity.class)));
			return true;
		case R.id.menu_categorylist:
			startActivity((new Intent(this, PoiCategoryListActivity.class)));
			return true;
		case R.id.menu_importpoi:
			startActivity((new Intent(this, ImportPoiActivity.class)));
			return true;
		case R.id.menu_deleteall:
			showDialog(R.id.menu_deleteall);
			return true;
		}

		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.menu_deleteall:
			return new AlertDialog.Builder(this)
				//.setIcon(R.drawable.alert_dialog_icon)
				.setTitle(R.string.warning_delete_all_poi)
				.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									mPoiManager.DeleteAllPoi();
									FillData();
								}
							}).setNegativeButton(android.R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

									/* User clicked Cancel so do some stuff */
								}
							}).create();
		}
		;

		return super.onCreateDialog(id);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		int pointid = (int) ((AdapterView.AdapterContextMenuInfo)menuInfo).id;
		PoiPoint poi = mPoiManager.getPoiPoint(pointid);

		menu.add(0, R.id.menu_gotopoi, 0, getText(R.string.menu_goto));
		menu.add(0, R.id.menu_editpoi, 0, getText(R.string.menu_edit));
		if(poi.Hidden)
			menu.add(0, R.id.menu_show, 0, getText(R.string.menu_show));
		else
			menu.add(0, R.id.menu_hide, 0, getText(R.string.menu_hide));
		menu.add(0, R.id.menu_deletepoi, 0, getText(R.string.menu_delete));

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int pointid = (int) ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).id;
		PoiPoint poi = mPoiManager.getPoiPoint(pointid);

		switch(item.getItemId()){
		case R.id.menu_editpoi:
			startActivity((new Intent(this, PoiActivity.class)).putExtra("pointid", pointid));
			break;
		case R.id.menu_gotopoi:
			setResult(RESULT_OK, (new Intent()).putExtra("pointid", pointid));
			finish();
			break;
		case R.id.menu_deletepoi:
			mPoiManager.deletePoi(pointid);
			FillData();
	        break;
		case R.id.menu_hide:
			poi.Hidden = true;
			mPoiManager.updatePoi(poi);
	        break;
		case R.id.menu_show:
			poi.Hidden = false;
			mPoiManager.updatePoi(poi);
	        break;
		}

		return super.onContextItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
	}

}
