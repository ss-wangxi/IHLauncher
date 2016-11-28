package cc.snser.launcher.iphone.model;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import cc.snser.launcher.LauncherSettings;

class AppLogDbHelper {
	public static boolean findProjection(String[] projection){
		boolean hasAppLog = false;
		
		for(int i = 0; i < projection.length; i ++){
			if(projection[i].equals(AppOperationLogSettings.Log.CALLED_NUM)
					   || projection[i].equals(AppOperationLogSettings.Log.CALLED_NUM)){
				hasAppLog = true;
				break;
			}
		}
			
		return hasAppLog;
	}
	
	public static Cursor queryWithLog(Context context, Cursor result, String[] projection){
		String[] columnNames = projection != null ? projection : result.getColumnNames();
		MatrixCursor cursor = new MatrixCursor(columnNames);
		
		ContentResolver cr = context.getContentResolver();
		while(result.moveToNext()){
			String rawIntent = result.getString(result.getColumnIndex(LauncherSettings.Favorites.INTENT));
			ComponentName cn = null;
			try{
				Intent intent = Intent.parseUri(rawIntent, 0);
				cn = intent.getComponent();
			}catch(Exception e){
				
			}
			long lastCalledTime = 0;
			long calledNum = 0;
			
			if(cn != null){
				Cursor logCursor = cr.query(AppOperationLogSettings.Log.CONTENT_URI, null, AppOperationLogSettings.Log.COMPONENT + "=\"" + cn.flattenToString() + "\"", null, null);
				if(logCursor != null){
					if(logCursor.moveToNext()){
						lastCalledTime = logCursor.getLong(logCursor.getColumnIndex(AppOperationLogSettings.Log.LAST_CALLED_TIME));
						calledNum = logCursor.getLong(logCursor.getColumnIndex(AppOperationLogSettings.Log.CALLED_NUM));
					}
					logCursor.close();
				}
			}
			
			RowBuilder rowBuilder = cursor.newRow();
			for(int i = 0; i < columnNames.length; i ++){
				int index = result.getColumnIndex(columnNames[i]);
				int type = result.getType(index);
				
				switch(type){
				case Cursor.FIELD_TYPE_NULL:
					rowBuilder.add(null);
					break;
				case Cursor.FIELD_TYPE_INTEGER:
					if(columnNames[i].equals(AppOperationLogSettings.Log.LAST_CALLED_TIME)){
						rowBuilder.add(lastCalledTime);
					}else if(columnNames[i].equals(AppOperationLogSettings.Log.CALLED_NUM)){
						rowBuilder.add(calledNum);
					}else{
						rowBuilder.add(result.getInt(index));
					}
					break;
				case Cursor.FIELD_TYPE_FLOAT:
					rowBuilder.add(result.getFloat(index));
					break;
				case Cursor.FIELD_TYPE_STRING:
					rowBuilder.add(result.getString(index));
					break;
				case Cursor.FIELD_TYPE_BLOB:
					rowBuilder.add(result.getBlob(index));
					break;
				default:
					break;
				}
			}
			
		}
	
		return cursor;
	}
	
	public static void insertToAppLog(Context context, ContentValues initialValues){
		Integer type = initialValues.getAsInteger(LauncherSettings.Favorites.ITEM_TYPE);
		if(type == null){
			return;
		}
		
		if(type.intValue() != 0){
			return;
		}
		String rawIntent = initialValues.getAsString(LauncherSettings.Favorites.INTENT);
		ComponentName cn = null;
		try{
			Intent intent = Intent.parseUri(rawIntent, 0);
			cn = intent.getComponent();
		}catch(Exception e){
			
		}
		
		if(cn == null) return;
		
		long lastCalledTime = 0;
		long calledNum = 0;
		if(initialValues.containsKey(IphoneUtils.FavoriteExtension.LAST_CALLED_TIME)){
			Long time = initialValues.getAsLong(IphoneUtils.FavoriteExtension.LAST_CALLED_TIME);
			if(time != null){
				lastCalledTime = time.longValue();
			}
		}
		
		if(initialValues.containsKey(IphoneUtils.FavoriteExtension.CALLED_NUM)){
			Long num = initialValues.getAsLong(IphoneUtils.FavoriteExtension.CALLED_NUM);
			if(num != null){
				calledNum = num.longValue();
			}
		}
		ContentValues values = new ContentValues();
		values.put(AppOperationLogSettings.Log.COMPONENT, cn.flattenToString());
		values.put(AppOperationLogSettings.Log.LAST_CALLED_TIME, lastCalledTime);
		values.put(AppOperationLogSettings.Log.CALLED_NUM, calledNum);
		
		ContentResolver contentResolver = context.getContentResolver();
		contentResolver.insert(AppOperationLogSettings.Log.CONTENT_URI, values);
		
	}
	
