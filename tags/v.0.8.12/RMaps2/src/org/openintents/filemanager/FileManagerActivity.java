/*
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Based on AndDev.org's file browser V 2.0.
 */

package org.openintents.filemanager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openintents.filemanager.util.FileUtils;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.robert.maps.R;

public class FileManagerActivity extends ListActivity {
	private int mState;

	private static final int STATE_BROWSE = 1;
//	private static final int STATE_PICK_FILE = 2;
//	private static final int STATE_PICK_DIRECTORY = 3;

	private static final String BUNDLE_CURRENT_DIRECTORY = "current_directory";
	private static final String BUNDLE_CONTEXT_FILE = "context_file";
	private static final String BUNDLE_CONTEXT_TEXT = "context_text";
	private static final String BUNDLE_SHOW_DIRECTORY_INPUT = "show_directory_input";
	private static final String BUNDLE_STEPS_BACK = "steps_back";

	/** Contains directories and files together */
     private ArrayList<IconifiedText> directoryEntries = new ArrayList<IconifiedText>();

     /** Dir separate for sorting */
     List<IconifiedText> mListDir = new ArrayList<IconifiedText>();

     /** Files separate for sorting */
     List<IconifiedText> mListFile = new ArrayList<IconifiedText>();

     /** SD card separate for sorting */
     List<IconifiedText> mListSdCard = new ArrayList<IconifiedText>();

     private File currentDirectory = new File("");

     private String mSdCardPath = "";

     //private MimeTypes mMimeTypes;

     private String mContextText;
     private File mContextFile = new File("");

     /** How many steps one can make back using the back key. */
     private int mStepsBack;

     private EditText mEditFilename;
     private Button mButtonPick;
//     private LinearLayout mDirectoryButtons;

     private LinearLayout mDirectoryInput;
     private EditText mEditDirectory;
     private ImageButton mButtonDirectoryPick;

     private TextView mEmptyText;
     private TextView mPathText;
//     private ProgressBar mProgressBar;

     private DirectoryScanner mDirectoryScanner;
     private File mPreviousDirectory;
//     private ThumbnailLoader mThumbnailLoader;

     private Handler currentHandler;

 	 static final public int MESSAGE_SHOW_DIRECTORY_CONTENTS = 500;	// List of contents is ready, obj = DirectoryContents

