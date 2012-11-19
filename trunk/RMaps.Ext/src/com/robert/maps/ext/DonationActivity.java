package com.robert.maps.ext;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class DonationActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.donationactivity);

		findViewById(R.id.buttonStart).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				try {
					final Intent intent = new Intent(Intent.ACTION_MAIN, null);

					intent.addCategory(Intent.CATEGORY_LAUNCHER);

					final ComponentName cn = new ComponentName("com.robert.maps", "com.robert.maps.applib.MainActivity");

					intent.setComponent(cn);

					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					startActivity(intent);

				} catch (ActivityNotFoundException e) {
					Toast.makeText(DonationActivity.this, R.string.message_noapp, Toast.LENGTH_LONG).show();
					try {
						startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=com.robert.maps")));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				DonationActivity.this.finish();
			}
		});
	}

}
