/*
 * Copyright (c) 2011-2012 CommonsWare, LLC
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


package com.commonsware.cwac.loaderex.acl;

import android.support.v4.content.AsyncTaskLoader;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

public class SharedPreferencesLoader extends AsyncTaskLoader<SharedPreferences> {
  private SharedPreferences prefs=null;
  
  @TargetApi(Build.VERSION_CODES.GINGERBREAD)
  public static void persist(final SharedPreferences.Editor editor) {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD) {
      editor.apply();
    }
    else {
      new Thread() {
        public void run() {
          editor.commit();
        }
      }.start();
    }
  }
  
  public SharedPreferencesLoader(Context context) {
    super(context);
  }
  
  /** 
   * Runs on a worker thread, loading in our data.  
   */
  @Override
  public SharedPreferences loadInBackground() {
    prefs=PreferenceManager.getDefaultSharedPreferences(getContext());
    
    return(prefs);
  }

  /**
   * Starts an asynchronous load of the list data.
   * When the result is ready the callbacks will be called
   * on the UI thread. If a previous load has been completed
   * and is still valid the result may be passed to the
   * callbacks immediately.
   * 
   * Must be called from the UI thread.
   */
  @Override
  protected void onStartLoading() {
    if (prefs!=null) {
      deliverResult(prefs);
    }
    
    if (takeContentChanged() || prefs==null) {
      forceLoad();
    }
  }
}