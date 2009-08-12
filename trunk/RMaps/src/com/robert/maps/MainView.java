package com.robert.maps;

import org.andnav.osm.OpenStreetMapActivity;
import org.andnav.osm.util.TypeConverter;
import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.controller.OpenStreetMapViewController;
import org.andnav.osm.views.overlay.OpenStreetMapViewSimpleLocationOverlay;
import org.andnav.osm.views.util.OpenStreetMapRendererInfo;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class MainView  extends OpenStreetMapActivity implements OpenStreetMapConstants{
	// ===========================================================
	// Constants
	// ===========================================================
	
	//private static final int MENU_ZOOMIN_ID = Menu.FIRST;
	//private static final int MENU_ZOOMOUT_ID = MENU_ZOOMIN_ID + 1;
	//private static final int MENU_RENDERER_ID = MENU_ZOOMOUT_ID + 1;
	//private static final int MENU_ANIMATION_ID = MENU_RENDERER_ID + 1;
	//private static final int MENU_MINIMAP_ID = MENU_ANIMATION_ID + 1;

	// ===========================================================
	// Fields
	// ===========================================================

	private OpenStreetMapView mOsmv; //, mOsmvMinimap; 
	private OpenStreetMapViewSimpleLocationOverlay mMyLocationOverlay; 

	// ===========================================================
	// Constructors
	// ===========================================================
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, false); // Pass true here to actually contribute to OSM!
        
        // Полноэкранный режим
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        //        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Без заголовка окна
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        final RelativeLayout rl = new RelativeLayout(this);
        
        this.mOsmv = new OpenStreetMapView(this, OpenStreetMapRendererInfo.YANDEXMAP);//FILEMAPNIK);//YANDEXMAP);//CLOUDMADESTANDARDTILES);//MAPNIK);
        rl.addView(this.mOsmv, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        /* SingleLocation-Overlay */
        {
	        /* Create a static Overlay showing a single location. (Gets updated in onLocationChanged(Location loc)! */
	        this.mMyLocationOverlay = new OpenStreetMapViewSimpleLocationOverlay(this);
	        this.mOsmv.getOverlays().add(mMyLocationOverlay);
        }
        
        /* ZoomControls */
        {
	        /* Create a ImageView with a zoomIn-Icon. */
	        final ImageView ivZoomIn = new ImageView(this);
	        ivZoomIn.setImageResource(R.drawable.zoom_in);
	        /* Create RelativeLayoutParams, that position in in the top right corner. */
	        final RelativeLayout.LayoutParams zoominParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        zoominParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	        zoominParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
	        rl.addView(ivZoomIn, zoominParams);
	        
	        ivZoomIn.setOnClickListener(new OnClickListener(){
				// @Override
				public void onClick(View v) {
					MainView.this.mOsmv.zoomIn();
				}
	        });
	        
	        
	        /* Create a ImageView with a zoomOut-Icon. */
	        final ImageView ivZoomOut = new ImageView(this);
	        ivZoomOut.setImageResource(R.drawable.zoom_out);
	        
	        /* Create RelativeLayoutParams, that position in in the top left corner. */
	        final RelativeLayout.LayoutParams zoomoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        zoomoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	        zoomoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
	        rl.addView(ivZoomOut, zoomoutParams);
	        
	        ivZoomOut.setOnClickListener(new OnClickListener(){
				// @Override
				public void onClick(View v) {
					MainView.this.mOsmv.zoomOut();
				}
	        });
        }
        
        /* MiniMap */
 /*       {
	         Create another OpenStreetMapView, that will act as the MiniMap for the 'MainMap'. They will share the TileProvider. 
	        mOsmvMinimap = new OpenStreetMapView(this, OpenStreetMapRendererInfo.CLOUDMADESTANDARDTILES, this.mOsmv);
	        final int aZoomDiff = 3; // Use OpenStreetMapViewConstants.NOT_SET to disable autozooming of this minimap
	        this.mOsmv.setMiniMap(mOsmvMinimap, aZoomDiff);
	        
	        
	         Create RelativeLayout.LayoutParams that position the MiniMap on the top-right corner of the RelativeLayout. 
	        RelativeLayout.LayoutParams minimapParams = new RelativeLayout.LayoutParams(90, 90);
	        minimapParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	        minimapParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
	        minimapParams.setMargins(5,5,5,5);
	        rl.addView(mOsmvMinimap, minimapParams);
        }
*/        
        this.setContentView(rl);
    }
    
    private static final int menu_mylocation = Menu.FIRST;
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		// Group ID
		int groupId = 0;
		// Unique menu item identifier. Used for event handling.
		int menuItemId = menu_mylocation;
		// The order position of the item
		int menuItemOrder = Menu.NONE;
		// Text to be displayed for this menu item.
		int menuItemText = R.string.menu_mylocation;
		// Create the menu item and keep a reference to it.
		MenuItem menuItem = menu.add(groupId, menuItemId, menuItemOrder, menuItemText);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case (menu_mylocation):
			final LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			this.mOsmv.getController().animateTo(TypeConverter.locationToGeoPoint(lm.getLastKnownLocation("gps")), OpenStreetMapViewController.AnimationType.MIDDLEPEAKSPEED, OpenStreetMapViewController.ANIMATION_SMOOTHNESS_HIGH, OpenStreetMapViewController.ANIMATION_DURATION_DEFAULT);
			//this.mOsmv.getController().animateTo(52370816, 9735936, OpenStreetMapViewController.AnimationType.MIDDLEPEAKSPEED, OpenStreetMapViewController.ANIMATION_SMOOTHNESS_HIGH, OpenStreetMapViewController.ANIMATION_DURATION_DEFAULT); // Hannover
			return true;
		}

		return false;
	}


	@Override
	public void onLocationChanged(Location loc) {
		this.mMyLocationOverlay.setLocation(TypeConverter.locationToGeoPoint(loc));
		this.mOsmv.getController().animateTo(TypeConverter.locationToGeoPoint(loc), OpenStreetMapViewController.AnimationType.MIDDLEPEAKSPEED, OpenStreetMapViewController.ANIMATION_SMOOTHNESS_HIGH, OpenStreetMapViewController.ANIMATION_DURATION_DEFAULT);
		//Log.i(DEBUGTAG, "onLocationChanged");
	}

	@Override
	public void onLocationLost() {
		// TODO Auto-generated method stub
		
	}

    

}
