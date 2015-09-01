package com.robert.maps.applib.utils;

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
import com.robert.maps.applib.tileprovider.FSCacheProvider;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InternalCachePreference extends Preference {
	private Button btnClear;
	private ExecutorService mThreadExecutor = Executors.newSingleThreadExecutor(new SimpleThreadFactory("InternalCachePreference"));
	private ProgressDialog mProgressDialog;
	private SimpleInvalidationHandler mHandler;
	private FSCacheProvider mFSCacheProvider;

	public InternalCachePreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		setWidgetLayoutResource(R.layout.preference_widget_btn_clear);

		mHandler = new SimpleInvalidationHandler();
		final File folder = Ut.getRMapsCacheTilesDir(context);
		mFSCacheProvider = new FSCacheProvider(folder, mHandler);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		btnClear = (Button) view.findViewById(R.id.btnClear);
		btnClear.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mProgressDialog = Ut.ShowWaitDialog(getContext());
				mThreadExecutor.execute(new Runnable() {

					public void run() {
						mFSCacheProvider.clearCache();
						Message.obtain(InternalCachePreference.this.mHandler).sendToTarget();
						InternalCachePreference.this.mProgressDialog.dismiss();

					}
				});

			}
		});

	}

	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {

			InternalCachePreference.this.setSummary(String.format(InternalCachePreference.this.getContext().getString(R.string.pref_internalcache_summary),
					(int) mFSCacheProvider.getUsedCacheSpace() / 1024));

		}
	}

}
