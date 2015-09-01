package com.robert.maps.applib.tileprovider;


public class TileURLGeneratorOrdnanceSurveyMap extends TileURLGeneratorBase {
	private static final int OpenSpaceLayersArray[] = {2500, 1000, 500, 200, 100, 50, 25, 10, 5, 2, 1};
	private static final int OpenSpaceUpperBoundArray[] = { 2, 5, 10, 25 , 50, 100, 200, 500, 1000, 2000, 4000};
	
	private int mZoomMinLevel;
	
	public TileURLGeneratorOrdnanceSurveyMap(String mName, final int aZoomMinLevel) {
		super(mName);
		mZoomMinLevel = aZoomMinLevel;
	}

	@Override
	public String Get(int x, int y, int z) {
		final int million = 1000000 / OpenSpaceUpperBoundArray[z - mZoomMinLevel];
		final int size = OpenSpaceLayersArray[z-mZoomMinLevel] < 5 ? 250 : 200;
		return new StringBuilder()
		.append(mName)
		.append("LAYERS=").append(OpenSpaceLayersArray[z-mZoomMinLevel])
		.append("&SRS=EPSG%3A27700&BBOX=")
		.append(million*x)
		.append(",")
		.append(million*(OpenSpaceUpperBoundArray[z - mZoomMinLevel]-1-y))
		.append(",")
		.append(million*(1+x))
		.append(",")
		.append(million*(1+(OpenSpaceUpperBoundArray[z - mZoomMinLevel]-1-y)))
		.append("&WIDTH=").append(size).append("&HEIGHT=").append(size)
		.toString();
	}
	
}
