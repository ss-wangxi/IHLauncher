package cc.snser.launcher.iphone.model;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Pair;
import cc.snser.launcher.Constant;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.Utils;
import cc.snser.launcher.apps.model.AppInfo;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.features.shortcut.CustomShortcutAction;
import cc.snser.launcher.model.DbManager;
import cc.snser.launcher.model.FavoriteGap;
import cc.snser.launcher.model.ItemInfoLoader;
import cc.snser.launcher.model.LauncherModel.Callbacks;
import cc.snser.launcher.style.SettingPreferences;
import cc.snser.launcher.ui.utils.Utilities;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.ui.view.FastBitmapDrawable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cc.snser.launcher.Constant.LOGD_ENABLED;
import static cc.snser.launcher.Constant.LOGE_ENABLED;
import static cc.snser.launcher.iphone.model.LauncherModelIphone.TAG;

public class SyncBinder {

	private static final boolean ENABLE_FIRSTSTARTUP_FOLDER_INIT_DELAY = true;
	private final Context mContext;

	private final LoaderTask loaderTask;

	private final Callbacks oldCallbacks;
	private final boolean isFirstStartup;
	private final ArrayList<HomeDesktopItemInfo> mLoadedShortcutInfos;
	private final Map<HomeDesktopItemInfo, ResolveInfo> mPendingResolveNewAppTitleIconList = new HashMap<HomeDesktopItemInfo, ResolveInfo>();

	private static final boolean DEBUG_LOADERS = LoaderTask.DEBUG_LOADERS;

	private static final int MIN_CONTENT_SIZE = 2;
	private static final int MIN_TIGGER_CLASSIFY= 24;
	private final DatabaseBinder mDatabaseBinder;
	private boolean mReloadDockbar = false;

	/**
	 * 同步shortcut和application，并更新为实际的最新图标和title.
	 * 对与桌面数据不同步的icon进行更新，其中包括shortcut和application
	 * 先执行删除操作，并重新排版桌面布局；然后执行添加和更新操作，来保证新增加的图标位置正确
	 */
	public SyncBinder(final Context context, LoaderTask loaderTask, final Callbacks oldCallbacks, final ArrayList<HomeDesktopItemInfo> loadedShortcutInfos, boolean firstStartup, DatabaseBinder databaseBinder) {
		mContext = context;
		this.loaderTask = loaderTask;
		this.oldCallbacks = oldCallbacks;
		this.mLoadedShortcutInfos = loadedShortcutInfos;
		this.mDatabaseBinder = databaseBinder;
		isFirstStartup = firstStartup;
	}

	public void sync(boolean syncDiff) {
		final long qiaTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

		ArrayList<HomeDesktopItemInfo> toRemoveShortcuts = new ArrayList<HomeDesktopItemInfo>();

		ArrayList<HomeDesktopItemInfo> toRemoveApps = new ArrayList<HomeDesktopItemInfo>();
		ArrayList<HomeDesktopItemInfo> toAddedApps = new ArrayList<HomeDesktopItemInfo>();
		ArrayList<HomeDesktopItemInfo> toUpdateApps = new ArrayList<HomeDesktopItemInfo>();

		ArrayList<ResolveInfo> updateResolveInfos = new ArrayList<ResolveInfo>(); //用于读取icon和title

		Map<ComponentName, Long> recentRunningComponentNames = null;
		Map<ComponentName, Pair<Long, Integer>> savedRunningInfoMap = null;

		if (isFirstStartup) {
			// load the statistics from the recent task
			try {
				long now = System.currentTimeMillis();
				recentRunningComponentNames = new HashMap<ComponentName, Long>();

				ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
				List<RecentTaskInfo> tasks = am.getRecentTasks(20, ActivityManager.RECENT_WITH_EXCLUDED);
				for (RecentTaskInfo task : tasks) {
					if (task.baseIntent != null && task.baseIntent.getComponent() != null) {
						recentRunningComponentNames.put(task.baseIntent.getComponent(), now);
					}
					if (task.origActivity != null) {
						recentRunningComponentNames.put(task.origActivity, now);
					}
					now--;
				}
			} catch (Throwable e) {
				if (LOGE_ENABLED) {
					XLog.e(TAG, "Failed to fetch the recent tasks.", e);
				}
			}
		}

		//计算需要同步的数据
		setupSyncData(mContext, oldCallbacks,mLoadedShortcutInfos, toRemoveShortcuts, toRemoveApps, toAddedApps, toUpdateApps, updateResolveInfos, recentRunningComponentNames, savedRunningInfoMap);

		if (loaderTask.stopped()) {
			return;
		}

		if (syncDiff) {
			// addList + updateList

			//sync待删除的item，并重新整理屏幕
			syncRemoveItemInfos(mContext, oldCallbacks, toRemoveShortcuts, toRemoveApps);

			if (loaderTask.stopped()) {
				return;
			}

			//正常的添加逻辑（仍然可以根据数据库来计算目标位置，或者到workspace上根据iteminfo来获取下一个位置）
			syncAddItems(mContext, oldCallbacks, toAddedApps);

		}

		if (loaderTask.stopped()) {
			return;
		}

		updateShortcutInfos(mContext, oldCallbacks, toUpdateApps, updateResolveInfos, recentRunningComponentNames, savedRunningInfoMap);

		if (DEBUG_LOADERS) {
			XLog.d(TAG, "sync apps from the db took " + (SystemClock.uptimeMillis() - qiaTime) + "ms");
		}
	}

