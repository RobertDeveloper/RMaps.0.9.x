package com.robert.maps.applib.geoui.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;
import com.manuelpeinado.multichoiceadapter.extras.actionbarcompat.MultiChoiceSimpleCursorAdapter;
import com.robert.maps.applib.R;
import com.robert.maps.applib.data.GeoData;
import com.robert.maps.applib.utils.CoordFormatter;
import com.robert.maps.applib.utils.IconManager;

public class PoiListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {
	private static final int URL_LOADER = 0;
	private SQLiteCursorLoader mLoader;
	private PoiListSimpleCursorAdapter mAdapter;

	public static Fragment newInstance() {
		return new PoiListFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		final View view = inflater.inflate(R.layout.poi_list, container, false);
		setHasOptionsMenu(true);

		mAdapter = new PoiListSimpleCursorAdapter(savedInstanceState, getActivity(),
                R.layout.poilist_item, null,
                        new String[] { "name", "iconid", "catname", "descr" },
                        new int[] { R.id.title1, R.id.pic, R.id.title2, R.id.descr});
        final PoiViewBinder binder = new PoiViewBinder(getActivity().getApplicationContext());
        mAdapter.setViewBinder(binder);
        mAdapter.setAdapterView((ListView) view.findViewById(android.R.id.list));
        setListAdapter(mAdapter);
		
		getLoaderManager().initLoader(URL_LOADER, null, this);
		
		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    inflater.inflate(R.menu.poilist_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	private static class PoiViewBinder implements PoiListSimpleCursorAdapter.ViewBinder {
		private static final String CATNAME = "catname";
		private static final String LAT = "lat";
		private static final String LON = "lon";
		private static final String ICONID = "iconid";
		private CoordFormatter mCf;
		private IconManager mIconManager;

		public PoiViewBinder(Context context) {
			super();
			mCf = new CoordFormatter(context);
			mIconManager = IconManager.getInstance(context);
		}

		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if(cursor.getColumnName(columnIndex).equalsIgnoreCase(CATNAME)) {
				((TextView)view.findViewById(R.id.title2)).setText(cursor.getString(cursor.getColumnIndex(CATNAME))
						+", "+mCf.convertLat(cursor.getDouble(cursor.getColumnIndex(LAT)))
						+", "+mCf.convertLon(cursor.getDouble(cursor.getColumnIndex(LON)))
			);
				return true;
			} else if(cursor.getColumnName(columnIndex).equalsIgnoreCase(ICONID)) {
				((ImageView) view.findViewById(R.id.pic)).setImageResource(mIconManager.getPoiIconResId(cursor.getInt(columnIndex)));
				return true;
			}
			return false;
		}
		
	}

	private static class PoiListSimpleCursorAdapter extends MultiChoiceSimpleCursorAdapter {

		public PoiListSimpleCursorAdapter(Bundle savedInstanceState,
				Context context, int layout, Cursor cursor, String[] from,
				int[] to) {
			super(savedInstanceState, context, layout, cursor, from, to, 0);
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
	        MenuInflater inflater = mode.getMenuInflater();
	        inflater.inflate(R.menu.poilist_select_menu, menu);
	        return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem menu) {
			return false;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
		switch (loaderID) {
        case URL_LOADER:
        	mLoader = GeoData.getInstance(getActivity().getApplicationContext()).getPoiListCursorLoader();
            return mLoader;
        default:
            // An invalid id was passed in
            return null;
    }
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.changeCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}

}
