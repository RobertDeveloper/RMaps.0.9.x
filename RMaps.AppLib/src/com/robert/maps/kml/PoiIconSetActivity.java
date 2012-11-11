package com.robert.maps.kml;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;

import com.robert.maps.R;

public class PoiIconSetActivity extends Activity {
	private int indx[] = {R.drawable.poi, R.drawable.poiblue, R.drawable.poigreen, R.drawable.poiwhite, R.drawable.poiyellow};
	private GridView mGridInt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.poiiconset);

		mGridInt = (GridView) findViewById(R.id.GridInt);
		mGridInt.setAdapter(new AppsAdapter());
		
		mGridInt.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				//Toast.makeText(PoiIconSetActivity.this, "sel="+arg3, Toast.LENGTH_SHORT).show();
				setResult(RESULT_OK, (new Intent()).putExtra("iconid", indx[arg2]));
				finish();
			}
		});
	}

    public class AppsAdapter extends BaseAdapter {
        public AppsAdapter() {
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i;

            if (convertView == null) {
                i = new ImageView(PoiIconSetActivity.this);
                i.setScaleType(ImageView.ScaleType.FIT_CENTER);
                i.setLayoutParams(new GridView.LayoutParams(50, 50));
            } else {
                i = (ImageView) convertView;
            }

            i.setImageResource(indx[position]);

            return i;
        }


        public final int getCount() {
            return 5;
        }

        public final Object getItem(int position) {
            return null;
        }

        public final long getItemId(int position) {
            return indx[position];
        }
    }
	
}