	public static int bulkInsertToAppLog(Context context, ContentValues[] values){
		
		ArrayList<ContentValues> logs = new ArrayList<ContentValues>();
		for(ContentValues contentValues : values){
			Integer type = contentValues.getAsInteger(LauncherSettings.Favorites.ITEM_TYPE);
			if(type == null){
				continue;
			}
			
			if(type.intValue() != 0){
				continue;
			}
			String rawIntent = contentValues.getAsString(LauncherSettings.Favorites.INTENT);
			ComponentName cn = null;
			try{
				Intent intent = Intent.parseUri(rawIntent, 0);
				cn = intent.getComponent();
			}catch(Exception e){
			
			}
		
			if(cn == null) continue;
		
			long lastCalledTime = contentValues.getAsLong(IphoneUtils.FavoriteExtension.LAST_CALLED_TIME);
			long calledNum = contentValues.getAsLong(IphoneUtils.FavoriteExtension.CALLED_NUM);
		
			ContentValues log = new ContentValues();
			log.put(AppOperationLogSettings.Log.COMPONENT, cn.flattenToString());
			log.put(AppOperationLogSettings.Log.LAST_CALLED_TIME, lastCalledTime);
			log.put(AppOperationLogSettings.Log.CALLED_NUM, calledNum);
			logs.add(log);
		}
		
		if(logs.size() > 0){
			ContentResolver contentResolver = context.getContentResolver();
			contentResolver.bulkInsert(AppOperationLogSettings.Log.CONTENT_URI, (ContentValues[])logs.toArray(new ContentValues[logs.size()]));
		}
		
		return logs.size();
	}
	
	public static void deleteAppLog(Context context, Cursor result){
		if(result == null) return;
		ContentResolver resolver = context.getContentResolver();
		while(result.moveToNext()){
			Integer type = result.getInt(result.getColumnIndex(LauncherSettings.Favorites.ITEM_TYPE));
			if(type == null){
				continue;
			}
			
			if(type.intValue() != 0){
				continue;
			}
			String rawIntent = result.getString(result.getColumnIndex(LauncherSettings.BaseLauncherColumns.INTENT));
			ComponentName cn = null;
			
			try{
				Intent intent = Intent.parseUri(rawIntent, 0);
				cn = intent.getComponent();
			}catch(Exception e){
				
			}
			
			if(cn == null) continue;
			resolver.delete(AppOperationLogSettings.Log.CONTENT_URI, AppOperationLogSettings.Log.COMPONENT + "=\"" + cn.flattenToString() + "\"", null);
		}
	}
	
	public static void updateAppLog(Context context, Cursor result, ContentValues values){
		if(result == null || values == null) return;
		ContentResolver resolver = context.getContentResolver();
		while(result.moveToNext()){
			String rawIntent = result.getString(result.getColumnIndex(LauncherSettings.BaseLauncherColumns.INTENT));
			ComponentName cn = null;
			
			try{
				Intent intent = Intent.parseUri(rawIntent, 0);
				cn = intent.getComponent();
			}catch(Exception e){
				
			}
			
			if(cn == null) continue;
			
			ContentValues v = new ContentValues();
			if(values.containsKey(AppOperationLogSettings.Log.LAST_CALLED_TIME)){
				v.put(AppOperationLogSettings.Log.LAST_CALLED_TIME, values.getAsLong(AppOperationLogSettings.Log.LAST_CALLED_TIME));
			}
			if(values.containsKey(AppOperationLogSettings.Log.CALLED_NUM)){
				v.put(AppOperationLogSettings.Log.CALLED_NUM, values.getAsLong(AppOperationLogSettings.Log.CALLED_NUM));
			}
			
			resolver.update(AppOperationLogSettings.Log.CONTENT_URI, v, AppOperationLogSettings.Log.COMPONENT + "=\"" + cn.flattenToString() + "\"", null);

		}
	}
}
