package com.robert.maps.kml.XMLparser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.robert.maps.kml.PoiManager;
import com.robert.maps.kml.Track;

public class KmlTrackParser extends DefaultHandler {
	private StringBuilder builder;
	private PoiManager mPoiManager;
	private Track mTrack;
	private String [] mStrArray;
	private String [] mStrArray2;
	private boolean mItIsTrack;

	private static final String Placemark = "Placemark";
	private static final String LineString = "LineString";
	private static final String NAME = "name";
	private static final String coordinates = "coordinates";
	private static final String description = "description";



	public KmlTrackParser(PoiManager poiManager) {
		super();
		builder = new StringBuilder();
		mPoiManager = poiManager;
		mTrack = new Track();
		mItIsTrack = false;
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
		if(localName.equalsIgnoreCase(Placemark)){
			mTrack = new Track();
			mItIsTrack = false;
		}
		super.startElement(uri, localName, name, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		if(localName.equalsIgnoreCase(Placemark)){
			if(mItIsTrack){
				if(mTrack.Name.equalsIgnoreCase("")) mTrack.Name = "Track";
				mPoiManager.updateTrack(mTrack);
			}
		}
		else if(localName.equalsIgnoreCase(NAME))
			mTrack.Name = builder.toString().trim();
		else if(localName.equalsIgnoreCase(description))
			mTrack.Descr = builder.toString().trim();
		else if(localName.equalsIgnoreCase(coordinates)){
			mStrArray = builder.toString().trim().split("\n");
			if(mStrArray.length < 2)
				mStrArray = builder.toString().trim().split(" ");
			for(int i = 0; i < mStrArray.length; i++){
				if(!mStrArray[i].trim().equals("")){
					mStrArray2 = mStrArray[i].trim().split(",");
					mTrack.AddTrackPoint();
					mTrack.LastTrackPoint.lat = Double.parseDouble(mStrArray2[1]);
					mTrack.LastTrackPoint.lon = Double.parseDouble(mStrArray2[0]);
					if(mStrArray2.length > 2)
						try {
							mTrack.LastTrackPoint.alt = Double.parseDouble(mStrArray2[2]);
						} catch (NumberFormatException e) {
							try {
								mTrack.LastTrackPoint.alt = (double)Integer.parseInt(mStrArray2[2]);
							} catch (NumberFormatException e1) {
								e1.printStackTrace();
							}
						}
				}
			}
		}
		else if(localName.equalsIgnoreCase(LineString))
			mItIsTrack = true;
		super.endElement(uri, localName, name);
	}

}
