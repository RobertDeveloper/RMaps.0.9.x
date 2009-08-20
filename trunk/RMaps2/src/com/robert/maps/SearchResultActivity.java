package com.robert.maps;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.andnav.osm.views.util.StreamUtils;
import org.json.JSONObject;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class SearchResultActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.searchresult);

        // get and process search query here
        final Intent queryIntent = getIntent();
        final String queryAction = queryIntent.getAction();
        if (Intent.ACTION_SEARCH.equals(queryAction)) {
            doSearchQuery(queryIntent);
        }
}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

        final Intent queryIntent = getIntent();
        final String queryAction = queryIntent.getAction();
        if (Intent.ACTION_SEARCH.equals(queryAction)) {
            doSearchQuery(queryIntent);
        }
}

	private void doSearchQuery(Intent queryIntent) {
        final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);

		InputStream in = null;
		OutputStream out = null;

		try {
			in = new BufferedInputStream(new URL("http://ajax.googleapis.com/ajax/services/search/local?v=1.0&q=" + queryString + "&key=ABQIAAAAuGNgHVLd5kgLL0dg4gxBCRQpDmmpk8q921mRIPG4qgOvy0T09hQeXgriFfxGyJEsGXrlS9NOA5oQAA").openStream(), StreamUtils.IO_BUFFER_SIZE);

			final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
			out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
			StreamUtils.copy(in, out);
			out.flush();

			String str = dataStream.toString();
			JSONObject json = new JSONObject(str);
			TextView mQueryText = (TextView) findViewById(R.id.TextView01);
			mQueryText.setText(json.toString(2));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			StreamUtils.closeStream(in);
			StreamUtils.closeStream(out);
		}
	}

}