     /** Called when the activity is first created. */
     @Override
     public void onCreate(Bundle icicle) {
          super.onCreate(icicle);

          currentHandler = new Handler() {
			public void handleMessage(Message msg) {
				FileManagerActivity.this.handleMessage(msg);
			}
		};

		  requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
          setContentView(R.layout.filelist);

          mEmptyText = (TextView) findViewById(R.id.empty_text);
          mPathText = (TextView) findViewById(R.id.path);

		  getListView().setEmptyView(findViewById(R.id.empty));
	      getListView().setTextFilterEnabled(true);
	      getListView().requestFocus();
	      getListView().requestFocusFromTouch();

//          mDirectoryButtons = (LinearLayout) findViewById(R.id.directory_buttons);
//          mEditFilename = (EditText) findViewById(R.id.filename);
//
//
//          mButtonPick = (Button) findViewById(R.id.button_pick);
//
//          mButtonPick.setOnClickListener(new View.OnClickListener() {
//
//				public void onClick(View arg0) {
//					pickFileOrDirectory();
//				}
//          });

          // Initialize only when necessary:
          mDirectoryInput = null;

          // Create map of extensions:
          mState = STATE_BROWSE;

          Intent intent = getIntent();
          String action = intent.getAction();

          File browseto = new File("/");

          if (!TextUtils.isEmpty(mSdCardPath)) {
        	  browseto = new File(mSdCardPath);
          }

//          if (action != null) {
//        	  if (action.equals(FileManagerIntents.ACTION_PICK_FILE)) {
//        		  mState = STATE_PICK_FILE;
//        	  } else if (action.equals(FileManagerIntents.ACTION_PICK_DIRECTORY)) {
//        		  mState = STATE_PICK_DIRECTORY;
//
//        		  // Remove edit text and make button fill whole line
//        		  mEditFilename.setVisibility(View.GONE);
//        		  mButtonPick.setLayoutParams(new LinearLayout.LayoutParams(
//        				  LinearLayout.LayoutParams.FILL_PARENT,
//        				  LinearLayout.LayoutParams.WRAP_CONTENT));
//        	  }
//          }
//
//          if (mState == STATE_BROWSE) {
//        	  // Remove edit text and button.
//        	  mEditFilename.setVisibility(View.GONE);
//        	  mButtonPick.setVisibility(View.GONE);
//          }

          // Set current directory and file based on intent data.
    	  File file = FileUtils.getFile(intent.getData());
    	  if (file != null) {
    		  File dir = FileUtils.getPathWithoutFilename(file);
    		  if (dir.isDirectory()) {
    			  browseto = dir;
    		  }
//    		  if (!file.isDirectory()) {
//    			  mEditFilename.setText(file.getName());
//    		  }
    	  }
//
//    	  String title = intent.getStringExtra(FileManagerIntents.EXTRA_TITLE);
//    	  if (title != null) {
//    		  setTitle(title);
//    	  }
//
//    	  String buttontext = intent.getStringExtra(FileManagerIntents.EXTRA_BUTTON_TEXT);
//    	  if (buttontext != null) {
//    		  mButtonPick.setText(buttontext);
//    	  }

          mStepsBack = 0;

          if (icicle != null) {
        	  browseto = new File(icicle.getString(BUNDLE_CURRENT_DIRECTORY));
        	  mContextFile = new File(icicle.getString(BUNDLE_CONTEXT_FILE));
        	  mContextText = icicle.getString(BUNDLE_CONTEXT_TEXT);

        	  boolean show = icicle.getBoolean(BUNDLE_SHOW_DIRECTORY_INPUT);
        	  showDirectoryInput(show);

        	  mStepsBack = icicle.getInt(BUNDLE_STEPS_BACK);
          }

          browseTo(browseto);
     }

     public void onDestroy() {
    	 super.onDestroy();

    	 // Stop the scanner.
    	 DirectoryScanner scanner = mDirectoryScanner;

    	 if (scanner != null) {
    		 scanner.cancel = true;
    	 }

    	 mDirectoryScanner = null;

//    	 ThumbnailLoader loader = mThumbnailLoader;
//
//    	 if (loader != null) {
//    		 loader.cancel = true;
////    		 mThumbnailLoader = null;
//    	 }
     }

     private void handleMessage(Message message) {
//    	 Log.v(TAG, "Received message " + message.what);

    	 switch (message.what) {
    	 case MESSAGE_SHOW_DIRECTORY_CONTENTS:
    		 showDirectoryContents((DirectoryContents) message.obj);
    		 break;
    	 }
     }

//     private void notifyIconChanged(IconifiedText text) {
//    	 if (getListAdapter() != null) {
//    		 ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
//    	 }
//     }
//
//     private void setProgress(int progress, int maxProgress) {
//    	 mProgressBar.setMax(maxProgress);
//    	 mProgressBar.setProgress(progress);
//    	 mProgressBar.setVisibility(View.VISIBLE);
//     }
//
     private void showDirectoryContents(DirectoryContents contents) {
    	 mDirectoryScanner = null;

    	 mListSdCard = contents.listSdCard;
    	 mListDir = contents.listDir;
    	 mListFile = contents.listFile;

    	 directoryEntries.ensureCapacity(mListSdCard.size() + mListDir.size() + mListFile.size());

         addAllElements(directoryEntries, mListSdCard);
         addAllElements(directoryEntries, mListDir);
         addAllElements(directoryEntries, mListFile);

         IconifiedTextListAdapter itla = new IconifiedTextListAdapter(this);
         itla.setListItems(directoryEntries, getListView().hasTextFilter());
         setListAdapter(itla);
	     getListView().setTextFilterEnabled(true);

         selectInList(mPreviousDirectory);
         refreshDirectoryPanel();
         setProgressBarIndeterminateVisibility(false);

//    	 mProgressBar.setVisibility(View.GONE);
    	 mEmptyText.setVisibility(View.VISIBLE);

//    	 mThumbnailLoader = new ThumbnailLoader(currentDirectory, mListFile, currentHandler);
//    	 mThumbnailLoader.start();
     }

