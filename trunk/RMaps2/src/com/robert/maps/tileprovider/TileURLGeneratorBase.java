package com.robert.maps.tileprovider;

public class TileURLGeneratorBase {
	protected final String mName;
	protected static final String DELIMITER = "_";
	protected static final String SLASH = "/";

	public TileURLGeneratorBase(String mName) {
		this.mName = mName;
	}

	public String Get(final int x, final int y, final int z) {
		return new StringBuilder(mName)
		.append(DELIMITER)
		.append(x)
		.append(DELIMITER)
		.append(y)
		.append(DELIMITER)
		.append(z)
		.toString();
	}
}
