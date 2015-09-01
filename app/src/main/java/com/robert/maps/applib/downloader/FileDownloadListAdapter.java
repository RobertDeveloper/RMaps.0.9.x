package com.robert.maps.applib.downloader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.robert.maps.R;
import com.robert.maps.applib.utils.Ut;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class FileDownloadListAdapter extends BaseAdapter {
	private ArrayList<JSONObject> mArray = null;
	private Context mCtx;
	private LayoutInflater mInflater;
	
	public FileDownloadListAdapter(Context ctx) {
		super();
		this.mCtx = ctx;
		this.mArray = new ArrayList<JSONObject>();

		final String str = Ut.loadStringFromResourceFile(ctx, R.raw.downlodablemaps);
		try {
			JSONArray ja = new JSONArray(str);
			for(int i = 0; i < ja.length(); i++) {
				mArray.add(ja.getJSONObject(i));
			}
		} catch (JSONException e) {
		}
		
	}

	@Override
	public int getCount() {
		return mArray.size();
	}
	
	@Override
	public Object getItem(int position) {
		return mArray.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return mArray.get(position).optLong("id", 0);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        mInflater = (LayoutInflater) mCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view;

        if (convertView == null) {
            view = mInflater.inflate(R.layout.filedownloadlist_item, parent, false);
        } else {
            view = convertView;
        }

		final JSONObject json = mArray.get(position);
		view.setId(json.optInt("id", 0));
		((TextView) view.findViewById(R.id.name)).setText(json.optString("listtitle", "Name"));
		((TextView) view.findViewById(R.id.descr)).setText(json.optString("owner", ""));
		
		return view;
	}
	
}
