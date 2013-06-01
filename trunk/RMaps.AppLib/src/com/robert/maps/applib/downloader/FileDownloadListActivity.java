package com.robert.maps.applib.downloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.R;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;

import com.robert.maps.applib.tileprovider.TileSourceBase;
import com.robert.maps.applib.utils.Ut;

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
		mProgressDialog.setIndeterminate(false);
		mProgressDialog.setMax(0);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getText(R.string.cancel), new DialogInterface.OnClickListener() {
			
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
			mProgressDialog.setMessage(json.getString("name"));
			mDownloadFile.execute(json.getString("source"), json.getString("filename"));
		} catch (JSONException e) {
		}

		
		super.onListItemClick(l, v, position, id);
	}
	
	private class DownloadFile extends AsyncTask<String, Integer, String> {
		private String fileName;
	    @Override
	    protected String doInBackground(String... sUrl) {
	        try {
	        	fileName = sUrl[1];
	            URL url = new URL(sUrl[0]);
	            URLConnection connection = url.openConnection();
	            connection.connect();
	            // this will be useful so that you can show a typical 0-100% progress bar
	            int fileLength = connection.getContentLength();

	            // download the file
	            InputStream input = new BufferedInputStream(url.openStream());
	            OutputStream output = new FileOutputStream(Ut.getRMapsMapsDir(FileDownloadListActivity.this).getAbsolutePath() + "/" + fileName);

	            byte data[] = new byte[1024];
	            long total = 0;
	            int count;
	            while ((count = input.read(data)) != -1) {
	                total += count;
	                // publishing the progress....
	                publishProgress((int) total, (int) fileLength);
	                output.write(data, 0, count);
	                
	                if(isCancelled())
	                	break;
	            }

	            output.flush();
	            output.close();
	            input.close();
	            
	            if(isCancelled()) {
	            	final File file = new File(Ut.getRMapsMapsDir(FileDownloadListActivity.this).getAbsolutePath() + "/" + fileName);
	            	if(file.exists())
	            		file.delete();
	            }
	        } catch (Exception e) {
	        }
	        return null;
	    }
	    
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        mProgressDialog.show();
	    }

	    @Override
	    protected void onProgressUpdate(Integer... progress) {
	        super.onProgressUpdate(progress);
	        mProgressDialog.setMax(progress[1]/1024);
	        mProgressDialog.setProgress(progress[0]/1024);
	    }

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			
			final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(FileDownloadListActivity.this);
			final Editor editor = pref.edit();
			final String name = Ut.FileName2ID(fileName);
			editor.putBoolean(TileSourceBase.PREF_USERMAP_+name+"_enabled", true);
			editor.putString(TileSourceBase.PREF_USERMAP_+name+"_name", fileName);
			editor.putString(TileSourceBase.PREF_USERMAP_+name+"_projection", "1");
			final File folder = Ut.getRMapsMapsDir(FileDownloadListActivity.this);
			editor.putString(TileSourceBase.PREF_USERMAP_+name+"_baseurl", folder.getAbsolutePath() + "/" + fileName);
			editor.commit();
			
			mProgressDialog.dismiss();
		}
	    
	}
}
