package com.robert.maps.kml.XMLparser;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.Track;
import com.robert.maps.utils.Ut;

public class GpxTrackParser extends DefaultHandler {
	private StringBuilder builder;
	private PoiManager mPoiManager;
	private Track mTrack;
	private final SimpleDateFormat format = new SimpleDateFormat(); 

	private static final String TRK = "trk";
	private static final String LAT = "lat";
	private static final String LON = "lon";
	private static final String NAME = "name";
	private static final String CMT = "cmt";
	private static final String DESC = "desc";
	private static final String POINT = "trkpt";
	private static final String ELE = "ele";
	private static final String TIME = "time";



	public GpxTrackParser(PoiManager poiManager) {
		super();
		builder = new StringBuilder();
		mPoiManager = poiManager;
		mTrack = new Track();
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		builder. append(ch, start, length);
		super.characters(ch, start, length);
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes)
			throws SAXException {
		builder.delete(0, builder.length());
		if(localName.equalsIgnoreCase(TRK))
			mTrack = new Track();
		else if(localName.equalsIgnoreCase(POINT)){
			mTrack.AddTrackPoint();
			mTrack.LastTrackPoint.lat = Double.parseDouble(attributes.getValue(LAT));
			mTrack.LastTrackPoint.lon = Double.parseDouble(attributes.getValue(LON));
		}

		super.startElement(uri, localName, name, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		if(localName.equalsIgnoreCase(TRK)){
			if(mTrack.Name.equalsIgnoreCase("")) mTrack.Name = "Track";
			mTrack.CalculateStat();
			mPoiManager.updateTrack(mTrack);
		}
		else if(localName.equalsIgnoreCase(NAME))
			mTrack.Name = builder.toString().trim();
		else if(localName.equalsIgnoreCase(CMT))
			mTrack.Descr = builder.toString().trim();
		else if(localName.equalsIgnoreCase(DESC)){
			if(mTrack.Descr.equals(""))
				mTrack.Descr = builder.toString().trim();
		}
		else if (localName.equalsIgnoreCase(ELE))
			mTrack.LastTrackPoint.alt = Double.parseDouble(builder.toString().trim());
		else if (localName.equalsIgnoreCase(TIME)) {
			Ut.dd(builder.toString().trim());
			try {
				mTrack.LastTrackPoint.date = format.parse(builder.toString().trim());
				Ut.dd(mTrack.LastTrackPoint.date.toString());
				Ut.dd(mTrack.LastTrackPoint.date.toGMTString());
			} catch (ParseException e) {
				Ut.dd("bad format!!!!");
			}
		}

		super.endElement(uri, localName, name);
	}

}
