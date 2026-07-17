package com.miz.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public abstract class AbstractDbAdapter {

	protected SQLiteDatabase mDatabase;
	protected Context mContext;

	public AbstractDbAdapter(Context context) {
		mContext = context;
		mDatabase = DatabaseHelper.getHelper(context).getWritableDatabase();
	}

	public void close() {
		mDatabase.close();
	}
}