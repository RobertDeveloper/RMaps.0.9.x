package com.robert.maps.applib.tileprovider;

public class TileURLGeneratorBase {
	protected final String mName;
	protected static final String DELIMITER = "_";
	protected static final String COMMA = ",";
	protected static final String SLASH = "/";
	protected static final char[][] M_TSQR = {{'q','t'},{'r','s'}};

	public TileURLGeneratorBase(String mName) {
		this.mName = mName;
	}

	public String Get(final int x, final int y, final int z) {
		return new StringBuilder(mName)
		.append(DELIMITER)
		.append(x)
		.append(DELIMITER)
		.append(y)
		.append(DELIMITER)
		.append(z)
		.toString();
	}
	
	protected String getQRTS(int x, int y, int zoomLevel){
		int i;
		int mask;

		String result = "t";
		mask = 1 << zoomLevel;
		x = x % mask;
		if (x < 0) x += mask;
		for (i = 2; i <= zoomLevel+1; i++){
			mask = mask >> 1;
		    result += M_TSQR[((x & mask) > 0)? 1 : 0][((y & mask) > 0)? 1 : 0];
		}
		return result;
	}
	
	public void Free() {
		
	}

}
