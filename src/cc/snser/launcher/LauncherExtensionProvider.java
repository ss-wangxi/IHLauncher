package cc.snser.launcher;

import com.btime.launcher.util.XLog;

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
import static cc.snser.launcher.Constant.LOGD_ENABLED;
import static cc.snser.launcher.Constant.LOGE_ENABLED;

/**
 * 桌面扩展数据库（用于桌面扩展功能的数据存储，如：下载管理、用户中心）
 * @author yangkai
 *
 */
public class LauncherExtensionProvider extends ContentProvider {
    private static final String TAG = "Launcher.LauncherExtensionProvider";

    private static final String DATABASE_NAME = "launcher_ex.db";

    private static final int DATABASE_VERSION = 2;

    public static final String AUTHORITY = "cc.snser.launcher.extension";

    public static final String TABLE_RECOMMEND = "fr";
    public static final String PARAMETER_NOTIFY = "notify";

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "onCreate");
        }
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
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
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);

        return result;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId = db.insert(args.table, null, initialValues);
        if (rowId <= 0) {
            return null;
        }

        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);

        return uri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
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

        sendNotify(uri);
        return values.length;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        try {
            SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            int count = db.delete(args.table, args.where, args.args);
            if (count > 0) {
                sendNotify(uri);
            }

            return count;
        } catch (Exception e) {
        	XLog.e(TAG, "Failed to delete the sql", e);
            return 0;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        try {
            SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            int count = db.update(args.table, values, args.where, args.args);
            if (count > 0) {
                sendNotify(uri);
            }

            return count;
        } catch (Exception e) {
        	XLog.e(TAG, "Failed to update the sql", e);
            return 0;
        }
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {
        private Context mContext;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);

            mContext = context;
        }

        private void createTableForRecommend(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_RECOMMEND + " (" +
                    "_id INTEGER PRIMARY KEY," +
                    "pn TEXT," + //packageName
                    "iu TEXT," + //icon url
                    "an TEXT," + //app name
                    "ad TEXT," + //app desc
                    "ab TEXT," + //app brief
                    "au TEXT," + //apk url
                    "gu TEXT," + //gp url
                    "gbu TEXT," + //global url
                    "mv INTEGER NOT NULL DEFAULT 0," + //min ver
                    "fid INTEGER," + //folder id
                    "ii String," + // interface
                    "ip INTEGER," + // interface page
                    "t LONG," + //timestamp
                    "si INTEGER" + //sort id
                    ");");
        }

        private void dropTablesForRecommend(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECOMMEND);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (LOGD_ENABLED) {
                XLog.d(TAG, "creating new launcher extension database");
            }

            createTableForRecommend(db);
        }

        private void dropAllTables(SQLiteDatabase db) {
            XLog.w(TAG, "Destroying all old data.");

            dropTablesForRecommend(db);
        }

        @SuppressWarnings("unused")
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (LOGD_ENABLED) {
                XLog.d(TAG, "onDowngrade triggered oldVersion: " + oldVersion + ", newVersion: " + newVersion);
            }

            dropAllTables(db);
            onCreate(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (LOGD_ENABLED) {
                XLog.d(TAG, "onUpgrade triggered oldVersion: " + oldVersion + ", newVersion: " + newVersion);
            }

            int version = oldVersion;

            if (version < 2) {
                dropTablesForRecommend(db);
                createTableForRecommend(db);
                version = 2;
            }

            if (version != DATABASE_VERSION) {
                dropAllTables(db);
                onCreate(db);
            }
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

    public static DatabaseHelper getDatabaseHelper(Context context) {
        return new DatabaseHelper(context);
    }
}
