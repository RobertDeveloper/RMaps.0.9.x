package com.robert.maps.applib.tileprovider;


public class TileURLGeneratorYANDEX extends TileURLGeneratorBase {
	private final String mImageFileNaming;
	
	public TileURLGeneratorYANDEX(String mName, final String aImageFileNaming) {
		super(mName);
		mImageFileNaming = aImageFileNaming;
	}

	@Override
	public String Get(int x, int y, int z) {
		return new StringBuilder().append(mName)
		.append(x)
		.append("&y=")
		.append(y)
		.append("&z=")
		.append(z)
		.append(mImageFileNaming)
		.toString();
	}
	
}
