package cc.snser.launcher.iphone.model;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class AppOperationLogProvider extends ContentProvider{

	public static final String DATABASE_NAME = "app_log.db";
	
	public static final String TABLE_APPLOG = "log";
	private static final int DATABASE_VERSION = 1;
	public static final String AUTHORITY = "cc.snser.launcher.iphone.applog";
	private DatabaseHelper mOpenHelper;
	
	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return false;
	}
	
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        return count;
	}

	@Override
	public String getType(Uri uri) {
		SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
	}

	@Override
	public Uri insert(Uri uri, ContentValues value) {
		SqlArguments args = new SqlArguments(uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		final long rowId = db.insertWithOnConflict(args.table, null, value, SQLiteDatabase.CONFLICT_IGNORE);
		
		if( rowId <= 0) return null;
		
		uri = ContentUris.withAppendedId(uri, rowId);
		return uri;
	}

	

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(args.table);
		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
		
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
		return count;
	}
	
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values){
		SqlArguments args = new SqlArguments(uri);

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			int numValues = values.length;
			for (int i = 0; i < numValues; i++) {
				if (db.insert(args.table, null, values[i]) < 0) {
					return 0;
				}
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		return values.length;
	}
	

	
	private static class DatabaseHelper extends SQLiteOpenHelper{
		
		DatabaseHelper(Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE "+ TABLE_APPLOG + " (" +
					"_id INTEGER PRIMARY KEY," +
					"component TEXT UNIQUE," +
					"last_called_time INTEGER DEFAULT 0," +
					"called_num INTEGER DEFAULT 0);");
			
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
			
			
		}
	}
	
	static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }

}
