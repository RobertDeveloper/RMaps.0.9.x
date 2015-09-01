package com.robert.maps.applib.kml.XMLparser;

import com.robert.maps.applib.kml.PoiManager;
import com.robert.maps.applib.kml.Track;
import com.robert.maps.applib.utils.Ut;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Date;

public class GpxTrackParser extends DefaultHandler {
	private StringBuilder builder;
	private PoiManager mPoiManager;
	private Track mTrack;
	private String mTrackName = "Track";
	private Date mTrackTime = new Date();

	private static final String TRK = "trk";
	private static final String LAT = "lat";
	private static final String LON = "lon";
	private static final String NAME = "name";
	private static final String CMT = "cmt";
	private static final String DESC = "desc";
	private static final String POINT = "trkpt";
	private static final String ELE = "ele";
	private static final String TIME = "time";
	private static final String EMPTY = "";



	public GpxTrackParser(PoiManager poiManager) {
		super();
		builder = new StringBuilder();
		mPoiManager = poiManager;
		mTrack = null;
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
		if(localName.equalsIgnoreCase(TRK)) {
			mTrack = new Track();
			mTrack.Date = mTrackTime;
		} else if(localName.equalsIgnoreCase(POINT)){
			mTrack.AddTrackPoint();
			mTrack.LastTrackPoint.lat = Double.parseDouble(attributes.getValue(LAT));
			mTrack.LastTrackPoint.lon = Double.parseDouble(attributes.getValue(LON));
		}

		super.startElement(uri, localName, name, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		if(localName.equalsIgnoreCase(TRK)){
			if(mTrack.Name.equalsIgnoreCase(EMPTY)) 
				mTrack.Name = mTrackName;
			mTrack.CalculateStat();
			mPoiManager.updateTrack(mTrack);
		} else if(localName.equalsIgnoreCase(NAME)) {
			if(mTrack != null)
				mTrack.Name = builder.toString().trim();
			else
				mTrackName = builder.toString().trim();
		} else if(localName.equalsIgnoreCase(CMT)) {
			if(mTrack != null)
				mTrack.Descr = builder.toString().trim();
		} else if(localName.equalsIgnoreCase(DESC)) {
			if(mTrack != null && mTrack.Descr.equals(EMPTY))
				mTrack.Descr = builder.toString().trim();
		} else if (localName.equalsIgnoreCase(ELE)) {
			if(mTrack.LastTrackPoint != null && !builder.toString().equalsIgnoreCase(EMPTY))
				mTrack.LastTrackPoint.alt = Double.parseDouble(builder.toString().trim());
		} else if (localName.equalsIgnoreCase(TIME)) {
			if(mTrack != null) {
				if(mTrack.LastTrackPoint != null && !builder.toString().equalsIgnoreCase(EMPTY))
					mTrack.LastTrackPoint.date = Ut.ParseDate(builder.toString().trim());
			} else {
				mTrackTime = Ut.ParseDate(builder.toString().trim());
			}
		}

		super.endElement(uri, localName, name);
	}

}
