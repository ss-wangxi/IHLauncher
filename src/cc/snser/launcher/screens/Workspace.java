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

package cc.snser.launcher.screens;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Toast;
import cc.snser.launcher.AbstractWorkspace;
import cc.snser.launcher.CellLayout;
import cc.snser.launcher.Constant;
import cc.snser.launcher.IconCache;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.Utils;
import cc.snser.launcher.CellLayout.CellInfo;
import cc.snser.launcher.Launcher.HomeItemInfoRemovedComparator;
import cc.snser.launcher.apps.components.IconTip;
import cc.snser.launcher.apps.components.workspace.Shortcut;
import cc.snser.launcher.apps.model.AppInfo;
import cc.snser.launcher.apps.model.ItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.apps.model.workspace.LauncherAppWidgetInfo;
import cc.snser.launcher.apps.model.workspace.LauncherWidgetViewInfo;
import cc.snser.launcher.iphone.model.LauncherModelIphone;
import cc.snser.launcher.model.DbManager;
import cc.snser.launcher.style.SettingPreferences;
import cc.snser.launcher.support.settings.GestureSettings;
import cc.snser.launcher.support.settings.GestureType;
import cc.snser.launcher.ui.components.ScreenIndicator;
import cc.snser.launcher.ui.components.pagedsv.PagedScrollStrategy;
import cc.snser.launcher.ui.dragdrop.DragController;
import cc.snser.launcher.ui.dragdrop.DragSource;
import cc.snser.launcher.ui.dragdrop.DragView;
import cc.snser.launcher.ui.dragdrop.DropTarget;
import cc.snser.launcher.ui.dragdrop.DragController.DragListener;
import cc.snser.launcher.util.Alarm;
import cc.snser.launcher.util.OnAlarmListener;
import cc.snser.launcher.widget.IScreenCtrlWidget;
import cc.snser.launcher.widget.IWorkspaceContext;
import cc.snser.launcher.widget.Widget;
import cc.snser.launcher.widget.WidgetView;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.util.ToastUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static cc.snser.launcher.Constant.LOGD_ENABLED;
import static cc.snser.launcher.Constant.LOGE_ENABLED;

/**
 * The workspace is a wide area with a wallpaper and a finite number of screens.
 * Each screen contains a number of icons, folders or widgets the user can
 * interact with. A workspace is meant to be used with a fixed width only.
 */
