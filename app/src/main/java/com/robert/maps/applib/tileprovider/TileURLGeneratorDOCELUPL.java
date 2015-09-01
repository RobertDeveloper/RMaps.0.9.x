package com.robert.maps.applib.tileprovider;


public class TileURLGeneratorDOCELUPL extends TileURLGeneratorBase {

	public TileURLGeneratorDOCELUPL(String mName) {
		super(mName);
	}

	@Override
	public String Get(int x, int y, int z) {
		final String sy = String.format("%06x", y);
		final String sx = String.format("%06x", x);
		final char[] cx = sx.toCharArray();
		final char[] cy = sy.toCharArray();
		final String szoom = Integer.toHexString(z);

		String s = mName + szoom + SLASH + cx[4] + cy[4] + SLASH + cx[3]
				+ cy[3] + SLASH + cx[2] + cy[2] + SLASH + cx[1] + cy[1] + SLASH + cx[0] + cy[0]
				+ "/z" + szoom + "x" + sx + "y" + sy + ".png";
		return s;
	}
	
}
