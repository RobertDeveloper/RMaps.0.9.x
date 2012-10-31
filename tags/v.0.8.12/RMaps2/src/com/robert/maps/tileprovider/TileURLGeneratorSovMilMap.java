package com.robert.maps.tileprovider;

public class TileURLGeneratorSovMilMap extends TileURLGeneratorBase {
	protected static final String F = "%f";
	protected static final String PART_END = "&WIDTH=256&HEIGHT=256";

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
		.append(String.format(F, delta * x - g))
		.append(COMMA)
		.append(String.format(F, delta * y2 - g))
		.append(COMMA)
		.append(String.format(F, delta * (x + 1) - g))
		.append(COMMA)
		.append(String.format(F, delta * (y2 + 1) - g))
		.append(PART_END)
		.toString();
	}
	
}