	public void updatePendingResolveIconTitleList(Context context, Callbacks oldCallbacks) {
		ArrayList<HomeDesktopItemInfo> toUpdateApps = new ArrayList<HomeDesktopItemInfo>();

		ArrayList<ResolveInfo> updateResolveInfos = new ArrayList<ResolveInfo>(); //用于读取icon和title

		Iterator<Map.Entry<HomeDesktopItemInfo, ResolveInfo>> it = mPendingResolveNewAppTitleIconList.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<HomeDesktopItemInfo, ResolveInfo> entry = it.next();
			if (entry.getKey() != null && entry.getValue() != null) {
				toUpdateApps.add(entry.getKey());
				updateResolveInfos.add(entry.getValue());
			}
		}

		mPendingResolveNewAppTitleIconList.clear();

		updateShortcutInfos(context, oldCallbacks, toUpdateApps, updateResolveInfos, null, null);
	}

	/**
	 * TODO: 重构：多参数转类
	 * TODO: 重构：对loaderTask的卸耦
	 * @param context
	 * @param oldCallbacks
	 * @param shortcutInfos
	 * @param firstStartup
	 * @param removedShortcuts
	 * @param addedShortcuts
	 * @param removeApps
	 * @param addedApps
	 * @param updateApps
	 * @param replacedDesktopItemInfo 被替换掉的itemInfo。其在UI层已经没有对应的view展示，但持久层仍然有其数据。其在DatabaseBinder已经被加入到shortcutInfos列表中
	 *     计算删除的逻辑时可以不必考虑replacedDesktopItemInfo，可直接用shortcutInfos来处理；这里主要是要进行添加逻辑的特殊操作，将replacedItemInfo再添加到桌面上
	 * @param updateResolveInfos
	 */
	private void setupSyncData(final Context context, final Callbacks oldCallbacks, final ArrayList<HomeDesktopItemInfo> shortcutInfos,
			List<HomeDesktopItemInfo> removedShortcuts, List<HomeDesktopItemInfo> removeApps, List<HomeDesktopItemInfo> addedApps,
			List<HomeDesktopItemInfo> updateApps, List<ResolveInfo> updateResolveInfos, Map<ComponentName, Long> recentRunningComponentNames, Map<ComponentName, Pair<Long, Integer>> savedRunningInfoMap) {

		final PackageManager packageManager = context.getPackageManager();
		//应存在的所有app
		List<ResolveInfo> apps = queryApps(packageManager);

		if (apps == null) {
			return;
		}

		Map<ComponentName, HomeDesktopItemInfo> allAppsFromDbMap = new LinkedHashMap<ComponentName, HomeDesktopItemInfo>();
		Map<Intent, HomeDesktopItemInfo> allShortcutsFromDbMap = new LinkedHashMap<Intent, HomeDesktopItemInfo>();

		// TODO: 构建函数内数据结构 or 对loaderTask进行干活？
		// 如果是前者，不要写loaderTask，如果是后者，该逻辑放到loaderTask中
		for (HomeDesktopItemInfo itemFromDb : shortcutInfos) {
			if (itemFromDb.isShortcut()) {
				/*if (!Constant.LAUNCHER_CUSTOM_SHORTCUT_ACTION.equals(itemFromDb.intent.getAction())) {
                    //not a unique shortcut
                    continue;
                }*/
				if (allShortcutsFromDbMap.containsKey(itemFromDb.intent)) {
					//删除重复数据,并添加到AllShortcutsList的removed列表中，供之后的同步来更新界面
					loaderTask.getAllShortcutsList().remove(itemFromDb); // TODO: 不应该在这里做...
				} else {
					allShortcutsFromDbMap.put(itemFromDb.intent, itemFromDb);
				}
			} else {
				if (allAppsFromDbMap.containsKey(itemFromDb.intent.getComponent())) {
					//删除重复数据,并添加到AllAppsList的removed列表中，供之后的同步来更新界面
					loaderTask.getAllAppsList().remove(itemFromDb);
				} else {
					allAppsFromDbMap.put(itemFromDb.intent.getComponent(), itemFromDb);
				}
			}
		}

		//填充待删除的列表
		for (HomeDesktopItemInfo info : allShortcutsFromDbMap.values()) {
			//shortcut对应的方式不存在，而且不是内置shortcut的情况下，删除之
			if (!CustomShortcutAction.isPresetShortcut(info)) {
				List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(info.getIntent(), 0);
				if (resolveInfos != null && resolveInfos.size() > 0) {
					//shortcut 有对应的结果，不做删除
					continue;
				}
			} else {
				continue;
			}
			if (LOGD_ENABLED) {
				XLog.d(TAG, "delete shortcut:" + info);
			}
			removedShortcuts.add(info);
		}

		/****************************************下面计算apps的变更******************************/
		int n = apps.size();

		ArrayList<ResolveInfo> adds = new ArrayList<ResolveInfo>(n);

		for (ResolveInfo info : apps) {
			ComponentName componentName = new ComponentName(
					info.activityInfo.applicationInfo.packageName,
					info.activityInfo.name);
			HomeDesktopItemInfo shortcutInfo = allAppsFromDbMap.remove(componentName);
			//replacedItemInfo既要完成添加操作，又要完成更新操作
			//replcaedItemInfo已经存在于allAppsFromDbMap中
			if (shortcutInfo == null) {
				adds.add(info);
			}
			if (shortcutInfo != null) {
				updateApps.add(shortcutInfo);
				updateResolveInfos.add(info);
				//XXX: 提前检查system字段，以免加载分类时出错。TODO:代码位置待整理。另外storage字段可能也需要检查，在重构时考虑
				final boolean system = (info.activityInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
				if (system != shortcutInfo.system) {
					shortcutInfo.system = system;
					shortcutInfo.syncUpdated = true;
				}
			}
		}

		// 填充待新增的列表
		n = adds.size();
		final int batchSize = loaderTask.mBatchSize == 0 ? n : loaderTask.mBatchSize;
		int i = 0;
		ContentResolver cr = mContext.getContentResolver();
		while (i < n && !loaderTask.stopped()) {
			for (int j = 0; i < n && j < batchSize; i++) {
				final ResolveInfo info = adds.get(i);

				HomeDesktopItemInfo shortcutInfo = ItemInfoLoader.getInstance(context).getShortcutInfo(packageManager, info, context, !ENABLE_FIRSTSTARTUP_FOLDER_INIT_DELAY || !isFirstStartup);
				if (isFirstStartup && ENABLE_FIRSTSTARTUP_FOLDER_INIT_DELAY) {
					mPendingResolveNewAppTitleIconList.put(shortcutInfo, info);
				}
				if (DEBUG_LOADERS) {
					XLog.d(TAG, "get info from added shortcut " + shortcutInfo);
				}

				if (savedRunningInfoMap != null && !savedRunningInfoMap.isEmpty()) {
					if (savedRunningInfoMap.containsKey(shortcutInfo.getComponentName())) {
						Pair<Long, Integer> data = savedRunningInfoMap.get(shortcutInfo.getComponentName());
						shortcutInfo.lastCalledTime = data.first;
						shortcutInfo.calledNum = data.second;
					}
				} else if (recentRunningComponentNames != null && recentRunningComponentNames.containsKey(shortcutInfo.getComponentName())) {
					shortcutInfo.lastCalledTime = recentRunningComponentNames.get(shortcutInfo.getComponentName());
					shortcutInfo.calledNum = 1;
				}

				ComponentName cn = shortcutInfo.getComponentName();

				Cursor cursor = cr.query(AppOperationLogSettings.Log.CONTENT_URI, null, AppOperationLogSettings.Log.COMPONENT + "=\"" + cn.flattenToString() + "\"", null, null);

				if(cursor != null && cursor.moveToNext()){
					Long lastCalledTime = cursor.getLong(cursor.getColumnIndex(AppOperationLogSettings.Log.LAST_CALLED_TIME));
					Integer calledNum = cursor.getInt(cursor.getColumnIndex(AppOperationLogSettings.Log.CALLED_NUM));

					if(lastCalledTime != null){
						shortcutInfo.lastCalledTime = lastCalledTime.longValue();
					}
					if(calledNum != null){
						shortcutInfo.calledNum = calledNum.intValue();
					}


				}
				if(cursor != null){
					cursor.close();
				}
				addedApps.add(shortcutInfo); // add item to db
				j++;
			}
		}

		// 填充待删除的列表
		for (HomeDesktopItemInfo info : allAppsFromDbMap.values()) {
			if (loaderTask.stopped()) {
				return;
			}

			ResolveInfo existLauncherActivity = null;
			// Bug 7705 - GT-N7102:三星桌面上卸载了预装软件后，切到LD OS风格桌面后，会有一个灰色的图标显示着
			// 修复：系统应用没有关联的Activity也要从桌面删除，可能是被禁用了
			boolean needUpdate = true;
			if(info.getComponentName() != null && !"com.android.stk".equals(info.getComponentName().getPackageName())){
				existLauncherActivity = getLauncherExistActivity(packageManager, info.getComponentName());
				if(info.storage != Constant.APPLICATION_EXTERNAL){
					needUpdate = !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
				}else{
					needUpdate = false;
				}
				if (existLauncherActivity == null && !needUpdate) {
					if (DEBUG_LOADERS) {
						XLog.d(TAG, "sync remove " + info.defaultTitle + " storage:" + info.storage);
					}
					removeApps.add(info);
				}
			}

			if(needUpdate){
				updateApps.add(info);
				updateResolveInfos.add(existLauncherActivity);//如果是个launcher的activity，要确保能够更新到

			}
		}

		final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

		if (DEBUG_LOADERS) {
			XLog.d(TAG, "resolve category cost "
					+ (SystemClock.uptimeMillis() - t) + "ms");
		}
	}

	private ResolveInfo getLauncherExistActivity(PackageManager packageManager, ComponentName cn) {
		if (mContext.getPackageName().equals(cn.getPackageName())) {
			try {
				Intent intent = new Intent();
				intent.setComponent(cn);
				List<ResolveInfo> result = packageManager.queryIntentActivities(intent, 0);

				if (result != null && result.size() > 0) {
					return result.get(0);
				}
			} catch (Exception e) {
			}
		}
		return null;
	}

	private List<HomeDesktopItemInfo> syncRemoveItemInfos(final Context context, final Callbacks oldCallbacks,
			List<HomeDesktopItemInfo> removeShortcuts, List<HomeDesktopItemInfo> removeApps) {
		//删除待删数据
		final List<HomeDesktopItemInfo> allRemoved = new ArrayList<HomeDesktopItemInfo>();

		for (HomeDesktopItemInfo info : removeShortcuts) {
			if (loaderTask.stopped()) {
				return allRemoved;
			}
			if (DEBUG_LOADERS) {
				XLog.d(TAG, "sync remove " + info.defaultTitle);
			}
			loaderTask.getAllShortcutsList().remove(info);
		}

		// TODO: 待重构：各种member的直接交换。。。没有封装。。。
		allRemoved.addAll(loaderTask.getAllShortcutsList().removed);
		loaderTask.getAllShortcutsList().removed = new ArrayList<HomeDesktopItemInfo>();

		// remove the old apps in the db
		for (HomeDesktopItemInfo info : removeApps) {
			if (loaderTask.stopped()) {
				return allRemoved;
			}
			if (DEBUG_LOADERS) {
				XLog.d(TAG, "sync remove " + info.defaultTitle);
			}
			loaderTask.getAllAppsList().remove(info);
			if (info != null && info.intent != null && info.intent.getComponent() != null) {
				loaderTask.getLauncherModel().syncRemoveApp(info.intent.getComponent());
			}
		}

		allRemoved.addAll(loaderTask.getAllAppsList().removed);
		loaderTask.getAllAppsList().removed = new ArrayList<HomeDesktopItemInfo>();

		//bindRemove（调用launcher进而workspace，更新位置）
		loaderTask.postRunnable(oldCallbacks, new Runnable() {
			@Override
			public void run() {
				final Callbacks callbacks = loaderTask.tryGetCallbacks(oldCallbacks);
				if (callbacks != null) {
					final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
					callbacks.bindAppsRemoved(allRemoved, true);
					//                    callbacks.reorderWorkspaceLayout();
					if (DEBUG_LOADERS) {
						XLog.d(TAG, "sync unbound in "
								+ (SystemClock.uptimeMillis() - t) + "ms");
					}
				} else {
					XLog.i(TAG, "not binding apps: no Launcher activity");
				}
			}
		});
		return allRemoved;
	}

	private void syncAddItems(final Context context, final Callbacks oldCallbacks,
			List<HomeDesktopItemInfo> addedApps) {
		if (!loaderTask.getAllShortcutsList().added.isEmpty()) {
			final ArrayList<HomeDesktopItemInfo> added = loaderTask.getAllShortcutsList().added;
			loaderTask.getAllShortcutsList().added = new ArrayList<HomeDesktopItemInfo>();

			loaderTask.postRunnable(oldCallbacks, new Runnable() {
				@Override
				public void run() {
					final Callbacks callbacks = loaderTask.tryGetCallbacks(oldCallbacks);
					if (callbacks != null) {
						final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
						callbacks.bindAppsAdded(added, false, true);
						if (DEBUG_LOADERS) {
							XLog.d(TAG, "sync bound " + added.size() + " apps in "
									+ (SystemClock.uptimeMillis() - t) + "ms");
						}
					} else {
						XLog.i(TAG, "not binding apps: no Launcher activity");
					}
				}
			});
		}

		syncAddAppsStrategy3(context, oldCallbacks, addedApps);
	}

	/**
	 * 同步添加应用的策略1，在第一次启动时，按照自定义的忽略列表生成系统工具文件夹
	 * @param context
	 * @param oldCallbacks
	 * @param firstStartup
	 * @param addedApps
	 */
	private void syncAddAppsStrategy1(final Context context, final Callbacks oldCallbacks,
			List<HomeDesktopItemInfo> addedApps) {
		int n = addedApps.size();
		int i = 0;
		final int batchSize = loaderTask.mBatchSize == 0 ? n : loaderTask.mBatchSize;
		while (i < n && !loaderTask.stopped()) {
			for (int j = 0; i < n && j < batchSize; i++) {
				HomeDesktopItemInfo shortcutInfo = addedApps.get(i);
				loaderTask.getAllAppsList().add(shortcutInfo, true); // add item to db
				j++;
			}

			if (loaderTask.getAllAppsList().added.isEmpty()) {
				continue;
			}

			final ArrayList<HomeDesktopItemInfo> added = loaderTask.getAllAppsList().added;
			loaderTask.getAllAppsList().added = new ArrayList<HomeDesktopItemInfo>();

			loaderTask.postRunnable(oldCallbacks, new Runnable() {
				@Override
				public void run() {
					final Callbacks callbacks = loaderTask.tryGetCallbacks(oldCallbacks);
					if (callbacks != null) {
						final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
						callbacks.bindAppsAdded(added, false, true);
						if (DEBUG_LOADERS) {
							XLog.d(TAG, "sync bound " + added.size() + " apps in "
									+ (SystemClock.uptimeMillis() - t) + "ms");
						}
					} else {
						XLog.i(TAG, "not binding apps: no Launcher activity");
					}
				}
			});
		}
	}

	/**
	 * 所有应用自动分组
	 * @param context
	 * @param oldCallbacks
	 * @param firstStartup
	 * @param addedApps
	 */
	private void syncAddAppsStrategy3(final Context context, final Callbacks oldCallbacks,
			List<HomeDesktopItemInfo> addedApps) {
		//计算文件夹信息

		//添加应用到下一屏的“其他应用”文件夹中
		if (isFirstStartup) {
			
			Map<Integer, List<HomeDesktopItemInfo>> categoryMappings = new HashMap<Integer, List<HomeDesktopItemInfo>>();

            for (AppInfo info : addedApps) {
                final HomeDesktopItemInfo appInfo = (HomeDesktopItemInfo) info;

                boolean added = loaderTask.getAllAppsList().add(appInfo, false, false);
                if (!added) {
                    continue;
                }
                final int category = appInfo.category >= 0 ? appInfo.category : -1;
                // try to generate new folders
                List<HomeDesktopItemInfo> infos = categoryMappings.get(category);

                if (infos == null) {
                    infos = new ArrayList<HomeDesktopItemInfo>();
                    categoryMappings.put(category, infos);
                }

                infos.add(appInfo);
            }
            
            final List<HomeDesktopItemInfo> shortcutList = new ArrayList<HomeDesktopItemInfo>();
            
            if (LOGD_ENABLED) {
                XLog.d(TAG, "category folder begin.");
            }
            
            for (Map.Entry<Integer, List<HomeDesktopItemInfo>> entry : categoryMappings.entrySet()) {
                List<HomeDesktopItemInfo> infos = entry.getValue();
                
                if (infos.isEmpty()) {
                    continue;
                }
                
                
                for(HomeDesktopItemInfo info : infos)
                {
                	IphoneItemInfoUtils.cleanPosition(info);
                	shortcutList.add(info);
                	DbManager.addItemToDatabase(context, info);
                }
            }
                
			//填补空缺为shortcut
			for(HomeDesktopItemInfo shortcut : shortcutList)
			{
				//填补预排时空缺
				FavoriteGap.GapPosition gapPos = FavoriteGap.getInstance().getGapPosition();
				if(gapPos != null)
				{
					shortcut.container = gapPos.mContainer;
					shortcut.screen = gapPos.mScreen;
					shortcut.cellX = gapPos.mCellX;
					shortcut.cellY = gapPos.mCellY;
					DbManager.updateItemInDatabase(context, shortcut);
				}
				else
				{
					//不存在空缺
					break;
				}
			}

			if (LOGD_ENABLED) {
				XLog.d(TAG, "category folder end.");
			}

			loaderTask.postRunnable(oldCallbacks, new Runnable() {
				@Override
				public void run() {
					final Callbacks callbacks = loaderTask.tryGetCallbacks(oldCallbacks);
					if (callbacks != null) {
						if(!shortcutList.isEmpty())
							callbacks.bindAppsAdded(shortcutList, false, false);
					} else {
						XLog.i(TAG, "not binding apps: no Launcher activity");
					}
				}
			});

		} else {
			syncAddAppsStrategy1(context, oldCallbacks, addedApps);
		}
	}

	private static ArrayList<HomeItemInfo> loadAndSetRecommendedApps(Context context, Map<Integer, List<HomeDesktopItemInfo>> categoryMappings) {
		ArrayList<HomeItemInfo> ret = new ArrayList<HomeItemInfo>();

		int maxRows = SettingPreferences.getHomeLayout(context)[0];

		try {
			// add the social
			handleRecommendation(context, ret, categoryMappings, 6, 1, maxRows - 1, 2);
		} catch (Throwable e) {
			if (LOGE_ENABLED) {
				XLog.e(TAG, "Failed to load and set recommended apps.", e);
			}
		}

		return ret;
	}

	private static void handleRecommendation(Context context, ArrayList<HomeItemInfo> ret, Map<Integer, List<HomeDesktopItemInfo>> categoryMappings, int category, int screen, int row, int column) throws Exception {
		List<HomeDesktopItemInfo> infos = categoryMappings.get(category);
		if (infos == null) {
			return;
		}

		Iterator<HomeDesktopItemInfo> iter = infos.iterator();
		if (!iter.hasNext()) {
			return;
		}

		HomeDesktopItemInfo info = iter.next();

		if (handleRecommendation(context, info, screen, row, column)) {
			ret.add(info);
			iter.remove();
		}
	}

	private static boolean handleRecommendation(Context context, HomeDesktopItemInfo info, int screen, int row, int column) throws Exception {
		int maxRows = SettingPreferences.getHomeLayout(context)[0];
		int maxColumns = SettingPreferences.getHomeLayout(context)[1];

		if (screen < 0 || screen >= Launcher.MAX_SCREEN_NUMBER) {
			return false;
		}

		if (row < 0 || row >= maxRows) {
			return false;
		}

		if (column < 0 || column >= maxColumns) {
			return false;
		}

		if (DbManager.containsItemInDatabase(context, screen, column, row)) {
			return false;
		}

		info.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
		info.screen = screen;
		info.cellX = column;
		info.cellY = row;

		DbManager.addItemToDatabase(context, info);

		return true;
	}

	/**
	 * refresh workspace icon and title.
	 * @param context
	 * @param oldCallbacks
	 * @param updates
	 * @param infos
	 */
	private void updateShortcutInfos(final Context context,
			final Callbacks oldCallbacks, ArrayList<HomeDesktopItemInfo> updates,
			ArrayList<ResolveInfo> infos, Map<ComponentName, Long> recentRunningComponentNames, Map<ComponentName, Pair<Long, Integer>> savedRunningInfoMap) {

		//set priority lower
		final int lastPriority = android.os.Process.getThreadPriority(loaderTask.mThreadId);
		loaderTask.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

		if (LOGD_ENABLED) {
			XLog.d(TAG, "begin updateShortcutInfos");
		}

		loaderTask.yieldLoader();

		int n = updates.size();

		if (n == 0) {
			return;
		}

		final int batchSize = loaderTask.mBatchSize == 0 ? n : loaderTask.mBatchSize;

		final ArrayList<HomeDesktopItemInfo> updated = new ArrayList<HomeDesktopItemInfo>(n);
		ArrayList<HomeDesktopItemInfo> tempUpdated = new ArrayList<HomeDesktopItemInfo>(batchSize);

		for (int i = 0; i < n; i++) {
			if (loaderTask.stopped()) {
				return;
			}
			final HomeDesktopItemInfo update = updates.get(i);
			final ResolveInfo info = infos.get(i);

			if (info == null) {
				update.setIcon(new FastBitmapDrawable(loaderTask.mIconCache.getDefaultIcon())); // TODO: 优化
			} else {
				//不重新载入application的预设icon
				if(update.itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION || 
						update.iconType != LauncherSettings.Favorites.ICON_TYPE_RESOURCE ||
						update.iconResource == null ||
						!update.isSystem())
					update.setIcon(loaderTask.mIconCache.getIcon(update.getIntent(), info)); // TODO: 优化
					String title = loaderTask.mIconCache.getTitle(update.getIntent(), info);
					if (!Utils.equals(update.getTitle(), title)) {
						update.setTitle(title);
						update.syncUpdated = true;
					}
					int storage = Utils.getApplicationStorage(info);
					if (update.storage != storage) {
						update.storage = storage;
						update.syncUpdated = true;
					}
					long lastUpdateTime = Utils.getLastUpdateTime(info);
					if (update.lastUpdateTime != lastUpdateTime) {
						update.lastUpdateTime = lastUpdateTime;
						update.syncUpdated = true;
					}
					boolean system = (info.activityInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
					if (update.system != system) {
						update.system = system;
						update.syncUpdated = true;
					}
					if (savedRunningInfoMap != null && !savedRunningInfoMap.isEmpty()) {
						if (savedRunningInfoMap.containsKey(update.getComponentName())) {
							Pair<Long, Integer> data = savedRunningInfoMap.get(update.getComponentName());
							update.lastCalledTime = data.first;
							update.calledNum = data.second;
							update.syncUpdated = true;
						}
					} else if (recentRunningComponentNames != null && recentRunningComponentNames.containsKey(update.getComponentName())) {
						update.lastCalledTime = recentRunningComponentNames.get(update.getComponentName());
						update.calledNum = 1;
						update.syncUpdated = true;
					}
			}

			tempUpdated.add(update);

			loaderTask.yieldLoader();

			if ((tempUpdated.size()) % batchSize == 0) {
				bindAppsUpdated(oldCallbacks, tempUpdated);
				updated.addAll(tempUpdated);
				tempUpdated = new ArrayList<HomeDesktopItemInfo>(batchSize);
				XLog.d(TAG, "batch update shortcut");
			}
		}

		if (!tempUpdated.isEmpty()) {
			bindAppsUpdated(oldCallbacks, tempUpdated);
			updated.addAll(tempUpdated);
		}

		if (LOGD_ENABLED) {
			XLog.d(TAG, "syncHomeDesktopItemInfoInDatabase");
		}

		if (!updated.isEmpty() && !loaderTask.stopped()) {
			for (HomeDesktopItemInfo info : updated) {
				if (info.syncUpdated) {
					DbManager.syncHomeDesktopItemInfoInDatabase(context, info);
				}
			}
		}

		//restore priority before
		loaderTask.setThreadPriority(lastPriority);

		if (LOGD_ENABLED) {
			XLog.d(TAG, "end updateShortcutInfos");
		}
	}

	// TODO: 实现成基础函数
	protected List<ResolveInfo> queryApps(PackageManager packageManager) {
		// no items in the db, init it
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final long qiaTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
		List<ResolveInfo> apps = null;

		try {
			apps = packageManager.queryIntentActivities(mainIntent, 0);
		} catch (Exception e) {
			XLog.e(TAG, "Failed to query the intent activities.", e);
		}

		if (DEBUG_LOADERS) {
			XLog.d(TAG, "queryIntentActivities took " + (SystemClock.uptimeMillis() - qiaTime) + "ms");
		}

		if (apps == null) {
			return null;
		}

		for (Iterator<ResolveInfo> iter = apps.iterator(); iter.hasNext();) {
			ResolveInfo info = iter.next();
			// TODO: 此函数是基础函数，不应该有业务逻辑
			ComponentName cn = new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
			if (Utils.shouldIgnoreApp(info.activityInfo.applicationInfo.packageName)) {
				iter.remove();
				if (LOGD_ENABLED) {
					XLog.d(TAG, "found hidden apps in queryApps:" + cn);
				}
			}
			// INFO: 此处不需要处理隐藏应用，隐藏应用在更高的层次完成(目前暂时在AllAppsList.add处理)
		}
		int n = apps.size();
		if (DEBUG_LOADERS) {
			XLog.d(TAG, "queryIntentActivities got " + n + " apps");
		}
		if (n == 0) {
			return null;
		}

		return apps;
	}

	private void bindAppsUpdated(final Callbacks oldCallbacks, final ArrayList<HomeDesktopItemInfo> updated) {
		loaderTask.postRunnable(oldCallbacks, new Runnable() {
			@Override
			public void run() {
				final Callbacks callbacks = loaderTask.tryGetCallbacks(oldCallbacks);
				if (callbacks != null) {
					final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

					callbacks.bindAppsUpdated(updated, null);

					if (DEBUG_LOADERS) {
						XLog.d(TAG, "sync update " + updated.size() + " apps in "
								+ (SystemClock.uptimeMillis() - t) + "ms");
					}
				} else {
					XLog.i(TAG, "not binding apps: no Launcher activity");
				}
			}
		});
	}
}
