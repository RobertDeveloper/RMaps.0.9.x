package com.robert.maps.applib.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.robert.maps.R;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnlineCachePreference extends Preference {
	private String mID;
    private Button btnClear;
    private ExecutorService mThreadExecutor = Executors.newSingleThreadExecutor(new SimpleThreadFactory("OnlineCachePreference"));
    private ProgressDialog mProgressDialog;
    private SimpleInvalidationHandler mHandler;


	public OnlineCachePreference(Context context, String aID) {
		super(context);
		mID = aID;
		setWidgetLayoutResource(R.layout.preference_widget_btn_clear);
		setTitle(R.string.pref_onlinecacheclear);
		setSummaryStr();
		mHandler = new SimpleInvalidationHandler();
	}
	
	void setSummaryStr() {
		final String name = mID+".sqlitedb";
		long size = 0;
		final File folder = Ut.getRMapsMainDir(getContext(), "cache");
		if(folder != null) {
			File[] files = folder.listFiles();
			if(files != null) {
				for (int i = 0; i < files.length; i++) {
					if(files[i].getName().startsWith(name)) {
						size += files[i].length();
					}
				}
			}
		}

		setSummary(mID + String.format(getContext().getString(R.string.pref_onlinecacheclear_summary), (int) size / 1024));
	}
	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		btnClear = (Button) view.findViewById(R.id.btnClear);
		btnClear.setOnClickListener(new OnClickListener() {
			// @Override
			public void onClick(View v) {
				mProgressDialog = Ut.ShowWaitDialog(getContext(), 0);
				mThreadExecutor.execute(new Runnable(){

					public void run() {
						final String name = mID+".sqlitedb";
						final File folder = Ut.getRMapsMainDir(getContext(), "cache");
						if(folder != null) {
							File[] files = folder.listFiles();
							if(files != null) {
								for (int i = 0; i < files.length; i++) {
									if(files[i].getName().startsWith(name)) {
										files[i].delete();
									}
								}
							}
						}

						Message.obtain(OnlineCachePreference.this.mHandler).sendToTarget();
						OnlineCachePreference.this.mProgressDialog.dismiss();
					}});

			}
		});

	}

	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {

			OnlineCachePreference.this.setSummaryStr();
		}
	}

}
