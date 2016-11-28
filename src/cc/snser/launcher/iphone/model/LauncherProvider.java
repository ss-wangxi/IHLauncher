/*
 * Copyright (C) 2008 The Android Open Source Project
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

package cc.snser.launcher.iphone.model;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.Xml;
import cc.snser.launcher.IconCache;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.Utils;
import cc.snser.launcher.LauncherSettings.Favorites;
import cc.snser.launcher.apps.ActionUtils;
import cc.snser.launcher.model.FavoriteGap;
import cc.snser.launcher.model.FavoritesItemInfo;
import cc.snser.launcher.style.SettingPreferences;

import com.btime.launcher.adapter.ChannelLayoutAdapter;
import com.btime.launcher.util.XLog;
import com.btime.launcher.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static cc.snser.launcher.Constant.LOGD_ENABLED;
import static cc.snser.launcher.Constant.LOGE_ENABLED;

public class LauncherProvider extends ContentProvider {
	private static final String TAG = "Launcher.LauncherProvider";

	public static final String DATABASE_DEFAULT_NAME = "launcher_i.db";
	public static final String DATABASE_PROTO_NAME = "launcher_proto.db";
	public static final String DATABASE_DEFAULT_WITH_SECONDARY_NAME = "launcher_i_with_secondary.db";
	public static final String DATABASE_PROTO_WITH_SECONDARY_NAME = "launcher_proto_with_secondary.db";

	private static final int DATABASE_VERSION = 1;
	public static final String AUTHORITY = "cc.snser.launcher.iphone.settings";

	public static final String TABLE_FAVORITES_I = "favorites";
	public static final String TABLE_FOLDER_SCREENS = "folderScreens";
	public static final String TABLE_APPHIDELIST = "apphidelist";
	public static final String TABLE_FOLDERHIDELIST = "folderhidelist";
	public static final String TABLE_FOLDERHIDELIST_SECONDARY = "folderhidelist_secondry";
	public static final String PARAMETER_NOTIFY = "notify";
	public static final String TABLE_FAVORITES_SECONDARY_I = "favorites_secondry";

	public static int APPWIDGET_HOST_ID = 1023;

	private AppWidgetHost mAppWidgetHost;
	private AppWidgetManager mAppWidgetManager;
	/**
	 * {@link Uri} triggered at any registered
	 * {@link android.database.ContentObserver} when
	 * {@link AppWidgetHost#deleteHost()} is called during database creation.
	 * Use this to recall {@link AppWidgetHost#startListening()} if needed.
	 */
	public static final Uri CONTENT_APPWIDGET_RESET_URI = Uri
			.parse("content://" + AUTHORITY + "/appWidgetReset");

	public static String getAuthority() {
		return AUTHORITY + "/" + DATABASE_DEFAULT_NAME;
	}

	private DatabaseHelper mOpenHelper;

	@Override
	public boolean onCreate() {
		if (LOGD_ENABLED) {
			XLog.d(TAG, "onCreate ilauncher");
		}
		mAppWidgetHost = new AppWidgetHost(getContext(), APPWIDGET_HOST_ID);
		mAppWidgetManager = AppWidgetManager.getInstance(getContext());
		return true;
	}

	private DatabaseHelper getDatabaseHelper(String database) {
		if (mOpenHelper != null
				&& mOpenHelper.getDatabaseName().equals(database)) {
			return mOpenHelper;
		}

		mOpenHelper = new DatabaseHelper(getContext(), database, mAppWidgetHost, mAppWidgetManager);

		return mOpenHelper;
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

		SQLiteDatabase db = getDatabaseHelper(args.database)
				.getWritableDatabase();
		
		
		String[] proj = projection;
		Cursor result = qb.query(db, proj, args.where, args.args, null,
				null, sortOrder);
		result.setNotificationUri(getContext().getContentResolver(), uri);

		return result;
	}
	
	

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		SqlArguments args = new SqlArguments(uri);

		SQLiteDatabase db = getDatabaseHelper(args.database)
				.getWritableDatabase();
		final long rowId = db.insert(args.table, null, initialValues);
		if (rowId <= 0) {
			return null;
		}
		
		if(args.table.equalsIgnoreCase(TABLE_FAVORITES_I))
			AppLogDbHelper.insertToAppLog(getContext(), initialValues);
		
		
		uri = ContentUris.withAppendedId(uri, rowId);
		sendNotify(uri);

		return uri;
	}
	
	

	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		SqlArguments args = new SqlArguments(uri);

		SQLiteDatabase db = getDatabaseHelper(args.database)
				.getWritableDatabase();
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
		
		if(args.table.equalsIgnoreCase(TABLE_FAVORITES_I))
			AppLogDbHelper.bulkInsertToAppLog(getContext(), values);

		sendNotify(uri);
		return values.length;
	}
	
	

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

		SQLiteDatabase db = getDatabaseHelper(args.database)
				.getWritableDatabase();
		
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(args.table);
	
		Cursor result = qb.query(db, null, args.where, args.args, null,
			null, null);
		if(args.table.equalsIgnoreCase(TABLE_FAVORITES_I)){
			AppLogDbHelper.deleteAppLog(getContext(), result);
		}
		
		result.close();
		int count = db.delete(args.table, args.where, args.args);
		if (count > 0) {
			sendNotify(uri);
		}

		return count;
	}
	
	

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		try {
			SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
			SQLiteDatabase db = getDatabaseHelper(args.database)
					.getWritableDatabase();
			int count = 0;
			if (values != null
					&& values
							.containsKey(LauncherSettings.BaseLauncherColumns.DECREASE_COLUMN)) {
				String columnName = values
						.getAsString(LauncherSettings.BaseLauncherColumns.DECREASE_COLUMN);
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE ");
				sql.append(args.table);
				sql.append(" SET " + columnName + " = " + columnName + " - 1");
				if (!TextUtils.isEmpty(selection)) {
					sql.append(" WHERE ");
					sql.append(selection);
				}
				if (selectionArgs != null) {
					db.execSQL(sql.toString(), selectionArgs);
				} else {
					db.execSQL(sql.toString());
				}
			} else if (values != null
					&& values
							.containsKey(LauncherSettings.BaseLauncherColumns.INCREASE_COLUMN)) {
				String columnName = values
						.getAsString(LauncherSettings.BaseLauncherColumns.INCREASE_COLUMN);
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE ");
				sql.append(args.table);
				sql.append(" SET " + columnName + " = " + columnName + " + 1");
				if (!TextUtils.isEmpty(selection)) {
					sql.append(" WHERE ");
					sql.append(selection);
				}
				if (selectionArgs != null) {
					db.execSQL(sql.toString(), selectionArgs);
				} else {
					db.execSQL(sql.toString());
				}
			} else {
				count = db.update(args.table, values, args.where, args.args);
				
				if(values.containsKey(AppOperationLogSettings.Log.LAST_CALLED_TIME)
					|| values.containsKey(AppOperationLogSettings.Log.CALLED_NUM)){
					SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
					qb.setTables(args.table);
				
					Cursor result = qb.query(db, null, args.where, args.args, null,
						null, null);
					if(result != null){
						AppLogDbHelper.updateAppLog(getContext(), result, values);
						result.close();
					}
				}
				
			}
			if (count > 0) {
				sendNotify(uri);
			}

			return count;
		} catch (Exception e) {
			if (LOGE_ENABLED) {
				XLog.e(TAG, "Failed to update the sql", e);
			}
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
		public static final String TAG_FAVORITES = "favorites";
		public static final String TAG_FAVORITE = "favorite";
		public static final String TAG_SHORTCUT = "shortcut";
		public static final String TAG_WIDGET_VIEW = "widgetview";
		public static final String TAG_HOME_USER_FOLDER = "homeuserfolder";
		public static final String TAG_SUGGEST = "suggest";
		public static final String TAG_CATEGORY = "category";

		private final Context mContext;
		private final AppWidgetHost mAppWidgetHost;
		private final AppWidgetManager mAppWidgetManager;

		DatabaseHelper(Context context, String database, AppWidgetHost widgetHost, 
				AppWidgetManager widgetMgr) {
			super(context, database, null, DATABASE_VERSION);
			mContext = context;
			//mAppWidgetHost = new AppWidgetHost(context, APPWIDGET_HOST_ID);
			//mAppWidgetManager = AppWidgetManager.getInstance(context);
			mAppWidgetHost = widgetHost;
			mAppWidgetManager = widgetMgr;
		}

		/**
		 * Send notification that we've deleted the {@link AppWidgetHost},
		 * probably as part of the initial database creation. The receiver may
		 * want to re-call {@link AppWidgetHost#startListening()} to ensure
		 * callbacks are correctly set.
		 */
		private void sendAppWidgetResetNotify() {
			final ContentResolver resolver = mContext.getContentResolver();
			resolver.notifyChange(CONTENT_APPWIDGET_RESET_URI, null);
			// DbManager.notifyChange(mContext, CONTENT_APPWIDGET_RESET_URI);
		}

		private void createFavoritesTable(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITES_I + " ("
					+ "_id INTEGER PRIMARY KEY," + "title TEXT,"
					+ "intent TEXT," + "container INTEGER," + "screen INTEGER,"
					+ "cellX INTEGER," + "cellY INTEGER," + "spanX INTEGER,"
					+ "spanY INTEGER," + "itemType INTEGER,"
					+ "appWidgetId INTEGER NOT NULL DEFAULT -1,"
					+ "isShortcut INTEGER," + "iconType INTEGER,"
					+ "iconPackage TEXT," + "iconResource TEXT,"
					+ "titlePackage TEXT," + "titleResource TEXT,"
					+ "icon BLOB," 
					+ "last_update_time INTEGER DEFAULT 0,"
					+ "last_called_time INTEGER DEFAULT 0,"
					+ "called_num INTEGER DEFAULT 0,"
					+ "storage INTEGER NOT NULL DEFAULT -1,"
					+ "system INTEGER NOT NULL DEFAULT 0,"
					+ "category INTEGER NOT NULL DEFAULT -1" + ");");
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_APPHIDELIST + " ("
					+ "_id INTEGER PRIMARY KEY," + "intent TEXT" + ");");
			db.execSQL("CREATE TABLE IF NOT EXISTS "
					+ TABLE_FAVORITES_SECONDARY_I + " ("
					+ "_id INTEGER PRIMARY KEY," + "title TEXT,"
					+ "intent TEXT," + "container INTEGER," + "screen INTEGER,"
					+ "cellX INTEGER," + "cellY INTEGER," + "spanX INTEGER,"
					+ "spanY INTEGER," + "itemType INTEGER,"
					+ "appWidgetId INTEGER NOT NULL DEFAULT -1,"
					+ "isShortcut INTEGER," + "iconType INTEGER,"
					+ "iconPackage TEXT," + "iconResource TEXT,"
					+ "titlePackage TEXT," + "titleResource TEXT,"
					+ "icon BLOB," + "last_update_time INTEGER DEFAULT 0,"
					+ "last_called_time INTEGER DEFAULT 0,"
					+ "called_num INTEGER DEFAULT 0,"
					+ "storage INTEGER NOT NULL DEFAULT -1,"
					+ "system INTEGER NOT NULL DEFAULT 0,"
					+ "category INTEGER NOT NULL DEFAULT -1" + ");");
		}

		private void createFolderScreenTable(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_FOLDER_SCREENS + " ("
					+ LauncherSettings.FolderScreens._ID + " INTEGER,"
					+ LauncherSettings.FolderScreens.FOLDER_ID + " INTEGER,"
					+ LauncherSettings.FolderScreens.MODIFIED
					+ " INTEGER NOT NULL DEFAULT 0" + ");");
		}

		private void createFolderHiddenTable(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE "
					+ TABLE_FOLDERHIDELIST
					+ " ("
					+ LauncherSettings.FolderHidden.FOLDER_ID
					+ " INTEGER,"
					+ LauncherSettings.FolderHidden.IS_FOLDER_HIDDEN
					+ " INTEGER,"
					+ LauncherSettings.FolderHidden.FOLDER_HIDDEN_COUNT
					+ " INTEGER NOT NULL DEFAULT 0,"
					+ LauncherSettings.FolderHidden.FOLDER_HIDDEN_SCHEDULE_BEGIN_HOUR
					+ " INTEGER NOT NULL DEFAULT 8,"
					+ LauncherSettings.FolderHidden.FOLDER_HIDDEN_SCHEDULE_BEGIN_MIN
					+ " INTEGER NOT NULL DEFAULT 0,"
					+ LauncherSettings.FolderHidden.FOLDER_HIDDEN_SCHEDULE_END_HOUR
					+ " INTEGER NOT NULL DEFAULT 20,"
					+ LauncherSettings.FolderHidden.FOLDER_HIDDEN_SCHEDULE_END_MIN
					+ " INTEGER NOT NULL DEFAULT 0,"
					+ LauncherSettings.FolderHidden.FOLDER_HIDDEN_SCHEDULE_ENABLE
					+ " INTEGER," + LauncherSettings.FolderHidden.MODIFIED
					+ " INTEGER NOT NULL DEFAULT 0" + ");");
			db.execSQL("CREATE TABLE "
					+ TABLE_FOLDERHIDELIST_SECONDARY
					+ " ("
					+ LauncherSettings.FolderHidden.FOLDER_ID
					+ " INTEGER,"
					+ LauncherSettings.FolderHidden.IS_FOLDER_HIDDEN
					+ " INTEGER,"
					+ LauncherSettings.FolderHidden.FOLDER_HIDDEN_COUNT
					+ " INTEGER NOT NULL DEFAULT 0,"
					+ LauncherSettings.FolderHidden.FOLDER_HIDDEN_SCHEDULE_BEGIN_HOUR
					+ " INTEGER NOT NULL DEFAULT 8,"
					+ LauncherSettings.FolderHidden.FOLDER_HIDDEN_SCHEDULE_BEGIN_MIN
					+ " INTEGER NOT NULL DEFAULT 0,"
					+ LauncherSettings.FolderHidden.FOLDER_HIDDEN_SCHEDULE_END_HOUR
					+ " INTEGER NOT NULL DEFAULT 20,"
					+ LauncherSettings.FolderHidden.FOLDER_HIDDEN_SCHEDULE_END_MIN
					+ " INTEGER NOT NULL DEFAULT 0,"
					+ LauncherSettings.FolderHidden.FOLDER_HIDDEN_SCHEDULE_ENABLE
					+ " INTEGER," + LauncherSettings.FolderHidden.MODIFIED
					+ " INTEGER NOT NULL DEFAULT 0" + ");");
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			createFavoritesTable(db);
			createFolderScreenTable(db);
			createFolderHiddenTable(db);
			loadFavoritesI(db);
		}

		private void dropAllTables(SQLiteDatabase db) {
			XLog.w(TAG, "Destroying all old data.");
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES_I);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPHIDELIST);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES_SECONDARY_I);
		}

		@SuppressWarnings("unused")
		public void onDowngrade(SQLiteDatabase db, int oldVersion,
				int newVersion) {
			dropAllTables(db);
			onCreate(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (LOGD_ENABLED) {
				XLog.d(TAG, "onUpgrade triggered oldVersion: " + oldVersion
						+ ", newVersion: " + newVersion);
			}

			int version = oldVersion;

			if (version < 1) {
				version = 1;
			}

			if (version != DATABASE_VERSION) {
				dropAllTables(db);
				onCreate(db);
			}
		}
		
		private long insertToDbWithAppLog(SQLiteDatabase db, String table, String nullColumnHack, ContentValues values){
			long ret = db.insert(table, nullColumnHack, values);
			String rawIntent = values.getAsString(LauncherSettings.BaseLauncherColumns.INTENT);
			ComponentName cn = null;
			
			try{
				Intent intent = Intent.parseUri(rawIntent, 0);
				cn = intent.getComponent();
			}catch(Exception e){
				
			}
			
			if(cn == null) return ret;
			
		
			Long lastCalledTime = values.getAsLong(IphoneUtils.FavoriteExtension.LAST_CALLED_TIME);
			Long calledNum = values.getAsLong(IphoneUtils.FavoriteExtension.CALLED_NUM);
			
			ContentValues logValues = new ContentValues();
			logValues.put(AppOperationLogSettings.Log.COMPONENT, cn.flattenToString());
			logValues.put(AppOperationLogSettings.Log.LAST_CALLED_TIME, lastCalledTime == null ? 0 : lastCalledTime);
			logValues.put(AppOperationLogSettings.Log.CALLED_NUM, calledNum);
			ContentResolver cr = mContext.getContentResolver();
			cr.insert(AppOperationLogSettings.Log.CONTENT_URI, logValues);
			
			return ret;
		}

		
		private void saveInfoToDB(SQLiteDatabase db, FavoritesItemInfo info, boolean fSaveToSecondLayer) {
			ContentValues values = new ContentValues();

			values.put(IphoneUtils.FavoriteExtension._ID, info.id);
			values.put(IphoneUtils.FavoriteExtension.TITLE, info.title);
			values.put(IphoneUtils.FavoriteExtension.INTENT, info.intent);
			values.put(IphoneUtils.FavoriteExtension.CONTAINER, info.container);
			values.put(IphoneUtils.FavoriteExtension.SCREEN, info.screen);
			values.put(IphoneUtils.FavoriteExtension.CELLX, info.cellX);
			values.put(IphoneUtils.FavoriteExtension.CELLY, info.cellY);
			values.put(IphoneUtils.FavoriteExtension.SPANX, info.spanX);
			values.put(IphoneUtils.FavoriteExtension.SPANY, info.spanY);

			values.put(IphoneUtils.FavoriteExtension.ITEM_TYPE, info.itemType);
			values.put(IphoneUtils.FavoriteExtension.APPWIDGET_ID,
					info.appWidgetId);
			values.put(IphoneUtils.FavoriteExtension.ICON_TYPE, info.iconType);

			values.put(IphoneUtils.FavoriteExtension.ICON_PACKAGE,
					info.iconPackage);
			values.put(IphoneUtils.FavoriteExtension.ICON_RESOURCE,
					info.iconResource);
			values.put(IphoneUtils.FavoriteExtension.ICON, info.icon);
			values.put(IphoneUtils.FavoriteExtension.TITLE_RESOURCE,
					info.titleResource);

			if (fSaveToSecondLayer) {
				//db.insert(TABLE_FAVORITES_SECONDARY_I, null, values);
				insertToDbWithAppLog(db, TABLE_FAVORITES_SECONDARY_I, null, values);
			} else {
				//db.insert(TABLE_FAVORITES_I, null, values);
				insertToDbWithAppLog(db, TABLE_FAVORITES_I, null, values);
			}
		}

		/**
		 * Loads the default set of favorite packages from an xml file.
		 * 
		 * @param db
		 *            The database to write the values into
		 */
		private int loadFavoritesI(SQLiteDatabase db) {
			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			Intent mainIntentForQuery = new Intent(Intent.ACTION_MAIN, null);
			mainIntentForQuery.addCategory(Intent.CATEGORY_LAUNCHER);
			ContentValues values = new ContentValues();

			PackageManager packageManager = mContext.getPackageManager();
			int i = 0;
			try {
	            final int xmlResid = ChannelLayoutAdapter.getWorkspaceContentViewResid(mContext);
	            XmlResourceParser parser = mContext.getResources().getXml(xmlResid);
				AttributeSet attrs = Xml.asAttributeSet(parser);

				final Map<Integer, Integer> idMappings = new HashMap<Integer, Integer>();
				final Map<Integer, Integer> referMappings = new HashMap<Integer, Integer>();
				final Set<String> existingSuggestedPackageNames = new HashSet<String>();
				final int depth = parser.getDepth();
				final int yCount = SettingPreferences.getHomeLayout(mContext)[0];
				final Map<String, String> packageNameMappings = IconCache
						.getInstance(mContext).getPackageNameMappings();
				final SparseArray<Map<String, Integer>> bottomSuggestion = new SparseArray<Map<String, Integer>>();
				int type;

				while (((type = parser.next()) != XmlPullParser.END_TAG || parser
						.getDepth() > depth)
						&& type != XmlPullParser.END_DOCUMENT) {

					if (type != XmlPullParser.START_TAG) {
						continue;
					}

					boolean added = false;
					final String name = parser.getName();

					TypedArray a = mContext.obtainStyledAttributes(attrs,
							R.styleable.Favorite);

					values.clear();
					int container = a.getInteger(
							R.styleable.Favorite_container,
							IphoneUtils.FavoriteExtension.CONTAINER_DESKTOP);
					if (container > 0) {
						if (TAG_HOME_USER_FOLDER.equals(name)) {
							container = IphoneUtils.FavoriteExtension.CONTAINER_DESKTOP;
						} else {
							if (idMappings.containsKey(container)) {
								container = idMappings.get(container);
							} else {
								container = IphoneUtils.FavoriteExtension.CONTAINER_DESKTOP;
							}
						}
					}
					values.put(IphoneUtils.FavoriteExtension.CONTAINER,
							container);
					values.put(IphoneUtils.FavoriteExtension.SCREEN,
							a.getString(R.styleable.Favorite_screen));
					values.put(IphoneUtils.FavoriteExtension.CELLX,
							a.getString(R.styleable.Favorite_x));

					boolean replaced = false;
					try {
						int maxYCount = a.getInt(
								R.styleable.Favorite_maxYCount, -1);
						if (maxYCount >= 0) {
							int cellY = a.getInt(R.styleable.Favorite_y, 0);
							if (cellY < maxYCount) {
								cellY += yCount - maxYCount;
								values.put(IphoneUtils.FavoriteExtension.CELLY,
										cellY);
								replaced = true;
							}
						}
					} catch (Exception e) {
						// ignore
					}

					if (!replaced) {
						values.put(IphoneUtils.FavoriteExtension.CELLY,
								a.getString(R.styleable.Favorite_y));
					}

					if (TAG_FAVORITE.equals(name)) {
						added = addAppShortcut(db, TABLE_FAVORITES_I, values,
								referMappings, a, packageManager, mainIntent,
								mainIntentForQuery, packageNameMappings,
								existingSuggestedPackageNames);
					} else if (TAG_WIDGET_VIEW.equals(name)) {
						added = addWidgetView(db, TABLE_FAVORITES_I, values,
								referMappings, a, packageManager);
					} else if (TAG_SHORTCUT.equals(name)) {
						added = addUriShortcut(db, TABLE_FAVORITES_I, values,
								a, packageManager);
					} else if (TAG_HOME_USER_FOLDER.equals(name)) {
						added = addHomeUserFolder(db, TABLE_FAVORITES_I,
								values, idMappings, a);
					} else if (TAG_SUGGEST.equals(name)) {
						added = addSuggest(db, TABLE_FAVORITES_I, values,
								referMappings, a,
								existingSuggestedPackageNames, bottomSuggestion);
					}
					if (added) {
						i++;
					}

					a.recycle();
				}

				addBottomSuggestion(db, TABLE_FAVORITES_I, bottomSuggestion,
						existingSuggestedPackageNames);

			} catch (XmlPullParserException e) {
			    XLog.printStackTrace(e);
				//XLog.w(TAG, "Got exception parsing favorites.", e);
			} catch (IOException e) {
			    XLog.printStackTrace(e);
				//XLog.w(TAG, "Got exception parsing favorites.", e);
			}

			return i;
		}

		private boolean addAppShortcut(SQLiteDatabase db, String tableName,
				ContentValues values, Map<Integer, Integer> referMappings,
				TypedArray a, PackageManager packageManager, Intent mainIntent,
				Intent mainIntentForQuery,
				Map<String, String> packageNameMappings,
				Set<String> existingSuggestedPackageNames) {

			int iconResId = a.getResourceId(R.styleable.Favorite_icon, 0);
			Resources r = mContext.getResources();
			ActivityInfo info = null;
			ComponentName cn = null;

			try {
				cn = ActionUtils.findBestMatchedComponent(mContext,
						a.getString(R.styleable.Favorite_packageName),
						a.getString(R.styleable.Favorite_className),
						a.getString(R.styleable.Favorite_action),
						a.getString(R.styleable.Favorite_uri),
						a.getString(R.styleable.Favorite_type),
						a.getString(R.styleable.Favorite_category));

				if (cn == null) {
					FavoriteGap.GapPosition gapPos = new FavoriteGap.GapPosition();
					gapPos.mContainer = values
							.getAsInteger(IphoneUtils.FavoriteExtension.CONTAINER);
					gapPos.mScreen = Integer
							.valueOf(values
									.getAsString(IphoneUtils.FavoriteExtension.SCREEN));
					gapPos.mCellX = Integer
							.valueOf(values
									.getAsString(IphoneUtils.FavoriteExtension.CELLX));
					gapPos.mCellY = Integer
							.valueOf(values
									.getAsString(IphoneUtils.FavoriteExtension.CELLY));
					FavoriteGap.getInstance().addGapPosition(gapPos);
					return false;
				}

				try {
					info = packageManager.getActivityInfo(cn, 0);
				} catch (PackageManager.NameNotFoundException nnfe) {
					String[] packages = packageManager
							.currentToCanonicalPackageNames(new String[] { cn
									.getPackageName() });
					cn = new ComponentName(packages[0], cn.getClassName());
					info = packageManager.getActivityInfo(cn, 0);
				}

				mainIntent.setComponent(cn);
				mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

				values.put(IphoneUtils.FavoriteExtension.INTENT,
						mainIntent.toUri(0));
				values.put(IphoneUtils.FavoriteExtension.TITLE,
						info.loadLabel(packageManager).toString());
				values.put(IphoneUtils.FavoriteExtension.ITEM_TYPE,
						IphoneUtils.FavoriteExtension.ITEM_TYPE_APPLICATION);
				values.put(IphoneUtils.FavoriteExtension.SPANX, 1);
				values.put(IphoneUtils.FavoriteExtension.SPANY, 1);

				values.put(IphoneUtils.FavoriteExtension.LAST_UPDATE_TIME,
						new File(info.applicationInfo.sourceDir).lastModified());
				values.put(
						IphoneUtils.FavoriteExtension.SYSTEM,
						info.applicationInfo.flags
								& android.content.pm.ApplicationInfo.FLAG_SYSTEM);
				values.put(IphoneUtils.FavoriteExtension.STORAGE,
						Utils.getApplicationStorage(info));

				if (iconResId != 0) {
					values.put(IphoneUtils.FavoriteExtension.ICON_TYPE,
							IphoneUtils.FavoriteExtension.ICON_TYPE_RESOURCE);
					values.put(IphoneUtils.FavoriteExtension.ICON_PACKAGE,
							mContext.getPackageName());
					values.put(IphoneUtils.FavoriteExtension.ICON_RESOURCE,
							r.getResourceName(iconResId));
				}

				//long ret = db.insert(tableName, null, values);
				long ret = insertToDbWithAppLog(db, tableName, null, values);

				int id = a.getInteger(R.styleable.Favorite_refer_id, 0);
				if (id > 0) {
					referMappings.put(id, (int) ret);
				}

			} catch (Exception e) {
				//XLog.w(TAG, "Unable to add favorite: " + cn, e);
				return false;
			}
			return true;
		}

		private int addBottomSuggestion(SQLiteDatabase db, String tableName,
				SparseArray<Map<String, Integer>> bottomSuggestion,
				Set<String> existingSuggestedPackageNames) {
			ContentValues values = new ContentValues();
			int added = 0;
			for (int i = 0; i < bottomSuggestion.size(); i++) {
				int screen = bottomSuggestion.keyAt(i);

				if (!bottomSuggestion.get(screen).containsKey("index")
						|| !bottomSuggestion.get(screen).containsKey("size"))
					continue;
				int index = bottomSuggestion.get(screen).get("index");

				for (int shiftPos = 0; shiftPos < bottomSuggestion.get(screen)
						.get("size"); shiftPos++) {
					int actualX = (index + 1 + shiftPos) % 4;
					int actualY = (index + 1 + shiftPos) / 4;

					values.clear();
					
					FavoriteGap.GapPosition gapPos = new FavoriteGap.GapPosition();
					gapPos.mContainer = IphoneUtils.FavoriteExtension.CONTAINER_DESKTOP;
					gapPos.mScreen = screen;
					gapPos.mCellX = actualX;
					gapPos.mCellY = actualY;
					FavoriteGap.getInstance().addGapPosition(gapPos);
				}

			}
			return added;
		}

		private boolean addSuggest(SQLiteDatabase db, String tableName,
				ContentValues values, Map<Integer, Integer> referMappings,
				TypedArray a, Set<String> existingSuggestedPackageNames,
				SparseArray<Map<String, Integer>> bottomSuggestion) {

			int container = values
					.getAsInteger(IphoneUtils.FavoriteExtension.CONTAINER);
			int cellX = values
					.getAsInteger(IphoneUtils.FavoriteExtension.CELLX);
			int cellY = values
					.getAsInteger(IphoneUtils.FavoriteExtension.CELLY);
			int index = cellY * 4 + cellX;
			int screen = values
					.getAsInteger(IphoneUtils.FavoriteExtension.SCREEN);
			int shiftPos = 0;
			if (container == IphoneUtils.FavoriteExtension.CONTAINER_DESKTOP) {

				if (bottomSuggestion.get(screen) != null
						&& bottomSuggestion.get(screen).containsKey("size")) {
					shiftPos = bottomSuggestion.get(screen).get("size");
				}
				if (shiftPos > 0) {
					int actualX = (index - shiftPos) % 4;
					int actualY = (index - shiftPos) / 4;
					index = actualY * 4 + actualX;
					values.put(IphoneUtils.FavoriteExtension.CELLX, actualX);
					values.put(IphoneUtils.FavoriteExtension.CELLY, actualY);

				}
			}

			int id = a.getInteger(R.styleable.Favorite_refer, 0);

			if (id > 0 && referMappings.containsKey(id)) {
				long existingId = referMappings.get(id);

				if (bottomSuggestion.get(screen) != null) {
					bottomSuggestion.get(screen).put("index", index);
				}

				db.update(tableName, values,
						Favorites._ID + " = " + existingId, null);

				return true;
			}
			// Add bottom suggestion list
			if (container == IphoneUtils.FavoriteExtension.CONTAINER_DESKTOP) {
				if (bottomSuggestion.get(screen) != null) {
					int size = bottomSuggestion.get(screen).get("size");
					bottomSuggestion.get(screen).put("size", size + 1);
				} else {
					Map<String, Integer> indexSize = new HashMap<String, Integer>();
					indexSize.put("size", 1);
					indexSize.put("index", index - 1);
					bottomSuggestion.put(screen, indexSize);
				}
			}

			return false;
		}

		private boolean addWidgetView(SQLiteDatabase db, String tableName,
				ContentValues values, Map<Integer, Integer> referMappings,
				TypedArray a, PackageManager packageManager) {
			int widgetViewType = a.getInt(R.styleable.Favorite_widgetViewType,
					-1);
			int spanX = a.getInt(R.styleable.Favorite_spanX, 1);
			int spanY = a.getInt(R.styleable.Favorite_spanY, 1);

			values.put(IphoneUtils.FavoriteExtension.ITEM_TYPE,
					IphoneUtils.FavoriteExtension.ITEM_TYPE_WIDGET_VIEW);
			values.put(IphoneUtils.FavoriteExtension.SPANX, spanX);
			values.put(IphoneUtils.FavoriteExtension.SPANY, spanY);

			values.put(IphoneUtils.FavoriteExtension.APPWIDGET_ID,
					widgetViewType);

			//long ret = db.insert(tableName, null, values);
			long ret = insertToDbWithAppLog(db, tableName, null, values);

			int id = a.getInteger(R.styleable.Favorite_refer_id, 0);
			if (id > 0) {
				referMappings.put(id, (int) ret);
			}

			return true;
		}

		private boolean addUriShortcut(SQLiteDatabase db, String tableName,
				ContentValues values, TypedArray a,
				PackageManager packageManager) {

			if (!setShortcutContentValue(mContext, values, a, packageManager)) {
				return false;
			}

			/*
			 * if
			 * (ChannelUtils.CHANNEL_HUAWEI.equals(ChannelUtils.getLcFromAssets
			 * (mContext))) { try { Intent intent =
			 * Intent.parseUri(values.getAsString
			 * (IphoneUtils.FavoriteExtension.INTENT), 0); if
			 * (CustomShortcutAction
			 * .ACTION_TYPE_APP_STORE.equals(intent.getType()) &&
			 * !AppUtils.isPackageExists(mContext,
			 * IntegrateUtils.HUAWEI_MARKET_PACKAGENAME)) { return false; } }
			 * catch (Throwable e) { } }
			 */

			if (values.containsKey(IphoneUtils.FavoriteExtension._ID)) {
				db.update(
						tableName,
						values,
						IphoneUtils.FavoriteExtension._ID + " = "
								+ values.get(IphoneUtils.FavoriteExtension._ID),
						null);
			} else {
				//db.insert(tableName, null, values);
				insertToDbWithAppLog(db, tableName, null, values);
			}

			return true;
		}

		private boolean addHomeUserFolder(SQLiteDatabase db, String tableName,
				ContentValues values, Map<Integer, Integer> idMappings,
				TypedArray a) {
			Resources r = mContext.getResources();

			final int titleResId = a.getResourceId(R.styleable.Favorite_title,
					0);
			final int folderCategory = a.getInt(
					R.styleable.Favorite_app_category, -1);

			if (titleResId == 0) {
				XLog.w(TAG, "HomeUserFolder is missing title resource ID");
				return false;
			}

			values.put(IphoneUtils.FavoriteExtension.TITLE,
					r.getString(titleResId));
			values.put(IphoneUtils.FavoriteExtension.ITEM_TYPE,
					IphoneUtils.FavoriteExtension.ITEM_TYPE_USER_FOLDER);
			values.put(IphoneUtils.FavoriteExtension.SPANX, 1);
			values.put(IphoneUtils.FavoriteExtension.SPANY, 1);
			values.put(IphoneUtils.FavoriteExtension.CATEGORY, folderCategory);

			//long ret = db.insert(tableName, null, values);
			long ret = insertToDbWithAppLog(db, tableName, null, values);

			int id = a.getInteger(R.styleable.Favorite_id, 0);
			if (id > 0) {
				idMappings.put(id, (int) ret);
			}

			return true;
		}
	}

	/**
	 * read data from xml and fillin ContentValues <br>
	 * TODO extract relate method to a single Util class to set contentValues
	 * 
	 * @param context
	 * @param values
	 * @param a
	 * @param packageManager
	 * @return
	 */
	public static boolean setShortcutContentValue(Context context,
			ContentValues values, TypedArray a, PackageManager packageManager) {
		final int iconResId = a.getResourceId(R.styleable.Favorite_icon, 0);
		final int titleResId = a.getResourceId(R.styleable.Favorite_title, 0);

		/*
		 * if (iconResId == 0 || titleResId == 0) { XLog.w(TAG,
		 * "Shortcut is missing title or icon resource ID"); return false; }
		 */

		Resources r = context.getResources();

		String action = a.getString(R.styleable.Favorite_action);
		String uri = a.getString(R.styleable.Favorite_uri);
		String type = a.getString(R.styleable.Favorite_type);
		String category = a.getString(R.styleable.Favorite_category);

		Intent intent = null;
		try {
			boolean alreadyFound = false;
			/*
			 * if (Intent.CATEGORY_BROWSABLE.equals(category)) { try {
			 * ComponentName componentName = new
			 * ComponentName("com.android.browser",
			 * "com.android.browser.BrowserActivity");
			 * packageManager.getActivityInfo(componentName, 0); intent = new
			 * Intent(Intent.ACTION_MAIN, null);
			 * intent.setComponent(componentName);
			 * intent.addCategory(Intent.CATEGORY_LAUNCHER);
			 * 
			 * intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
			 * Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			 * 
			 * alreadyFound = true; } catch (NameNotFoundException
			 * localNameNotFoundException) { // ignore } }
			 */
			if (!alreadyFound) {
				Uri changedUri = null;
				if (action != null && uri != null) {
					changedUri = Uri.parse(uri);
					intent = new Intent(action, changedUri);
				} else if (action != null) {
					intent = new Intent(action);
				} else if (uri != null) {
					intent = Intent.parseUri(uri, 0);
					changedUri = intent.getData();
				} else {
					XLog.w(TAG, "Shortcut has no action or uri.");
					return false;
				}

				if (type != null && changedUri == null) {
					intent.setType(type);
				}
				if (category != null) {
					intent.addCategory(category);
				}

				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			}
		} catch (Exception e) {
		    XLog.printStackTrace(e);
/*			XLog.w(TAG, "Shortcut has malformed uri: " + uri + " for action: "
					+ action + ".", e);
*/			return false; // Oh well
		}

		values.put(IphoneUtils.FavoriteExtension.INTENT, intent.toUri(0));
		values.put(IphoneUtils.FavoriteExtension.ITEM_TYPE,
				IphoneUtils.FavoriteExtension.ITEM_TYPE_SHORTCUT);

		// values.put(Favorites.TITLE, r.getString(titleResId));
		if (titleResId != 0) {
			values.put(IphoneUtils.FavoriteExtension.TITLE_RESOURCE,
					r.getResourceName(titleResId));
		}
		values.put(IphoneUtils.FavoriteExtension.SPANX, 1);
		values.put(IphoneUtils.FavoriteExtension.SPANY, 1);
		values.put(IphoneUtils.FavoriteExtension.ICON_TYPE,
				IphoneUtils.FavoriteExtension.ICON_TYPE_RESOURCE);
		values.put(IphoneUtils.FavoriteExtension.ICON_PACKAGE,
				context.getPackageName());
		if (iconResId != 0) {
			values.put(IphoneUtils.FavoriteExtension.ICON_RESOURCE,
					r.getResourceName(iconResId));
		}

		return true;
	}

	static class SqlArguments {
		public final String database;
		public final String table;
		public final String where;
		public final String[] args;

		SqlArguments(Uri url, String where, String[] args) {
			if (url.getPathSegments().size() == 1) {
				this.database = null;
				this.table = url.getPathSegments().get(0);
				this.where = where;
				this.args = args;
			} else if (url.getPathSegments().size() == 2) {
				this.database = url.getPathSegments().get(0);
				this.table = url.getPathSegments().get(1);
				this.where = where;
				this.args = args;
			} else if (url.getPathSegments().size() != 3) {
				throw new IllegalArgumentException("Invalid URI: " + url);
			} else if (!TextUtils.isEmpty(where)) {
				throw new UnsupportedOperationException(
						"WHERE clause not supported: " + url);
			} else {
				this.database = url.getPathSegments().get(0);
				this.table = url.getPathSegments().get(1);
				this.where = "_id=" + ContentUris.parseId(url);
				this.args = null;
			}
		}

		SqlArguments(Uri url) {
			if (url.getPathSegments().size() == 2) {
				database = url.getPathSegments().get(0);
				table = url.getPathSegments().get(1);
				where = null;
				args = null;
			} else {
				throw new IllegalArgumentException("Invalid URI: " + url);
			}
		}
	}
}
