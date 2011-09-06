package com.robert.maps.kml;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.robert.maps.R;
import com.robert.maps.kml.constants.PoiConstants;

public class TrackStatActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.track_stat);

        Bundle extras = getIntent().getExtras();
        if(extras == null) extras = new Bundle();
        final int id = extras.getInt("id", PoiConstants.EMPTY_ID);
        
        if (id >= 0) {
        	final PoiManager mPoiManager = new PoiManager(this);
        	final Track tr = mPoiManager.getTrack(id);
        	
        	if (tr != null){
        		((TextView)findViewById(R.id.Name)).setText(tr.Name);
				final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
				sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        		((TextView)findViewById(R.id.duration_data)).setText(sdf.format(new Date((long) (tr.Duration*1000))));
        		((TextView)findViewById(R.id.distance_data)).setText(String.format("%.3f", tr.Distance/1000));
        	}
        }
	}


}
