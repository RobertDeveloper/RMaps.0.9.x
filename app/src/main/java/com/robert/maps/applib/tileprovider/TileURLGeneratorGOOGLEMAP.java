package com.robert.maps.applib.tileprovider;


public class TileURLGeneratorGOOGLEMAP extends TileURLGeneratorBase {
	private final String GOOGLE_LANG_CODE;
	private final String GOOGLESCALE;
	
	public TileURLGeneratorGOOGLEMAP(String mName, final String langCode, final String googleScale) {
		super(mName);
		GOOGLE_LANG_CODE = langCode;
		GOOGLESCALE = googleScale;
	}
	
	private static final String strGalileo = new String("Galileo");
	
	@Override
	public String Get(int x, int y, int z) {
		return new StringBuilder().append(mName)
		.append("hl=")
		.append(GOOGLE_LANG_CODE)
		.append("&src=app&x=")
		.append(x)
		.append("&y=")
		.append(y)
		.append("&z=")
		.append(z)
		.append("&scale=")
		.append(GOOGLESCALE)
		.append("&s=")
		.append(strGalileo.substring(0, (x*3+y)% 8))
		.toString();
	}
	
}
