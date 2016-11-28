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

package cc.snser.launcher;

import static cc.snser.launcher.Constant.LOGD_ENABLED;
import static cc.snser.launcher.Constant.LOGE_ENABLED;
import static cc.snser.launcher.Constant.LOGI_ENABLED;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.content.LauncherMetadata;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.TextKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import cc.snser.launcher.HomeWatcher.OnHomePressedListener;
import cc.snser.launcher.apps.ActionUtils;
import cc.snser.launcher.apps.components.IconPressAnimation;
import cc.snser.launcher.apps.components.IconTip;
import cc.snser.launcher.apps.components.workspace.Shortcut;
import cc.snser.launcher.apps.model.AppInfo;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.apps.model.workspace.LauncherAppWidgetInfo;
import cc.snser.launcher.apps.model.workspace.LauncherWidgetViewInfo;
import cc.snser.launcher.component.themes.iconbg.model.local.IconBg;
import cc.snser.launcher.iphone.model.IphoneUtils;
import cc.snser.launcher.iphone.model.LauncherModelIphone;
import cc.snser.launcher.model.DbManager;
import cc.snser.launcher.model.LauncherModel;
import cc.snser.launcher.screens.DeleteZone;
import cc.snser.launcher.screens.NotifyZone;
import cc.snser.launcher.screens.OverScrollView;
import cc.snser.launcher.screens.Workspace;
import cc.snser.launcher.screens.WorkspaceCellLayoutMeasure;
import cc.snser.launcher.style.SettingPreferences;
import cc.snser.launcher.support.report.StatManager;
import cc.snser.launcher.support.report.UnexpectedExceptionHandler;
import cc.snser.launcher.ui.components.ScreenIndicator;
import cc.snser.launcher.ui.dragdrop.DragController;
import cc.snser.launcher.ui.dragdrop.DragControllerHolder;
import cc.snser.launcher.ui.dragdrop.DragLayer;
import cc.snser.launcher.ui.dragdrop.DropTarget;
import cc.snser.launcher.ui.utils.PrefConstants;
import cc.snser.launcher.ui.utils.PrefUtils;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;
import cc.snser.launcher.ui.view.StatusDialog;
import cc.snser.launcher.util.LifecycleSubject;
import cc.snser.launcher.util.PackageUtils;
import cc.snser.launcher.util.ReflectionUtils;
import cc.snser.launcher.util.ResourceUtils;
import cc.snser.launcher.util.StatusBarTransparentUtils;
import cc.snser.launcher.widget.BuiltinWidgetMgr;
import cc.snser.launcher.widget.IconWidgetCache;
import cc.snser.launcher.widget.LauncherAppWidgetHost;
import cc.snser.launcher.widget.LauncherAppWidgetHostView;
import cc.snser.launcher.widget.UserServiceProtolHelper;
import cc.snser.launcher.widget.Widget;
import cc.snser.launcher.widget.WidgetContext;
import cc.snser.launcher.widget.WidgetView;

import com.btime.launcher.CarOSLauncherBase;
import com.btime.launcher.Constants;
import com.btime.launcher.adapter.ChannelLayoutAdapter;
import com.btime.launcher.statusbar.VirtualStatusBar;
import com.btime.launcher.statusbar.VirtualStatusBarBase;
import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.activity.BaseActivity;
import com.shouxinzm.launcher.dialog.AlertDialog;
import com.shouxinzm.launcher.dialog.ProgressDialog;
import com.shouxinzm.launcher.dialog.ProgressDialog.PROGRESS_STYLE;
import com.shouxinzm.launcher.support.v4.util.AppWidgetUtils;
import com.shouxinzm.launcher.support.v4.util.ViewUtils;
import com.shouxinzm.launcher.util.DeviceUtils;
import com.shouxinzm.launcher.util.DialogUtils;
import com.shouxinzm.launcher.util.NotificationUtils;
import com.shouxinzm.launcher.util.ScreenDimensUtils;
import com.shouxinzm.launcher.util.SdCardUtils;
import com.shouxinzm.launcher.util.ToastUtils;
import com.shouxinzm.launcher.util.WindowManagerUtils;

/**
 * Default launcher application.
 */
