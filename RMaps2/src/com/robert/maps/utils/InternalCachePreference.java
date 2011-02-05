package com.robert.maps.utils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.andnav.osm.views.util.OpenStreetMapTileCache;
import org.andnav.osm.views.util.OpenStreetMapTileFilesystemProvider;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.robert.maps.R;

public class InternalCachePreference extends Preference {
    private Button btnClear;
    private Context mCtx;
    private File mDbFile;
    private OpenStreetMapTileFilesystemProvider mFSTileProvider;
    private ExecutorService mThreadExecutor = Executors.newSingleThreadExecutor();
    private ProgressDialog mProgressDialog;
    private SimpleInvalidationHandler mHandler;

	public InternalCachePreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		setWidgetLayoutResource(R.layout.preference_widget_btn_clear);

		mCtx = context;
		this.mFSTileProvider = new OpenStreetMapTileFilesystemProvider(context, 4 * 1024 * 1024, new OpenStreetMapTileCache(), null); // 4MB FSCache
		mDbFile = context.getDatabasePath("osmaptilefscache_db");

		setSummary(String.format(mCtx.getString(R.string.pref_internalcache_summary), (int) (mDbFile
				.length() + mFSTileProvider.getCurrentFSCacheByteSize())/ 1024));

		mHandler = new SimpleInvalidationHandler();
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		btnClear = (Button) view.findViewById(R.id.btnClear);
		btnClear.setOnClickListener(new OnClickListener() {
			// @Override
			public void onClick(View v) {
				mProgressDialog = Ut.ShowWaitDialog(mCtx, 0);
				mThreadExecutor.execute(new Runnable(){

					public void run() {
						InternalCachePreference.this.mFSTileProvider.clearCurrentFSCache();
						Message.obtain(InternalCachePreference.this.mHandler).sendToTarget();
						InternalCachePreference.this.mProgressDialog.dismiss();

					}});

			}
		});

	}

	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {

			InternalCachePreference.this
			.setSummary(String
					.format(
							InternalCachePreference.this.mCtx
									.getString(R.string.pref_internalcache_summary),
							(int) (InternalCachePreference.this.mDbFile.length() + InternalCachePreference.this.mFSTileProvider
									.getCurrentFSCacheByteSize()) / 1024));

		}
	}

}