     private void onCreateDirectoryInput() {
//    	 mDirectoryInput = (LinearLayout) findViewById(R.id.directory_input);
//         mEditDirectory = (EditText) findViewById(R.id.directory_text);
//
//         mButtonDirectoryPick = (ImageButton) findViewById(R.id.button_directory_pick);
//
//         mButtonDirectoryPick.setOnClickListener(new View.OnClickListener() {
//
//				public void onClick(View arg0) {
//					goToDirectoryInEditText();
//				}
//         });
     }

     //private boolean mHaveShownErrorMessage;
     private File mHaveShownErrorMessageForFile = null;

     private void goToDirectoryInEditText() {
    	 File browseto = new File(mEditDirectory.getText().toString());

    	 if (browseto.equals(currentDirectory)) {
    		 showDirectoryInput(false);
    	 } else {
    		 if (mHaveShownErrorMessageForFile != null
    				 && mHaveShownErrorMessageForFile.equals(browseto)) {
    			 // Don't let user get stuck in wrong directory.
    			 mHaveShownErrorMessageForFile = null;
        		 showDirectoryInput(false);
    		 } else {
	    		 if (!browseto.exists()) {
	    			 // browseTo() below will show an error message,
	    			 // because file does not exist.
	    			 // It is ok to show this the first time.
	    			 mHaveShownErrorMessageForFile = browseto;
	    		 }
				 browseTo(browseto);
    		 }
    	 }
     }

     /**
      * Show the directory line as input box instead of button row.
      * If Directory input does not exist yet, it is created.
      * Since the default is show == false, nothing is created if
      * it is not necessary (like after icicle).
      * @param show
      */
     private void showDirectoryInput(boolean show) {
    	 if (show) {
    		 if (mDirectoryInput == null) {
        		 onCreateDirectoryInput();
        	 }
    	 }
    	 if (mDirectoryInput != null) {
	    	 mDirectoryInput.setVisibility(show ? View.VISIBLE : View.GONE);
//	    	 mDirectoryButtons.setVisibility(show ? View.GONE : View.VISIBLE);
    	 }

    	 refreshDirectoryPanel();
     }

 	/**
 	 *
 	 */
 	private void refreshDirectoryPanel() {
		String path = currentDirectory.getAbsolutePath();
 		if (isDirectoryInputVisible()) {
 			// Set directory path
 			mEditDirectory.setText(path);

 			// Set selection to last position so user can continue to type:
 			mEditDirectory.setSelection(path.length());
 		} else {
 			//setDirectoryButtons();
 			mPathText.setText(path);
 		}
 	}
     /*
     @Override
	protected void onResume() {
		super.onResume();
	}
*/


 	@Override
 	protected void onSaveInstanceState(Bundle outState) {
 		super.onSaveInstanceState(outState);

 		// remember file name
 		outState.putString(BUNDLE_CURRENT_DIRECTORY, currentDirectory.getAbsolutePath());
 		outState.putString(BUNDLE_CONTEXT_FILE, mContextFile.getAbsolutePath());
 		outState.putString(BUNDLE_CONTEXT_TEXT, mContextText);
 		boolean show = isDirectoryInputVisible();
 		outState.putBoolean(BUNDLE_SHOW_DIRECTORY_INPUT, show);
 		outState.putInt(BUNDLE_STEPS_BACK, mStepsBack);
 	}

