package com.robert.maps.applib.downloader;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;

import com.robert.maps.applib.MainActivity;
import com.robert.maps.applib.tileprovider.TileSourceBase;
import com.robert.maps.applib.utils.Ut;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownloadListActivity extends ListActivity {
	ProgressDialog mProgressDialog;
	DownloadFile mDownloadFile;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

		FileDownloadListAdapter adapter = new FileDownloadListAdapter(this);
		
		setListAdapter(adapter);

		mProgressDialog = new ProgressDialog(FileDownloadListActivity.this);
		mProgressDialog.setIndeterminate(true);
		//mProgressDialog.setMax(0);
		//mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					if(mDownloadFile != null)
						mDownloadFile.cancel(true);
				} catch (Exception e) {
				}
			}
		});
}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final JSONObject json = (JSONObject) getListAdapter().getItem(position);
		
		mDownloadFile = new DownloadFile();
		try {
			mProgressDialog.setMessage(json.getString("listtitle"));
			mDownloadFile.execute(json.getString("source"), json.getString("filename"), json.getString("mapname"), json.optString("center", ""), json.optString("zoom", ""));
		} catch (JSONException e) {
			e.printStackTrace();
		}

		
		super.onListItemClick(l, v, position, id);
	}
	
	private class DownloadFile extends AsyncTask<String, Integer, String> {
		private String fileName;
		private String mapName;
		private String mapCenter = "";
		private String mapZoom = "";

		@Override
	    protected String doInBackground(String... sUrl) {
			String ret = "OK";
	        try {
	        	fileName = sUrl[1];
	        	mapName = sUrl[2];
	        	mapCenter = sUrl[3];
	        	mapZoom = sUrl[4];

	        	URL url = new URL(sUrl[0]);
	        	HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	            connection.setRequestProperty("Cache-Control", "no-cache");
	            //connection.setRequestProperty("User-Agent", "Java bot");
	            connection.connect();
	            // this will be useful so that you can show a typical 0-100% progress bar

	            // download the file
	            InputStream input = new BufferedInputStream(url.openStream());
	            //int fileLength = connection.getContentLength();
	            //Ut.d("Content-Length="+connection.getHeaderField("Content-Length"));
	            OutputStream output = new FileOutputStream(Ut.getRMapsMapsDir(FileDownloadListActivity.this).getAbsolutePath() + "/" + fileName);

	            byte data[] = new byte[1024];
	            long total = 0;
	            int count;
	            while ((count = input.read(data)) != -1) {
	                total += count;
	                // publishing the progress....
	                publishProgress((int) total); //, (int) fileLength);
	                output.write(data, 0, count);
	                
	                if(isCancelled()) {
	                	ret = null;
	                	break;
	                }
	            }

	            output.flush();
	            output.close();
	            input.close();
	            connection.disconnect();
	            
	            if(isCancelled()) {
	            	ret = null;
	            	final File file = new File(Ut.getRMapsMapsDir(FileDownloadListActivity.this).getAbsolutePath() + "/" + fileName);
	            	if(file.exists())
	            		file.delete();
	            }
	        } catch (Exception e) {
	        	ret = null;
	        	e.printStackTrace();
	        }
	        return ret;
	    }
	    
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        mProgressDialog.show();
	    }

	    @Override
	    protected void onProgressUpdate(Integer... progress) {
	        super.onProgressUpdate(progress);
	        //mProgressDialog.setMax(progress[1]/1024);
	        //mProgressDialog.setProgress(progress[0]/1024);
	        mProgressDialog.setMessage(String.format("%s: %dKB", mapName, (int)(progress[0]/1024)));
	    }

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			
			if(result != null) {
				final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(FileDownloadListActivity.this);
				final Editor editor = pref.edit();
				final String name = Ut.FileName2ID(fileName);
				editor.putBoolean(TileSourceBase.PREF_USERMAP_+name+"_enabled", true);
				editor.putString(TileSourceBase.PREF_USERMAP_+name+"_name", mapName);
				editor.putString(TileSourceBase.PREF_USERMAP_+name+"_projection", "1");
				final File folder = Ut.getRMapsMapsDir(FileDownloadListActivity.this);
				editor.putString(TileSourceBase.PREF_USERMAP_+name+"_baseurl", folder.getAbsolutePath() + "/" + fileName);
				editor.commit();
				
				mProgressDialog.dismiss();
	
				final Intent intent = new Intent(FileDownloadListActivity.this, MainActivity.class)
				.setAction("SHOW_MAP_ID")
				.putExtra("MapName", "usermap_"+name)
				.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				if(!mapCenter.equalsIgnoreCase(""))
					intent.putExtra("center", mapCenter);
				if(!mapZoom.equalsIgnoreCase(""))
					intent.putExtra("zoom", mapZoom);
				startActivity(intent);
				finish();
			}
		}
	    
	}
}
