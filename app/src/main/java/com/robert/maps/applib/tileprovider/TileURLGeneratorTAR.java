package com.robert.maps.applib.tileprovider;


public class TileURLGeneratorTAR extends TileURLGeneratorBase {

	public TileURLGeneratorTAR(String mName) {
		super(mName);
	}
	
	public String Get(final int x, final int y, final int z) {
		return new StringBuilder()
				.append(0).append(z + 1).reverse().delete(2, 3).reverse()
				.append(SLASH)
				.append(getQRTS(x, y, z))
				.toString();
	}
	
}
