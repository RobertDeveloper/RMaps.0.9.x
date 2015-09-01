package com.robert.maps.applib.tileprovider;


public class TileURLGeneratorGOOGLESAT extends TileURLGeneratorBase {
	@SuppressWarnings("unused")
	private final String GOOGLE_LANG_CODE;
	
	public TileURLGeneratorGOOGLESAT(String mName, final String langCode) {
		super(mName);
		GOOGLE_LANG_CODE = langCode;
	}
	
	private static final String strGalileo = new String("Galileo");

	@Override
	public String Get(int x, int y, int z) {
		return new StringBuilder()
		.append(mName)
		.append("&src=app&x=")
		.append(x)
		.append("&y=")
		.append(y)
		.append("&z=")
		.append(z)
		.append("&s=")
		.append(strGalileo.substring(0, (x * 3 + y) % 8))
		.toString();
	}
	
}
