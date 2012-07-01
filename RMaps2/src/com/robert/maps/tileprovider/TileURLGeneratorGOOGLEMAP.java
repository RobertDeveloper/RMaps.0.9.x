package com.robert.maps.tileprovider;


public class TileURLGeneratorGOOGLEMAP extends TileURLGeneratorBase {
	private final String GOOGLE_LANG_CODE;
	
	public TileURLGeneratorGOOGLEMAP(String mName, final String langCode) {
		super(mName);
		GOOGLE_LANG_CODE = langCode;
	}
	
	private static final String strGalileo = new String("Galileo");
	
	@Override
	public String Get(int x, int y, int z) {
		return new StringBuilder().append(mName)
		.append("hl=")
		.append(GOOGLE_LANG_CODE)
		.append("&x=")
		.append(x)
		.append("&y=")
		.append(y)
		.append("&zoom=")
		.append(18-z-1)
		.append("&s=")
		.append(strGalileo.substring(0, (x*3+y)% 8))
		.toString();
	}
	
}
