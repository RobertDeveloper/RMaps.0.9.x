package com.robert.maps.applib.downloader;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.robert.maps.applib.R;

public class FileDownloadListAdapter extends BaseAdapter {
	private ArrayList<JSONObject> mArray = null;
	private Context mCtx;
	private LayoutInflater mInflater;
	
	public FileDownloadListAdapter(Context ctx) {
		super();
		this.mCtx = ctx;
		this.mArray = new ArrayList<JSONObject>();

		JSONObject j = null;
		try {
			j = new JSONObject("{\"name\":\"hello\",\"descr\":\"sdfsdfsdf234234\"}");
			mArray.add(j);
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
		((TextView) view.findViewById(R.id.name)).setText(json.optString("name", "name"));
		((TextView) view.findViewById(R.id.descr)).setText(json.optString("descr", "descr"));
		
		return view;
	}
	
}
