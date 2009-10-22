package com.robert.maps.kml;

import com.robert.maps.kml.constants.PoiConstants;

public class PoiCategory implements PoiConstants {
	private final int Id;
	public String Title;

	public PoiCategory(int id, String title) {
		super();
		Id = id;
		Title = title;
	}

	public PoiCategory() {
		this(PoiConstants.EMPTY_ID, "");
	}

	public PoiCategory(String title) {
		this(PoiConstants.EMPTY_ID, title);
	}

	public int getId() {
		return Id;
	}

}