	/**
	 * @return
	 */
	private boolean isDirectoryInputVisible() {
		return ((mDirectoryInput != null) && (mDirectoryInput.getVisibility() == View.VISIBLE));
	}

	private void pickFileOrDirectory() {
//		File file = null;
//		if (mState == STATE_PICK_FILE) {
//			String filename = mEditFilename.getText().toString();
//			file = FileUtils.getFile(currentDirectory.getAbsolutePath(), filename);
//		} else if (mState == STATE_PICK_DIRECTORY) {
//			file = currentDirectory;
//		}
//
//    	Intent intent = getIntent();
//    	intent.setData(FileUtils.getUri(file));
//    	setResult(RESULT_OK, intent);
//    	finish();
     }

	/**
      * This function browses up one level
      * according to the field: currentDirectory
      */
     private void upOneLevel(){
    	 if (mStepsBack > 0) {
    		 mStepsBack--;
    	 }
         if(currentDirectory.getParent() != null)
               browseTo(currentDirectory.getParentFile());
     }

     /**
      * Jump to some location by clicking on a
      * directory button.
      *
      * This resets the counter for "back" actions.
      *
      * @param aDirectory
      */
     private void jumpTo(final File aDirectory) {
    	 mStepsBack = 0;
    	 browseTo(aDirectory);
     }

     /**
      * Browse to some location by clicking on a list item.
      * @param aDirectory
      */
     private void browseTo(final File aDirectory){
          // setTitle(aDirectory.getAbsolutePath());

          if (aDirectory.isDirectory()){
        	  if (aDirectory.equals(currentDirectory)) {
        		  // Switch from button to directory input
        		  showDirectoryInput(true);
        	  } else {
        		   mPreviousDirectory = currentDirectory;
	               currentDirectory = aDirectory;
	               refreshList();
//	               selectInList(previousDirectory);
	//               refreshDirectoryPanel();
        	  }
          }
//          else{
//        	  if (mState == STATE_BROWSE || mState == STATE_PICK_DIRECTORY) {
//	              // Lets start an intent to View the file, that was clicked...
//	        	  //openFile(aDirectory);
//        	  } else if (mState == STATE_PICK_FILE) {
//        		  // Pick the file
//        		  mEditFilename.setText(aDirectory.getName());
//        	  }
//          }
     }


     private void refreshList() {

    	  // Cancel an existing scanner, if applicable.
    	  DirectoryScanner scanner = mDirectoryScanner;

    	  if (scanner != null) {
    		  scanner.cancel = true;
    	  }

//    	  ThumbnailLoader loader = mThumbnailLoader;

//    	  if (loader != null) {
//    		  loader.cancel = true;
//    		  mThumbnailLoader = null;
//    	  }

    	  directoryEntries.clear();
          mListDir.clear();
          mListFile.clear();
          mListSdCard.clear();

          setProgressBarIndeterminateVisibility(true);

          // Don't show the "folder empty" text since we're scanning.
          mEmptyText.setVisibility(View.GONE);

          // Also DON'T show the progress bar - it's kind of lame to show that
          // for less than a second.
//          mProgressBar.setVisibility(View.GONE);
          setListAdapter(null);

		  mDirectoryScanner = new DirectoryScanner(currentDirectory, this, currentHandler);
		  mDirectoryScanner.start();



          // Add the "." == "current directory"
          /*directoryEntries.add(new IconifiedText(
                    getString(R.string.current_dir),
                    getResources().getDrawable(R.drawable.ic_launcher_folder)));        */
          // and the ".." == 'Up one level'
          /*
          if(currentDirectory.getParent() != null)
               directoryEntries.add(new IconifiedText(
                         getString(R.string.up_one_level),
                         getResources().getDrawable(R.drawable.ic_launcher_folder_open)));
          */
     }

