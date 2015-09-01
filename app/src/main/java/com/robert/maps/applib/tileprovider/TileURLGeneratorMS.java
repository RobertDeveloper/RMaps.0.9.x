package com.robert.maps.applib.tileprovider;


public class TileURLGeneratorMS extends TileURLGeneratorBase {
	private final String mImageFileNaming;

	public TileURLGeneratorMS(String mName, final String aImageFileNaming) {
		super(mName);
		mImageFileNaming = aImageFileNaming;
	}

	@Override
	public String Get(int x, int y, int z) {
		return new StringBuilder().append(mName)
		.append(encodeQuadTree(z, x, y))
		.append(mImageFileNaming)
		.toString();
	}
	
	protected static final char[] NUM_CHAR = { '0', '1', '2', '3' };

	private String encodeQuadTree(int zoom, int tilex, int tiley) {
		char[] tileNum = new char[zoom];
		for (int i = zoom - 1; i >= 0; i--) {
			// Binary encoding using ones for tilex and twos for tiley. if a bit
			// is set in tilex and tiley we get a three.
			int num = (tilex % 2) | ((tiley % 2) << 1);
			tileNum[i] = NUM_CHAR[num];
			tilex >>= 1;
			tiley >>= 1;
		}
		return new String(tileNum);
	}

}
