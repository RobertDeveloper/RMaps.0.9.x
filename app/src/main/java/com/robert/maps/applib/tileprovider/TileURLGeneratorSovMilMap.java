package com.robert.maps.applib.tileprovider;

public class TileURLGeneratorSovMilMap extends TileURLGeneratorBase {
	protected static final String F = "%f";
	protected static final String PART_END = "&WIDTH=256&HEIGHT=256";
	protected static final String COMMA = ",";
	protected static final String DOT = ".";

	public TileURLGeneratorSovMilMap(String mName) {
		super(mName);
	}

	@Override
	public String Get(int x, int y1, int z) {
		final double g = 20037508.34;
		final int tilecount = (int) Math.pow(2, z);
		final double delta = g * 2 / tilecount;
		final int y2 = tilecount - 1 - y1;
		
		
		return new StringBuilder()
		.append(mName)
		.append(String.format(F, delta * x - g).replace(COMMA, DOT))
		.append(COMMA)
		.append(String.format(F, delta * y2 - g).replace(COMMA, DOT))
		.append(COMMA)
		.append(String.format(F, delta * (x + 1) - g).replace(COMMA, DOT))
		.append(COMMA)
		.append(String.format(F, delta * (y2 + 1) - g).replace(COMMA, DOT))
		.append(PART_END)
		.toString();
	}
	
}