     private void selectInList(File selectFile) {
    	 String filename = selectFile.getName();
    	 IconifiedTextListAdapter la = (IconifiedTextListAdapter) getListAdapter();
    	 int count = la.getCount();
    	 for (int i = 0; i < count; i++) {
    		 IconifiedText it = (IconifiedText) la.getItem(i);
    		 if (it.getText().equals(filename)) {
    			 getListView().setSelection(i);
    			 break;
    		 }
    	 }
     }

     private void addAllElements(List<IconifiedText> addTo, List<IconifiedText> addFrom) {
    	 int size = addFrom.size();
    	 for (int i = 0; i < size; i++) {
    		 addTo.add(addFrom.get(i));
    	 }
     }

//     private void setDirectoryButtons() {
//    	 String[] parts = currentDirectory.getAbsolutePath().split("/");
//
//    	 mDirectoryButtons.removeAllViews();
//
//    	 int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;
//
//    	 // Add home button separately
//    	 ImageButton ib = new ImageButton(this);
//    	 //ib.setImageResource(R.drawable.ic_launcher_home_small);
//		 ib.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
//		 ib.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View view) {
//				jumpTo(new File("/"));
//			}
//		 });
//		 mDirectoryButtons.addView(ib);
//
//    	 // Add other buttons
//
//    	 String dir = "";
//
//    	 for (int i = 1; i < parts.length; i++) {
//    		 dir += "/" + parts[i];
//    		 if (dir.equals(mSdCardPath)) {
//    			 // Add SD card button
//    			 ib = new ImageButton(this);
//    	    	 //ib.setImageResource(R.drawable.icon_sdcard_small);
//    			 ib.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
//    			 ib.setOnClickListener(new View.OnClickListener() {
//    					public void onClick(View view) {
//    						jumpTo(new File(mSdCardPath));
//    					}
//    			 });
//    			 mDirectoryButtons.addView(ib);
//    		 } else {
//	    		 Button b = new Button(this);
//	    		 b.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
//	    		 b.setText(parts[i]);
//	    		 b.setTag(dir);
//	    		 b.setOnClickListener(new View.OnClickListener() {
//	 				public void onClick(View view) {
//	 					String dir = (String) view.getTag();
//	 					jumpTo(new File(dir));
//	 				}
//	    		 });
//    			 mDirectoryButtons.addView(b);
//    		 }
//    	 }
//
//    	 checkButtonLayout();
//     }
//
//     private void checkButtonLayout() {
//
//    	 // Let's measure how much space we need:
//    	 int spec = View.MeasureSpec.UNSPECIFIED;
//    	 mDirectoryButtons.measure(spec, spec);
//    	 int count = mDirectoryButtons.getChildCount();
//
//    	 int requiredwidth = mDirectoryButtons.getMeasuredWidth();
//    	 int width = getWindowManager().getDefaultDisplay().getWidth();
//
//    	 if (requiredwidth > width) {
//        	 int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;
//
//        	 // Create a new button that shows that there is more to the left:
//        	 ImageButton ib = new ImageButton(this);
//        	 //ib.setImageResource(R.drawable.ic_menu_back_small);
//    		 ib.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
//    		 //
//    		 ib.setOnClickListener(new View.OnClickListener() {
//    				public void onClick(View view) {
//    					// Up one directory.
//    					upOneLevel();
//    				}
//    		 });
//    		 mDirectoryButtons.addView(ib, 0);
//
//    		 // New button needs even more space
//    		 ib.measure(spec, spec);
//    		 requiredwidth += ib.getMeasuredWidth();
//
//    		 // Need to take away some buttons
//    		 // but leave at least "back" button and one directory button.
//    		 while (requiredwidth > width && mDirectoryButtons.getChildCount() > 2) {
//    			 View view = mDirectoryButtons.getChildAt(1);
//    			 requiredwidth -= view.getMeasuredWidth();
//
//	    		 mDirectoryButtons.removeViewAt(1);
//    		 }
//    	 }
//     }
//
     @Override
     protected void onListItemClick(ListView l, View v, int position, long id) {
          super.onListItemClick(l, v, position, id);

          IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) getListAdapter();

