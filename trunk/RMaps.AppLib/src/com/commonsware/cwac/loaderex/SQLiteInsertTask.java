/* Copyright (c) 2011-2012 -- CommonsWare, LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.commonsware.cwac.loaderex;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

@Deprecated
public class SQLiteInsertTask
  extends AsyncTask<Void, Void, Exception> {
  SQLiteDatabase db;
  String table;
  String nullColumnHack;
  ContentValues values;
  
  public SQLiteInsertTask(SQLiteDatabase db, String table,
                           String nullColumnHack,
                           ContentValues values) {
    this.db=db;
    this.table=table;
    this.nullColumnHack=nullColumnHack;
    this.values=values;
  }
  
  @Override
  protected Exception doInBackground(Void... params) {
    try {
      db.insert(table, nullColumnHack, values);
    }
    catch (Exception e) {
      return(e);
    }
    
    return(null);
  }
}
