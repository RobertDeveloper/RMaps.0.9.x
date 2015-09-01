package com.robert.maps.applib.kml;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import com.robert.maps.R;
import com.robert.maps.applib.kml.constants.PoiConstants;
import com.robert.maps.applib.utils.DistanceFormatter;
import com.robert.maps.applib.utils.Units;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TrackStatActivity extends Activity {
	private DistanceFormatter mDf;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDf = new DistanceFormatter(this);
		this.setContentView(R.layout.track_stat);

        Bundle extras = getIntent().getExtras();
        if(extras == null) extras = new Bundle();
        final int id = extras.getInt("id", PoiConstants.EMPTY_ID);
        
        if (id >= 0) {
        	final PoiManager mPoiManager = new PoiManager(this);
        	final Track tr = mPoiManager.getTrack(id);
        	
        	if (tr != null){
				final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        		final TrackStatHelper stat = tr.CalculateStatFull();
        		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        		final Units units = new Units(pref);

        		((TextView)findViewById(R.id.duration_unit)).setText(String.format("(%s-%s)", sdf.format(stat.Date1), sdf.format(stat.Date2)));

				sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
				sdf.applyPattern("HH:mm:ss");
        		
        		((TextView)findViewById(R.id.Name)).setText(tr.Name);
        		((TextView)findViewById(R.id.duration_data)).setText(sdf.format(new Date((long) (tr.Duration*1000))));
        		((TextView)findViewById(R.id.distance_data)).setText(String.format("%.2f", units.KM(tr.Distance/1000)));
        		((TextView)findViewById(R.id.points_data)).setText(String.format("%d", tr.Cnt));
        		((TextView)findViewById(R.id.avgspeed_data)).setText(mDf.formatSpeed2(stat.AvgSpeed)[0]);
        		((TextView)findViewById(R.id.avgpace_data)).setText(String.format("%d:%02d", (int)(units.MINKM(stat.AvgPace)/60), (int)(units.MINKM(stat.AvgPace) - 60 * (int)(units.MINKM(stat.AvgPace)/60))));
        		((TextView)findViewById(R.id.maxspeed_data)).setText(mDf.formatSpeed2(stat.MaxSpeed)[0]);
        		((TextView)findViewById(R.id.movetime_data)).setText(sdf.format(new Date((long) (stat.MoveTime))));
        		((TextView)findViewById(R.id.moveavgspeed_data)).setText(mDf.formatSpeed2(stat.AvgMoveSpeed)[0]);
        		((TextView)findViewById(R.id.minele_data)).setText(String.format("%.1f", units.M(stat.MinEle)));
        		((TextView)findViewById(R.id.maxele_data)).setText(String.format("%.1f", units.M(stat.MaxEle)));
        		
        		((TextView)findViewById(R.id.distance_unit)).setText(units.KM());
        		((TextView)findViewById(R.id.points_unit)).setText(R.string.blank);
        		((TextView)findViewById(R.id.avgspeed_unit)).setText(units.KMH());
        		((TextView)findViewById(R.id.avgpace_unit)).setText(units.MINKM());
        		((TextView)findViewById(R.id.maxspeed_unit)).setText(units.KMH());
        		((TextView)findViewById(R.id.movetime_unit)).setText(R.string.blank);
        		((TextView)findViewById(R.id.moveavgspeed_unit)).setText(units.KMH());
        		((TextView)findViewById(R.id.minele_unit)).setText(units.M());
        		((TextView)findViewById(R.id.maxele_unit)).setText(units.M());
        		
        		((ChartView) findViewById(R.id.chart)).setTrack(tr);
        	}
        }
	}


}
