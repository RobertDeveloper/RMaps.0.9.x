package com.robert.maps.kml;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.openintents.filemanager.FileManagerActivity;
import org.openintents.filemanager.util.FileUtils;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.robert.maps.R;
import com.robert.maps.kml.XMLparser.GpxPoiParser;
import com.robert.maps.kml.XMLparser.KmlPoiParser;
import com.robert.maps.utils.Ut;

public class ImportPoiActivity extends Activity {
	EditText mFileName;
	Spinner mSpinner;
	private PoiManager mPoiManager;

	private ProgressDialog dlgWait;
	protected ExecutorService mThreadPool = Executors.newFixedThreadPool(2);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences settings = getPreferences(Activity.MODE_PRIVATE);
		this.setContentView(R.layout.importpoi);

		if (mPoiManager == null)
			mPoiManager = new PoiManager(this);

		mFileName = (EditText) findViewById(R.id.FileName);
		mFileName.setText(settings.getString("IMPORT_POI_FILENAME", Ut.getRMapsImportDir(this).getAbsolutePath()));

		mSpinner = (Spinner) findViewById(R.id.spinnerCategory);
		Cursor c = mPoiManager.getGeoDatabase().getPoiCategoryListCursor();
		startManagingCursor(c);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, c,
				new String[] { "name" }, new int[] { android.R.id.text1 });
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(adapter);

		((Button) findViewById(R.id.SelectFileBtn))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doSelectFile();
			}
		});
		((Button) findViewById(R.id.ImportBtn))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doImportPOI();
			}
		});
		((Button) findViewById(R.id.discardButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ImportPoiActivity.this.finish();
			}
		});
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
		return null;
	}

	protected void doSelectFile() {
		Intent intent = new Intent(this, FileManagerActivity.class);
		intent.setData(Uri.parse(mFileName.getText().toString()));
		startActivityForResult(intent, R.id.ImportBtn);
/*
//		Intent intent = new Intent("org.openintents.action.PICK_FILE");
//		startActivityForResult(intent, 1);
//
		String fileName = mFileName.getText().toString();

		Intent intent = new Intent("org.openintents.action.PICK_FILE");

		// Construct URI from file name.
		intent.setData(Uri.parse("file://" + fileName));

		// Set fancy title and button (optional)
//		intent.putExtra(FileManagerIntents.EXTRA_TITLE, getString(R.string.open_title));
//		intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, getString(R.string.open_button));

		try {
			startActivityForResult(intent, R.id.ImportBtn);
		} catch (ActivityNotFoundException e) {
			// No compatible file manager was found.
			Toast.makeText(this, "No compatible file manager found",
					Toast.LENGTH_SHORT).show();
		}
*/
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case R.id.ImportBtn:
			if (resultCode == RESULT_OK && data != null) {
				// obtain the filename
				String filename = Uri.decode(data.getDataString());
				if (filename != null) {
					// Get rid of URI prefix:
					if (filename.startsWith("file://")) {
						filename = filename.substring(7);
					}

					mFileName.setText(filename);
				}

			}
			break;
		}
	}

	private void doImportPOI() {
		File file = new File(mFileName.getText().toString());

		if(!file.exists()){
			Toast.makeText(this, "No such file", Toast.LENGTH_LONG).show();
			return;
		}

		showDialog(R.id.dialog_wait);

		this.mThreadPool.execute(new Runnable() {
			public void run() {
				int CategoryId = (int)mSpinner.getSelectedItemId();
				File file = new File(mFileName.getText().toString());

				SAXParserFactory fac = SAXParserFactory.newInstance();
				SAXParser parser = null;
				try {
					parser = fac.newSAXParser();
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if(parser != null){
					mPoiManager.beginTransaction();
					Ut.dd("Start parsing file " + file.getName());
					try {
						if(FileUtils.getExtension(file.getName()).equalsIgnoreCase(".kml"))
							parser.parse(file, new KmlPoiParser(mPoiManager, CategoryId));
						else if(FileUtils.getExtension(file.getName()).equalsIgnoreCase(".gpx"))
							parser.parse(file, new GpxPoiParser(mPoiManager, CategoryId));

						mPoiManager.commitTransaction();
					} catch (SAXException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						mPoiManager.rollbackTransaction();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						mPoiManager.rollbackTransaction();
					} catch (IllegalStateException e) {
					} catch (OutOfMemoryError e) {
						Ut.w("OutOfMemoryError");
						mPoiManager.rollbackTransaction();
					}
					Ut.dd("Pois commited");
				}


				/*
				SimpleXML xml = null;

				try {
					xml = SimpleXML.loadXml(new FileInputStream(file));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

				if (xml != null) {
					if(FileUtils.getExtension(file.getName()).equalsIgnoreCase(".kml")){
						SimpleXML document = xml.getNodeByPath("document", false);
						Vector<SimpleXML> placemarks = document.getChildren("placemark");
						if (placemarks != null && placemarks.size() > 0) {
							for (SimpleXML placemark : placemarks) {
								PoiPoint poi = new PoiPoint();
								poi.CategoryId = CategoryId;
								poi.Title = placemark.getNodeText("name");
								poi.Descr = placemark.getNodeText("description");
								SimpleXML point = placemark.getNodeByPath("point", false);
								if(point != null){
									String [] f = point.getNodeText("coordinates").split(",");
									poi.GeoPoint = GeoPoint.from2DoubleString(f[1], f[0]);
								}
								if(poi.Title.equalsIgnoreCase("")) poi.Title = "POI";
								mPoiManager.updatePoi(poi);
							}
						}
					}else if(FileUtils.getExtension(file.getName()).equalsIgnoreCase(".gpx")){
						Vector<SimpleXML> wpts = xml.getChildren("wpt");
						if (wpts != null && wpts.size() > 0) {
							for (SimpleXML wpt : wpts) {
								PoiPoint poi = new PoiPoint();
								poi.CategoryId = CategoryId;
								poi.GeoPoint = GeoPoint.from2DoubleString(wpt.getAttr("lat"), wpt.getAttr("lon"));
								poi.Title = wpt.getNodeText("name");
								poi.Descr = wpt.getNodeText("cmt");
								if(poi.Title.equalsIgnoreCase(""))
									poi.Title = "POI";
								if(poi.Descr.equalsIgnoreCase(""))
									poi.Descr = wpt.getNodeText("desc");
								mPoiManager.updatePoi(poi);
							}
						}
					}
				}
				*/
				/*
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = null;
				try {
					db = dbf.newDocumentBuilder();
				} catch (ParserConfigurationException e1) {
					e1.printStackTrace();
				}
				Document doc = null;
				try {
					Ut.dd("Start parsing file " + file.getName());
					doc = db.parse(file);
					Ut.dd("Parsing file done");
					mPoiManager.beginTransaction();
					// gpx
					NodeList wpts = doc.getDocumentElement().getElementsByTagName("wpt");
					Node wpt = null;
					NamedNodeMap attr = null;
					NodeList wptchild = null;
					Node node = null;
					int cnt = 0;

					for (int i = 0; i < wpts.getLength(); i++) {
						//Ut.dd("Loading poi "+i);
						wpt = wpts.item(i);
						PoiPoint poi = new PoiPoint();
						poi.CategoryId = CategoryId;

						attr = wpt.getAttributes();
						poi.GeoPoint = GeoPoint.from2DoubleString(attr.getNamedItem("lat").getNodeValue(), attr.getNamedItem("lon").getNodeValue());
						wptchild = wpt.getChildNodes();

						for (int j = 0; j < wptchild.getLength(); j++) {
							node = wptchild.item(j);

							if (node.getNodeName().equalsIgnoreCase("name"))
								poi.Title = node.getFirstChild().getNodeValue().trim();
							else if (node.getNodeName().equalsIgnoreCase("cmt"))
								poi.Descr = node.getFirstChild().getNodeValue().trim();
							else if (node.getNodeName().equalsIgnoreCase("desc"))
								poi.Descr = node.getFirstChild().getNodeValue().trim();
						}

						if (poi.Title.equalsIgnoreCase(""))
							poi.Title = "POI";
						mPoiManager.updatePoi(poi);
						cnt++;
					}


					Ut.dd("Loading done: "+cnt);
					mPoiManager.commitTransaction();
					Ut.dd("Pois commited");
					*/
					/*
					// kml
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
					Ut.dd("Loading done");
					*/
				/*

				} catch (SAXException e1) {
					e1.printStackTrace();
					mPoiManager.rollbackTransaction();
				} catch (IOException e1) {
					e1.printStackTrace();
					mPoiManager.rollbackTransaction();
				}
				*/

				dlgWait.dismiss();
				ImportPoiActivity.this.finish();
			};
		});

	}


	@Override
	protected void onDestroy() {
		mPoiManager.FreeDatabases();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("IMPORT_POI_FILENAME", mFileName.toString());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		SharedPreferences uiState = getPreferences(0);
		SharedPreferences.Editor editor = uiState.edit();
		editor.putString("IMPORT_POI_FILENAME", mFileName.getText().toString());
		editor.commit();
		super.onPause();
	}

}
