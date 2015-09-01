package com.robert.maps.applib.tileprovider;


public class TileURLGeneratorAVC extends TileURLGeneratorBase {
	private final String mImageFileNaming;
	
	public TileURLGeneratorAVC(String mName, final String aImageFileNaming) {
		super(mName);
		mImageFileNaming = aImageFileNaming;
	}
	
	@Override
	public String Get(int x, int y, int z) {
		return new StringBuilder().append(mName)
		.append(z)
		.append(SLASH)
		.append(x)
		.append(SLASH)
		.append((1<<z)-y-1)
		.append(mImageFileNaming)
		.toString();
	}
	
}