public class Workspace extends AbstractWorkspace implements DropTarget,
DragSource, DragListener, IWorkspaceContext{
	public static final int OLD_DEFAULT_SCREEN_NUMBER = 1;
	public static final int OLD_DEFAULT_DEFAULT_SCREEN = 0;
	public static final int NEW_DEFAULT_SCREEN_NUMBER = 0;
	public static final int NEW_DEFAULT_DEFAULT_SCREEN = 0;

	private static final float EDIT_MODE_SCALE_RADIO_WITH_DOCKBAR_IN_IPHONE = 0.722F;

	public static float sEditModeScaleRatio = EDIT_MODE_SCALE_RADIO_WITH_DOCKBAR_IN_IPHONE;

	public static boolean sInEditMode;

	public static final String TAG = "CarOS.Workspace";

	/**
	* CellInfo for the cell that is currently being dragged
	*/
	private CellLayout.CellInfo mDragInfo;

	private int mDragScreenIndex = -1;
	private int mDragScreenIndexHolder = -1; // 存放拖动屏幕一开始时，屏幕的序列
	private boolean mFirstResume = true;

	private float mWallpaperOffsetX;
	private float mWallpaperOffsetY;



	/**
	* Target drop area calculated during last acceptDrop call.
	*/
	private int[] mTargetCell = null;

	private final IconCache mIconCache;

	private final Paint mPaint;
	private final WorkspaceShadowHelper mShadowHelper;

	private static final int REORDER_TIMEOUT = 250;
	private final Alarm mReorderAlarm = new Alarm();

	private boolean mDragStart;

	private boolean mbEnableLongClick = false;

	private boolean mbAddScreenBySecondlayer = false;

	private final LastClickInfo mWorkspaceClickInfo = new LastClickInfo();

	CellLayout m_CurrentScreen = null;
	
	//跨屏拖拽到满屏时，满屏中被删除的Item
	private ArrayList<HomeItemInfo> mDragScrollRemovedItemInfos = new ArrayList<HomeItemInfo>();
	
	private class LastClickInfo {
		private int cellX = -1;
		private int cellY = -1;
		private long clickTime;

		public boolean acceptDoubleClick(int cellX, int cellY, long clickTime) {
			long interval = clickTime - this.clickTime;
			if (this.cellX >= 0 && this.cellY >= 0
					&& Math.abs(this.cellX - cellX) <= 1
					&& Math.abs(this.cellY - cellY) <= 1 && interval >= 0
					&& interval <= 300) {
				this.cellX = -1;
				this.cellY = -1;
				this.clickTime = clickTime;
				return true;
			} else {
				if (interval < 300) {
					return false;
				}
				this.cellX = cellX;
				this.cellY = cellY;
				this.clickTime = clickTime;
				return false;
			}
		}
	}

	/**
	* TODO: 暂时设计为public，供其它类使用
	*/
	public View.OnClickListener mWorkspaceOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			XLog.d("Laucher WorkSpace Click", "Laucher WorkSpace Click============");
			handleClick(v);
		}
	};

	/**
	* TODO：暂时设计为public，供其它类使用
	*/
	public View.OnLongClickListener mWorkspaceOnLongClickListener = new View.OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			if (false == mbEnableLongClick ) {
				return handleLongClick(v);
			}
			else {
				return false;
			}
		}
	};

	public void disableLongClick(){
		mbEnableLongClick  = true;
	}

	public void enableLongClick(){
		mbEnableLongClick  = false;
	}

	/**
	* Used to inflate the Workspace from XML.
	*
	* @param context
	*            The application's context.
	* @param attrs
	*            The attributes set containing the Workspace's customization
	*            values.
	*/
	public Workspace(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	* Used to inflate the Workspace from XML.
	*
	* @param context
	*            The application's context.
	* @param attrs
	*            The attributes set containing the Workspace's customization
	*            values.
	* @param defStyle
	*            Unused.
	*/
	public Workspace(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mIconCache = IconCache.getInstance(context);

		this.mPaint = new Paint();
		this.mPaint.setColor(0);

		mShadowHelper = new WorkspaceShadowHelper(this, getContext());
		initScreens();

		setCanLoopScreen(SettingPreferences.isLoopHomeScreen());
	}

	public float getWallpaperOffsetX(){
		return mWallpaperOffsetX;
	}

	public float getWallpaperOffsetY(){
		return mWallpaperOffsetY;
	}

	@Override
	protected void onFinishInflate() {
		this.setOnClickListener(this.mWorkspaceOnClickListener);
		this.setOnLongClickListener(this.mWorkspaceOnLongClickListener);
	}

	private void handleClick(View v) {
		if (isInEditMode()) {
			while (v != null && !(v instanceof CellLayout)) {
				v = (View) v.getParent();
			}
			if (v instanceof CellLayout) {
				CellLayout.CellInfo cellInfo = (CellLayout.CellInfo) v.getTag();
				if (cellInfo == null) {
					return;
				}
			}
			return;
		}

		Object tag = v.getTag();
		if (tag instanceof HomeDesktopItemInfo) {
			mLauncher.handleShortcutInfoClick(v, (HomeDesktopItemInfo) tag);
		} else {
			if (tag instanceof CellInfo) {
				CellInfo cellInfo = (CellInfo) tag;
				if (mWorkspaceClickInfo.acceptDoubleClick(cellInfo.cellX,
						cellInfo.cellY, System.currentTimeMillis())) {
					fireDoubleClickAction();
				}
			}
		}
	}

	private boolean handleLongClick(View v) {
		if (mLauncher.isPaused()) {
			return false;
		}

		if (mLauncher.isWorkspaceLocked()) {
			return false;
		}
		
		if (this.allowLongPress()) {
			while (v != null && !(v instanceof CellLayout)) {
				v = (View) v.getParent();
			}

			CellLayout.CellInfo cellInfo = (CellLayout.CellInfo) v.getTag();

			// This happens when long clicking an item with the dpad/trackball
			if (cellInfo == null) {
				return true;
			}
			resetScreenVerticalScroller();

			if(!mDragController.isDragging()){
				startDrag(cellInfo);	
			}
		}

		return true;
	}

	public boolean onBackPressed() {
		CellLayout cellLayout = (CellLayout) this.getChildAt(this
				.getCurrentScreen());

		if (cellLayout != null) {
			int count = cellLayout.getChildCount();

			for (int i = 0; i < count; i++) {
				View child = cellLayout.getChildAt(i);
				if (child instanceof WidgetView) {
					if (((WidgetView) child).onBackPressed()) {
						return true;
					}
				}
			}
		}

		if (this.getCurrentScreen() != this.getDefaultScreen()) {
			scrollToScreen(this.getDefaultScreen());
			return true;
		}

		return false;
	}

	static int sWorkspaceSuffixScreenSize;

	// 用来表示workspace的屏幕中，前面有几屏是非内容区，在EDIT模式下，此值会变成1
	static int sWorkspacePrefixScreenSize;

	public static int getWorkspacePrefixScreenSize() {
		return sWorkspacePrefixScreenSize;
	}

	public static int getWorkspaceSuffixScreenSize() {
		return sWorkspaceSuffixScreenSize;
	}

	private void initScreens() {
		// get the screen number
		int screenNumber;

		screenNumber = SettingPreferences.getScreenNumber(OLD_DEFAULT_SCREEN_NUMBER);

		if (LOGD_ENABLED) {
			XLog.d(TAG, screenNumber + " screens is building.");
		}

		for (int i = 0; i < screenNumber; i++) {
			CellLayout screen = (CellLayout) LayoutInflater.from(getContext()).inflate(R.layout.workspace_screen, this, false);
			addView(screen);
		}

		// get the default screen
		int defaultScreen;

		defaultScreen = SettingPreferences.getHomeScreen(OLD_DEFAULT_DEFAULT_SCREEN);
		if (defaultScreen < 0 || defaultScreen >= screenNumber) {
			defaultScreen = 0;
		}

		sWorkspacePrefixScreenSize = 0;

		mDefaultScreen = defaultScreen;
		mCurrentScreen = mDefaultScreen;
		if (LOGD_ENABLED) {
			XLog.d(TAG, "Workspace mCurrentScreen = " + mCurrentScreen);
		}
	}

	@Override
	protected boolean canEnableDrawCache() {
		return super.canEnableDrawCache() || this.isInEditMode();
	}

	@Override
	public void scrollTo(int x, int y) {
		if (getScrollX() != x || getScrollY() != y) {
		    //XLog.d(TAG, "scrollTo x=" + x + " y=" + y);
			super.scrollTo(x, y);
			handleWidgetScroll(x, y);
			handleOverScroll(x, y);
		}
	}
	
	/**
	* 只为{@link PagedScrollStrategy} 使用
	*/
	@Override
	protected void setPagedScrollX(int scrollX) {
	    //XLog.d(TAG, "setPagedScrollX scrollX=" + scrollX);
		super.setPagedScrollX(scrollX);
	}

	@Override
	public boolean onInterceptTouchEvent(final MotionEvent ev) {
		if (!isEnabled()) {
			return true;
		}
		return super.onInterceptTouchEvent(ev);
	}
	
	private boolean handleGesture() {
		return !this.isInEditMode();
	}

	@SuppressLint("ClickableViewAccessibility")
    @Override
	public boolean onTouchEvent(final MotionEvent ev) {
		if (!isEnabled()) {
			return true;
		}
		handleSecondFingerGusture(ev);
		return super.onTouchEvent(ev);
	}

	@Override
	public void fireSwipeDownAction() {
		if (handleGesture()) {
			GestureSettings gestureSettings = SettingPreferences
					.getWorkspaceGestureDownAction(this.getContext());
			gestureSettings.fireAction(GestureType.DOWN, mLauncher);
		}
	}

	@Override
	public void fireSwipeUpAction() {
		if (handleGesture()) {
			GestureSettings gestureSettings = SettingPreferences
					.getWorkspaceGestureUpAction(this.getContext());
			gestureSettings.fireAction(GestureType.UP, mLauncher);
		}
	}

	@Override
	public void fireDoubleClickAction() {
		// 此版本不响应 doubleclick 手势
		/*
		* if (handleGesture()) { GestureSettings gestureSettings =
		* LauncherSettings
		* .getWorkspaceGestureDoubleClickAction(this.getContext());
		* gestureSettings.fireAction(GestureType.DOUBLE_CLICK, mLauncher); }
		*/
	}


	void drawChild(Canvas canvas, int childIndex, long drawingTime) {
		int childCount = this.getChildCount();
		if (childIndex < 0 || childIndex >= childCount) {
			childIndex = (childIndex + childCount) % childCount;
		}
		drawChild(canvas, getChildAt(childIndex), drawingTime);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (!isEnabled()) {
			return true;
		}
		return super.dispatchTouchEvent(ev);
	}

	@Override
	public void loadScreenTransitionType() {
		setScreenTransitionType(SettingPreferences
				.getHomeScreenTransformationType());
	}

	@Override
	public boolean isLoopWallpaper() {
		return false;
		/*return this.getChildCount() > 1
                && !WallpaperUtils.isCachedWallpaperSingleScreen(mContext);*/
	}

	/**
	* XXX Consider refactor {@link #setOnClickListener(OnClickListener)},
	* {@link #setOnLongClickListener(OnLongClickListener)} and
	* {@link #buildScreens()} methods to made this method be the only entrance
	* to add screen.
	*/
	@Override
	public void addScreen() {
		addScreen(-1);
	}

	public void addScreen(int screenIndex) {
		addScreen(screenIndex, true);
	}


	/**
	* 增加屏幕，执行如下三个方面动作：数据结构、存储、视图和UI
	* 
	* @param screenIndex
	*            -1表示添加在末尾
	*/

	private void addScreen(int screenIndex, boolean updateData) {
		createScreen(screenIndex, updateData);
		addToScreenIndicator(screenIndex);
	}

	public void addToScreenIndicator(int screenIndex){
		mScreenIndicator.addScreen(screenIndex,
				ScreenIndicator.INDICATOR_SCENE_HOME, ScreenIndicator.NORMAL_INDICATOR);
	}

	public CellLayout createScreen(int screenIndex, boolean updateData){

		int screenCount = getChildCount();
		if (screenIndex < 0) {
			screenIndex = screenCount;
		} else {
			// Correct current screen
			if (screenIndex <= mCurrentScreen) {
				setCurrentScreen(mCurrentScreen + 1);
			}

			increaseScreenCountForHomeItemInfos(screenIndex, screenCount - 1, 1);
			DbManager.increaseItemsScreenCountFromDatabase(this.getContext(),screenIndex, true);
		}

		CellLayout screen = (CellLayout) LayoutInflater.from(getContext()).inflate(R.layout.workspace_screen, this, false);

		if (sInEditMode && screen instanceof WorkspaceCellLayout) {
			WorkspaceCellLayout workspacescren = (WorkspaceCellLayout) screen;
			workspacescren.setLayoutStatus(WorkspaceCellLayout.EDIT);
		}

		// Bind events
		screen.setOnClickListener(this.mWorkspaceOnClickListener);
		screen.setOnLongClickListener(this.mWorkspaceOnLongClickListener);

		addView(screen, screenIndex);

		if (updateData) {
			// update folder screens
			updateScreenIndex(screenIndex);

			// Store screen number
			saveScreenNumber();
		}
		return screen;
	}


	void setScreenIndexForHomeItemInfos(WorkspaceCellLayout layout, int index) {
		if (layout == null)
			return;

		int childCount = layout.getChildCount();
		for (int j = 0; j < childCount; j++) {
			final View view = layout.getChildAt(j);
			Object tag = view.getTag();
			if (tag instanceof HomeItemInfo) {
				HomeItemInfo itemInfo = (HomeItemInfo) tag;
				itemInfo.screen = index;
			}
		}

		layout.setScreenIndex(index);
	}

	void increaseScreenCountForHomeItemInfos(int startScreenIndex,
			int toScreenIndex, int diff) {
		if (diff == 0) {
			return;
		}

		for (int i = startScreenIndex; i <= toScreenIndex; i++) {
			final CellLayout layout = (CellLayout) getChildAt(i);

			int childCount = layout.getChildCount();
			for (int j = 0; j < childCount; j++) {
				final View view = layout.getChildAt(j);
				Object tag = view.getTag();

				if (tag instanceof HomeItemInfo) {
					HomeItemInfo itemInfo = (HomeItemInfo) tag;
					itemInfo.screen += diff;
				}
			}
		}
	}

	public boolean isAnyAppInScreen(int screenIndex) {
		CellLayout screen = (CellLayout) getChildAt(screenIndex);

		final int count = screen.getChildCount();

		if (count == 0) {
			return false;
		}

		for (int i = 0; i < count; i++) {
			CellLayout.LayoutParams lp = (CellLayout.LayoutParams) screen
					.getChildAt(i).getLayoutParams();
			if (!lp.isDragging) {
				return true;
			}
		}

		return false;
	}

	public void removeScreen(CellLayout layout) {
		if (layout == null) return;
		int index = indexOfChild(layout);
		removeScreen(index);
	}

	public void removeScreen(CellLayout layout,boolean showAnima) {
		int index = indexOfChild(layout);
		if(showAnima){
			//TODO:动画,先缩回去，再moveToSceen
			if(index > 0){
				getChildAt(index-1).setTranslationX(0);
			}
		if(index < getChildCount()-1){
			getChildAt(index+1).setTranslationX(0);
		}
		}
		removeScreen(layout);

	}

	@Override
	public boolean removeScreen(int screenIndex) {
		return removeScreen(screenIndex, true);
	}

	public boolean removeEmptyScreen(int screenIndex) {
		return removeEmptyScreen(screenIndex, true);
	}

	@Override
	public  boolean removeAllScreens(){
		mScreenIndicator.clear();
		/*for(int i = 0; i < getChildCount(); i ++){
    		clearItemsOnScreen(i);
    	}*/
		removeAllViews();
		addScreen(-1, false);
		setCurrentScreen(0);
		//saveScreenNumber();
		return true;
	}

	private boolean removeEmptyScreen(int screenIndex, boolean updateData){
		return removeScreenInternal(screenIndex, updateData, false);
	}

	private boolean removeScreenInternal(int screenIndex, boolean updateData, boolean removeContent){
		mScreenIndicator.removeScreen(screenIndex);

		// Correct current screen
		if (screenIndex <= mCurrentScreen && mCurrentScreen > 0) {
			setCurrentScreen(mCurrentScreen - 1);
		} else if (isScrolling() && screenIndex == mNextScreen) {
			setCurrentScreen(mCurrentScreen);
		} else {
			mScreenIndicator.snapToScreen(mCurrentScreen);
		}

		if(mDefaultScreen > screenIndex){
			setHomeScreen(mDefaultScreen-1);
		}else if(mDefaultScreen == screenIndex){
			int totalScreen = getChildCount() - sWorkspaceSuffixScreenSize;
			setHomeScreen(mDefaultScreen == totalScreen - 1 ? mDefaultScreen - 1 : mDefaultScreen);
			invalidate();
		}

		int screenCount = getChildCount();
		increaseScreenCountForHomeItemInfos(screenIndex + 1, screenCount - 1, -1);

		if(removeContent){
			DbManager.deleteItemsInScreenFromDatabase(this.getContext(),
					screenIndex);/// 注意调用顺序及内部实现是否能够真正串行，避免数据出错
		}
		DbManager.increaseItemsScreenCountFromDatabase(this.getContext(),
				screenIndex + 1, false);

		clearItemsOnScreen(screenIndex);
		removeViewAt(screenIndex);

		// update folder screens
		if (updateData) {
			updateScreenIndex(screenIndex);
		}

		// Store screen number
		saveScreenNumber();

		return true;
	}

	private boolean removeScreen(int screenIndex, boolean updateData) {    
		return removeScreenInternal(screenIndex, updateData, true);
	}

	public void moveScreen(int srcScreenIndex, int targetScreenIndex) {
		moveScreen(srcScreenIndex, targetScreenIndex, true);
	}

	public void moveScreen(int srcScreenIndex, int targetScreenIndex,
			boolean swapView) {
		if (srcScreenIndex == targetScreenIndex) {
			return;
		}
		// Correct current screen
		int currentScreen = getCurrentScreen();
		int defaultScreen = mDefaultScreen;

		if (srcScreenIndex < targetScreenIndex) {
			if (currentScreen == srcScreenIndex) {
				currentScreen = targetScreenIndex;
			} else if (currentScreen >= Math.min(srcScreenIndex,
					targetScreenIndex)
					&& currentScreen <= Math.max(srcScreenIndex,
							targetScreenIndex)) {
				currentScreen--;
			}
			if (defaultScreen == srcScreenIndex) {
				defaultScreen = targetScreenIndex;
			} else if (defaultScreen >= Math.min(srcScreenIndex,
					targetScreenIndex)
					&& defaultScreen <= Math.max(srcScreenIndex,
							targetScreenIndex)) {
				defaultScreen--;
			}

			increaseScreenCountForHomeItemInfos(srcScreenIndex + 1,
					targetScreenIndex, -1);
		}

		if (srcScreenIndex > targetScreenIndex) {
			if (currentScreen == srcScreenIndex) {
				currentScreen = targetScreenIndex;
			} else if (currentScreen >= Math.min(srcScreenIndex,
					targetScreenIndex)
					&& currentScreen <= Math.max(srcScreenIndex,
							targetScreenIndex)) {
				currentScreen++;
			}
			if (defaultScreen == srcScreenIndex) {
				defaultScreen = targetScreenIndex;
			} else if (defaultScreen >= Math.min(srcScreenIndex,
					targetScreenIndex)
					&& defaultScreen <= Math.max(srcScreenIndex,
							targetScreenIndex)) {
				defaultScreen++;
			}

			increaseScreenCountForHomeItemInfos(targetScreenIndex,
					srcScreenIndex - 1, 1);
		}

		increaseScreenCountForHomeItemInfos(srcScreenIndex, srcScreenIndex,
				targetScreenIndex - srcScreenIndex);

		DbManager.moveItemsInScreenFromDatabase(this.getContext(),
				srcScreenIndex, targetScreenIndex);

		setCurrentScreen(currentScreen);

		if (swapView) {
			View view = getChildAt(srcScreenIndex);
			removeViewAt(srcScreenIndex);
			addView(view, targetScreenIndex);
		}

		// Store screen number
		saveScreenNumber();
	}

	public void saveScreenNumber() {
		int screenNumber = getChildCount() - getWorkspacePrefixScreenSize()
				- getWorkspaceSuffixScreenSize();
		SettingPreferences.setScreenNumber(screenNumber);
	}

	public int getScreenNumber() {
		return getChildCount() - getWorkspacePrefixScreenSize()
				- getWorkspaceSuffixScreenSize();
	}

	private List<View> getCellsOfScreen(int screenIndex) {
		CellLayout screen = (CellLayout) getChildAt(screenIndex);

		List<View> cells = new ArrayList<View>();
		if (screen != null) {
			int childCount = screen.getChildCount();
			for (int i = 0; i < childCount; i++) {
				View cell = screen.getChildAt(i);
				cells.add(cell);
			}
		}
		return cells;
	}
	
	public View getCellAt(int screen, int cellX, int cellY) {
	    final CellLayout cellLayout = (CellLayout)getChildAt(screen);
	    if (cellLayout != null) {
	        return cellLayout.getCellView(cellX, cellY);
	    } else {
	        return null;
	    }
	}
	
	public HomeItemInfo getCellInfoAt(int screen, int cellX, int cellY) {
	    final View view = getCellAt(screen, cellX, cellY);
	    if (view != null && view.getTag() instanceof HomeItemInfo) {
	        return (HomeItemInfo)view.getTag();
	    } else {
	        return null;
	    }
	}

	private void clearItemsOnScreen(int screenIndex) {
		List<View> cells = getCellsOfScreen(screenIndex);

		for (View cell : cells) {
			if (cell instanceof DropTarget) {
				mDragController.removeDropTarget((DropTarget) cell);
			}

			HomeItemInfo item = (HomeItemInfo) cell.getTag();
			if (item != null) {
				CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell
						.getLayoutParams();
				if (lp == null || !lp.isDragging) {
					removeItem(item);
				} else {
					item.id = HomeItemInfo.NO_ID;
				}
			}
		}
	}

	public void removeItem(HomeItemInfo item) {
		// XXX The logic below is copied from
		// cc.snser.launcher.screens.DeleteZone.onDrop, may refactor
		// to reuse one day
		if (item instanceof HomeDesktopItemInfo) {
			mLauncher.removeItem((HomeDesktopItemInfo) item, true);
		} else if (item instanceof LauncherWidgetViewInfo) {
			mLauncher.removeWidgetView((LauncherWidgetViewInfo) item);
		} else if (item instanceof LauncherAppWidgetInfo) {
			mLauncher.removeAppWidget((LauncherAppWidgetInfo) item);
		}
	}

	public int getDefaultScreen() {
		return mDefaultScreen;
	}

	public void setDefaultScreen(int defaultScreen){
		mDefaultScreen = defaultScreen;

	}

	public void refreshHomeScreen(int defaultIndex){
		final int index = defaultIndex;

		post(new Runnable() {
			@Override
			public void run() {
				mDefaultScreen = index;
				SettingPreferences.setHomeScreen(index);
				//交换
				mLauncher.getIndicator().setHomeScreen(mDefaultScreen);
			}
		});
	}

	/**
	* Find vacant area on all screens.
	* <p>
	* The search will start from specified screen, then the two screens beside
	* of it, then the one more far, and so on...
	* </p>
	* 
	* @param screenIndex
	*            Screen index to search start from
	* @param spanX
	*            The vacant span on X coordinate
	* @param spanY
	*            The vacant span on Y coordinate
	* @return The left-top position of vacant area and screen index if found,
	*         or <code>null</code> if not found
	*/
	public int[] findVacantArea(int screenIndex, boolean includeCurrentScreen,
			int spanX, int spanY) {
		int childCount = getChildCount();
		if (screenIndex < 0) {
			screenIndex = 0;
		} else if (screenIndex >= childCount) {
			screenIndex = childCount - 1;
		}

		int[] ret = new int[3];

		// Check start screen first
		if (includeCurrentScreen
				&& findVacantAreaAt(screenIndex, spanX, spanY, ret)) {
			return ret;
		}

		// Check other screens
		int offset = Math.max(childCount - 1 - screenIndex, screenIndex);
		for (int i = 1; i <= offset; i++) {
			if (findVacantAreaAt(screenIndex + i, spanX, spanY, ret)) {
				return ret;
			}
			if (findVacantAreaAt(screenIndex - i, spanX, spanY, ret)) {
				return ret;
			}
		}
		return null;
	}

	public int[] findVacantAreaFromFirst(int screenIndex, boolean includeCurrentScreen,
			boolean includeExportedScreen, int spanX, int spanY) {
		int childCount = getChildCount();
		if (screenIndex < 0) {
			screenIndex = 0;
		} else if (screenIndex >= childCount) {
			screenIndex = childCount - 1;
		}

		int[] ret = new int[3];

		// Check start screen first
		if (includeCurrentScreen && includeExportedScreen &&
				findVacantAreaAt(screenIndex, spanX, spanY, ret)) {
			return ret;
		}

		// Check other screens from first screen
		for (int i = 0; i < childCount; i++) {
			if (i == screenIndex) {
				continue;
			}
			if (includeExportedScreen && findVacantAreaAt(i, spanX, spanY, ret)) {
				return ret;
			}
		}
		return null;
	}

	public int[] findVacantAreaFromNext(int screenIndex, boolean includeCurrentScreen,
			boolean includeExportedScreen, int spanX, int spanY) {
		int childCount = getChildCount();
		if (screenIndex < 0) {
			screenIndex = 0;
		} else if (screenIndex >= childCount) {
			screenIndex = childCount - 1;
		}

		int[] ret = new int[3];

		// Check start screen first
		if (includeCurrentScreen && includeExportedScreen && findVacantAreaAt(screenIndex, spanX, spanY, ret)) {
			return ret;
		}

		// Check other screens from right screen
		for (int i = screenIndex+1; i < childCount; i++) {
			if (includeExportedScreen  &&
					findVacantAreaAt(i, spanX, spanY, ret)) {
				return ret;
			}
		}
		return null;
	}

	private boolean findVacantAreaAt(int screenIndex, int spanX, int spanY,
			int[] ret) {
		if (screenIndex >= getChildCount() || screenIndex < 0) {
			return false;
		}
		CellLayout screen = (CellLayout) getChildAt(screenIndex);

		if (screen.findCellForSpan(ret, spanX, spanY)) {
			ret[2] = screenIndex;
			return true;
		}
		return false;
	}

	public void startDrag(CellLayout.CellInfo cellInfo) {
		View child = cellInfo.getCell();

		// Make sure the drag was started by a long press as opposed to a long
		// click.
		if (child == null || !child.isInTouchMode()) {
			return;
		}

		mDragInfo = cellInfo;
		mDragInfo.screen = mCurrentScreen;

		CellLayout current = ((CellLayout) getChildAt(mCurrentScreen));

		current.onDragChild(child);
		mDragController.startDrag(child, this, child.getTag(),DragController.DRAG_ACTION_MOVE, true, true);
		invalidate();
	}

	public boolean isDragScreen() {
		return isInEditMode() && mDragInfo == null && mDragScreenIndex >= 0;
	}

	/**
	* 开始拖动第 screen 屏，
	* 
	* @param screen
	*/
	public void startDragScreen(CellLayout.CellInfo cellInfo) {
		if (!isInEditMode())
			return;

		View child = cellInfo.getCell();
		if (child != null && !child.isInTouchMode()) {
			return;
		}

		mDragScreenIndex = cellInfo.screen + getWorkspacePrefixScreenSize();
		mDragScreenIndexHolder = mDragScreenIndex;
		if (mDragScreenIndex >= getChildCount()) {
			return;
		}

		if (mDragScreenIndex != getCurrentScreen()) {
			setCurrentScreen(mDragScreenIndex);
		}

		CellLayout dragScreen = ((CellLayout) getChildAt(mDragScreenIndex));
		if (!(dragScreen instanceof WorkspaceCellLayout)) {
			return;
		}

		mDragController.startDrag(dragScreen, this, dragScreen,
				DragController.DRAG_ACTION_COPY, true, false);
		((WorkspaceCellLayout) dragScreen).setLayoutStatus(WorkspaceCellLayout.DRAGING);

		getWidth();
		invalidate();
	}

	@Override
	public void setDragController(DragController dragController) {
		super.setDragController(dragController);
		if (dragController != null) {
			dragController.setCreateOutline(mShadowHelper);
		}
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final SavedState state = new SavedState(super.onSaveInstanceState());
		state.currentScreen = mCurrentScreen;
		return state;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		SavedState savedState = (SavedState) state;
		super.onRestoreInstanceState(savedState.getSuperState());
		if (savedState.currentScreen != -1) {
			mCurrentScreen = savedState.currentScreen;
			if (LOGD_ENABLED) {
				XLog.d(TAG, "onRestoreInstanceState mCurrentScreen = "
						+ mCurrentScreen);
			}
		}
	}
	
	@Override
	public void onStart() {
	    super.onStart();
	    XLog.d(TAG, "Workspace onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
		XLog.d(TAG, "Workspace onResume");
		setCanLoopScreen(SettingPreferences.isLoopHomeScreen());
		handleWidgetScreenEvent();
	}
	
	@Override
	public void onPause() {
	    super.onPause();
	    XLog.d(TAG, "Workspace onPause");
	    handleWidgetScreenEvent();
	}
	
	@Override
	public void onStop() {
	    super.onStop();
	    XLog.d(TAG, "Workspace onStop");
	}
	
	public void onFinishBindingInHome() {
	    XLog.d(TAG, "initParameters onMeasure onFinishBindingInHome");
	    handleWidgetLoadingFinished();
	    handleOverScrollLoadingFinished();
	    handleWidgetScreenEvent();
	}
	
	@Override
	public void onWindowFocusChanged (boolean hasWindowFocus){
	    XLog.d(TAG, "Workspace onWindowFocusChanged hasWindowFocus=" + hasWindowFocus + " getVisibility=" + getVisibility());
		if(mFirstResume && hasWindowFocus){
			mFirstResume = false;
		}
		handleWidgetScreenEvent();
	}
	
	@Override
	protected void onWindowVisibilityChanged(int visibility) {
	    super.onWindowVisibilityChanged(visibility);
	    XLog.d(TAG, "Workspace onWindowFocusChanged visibility=" + visibility);
	}

	@Override
	public void scrollToScreen(int whichScreen, int velocity, boolean settle,
			boolean isSnapDirectly) {

		XLog.d(TAG, "scroll screen =================");

		if (this.mDragController != null && this.mDragController.isDragging()) {
			setVisibleShadowDragView(View.GONE);
		}
		
		super.scrollToScreen(whichScreen, velocity, settle, isSnapDirectly);
		
        handleWidgetScreenEvent();
        handleDragScrollScreen(whichScreen);
	}
	
	@Override
	public void onPageFrom(int currentPage) {
		super.onPageFrom(currentPage);

		CellLayout currentScreen = (CellLayout) getChildAt(currentPage);

		if (currentScreen != null) {
			int count = currentScreen.getChildCount();

			for (int i = 0; i < count; i++) {
				View child = currentScreen.getChildAt(i);
				if (child instanceof WidgetView) {
					((WidgetView) child).screenOut();
				}
			}
		}
	}

	public void updateChildren(){    

		XLog.d("updateClildren", "updateClildren============================");

		if (m_CurrentScreen != null) {
			int count = m_CurrentScreen.getChildCount();

			for (int i = 0; i < count; i++) {
				View child = m_CurrentScreen.getChildAt(i);
				if (child instanceof WidgetView) {
					((WidgetView) child).onUpdate();
				}
			}
		}
	}

	@Override
	public void onPageTo(int currentPage) {
		super.onPageTo(currentPage);

		mScreenIndicator.snapToScreen(mCurrentScreen);

		//由于异步动画可能导致在非文件夹时显示了dragviewshadow,然后滑到文件夹屏时，区域还在
		CellLayout currentScreen = (CellLayout) getChildAt(currentPage);
		m_CurrentScreen = currentScreen;

		if (currentScreen != null) {
			int count = currentScreen.getChildCount();

			for (int i = 0; i < count; i++) {
				View child = currentScreen.getChildAt(i);
				if (child instanceof WidgetView) {
					((WidgetView) child).screenIn();
				}
			}
		}

		if (mDragController != null && this.mDragController.isDragging()
				&& mDragController.getLastDropTarget() == this) {
			onDragOver(this.mDragController.getDragObject());
		}
	}

	public boolean tryDropHomeItemInfo(HomeItemInfo info) {
		if (mDragInfo != null) {
			CellLayout.LayoutParams lp = (CellLayout.LayoutParams) mDragInfo
					.getCell().getLayoutParams();

			mTargetCell = new int[] { lp.cellX, lp.cellY };

			onDropExternal(mTargetCell, info, mDragInfo.screen, false);

			return true;
		} else {
			return false;
		}
	}

	/**
	* called from userfolder onDropSwap
	* 
	* @param info
	* @param screen
	* @param cellX
	* @param cellY
	* @return
	*/
	public boolean tryDropHomeItemInfo(HomeItemInfo info, int screen,
			int cellX, int cellY) {
		int[] targetCell = new int[] { cellX, cellY };

		onDropExternal(targetCell, info, screen, false);

		return true;
	}

	/**
	* {@inheritDoc}
	*/
	@Override
	public boolean acceptDrop(DragObject dragObject) {
		if (isLocked()) {
			return false;
		}

		// 拖去屏幕
		if (isDragScreen()) {
			return true;
		}

		WorkspaceCellLayout cellLayout = (WorkspaceCellLayout) getCurrentDropLayout();

		mDragViewVisualCenter = getDragViewVisualCenter(dragObject.x,
				dragObject.y, dragObject.xOffset, dragObject.yOffset,
				dragObject.dragView, mDragViewVisualCenter);
		
		//拖拽正在跨屏的时候不支持
		final float dragViewCenterX = mDragViewVisualCenter[0];
		if (dragViewCenterX < 0 || dragViewCenterX > WorkspaceCellLayoutMeasure.cellLayoutWidth) {
		    return false;
		}

		int spanX = 1;
		int spanY = 1;

		if (mDragInfo != null) {
			spanX = mDragInfo.spanX;
			spanY = mDragInfo.spanY;
		} else if (dragObject.dragInfo instanceof Widget) {
			Widget info = (Widget) dragObject.dragInfo;
			spanX = info.getSpanX();
			spanY = info.getSpanY();
		}
		spanX = Math.max(1, spanX);
		spanY = Math.max(1, spanY);

		mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
				(int) mDragViewVisualCenter[1], spanX, spanY, cellLayout,
				mTargetCell);
		
		boolean foundCell = mTargetCell[0] >= 0 && mTargetCell[1] >= 0;
		if (foundCell) {
		    //检查找到的位置是否被LeftScreenWidget占用了
            final View child = (mDragInfo == null) ? null : mDragInfo.getCell();
            final ArrayList<View> occupiedViews = new ArrayList<View>();
            cellLayout.getDropLocationOccupiedViews(mTargetCell[0],mTargetCell[1], spanX, spanY, child, occupiedViews);
		}
		if (foundCell) {
	        mTargetCell = cellLayout.createArea((int) mDragViewVisualCenter[0],
	                (int) mDragViewVisualCenter[1], spanX, spanY, spanX, spanY,
	                mDragInfo == null ? null : mDragInfo.getCell(), mTargetCell,
	                        null, WorkspaceCellLayout.MODE_ACCEPT_DROP);
	        foundCell = mTargetCell[0] >= 0 && mTargetCell[1] >= 0;
		}

		if (!foundCell) {
			ToastUtils.showMessage(this.getContext(), R.string.homescreen_available_for_app_alert);
		}
		
		return foundCell;
	}

	@Override
	public void onDrop(DragObject dragObject) {
		if (isLocked()) { return; }

		if (LOGD_ENABLED) {
			XLog.d(TAG, "onDrop from source: " + dragObject.dragSource + " to: "
					+ this.getClass().getName() + " with dragInfo: "
					+ dragObject.dragInfo);
		}

		if(processDropScreen(false)){
			return;
		}

		int cellLayoutIndex = getCurrentDropLayoutIndex();

		final WorkspaceCellLayout cellLayout = (WorkspaceCellLayout) getChildAt(cellLayoutIndex);

		mDragViewVisualCenter = getDragViewVisualCenter(dragObject.x,
				dragObject.y, dragObject.xOffset, dragObject.yOffset,
				dragObject.dragView, mDragViewVisualCenter);

		int spanX = 1;
		int spanY = 1;

		if (mDragInfo != null) {
			spanX = mDragInfo.spanX;
			spanY = mDragInfo.spanY;
		} else if (dragObject.dragInfo instanceof Widget) {
			Widget info = (Widget) dragObject.dragInfo;
			spanX = info.getSpanX();
			spanY = info.getSpanY();
		} else if (dragObject.dragInfo instanceof HomeItemInfo) {
			HomeItemInfo info = (HomeItemInfo) dragObject.dragInfo;
			spanX = info.spanX;
			spanY = info.spanY;
		}
		spanX = Math.max(1, spanX);
		spanY = Math.max(1, spanY);

		mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
				(int) mDragViewVisualCenter[1], spanX, spanY, cellLayout,mTargetCell);

		cellLayout.getDistanceFromCell(mDragViewVisualCenter[0],mDragViewVisualCenter[1], mTargetCell);

		if (dragObject.dragSource != this) { // 从外部拖拽进来
			mTargetCell = cellLayout.createArea(
					(int) mDragViewVisualCenter[0],
					(int) mDragViewVisualCenter[1], spanX, spanY, spanX,
					spanY, null, mTargetCell, null,
					WorkspaceCellLayout.MODE_ON_DROP_EXTERNAL);

			final View cell = onDropExternal(mTargetCell,
					dragObject.dragInfo, cellLayoutIndex, false);

			if (cell != null) {
				//If the shortcut is new APP, add the tip
				if(cell instanceof Shortcut && dragObject.dragInfo instanceof HomeDesktopItemInfo)
				{
					HomeDesktopItemInfo homeInfo = (HomeDesktopItemInfo)dragObject.dragInfo;
					Shortcut shortcut = (Shortcut)cell;
					if(homeInfo.intent != null 
							&& mLauncher.containsNewComponent(homeInfo.intent.getComponent()) != null
							&& homeInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION)
						shortcut.showTipImage(IconTip.TIP_NEW, true);
				}
/*				performDropAnimation(dragObject, cellLayout, cell,
						mTargetCell[0], mTargetCell[1], spanX, spanY, needRemoveFromFolderInfo,null);*/
			}
		} else { // 仅仅在内部拖拽
			// Move internally
			if (mDragInfo != null && mDragInfo.getCell() != null) {
				final View cell = mDragInfo.getCell();
				// 跨屏拖动
				if (cellLayout != cell.getParent()) {
					final CellLayout originalCellLayout = (CellLayout) cell
							.getParent();
					if (originalCellLayout != null) {
						originalCellLayout.removeView(cell);
					}
					cellLayout.addView(cell);
				}

				mTargetCell = cellLayout.createArea(
						(int) mDragViewVisualCenter[0],
						(int) mDragViewVisualCenter[1], spanX, spanY,
						spanX, spanY, cell, mTargetCell, null,
						WorkspaceCellLayout.MODE_ON_DROP);

				cellLayout.onDropChild(cell, mTargetCell);
				
				final HomeItemInfo info = (HomeItemInfo) cell.getTag();
				CellLayout.LayoutParams lp = (CellLayout.LayoutParams)cell.getLayoutParams();

/*				if(cell instanceof LauncherAppWidgetHostView){
					final LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView) cell;
					AppWidgetProviderInfo pinfo = hostView.getAppWidgetInfo();
					if(pinfo.resizeMode != AppWidgetProviderInfo.RESIZE_NONE){
					    Runnable runnable = new Runnable() {
							public void run() {
								DragLayer dragLayer = mLauncher.getDragLayer();
								dragLayer.addResizeFrame(hostView, cellLayout);
							}
						};
					}
				}*/

				DbManager.moveItemInDatabase(mLauncher, info,
						LauncherSettings.Favorites.CONTAINER_DESKTOP, cellLayoutIndex, lp.cellX, lp.cellY);

				//performDropAnimation(dragObject, cellLayout, cell,lp.cellX, lp.cellY,spanX, spanY, needRemoveFromFolderInfo,runnable);
			}
		}
		
		mLauncher.resetAddItemCellInfo();
		mDragInfo = null;
	}