          if (adapter == null) {
        	  return;
          }

          IconifiedText text = (IconifiedText) adapter.getItem(position);

          String file = text.getText();

          if (file.equals("...")) {
			upOneLevel();
		} else {
			String curdir = currentDirectory.getAbsolutePath();
			File clickedFile = FileUtils.getFile(curdir, file);
			if (clickedFile != null) {
				if (clickedFile.isDirectory()) {
					// If we click on folders, we can return later by the "back" key.
					mStepsBack++;

					browseTo(clickedFile);
				}
				else{
			    	Intent intent = getIntent();
			    	intent.setData(FileUtils.getUri(clickedFile));
			    	setResult(RESULT_OK, intent);
			    	finish();
				}

			}
		}
     }

//     @Override
// 	public boolean onCreateOptionsMenu(Menu menu) {
// 		super.onCreateOptionsMenu(menu);
//
// 		menu.add(0, MENU_NEW_FOLDER, 0, R.string.menu_new_folder).setIcon(
// 				android.R.drawable.ic_menu_add).setShortcut('0', 'f');
//
// 		UpdateMenu
// 				.addUpdateMenu(this, menu, 0, MENU_UPDATE, 0, R.string.update);
// 		menu.add(0, MENU_ABOUT, 0, R.string.about).setIcon(
// 				android.R.drawable.ic_menu_info_details).setShortcut('0', 'a');
//
// 		return true;
// 	}
//
//
//	@Override
//	public boolean onPrepareOptionsMenu(Menu menu) {
//		super.onPrepareOptionsMenu(menu);
//
//		// Generate any additional actions that can be performed on the
//		// overall list. This allows other applications to extend
//		// our menu with their own actions.
//		Intent intent = new Intent(null, getIntent().getData());
//		intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
//		// menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
//		// new ComponentName(this, NoteEditor.class), null, intent, 0, null);
//
//		// Workaround to add icons:
//		MenuIntentOptionsWithIcons menu2 = new MenuIntentOptionsWithIcons(this,
//				menu);
//		menu2.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
//				new ComponentName(this, FileManagerActivity.class), null, intent,
//				0, null);
//
//		return true;
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		//Intent intent;
//		switch (item.getItemId()) {
//		case MENU_NEW_FOLDER:
//			showDialog(DIALOG_NEW_FOLDER);
//			return true;
//
//		case MENU_UPDATE:
//			UpdateMenu.showUpdateBox(this);
//			return true;
//
//		case MENU_ABOUT:
//			showAboutBox();
//			return true;
///*
//		case MENU_PREFERENCES:
//			intent = new Intent(this, PreferenceActivity.class);
//			startActivity(intent);
//			return true;
//			*/
//		}
//		return super.onOptionsItemSelected(item);
//
//	}
//
//	@Override
//	public void onCreateContextMenu(ContextMenu menu, View view,
//			ContextMenuInfo menuInfo) {
//		AdapterView.AdapterContextMenuInfo info;
//		try {
//			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
//		} catch (ClassCastException e) {
//			Log.e(TAG, "bad menuInfo", e);
//			return;
//		}
///*
//		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
//		if (cursor == null) {
//			// For some reason the requested item isn't available, do nothing
//			return;
//		}
//*/
//        IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) getListAdapter();
//
//        if (adapter == null) {
//      	  return;
//        }
//
//        IconifiedText it = (IconifiedText) adapter.getItem(info.position);
//		menu.setHeaderTitle(it.getText());
//		menu.setHeaderIcon(it.getIcon());
//		File file = FileUtils.getFile(currentDirectory, it.getText());
//
//
//		if (!file.isDirectory()) {
//			if (mState == STATE_PICK_FILE) {
//				// Show "open" menu
//				menu.add(0, MENU_OPEN, 0, R.string.menu_open);
//			}
//			menu.add(0, MENU_SEND, 0, R.string.menu_send);
//		}
//		menu.add(0, MENU_MOVE, 0, R.string.menu_move);
//
//		if (!file.isDirectory()) {
//			menu.add(0, MENU_COPY, 0, R.string.menu_copy);
//		}
//
//		menu.add(0, MENU_RENAME, 0, R.string.menu_rename);
//		menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
//
//		//if (!file.isDirectory()) {
//	        Uri data = Uri.fromFile(file);
//	        Intent intent = new Intent(null, data);
//	        String type = mMimeTypes.getMimeType(file.getName());
//
//	        intent.setDataAndType(data, type);
//	        //intent.addCategory(Intent.CATEGORY_SELECTED_ALTERNATIVE);
//
//	        Log.v(TAG, "Data=" + data);
//	        Log.v(TAG, "Type=" + type);
//
//	        if (type != null) {
//	        	// Add additional options for the MIME type of the selected file.
//				menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
//						new ComponentName(this, FileManagerActivity.class), null, intent, 0, null);
//	        }
//		//}
//	}
//
//	@Override
//	public boolean onContextItemSelected(MenuItem item) {
//		super.onContextItemSelected(item);
//		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
//				.getMenuInfo();
//
//		// Remember current selection
//        IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) getListAdapter();
//
//        if (adapter == null) {
//      	  return false;
//        }
//
//        IconifiedText ic = (IconifiedText) adapter.getItem(menuInfo.position);
//		mContextText = ic.getText();
//		mContextIcon = ic.getIcon();
//		mContextFile = FileUtils.getFile(currentDirectory, ic.getText());
//
//		switch (item.getItemId()) {
//		case MENU_OPEN:
//            openFile(mContextFile);
//			return true;
//
//		case MENU_MOVE:
//			promptDestinationAndMoveFile();
//			return true;
//
//		case MENU_COPY:
//			promptDestinationAndCopyFile();
//			return true;
//
//		case MENU_DELETE:
//			showDialog(DIALOG_DELETE);
//			return true;
//
//		case MENU_RENAME:
//			showDialog(DIALOG_RENAME);
//			return true;
//
//		case MENU_SEND:
//			sendFile(mContextFile);
//			return true;
//		}
//
//		return false;
//	}
//
//	@Override
//	protected Dialog onCreateDialog(int id) {
//
//		switch (id) {
//		case DIALOG_NEW_FOLDER:
//			LayoutInflater inflater = LayoutInflater.from(this);
//			View view = inflater.inflate(R.layout.dialog_new_folder, null);
//			final EditText et = (EditText) view
//					.findViewById(R.id.foldername);
//			et.setText("");
//			return new AlertDialog.Builder(this)
//            	.setIcon(android.R.drawable.ic_dialog_alert)
//            	.setTitle(R.string.create_new_folder).setView(view).setPositiveButton(
//					android.R.string.ok, new OnClickListener() {
//
//						public void onClick(DialogInterface dialog, int which) {
//							createNewFolder(et.getText().toString());
//						}
//
//					}).setNegativeButton(android.R.string.cancel, new OnClickListener() {
//
//						public void onClick(DialogInterface dialog, int which) {
//							// Cancel should not do anything.
//						}
//
//					}).create();
//
//
//		case DIALOG_DELETE:
//			return new AlertDialog.Builder(this).setTitle(getString(R.string.really_delete, mContextText))
//            	.setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(
//					android.R.string.ok, new OnClickListener() {
//
//						public void onClick(DialogInterface dialog, int which) {
//							deleteFileOrFolder(mContextFile);
//						}
//
//					}).setNegativeButton(android.R.string.cancel, new OnClickListener() {
//
//						public void onClick(DialogInterface dialog, int which) {
//							// Cancel should not do anything.
//						}
//
//					}).create();
//
//		case DIALOG_RENAME:
//			inflater = LayoutInflater.from(this);
//			view = inflater.inflate(R.layout.dialog_new_folder, null);
//			final EditText et2 = (EditText) view
//				.findViewById(R.id.foldername);
//			return new AlertDialog.Builder(this)
//            	.setTitle(R.string.menu_rename).setView(view).setPositiveButton(
//					android.R.string.ok, new OnClickListener() {
//
//						public void onClick(DialogInterface dialog, int which) {
//
//							renameFileOrFolder(mContextFile, et2.getText().toString());
//						}
//
//					}).setNegativeButton(android.R.string.cancel, new OnClickListener() {
//
//						public void onClick(DialogInterface dialog, int which) {
//							// Cancel should not do anything.
//						}
//
//					}).create();
//
////		case DIALOG_ABOUT:
////			return new AboutDialog(this);
//
//		}
//		return null;
//
//	}
//
//
//	@Override
//	protected void onPrepareDialog(int id, Dialog dialog) {
//		super.onPrepareDialog(id, dialog);
//
//		switch (id) {
//		case DIALOG_NEW_FOLDER:
//			EditText et = (EditText) dialog.findViewById(R.id.foldername);
//			et.setText("");
//			break;
//
//		case DIALOG_DELETE:
//			((AlertDialog) dialog).setTitle(getString(R.string.really_delete, mContextText));
//			break;
//
//		case DIALOG_RENAME:
//			et = (EditText) dialog.findViewById(R.id.foldername);
//			et.setText(mContextText);
//			TextView tv = (TextView) dialog.findViewById(R.id.foldernametext);
//			if (mContextFile.isDirectory()) {
//				tv.setText(R.string.file_name);
//			} else {
//				tv.setText(R.string.file_name);
//			}
//			((AlertDialog) dialog).setIcon(mContextIcon);
//			break;
//
//		case DIALOG_ABOUT:
//			break;
//		}
//	}
//
//	private void showAboutBox() {
//		//startActivity(new Intent(this, AboutDialog.class));
//		AboutDialog.showDialogOrStartActivity(this, DIALOG_ABOUT);
//	}
//
//	private void promptDestinationAndMoveFile() {
//
//		Intent intent = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);
//
//		intent.setData(FileUtils.getUri(currentDirectory));
//
//		intent.putExtra(FileManagerIntents.EXTRA_TITLE, getString(R.string.move_title));
//		intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, getString(R.string.move_button));
//
//		startActivityForResult(intent, REQUEST_CODE_MOVE);
//	}
//
//	private void promptDestinationAndCopyFile() {
//
//		Intent intent = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);
//
//		intent.setData(FileUtils.getUri(currentDirectory));
//
//		intent.putExtra(FileManagerIntents.EXTRA_TITLE, getString(R.string.copy_title));
//		intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, getString(R.string.copy_button));
//
//		startActivityForResult(intent, REQUEST_CODE_COPY);
//	}
//
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//
//		if (keyCode == KeyEvent.KEYCODE_BACK) {
//			if (mStepsBack > 0) {
//				upOneLevel();
//				return true;
//			}
//		}
//
//		return super.onKeyDown(keyCode, event);
//	}

	// For targetSdkVersion="5" or higher, one needs to use the following code instead of the one above:
	// (See http://android-developers.blogspot.com/2009/12/back-and-other-hard-keys-three-stories.html )

	/*
	//@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
	            && keyCode == KeyEvent.KEYCODE_BACK
	            && event.getRepeatCount() == 0) {
	        // Take care of calling this method on earlier versions of
	        // the platform where it doesn't exist.
	        onBackPressed();
	    }

	    return super.onKeyDown(keyCode, event);
	}

	//@Override
	public void onBackPressed() {
	    // This will be called either automatically for you on 2.0
	    // or later, or by the code above on earlier versions of the
	    // platform.
		if (mStepsBack > 0) {
			upOneLevel();
		} else {
			finish();
		}
	}
	*/

}