package com.robert.maps.kml;

import com.robert.maps.R;
import com.robert.maps.kml.constants.PoiConstants;

public class PoiCategory implements PoiConstants {
	private final int Id;
	public String Title;
	public boolean Hidden;
	public int IconId;

	public PoiCategory(int id, String title, boolean hidden, int iconid) {
		super();
		Id = id;
		Title = title;
		Hidden = hidden;
		IconId = iconid;
	}

	public PoiCategory() {
		this(PoiConstants.EMPTY_ID, "", false, R.drawable.poi);
	}

	public PoiCategory(String title) {
		this(PoiConstants.EMPTY_ID, title, false, R.drawable.poi);
	}

	public int getId() {
		return Id;
	}

}