/*	private void performDropAnimation(DropTarget.DragObject dragObject,
			CellLayout cellLayout, final View cell, int cellX, int cellY,
			int spanX, int spanY, final boolean removeFromExported,final Runnable runnable) {
		if (cell == null) {
			return;
		}

		if (dragObject.dragView == null) {
			return;
		}

		ViewUtils.enableHardwareLayer(cellLayout, false);

		float dragX = dragObject.dragView.getX(), dragY = dragObject.dragView
				.getY();
		float scale = 1f;
		if (isInEditMode()) {
			int centerX = getWidth() / 2;
			int centerY = CellLayout.getSmartTopPadding();
			dragX = Utils.scaleFrom(dragX, centerX, sEditModeScaleRatio);
			dragY = Utils.scaleFrom(dragY, centerY, sEditModeScaleRatio);
			scale = sEditModeScaleRatio;
		}

		int[] pos = mTempEstimate;

		cellLayout.getLocationOnScreen(pos);
		if (pos[0] < 0) {
			pos[0] = 0;
		}
		float ox = dragX
				- pos[0]
						+ (dragObject.dragView.getWidth() / scale - cellLayout
								.getItemWidthForSpan(spanX)) / 2f;
		float oy = dragY 
		* TODO 修复状态栏不隐藏时会跳的问题。待floatView的getY方法修复后再进行修改 -
		* pos[1]
		
		+ (dragObject.dragView.getHeight() / scale - cellLayout
				.getItemHeightForSpan(spanY)) / 2f;

		cellLayout.cellToPoint(cellX, cellY, pos);
		final CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell
				.getLayoutParams();
		lp.x = pos[0];
		lp.y = pos[1];

		TranslateAnimation trans = new TranslateAnimation(ox - lp.x, 0, oy
				- lp.y, 0);
		trans.setDuration(250);
		trans.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				cell.setVisibility(View.VISIBLE);
				post(new Runnable() {
					@Override
					public void run() {
						mDragController.onDeferredEndDrag(null);

						if(runnable != null){
							runnable.run();
						}
					}
				});
			}
		});

		mDragController.setDropAnimatingView(cell);
		dragObject.deferDragViewCleanupPostAnimation = true;
		dragObject.dragView.remove();
		cell.setVisibility(View.INVISIBLE);
		cell.startAnimation(trans);

		if (LOGD_ENABLED) {
			XLog.d(TAG, "performDropAnimation from (" + ox + ", " + oy
					+ "), to (" + lp.x + ", " + lp.y + "), for cell (" + cellX
							+ ", " + cellY + ")");
		}
	}*/


	@Override
	public void onDragEnter(DragObject dragObject) {
		if (isLocked()) {
			return;
		}

		//解bug7659：双层桌面，从抽屉层拖一个item到桌面层，桌面层没有自动增加一屏
		if (!mbAddScreenBySecondlayer 
				&& !mDragStart 
				&& dragObject.dragInfo instanceof ItemInfo 
				&& ((ItemInfo)dragObject.dragInfo).isInSecondLayer
				&& !sInEditMode
				) {
			addScreen(getChildCount(), false);
			mbAddScreenBySecondlayer = true;
		}

		WorkspaceCellLayout cellLayout = (WorkspaceCellLayout) getCurrentDropLayout();
		setCurrentDropLayout(cellLayout);

		int viewLocation[] = getDragViewLocation(cellLayout, dragObject.x
				- dragObject.xOffset, dragObject.y - dragObject.yOffset);
		
        mShadowHelper.onDragEnter(mDragInfo, cellLayout, dragObject, viewLocation);

		View cellView = cellLayout.getCellView(viewLocation[0], viewLocation[1]);

		if (cellView != null) {
			onDragOver(dragObject);
		}

	}

	@Override
	public void onDragOver(DragObject dragObject) {
		if (isLocked()) {
			return;
		}

		WorkspaceCellLayout layout = (WorkspaceCellLayout) getCurrentDropLayout();
		if (layout != mDragTargetLayout) {
			setCurrentDropLayout(layout);
		}

		boolean showNotify = false;
		
		if(mDragController.getDragObject() != null && mDragController.getDragObject().dragView != null){
			//showNotify = true;
			mDragController.getDragObject().dragView.setWarningFilter(false);
			if(mShadowHelper != null && mShadowHelper.getShadowDragView() != null){
				mShadowHelper.getShadowDragView().setWarningFilter(false);
			}
		}

		// Handle the drag over
		if (mDragTargetLayout != null) {
			final View child = (mDragInfo == null) ? null : mDragInfo.getCell();

			mDragViewVisualCenter = getDragViewVisualCenter(dragObject.x,
					dragObject.y, dragObject.xOffset, dragObject.yOffset,
					dragObject.dragView, mDragViewVisualCenter);

			int spanX = 1;
			int spanY = 1;

			if (mDragInfo != null) {
				spanX = mDragInfo.spanX;
				spanY = mDragInfo.spanY;
			} else if (dragObject.dragInfo instanceof Widget) {
				Widget info = (Widget) dragObject.dragInfo;
				spanX = info.getSpanX();
				spanY = info.getSpanY();
			} else if (dragObject.dragInfo instanceof HomeItemInfo) {
				HomeItemInfo info = (HomeItemInfo) dragObject.dragInfo;
				spanX = info.spanX;
				spanY = info.spanY;
			}
			spanX = Math.max(1, spanX);
			spanY = Math.max(1, spanY);

			if (mLauncher.getNotifyZone() != null){
				if(!mDragTargetLayout.hasEnoughSpace(spanX, spanY)){
					showNotify = true;
					mLauncher.getNotifyZone().showText(R.string.no_space_foradd);
				}
			}

			CellLayout nonAddScreenLayout = mDragTargetLayout;

			mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
					(int) mDragViewVisualCenter[1], spanX, spanY,
					mDragTargetLayout, mTargetCell);

			setCurrentDropOverCell(mTargetCell[0], mTargetCell[1]);

			ArrayList<View> occupiedViews = mDragTargetLayout.getDropLocationOccupiedViews(mTargetCell[0],mTargetCell[1], spanX, spanY, child);
			boolean nearestDropOccupied = !occupiedViews.isEmpty();
			
			if (nearestDropOccupied) {
				if ((mDragMode == DRAG_MODE_NONE || mDragMode == DRAG_MODE_REORDER)
						&& !mReorderAlarm.alarmPending()
						&& (mLastReorderX != mTargetCell[0] || mLastReorderY != mTargetCell[1])) {

					// Otherwise, if we aren't adding to or creating a folder
					// and there's no pending
					// reorder, then we schedule a reorder
					ReorderAlarmListener listener = new ReorderAlarmListener(
							mDragTargetLayout, mTargetCell,
							mDragViewVisualCenter, spanX, spanY, spanX, spanY,
							child);
					mReorderAlarm.setOnAlarmListener(listener);
					mReorderAlarm.startAlarm(REORDER_TIMEOUT);
				}
			}

			int cellX = Math.max( 0, Math.min(mTargetCell[0], nonAddScreenLayout.getCountX() - spanX));
			int cellY = Math.max( 0, Math.min(mTargetCell[1], nonAddScreenLayout.getCountY() - spanY));
			boolean moveShadowDragView = !mDragTargetLayout.isCurrentLocationOccupied(cellX,cellY, spanX, spanY, child);
			if (moveShadowDragView) {
				moveShadowDragView(nonAddScreenLayout, cellX, cellY, spanX, spanY);
			} else {
				setVisibleShadowDragView(View.GONE);
			}

			if (mDragMode == DRAG_MODE_CREATE_FOLDER || !nearestDropOccupied) {
				if (mDragTargetLayout != null) {
					mDragTargetLayout.revertTempState();
				}
			}
		}

		if(!showNotify && isInEditMode()){
			mLauncher.getNotifyZone().hide();
		}
	}
	
