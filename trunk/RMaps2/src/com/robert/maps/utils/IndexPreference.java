package com.robert.maps.utils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

public class IndexPreference extends Preference {
    private Button btnClear;
    private Context mCtx;
    private File mDbFile;
    private ExecutorService mThreadExecutor = Executors.newSingleThreadExecutor();
    private ProgressDialog mProgressDialog;
    private SimpleInvalidationHandler mHandler;

	public IndexPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mCtx = context;
		setWidgetLayoutResource(R.layout.preference_widget_btn_clear);
		final File folder = Ut.getRMapsMainDir(mCtx, "data");
		mDbFile = new File(folder.getAbsolutePath()+"/index.db");
		setSummary(String.format(mCtx.getString(R.string.pref_index_summary), (int) mDbFile
				.length() / 1024));
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
						if(IndexPreference.this.mDbFile.exists())
							IndexPreference.this.mDbFile.delete();

						Message.obtain(IndexPreference.this.mHandler).sendToTarget();
						IndexPreference.this.mProgressDialog.dismiss();
					}});

			}
		});

	}

	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {

			IndexPreference.this.setSummary(String.format(IndexPreference.this.mCtx
					.getString(R.string.pref_index_summary), (int) IndexPreference.this.mDbFile
					.length() / 1024));

		}
	}

}
