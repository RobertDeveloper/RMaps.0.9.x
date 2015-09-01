package com.robert.maps.applib.tileprovider;

public class TileURLGeneratorCustom extends TileURLGeneratorBase {
	private final static String X = "{x}";
	private final static String Y = "{y}";
	private final static String Z = "{z}";
	private final static String strGalileo = "Galileo";
	private final static String GALILEO = "{galileo}";

	public TileURLGeneratorCustom(String baseurl) {
		super(baseurl);
	}
	
	@Override
	public String Get(int x, int y, int z) {
		return mName
				.replace(X, Integer.toString(x))
				.replace(Y, Integer.toString(y))
				.replace(Z, Integer.toString(z))
				.replace(GALILEO, strGalileo.substring(0, (x*3+y)% 8))
				;
	}

}