/*	private int getDragHoverScreenIndex(int x, int y) {
		final int nStartIndex = mDragScreenIndex - 1;
		final int nEndIndex = mDragScreenIndex + 1;

		int scaleWidth = (int) (mWidth * sEditModeScaleRatio);
		int minborder = (mWidth - scaleWidth) / 2;
		int maxborder = scaleWidth + minborder;
		if (x < minborder) {
			return Math.max(nStartIndex, 0);
		} else if (x > maxborder) {
			return Math.min(nEndIndex, getChildCount() - 1);
		} else {
			return mDragScreenIndex;
		}
	}*/

	private void moveShadowDragView(CellLayout cellLayout, int[] targetCell,int spanX, int spanY) {
		if (targetCell == null) {
			setVisibleShadowDragView(View.GONE);
		} else {
			moveShadowDragView(cellLayout, targetCell[0], targetCell[1], spanX,spanY);
		}
	}

	private void moveShadowDragView(CellLayout cellLayout, int cellX,
			int cellY, int spanX, int spanY) {
		mShadowHelper.moveShadowDragView(cellLayout, cellX, cellY, spanX, spanY);
	}

	@Override
	public void onDragExit(DragObject dragObject, DropTarget dropTarget) {
		// if (isLocked()) {
			// return;
		// }


		setCurrentDropLayout(null);

		setVisibleShadowDragView(View.GONE);
	}

	private void setVisibleShadowDragView(int visible) {
		mShadowHelper.setVisibleShadowDragView(visible);
	}

	private int[] getDragViewLocation(CellLayout cellLayout, int x, int y) {
		int[] viewLocation = new int[4];

		cellLayout.pointToCellRounded(x, y, viewLocation);
		viewLocation[3] = mCurrentScreen;
		return viewLocation;
	}

	/**
	* Calculate the nearest cell where the given object would be dropped.
	*
	* pixelX and pixelY should be in the coordinate system of layout
	*/
	private int[] findNearestArea(int pixelX, int pixelY, int spanX, int spanY,
			WorkspaceCellLayout layout, int[] recycle) {
		return layout.findNearestArea(pixelX, pixelY, spanX, spanY, recycle);
	}

	private View onDropExternal(int[] targetCell, Object dragInfo,
			int screenIndex, boolean insertAtFirst) {
		if (screenIndex < 0 || screenIndex >= getChildCount()
				|| dragInfo == null) {
			return null;
		}
		CellLayout cellLayout = (CellLayout) getChildAt(screenIndex);

		// 目标是：Widget
		if (dragInfo instanceof Widget) {
			if (targetCell == null) {
				ToastUtils.showMessage(getContext(), R.string.out_of_space,
						Toast.LENGTH_SHORT);
			} else {
				mLauncher.completeAddWidgetViewToPosition((Widget) dragInfo,
						null, targetCell);
			}
			return null;
		}

		// Drag from somewhere else
		HomeItemInfo info = null;
		info = (HomeItemInfo) dragInfo;

		View view;

		switch (info.itemType) {
		case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
		case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
			view = mLauncher.createShortcut(R.layout.shortcut, cellLayout,
					(HomeDesktopItemInfo) info);
			break;
		case LauncherSettings.Favorites.ITEM_TYPE_WIDGET_VIEW:
			view = ((LauncherWidgetViewInfo) info).hostView;
			try {
				((WidgetView) view).dispatchConfigurationChanged(this
						.getResources().getConfiguration());
			} catch (Throwable e) {
				// ignore
			}
			break;

		default:
			throw new IllegalStateException("Unknown item type: "
					+ info.itemType);
		}

		cellLayout.addView(view, insertAtFirst ? 0 : -1);
		view.setHapticFeedbackEnabled(false);
		view.setOnLongClickListener(mLongClickListener);
		if (view instanceof DropTarget) {
			mDragController.addDropTarget((DropTarget) view);
		}

		cellLayout.onDropChild(view, targetCell);
		CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view
				.getLayoutParams();

		DbManager.addOrMoveItemInDatabase(mLauncher, info,
				LauncherSettings.Favorites.CONTAINER_DESKTOP, screenIndex,
				lp.cellX, lp.cellY);

		return view;
	}

	@Override
	public void onDropCompleted(View target, boolean success) {
		if (LOGD_ENABLED) {
			XLog.d(TAG, "onDropCompleted from source: "
					+ this.getClass().getName() + " to: " + target + " success: " + success);
		}

		boolean bcandelete = false;
		if (mDragController.getDragObject().dragInfo instanceof DeleteZone.Deletable){
			DeleteZone.Deletable deleteable = (DeleteZone.Deletable)mDragController.getDragObject().dragInfo;
			bcandelete = deleteable.isDeletable(getContext());
		}
		processDropScreen((target instanceof  DeleteZone) && bcandelete);

		if (success) {
			if (target instanceof DeleteZone) {
				if (mDragInfo != null) {
					if (mDragInfo.getCell().getTag() instanceof HomeDesktopItemInfo) {
						HomeDesktopItemInfo itemInfo = ((HomeDesktopItemInfo) mDragInfo.getCell()
								.getTag());
						if (!itemInfo.isShortcut()) {
							success = false;
						}
					} 
				}
			} 
		}

		if (success) {
			if (target != this && mDragInfo != null
					&& mDragInfo.getCell() != null) {
				if (mDragInfo.getCell().getParent() != null
						&& mDragInfo.getCell().getParent() instanceof CellLayout) {
					final CellLayout cellLayout = (CellLayout) mDragInfo.getCell().getParent();
					if (cellLayout != null) {
						cellLayout.removeView(mDragInfo.getCell());
					}
				}
				if (mDragInfo.getCell() instanceof DropTarget) {
					mDragController.removeDropTarget((DropTarget)mDragInfo.getCell());
				}
				// final Object tag = mDragInfo.cell.getTag();
			}
		} else {
			if (mDragInfo != null && mDragInfo.getCell() != null) {
				final CellLayout cellLayout = (CellLayout) mDragInfo.getCell()
						.getParent();
				if (cellLayout != null) {
					cellLayout.onDropAborted(mDragInfo.getCell());
				}
			}
		}

		mDragInfo = null;
	}

	public void removeItems(final HomeItemInfoRemovedComparator comparator) {
	    removeItems(comparator, true, true);
	}
	
	public void removeItems(final HomeItemInfoRemovedComparator comparator, final boolean bUpdateDB, boolean bAvoidANR) {
	    removeItems(comparator, bUpdateDB, bAvoidANR, false);
	}
	
    public void removeItems(final HomeItemInfoRemovedComparator comparator, final boolean bUpdateDB, boolean bAvoidANR, final boolean isRelayout) {
		final int count = getChildCount();

		for (int i = 0; i < count; i++) {
			final CellLayout layout = (CellLayout) getChildAt(i);
			final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    final ArrayList<View> childrenToRemove = new ArrayList<View>();
                    childrenToRemove.clear();
                    int childCount = layout.getChildCount();
                    for (int j = 0; j < childCount; j++) {
                        final View view = layout.getChildAt(j);
                        Object tag = view.getTag();
                        if (tag instanceof HomeDesktopItemInfo) {
                            final HomeDesktopItemInfo info = (HomeDesktopItemInfo) tag;
                            if (comparator.isHomeItemInfoRemoved(info)) {
                                ((LauncherModelIphone) mLauncher.getModel()).removeItem(info, false, false);
                                mLauncher.removeItem(info, true);
                                if (bUpdateDB) {
                                    DbManager.deleteItemFromDatabase(mLauncher, info);
                                }
                                childrenToRemove.add(view);
                            }

                        } else if (tag instanceof LauncherAppWidgetInfo) {
                            if (comparator.isHomeItemInfoRemoved((LauncherAppWidgetInfo) tag)) {
                                mLauncher.removeAppWidget((LauncherAppWidgetInfo) tag, !isRelayout);
                                if (bUpdateDB) {
                                    DbManager.deleteItemFromDatabase(mLauncher, (LauncherAppWidgetInfo) tag);
                                }
                                childrenToRemove.add(view);
                            }
                        } else if (tag instanceof LauncherWidgetViewInfo) {
                            if (comparator.isHomeItemInfoRemoved((LauncherWidgetViewInfo) tag)) {
                                mLauncher.removeWidgetView((LauncherWidgetViewInfo) tag);
                                if (bUpdateDB) {
                                    DbManager.deleteItemFromDatabase(mLauncher, (LauncherWidgetViewInfo) tag);
                                }
                                childrenToRemove.add(view);
                            }
                        }
                    }
                    childCount = childrenToRemove.size();
                    for (int j = 0; j < childCount; j++) {
                        View child = childrenToRemove.get(j);
                        layout.removeViewInLayout(child);
                        if (child instanceof DropTarget) {
                            mDragController.removeDropTarget((DropTarget) child);
                        }
                    }
                    if (childCount > 0) {
                        layout.requestLayout();
                        layout.invalidate();
                    }
                }
            };
            
            if (bAvoidANR) {
                post(runnable);
            } else {
                runnable.run();
            }
		}
	}

	public void updateShortcuts(List<? extends AppInfo> apps,
			Map<ComponentName, ComponentName> modifiedMapping) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final CellLayout layout = (CellLayout) getChildAt(i);
			int childCount = layout.getChildCount();
			for (int j = 0; j < childCount; j++) {
				final View view = layout.getChildAt(j);
				Object tag = view.getTag();
				if (tag instanceof HomeDesktopItemInfo) {
					HomeDesktopItemInfo info = (HomeDesktopItemInfo) tag;
					// We need to check for ACTION_MAIN otherwise getComponent()
					// might
					// return null for some shortcuts (for instance, for
					// shortcuts to
					// web pages.)
					if (apps.contains(info)) {
						Shortcut shortcut = (Shortcut) view;
						shortcut.setIcon(info.getIcon(mIconCache));
						shortcut.setText(info.getTitle());
					} else if (info.intent != null) {
						final Intent intent = info.intent;
						final ComponentName name = intent.getComponent();
						if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
								&& Intent.ACTION_MAIN
								.equals(intent.getAction())
								&& name != null) {
							final int appCount = apps.size();
							for (int k = 0; k < appCount; k++) {
								AppInfo app = apps.get(k);
								ComponentName targetComponent = modifiedMapping == null ? null
										: modifiedMapping.get(app.getIntent()
												.getComponent());
								if (targetComponent == null) {
									targetComponent = app.getIntent()
											.getComponent();
								}
								if (targetComponent.equals(name)) {
									intent.setComponent(app.getIntent()
											.getComponent());
									info.setIcon(app.getIcon());
									info.setTitle(app.getTitle());
									Shortcut shortcut = (Shortcut) view;
									shortcut.setIcon(info.getIcon(mIconCache));
									shortcut.setText(info.getTitle());
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean isLocked() {
		return mIsLocked || !mLauncher.isWorkspaceVisible();
	}
	
	public void setLocked(boolean isLocked) {
	    XLog.d(Workspace.TAG, "Workspace setLocked isLocked=" + isLocked);
	    mIsLocked = isLocked;
	}

	@Override
	public boolean isCanLoopScreen() { // TODO
		// 这里可能需要缓存起来，一个bool值记录是否可以循环划屏，因为这个函数在划屏过程中调用太多次了
		return !this.isInEditMode() && super.isCanLoopScreen();
	}

	@Override
	protected boolean isCanScrollMoreScreen() {
		return isInEditMode();
	}

	@Override
	protected int getScrollToScreenDuration(int whichScreen, int velocity,
			boolean settle, boolean isSnapDirectly) {
		int ret = super.getScrollToScreenDuration(whichScreen, velocity,
				settle, isSnapDirectly);
		if (isCanScrollMoreScreen()) {
			ret *= (isSnapDirectly ? Math.min(1,
					Math.abs(whichScreen - mCurrentScreen)) : Math.max(1,
							Math.abs(whichScreen - mCurrentScreen)));
		}

		return ret;
	}

	@Override
	protected int fixFlingVelocity(int velocity) {
		if (isInEditMode()) {
			return (int) (velocity * sEditModeScaleRatio);
		}
		return super.fixFlingVelocity(velocity);
	}

	@Override
	public void changeEditMode(int editMode) {

		if (editMode == mEditMode) {
			return;
		}

		//清空所有屏的transx状态，回0，
		if(editMode == Workspace.EDIT_MODE_NORMAL){
			int childCount = getChildCount();
			for(int i = 0;i < childCount;i++){
				getChildAt(i).setTranslationX(0);
			}
		}

		if(getCurrentScreen() >= getChildCount()){
			setCurrentScreen(getChildCount() - 1);
		}

		mEditMode = editMode;

		//        mDisablePullPause = mEditMode > Workspace.EDIT_MODE_NORMAL;

		// 通知所有屏幕，当前状态改变
		int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			View v = getChildAt(i);
			if (v instanceof WorkspaceCellLayout) {
				WorkspaceCellLayout cellLayout = (WorkspaceCellLayout) v;
				cellLayout
				.setLayoutStatus(mEditMode == EDIT_MODE_NORMAL ? WorkspaceCellLayout.NORMAL
						: WorkspaceCellLayout.EDIT);
			}
		}
	}

	public static class SavedState extends BaseSavedState {
		public int currentScreen = -1;

		public SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			currentScreen = in.readInt();
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(currentScreen);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	@Override
	public ViewParent invalidateChildInParent(final int[] location,
			final Rect dirty) {
		ViewParent ret = super.invalidateChildInParent(location, dirty);

		if (this.isInEditMode()) {
			int centerX = mWidth / 2;
			int centerY = CellLayout.getSmartTopPadding();
			dirty.set((int) Utils.scaleTo(dirty.left, centerX,
					sEditModeScaleRatio), (int) Utils.scaleTo(dirty.top,
							centerY, sEditModeScaleRatio), (int) Utils.scaleTo(
									dirty.right, centerX, sEditModeScaleRatio), (int) Utils
									.scaleTo(dirty.bottom, centerY, sEditModeScaleRatio));
		}

		return ret;
	}

	@Override
	public void setLauncher(Launcher launcher) {
		super.setLauncher(launcher);
		if (!Constant.ENABLE_WIDGET_SCROLLABLE) {
			return;
		}
		registerProvider();
	}

	@Override
	public boolean isViewAtLocationScrollable(int x, int y) {
		boolean ret = super.isViewAtLocationScrollable(x, y);
		if (ret) {
			return ret;
		}

		if (!Constant.ENABLE_WIDGET_SCROLLABLE) {
			return false;
		}
		// will return true if widget at this position is scrollable.
				// Get current screen from the whole desktop
				CellLayout currentScreen = (CellLayout) getChildAt(mCurrentScreen);
				if(currentScreen == null){
					return false;
				}
				int[] cellXy = new int[2];
				// Get the cell where the user started the touch event
				currentScreen.pointToCellExact(x, y, cellXy);
				int count = currentScreen.getChildCount();

				// Iterate to find which widget is located at that cell
				// Find widget backwards from a cell does not work with
				// (View)currentScreen.getChildAt(cell_xy[0]*currentScreen.getCountX etc
				// etc); As the widget is positioned at the very first cell of the
				// widgetspace
				for (int i = 0; i < count; i++) {
					View child = currentScreen.getChildAt(i);
					if (child instanceof AppWidgetHostView
							|| child instanceof WidgetView) {
						// Get Layount graphical info about this widget
						CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child
								.getLayoutParams();
						// Calculate Cell Margins
						int leftCellmargin = lp.cellX;
						int rigthCellmargin = lp.cellX + lp.cellHSpan;
						int topCellmargin = lp.cellY;
						int bottonCellmargin = lp.cellY + lp.cellVSpan;
						// See if the cell where we touched is inside the Layout of the
						// widget beeing analized
						if (cellXy[0] >= leftCellmargin && cellXy[0] < rigthCellmargin
								&& cellXy[1] >= topCellmargin
								&& cellXy[1] < bottonCellmargin) {
							if (child instanceof AppWidgetHostView) {
								try {
									// Get Widget ID
									int id = ((AppWidgetHostView) child)
											.getAppWidgetId();
									// Ask to WidgetSpace if the Widget identified
									// itself
									// when created as 'Scrollable'
									boolean isScrollable = isWidgetScrollable(id);
									if (isScrollable) {
										return true;
									}
									ComponentName component = ((AppWidgetHostView) child)
											.getAppWidgetInfo().provider;
									if ("com.google.android.gallery3d".equals(component
											.getPackageName())
											&& "com.android.gallery3d.gadget.PhotoAppWidgetProvider"
											.equals(component.getClassName())) {
										return true;
									}
									return false;
								} catch (Exception e) {
									if (LOGE_ENABLED) {
										XLog.e(TAG,
												"Failed to judge whether the widget is scrollable",
												e);
									}
								}
							} else if (child instanceof WidgetView) {
								return ((WidgetView) child).scrollable();
							}
						}
					}
				}

				return false;
	}

	@Override
	public void onDragStart(DragSource source, Object info, int dragAction) {
	    onDragStart();
	    
		if(!(source instanceof Workspace)) {
			mDragStart = false;
			return;
		}
		mDragStart = true;

		if (!sInEditMode ) {
			//addScreen(getChildCount(), false);
		} else {
		}
	}

	@Override
	public void onDragEnd(boolean immediately) {
	    onDragStop();
	    
		if (mDragStart) {
			mShadowHelper.onDragEnd();
		}

		if (mDragStart || mbAddScreenBySecondlayer) {
			mbAddScreenBySecondlayer = false;
			if (!sInEditMode) {
				int lastScreenIndex = getChildCount() - 1;
				if (!isAnyAppInScreen(lastScreenIndex)) {
					removeScreen(lastScreenIndex, false);
				} else {
					saveScreenNumber();
				}
			}
		}

		setVisibleShadowDragView(View.GONE);
	}

	/**
	* 只在添加模式中存在
	* 
	* @return
	*/
	public boolean hasLeftAddScreen() {
		return false;
	}

	// Related to dragging, folder creation and reordering
	private static final int DRAG_MODE_NONE = 0;
	private static final int DRAG_MODE_CREATE_FOLDER = 1;
	private static final int DRAG_MODE_REORDER = 2;
	private int mDragMode = DRAG_MODE_NONE;
	private int mLastReorderX = -1;
	private int mLastReorderY = -1;

	private float[] mDragViewVisualCenter = new float[2];

	/**
	* The CellLayout that is currently being dragged over
	*/
	private WorkspaceCellLayout mDragTargetLayout = null;

	private int mDragOverX = -1;
	private int mDragOverY = -1;


	private void setCurrentDropLayout(WorkspaceCellLayout layout) {
		if (mDragTargetLayout != null) {
			mDragTargetLayout.revertTempState();
			mDragTargetLayout.onDragExit();
		}
		mDragTargetLayout = layout;
		if (mDragTargetLayout != null) {
			mDragTargetLayout.onDragEnter();
		}
		cleanupReorder(true);
		setCurrentDropOverCell(-1, -1);
	}

	private void setCurrentDropOverCell(int x, int y) {
		if (x != mDragOverX || y != mDragOverY) {
			mDragOverX = x;
			mDragOverY = y;
			setDragMode(DRAG_MODE_NONE);
		}
	}

	private void setDragMode(int dragMode) {
		if (dragMode != mDragMode) {
			if (dragMode == DRAG_MODE_NONE) {
				// We don't want to cancel the re-order alarm every time the
				// target cell changes
				// as this feels to slow / unresponsive.
				cleanupReorder(false);
			} else if (dragMode == DRAG_MODE_CREATE_FOLDER) {
				cleanupReorder(true);
			} 
			mDragMode = dragMode;
		}
	}

	private void cleanupReorder(boolean cancelAlarm) {
		// Any pending reorders are canceled
		if (cancelAlarm) {
			mReorderAlarm.cancelAlarm();
		}
		mLastReorderX = -1;
		mLastReorderY = -1;
	}

	// This is used to compute the visual center of the dragView. This point is
	// then
	// used to visualize drop locations and determine where to drop an item. The
	// idea is that
	// the visual center represents the user's interpretation of where the item
	// is, and hence
	// is the appropriate point to use when determining drop location.
	private float[] getDragViewVisualCenter(int x, int y, int xOffset,
			int yOffset, DragView dragView, float[] recycle) {
		float res[];
		if (recycle == null) {
			res = new float[2];
		} else {
			res = recycle;
		}

		// These represent the visual top and left of drag view if a dragRect
		// was provided.
		// If a dragRect was not provided, then they correspond to the actual
		// view left and
		// top, as the dragRect is in that case taken to be the entire dragView.
		// R.dimen.dragViewOffsetY.
		int left = x - xOffset;
		int top = y - yOffset;

		// In order to find the visual center, we shift by half the dragRect
		res[0] = left + dragView.getDragRegionWidth() / 2;
		res[1] = top + dragView.getDragRegionHeight() / 2;

		return res;
	}

	class ReorderAlarmListener implements OnAlarmListener {
		private static final String TAG = "Launcher.ReorderAlarmListener";

		private final WorkspaceCellLayout targetLayout;
		private int[] targetCell;
		private final float[] dragViewCenter;
		private final int minSpanX, minSpanY, spanX, spanY;
		private final View child;

		public ReorderAlarmListener(WorkspaceCellLayout targetLayout,
				int[] targetCell, float[] dragViewCenter, int minSpanX,
				int minSpanY, int spanX, int spanY, View child) {
			this.targetLayout = targetLayout;
			this.targetCell = targetCell;
			this.dragViewCenter = dragViewCenter;
			this.minSpanX = minSpanX;
			this.minSpanY = minSpanY;
			this.spanX = spanX;
			this.spanY = spanY;
			this.child = child;
		}

		@Override
		public void onAlarm(Alarm alarm) {
			if (LOGD_ENABLED) {
				XLog.d(TAG, "onAlarm target x: " + targetCell[0] + ", y: "
						+ targetCell[1]);
			}

			mLastReorderX = targetCell[0];
			mLastReorderY = targetCell[1];

			targetCell = targetLayout
					.createArea((int) dragViewCenter[0],
							(int) dragViewCenter[1], minSpanX, minSpanY, spanX,
							spanY, child, targetCell, null,
							WorkspaceCellLayout.MODE_DRAG_OVER);

			if (targetCell[0] < 0 || targetCell[1] < 0) {
				targetLayout.revertTempState();
			} else {
				setDragMode(DRAG_MODE_REORDER);

				moveShadowDragView(targetLayout, targetCell, spanX, spanY);
			}
		}
	}

	@Override
	protected boolean ableToHandleSecondFinger() {
		return mDragController.isDragging();
	}


	/**
	* 为单层模式添加
	* 
	* @return
	*/
	public int[] getFirstEmptyCellLocation(int targetScreen,
			boolean forceNewScreen) {
		int screenNum = getScreenCount() - 1;
		if (forceNewScreen) {
			return new int[] { screenNum + 1, 0, 0 };
		}

		if (targetScreen >= 0 && targetScreen <= screenNum) {
			int[] ret = getFirstEmptyCellLocationInScreen(targetScreen);
			if (ret != null) {
				return ret;
			}
		}

		for (; screenNum > 0; screenNum--) {
			WorkspaceCellLayout celllayout = (WorkspaceCellLayout) getChildAt(screenNum);
			if (celllayout != null && celllayout.getChildCount() > 0) {
				break;
			}
		}
		int[] ret = getFirstEmptyCellLocationInScreen(screenNum);
		if (ret != null) {
			return ret;
		}

		return new int[] { screenNum + 1, 0, 0 };
	}

	/**
	* 为单层模式添加
	* 
	* @return
	*/
	public int[] getFirstEmptyCellLocationInScreen(int screenNum) {
		if (screenNum < 0) {
			screenNum = getScreenCount() - 1;
		}
		WorkspaceCellLayout celllayout = (WorkspaceCellLayout) getChildAt(screenNum);
		if (celllayout == null) {
			return null;
		}
		int[] nextCell = celllayout.findFirstEmptyCell(screenNum);
		if (nextCell != null) {
			return nextCell;
		}
		return null;
	}

	/**
	* 为单层模式添加
	* 
	* @return
	*/
	public int[] getSpaceScreenAvailableFor(int screen, int spanX, int spanY) {
		View view = getChildAt(screen);
		if (view == null) {
			return null;
		}
		CellLayout layout = (CellLayout) view;
		int[] position = new int[2];
		boolean found = layout.findCellForSpan(position, spanX, spanY);
		if (found) {
			return position;
		}

		return null;
	}

	/**
	* 获取屏幕上下一个放置图标的位置 获取最后一个非空屏的最后一个图标后面的位置。不考虑此位置之前的空位
	* 
	* @return 返回数组，分别代表screenNum, cellx, celly.若屏幕已满则返回空
	*/
/*	private int[] getNextPositionInScreen() {
		int candidateScreen = 0;
		for (int i = getScreenCount() - 1; i >= 0; i--) {
			candidateScreen = i;
			if (isAnyAppInScreen(i)) {
				break;
			}
		}

		WorkspaceCellLayout cellLayout = (WorkspaceCellLayout) getChildAt(candidateScreen);
		int[] ret = cellLayout.findNextEmptyCell(candidateScreen);
		if (ret != null) {
			return ret;
		} else {
			return new int[] { candidateScreen + 1, 0, 0 };
		}
	}*/

/*	private int[] getNextPositionInScreen(int screenIndex) {
		WorkspaceCellLayout cellLayout = (WorkspaceCellLayout) getChildAt(screenIndex);
		int[] ret = cellLayout.findNextEmptyCell(screenIndex);
		return ret;
	}*/

	public boolean hasEnoughSpace(int screenIndex,int folderCount){
		WorkspaceCellLayout cellLayout = (WorkspaceCellLayout) getChildAt(screenIndex);
		if(cellLayout == null) return false;

		int[] ret = cellLayout.findNextEmptyCell(screenIndex);
		if (ret != null) {
			int leftRow = cellLayout.getCountY() - 1 - ret[2];
			int leftCount = leftRow * cellLayout.getCountX() + (cellLayout.getCountX() - ret[1]);
			return leftCount >= folderCount;
		}
		return false;
	}

	@Override
	protected boolean canScroll(int currentScreen, int direction) {
		if (!super.canScroll(currentScreen, direction)) {
			return false;
		}

		if (isInEditMode() || mDragController.isDragging()) {
			if (direction == DragController.SCROLL_LEFT && currentScreen <= 0) {
				return false;
			}

			if (direction == DragController.SCROLL_RIGHT
					&& currentScreen >= getChildCount() - 1) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void onPageSwitched(int previousPage, int currentPage) {
		super.onPageSwitched(previousPage, currentPage);
	}

	/**
	* 在拖拽之后将屏幕数据保存到数据库
	*/
	public void moveDragScreenAndSync() {
		if(mDragScreenIndexHolder == mDragScreenIndex) return;
		if(mDragScreenIndexHolder < 0 || mDragScreenIndex < 0 ) return;

		int reserveDragScreenIndex = mDragScreenIndex;
		int reserveDragScreenIndexHolder = mDragScreenIndexHolder;

		int start = Math.min(mDragScreenIndexHolder, mDragScreenIndex);
		int realStart = Math.min(reserveDragScreenIndexHolder, reserveDragScreenIndex);

		int end = Math.max(mDragScreenIndexHolder, mDragScreenIndex);
		int realEnd = Math.max(reserveDragScreenIndexHolder, reserveDragScreenIndex);

		// 同步UI相关的缓存, 从开始屏，到结束屏,此处处理是方便下次拖动,不可见屏，会在closeEditMode的时候处理
		for (int i = start; i <= end; i++) {
			CellLayout cellLayout = (CellLayout) getChildAt(i);
			if (cellLayout instanceof WorkspaceCellLayout) {
				((WorkspaceCellLayout) cellLayout).refreshScreenIndex();
				setScreenIndexForHomeItemInfos((WorkspaceCellLayout)cellLayout, i - getWorkspacePrefixScreenSize());
			}
		}

		//左移为1，右移-1
		int dragDirect = mDragScreenIndexHolder > mDragScreenIndex ? 1 : -1;

		//设置移动DragScreen的参数
		int distantunit = 1;
		//设置移动DragScreen的参数
		int moveDirection = mDragScreenIndexHolder > mDragScreenIndex ? distantunit : -distantunit;
		realStart = moveDirection > 0 ? realStart : realStart + 1;  
		realEnd = moveDirection > 0 ? realEnd-1 : realEnd;

		if(realStart >= getChildCount() || realEnd < 0){
			if (LOGD_ENABLED) {
				XLog.e(TAG, "sync failure,from " + mDragScreenIndexHolder + " to " + mDragScreenIndex +
						",totalscreen" + getChildCount() + ",real start=" + realStart + ", real end=" + realEnd);
			}
		}


		DbManager.moveItemsInScreenFromDatabase(
				getContext(), reserveDragScreenIndexHolder, reserveDragScreenIndex);

		if(mDragScreenIndexHolder == getDefaultScreen()){
			setHomeScreen(mDragScreenIndex);
			invalidate();
		}else if(mDefaultScreen >= realStart && mDefaultScreen <= realEnd){
			mDefaultScreen += dragDirect;
			setHomeScreen(mDefaultScreen);
		}

		//mScreenIndicator.update(new ArrayList<Integer>(), ScreenIndicator.FOLDER_INDICATOR);
	}

	public void startExportAnimationInScreen(int screen, List<View> views){
		int base = getScrollToScreenDuration(screen, 0, false, false);

		for(int i = 0; i < views.size(); i ++){
			View view = views.get(i);

			AnimationSet animations = new AnimationSet(true);
			Animation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
			alphaAnimation.setDuration(100);
			alphaAnimation.setFillAfter(true);
			alphaAnimation.setStartOffset(base + i * 50);

			Animation scaleAnimation = new ScaleAnimation(1.5f, 1.0f, 1.5f, 1.0f,
					Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
			scaleAnimation.setDuration(100);
			scaleAnimation.setFillAfter(true);
			scaleAnimation.setStartOffset(base + i * 50);

			animations.addAnimation(alphaAnimation);
			animations.addAnimation(scaleAnimation);

			view.setVisibility(View.VISIBLE);
			view.startAnimation(animations);
		}
	}

	private boolean processDropScreen(boolean dropOnDeletezone){
		if (!isDragScreen()) {
			return false;
		}

		if(mDragController.getDragObject().isCancelingDrag){
			//当前就是正常的拖拽下，没有准备执行的动画或是正在执行的滑动动画
			moveDragScreenAndSync();

			return true;
		}


		return true;    
	}

	protected void updateScreenIndex(int index) {
		for (int i = index; i <= getChildCount(); i++) {
			CellLayout cellLayout = (CellLayout)getChildAt(i);
			if (cellLayout instanceof WorkspaceCellLayout) {
				((WorkspaceCellLayout) cellLayout).refreshScreenIndex();
				setScreenIndexForHomeItemInfos(
						(WorkspaceCellLayout)cellLayout, i - getWorkspacePrefixScreenSize());
			}
		}
	}

	@Override
	public void invalidateWorkspace() {
		invalidate();
	}

	public void setHomeScreen(int index){
		if(index < 0 || index >= getChildCount() - Workspace.getWorkspaceSuffixScreenSize() - Workspace.getWorkspacePrefixScreenSize()){
			return;
		}

		mDefaultScreen = index;
		SettingPreferences.setHomeScreen(index);

		//交换
		mLauncher.getIndicator().setHomeScreen(mDefaultScreen);
	}

    @Override
    public boolean isEditMode() {
        return mEditMode >= EDIT_MODE_EDIT;
    }
    
    public int getDragScreenCurrentIndex(){
        return mDragScreenIndex;
    }
    
    public int getDragScreenHoldIndex(){
        return mDragScreenIndexHolder;
    }
    
    @Override
    public void invalidateDockbar() {
    }
    	
    public boolean isAtHomeScreen() {
        return getCurrentScreen() == getDefaultScreen();
    }
    
    @Override
    protected void onActionDown() {
        super.onActionDown();
        XLog.d(TAG, "onActionDown");
        handleWidgetActionDown();
    }
    
    @Override
    protected void onActioUp() {
        super.onActioUp();
        XLog.d(TAG, "onActioUp");
        handleWidgetActionUp();
    }
    
    @Override
    protected void onScrollStart(int scrollX, int scrollY) {
        super.onScrollStart(scrollX, scrollY);
        XLog.d(TAG, "onScrollStart");
        handleWidgetScrollStart(scrollX, scrollY);
    }
    
    @Override
    protected void onScrollStop(int scrollX, int scrollY) {
        super.onScrollStop(scrollX, scrollY);
        XLog.d(TAG, "onScrollStop");
        requestFocus();
        handleWidgetScrollStop(scrollX, scrollY);
    }
    
    private void onDragStart() {
        XLog.d(TAG, "onDragStart");
        handleWidgetDragStart();
    }
    
    private void onDragStop() {
        XLog.d(TAG, "onDragStop");
        
        autoRelayoutWorkspace();
        
        handleWidgetLoadingFinished();
        handleWidgetDragStop();
        handleWidgetScreenEvent();
    }
    
    /**
     * 获取指定位置、大小的单元格在屏幕中的位置
     * @param cellX cellX
     * @param cellY cellY
     * @param spanX spanX
     * @param spanY spanY
     * @return Rect(left, top, right, bottom)
     */
    public Rect getCellLocation(int cellX, int cellY, int spanX, int spanY) {
        Rect loc = new Rect();
        if (cellX >= 0 && cellY >= 0 && spanX > 0 && spanY > 0) {
            final int cw = WorkspaceCellLayoutMeasure.cellWidth;
            final int ch = WorkspaceCellLayoutMeasure.cellHeight;
            final int ml = WorkspaceCellLayoutMeasure.cellLayoutMarginLeft;
            final int mt = WorkspaceCellLayoutMeasure.cellLayoutMarginTop;
            final int pl = WorkspaceCellLayoutMeasure.cellLayoutPaddingLeft;
            final int pt = WorkspaceCellLayoutMeasure.cellLayoutPaddingTop;
            final int gh = WorkspaceCellLayoutMeasure.cellGapHorizontal;
            final int gv = WorkspaceCellLayoutMeasure.cellGapVertical;
            int left = (cw + gh) * cellX + ml + pl;
            int top = (ch + gv) * cellY + mt + pt;
            int right = left + cw * spanX + gh * (spanX - 1);
            int bottom = top + ch * spanY + gv * (spanY - 1);
            loc.set(left, top, right, bottom);
        }
        return loc;
    }
    
    public Rect getCellSize(int spanX, int spanY) {
        Rect size = new Rect();
        final int cw = WorkspaceCellLayoutMeasure.cellWidth;
        final int ch = WorkspaceCellLayoutMeasure.cellHeight;
        final int gh = WorkspaceCellLayoutMeasure.cellGapHorizontal;
        final int gv = WorkspaceCellLayoutMeasure.cellGapVertical;
        size.right = cw * spanX + gh * (spanX - 1);
        size.bottom = ch * spanY + gv * (spanY - 1);
        return size;
    }
    
    private Rect getWidget2x3Margin() {
        final int marginLeft = getResources().getDimensionPixelSize(R.dimen.widget2x3_margin_left);
        final int marginTop = getResources().getDimensionPixelSize(R.dimen.widget2x3_margin_top);
        final int marginRight = getResources().getDimensionPixelSize(R.dimen.widget2x3_margin_right);
        final int marginBottom = getResources().getDimensionPixelSize(R.dimen.widget2x3_margin_bottom);
        return new Rect(marginLeft, marginTop, marginRight, marginBottom);
    }
    
    private void handleWidgetLoadingFinished() {
        final int cw = WorkspaceCellLayoutMeasure.cellLayoutWidth;
        final int pl = WorkspaceCellLayoutMeasure.cellLayoutPaddingLeft;
        final int pr = WorkspaceCellLayoutMeasure.cellLayoutPaddingRight;
        final int workspaceWidth = cw - pl - pr;
        final int maxScrollX = cw * (getScreenCount() - 1);
        final Rect widget2x3Margin = getWidget2x3Margin();
        final Rect widget2x3Size = getCellSize(2, 3);
        final ArrayList<HomeItemInfo> items = mLauncher.getDesktopItems();
        for (HomeItemInfo item : items) {
            final View hostView = item.getHostView();
            if (hostView instanceof IScreenCtrlWidget) {
                final Rect loc = getCellLocation(item.cellX, item.cellY, item.spanX, item.spanY);
                if (item.spanX == 2 && item.spanY == 3) {
                    loc.set(loc.left + widget2x3Margin.left, loc.top + widget2x3Margin.top, 
                            loc.right - widget2x3Margin.right, loc.bottom - widget2x3Margin.bottom);
                }
                //注意以下四个接口的调用顺序
                ((IScreenCtrlWidget)hostView).setWorkspaceInfo(workspaceWidth, 0, maxScrollX);
                ((IScreenCtrlWidget)hostView).setWidget2x3Info(widget2x3Size, widget2x3Margin);
                ((IScreenCtrlWidget)hostView).setVisibleRegion(loc, item.screen);
                ((IScreenCtrlWidget)hostView).onWorkspaceScroll(getScrollX(), getScrollY());
            }
        }
    }
    
    private void handleWidgetScroll(int x, int y) {
        final ArrayList<HomeItemInfo> items = mLauncher.getDesktopItems();
        for (HomeItemInfo item : items) {
            final View hostView = item.getHostView();
            if (hostView instanceof IScreenCtrlWidget) {
                ((IScreenCtrlWidget)hostView).onWorkspaceScroll(x, y);
            }
        }
    }
    
    private void handleWidgetScrollStart(int scrollX, int scrollY) {
        final int currentScreen = getCurrentScreen();
        final ArrayList<HomeItemInfo> items = mLauncher.getDesktopItems();
        for (HomeItemInfo item : items) {
            final View hostView = item.getHostView();
            if (hostView instanceof IScreenCtrlWidget) {
                ((IScreenCtrlWidget)hostView).onWorkspaceScrollStart(currentScreen, scrollX, scrollY);
            }
        }
    }
    
    private void handleWidgetScrollStop(int scrollX, int scrollY) {
        final int currentScreen = getCurrentScreen();
        final ArrayList<HomeItemInfo> items = mLauncher.getDesktopItems();
        for (HomeItemInfo item : items) {
            final View hostView = item.getHostView();
            if (hostView instanceof IScreenCtrlWidget) {
                ((IScreenCtrlWidget)hostView).onWorkspaceScrollStop(currentScreen, scrollX, scrollY);
            }
        }
    }
    
    private void handleWidgetDragStart() {
        final ArrayList<HomeItemInfo> items = mLauncher.getDesktopItems();
        for (HomeItemInfo item : items) {
            final View hostView = item.getHostView();
            if (hostView instanceof IScreenCtrlWidget) {
                ((IScreenCtrlWidget)hostView).onWorkspaceDragStart();
            }
        }
    }
    
    private void handleWidgetDragStop() {
        final ArrayList<HomeItemInfo> items = mLauncher.getDesktopItems();
        for (HomeItemInfo item : items) {
            final View hostView = item.getHostView();
            if (hostView instanceof IScreenCtrlWidget) {
                ((IScreenCtrlWidget)hostView).onWorkspaceDragStop();
            }
        }
    }
    
    private void handleWidgetScreenEvent() {
        final int destScreen = getScrollScreen();
        final boolean isSupportDisp = hasWindowFocus() && !isLocked();
        final ArrayList<HomeItemInfo> items = mLauncher.getDesktopItems();
        for (HomeItemInfo item : items) {
            final View hostView = item.getHostView();
            if (hostView instanceof IScreenCtrlWidget) {
                if (isSupportDisp && item.screen == destScreen) {
                    ((IScreenCtrlWidget)hostView).onWidgetScreenIn();
                } else {
                    ((IScreenCtrlWidget)hostView).onWidgetScreenOut();
                }
            }
        }
    }
    
    private void handleWidgetActionDown() {
        final ArrayList<HomeItemInfo> items = mLauncher.getDesktopItems();
        for (HomeItemInfo item : items) {
            final View hostView = item.getHostView();
            if (hostView instanceof IScreenCtrlWidget) {
                ((IScreenCtrlWidget)hostView).onActionDown(getCurrentScreen());
            }
        }
    }
    
    private void handleWidgetActionUp() {
/*        final ArrayList<HomeItemInfo> items = mLauncher.getDesktopItems();
        for (HomeItemInfo item : items) {
            final View hostView = item.getHostView();
            if (hostView instanceof IScreenCtrlWidget) {
                ((IScreenCtrlWidget)hostView).onActionUp(getCurrentScreen());
            }
        }*/
    }
    
    private void handleWidgetAccOn() {
        final ArrayList<HomeItemInfo> items = mLauncher.getDesktopItems();
        for (HomeItemInfo item : items) {
            final View hostView = item.getHostView();
            if (hostView instanceof IScreenCtrlWidget) {
                ((IScreenCtrlWidget)hostView).onAccOn();
            }
        }
    }
    
    private void handleWidgetAccOff() {
        final ArrayList<HomeItemInfo> items = mLauncher.getDesktopItems();
        for (HomeItemInfo item : items) {
            final View hostView = item.getHostView();
            if (hostView instanceof IScreenCtrlWidget) {
                ((IScreenCtrlWidget)hostView).onAccOff();
            }
        }
    }
    
    private void handleOverScrollLoadingFinished() {
        int minScrollX = 0;
        int maxScrollX = WorkspaceCellLayoutMeasure.cellLayoutWidth * (getScreenCount() - 1);
        int maxOverScrollX = getResources().getDrawable(R.drawable.outerscroll_left_bg).getIntrinsicWidth();
        mOverScrollView.init(minScrollX, maxScrollX, maxOverScrollX);
    }
    
    private void handleOverScroll(int x, int y) {
        mOverScrollView.onWorkspaceScroll(x, y);
    }
    
    private void handleDragScrollScreen(int toScreen) {
        if (mDragController.isDragging()) {
            final HomeItemInfo dragInfo = (HomeItemInfo)mDragController.getDragObject().dragInfo;
            final WorkspaceCellLayout toCellLayout = (WorkspaceCellLayout)getChildAt(toScreen);
            if (!toCellLayout.hasEnoughSpace(dragInfo.spanX, dragInfo.spanY)) {
                final View lastChild = toCellLayout.getCellView(WorkspaceCellLayoutMeasure.cellCountHorizontal - dragInfo.spanX, 
                                                                 WorkspaceCellLayoutMeasure.cellCountVertical - dragInfo.spanY);
                final HomeItemInfo lastChildInfo = lastChild != null ? (HomeItemInfo)lastChild.getTag() : null;
                XLog.d("Snser", "handleDragScrollScreen kkk toScreen=" + toScreen + " lastChild=" + lastChildInfo.getHostViewName());
                if (lastChildInfo != null && lastChildInfo.spanX == dragInfo.spanX && lastChildInfo.spanY == dragInfo.spanY) {
                    //拖拽到满屏时，将满屏中最后一个widget先删掉
                    mDragScrollRemovedItemInfos.add(lastChildInfo.cloneSelf());
                    removeItems(new Launcher.HomeItemInfoEqualComparator(lastChildInfo), false, false);
                }
            }
        }
    }
    
    /**
     * 自动对齐，将所有widget靠前紧密排列(仅支持高度满屏的widget)
     */
    private void autoRelayoutWorkspace() {
        final int nCellCountHorizontal = WorkspaceCellLayoutMeasure.cellCountHorizontal;
        final int nCellCountVertical = WorkspaceCellLayoutMeasure.cellCountVertical;
        
        final ArrayList<HomeItemInfo> rawItems = mLauncher.getDesktopItems();
        final ArrayList<HomeItemInfo> copyItems = new ArrayList<HomeItemInfo>();
        
        //复制一份数据，并校验数据合法性
        boolean isSupportRelayout = true;
        for (HomeItemInfo rawItem : rawItems) {
            if (rawItem.cellY == 0 && rawItem.spanY == nCellCountVertical) {
                copyItems.add(rawItem.cloneSelf());
            } else {
                isSupportRelayout = false;
            }
            if (!isSupportRelayout) {
                //自动对齐仅支持高度满屏的widget
                return;
            }
        }
        
        //将跨屏拖拽中临时删掉的item补上
        if (!mDragScrollRemovedItemInfos.isEmpty()) {
            final int currentDropLayoutIndex = getCurrentDropLayoutIndex();
            for (HomeItemInfo removedItemInfo : mDragScrollRemovedItemInfos) {
                if (currentDropLayoutIndex == removedItemInfo.screen) {
                    //拖拽的view落在了原先满屏的页面上
                    ++removedItemInfo.screen;
                    removedItemInfo.cellX = Integer.MIN_VALUE;
                } else {
                    //拖拽的view没有落在原先满屏的页面上
                    removedItemInfo.cellX = Integer.MAX_VALUE;
                }
                copyItems.add(removedItemInfo.cloneSelf());
            }
            mDragScrollRemovedItemInfos.clear();
        }
        
        //按位置排序
        Collections.sort(copyItems, new Comparator<HomeItemInfo>() {
            @Override
            public int compare(HomeItemInfo lhs, HomeItemInfo rhs) {
                if (lhs.screen != rhs.screen) {
                    return lhs.screen < rhs.screen ? -1 : 1; //return lhs.screen - rhs.screen; 可能会溢出
                } else if (lhs.cellX != rhs.cellX) {
                    return lhs.cellX < rhs.cellX ? -1 : 1;
                } else if (lhs.cellY != rhs.cellY) {
                    return lhs.cellY < rhs.cellY ? -1 : 1;
                } else {
                    return 0;
                }
            }
        });
        
        //重新计算位置
        int cellX = 0;
        int screen = 0;
        for (HomeItemInfo copyItem : copyItems) {
            if (cellX + copyItem.spanX > nCellCountHorizontal) {
                ++screen;
                cellX = 0;
            }
            copyItem.screen = screen;
            copyItem.cellX = cellX;
            cellX += copyItem.spanX;
        }
        
/*        for (HomeItemInfo copyItem : copyItems) {
            XLog.d(TAG, "relayoutWorkspace relayoutCopyItem s=" + copyItem.screen + " x=" + copyItem.cellX + " y=" + copyItem.cellY + " host=" + copyItem.getHostViewName());
        }*/
        
        //刷新UI和DB
        mLauncher.bindItems(copyItems, 0, copyItems.size(), true);
    }
    
    public void onAccOn() {
        XLog.d(Workspace.TAG, "Workspace onAccOn");
        handleWidgetAccOn();
    }
    
    public void onAccOff() {
        XLog.d(Workspace.TAG, "Workspace onAccOff");
        handleWidgetAccOff();
    }

}