public final class Launcher extends BaseActivity implements
		LauncherModel.Callbacks, DragControllerHolder, AbstractLauncher, IDialogSingletonManager {

	static final String TAG = "Launcher.Launcher";
	static final boolean DEBUG_USER_INTERFACE = false;

	static final int REQUEST_CREATE_SYS_SHORTCUT = 1;
	static final int REQUEST_CREATE_HOME_SHORTCUT = 2;
	static final int REQUEST_CREATE_APPWIDGET = 7;
	static final int REQUEST_CREATE_APPWIDGET_FROM_WIDGETVIEW = 1000;
	public static final int REQUEST_CODE_FOR_REGISTRATION_APPWIDGET = 8;
	public static final int REQUEST_PICK_APPWIDGET = 9;

	static final int REQUEST_GETTING_START = 17;

	private static final int VIBRATE_DURATION = 35;
	public static final int REQUEST_BATCH_ADD_APP_TO_FOLDER = 19;
	public static final int REQUEST_EDIT_HIDDEN_APPLICATIONS = 20;

	public static final int REQUEST_WORKSPACE_SETTINGS = 24;
	public static final int REQUEST_UNINSTALL_PACKAGE = 25;

	public static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";

	public static final int MAX_SCREEN_NUMBER = 9;
	public static final int MIN_SCREEN_NUMBER = 1;
	private Vibrator mVibrator;

	private LauncherReceiver mReceiver;
	private WidgetContext mWidgetContext;
	private ContentObserver mWidgetObserver;

	private Timer mTimer;
	private LayoutInflater mInflater;

	private DragController mDragController;
	private Workspace mWorkspace;
	
	private VirtualStatusBar mVirtualStatusBar;

	private OverScrollView mOverScrollView;

	// TODO: 重构：内存优化：lazy initialize
	private DeleteZone mDeleteZone;
	private NotifyZone mNotifyZone;

	private DragLayer dragLayer;

	private AppWidgetManager mAppWidgetManager;
	private LauncherAppWidgetHost mAppWidgetHost;
	private final CellLayout.CellInfo mPendingAddInfo = new CellLayout.CellInfo();
	private final int[] mCellCoordinates = new int[2];
	private Long mLastScreenOffTime = null;

	private SpannableStringBuilder mDefaultKeySsb = null;

	boolean mWorkspaceLoading = true;

	private boolean mPaused = true;
	private long mPausedTime = -1;
	private boolean mWaitingForResult;
	private boolean mOnResumeNeedsLoad;
	private boolean mIgnoreOnResumeNeedsLoad;

	private LauncherModel mModel;
	private IconCache mIconCache;

	private final ArrayList<HomeItemInfo> mDesktopItems = new ArrayList<HomeItemInfo>();
	private final ArrayList<WidgetView> mWidgetViews = new ArrayList<WidgetView>();
	private final Map<ComponentName, NewComponentState> mNewComponents = Collections
			.synchronizedMap(new LinkedHashMap<ComponentName, NewComponentState>());

	private ScreenIndicator mIndicatorHome;
	private HomeWatcher mHomeWatcher;

	private static final int TIME_INTERVAL = 1000 * 60;
	private static final int MSG_HOME_FIRST_LOAD = 2;
	private static final int MSG_HOME_INLOADING = 4;
	// private static final int MSG_ADD_WIDGET_VIEW = 8;
	private static final int MSG_HOME_UPGRADE_LOAD = 9;
	private static final int MSG_RELOAD_FOLDER_INDICATOR = 20;

	private final UnexpectedExceptionHandler mCrashHandler = new UnexpectedExceptionHandler();

	private boolean mFirstStart = false;
	private boolean mUpgraded = false;
	private boolean mActivityStarted;
	private boolean mFlushAllAppsOnCreate;

	private ProgressDialog mCurrentProgressDialog = null;
	public AlertDialog mClassifyDialog = null;
	public Boolean mIsClarify = false;
	public Object mLock = new Object();

	private final LifecycleSubject mLifecycleSubject = new LifecycleSubject();
	private static int sIntanceCount = 0;
	boolean isNeedSetDefaultLauncher = false;
	private int mAppWidgetId = 0;
	private MakeNewScreen mMakeNewScreen = null;

	private boolean mScrollToHomeScreen = false;

	private static List<Launcher> sInstances = new ArrayList<Launcher>();
	@SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LauncherModel.MSG_LOADER_TASK_FINISHED:
				if (mCurrentProgressDialog != null) {
					DialogUtils.dismissProgressDialog(mCurrentProgressDialog,
							Launcher.this);
					mCurrentProgressDialog = null;
				}
				break;
			case MSG_HOME_FIRST_LOAD:
				// 初次加载，显示loading
				if (mCurrentProgressDialog == null) {
					/*mCurrentProgressDialog = DialogUtils.showProgressDialog(
							Launcher.this,
							getString(R.string.launcher_reloading_message),
							true, new OnCancelListener() {
								public void onCancel(DialogInterface dialog) {

								}
							}, PROGRESS_STYLE.PROGRESSDIALOG_STYLE2);
					
					mCurrentProgressDialog.setCanceledOnTouchOutside(false);*/
				}

				mModel.startLoader(Launcher.this, true, mFlushAllAppsOnCreate);
				mHandler.sendEmptyMessageDelayed(MSG_HOME_INLOADING,
						DateUtils.SECOND_IN_MILLIS);
				break;

			case MSG_HOME_UPGRADE_LOAD:
				mModel.startLoader(Launcher.this, true, mFlushAllAppsOnCreate);
				mHandler.sendEmptyMessageDelayed(MSG_HOME_INLOADING,
						DateUtils.SECOND_IN_MILLIS);
				break;

			case MSG_HOME_INLOADING:
				if (mWorkspaceLoading) {
					mHandler.sendEmptyMessageDelayed(MSG_HOME_INLOADING, DateUtils.SECOND_IN_MILLIS);
				} else {
					if (mCurrentProgressDialog != null) {
						DialogUtils.dismissProgressDialog(mCurrentProgressDialog, Launcher.this);
						mCurrentProgressDialog = null;
					}
					onLoadingFinished();
					mWorkspace.enableLongClick();
				}
				break;
			case LauncherModel.MSG_HOME_CLASSIGY:
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {

						mIsClarify = (arg1 == DialogInterface.BUTTON_POSITIVE);
						synchronized (mLock) {
							mLock.notify();
						}
						arg0.dismiss();

						mCurrentProgressDialog = DialogUtils
								.showProgressDialog(
										Launcher.this,
										getString(R.string.launcher_reloading_message),
										true, new OnCancelListener() {
											public void onCancel(
													DialogInterface dialog) {

											}
										}, PROGRESS_STYLE.PROGRESSDIALOG_STYLE2);
						mCurrentProgressDialog.setCanceledOnTouchOutside(false);
					}
				};

				new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface arg0) {
						synchronized (mLock) {
							mLock.notify();
						}
						arg0.dismiss();
						mCurrentProgressDialog = DialogUtils
								.showProgressDialog(
										Launcher.this,
										getString(R.string.launcher_reloading_message),
										true, new OnCancelListener() {
											public void onCancel(
													DialogInterface dialog) {

											}
										}, PROGRESS_STYLE.PROGRESSDIALOG_STYLE2);
						
						mCurrentProgressDialog.setCanceledOnTouchOutside(false);
					}
				};
				if (mCurrentProgressDialog != null) {
					DialogUtils.dismissProgressDialog(mCurrentProgressDialog,
							Launcher.this);
					mCurrentProgressDialog = null;
				}
				/*
				 * mClassifyDialog = DialogUtils.showDialog( Launcher.this,
				 * getString(R.string.clarify_title),
				 * getString(R.string.clarify_msg), getString(R.string.ok),
				 * listener, getString(R.string.cancel), listener, null,null,
				 * cancelListener);
				 */

				break;
			case MSG_RELOAD_FOLDER_INDICATOR:
				// if(msg.obj instanceof ArrayList<?>)
				// {
				// mIndicatorHome.update((ArrayList<Integer>)msg.obj,
				// ScreenIndicator.FOLDER_INDICATOR);
				// }
				break;

			default:
				break;
			}
		}
	};
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    XLog.d("Snser", "LauncherState onConfigurationChanged mcc=" + newConfig.mcc + " mnc=" + newConfig.mnc + " this=" + this.hashCode() + " pid=" + android.os.Process.myPid());
	}

	private void onLoadingFinished() {
		if (mScrollToHomeScreen) {
			mScrollToHomeScreen = false;
			mWorkspace.moveToDefaultScreen(false);
		}
        mHandler.post(new Runnable() { // 耗时工作放在下次消息循环中
            @Override
            public void run() {
                int count = mWidgetViews.size();
                for (int i = 0; i < count; i++) {
                    mWidgetViews.get(i).onLauncherLoadingFinished();
                }
            }
        });
	}
	
	public enum LauncherState {
	    ONCREATE, ONSTART, ONRESUME, ONPAUSE, ONSTOP, ONDESTORY
	}
	
	private LauncherState mLauncherState = LauncherState.ONCREATE;
	
	public LauncherState getLauncherState() {
	    return mLauncherState;
	}
	
    public void setLauncherState(LauncherState state) {
        mLauncherState = state;
    }

	private static Launcher sInstance;

	public static Launcher getInstance() {
		return sInstance;
	}

	public Launcher() {
		if (LOGD_ENABLED) {
			XLog.d(TAG, "Launcher has been initialized.");
		}
	}

	private void addInstance(){
		sInstances.add(this);
	}
	
	private void removeInstance(){
		sInstances.remove(this);
	}
	
	private void clearOtherInstances(){
		for(Launcher instance : sInstances){
			if(instance != null && instance != this){
				if(!instance.isFinishing())
					instance.finish();
			}
		}
		sInstances.clear();
		addInstance();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    XLog.e("LSTAT", "Launcher.Launcher.onCreate nanoTime=" + String.format(Locale.getDefault(), "%.3fs", System.nanoTime() / 1000000000.0f));
	    
	    final Configuration cfg = getResources().getConfiguration();
	    XLog.d("Snser", "LauncherState onCreate mcc=" + cfg.mcc + " mnc=" + cfg.mnc + " this=" + this.hashCode() + " pid=" + android.os.Process.myPid());
	    setLauncherState(LauncherState.ONCREATE);
	    
	    if (App.TIME_LAUNCHER_ONCREATE == 0) {
	        App.TIME_LAUNCHER_ONCREATE = System.currentTimeMillis();
	    }
	    getResources().getDimensionPixelSize(R.dimen.widget2x3_margin_left);
	    getResources().getDimensionPixelSize(R.dimen.widget2x3_margin_right);
	    getResources().getDimensionPixelSize(R.dimen.widget2x3_margin_top);
	    getResources().getDimensionPixelSize(R.dimen.widget2x3_margin_bottom);
		
		mFirstStart = PrefUtils.getBooleanPref(this, PrefConstants.KEY_FIRST_START_FLAG, true);
		
		//if setup os by itself ,this flag will be set to true.
		boolean bSuccess = PrefUtils.getBooleanPref(this, PrefConstants.KEY_UPDATE_SUCCESS, false);
		if (bSuccess) {
			PrefUtils.setBooleanPref(this, PrefConstants.KEY_UPDATE_SUCCESS, false);
		}
		
		final String strDefVersion = "0.0.0";
		String strVersion          = PrefUtils.getStringPref(App.getApp(), PrefConstants.KEY_UPDATE_SUCCESS_CUSTOMIZATIONASSIST,strDefVersion);
		String strPackageVersion   = String.valueOf(PackageUtils.getPackageVersionName(this, getPackageName()));
		
		if (strPackageVersion.equals(strVersion)) {
			PrefUtils.setStringPref(App.getApp(), PrefConstants.KEY_UPDATE_SUCCESS_CUSTOMIZATIONASSIST,strDefVersion);
		}

    	mScrollToHomeScreen = true;
    	WorkspaceIconUtils.resetWorkspaceIconTextSize();
		clearOtherInstances();
		
		sIntanceCount++;
		sInstance = this;

		int flag = getIntent().getIntExtra("flag", 0);

		if (flag == Constant.FLAG_STOP_LAUNCHER
				|| flag == Constant.FLAG_RESTART_LAUNCHER) {
			getIntent().removeExtra("flag");
		}

		if (LauncherSettings.isEnableStatusBarAutoTransparent()) { // 4.4透明通知栏和底部导航
			boolean successful = false;
			if (DeviceUtils.isLollipop() && DeviceUtils.isStandardRom()) {
				try {
					Method method = ReflectionUtils.getMethod(getWindow()
							.getClass(), "setStatusBarColor",
							new Class[] { int.class });
					if (method != null) {
						// SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						// ViewUtils.setSystemUiVisibility(getWindow().getDecorView(),
						// 0x00000400);
						method.invoke(getWindow(), 0x00000000);
						successful = true;
					}
				} catch (Throwable e) {
					// ignore
				}
			}

			if (!successful) {
				getWindow()
						.addFlags(WindowManagerUtils.FLAG_TRANSLUCENT_STATUS);
			}
		}

		if (LauncherSettings.isEnableNavigationBarAutoTransparent()) { // 4.4透明通知栏和底部导航
			RuntimeConfig.sGlobalBottomPadding = ResourceUtils.getNavigationBarHeight(this);
			//横屏状态下NavigationBar为0也要处理通知栏 add by snsermail@gmail.com
			if (RuntimeConfig.sGlobalBottomPadding > 0 || App.getApp().isScreenLandscape()) {
				boolean successful = false;
				if (DeviceUtils.isLollipop() && DeviceUtils.isStandardRom()) {
					try {
						Method method = ReflectionUtils.getMethod(getWindow().getClass(), "setNavigationBarColor", new Class[] { int.class });
						if (method != null) {
							getWindow().addFlags(WindowManagerUtils.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
							// SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							ViewUtils.setSystemUiVisibility(getWindow().getDecorView(), 0x00000200);
							method.invoke(getWindow(), 0x00000000);
							successful = true;
						}
					} catch (Throwable e) {
						// ignore
					}
				}

				if (!successful) {
					getWindow().addFlags(WindowManagerUtils.FLAG_TRANSLUCENT_NAVIGATION);
				}

				RuntimeConfig.sGlobalBottomPadding = 0;
			}
		}

		if (LauncherSettings.isEnableStatusBarAutoTransparentV2()) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		}

		mWidgetContext = new WidgetContext(this);

		super.onCreate(savedInstanceState);

		LauncherSettings.clearHomeLayout();

		mInflater = getLayoutInflater();

		// 修复widget在刷rom后无法加载出来的问题
		int thisVersionCode = PackageUtils.getPackageVersionCode(this,
				Constant.PACKAGE_NAME);
		int lastVersionCode = PrefUtils.getIntPref(this,
				PrefConstants.KEY_LAST_VERSON_CODE, 0);
		if (thisVersionCode != lastVersionCode) {
			mUpgraded = true;
			PrefUtils.setIntPref(this, PrefConstants.KEY_LAST_VERSON_CODE,
					thisVersionCode);

			if (LOGI_ENABLED) {
				XLog.i(TAG, "launcher upgraded from " + lastVersionCode
						+ " to " + thisVersionCode);
			}

			mFlushAllAppsOnCreate = true;
			IconFsCache.getInstance(this).expireCache();
		}

		if (LOGD_ENABLED) {
			XLog.d(TAG, "mFirstStart:" + mFirstStart);
		}

		if (mFirstStart) {
			SettingPreferences.setIsLoopHomeScreen(false);
			SettingPreferences
					.setHomeScreen(Workspace.NEW_DEFAULT_DEFAULT_SCREEN);
			SettingPreferences
					.setScreenNumber(Workspace.NEW_DEFAULT_SCREEN_NUMBER);
		}

		App app = App.getApp();
		mModel = app.setLauncher(this);
		mIconCache = IconCache.getInstance(this);
		mDragController = new DragController(this);

		if (mFirstStart) {
			PrefUtils.setBooleanPref(this, PrefConstants.KEY_FIRST_START_FLAG, false);
		}

		Intent intent = getIntent();
		if (intent != null) {
			if (flag == Constant.FLAG_RELOAD_LAUNCHER_FOR_THEME) {
				resetCurrentTheme();
				mFlushAllAppsOnCreate = true;
				IconFsCache.getInstance(this).expireCache();
			}
		}

		//三星的手机，在这里可能crash，先简单的做一层保护
		try {
			mAppWidgetManager = AppWidgetManager.getInstance(this);
			mAppWidgetHost = new LauncherAppWidgetHost(this,
					mModel.getAppWidgetHostId());
			mAppWidgetHost.startListening();
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		if (mTimer == null && !mNewComponents.isEmpty()) {
			mTimer = new Timer();
			mTimer.schedule(new NewInstalledAppTask(), 0, TIME_INTERVAL);
		}

		// 语言环境检查
		LocaleCfgMgr.getInstance().setLauncher(this);
		LocaleCfgMgr.getInstance().checkForLocaleChange();

		setupViews(getIntent());
		
		registerContentObservers();

		if (mFirstStart || mUpgraded) {
			if (mFirstStart) {
				mHandler.sendEmptyMessageDelayed(MSG_HOME_FIRST_LOAD,
						DateUtils.SECOND_IN_MILLIS);
			} else {
				mHandler.sendEmptyMessageDelayed(MSG_HOME_UPGRADE_LOAD,
						DateUtils.SECOND_IN_MILLIS);
			}
		} else {
			mModel.startLoader(this, true, mFlushAllAppsOnCreate);

			mWorkspace.disableLongClick();
			mHandler.sendEmptyMessageDelayed(MSG_HOME_INLOADING,
					DateUtils.SECOND_IN_MILLIS);
		}

		// For handling default keys
		mDefaultKeySsb = new SpannableStringBuilder();
		Selection.setSelection(mDefaultKeySsb, 0);

		mReceiver = new LauncherReceiver(this);
		registerReceiver(mReceiver, mReceiver.getLauncherIntentFilter());

		mCrashHandler.init(this);


		CellLayout.refreshSmartTopPadding();

		if (mFirstStart) {
			new Thread() {
				@Override
				public void run() {
					try {
						android.os.Process
								.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
					} catch (Exception e) {
						// ignore
					}

				}
			}.start();
		}


		mHandler.post(new Runnable() { // 耗时工作放在下次消息循环中
			@Override
			public void run() {
				mModel.register();
			}
		});

		ResourceUtils.updateStatesBarCurrentHeight(this,
				ScreenDimensUtils.isFullScreen(this));
		if (!StatusBarTransparentUtils.autoSetSystemUiTransparent(getWindow()
				.getDecorView())) {
			if (!LauncherSettings.isEnableStatusBarAutoTransparent()) {
				new StatusDialog(Launcher.this, getWindow().getDecorView());
			}
		}

		WindowManagerUtils.addLegacyOverflowButton(getWindow());

		XLog.e("adapter", "end onCreate " + this);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			RuntimeConfig.sLauncherInTouching = true;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			RuntimeConfig.sLauncherInTouching = false;
			break;
		default:
			break;
		}

		try {
			boolean handled = super.dispatchTouchEvent(ev);
			if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
				if (mDragController.isExceptionDrag()) {
					mDragController.onExceptionDrag();
				}
			}
			return handled;
		} catch (Throwable e) {
			// ignore
			StatManager.handleException(this, e);
			return false;
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		this.mLifecycleSubject.notifyOnActivityResult(requestCode, resultCode,data);

		mWaitingForResult = false;

		if (LOGD_ENABLED) {
			XLog.d(TAG, "onActivityResult for requestCode: " + requestCode
					+ " and resultCode: " + resultCode + " and data: " + data);
		}
		
		if (requestCode >= cc.snser.launcher.widget.Constant.REQUEST_CODE_WIDGET_BEGIN
		    && requestCode <= cc.snser.launcher.widget.Constant.REQUEST_CODE_WIDGET_END) {
		      mHandler.post(new Runnable() { // 耗时工作放在下次消息循环中
		            @Override
		            public void run() {
		                int count = mWidgetViews.size();
		                for (int i = 0; i < count; i++) {
		                    mWidgetViews.get(i).onActivityResult(requestCode, resultCode, data);
		                }
		            }
		        });
		} else if (requestCode == REQUEST_GETTING_START) {
			if (!mFirstStart) {
				if (mHandler.hasMessages(MSG_HOME_INLOADING)) {
					mCurrentProgressDialog = DialogUtils.showProgressDialog(
							Launcher.this,
							getString(R.string.launcher_reloading_message),
							true, new OnCancelListener() {
								public void onCancel(DialogInterface dialog) {

								}
							}, PROGRESS_STYLE.PROGRESSDIALOG_STYLE2);
					
					mCurrentProgressDialog.setCanceledOnTouchOutside(false);
				}
			}
			return;
		} else if (requestCode == REQUEST_CODE_FOR_REGISTRATION_APPWIDGET) {
			if (resultCode == Activity.RESULT_CANCELED) {
				// API规定绑定不成功，就要先删除掉该appWidgetId，然后再绑定
				Launcher.this.mAppWidgetHost.deleteAppWidgetId(mAppWidgetId);
			} else {
				// 启动绑定intent
				Intent intent = new Intent();
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
						mAppWidgetId);
				addAppWidget(intent);
			}
			return;
		}

		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQUEST_CREATE_SYS_SHORTCUT:
				completeAddSysShortcut(data, mPendingAddInfo);
				break;
			case REQUEST_PICK_APPWIDGET:
				addAppWidget(data);
				break;
			case REQUEST_CREATE_APPWIDGET:
				completeAddAppWidget(data, mPendingAddInfo);
				break;
			case REQUEST_CREATE_APPWIDGET_FROM_WIDGETVIEW:
				completeAddAppWidget(data);
				break;
			}
		} else if ((requestCode == REQUEST_PICK_APPWIDGET || requestCode == REQUEST_CREATE_APPWIDGET)
				&& resultCode == RESULT_CANCELED && data != null) {
			// Clean up the appWidgetId if we canceled
			int appWidgetId = data.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			if (appWidgetId != -1) {
				mAppWidgetHost.deleteAppWidgetId(appWidgetId);
			}
		}
	}

	@Override
	public void onStart() {
	    if (App.TIME_LAUNCHER_ONSTART == 0) {
	        App.TIME_LAUNCHER_ONSTART = System.currentTimeMillis();
	    }
		super.onStart();
		XLog.d("Snser", "LauncherState onStart this=" + this.hashCode() + " pid=" + android.os.Process.myPid());
		setLauncherState(LauncherState.ONSTART);

		// DefaultLauncherHelper.clearOtherLauncher(getApplicationContext());

		if (LOGD_ENABLED) {
			XLog.d(TAG,
					"onStart " + this + " begins at "
							+ System.currentTimeMillis());
		}

		Intent intent = getIntent();
		if (killProcessIfPossible(intent)) {
			return;
		}

		mWorkspace.onStart();

		mLifecycleSubject.notifyStart();


		if (LOGD_ENABLED) {
			XLog.d(TAG,
					"onStart " + this + " ends at "
							+ System.currentTimeMillis());
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		XLog.d("Snser", "LauncherState onStop this=" + this.hashCode() + " pid=" + android.os.Process.myPid());
		setLauncherState(LauncherState.ONSTOP);
		
		XLog.e("adapter", "onStop " + this);

		mLifecycleSubject.notifyStop();
		mWorkspace.onStop();
	}

	void onScreenOff() {
		closeSystemDialogs();

		int count = mWidgetViews.size();
		for (int i = 0; i < count; i++) {
			mWidgetViews.get(i).onScreenOff();
		}

		mLastScreenOffTime = System.currentTimeMillis();

		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
	}

	void onScreenOn() {
		int count = mWidgetViews.size();
		for (int i = 0; i < count; i++) {
			mWidgetViews.get(i).onScreenOn();
		}

		if (mLastScreenOffTime != null) {
			int minute = (int) ((System.currentTimeMillis() - mLastScreenOffTime) / DateUtils.MINUTE_IN_MILLIS);

			synchronized (mNewComponents) {
				Iterator<ComponentName> iterator = mNewComponents.keySet()
						.iterator();
				while (iterator.hasNext()) {
					final ComponentName componentName = iterator.next();
					int time = mNewComponents.get(componentName).showTimeLeft;
					time -= minute;
					mNewComponents.get(componentName).showTimeLeft = time;
					if (time <= 0) {
						iterator.remove();
					}
				}
			}

			mLastScreenOffTime = null;
		}

		// FlashlightResolver.screenOn(this, null);

		if (mTimer == null && !mNewComponents.isEmpty()) {
			mTimer = new Timer();
			mTimer.schedule(new NewInstalledAppTask(), 0, TIME_INTERVAL);
		}
	}

	@Override
	protected void onResume() {
	    XLog.d("Snser", "LauncherState onResume this=" + this.hashCode() + " pid=" + android.os.Process.myPid());
	    setLauncherState(LauncherState.ONRESUME);
	    if (App.TIME_LAUNCHER_ONRESUME == 0) {
	        App.TIME_LAUNCHER_ONRESUME = System.currentTimeMillis();
	    }
		initHomeWatch();
		
		super.onResume();
		
		mActivityStarted = false;

		doResume();

		if (Constant.PROFILE_STARTUP) {
			if (Utils.isExternalStorageWritable()) {
				android.os.Debug.stopMethodTracing();
			}
		}

		// 统一放在ResolverCommons的监听里处理
		// if(!ResolverCommons.isAutoBrightness(this)){
		// // Reload the brightness from system settings.
		// ResolverCommons.setBrightness(this, -1,
		// ResolverCommons.getCurrentBrightness(this));
		// }

		App.getApp().sendLocalBroadcast(new Intent("cc.snser.launcher.action.FORCE_HIDE"));
		
		requestFullScreen(true);
		
		SharedPreferences sharedPreferences = getSharedPreferences("FRIST_SHOW_SERVICE_PROTOCAL", Context.MODE_PRIVATE);
    	boolean isFirstShowServuceProtocal = sharedPreferences.getBoolean("FirstShowUserServiceProtocal", true);
    	if(isFirstShowServuceProtocal){         
    		boolean isshowuserService = UserServiceProtolHelper.getInstance().showUseService();
    		if(!isshowuserService){
    			//UserServiceProtolHelper.getInstance().createWindowManager();
        		//UserServiceProtolHelper.getInstance().showDesk();	
    		}
    		
    	}
	}
	private void initHomeWatch(){
		if(mHomeWatcher == null){
			mHomeWatcher = new HomeWatcher(this);
			mHomeWatcher.setOnHomePressedListener(new OnHomePressedListener() {
				@Override
				public void onHomePressed() {
					dispatchHomePressed();
				}

				@Override
				public void onHomeLongPressed() {
				}
			});
			mHomeWatcher.startWatch();
		}
	}

	private void doResume() {
		if (LOGD_ENABLED) {
			XLog.d(TAG,
					"onResume " + this + " begins at "
							+ System.currentTimeMillis());
		}

		// 设定为非touch状态，虽然不是很可靠
		RuntimeConfig.sLauncherInTouching = false;

		mPaused = false;
		mIgnoreOnResumeNeedsLoad = false;
		if (mOnResumeNeedsLoad) {
			if (mPausedTime <= 0
					|| System.currentTimeMillis() - mPausedTime >= 10 * DateUtils.MINUTE_IN_MILLIS) {
				mWorkspaceLoading = true;
				mModel.startLoader(this, true, false);
			}
			mOnResumeNeedsLoad = false;
		}
		mPausedTime = -1;

		mHandler.post(new Runnable() { // 耗时工作放在下次消息循环中
			@Override
			public void run() {
				int count = mWidgetViews.size();
				for (int i = 0; i < count; i++) {
					mWidgetViews.get(i).onLauncherResume();
				}
			}
		});
		
		mWorkspace.onResume();

		mLifecycleSubject.notifyResume();

		mWaitingForResult = false;

		if (LOGD_ENABLED) {
			XLog.d(TAG,
					"onResume " + this + " ends at "
							+ System.currentTimeMillis());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		XLog.d("Snser", "LauncherState onPause this=" + this.hashCode() + " pid=" + android.os.Process.myPid());
		setLauncherState(LauncherState.ONPAUSE);
		
		doPause();
	}

	private void doPause() {
		if (LOGD_ENABLED) {
			XLog.d(TAG,
					"onPause " + this + " begins at "
							+ System.currentTimeMillis());
		}

		// 设定为非touch状态，虽然不是很可靠
		RuntimeConfig.sLauncherInTouching = false;

		mPaused = true;
		mPausedTime = System.currentTimeMillis();
		mDragController.cancelDrag(true);

		dragLayer.clearAllResizeFrames();

		mHandler.post(new Runnable() { // 耗时工作放在下次消息循环中
			@Override
			public void run() {
				int count = mWidgetViews.size();
				for (int i = 0; i < count; i++) {
					mWidgetViews.get(i).onLauncherPause();
				}
			}
		});

		mWorkspace.onPause();

		mLifecycleSubject.notifyPause();

		if (LOGD_ENABLED) {
			XLog.d(TAG,
					"onPause " + this + " ends at "
							+ System.currentTimeMillis());
		}

	}
	
	public boolean isPaused() {
		return mPaused;
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (LOGD_ENABLED) {
			XLog.d(TAG, "onRetainNonConfigurationInstance");
		}
		// Flag the loader to stop early before switching
		// if (mModel != null) {
		// mModel.stopLoader(this);
		// }
		return Boolean.TRUE;
	}

	// We can't hide the IME if it was forced open. So don't bother
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (LOGD_ENABLED) {
			XLog.d(TAG, "onWindowFocusChanged: " + hasFocus);
		}

		if (hasFocus && this.mStatusbarExpanded) {
			this.mStatusbarExpanded = false;
		}

		if (mWorkspace != null) {
			mWorkspace.onWindowFocusChanged(hasFocus);
		}
	}

	@Override
	public void onWindowAttributesChanged(WindowManager.LayoutParams params) {
		super.onWindowAttributesChanged(params);
		if (dragLayer != null) {
			ResourceUtils
					.updateStatesBarCurrentHeight(
							this,
							(params.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0);
		}
	}

	private boolean acceptFilter() {
		final InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		return !inputManager.isFullscreenMode();
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (isFinishing()) {
			return super.onKeyUp(keyCode, event);
		}
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return super.onKeyUp(keyCode, event);
		}
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			boolean isMenuKeyLongPressFlag = mIsMenuKeyLongPressFlag;
			mIsMenuKeyLongPressFlag = false;

			if(mDragController != null && mDragController.isDragging()){
				return true;
			}

			if (isWorkspaceVisible()) {
				if (isMenuKeyLongPressFlag) {
					return true;
				}
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	private boolean mIsMenuKeyLongPressFlag;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (isFinishing()) {
			return super.onKeyDown(keyCode, event);
		}
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return super.onKeyDown(keyCode, event);
		}
		boolean handled = super.onKeyDown(keyCode, event);
		try {
			if (!handled && acceptFilter() && keyCode != KeyEvent.KEYCODE_ENTER
					&& mDefaultKeySsb != null) {
				if (DeviceUtils.isW2013()
						&& getResources().getConfiguration().keyboard == 3
						&& keyCode >= 7 && keyCode <= 18) {
					Intent intent = new Intent("android.intent.action.DIAL",
							Uri.parse("tel:"));
					if (keyCode >= 7 && keyCode <= 16) {
						intent.putExtra("isKeyTone", 1);
					} else if (keyCode == 18) {
						intent.putExtra("isPoundKey", 1);
					}
					ActionUtils.startActivitySafely(this, intent);
					return handled;
				} else {
					boolean gotKey = TextKeyListener.getInstance().onKeyDown(
							mWorkspace, mDefaultKeySsb, keyCode, event);
					if (gotKey && mDefaultKeySsb != null
							&& mDefaultKeySsb.length() > 0) {
						// something usable has been typed - start a search
						// the typed text will be retrieved and cleared by
						// showSearchDialog()
						// If there are multiple keystrokes before the search
						// dialog takes focus,
						// onSearchRequested() will be called for every
						// keystroke,
						// but it is idempotent, so it's fine.
						return onSearchRequested();
					}
				}
			}
		} catch (Exception e) {
			XLog.e(TAG, "Failed to handle the search request.", e);
		}

		// Eat the long press event so the keyboard doesn't come up.
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (event.isLongPress()) {
				mIsMenuKeyLongPressFlag = true;
				return true;
			}
		}

		return handled;
	}
	
	public ArrayList<HomeItemInfo> getDesktopItems() {
	    return mDesktopItems;
	}

	public ArrayList<WidgetView> getWidgetViews() {
		return mWidgetViews;
	}

	/**
	 * Finds all the views we need and configure them properly.
	 */
	private void setupViews(Intent intent) {
		setContentView(R.layout.launcher);

		final DragController dragController = mDragController;
		dragController.removeDragScollers();
		dragController.removeDragListeners();
		dragController.removeDropTargets();

		dragLayer = (DragLayer) findViewById(R.id.drag_layer);
		dragLayer.setDragController(dragController);
		dragLayer.setLauncher(this);

		// init workspace resources
		mWorkspace = (Workspace) dragLayer.findViewById(R.id.workspace);
		mWorkspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CarOSLauncherBase.getInstance().printStartLog();
            }
        });
		final Workspace workspace = mWorkspace;
		workspace.setHapticFeedbackEnabled(false);

		mIndicatorHome = (ScreenIndicator) dragLayer.findViewById(R.id.indicator);
		mIndicatorHome.init(Constant.CONTAINER_HOME, workspace,
				workspace.getScreenCount(), workspace.getCurrentScreen(),
				new ScreenIndicator.OnIndicatorChangedListener() {
					@Override
					public void snapToScreen(int whichScreen) {
						workspace.scrollToScreen(whichScreen);
					}
				});
		mIndicatorHome.setHomeScreen(workspace.getDefaultScreen());
		//根据全屏和分屏，设置不同的margin
		MarginLayoutParams params = (MarginLayoutParams) mIndicatorHome.getLayoutParams();
		params.setMarginStart(ChannelLayoutAdapter.getIndicatorMarginLeft(this));
		mIndicatorHome.setLayoutParams(params);

		workspace.setScreenIndicator(mIndicatorHome);

		workspace.setDragController(dragController);
		workspace.setLauncher(this);


		dragController.addDragScoller(workspace);

		dragController.setMoveTarget(workspace);

		dragController.addDropTarget(workspace);

		// init common resources
		mDeleteZone = (DeleteZone) dragLayer.findViewById(R.id.delete_zone);
		final DeleteZone deleteZone = mDeleteZone;

		mNotifyZone = (NotifyZone) dragLayer.findViewById(R.id.notify_zone);
		mNotifyZone.setLauncher(this);

		deleteZone.setLauncher(this);
		//deleteZone.setDragController(dragController);
		
		mVirtualStatusBar = (VirtualStatusBar)dragLayer.findViewById(R.id.statusbar);
		mVirtualStatusBar.setLauncher(this);
		VirtualStatusBarBase.getInstance().init(getApplicationContext(), mVirtualStatusBar);
		
		mOverScrollView = (OverScrollView)dragLayer.findViewById(R.id.overscroll);
		mWorkspace.setOverScrollView(mOverScrollView);

		dragController.setScrollView(dragLayer);
		//dragController.addDragListener(deleteZone);
		dragController.addDragListener(mWorkspace);

		//dragController.addDropTarget(deleteZone);
		dragController.addDropTarget(mNotifyZone);
	}

	/**
	 * Creates a view representing a shortcut.
	 * 
	 * @param info
	 *            The data structure describing the shortcut.
	 * 
	 * @return A View inflated from R.layout.application.
	 */
	public View createShortcut(HomeDesktopItemInfo info) {
		return createShortcut(R.layout.shortcut,

		(ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentScreen()), info);
	}

	/**
	 * Creates a view representing a shortcut inflated from the specified
	 * resource.
	 * 
	 * @param layoutResId
	 *            The id of the XML layout used to create the shortcut.
	 * @param parent
	 *            The group the shortcut belongs to.
	 * @param info
	 *            The data structure describing the shortcut.
	 * 
	 * @return A View inflated from layoutResId.
	 */
	public View createShortcut(int layoutResId, ViewGroup parent,
			HomeDesktopItemInfo info) {
		Shortcut shortcut = (Shortcut) mInflater.inflate(layoutResId, parent,
				false);
		shortcut.setTag(info);
		shortcut.setOnClickListener(mWorkspace.mWorkspaceOnClickListener);
		shortcut.setIcon(info.getIcon(mIconCache));
		shortcut.setText(info.getTitle());

		if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
				&& info.getIntent() != null
				&& containsNewComponent(info.getIntent().getComponent()) != null) {
			shortcut.showTipImage(IconTip.TIP_NEW, false);
		}

		return shortcut;
	}


	/**
	 * Add a shortcut to the workspace.
	 * 
	 * @param data
	 *            The intent describing the shortcut.
	 * @param cellInfo
	 *            The position on screen where to create the shortcut.
	 */
	private void completeAddSysShortcut(Intent data,
			CellLayout.CellInfo cellInfo) {
		// handleAddScreenBeforeAddItem();
		int screenIndex = mWorkspace.getCurrentScreen();
		CellLayout layout = (CellLayout) mWorkspace.getChildAt(screenIndex);

		if (!layout.hasFreeCell(1, 1, -1, -1)) {
			final Intent finalData = data;
			final CellLayout.CellInfo finalCellInfo = cellInfo;
			if (mMakeNewScreen == null) {
				mMakeNewScreen = new MakeNewScreen(new MakeNewScreenCallback() {
					@Override
					public void onMakeNewScreenFinish(int newScreenIndex) {
						Launcher.this.mMakeNewScreen = null;
						completeAddSysShortcut(finalData, finalCellInfo);
					}
				});
				mMakeNewScreen.start();
			} else {
				// ignore multi click
			}
			return;
		}

		final int[] xy = mCellCoordinates;
		if (!findSlot(xy, mWorkspace.getCurrentScreen(), 1, 1, cellInfo == null ? -1 : cellInfo.cellX,
				cellInfo == null ? -1 : cellInfo.cellY)) {
			return;
		}
		final HomeDesktopItemInfo info = DbManager.addShortcutToDesktop(this,
				data, mWorkspace.getCurrentScreen(), xy[0], xy[1], false, true,
				false);
		((LauncherModelIphone) getModel()).addItem(info, false);// 补充到shortcut列表中
		completeAddShortcutOnWorkspace(info);
	}

	private void completeAddShortcutOnWorkspace(HomeDesktopItemInfo info) {
		final View view = createShortcut(info);
		mWorkspace.addInScreen(view, info.screen, info.cellX, info.cellY, 1, 1,
				isWorkspaceLocked());

	}
	
	   private void completeAddAppWidget(Intent data, CellLayout.CellInfo cellInfo) {
	        completeAddAppWidget(data, cellInfo, mWorkspace.getCurrentScreen());
	    }

	/**
	 * 系统Widget被成功创建出来 Add a widget to the workspace.
	 * 
	 * @param data
	 *            The intent describing the appWidgetId.
	 * @param cellInfo
	 *            The position on screen where to create the widget.
	 */
	private void completeAddAppWidget(Intent data, CellLayout.CellInfo cellInfo, int screen) {
		Bundle extras = data.getExtras();
		int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		if (LOGD_ENABLED) {
			XLog.d(TAG, "dumping extras content=" + extras.toString());
		}

		// Calculate the grid spans needed to fit this widget
		AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
		
		if (appWidgetInfo == null) {
		    return;
		}
		
		int[] spans = {0}; 
		//minWidth + padding.left + padding.right && minHeight + padding.top + padding.bottom
		spans = getSpanForWidget(this, appWidgetInfo);
		
		if (LOGD_ENABLED) {
			XLog.d(TAG, "expected size: " + spans[0] + " * " + spans[1]);
		}
		final int xCount = SettingPreferences.getHomeLayout(this)[1];
		final int yCount = SettingPreferences.getHomeLayout(this)[0];
		if (spans[0] > xCount) {
			spans[0] = xCount;
		}
		if (spans[1] > yCount) {
			spans[1] = yCount;
		}
		if (appWidgetInfo.provider != null && yCount == 5) {
			if ("com.sec.android.widgetapp.favoriteswidget"
					.equals(appWidgetInfo.provider.getPackageName())) {
				if ("com.sec.android.widgetapp.favoriteswidget.SeniorFavoriteWidgetProviderSmall"
						.equals(appWidgetInfo.provider.getClassName())) {
					if (spans[1] == 2) {
						spans[1] = 3;
					}
				} else if ("com.sec.android.widgetapp.favoriteswidget.SeniorFavoriteWidgetProviderLarge"
						.equals(appWidgetInfo.provider.getClassName())) {
					if (spans[1] == 4) {
						spans[1] = 5;
					}
				}
			}
		}

		CellLayout currentCellLayout = (CellLayout) mWorkspace.getChildAt(screen);
		if (!currentCellLayout.hasFreeCell(spans[0], spans[1],
						cellInfo == null ? -1 : cellInfo.cellX,
						cellInfo == null ? -1 : cellInfo.cellY)) {

			//上面已经判断了currentCellLayout has not FreeCell（没有空余的空间），但是这一屏又一个元素都没有，说明这个widget太大了，以至于一整屏都放不下。
			if(currentCellLayout.getChildCount() == 0){
				return;
			}

			final Intent finalData = data;
			final CellLayout.CellInfo finalCellInfo = cellInfo;
			if (mMakeNewScreen == null) {
				mMakeNewScreen = new MakeNewScreen(new MakeNewScreenCallback() {

					@Override
					public void onMakeNewScreenFinish(int newScreenIndex) {
						Launcher.this.mMakeNewScreen = null;
						completeAddAppWidget(finalData, finalCellInfo);
					}
				});
				mMakeNewScreen.start();
			}
			return;
		}

		// Try finding open space on Launcher screen
		final int[] xy = mCellCoordinates;
		if (!findSlot(xy, screen, spans[0], spans[1], cellInfo == null ? -1
				: cellInfo.cellX, cellInfo == null ? -1 : cellInfo.cellY)) {
			if (appWidgetId != -1) {
				mAppWidgetHost.deleteAppWidgetId(appWidgetId);
			}
			return;
		}

		// Build Launcher-specific widget info and save to database
		final LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(
				appWidgetId);
		launcherInfo.spanX = spans[0];
		launcherInfo.spanY = spans[1];
		launcherInfo.appWidgetCn = data.getComponent();

		DbManager.addItemToDatabase(this, launcherInfo,
				LauncherSettings.Favorites.CONTAINER_DESKTOP,
				screen, xy[0], xy[1], false);

		mDesktopItems.add(launcherInfo);

		// Perform actual inflation because we're live
		launcherInfo.setHostView((LauncherAppWidgetHostView)mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo));
		launcherInfo.getHostView().setAppWidget(appWidgetId, appWidgetInfo);
        if (launcherInfo.spanX == 2 && launcherInfo.spanY == 3) {
            launcherInfo.getHostView().setPadding(WorkspaceCellLayoutMeasure.widget2x3MarginLeft, 
                                                  WorkspaceCellLayoutMeasure.widget2x3MarginTop, 
                                                  WorkspaceCellLayoutMeasure.widget2x3MarginRight, 
                                                  WorkspaceCellLayoutMeasure.widget2x3MarginBottom);
        }
		launcherInfo.getHostView().setTag(launcherInfo);

		try {
			mWorkspace.addInScreen(launcherInfo.getHostView(), screen, xy[0], xy[1],
							launcherInfo.spanX, launcherInfo.spanY,
							isWorkspaceLocked());
		} catch (Exception e) {
			if (LOGE_ENABLED) {
				XLog.e(TAG,
						"bind app widget error for its own error! try to catch this exception",
						e);
			}
			try {
				mWorkspace.removeInScreen(launcherInfo.getHostView(), screen);
			} catch (Exception e2) {
				if (LOGE_ENABLED) {
					XLog.e(TAG,
							"bind app widget error for its own error! remove view exception",
							e2);
				}
			}

			removeAppWidget(launcherInfo);
			DbManager.deleteItemByIdImmediately(getApplicationContext(),
					launcherInfo.id);
			ToastUtils.showMessage(
					getApplicationContext(),
					getString(R.string.widget_add_app_widget_failed,
							appWidgetInfo.label));

			return;
		}

		// finish load a widget, send it an intent
		if (Constant.ENABLE_WIDGET_SCROLLABLE && appWidgetInfo != null) {
			appwidgetReadyBroadcast(appWidgetId, appWidgetInfo.provider);
		}

		if (LOGD_ENABLED) {
			XLog.d(TAG, "completeAddAppWidget ends.");
		}

		notifyAppWidgetResized(launcherInfo.getHostView(), launcherInfo.spanX,
				launcherInfo.spanY);

		if (mWorkspace.isInEditMode()) {
			mWorkspace.enableCurrentScreenCache();
		}
	}

	private void notifyAppWidgetResized(final AppWidgetHostView widget,
			int spanX, int spanY) {
		if (!AppWidgetUtils.isSizeUpdateable()) {
			return;
		}
		boolean delay = false;
		CellLayout cell = (CellLayout) mWorkspace.getChildAt(mWorkspace
				.getCurrentScreen());

		float density = getResources().getDisplayMetrics().density;
		final int w = (int) (cell.getItemWidthForSpan(spanX) / density);
		final int h = (int) (cell.getItemHeightForSpan(spanY) / density);
		if (!delay) {
			AppWidgetUtils.resizeAppWidget(widget, w, h, w, h);
		} else {
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					AppWidgetUtils.resizeAppWidget(widget, w, h, w, h);
				}
			}, 50);
		}
	}

	/**
	 * TODO: 重构函数 completeAddWidgetView completeAddWidgetViewToPosition
	 * 
	 * @param widget
	 */
	public void completeAddWidgetView(final Widget widget) {
		final int targetScreen = reviseAddItemCellInfo(-1, true,
				widget.getSpanX(), widget.getSpanY());

		if (targetScreen < 0) {
			return;
		}

		if (targetScreen != mWorkspace.getCurrentScreen()) {
			if (mMakeNewScreen == null) {
				mMakeNewScreen = new MakeNewScreen(
						new Launcher.MakeNewScreenCallback() {
							@Override
							public void onMakeNewScreenFinish(int newScreenIndex) {
								Launcher.this.mMakeNewScreen = null;
								completeAddWidgetView(widget, null);
							}
						});
				mMakeNewScreen.start();
			} else {
				// 动画完成期间，不用再次创建新屏
			}
		} else {
			completeAddWidgetView(widget, mPendingAddInfo);
		}
	}

	/**
	 * TODO: 重构函数 completeAddWidgetView completeAddWidgetViewToPosition
	 * 
	 * @param widget
	 * @param cellInfo
	 * @return
	 */
	private boolean completeAddWidgetView(Widget widget,
			CellLayout.CellInfo cellInfo) {
		WidgetView widgetView = widget.getWidgetView(this);
		if (widgetView == null) {
			return false;
		}
		widgetView.setWidgetContext(mWidgetContext);

		// Calculate the grid spans needed to fit this widget
		int[] spans = new int[] { widget.getSpanX(), widget.getSpanY() };

		// WorkspaceLayoutPolicy.getInstance().autoExpandWidgetSpans(this,
		// spans);

		// Try finding open space on Launcher screen
		final int[] xy = mCellCoordinates;
		if (!findSlot(xy, mWorkspace.getCurrentScreen(), spans[0], spans[1], cellInfo == null ? -1
				: cellInfo.cellX, cellInfo == null ? -1 : cellInfo.cellY)) {
			return false;
		}
		return completeAddWidgetViewToPosition(widget, widgetView, xy);
	}

	/**
	 * Add a widget view to the workspace. TODO: 重构函数 completeAddWidgetView
	 * completeAddWidgetViewToPosition
	 * 
	 * @param widget
	 *            The custom widget to add
	 * @param cellInfo
	 *            The position on screen where to create the widget.
	 */
	public boolean completeAddWidgetViewToPosition(Widget widget,
			WidgetView widgetView, int[] xy) {
		if (widgetView == null) {
			widgetView = widget.getWidgetView(this);
			if (widgetView == null) {
				return false;
			} else {
				widgetView.setWidgetContext(mWidgetContext);
			}
		}

		completeAddWidgetView(widget, widgetView, xy);

		return true;
	}

	public void completeAddWidgetView(Widget widget, WidgetView widgetView,
			int[] xy) {
		handleAddScreenBeforeAddItem();
		completeAddWidgetView(widget, widgetView, xy,
				mWorkspace.getCurrentScreen());
	}

	public void completeAddWidgetView(Widget widget, WidgetView widgetView,
			int[] xy, int screenIndex) {
		Serializable identity = null;

		identity = widget.type;

		// Build Launcher-specific widget info and save to database
		LauncherWidgetViewInfo launcherInfo = new LauncherWidgetViewInfo(
				widget.type, identity);

		launcherInfo.spanX = widget.getSpanX();
		launcherInfo.spanY = widget.getSpanY();

		DbManager.addItemToDatabase(this, launcherInfo,
				LauncherSettings.Favorites.CONTAINER_DESKTOP, screenIndex,
				xy[0], xy[1], false);

		mDesktopItems.add(launcherInfo);
		mWidgetViews.add(widgetView);

		// Perform actual inflation because we're live
		launcherInfo.hostView = widgetView;

		launcherInfo.hostView.setTag(launcherInfo);
		launcherInfo.hostView
				.setOnLongClickListener(mWorkspace.mWorkspaceOnLongClickListener);
		launcherInfo.hostView.setDrawingCacheEnabled(true);

		vibrator();
		launcherInfo.hostView.init(launcherInfo);

		// WorkspaceLayoutPolicy.getInstance().autoExpandWidgetItem(this,
		// launcherInfo);
		mWorkspace.addInScreen(launcherInfo.hostView, screenIndex, xy[0],
				xy[1], launcherInfo.spanX, launcherInfo.spanY,
				isWorkspaceLocked());
		launcherInfo.hostView.onAdded(true);
		if (mWorkspace.getCurrentScreen() == screenIndex) {
			launcherInfo.hostView.screenIn();
		}
	}

	public void removeItem(HomeDesktopItemInfo itemInfo,
			final boolean isRemoveModelOnly) {
		mDesktopItems.remove(itemInfo);

		if (isRemoveModelOnly) {
			return;
		}

		View host;

		if (itemInfo.getContainer() == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
			final CellLayout cellLayout = (CellLayout) mWorkspace
					.getChildAt(itemInfo.getScreen());
			host = cellLayout.getCellView(itemInfo.getCellX(),
					itemInfo.getCellY());
		} else {
			host = null;
		}

		if (host != null) {
			final View targetView = host;
			doRemoveView(targetView, itemInfo, itemInfo.getContainer());
		}

		DbManager.deleteItemFromDatabase(this, itemInfo);
	}

	public void removeItemOnly(HomeDesktopItemInfo itemInfo) {
		mDesktopItems.remove(itemInfo);

		View host;

		if (itemInfo.getContainer() == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
			final CellLayout cellLayout = (CellLayout) mWorkspace
					.getChildAt(itemInfo.getScreen());
			host = cellLayout.getCellView(itemInfo.getCellX(),
					itemInfo.getCellY());
		} else {
			host = null;
		}

		if (host != null) {
			final View targetView = host;
			doRemoveView(targetView, itemInfo, itemInfo.getContainer());
		}
	}

	public void removeAppWidget(LauncherAppWidgetInfo launcherInfo) {
	    removeAppWidget(launcherInfo, true);
	}
	
	public void removeAppWidget(LauncherAppWidgetInfo launcherInfo, boolean bUnbindWidgetId) {
	    if (bUnbindWidgetId) {
	        final int appWidgetId = launcherInfo.appWidgetId;
	        if (Constant.ENABLE_WIDGET_SCROLLABLE
	                && mWorkspace.isWidgetScrollable(appWidgetId)) {
	            mWorkspace.unbindWidgetScrollableId(appWidgetId);
	        }
	        final LauncherAppWidgetHost appWidgetHost = this.mAppWidgetHost;
	        if (appWidgetHost != null) {
	            // Deleting an app widget ID is a void call but writes to disk
	            // before returning
	            // to the caller...
	            new Thread("deleteAppWidgetId") {
	                @Override
	                public void run() {
	                    appWidgetHost.deleteAppWidgetId(appWidgetId);
	                }
	            }.start();
	        }
	    }

		mDesktopItems.remove(launcherInfo);
		launcherInfo.clearHostView();
	}

	public void removeWidgetView(LauncherWidgetViewInfo launcherInfo) {
		mDesktopItems.remove(launcherInfo);
		if (launcherInfo.hostView != null) {
			mWidgetViews.remove(launcherInfo.hostView);
			launcherInfo.hostView.onRemoved(true);
			launcherInfo.hostView = null;
		}
	}

	private void doRemoveView(View targetView, HomeItemInfo item, long container) {
		boolean removed = false;
		if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
			final CellLayout cellLayout = (CellLayout) targetView.getParent();
			if (cellLayout != null) {
				cellLayout.removeView(targetView);
			}
			removed = true;
		}

		if (removed && targetView instanceof DropTarget) {
			mDragController.removeDropTarget((DropTarget) targetView);
		}
	}

	void closeSystemDialogs() {
		getWindow().closeAllPanels();

		int count = mWidgetViews.size();
		for (int i = 0; i < count; i++) {
			mWidgetViews.get(i).onCloseSystemDialogs();
		}

		// Whatever we were doing is hereby canceled.
		mWaitingForResult = false;
	}

	private boolean killProcessIfPossible(Intent intent) {
		int flag = intent.getIntExtra("flag", 0);

		if (flag == Constant.FLAG_STOP_LAUNCHER) {
			killLauncher(false, false);
			return true;
		}

		if (flag == Constant.FLAG_RESTART_LAUNCHER) {
			killLauncher(true, true);
			return true;
		}

		if (flag == Constant.FLAG_RESTART_ALL_PROCESS) {
			killLauncher(true, true);
			return true;
		}

		return false;
	}

	private void killLauncher(boolean restart, boolean allProcess) {
		NotificationUtils.cancelAllNotificationBeforeKilled(this);

		if (!restart) {
			finish();
		}

		if (allProcess) {
			Utils.killLauncherAllProcess(this);
		} else {
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	private void reloadWorkspace(boolean reloadLauncherForTheme,
			boolean restartLauncherForWallpaper,
			boolean restartLauncherForIconTextSize) {
		if (reloadLauncherForTheme || restartLauncherForIconTextSize) {
			if (mCurrentProgressDialog != null) {
				DialogUtils.dismissProgressDialog(mCurrentProgressDialog, this);
			}

			mCurrentProgressDialog = DialogUtils.showProgressDialog(
					Launcher.this,
					getString(R.string.launcher_reloading_message), true,
					new OnCancelListener() {
						public void onCancel(DialogInterface dialog) {

						}
					}, PROGRESS_STYLE.PROGRESSDIALOG_STYLE2);
			
			mCurrentProgressDialog.setCanceledOnTouchOutside(false);

			if (reloadLauncherForTheme) {
				IconFsCache.getInstance(this).expireCache();
				IconWidgetCache.clear();
			}

			if (reloadLauncherForTheme) {
				resetCurrentTheme();
			}

			mWorkspaceLoading = true;
			mModel.startLoader(this, mHandler, true);
		}
	}

	private void handleStartIntent(Intent intent) {
		boolean reloadLauncherForTheme = false;
		boolean restartLauncherForWallpaper = false;
		boolean restartLauncherForIconTextSize = false;

		int flag = intent.getIntExtra("flag", 0);
		if (flag == Constant.FLAG_RELOAD_LAUNCHER_FOR_THEME) {
			reloadLauncherForTheme = true;
		} else if (flag == Constant.FLAG_RESTART_LAUNCHER_FOR_WALLPAPER) {
			restartLauncherForWallpaper = true;
		} else if (flag == Constant.FLAG_START_LAUNCHER_FOR_SETTING_DEFAULT) {
			App.getApp().sendLocalBroadcast(
					new Intent("cc.snser.launcher.action.FORCE_HIDE"));
			ResolveInfo info = Utils
					.queryDefaultLauncher(getApplicationContext());
			boolean isDefault = info != null
					&& Constant.PACKAGE_NAME
							.equals(info.activityInfo.applicationInfo.packageName);
			if (!isDefault) {
				isNeedSetDefaultLauncher = true;
			} else {
				ToastUtils.showMessage(this,
						R.string.set_default_launcher_success);
			}
		} else if (flag == Constant.FLAG_RESTART_LAUNCHER_FOR_ICON_TEXT_SIZE) {
			restartLauncherForIconTextSize = true;
		}

		if (LOGD_ENABLED) {
			XLog.d(TAG, "onHandleStartIntent. flag = " + flag);
		}

		reloadWorkspace(reloadLauncherForTheme, restartLauncherForWallpaper,
				restartLauncherForIconTextSize);
	}

	private void resetCurrentTheme() {
		IconBg.resetCurrentId();
		IconWidgetCache.clear();

		WorkspaceIconUtils.resetWorkspaceIconTextColor();
		WorkspaceIconUtils.resetWorkspaceIconTextSize();
		WorkspaceIconUtils.resetIconSizeTypeAndCustomIconSize();
		WorkspaceIconUtils.resetIconSize();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (dragLayer != null)
			dragLayer.clearAllResizeFrames();

		if (LOGD_ENABLED) {
			XLog.d(TAG, "onNewIntent: " + intent);
		}

		if (killProcessIfPossible(intent)) {
			return;
		}

		mLifecycleSubject.notifyNewIntent();


		handleStartIntent(intent);

		// Close the menu
		if (Intent.ACTION_MAIN.equals(intent.getAction())) {
			handleActionMain(intent);
		}
	}

	private void handleActionMain(Intent intent) {
		// also will cancel mWaitingForResult.
		closeSystemDialogs();
		boolean alreadyOnHome = ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		if (!mActivityStarted && mWorkspace != null && !mWorkspace.isDefaultScreenShowing()) {
			boolean animate = alreadyOnHome;
			if (!animate) {
				mWorkspace.moveToDefaultScreen(alreadyOnHome);
			} else {
				mWorkspace.post(new Runnable() {
					@Override
					public void run() {
						if(mWorkspace != null){
							mWorkspace.moveToDefaultScreen(true);
						}
					}
				});
			}
		}

		final View v = getWindow().peekDecorView();
		if (v != null && v.getWindowToken() != null) {
			InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			try {
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// Do not call super here
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Do not call super here
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		XLog.d("Snser", "LauncherState onDestroy this=" + this.hashCode() + " pid=" + android.os.Process.myPid());
		setLauncherState(LauncherState.ONDESTORY);

		// killLauncher(true, true);
		sIntanceCount--;
		removeInstance();

		XLog.e("adapter", "begin onDestroy " + this);

		if (LOGD_ENABLED) {
			XLog.d(TAG, "onDestroy " + this + " begins");
		}
		
		if(mHomeWatcher != null){
			mHomeWatcher.stopWatch();
		}

		// 设定为非touch状态，虽然不是很可靠
		RuntimeConfig.sLauncherInTouching = false;

		IconPressAnimation.release();

		mCrashHandler.destroy();


		if (sIntanceCount <= 0) {
			try {
				mAppWidgetHost.stopListening();
			} catch (NullPointerException ex) {
			    XLog.printStackTrace(ex);
				//XLog.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
			}
		}

		TextKeyListener.getInstance().release();

		if (mModel != null) {
			mModel.stopLoader(this);
		}

		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}

		unbindDesktopItems();

		getContentResolver().unregisterContentObserver(mWidgetObserver);

		unregisterReceiver(mReceiver);

		if (Constant.ENABLE_WIDGET_SCROLLABLE) {
			mWorkspace.unregisterProvider();
		}

		int count = mWidgetViews.size();
		for (int i = 0; i < count; i++) {
			mWidgetViews.get(i).onDestroy();
		}

		if (mCurrentProgressDialog != null) {
			DialogUtils.dismissProgressDialog(mCurrentProgressDialog,
					Launcher.this);
			mCurrentProgressDialog = null;
		}

		mLifecycleSubject.notifyDestroy();
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		if (requestCode >= 0) {
			mWaitingForResult = true;
		}

		try {
			super.startActivityForResult(intent, requestCode);
			mActivityStarted = true;
		} catch (ActivityNotFoundException e) {
			ToastUtils.showMessage(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT);
			XLog.e(TAG, "ActivityNotFoundException. intent=" + intent, e);
			onActivityResult(requestCode, RESULT_CANCELED, null);
		} catch (SecurityException e) {
			ToastUtils.showMessage(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT);
			XLog.e(TAG,
					"Launcher does not have the permission to launch "
							+ intent
							+ ". Make sure to create a MAIN intent-filter for the corresponding activity "
							+ "or use the exported attribute for this activity.",
					e);
			onActivityResult(requestCode, RESULT_CANCELED, null);
		} catch (Exception e) {
			XLog.e(TAG, "Failed to start the activity", e);
			onActivityResult(requestCode, RESULT_CANCELED, null);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	public boolean isWorkspaceLoading() {
		return mWorkspaceLoading
				|| (mCurrentProgressDialog != null && mCurrentProgressDialog
						.isShowing());
	}

	public boolean isWorkspaceLocked() {
		return mWorkspaceLoading || mWaitingForResult;
	}

	/**
	 * 系统Widget选择列表被关闭，选择完成
	 * 
	 * @param data
	 */
	private void addAppWidget(Intent data) {
		// TODO: catch bad widget exception when sent
		int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		AppWidgetProviderInfo appWidget = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

		if (appWidget != null && appWidget.configure != null) {
			// Launch over to configure widget, if needed
			Intent intent = new Intent(
					AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			intent.setComponent(appWidget.configure);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

			startActivityForResultSafely(intent, REQUEST_CREATE_APPWIDGET);
		} else {
			// Otherwise just add it
			onActivityResult(REQUEST_CREATE_APPWIDGET, Activity.RESULT_OK, data);
		}
	}
	
    public void addAppWidget(final ComponentName cn, final int screen, final int cellX, final int cellY) {
        final String widgetPkgName = cn.getPackageName();
        final String widgetClsName = cn.getClassName();
        if (TextUtils.isEmpty(widgetPkgName) || TextUtils.isEmpty(widgetClsName)) {
            return;
        }
        
        try {
            final List<AppWidgetProviderInfo> infos = mAppWidgetManager.getInstalledProviders();
            for (AppWidgetProviderInfo info : infos) {
                final ComponentName provider = info.provider;
                if (provider != null 
                    && widgetPkgName.equals(provider.getPackageName()) 
                    && widgetClsName.equals(provider.getClassName())) {
                    mAppWidgetId = mAppWidgetHost.allocateAppWidgetId();
                    if (AppWidgetUtils.bindAppWidgetIfAllowed(mAppWidgetManager, mAppWidgetId, provider)) {
                        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                        intent.setComponent(provider);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                        setAddItemCellInfo(cellX, cellY, -1, -1);
                        completeAddAppWidget(intent, mPendingAddInfo, screen);
                    } else {
                        //权限问题，后续处理
                        Intent appIntent = AppWidgetUtils.registrationAppWidget(mAppWidgetId, info);
                        startActivityForResult(appIntent,REQUEST_CODE_FOR_REGISTRATION_APPWIDGET);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	private boolean findSlot(int[] cellXY, int screen, int spanX, int spanY,
			int intersectX, int intersectY) {
		CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(screen);
		if (!cellLayout.findCellForSpanThatIntersects(cellXY, spanX, spanY,
				intersectX, intersectY)) {
			ToastUtils.showMessage(this,
					R.string.homescreen_available_for_app_alert,
					Toast.LENGTH_SHORT);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Registers various content observers. The current implementation registers
	 * only a favorites observer to keep track of the favorites applications.
	 */
	private void registerContentObservers() {
		ContentResolver resolver = getContentResolver();
		mWidgetObserver = new AppWidgetResetObserver();
		resolver.registerContentObserver(mModel.getContentAppWidgetResetUri(),
				true, mWidgetObserver);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_HOME:
				return true;
			}
		} else if (event.getAction() == KeyEvent.ACTION_UP) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_HOME:
				return true;
			}
		}

		return super.dispatchKeyEvent(event);
	}

	@Override
	public void onBackPressed() {
		//mHomeWatcher == null,证明activity还没有onresume过，这个时候，直接返回好了。
		//处理QA在一启launcher(先设置default)时，就不停的按返回，home导致异常的bug
		if(mHomeWatcher == null) return;
		
		if (mDragController != null && mDragController.isDragging()) {
			mDragController.cancelDrag(true);
		}

		if (dragLayer != null)
			dragLayer.clearAllResizeFrames();

		if (mMakeNewScreen != null) {
			mMakeNewScreen.stopAnimation();
		}


		if (isWorkspaceVisible()) {

			if (mWorkspace.onBackPressed()) {
				return;
			}
		}
	}

	@Override
	public DragController getDragController() {
		return mDragController;
	}

	/**
	 * Re-listen when widgets are reset.
	 */
	private void onAppWidgetReset() {
		mAppWidgetHost.startListening();
	}

	/**
	 * Go through the and disconnect any of the callbacks in the drawables and
	 * the views or we leak the previous Home screen on orientation change.
	 */
	private void unbindDesktopItems() {
		for (HomeItemInfo item : mDesktopItems) {
			item.unbind();
		}
	}

	public void handleShortcutInfoClick(View v, HomeDesktopItemInfo shortCutInfo) {
		int[] pos = new int[2];
		v.getLocationOnScreen(pos);
		if (v instanceof Shortcut) {
			Shortcut shortcut = (Shortcut) v;
			shortcut.hideTipImage();
		} 
		
		handleShortcutInfoClick(shortCutInfo.intent, new Rect(pos[0], pos[1],
				pos[0] + v.getWidth(), pos[1] + v.getHeight()), v, shortCutInfo);
	}

	private void handleShortcutInfoClick(Intent intent, Rect sourceBounds,
			View v, HomeDesktopItemInfo shortCutInfo) {
		// Open shortcut
		// closeFolder(false);
		if (intent != null
				&& intent.getComponent() != null
				&& "com.motorola.blurgallery".equals(intent.getComponent()
						.getPackageName())
				&& "com.motorola.cgallery.SingleGroup".equals(intent
						.getComponent().getClassName())) {

			intent.setComponent(new ComponentName("com.motorola.blurgallery",
					"com.motorola.cgallery.Dashboard"));
		}

		if (intent == null) {
			ToastUtils.showMessage(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT);
			XLog.e(TAG,
					"Unable to launch start shortcut because intent is null.");
			return;
		}

		if (sourceBounds != null) {
			try {
				intent.setSourceBounds(sourceBounds);
			} catch (Throwable e) {
				// ignore
			}
		}

		//
		ComponentName componentName = intent.getComponent();
		if(componentName != null){
			String pkgName = componentName.getPackageName();
			if(pkgName != null && (pkgName.equalsIgnoreCase("com.lenovo.ideafriend") || pkgName.equalsIgnoreCase("com.yunos.alicontacts"))){
				if(intent.getAction().equalsIgnoreCase(Intent.ACTION_VIEW)){
					intent.setAction(Intent.ACTION_MAIN);
					final HomeDesktopItemInfo asyncUpdate = shortCutInfo;
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							DbManager.updateItemInDatabase(App.getApp().getApplicationContext(), asyncUpdate);
						}
					});
				}
			}
		}
		
		startActivitySafely(intent, shortCutInfo);
	}

	public void startActivitySafely(Intent intent, Object tag) {
		if (intent == null) {
			return;
		}

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		if (ActionUtils.startActivitySafely(this, intent)) {
			mModel.updateAppCalledNum(intent);
			ComponentName name = intent.getComponent();
			removeNewComponent(name);
		}
	}

	public void startActivityForResultSafely(Intent intent, int requestCode) {
		if (intent == null) {
			return;
		}

		if (ActionUtils.startActivityForResultSafely(this, intent, requestCode)) {
			
			mModel.updateAppCalledNum(intent);

			ComponentName name = intent.getComponent();
			removeNewComponent(name);
		}
	}

	public LauncherModel getModel() {
		return mModel;
	}

	public Workspace getWorkspace() {
		return mWorkspace;
	}

	public ScreenIndicator getIndicator() {
		return mIndicatorHome;
	}

	public View getBottomArea() {
		return mIndicatorHome;
	}

	public DeleteZone getDeleteZone() {
		return mDeleteZone;
	}

	public NotifyZone getNotifyZone() {
		return mNotifyZone;
	}

	public DragLayer getDragLayer() {
		return dragLayer;
	}

	public boolean isWorkspaceVisible() {
		return mWorkspace != null;
	}

	public void resetAddItemCellInfo() {
		this.mPendingAddInfo.cellX = -1;
		this.mPendingAddInfo.cellY = -1;
		this.mPendingAddInfo.spanX = -1;
		this.mPendingAddInfo.spanY = -1;
	}

	public void setAddItemCellInfo(int cellX, int cellY, int spanX, int spanY) {
		this.mPendingAddInfo.cellX = cellX;
		this.mPendingAddInfo.cellY = cellY;
		this.mPendingAddInfo.spanX = spanX;
		this.mPendingAddInfo.spanY = spanY;
	}

	/**
	 * ?
	 * 
	 * @return
	 */
	private int reviseAddItemCellInfo(int screen, boolean tryNeighborScreen,
			int spanX, int spanY) {
		if (mWaitingForResult) {
			return -1;
		}

		if (screen < 0) {
			screen = mWorkspace.getCurrentScreen();
		}
		CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(screen);

		boolean find = cellLayout.findCellForSpan(null, spanX, spanY);
		if (!find) {
			if (tryNeighborScreen) {
				final int[] ret = mWorkspace.findVacantArea(screen, false,
						spanX, spanY);
				if (ret != null) {
					return ret[2];
				} else {
					final int lastScreen = mWorkspace.getChildCount() - 1;

					cellLayout = (CellLayout) mWorkspace.getChildAt(lastScreen);
				}
			}

			ToastUtils.showMessage(this,
					R.string.homescreen_available_for_app_alert,
					Toast.LENGTH_SHORT);

			return -1;
		} else {
			return screen;
		}
	}

	private boolean handleAddScreenBeforeAddItem() {
		return handleAddScreenBeforeAddItem(mWorkspace.getCurrentScreen());
	}

	private boolean handleAddScreenBeforeAddItem(int screenIndex) {
		return false;
	}

	/**
	 * Receives notifications whenever the appwidgets are reset.
	 */
	private class AppWidgetResetObserver extends ContentObserver {
		public AppWidgetResetObserver() {
			super(new Handler());
		}

		@Override
		public void onChange(boolean selfChange) {
			onAppWidgetReset();
		}
	}

	/**
	 * If the activity is currently paused, signal that we need to re-run the
	 * loader in onResume.
	 * 
	 * This needs to be called from incoming places where resources might have
	 * been loaded while we are paused. That is becaues the Configuration might
	 * be wrong when we're not running, and if it comes back to what it was when
	 * we were paused, we are not restarted.
	 * 
	 * Implementation of the method from LauncherModel.Callbacks.
	 * 
	 * @return true if we are currently paused. The caller might be able to skip
	 *         some work in that case since we will come back again.
	 */
	@Override
	public boolean setLoadOnResume() {
		if (mPaused) {
			if (mIgnoreOnResumeNeedsLoad) {
				return false;
			}
			if (LOGI_ENABLED) {
				XLog.i(TAG, "setLoadOnResume");
			}
			mOnResumeNeedsLoad = true;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void setIgnoreOnResumeNeedsLoad(boolean ignoreOnResumeNeedsLoad) {
		mIgnoreOnResumeNeedsLoad = ignoreOnResumeNeedsLoad;
	}

	/**
	 * Implementation of the method from LauncherModel.Callbacks.
	 */
	@Override
	public int getCurrentWorkspaceScreen() {
		return mWorkspace == null ? -1 : mWorkspace.getCurrentScreen();
	}

	/**
	 * Refreshes the shortcuts shown on the workspace.
	 * 
	 * Implementation of the method from LauncherModel.Callbacks.
	 */
	@Override
	public void startBindingInHome() {
		for (HomeItemInfo itemInfo : mDesktopItems) {
			if (itemInfo instanceof LauncherWidgetViewInfo) {
				if (((LauncherWidgetViewInfo) itemInfo).hostView != null) {
					((LauncherWidgetViewInfo) itemInfo).hostView.onRemoved(false);
					((LauncherWidgetViewInfo) itemInfo).hostView = null;
				}
			} else if (itemInfo instanceof LauncherAppWidgetInfo) {
				((LauncherAppWidgetInfo)itemInfo).clearHostView();
			}
		}

		mDesktopItems.clear();
		mWidgetViews.clear();

		setLoadOnResume();

		final Workspace workspace = mWorkspace;
		int count = workspace.getChildCount();
		for (int i = 0; i < count; i++) {
			CellLayout screen = ((CellLayout) workspace.getChildAt(i));
			int childCount = screen.getChildCount();
			for (int j = 0; j < childCount; j++) {
				View cell = screen.getChildAt(j);
				if (cell instanceof DropTarget) {
					mDragController.removeDropTarget((DropTarget) cell);
				}
			}
			screen.removeAllViews();
			screen.resetLayout();
		}

		if (DEBUG_USER_INTERFACE) {
			android.widget.Button finishButton = new android.widget.Button(this);
			finishButton.setText("Finish");
			workspace.addInScreen(finishButton, 1, 0, 0, 1, 1);

			finishButton
					.setOnClickListener(new android.widget.Button.OnClickListener() {
						@Override
						public void onClick(View v) {
							finish();
						}
					});
		}
	}

	/**
	 * Bind the items start-end from the list.
	 * 
	 * Implementation of the method from LauncherModel.Callbacks.
	 */
	public View addItem(HomeItemInfo item) {
		final Workspace workspace = mWorkspace;
		View showView = null;

		int screenInDb = item.screen - Workspace.getWorkspacePrefixScreenSize();
		if (handleAddScreenBeforeAddItem(item.screen)) {
			if (screenInDb < 0) {
				screenInDb = 0;
				item.screen = Workspace.getWorkspacePrefixScreenSize();
				DbManager.moveItemInDatabase(this, item);
			}
		}
		item.screen = screenInDb + Workspace.getWorkspacePrefixScreenSize();

		mDesktopItems.add(item);
		switch (item.itemType) {
		case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
		case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
			HomeDesktopItemInfo homeInfo = (HomeDesktopItemInfo) item;
			final Shortcut shortcut = (Shortcut) createShortcut(homeInfo);
			if (homeInfo.intent != null
					&& this.containsNewComponent(homeInfo.intent.getComponent()) != null
					&& item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION)
				shortcut.showTipImage(IconTip.TIP_NEW);
			workspace.addInScreen(shortcut, item.screen, item.cellX,
					item.cellY, 1, 1, false);
			showView = shortcut;
			break;
		}

		workspace.requestLayout();
		return showView;
	}

	/**
	 * Bind the items start-end from the list.
	 * 
	 * Implementation of the method from LauncherModel.Callbacks.
	 */
	@Override
	public void bindItems(ArrayList<HomeItemInfo> shortcuts, int start, int end) {
	    bindItems(shortcuts, start, end, false);
	}
	
	/**
	 * bindItems
	 * @param shortcuts 
	 * @param start
	 * @param end
	 * @param isRelayout 调用是否来源于拖拽后的自动对齐
	 */
	public void bindItems(ArrayList<HomeItemInfo> shortcuts, int start, int end, boolean isRelayout) {
        setLoadOnResume();

        final Workspace workspace = mWorkspace;

        for (int i = start; i < end; i++) {
            final HomeItemInfo item = shortcuts.get(i);
            
            if (isRelayout) {
                if (item.equals(mWorkspace.getCellInfoAt(item.screen, item.cellX, item.cellY))) {
                    continue;
                } else {
                    mWorkspace.removeItems(new HomeItemInfoCoincidentComparator(item), false, false, isRelayout);
                }
            }
            
            if (item instanceof LauncherAppWidgetInfo) {
                bindAppWidget((LauncherAppWidgetInfo) item, isRelayout, isRelayout);
                continue;
            }
            if (item instanceof LauncherWidgetViewInfo) {
                bindWidgetView((LauncherWidgetViewInfo) item, isRelayout, isRelayout);
                continue;
            }
            if (item instanceof HomeDesktopItemInfo) {
                bindHomeDesktopItem((HomeDesktopItemInfo) item, isRelayout, isRelayout);
                continue;
            }
        }
        
        if (isRelayout) {
            mWorkspace.removeItems(new HomeItemInfoResidualComparator(shortcuts.get(shortcuts.size() - 1)), false, false, isRelayout);
        }

        workspace.requestLayout();
	}
	
	
	
	public void bindHomeDesktopItem(HomeDesktopItemInfo item, boolean forceUpdateToDb, boolean isRelayout) {
        HomeDesktopItemInfo homeInfo = (HomeDesktopItemInfo) item;
        final Shortcut shortcut = (Shortcut) createShortcut(homeInfo);
        mWorkspace.addInScreen(shortcut, item.screen, item.cellX, item.cellY, 1, 1, false);
        mDesktopItems.add(item);
	}

	/**
	 * Add the views for a widget to the workspace.
	 * 
	 * Implementation of the method from LauncherModel.Callbacks.
	 */
	@Override
	public void bindAppWidget(LauncherAppWidgetInfo item, boolean forceUpdateToDb) {
	    bindAppWidget(item, forceUpdateToDb, false);
	}
	    
    public void bindAppWidget(LauncherAppWidgetInfo item, boolean forceUpdateToDb, boolean isRelayout) {
		setLoadOnResume();
		
		final long start = LOGD_ENABLED ? SystemClock.uptimeMillis() : 0;
		if (LOGD_ENABLED) {
			XLog.d(TAG, "bindAppWidget: " + item);
		}
		final Workspace workspace = mWorkspace;

		final int appWidgetId = item.appWidgetId;
		final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
		if (LOGD_ENABLED) {
			XLog.d(TAG, "bindAppWidget: id=" + item.appWidgetId
					+ " belongs to component "
					+ (appWidgetInfo == null ? null : appWidgetInfo.provider));
		}

		if (appWidgetInfo == null) {
			removeAppWidget(item);
			DbManager.deleteItemFromDatabase(getApplicationContext(), item);
			return;
		}
		
		mDesktopItems.add(item);

		item.setHostView((LauncherAppWidgetHostView)mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo));
	    item.getHostView().setAppWidget(appWidgetId, appWidgetInfo);
		
		item.appWidgetCn = appWidgetInfo.provider;
        if (item.spanX == 2 && item.spanY == 3) {
            item.getHostView().setPadding(WorkspaceCellLayoutMeasure.widget2x3MarginLeft, 
                                          WorkspaceCellLayoutMeasure.widget2x3MarginTop, 
                                          WorkspaceCellLayoutMeasure.widget2x3MarginRight, 
                                          WorkspaceCellLayoutMeasure.widget2x3MarginBottom);
        }
		item.getHostView().setTag(item);
		
		try {// 华为荣耀3c添加系统天气widget时会在此处抛异常：java.lang.SecurityException:
				// Permission Denial: opening provider
				// com.huawei.android.totemweather.provider.WeatherProvider from
				// ProcessRecord{42483888 31824:android.process.acore/u0a154}
				// (pid=31824, uid=10154) requires
				// android.permission.ACCESS_WEATHERCLOCK_PROVIDER or
				// android.permission.ACCESS_WEATHERCLOCK_PROVIDER
			workspace.addInScreen(item.getHostView(), item.screen, item.cellX,
					item.cellY, item.spanX, item.spanY, false);

		} catch (Exception e) {
			if (LOGE_ENABLED) {
				XLog.e(TAG,
						"bind app widget error for its own error! try to catch this exception",
						e);
			}
			try {
				mWorkspace.removeInScreen(item.getHostView(), item.screen);
			} catch (Exception e2) {
				if (LOGE_ENABLED) {
					XLog.e(TAG,
							"bind app widget error for its own error! remove view exception",
							e2);
				}
			}

			removeAppWidget(item);
			DbManager.deleteItemFromDatabase(getApplicationContext(), item);
			ToastUtils.showMessage(
					getApplicationContext(),
					getString(R.string.widget_loaded_app_widget_failed,
							appWidgetInfo.label));

			return;
		}
		
		workspace.requestLayout();

		// finish load a widget, send it an intent
		if (Constant.ENABLE_WIDGET_SCROLLABLE && appWidgetInfo != null) {
			appwidgetReadyBroadcast(appWidgetId, appWidgetInfo.provider);
		}

		notifyAppWidgetResized(item.getHostView(), item.spanX, item.spanY);
		
		if (LOGD_ENABLED) {
			XLog.d(TAG, "bound widget id=" + item.appWidgetId + " in "
					+ (SystemClock.uptimeMillis() - start) + "ms");
		}

		if (forceUpdateToDb) {
			DbManager.addOrMoveItemInDatabase(getApplicationContext(), item,
					item.container, item.screen, item.cellX, item.cellY);
		}
	}

	@Override
	public void bindWidgetView(LauncherWidgetViewInfo item, boolean forceUpdateToDb) {
	    bindWidgetView(item, forceUpdateToDb, false);
	}
	    
    public void bindWidgetView(LauncherWidgetViewInfo item, boolean forceUpdateToDb, boolean isRelayout) {
		WidgetView widgetView = null;
		Widget widget = BuiltinWidgetMgr.get(this, item.widgetViewType);
		if (LOGD_ENABLED) {
			XLog.d(TAG, "intent is:" + item.intent + " widget is :" + widget
					+ " sdcardstate" + SdCardUtils.getExternalStorageState());
		}

		if (widget != null) {
			widgetView = widget.getWidgetView(this);
		}

		if (widgetView == null) {
			removeWidget(item);
			return;
		}
		widgetView.setWidgetContext(mWidgetContext);

		setLoadOnResume();

		final long start = LOGD_ENABLED ? SystemClock.uptimeMillis() : 0;
		if (LOGD_ENABLED) {
			XLog.d(TAG, "bindWidgetView: " + item);
		}

		item.hostView = widgetView;

		item.hostView.setTag(item);
		item.hostView.setOnLongClickListener(mWorkspace.mWorkspaceOnLongClickListener);
		item.hostView.init(item);

		if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
			mWorkspace.addInScreen(widgetView, item.screen, item.cellX,
					item.cellY, item.spanX, item.spanY, false);
		}
		widgetView.onAdded(false);
		if (mWorkspace.getCurrentScreen() == item.screen) {
			widgetView.screenIn();
		}
		mWidgetViews.add(widgetView);

		if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
			mDesktopItems.add(item);
			mWorkspace.requestLayout();
		} else {
			// do nothing
		}

		if (LOGD_ENABLED) {
			XLog.d(TAG, "bound widget view type=" + item.widgetViewType
					+ " in " + (SystemClock.uptimeMillis() - start) + "ms");
		}

		if (forceUpdateToDb) {
			DbManager.addOrMoveItemInDatabase(getApplicationContext(), item,
					item.container, item.screen, item.cellX, item.cellY);
		}
	}

	private void removeWidget(LauncherWidgetViewInfo info) {
		removeWidget(info, true);
	}

	private void removeWidget(LauncherWidgetViewInfo info, boolean removeInDb) {
		if (info == null) {
			return;
		}
		if (removeInDb) {
			DbManager.deleteItemFromDatabase(this, info);
		}
		WidgetView hostView = info.getHostView();
		if (hostView == null) {
			return;
		}
		ViewGroup parent = (ViewGroup) hostView.getParent();
		if (parent != null) {
			parent.removeView(hostView);
		}
		removeWidgetView(info);
	}

	/**
	 * Callback saying that there aren't any more items to bind.
	 * 
	 * Implementation of the method from LauncherModel.Callbacks.
	 */
	@Override
	public void finishBindingInHome() {
	    XLog.e("LSTAT", "Launcher.Launcher.finishBindingInHome nanoTime=" + String.format(Locale.getDefault(), "%.3fs", System.nanoTime() / 1000000000.0f));
	    
	    if (App.TIME_LAUNCHER_FINISHBIND == 0)  {
	        App.TIME_LAUNCHER_FINISHBIND = System.currentTimeMillis();
	    }
		setLoadOnResume();

		if (isWorkspaceVisible()) {
			mWorkspace.clearChildrenCache(true, false, false);
			mWorkspace.invalidate();
		} else {
			mWorkspace.clearChildrenCache(true, true, false);
		}

		mWorkspaceLoading = false;
		try {
            //这里预排加入一个标准的AppWidget
            //final ComponentName cn = new ComponentName("cc.snser.widgetdemo", "cc.snser.widgetdemo.PowerWidgetProvider");
		    final ComponentName cn = new ComponentName("com.caros.carcorder", "com.caros.carcorder.AppWidget");
            final int screen = Constants.APPWIDGET_RECORDER_SCREEN;
            final int cellx = Constants.APPWIDGET_RECORDER_CELLX_WIDESCREEN;
            final int celly = Constants.APPWIDGET_RECORDER_CELLY;
            if (mFirstStart) {
                addAppWidget(cn, screen, cellx, celly);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        mWorkspace.onFinishBindingInHome();
        
        App.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //CarOSLauncherBase.getInstance().initReport(getApplicationContext());
            }
        });
        
        sendBroadcast(new Intent(Constants.ACTION_LOADING_FINISHED));
	}

	private boolean mStatusbarExpanded;

	public void showStatusBar(boolean isChanged) {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		ResourceUtils.resetStatusBarHeight(this);
		CellLayout.refreshSmartTopPadding();
	}

	public void expandStatusbar() {
		if (LOGD_ENABLED) {
			XLog.d(TAG, "expand system statusBar");
		}
		try {
			Object service = this.getSystemService("statusbar");
			if (service != null) {
				Method expand = null;
				try {
					expand = service.getClass().getMethod("expand");
				} catch (Exception e) {
					// for 4.2
					expand = service.getClass().getMethod(
							"expandNotificationsPanel");
				}

				if (expand != null) {
					expand.invoke(service);
					mStatusbarExpanded = true;
					if (ScreenDimensUtils.isFullScreen(this)) {
						showStatusBar(false);
					}
				}
			}
		} catch (Throwable e) {
			if (LOGE_ENABLED) {
				XLog.e(TAG, "Failed", e);
			}
			ToastUtils.showMessage(this,
					R.string.open_notification_bar_failed_toast);
		}
	}

	public void requestFullScreen(boolean request) {
		if (!request || request == ScreenDimensUtils.isFullScreen(this)) {
			return;
		}

		if (request) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		CellLayout.refreshSmartTopPadding();
	}

	/**
	 * A package was installed.
	 * 
	 * Implementation of the method from LauncherModel.Callbacks.
	 */
	@Override
	public void bindAppsAdded(List<? extends HomeItemInfo> apps,
			boolean currentScreen, boolean clarify) {
		
		if(apps.size() == 1 && apps.get(0).itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT){
		}
		
		bindNewAppWithoutCategory(apps, currentScreen);
	}

	public void bindNewAppWithoutCategory(List<? extends HomeItemInfo> apps,
			boolean currentScreen) {
		for (HomeItemInfo app : apps) {
			boolean updateToDb = IphoneUtils.fillPosition(app, mWorkspace,
					false, currentScreen ? mWorkspace.getCurrentScreen() : -1);
			// 如果新增项需要在新建的屏幕中显示，并且新建屏幕个数已经达到上限了
			// if(app.screen >= MAX_SCREEN_NUMBER){
			// MaxScreenLimitedPolicy.getInsance().handleAppsAdded(apps);
			// }
			// else {
			addItem(app);

			if (updateToDb) {
				DbManager.addOrMoveItemInDatabase(this, app, app.container,
						app.screen, app.cellX, app.cellY);
			}
			// }
		}
	}
	
	public void bindShortcutRemoved(final List<HomeDesktopItemInfo> itemInfos) {
		HomeItemInfoRemovedComparator comparator = new HomeItemInfoRemovedComparator() {

			@Override
			public boolean isHomeItemInfoRemoved(HomeItemInfo itemInfo) {
				for (HomeDesktopItemInfo removedItemInfo : itemInfos) {
					if (removedItemInfo == itemInfo) {
						return true;
					}
				}
				return false;
			}
		};
		if (mWorkspace != null) {
			mWorkspace.removeItems(comparator);
		}
	}

	@Override
	public void bindAppsRemoved(List<? extends AppInfo> apps, boolean permanent) {
		if (permanent) {
			final HashSet<ComponentName> packageNames = new HashSet<ComponentName>();
			final int appCount = apps.size();
			for (int i = 0; i < appCount; i++) {
				if (apps.get(i).getIntent() == null
						|| apps.get(i).getIntent().getComponent() == null
						|| apps.get(i).getIntent().getComponent()
								.getPackageName() == null) {
					continue;
				}
				/*
				 * if (Constant.PACKAGE_NAME.equals(apps.get(i).getIntent().
				 * getComponent().getPackageName())) { continue; }
				 */
				packageNames.add(apps.get(i).getIntent().getComponent());
			}

			// TODO:下面待修改为List<? extends AppInfo> apps，避免误删
			HomeItemInfoRemovedComparator comparator = new HomeItemInfoRemovedComparator() {

				@Override
				public boolean isHomeItemInfoRemoved(HomeItemInfo itemInfo) {
					if (itemInfo instanceof HomeDesktopItemInfo) {

						final Intent intent = ((HomeDesktopItemInfo) itemInfo).intent;


						if (Intent.ACTION_MAIN.equals(intent.getAction())) {
							ComponentName targetPackageName = intent
									.getComponent() == null ? null : intent
											.getComponent();
							return packageNames.contains(targetPackageName);
						}
					} else if (itemInfo instanceof LauncherAppWidgetInfo) {
						final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) itemInfo;
						final AppWidgetProviderInfo provider = AppWidgetManager
								.getInstance(getApplicationContext()).getAppWidgetInfo(info.appWidgetId);
						if (provider != null) {
							for (ComponentName componentName : packageNames) {
								// TODO 是否要判断包是否还存在，而不仅仅是删除的item中包含该包名的iteminfo
								if (componentName.getPackageName().equals(
										provider.provider.getPackageName())) {
									return true;
								}
							}
						}else if(info.getHostView() != null){
							AppWidgetProviderInfo existProvider = info.getHostView().getAppWidgetInfo();
							if(existProvider != null && existProvider.provider != null){
								for (ComponentName componentName : packageNames) {
									// TODO 是否要判断包是否还存在，而不仅仅是删除的item中包含该包名的iteminfo
									if (componentName.getPackageName().equals(
											existProvider.provider.getPackageName())) {
										return true;
									}
								}
							}
						}
					}
					return false;
				}
			};
			if (mWorkspace != null) {
				mWorkspace.removeItems(comparator);
			}
		}
	}

	@Override
	public void bindAppsUpdated(List<? extends AppInfo> apps,
			Map<ComponentName, ComponentName> modifiedMapping) {
		if (mWorkspace != null) {
			mWorkspace.updateShortcuts(apps, modifiedMapping);
		}
	}

	@Override
	public void bindNewInstalledApps(List<? extends AppInfo> apps) {
		for (AppInfo app : apps) {
			if (LOGD_ENABLED) {
				XLog.d(TAG,
						"New component is added: "
								+ app.getIntent().getComponent()
								+ ", with last update time: "
								+ new Date(app.getLastUpdateTime()));
			}
			if (app.getCalledNum() == 0
					&& System.currentTimeMillis() <= app.getLastUpdateTime()
					+ Constant.DEFAULT_NEW_INSTALLED_APP_TIP_MILLISECONDS) {
				mNewComponents.put(app.getIntent().getComponent(),
						new NewComponentState(
								Constant.DEFAULT_NEW_INSTALLED_APP_TIP_MINUTE));

				if (mTimer == null) {
					mTimer = new Timer();
					mTimer.schedule(new NewInstalledAppTask(), 0, TIME_INTERVAL);
				}
			}
		}
	}

	public void removeNewComponent(ComponentName componentName) {
		if (componentName != null) {
			mNewComponents.remove(componentName);
		}
	}

	public NewComponentState containsNewComponent(ComponentName name) {
		return mNewComponents.get(name);
	}

	class NewInstalledAppTask extends TimerTask {
		@Override
		public void run() {
			reduce();
		}

		void reduce() {
			if (mNewComponents.isEmpty()) {
				if (mTimer != null) {
					mTimer.cancel();
					mTimer = null;
				}
				return;
			}
			synchronized (mNewComponents) {
				Iterator<ComponentName> iterator = mNewComponents.keySet()
						.iterator();
				while (iterator.hasNext()) {
					final ComponentName componentName = iterator.next();
					int time = mNewComponents.get(componentName).showTimeLeft;
					time--;
					mNewComponents.get(componentName).showTimeLeft = time;
					if (time <= 0) {
						iterator.remove();
					}
				}
			}
		}
	}

	/**
	 * 仅供launcherModel使用
	 */
	@Override
	public boolean isFirst() {
		return mFirstStart;
	}

	private void appwidgetReadyBroadcast(int appWidgetId, ComponentName cname) {
		Intent ready = new Intent(LauncherIntent.Action.ACTION_READY)
				.putExtra(LauncherIntent.Extra.EXTRA_APPWIDGET_ID, appWidgetId)
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
				.putExtra(LauncherIntent.Extra.EXTRA_API_VERSION,
						LauncherMetadata.CurrentAPIVersion).setComponent(cname);
		sendBroadcast(ready);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();

		releaseResources();
	}

	@Override
	public void onTrimMemory(int level) {
		if (level >= 40) {
			releaseResources();
		}
	}

	private void releaseResources() {
		// 系统整体内存不足，我们应该释放cache，减小内存占用，以后考虑这里做更多事情
		if (mWorkspace != null) {
			mWorkspace.clearChildrenCache(true, !this.isWorkspaceVisible(),
					true);
		}
		if (dragLayer != null) {
			dragLayer.clearCache();
		}
	}

	public Handler getHandler() {
		return mHandler;
	}

	public static class NewComponentState {
		public int showTimeLeft;

		public NewComponentState(int showTimeLeft) {
			this.showTimeLeft = showTimeLeft;
		}
	}

	/**
	 * 在UI线程上下文中调用
	 * 
	 * @param homeItemInfos
	 */
	@Override
	public void bindMissedItem(ArrayList<? extends HomeItemInfo> homeItemInfos) {
		if (LOGD_ENABLED) {
			XLog.d(TAG, "bind missed item");
		}
		List<HomeItemInfo> missedLargeWidgets = new ArrayList<HomeItemInfo>();
		if (homeItemInfos != null && homeItemInfos.size() > 0) {
			for (HomeItemInfo homeItemInfo : homeItemInfos) {
				if (LOGD_ENABLED) {
					XLog.d(TAG, "bind missed item:" + homeItemInfo);
				}

				if (homeItemInfo.spanX == 1 && homeItemInfo.spanY == 1) {
					if (IphoneUtils.fillPosition(homeItemInfo, mWorkspace,
							true, homeItemInfo.screen)) {
						if (homeItemInfo instanceof LauncherWidgetViewInfo) {
							bindWidgetView(
									(LauncherWidgetViewInfo) homeItemInfo, true);
						} else if (homeItemInfo instanceof LauncherAppWidgetInfo) {
							bindAppWidget((LauncherAppWidgetInfo) homeItemInfo,
									true);
						} else {
							if (homeItemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER) {
								continue;
							}
							addItem(homeItemInfo);
							DbManager.addOrMoveItemInDatabase(this,
									homeItemInfo, homeItemInfo.container,
									homeItemInfo.screen, homeItemInfo.cellX,
									homeItemInfo.cellY);
						}
					} else {
						XLog.e(TAG, "no position found for missed item:"
								+ homeItemInfo);
					}

				} else {
					missedLargeWidgets.add(homeItemInfo);
				}
			}

			if (missedLargeWidgets.size() == 0) {
				return;
			}

			for (HomeItemInfo homeItemInfo2 : missedLargeWidgets) {
				if (IphoneUtils.fillPosition(homeItemInfo2, mWorkspace, true,
						homeItemInfo2.screen)) {
					if (homeItemInfo2 instanceof LauncherWidgetViewInfo) {
						bindWidgetView((LauncherWidgetViewInfo) homeItemInfo2,
								true);
					} else if (homeItemInfo2 instanceof LauncherAppWidgetInfo) {
						bindAppWidget((LauncherAppWidgetInfo) homeItemInfo2,
								true);
					}
				} else {
					XLog.e(TAG, "no position found for missed item:"
							+ homeItemInfo2);
				}

			}
		}
	}

	@Override
	public void onFolderScreensAdded(ArrayList<Integer> screenIds) {
		Message msg = mHandler.obtainMessage(MSG_RELOAD_FOLDER_INDICATOR);
		msg.obj = screenIds;
		mHandler.sendMessage(msg);
	}

	public interface HomeItemInfoRemovedComparator {
		public boolean isHomeItemInfoRemoved(HomeItemInfo itemInfo);
	}
	
	public class HomeItemInfoCoincidentComparator implements HomeItemInfoRemovedComparator {
	    private HomeItemInfo mItemInfo;
	    
	    public HomeItemInfoCoincidentComparator(HomeItemInfo item) {
	        mItemInfo = item;
	    }

        @Override
        public boolean isHomeItemInfoRemoved(HomeItemInfo itemInfo) {
            return checkItemCoincidentX(mItemInfo, itemInfo);
        }
        
        private boolean checkItemCoincidentX(HomeItemInfo lhItem, HomeItemInfo rhItem) {
            if (lhItem.screen == rhItem.screen) {
                if (lhItem.id != rhItem.id || lhItem.cellX != rhItem.cellX || lhItem.spanX != rhItem.spanX) {
                    int sCoincidentX = Math.max(lhItem.cellX, rhItem.cellX);
                    int eCoincidentX = Math.min(lhItem.cellX + lhItem.spanX - 1, rhItem.cellX + rhItem.spanX - 1);
                    if (sCoincidentX <= eCoincidentX) {
                        String host = lhItem.getHostView().getClass().getName().replace("cc.snser.launcher.widget.", "");
                        String hostrh = rhItem.getHostView().getClass().getName().replace("cc.snser.launcher.widget.", "");
                        XLog.d(TAG, "relayoutWorkspace item=" + host + " scx=" + lhItem.cellX + " ecx=" + (lhItem.cellX + lhItem.spanX - 1) + " Coincident=" + hostrh);
                        return true;
                    }
                }
            }
            return false;
        }
	}
	
   public class HomeItemInfoResidualComparator implements HomeItemInfoRemovedComparator {
        private HomeItemInfo mItemInfo;
        
        public HomeItemInfoResidualComparator(HomeItemInfo item) {
            mItemInfo = item;
        }

        @Override
        public boolean isHomeItemInfoRemoved(HomeItemInfo itemInfo) {
            if (itemInfo.screen != mItemInfo.screen) {
                return itemInfo.screen > mItemInfo.screen;
            } else if (itemInfo.cellX != mItemInfo.cellX) {
                return itemInfo.cellX > mItemInfo.cellX;
            } else if (itemInfo.cellY != mItemInfo.cellY) {
                return itemInfo.cellY > mItemInfo.cellY;
            } else {
                return false;
            }
        }
    }
   
   public static class HomeItemInfoEqualComparator implements HomeItemInfoRemovedComparator {
       private HomeItemInfo mItemInfo;
       
       public HomeItemInfoEqualComparator(HomeItemInfo item) {
           mItemInfo = item;
       }

       @Override
       public boolean isHomeItemInfoRemoved(HomeItemInfo itemInfo) {
           return mItemInfo.equals(itemInfo);
       }
   }

	public interface MakeNewScreenCallback {
		void onMakeNewScreenFinish(int newScreenIndex);
	}

	public class MakeNewScreen {
		private int mTargetScreen = -1;
		private MakeNewScreenCallback mCallback;

		public MakeNewScreen(MakeNewScreenCallback callback) {
			mCallback = callback;
		}

		public int getTagetScreen() {
			return mTargetScreen;
		}

		public void stopAnimation() {
			if (mTargetScreen >= 0
					&& mTargetScreen < mWorkspace.getChildCount()) {
				mWorkspace.setCurrentScreen(mTargetScreen);
			}
		}

		public void start() {
			mWorkspace.addScreen(mWorkspace.getChildCount()
					- Workspace.getWorkspaceSuffixScreenSize());
			mTargetScreen = mWorkspace.getScreenCount() - 1
					- Workspace.getWorkspaceSuffixScreenSize();
			mWorkspace.scrollToScreen(mTargetScreen, true);
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					if (mWorkspace.isScrolling()) {
						mHandler.postDelayed(this, 50);
					} else {
						ToastUtils.showMessage(Launcher.this,
								R.string.homescreen_added_for_app_alert,
								Toast.LENGTH_SHORT);
						mWorkspace.setCurrentScreen(mTargetScreen);
						if (mCallback != null) {
							mCallback.onMakeNewScreenFinish(mTargetScreen);
						}
					}
				}
			}, 100);
		}
	}

	public boolean isVibrate() {
		try {
			if (Settings.System.getInt(getApplicationContext()
					.getContentResolver(), "haptic_feedback_enabled", 0) != 0) {
				return true;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public Vibrator getVibrator() {
		if (mVibrator == null) {
			mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		}
		return mVibrator;
	}

	public void vibrator() {
		Vibrator vibrator = getVibrator();
		if (vibrator != null && isVibrate()) {
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	private int[] mPendingPosition;

	public void addWidgetWithConfigure(ComponentName cn, int cellX, int cellY,
			int spanX, int spanY) {
		try {
			int widgetId = mAppWidgetHost.allocateAppWidgetId();
			if (AppWidgetUtils.bindAppWidgetIfAllowed(mAppWidgetManager, widgetId,
					cn)) {
				Intent intent = new Intent();
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
				mPendingPosition = new int[4];
				mPendingPosition[0] = cellX;
				mPendingPosition[1] = cellY;
				mPendingPosition[2] = spanX;
				mPendingPosition[3] = spanY;
				addWidgetWithConfigure(intent);
			}
		} catch (Exception e) {
			
		}
	}

	private void addWidgetWithConfigure(Intent data) {
		int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				-1);
		AppWidgetProviderInfo appWidget = mAppWidgetManager
				.getAppWidgetInfo(appWidgetId);

		if (appWidget != null && appWidget.configure != null) {
			// Launch over to configure widget, if needed
			Intent intent = new Intent(
					AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			intent.setComponent(appWidget.configure);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

			startActivityForResultSafely(intent,
					REQUEST_CREATE_APPWIDGET_FROM_WIDGETVIEW);
		}
	}

	private void completeAddAppWidget(Intent data) {
		if (mPendingPosition == null)
			return;

		int cellX = mPendingPosition[0];
		int cellY = mPendingPosition[1];

		int spanX = mPendingPosition[2];
		int spanY = mPendingPosition[3];

		mPendingPosition = null;
		Bundle extras = data.getExtras();
		int appWidgetId = extras
				.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		if (LOGD_ENABLED) {
			XLog.d(TAG, "dumping extras content=" + extras.toString());
		}

		AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
				.getAppWidgetInfo(appWidgetId);

		int screenIndex = mWorkspace.getCurrentScreen();
		CellLayout currentCellLayout = (CellLayout) mWorkspace
				.getChildAt(screenIndex);

		View view = currentCellLayout.getCellView(cellX, cellY);

		if (view instanceof WidgetView) {
			if (view.getTag() instanceof LauncherWidgetViewInfo) {
				removeWidget((LauncherWidgetViewInfo) view.getTag());
			}
		}

		// Try finding open space on Launcher screen

		// Build Launcher-specific widget info and save to database
		LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(
				appWidgetId);
		launcherInfo.spanX = spanX;
		launcherInfo.spanY = spanY;
		launcherInfo.appWidgetCn = data.getComponent();

		DbManager.addItemToDatabase(this, launcherInfo,
				LauncherSettings.Favorites.CONTAINER_DESKTOP,
				mWorkspace.getCurrentScreen(), cellX, cellY, false);

		mDesktopItems.add(launcherInfo);

		// Perform actual inflation because we're live
		launcherInfo.setHostView((LauncherAppWidgetHostView)mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo));
		launcherInfo.getHostView().setAppWidget(appWidgetId, appWidgetInfo);
        if (launcherInfo.spanX == 2 && launcherInfo.spanY == 3) {
            launcherInfo.getHostView().setPadding(WorkspaceCellLayoutMeasure.widget2x3MarginLeft, 
                                                  WorkspaceCellLayoutMeasure.widget2x3MarginTop, 
                                                  WorkspaceCellLayoutMeasure.widget2x3MarginRight, 
                                                  WorkspaceCellLayoutMeasure.widget2x3MarginBottom);
        }
		launcherInfo.getHostView().setTag(launcherInfo);

		try {// 华为荣耀3c添加系统天气widget时会在此处抛异常：java.lang.SecurityException:
				// Permission Denial: opening provider
				// com.huawei.android.totemweather.provider.WeatherProvider from
				// ProcessRecord{42483888 31824:android.process.acore/u0a154}
				// (pid=31824, uid=10154) requires
				// android.permission.ACCESS_WEATHERCLOCK_PROVIDER or
				// android.permission.ACCESS_WEATHERCLOCK_PROVIDER
			mWorkspace
					.addInCurrentScreen(launcherInfo.getHostView(), cellX, cellY,
							launcherInfo.spanX, launcherInfo.spanY,
							isWorkspaceLocked());
		} catch (Exception e) {
			if (LOGE_ENABLED) {
				XLog.e(TAG,
						"bind app widget error for its own error! try to catch this exception",
						e);
			}
			try {
				mWorkspace.removeInScreen(launcherInfo.getHostView(),
						mWorkspace.getCurrentScreen());
			} catch (Exception e2) {
				if (LOGE_ENABLED) {
					XLog.e(TAG,
							"bind app widget error for its own error! remove view exception",
							e2);
				}
			}

			removeAppWidget(launcherInfo);
			DbManager.deleteItemByIdImmediately(getApplicationContext(),
					launcherInfo.id);
			ToastUtils.showMessage(
					getApplicationContext(),
					getString(R.string.widget_add_app_widget_failed,
							appWidgetInfo.label));

			return;
		}

		// finish load a widget, send it an intent
		if (Constant.ENABLE_WIDGET_SCROLLABLE && appWidgetInfo != null) {
			appwidgetReadyBroadcast(appWidgetId, appWidgetInfo.provider);
		}

		if (LOGD_ENABLED) {
			XLog.d(TAG, "completeAddAppWidget ends.");
		}

		notifyAppWidgetResized(launcherInfo.getHostView(), launcherInfo.spanX, launcherInfo.spanY);

		if (mWorkspace.isInEditMode()) {
			mWorkspace.enableCurrentScreenCache();
		}
	}

	// 与app widget布局相关的方法
	@SuppressLint("NewApi")
	static int[] getSpanForWidget(Context context, ComponentName component,
			int minWidth, int minHeight) {
        //Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(context, component, null);
		Rect padding = new Rect(0, 0, 0, 0);
		// We want to account for the extra amount of padding that we are adding
		// to the widget
		// to ensure that it gets the full amount of space that it has requested
		int requiredWidth = minWidth + padding.left + padding.right;
		int requiredHeight = minHeight + padding.top + padding.bottom;
		return CellLayout.rectToCell(context.getResources(), requiredWidth,
				requiredHeight);
	}

	public static int[] getSpanForWidget(Context context,
			AppWidgetProviderInfo info) {
		return getSpanForWidget(context, info.provider, info.minWidth,
				info.minHeight);
	}

	public static int[] getMinSpanForWidget(Context context,
			AppWidgetProviderInfo info) {
		return getSpanForWidget(context, info.provider, info.minResizeWidth,
				info.minResizeHeight);
	}

	/*
	 * static int[] getSpanForWidget(Context context, CellLayout.CellInfo info)
	 * { return getSpanForWidget(context, info.componentName,
	 * info.info.minWidth, info.info.minHeight); }
	 * 
	 * static int[] getMinSpanForWidget(Context context, PendingAddWidgetInfo
	 * info) { return getSpanForWidget(context, info.componentName,
	 * info.info.minResizeWidth,info.info.minResizeHeight); }
	 */

	public int getCurrentScreens() {
		return mWorkspace.getChildCount();
	}

	public void addScreen() {
		mWorkspace.addScreen();
	}

	private void dispatchHomePressed() {
		int count = mWidgetViews.size();
		for (int i = 0; i < count; i++) {
			mWidgetViews.get(i).onHomePressed();
		}
	}

	public void onLocaleCfgChange() {
		mFlushAllAppsOnCreate = true;
	}
	
    protected Dialog mCurShownDlg;

    @Override
    public boolean isDialogShowing() {
        return mCurShownDlg != null && mCurShownDlg.isShowing();
    }

    @Override
    public void setShowingDlg(Dialog dlg){
        mCurShownDlg = dlg;
    }
    
    public void hideNavigation() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }
    
}