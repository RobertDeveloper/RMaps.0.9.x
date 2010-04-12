package com.robert.maps.kml;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.andnav.osm.util.GeoPoint;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.robert.maps.R;
import com.robert.maps.utils.Ut;

public class PoiListActivity extends ListActivity {
	private PoiManager mPoiManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        registerForContextMenu(getListView());
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
		Cursor c = mPoiManager.getGeoDatabase().getPoiListCursor();
        startManagingCursor(c);

        ListAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_2, c,
                        new String[] { "name", "descr" },
                        new int[] { android.R.id.text1, android.R.id.text2 });
        setListAdapter(adapter);
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
			doImportPOI();
			return true;
		}

		return true;
	}

	private void doImportPOI() {
		Ut.getRMapsFolder("import", false);
		File file = new File("/sdcard/rmaps/import/1.kml");

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
		Document doc = null;
		try {
			doc = db.parse(file);

			NodeList nl = doc.getDocumentElement().getElementsByTagName("Placemark");
			NodeList plk = null;
			Node node = null;
			for(int i = 0; i < nl.getLength(); i++){
				Ut.dd("placemark"+i);
				plk = nl.item(i).getChildNodes();
				PoiPoint poi = new PoiPoint();
				for(int j = 0; j < plk.getLength(); j++){
					node = plk.item(j); //node.getLocalName() node.getNodeType()

					if(node.getNodeName().equalsIgnoreCase("name"))
						poi.Title = node.getFirstChild().getNodeValue().trim();
					else if(node.getNodeName().equalsIgnoreCase("description"))
						poi.Descr = node.getFirstChild().getNodeValue().trim();
					else if(node.getNodeName().equalsIgnoreCase("Point")){
						NodeList crd = node.getChildNodes();
						for(int k = 0; k < crd.getLength(); k++){
							if(crd.item(k).getNodeName().equalsIgnoreCase("coordinates")){
								String [] f = crd.item(k).getFirstChild().getNodeValue().split(",");
								poi.GeoPoint = GeoPoint.from2DoubleString(f[1], f[0]);
							}
						}
					}

				}
				if(poi.Title.equalsIgnoreCase("")) poi.Title = "POI";
				mPoiManager.updatePoi(poi);
			}
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

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
			FillData();
	        break;
		case R.id.menu_show:
			poi.Hidden = false;
			mPoiManager.updatePoi(poi);
			FillData();
	        break;
		}

		return super.onContextItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
	}
}
