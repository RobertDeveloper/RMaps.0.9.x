package com.robert.maps.kml;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
        	tr.CalculateStatFull();
        	
        	if (tr != null){
				final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        		final Track.Stat stat = tr.CalculateStatFull();
        		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        		final int units = Integer.parseInt(pref.getString("pref_units", "0"));

        		((TextView)findViewById(R.id.duration_unit)).setText(String.format("(%s-%s)", sdf.format(stat.Date1), sdf.format(stat.Date2)));

				sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
				sdf.applyPattern("HH:mm:ss");
        		
        		((TextView)findViewById(R.id.Name)).setText(tr.Name);
        		((TextView)findViewById(R.id.duration_data)).setText(sdf.format(new Date((long) (tr.Duration*1000))));
        		((TextView)findViewById(R.id.distance_data)).setText(String.format("%.2f", tr.Distance/1000));
        		((TextView)findViewById(R.id.points_data)).setText(String.format("%d", tr.Cnt));
        		((TextView)findViewById(R.id.avgspeed_data)).setText(String.format("%.1f", stat.AvgSpeed));
        		((TextView)findViewById(R.id.avgpace_data)).setText(String.format("%d:%02d", (int)(stat.AvgPace/60), (int)(stat.AvgPace - 60 * (int)(stat.AvgPace/60))));
        		((TextView)findViewById(R.id.maxspeed_data)).setText(String.format("%.1f", stat.MaxSpeed));
        		((TextView)findViewById(R.id.movetime_data)).setText(sdf.format(new Date((long) (stat.MoveTime))));
        		((TextView)findViewById(R.id.moveavgspeed_data)).setText(String.format("%.1f", stat.AvgMoveSpeed));
        		((TextView)findViewById(R.id.minele_data)).setText(String.format("%.1f", stat.MinEle));
        		((TextView)findViewById(R.id.maxele_data)).setText(String.format("%.1f", stat.MaxEle));
        		
        		((TextView)findViewById(R.id.distance_unit)).setText(R.string.km);
        		((TextView)findViewById(R.id.points_unit)).setText(R.string.blank);
        		((TextView)findViewById(R.id.avgspeed_unit)).setText(R.string.kmh);
        		((TextView)findViewById(R.id.avgpace_unit)).setText(R.string.minkm);
        		((TextView)findViewById(R.id.maxspeed_unit)).setText(R.string.kmh);
        		((TextView)findViewById(R.id.movetime_unit)).setText(R.string.blank);
        		((TextView)findViewById(R.id.moveavgspeed_unit)).setText(R.string.kmh);
        		((TextView)findViewById(R.id.minele_unit)).setText(R.string.m);
        		((TextView)findViewById(R.id.maxele_unit)).setText(R.string.m);
        	}
        }
	}


}
