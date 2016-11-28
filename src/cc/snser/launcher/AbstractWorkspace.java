
package cc.snser.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import cc.snser.launcher.apps.components.IconTip;
import cc.snser.launcher.apps.components.workspace.Shortcut;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.model.DbManager;
import cc.snser.launcher.screens.OverScrollView;
import cc.snser.launcher.screens.Workspace;
import cc.snser.launcher.ui.components.ScreenIndicator;
import cc.snser.launcher.ui.components.pagedsv.DefCacheManager;
import cc.snser.launcher.ui.components.pagedsv.PagedScrollView;
import cc.snser.launcher.ui.components.pagedsv.PagedScrollView.PageSwitchListener;
import cc.snser.launcher.ui.dragdrop.DragController;
import cc.snser.launcher.ui.dragdrop.DragScroller;
import cc.snser.launcher.ui.dragdrop.DropTarget;
import cc.snser.launcher.ui.effects.EffectFactory;
import cc.snser.launcher.ui.effects.EffectInfo;
import cc.snser.launcher.ui.utils.UiConstants;
import cc.snser.launcher.ui.utils.Utilities;
import cc.snser.launcher.util.ResourceUtils;
import cc.snser.launcher.widget.WidgetView;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.support.v4.util.ViewUtils;
import com.shouxinzm.launcher.util.ScreenDimensUtils;

import java.util.ArrayList;

import static cc.snser.launcher.Constant.LOGE_ENABLED;

/**
 * <p>
 * Abstract the common logic for the workspace view.
 * </p>
 *
 * @author huangninghai
 * @version 1.0
 */
public abstract class AbstractWorkspace extends PagedScrollView implements DragScroller, PageSwitchListener {
	private static final String TAG = "Launcher.AbstractWorkspace";

	public static final int LEFT_PAGE_SCREEN_ID = 0;

	protected int mDefaultScreen;

	protected OnLongClickListener mLongClickListener;

	protected Launcher mLauncher;
	
	protected OverScrollView mOverScrollView;

	protected int[] mTempCell = new int[2];

	protected int[] mTempEstimate = new int[2];

	// TODO: 重构：考虑设计为interface代替 View/ViewGroup，卸耦
	protected ScreenIndicator mScreenIndicator;

	private int mScreenTransitionType = Integer.MIN_VALUE;

	private int mCurrentScreenTransitionType;

	private EffectInfo mCurrentEffectInfo = null;

	public static final int EDIT_MODE_NORMAL = 0;
	public static final int EDIT_MODE_EDIT = 1;
	public static final int EDIT_MODE_DRAGSCREEN = 2;

	protected int mEditMode = EDIT_MODE_NORMAL;

	protected Transformation mChildTransformation = new Transformation();

	private boolean isTransformationDirty = false;

	private static final int CACHE_STRATEGY_CHILDREN = 0;

	private static final int CACHE_STRATEGY_SELF = 1;

	private int mCacheStrategy = CACHE_STRATEGY_SELF;

	private Drawable mScreenDecor;

	private int mOverScrollIndex = -1;

	private Drawable mLeftDrawable;

	private Drawable mRightDrawable;

	private int mMaxOverScrollX;

	private int mLastScrollX;
	private Context mContext;
	
	private boolean mIsKeepScrolling = false;
	
	protected boolean mIsLocked = false;

	public AbstractWorkspace(Context context) {
		this(context, null);
	}

	public AbstractWorkspace(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AbstractWorkspace(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.addPageSwitchListener(this);
		mContext = context;

		/*
		 * 缓存机制相关代码如下（可考虑将这块代码进一步集中起来）
		 * 1、滑动过程中/延时停止，需要更新缓存设置
		 * 2、初始化/更改Workspace页面切换特效时，需要更新缓存设置
		 * 3、进入/退出编辑模式时，需要更新缓存设置
		 * 4、增加屏幕时，需要更新缓存设置
		 */

		// 开启子页面缓存，实现平滑滚动效果
		this.setCacheHandler(new DefCacheManager(this) {
			@Override
			protected void enableChildViewCache(View childView) {
				boolean enabled = canEnableDrawCache();
				if (enabled) {
					if (mCacheStrategy == CACHE_STRATEGY_SELF || isInEditMode()) {
						((CellLayout) childView).enableSelfCache();
					} else {
						((CellLayout) childView).enableChildrenCache();
					}
				} else {
					disableChildViewCache(childView, false);
				}
			}

			@Override
			protected void disableChildViewCache(View childView, boolean destroyCache) {
				((CellLayout) childView).disableCache();
				if (destroyCache) {
					((CellLayout) childView).destroyCache();
				}
			}

		});
	}

	protected boolean canEnableDrawCache() {
		return LauncherSettings.canEnableDrawCache(this.getContext());
	}

	@Override
	public boolean isInEditMode() {
		return mEditMode >= EDIT_MODE_EDIT;
	}

	public int getEditMode(){
		return mEditMode;
	}

	protected abstract void changeEditMode(int editMode);

	protected abstract boolean isLocked();

	public abstract void loadScreenTransitionType();

	protected abstract boolean isLoopWallpaper();

	private void checkAndInitChildView(View child) {
		if (!(child instanceof CellLayout)) {
			throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
		}
	}

	@Override
	public void addView(View child, int index, LayoutParams params) {
		checkAndInitChildView(child);
		super.addView(child, index, params);
	}

	@Override
	public void addView(View child) {
		checkAndInitChildView(child);
		super.addView(child);
	}

	@Override
	public void addView(View child, int index) {
		checkAndInitChildView(child);
		super.addView(child, index);
	}

	@Override
	public void addView(View child, int width, int height) {
		checkAndInitChildView(child);
		super.addView(child, width, height);
	}

	@Override
	public void addView(View child, LayoutParams params) {
		checkAndInitChildView(child);
		super.addView(child, params);
	}

	public void removeInScreen(View child, int screen) {
		if (screen < 0 || screen >= getChildCount()) {
			if (LOGE_ENABLED) {
				XLog.e(TAG, "The screen must be >= 0 and < " + getChildCount() + " (was " + screen + "); skipping child");
			}
			return;
		}
		final CellLayout group = (CellLayout) getChildAt(screen);
		group.removeView(child);
	}

	public View removeInScreen(int screen, int cellX, int cellY) {
		if (screen < 0 || screen >= getChildCount()) {
			if (LOGE_ENABLED) {
				XLog.e(TAG, "The screen must be >= 0 and < " + getChildCount() + " (was " + screen + "); skipping child");
			}
			return null;
		}
		final CellLayout group = (CellLayout) getChildAt(screen);
		View view = group.getCellView(cellX, cellY);
		if (view != null) {
			removeInScreen(view, screen);
		}
		return view;
	}

	public void transparentActivityLaunched() {
		CellLayout currentScreen = (CellLayout) getChildAt(getCurrentScreen());
		if(currentScreen == null) return;
		int count = currentScreen.getChildCount();
		for (int i = 0; i < count; i++) {
			View child = currentScreen.getChildAt(i);
			if (child instanceof WidgetView) {
				((WidgetView) child).onTransparentActivityLaunched();
			}
		}
	}

	public boolean isDefaultScreenShowing() {
		return mCurrentScreen == mDefaultScreen;
	}

	public abstract void addScreen();

	public abstract boolean removeScreen(int index);

	public abstract boolean removeAllScreens();

	/**
	 * Adds the specified child in the current screen. The position and
	 * dimension of the child are defined by x, y, spanX and spanY.
	 *
	 * @param child The child to add in one of the workspace's screens.
	 * @param x The X position of the child in the screen's grid.
	 * @param y The Y position of the child in the screen's grid.
	 * @param spanX The number of cells spanned horizontally by the child.
	 * @param spanY The number of cells spanned vertically by the child.
	 * @param insert When true, the child is inserted at the beginning of the
	 *            children list.
	 */
	protected void addInCurrentScreen(View child, int x, int y, int spanX, int spanY, boolean insert) {
		addInScreen(child, mCurrentScreen, x, y, spanX, spanY, insert);
	}

	/**
	 * Adds the specified child in the specified screen. The position and
	 * dimension of the child are defined by x, y, spanX and spanY.
	 *
	 * @param child The child to add in one of the workspace's screens.
	 * @param screen The screen in which to add the child.
	 * @param x The X position of the child in the screen's grid.
	 * @param y The Y position of the child in the screen's grid.
	 * @param spanX The number of cells spanned horizontally by the child.
	 * @param spanY The number of cells spanned vertically by the child.
	 */
	protected void addInScreen(View child, int screen, int x, int y, int spanX, int spanY) {
		addInScreen(child, screen, x, y, spanX, spanY, false);
	}

	/**
	 * Adds the specified child in the specified screen. The position and
	 * dimension of the child are defined by x, y, spanX and spanY.
	 *
	 * @param child The child to add in one of the workspace's screens.
	 * @param screen The screen in which to add the child.
	 * @param x The X position of the child in the screen's grid.
	 * @param y The Y position of the child in the screen's grid.
	 * @param spanX The number of cells spanned horizontally by the child.
	 * @param spanY The number of cells spanned vertically by the child.
	 * @param insert When true, the child is inserted at the beginning of the
	 *            children list.
	 */
	public void addInScreen(View child, int screen, int x, int y, int spanX, int spanY, boolean insert) {
		addInScreen(child, screen, x, y, spanX, spanY, insert, true);
	}

	/**
	 * Adds the specified child in the specified screen. The position and
	 * dimension of the child are defined by x, y, spanX and spanY.
	 *
	 * @param child The child to add in one of the workspace's screens.
	 * @param screen The screen in which to add the child.
	 * @param x The X position of the child in the screen's grid.
	 * @param y The Y position of the child in the screen's grid.
	 * @param spanX The number of cells spanned horizontally by the child.
	 * @param spanY The number of cells spanned vertically by the child.
	 * @param insert When true, the child is inserted at the beginning of the
	 *            children list.
	 */
	protected void addInScreen(View child, int screen, int x, int y, int spanX, int spanY, boolean insert, boolean invalidate) {
		int childCount = getChildCount();
		if (childCount <= screen) {
			for (; childCount <= screen; childCount++) {
				addScreen();
			}
		}

		if (screen < 0 || screen >= getChildCount()) {
			if (LOGE_ENABLED) {
				XLog.e(TAG, "The screen must be >= 0 and < " + getChildCount() + " (was " + screen + "); skipping child");
			}
			return;
		}

		final CellLayout group = (CellLayout) getChildAt(screen);
		CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
		if (lp == null) {
			lp = new CellLayout.LayoutParams(x, y, spanX, spanY);
		} else {
			lp.cellX = x;
			lp.cellY = y;
			lp.cellHSpan = spanX;
			lp.cellVSpan = spanY;
		}
		group.addView(child, insert ? 0 : -1, lp, invalidate);
		child.setHapticFeedbackEnabled(false);
		child.setOnLongClickListener(mLongClickListener);
		
		if (child instanceof DropTarget) {
			if (mDragController != null) {
				mDragController.addDropTarget((DropTarget) child);
			}
		}
	}

	public boolean addApplicationShortcut(HomeItemInfo contentInfo, int screen, int cellX, int cellY,
			boolean insertAtFirst, int intersectX, int intersectY) {
		View v = mLauncher.createShortcut((HomeDesktopItemInfo)contentInfo);
		final CellLayout target = (CellLayout)getChildAt(screen);
		final int[] cellXY = new int[2];
		boolean found = target.findCellForSpanThatIntersects(cellXY, 1, 1, intersectX, intersectY);
		if (found) {

			if(contentInfo instanceof HomeDesktopItemInfo && v instanceof Shortcut)
			{
				HomeDesktopItemInfo item = (HomeDesktopItemInfo)contentInfo;
				Shortcut shortcut = (Shortcut) v;
				if(item.intent != null
						&& mLauncher.containsNewComponent(item.intent.getComponent()) != null
						&& item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION)
					shortcut.showTipImage(IconTip.TIP_NEW);
			}

			addInScreen(v, screen, cellXY[0], cellXY[1], 1, 1, insertAtFirst);

			DbManager.addOrMoveItemInDatabase(mLauncher, (HomeItemInfo)contentInfo,
					LauncherSettings.Favorites.CONTAINER_DESKTOP, screen, cellXY[0], cellXY[1]);
		}
		return found;
	}

	public View addApplicationShortcut(HomeItemInfo contentInfo, int screen, int cellX, int cellY,
			boolean insertAtFirst, int intersectX, int intersectY, boolean visible){
		View v = mLauncher.createShortcut((HomeDesktopItemInfo)contentInfo);
		final CellLayout target = (CellLayout) getChildAt(screen);
		final int[] cellXY = new int[2];
		target.findCellForSpanThatIntersects(cellXY, 1, 1, intersectX, intersectY);

		if(contentInfo instanceof HomeDesktopItemInfo && v instanceof Shortcut)
		{
			HomeDesktopItemInfo item = (HomeDesktopItemInfo)contentInfo;
			Shortcut shortcut = (Shortcut) v;
			if(item.intent != null
					&& mLauncher.containsNewComponent(item.intent.getComponent()) != null
					&& item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION)
				shortcut.showTipImage(IconTip.TIP_NEW);
		}
		addInScreen(v, screen, cellXY[0], cellXY[1], 1, 1, insertAtFirst);
		if(!visible){
			v.setVisibility(View.INVISIBLE);
		}
		DbManager.addOrMoveItemInDatabase(mLauncher, (HomeItemInfo)contentInfo,
				LauncherSettings.Favorites.CONTAINER_DESKTOP, screen, cellXY[0], cellXY[1]);

		return v;

	}

	public void addChildAndUpdateDb(View child, int screen, int cellX, int cellY,
			boolean insertAtFirst, int intersectX, int intersectY) {
		final CellLayout target = (CellLayout)getChildAt(screen);
		final int[] cellXY = new int[2];
		target.findCellForSpanThatIntersects(cellXY, 1, 1, intersectX, intersectY);
		addInScreen(child, screen, cellXY[0], cellXY[1], 1, 1, insertAtFirst);

		HomeItemInfo info = (HomeItemInfo)child.getTag();
		DbManager.addOrMoveItemInDatabase(mLauncher, info,
				LauncherSettings.Favorites.CONTAINER_DESKTOP, screen, cellXY[0], cellXY[1]);
	}

	/**
	 * Registers the specified listener on each screen contained in this
	 * workspace.
	 *
	 * @param l The listener used to respond to long clicks.
	 */
	 @Override
	 public void setOnLongClickListener(OnLongClickListener l) {
		 mLongClickListener = l;
		 final int count = getChildCount();
		 for (int i = 0; i < count; i++) {
			 View child = getChildAt(i);
			 child.setOnLongClickListener(l);
		 }
	 }

	 @Override
	 public void setOnClickListener(OnClickListener l) {
		 final int count = getChildCount();
		 for (int i = 0; i < count; i++) {
			 getChildAt(i).setOnClickListener(l);
		 }
	 }

	 @Override
	 protected void onAttachedToWindow() {
		 super.onAttachedToWindow();
		 computeScroll();
		 if (mDragController != null) {
			 mDragController.setWindowToken(getWindowToken());
		 }
	 }

	 @Override
	 public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
		 if (mLauncher == null || !mLauncher.isWorkspaceLocked()) {
			 int screen = indexOfChild(child);
			 if (screen != mCurrentScreen || !mScroller.isFinished()) {
				 scrollToScreen(screen);
				 return true;
			 }
		 }
		 return false;
	 }

	 @Override
	 protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
		 if (!isLocked()) {
			 int focusableScreen;
			 if (mNextScreen != INVALID_SCREEN) {
				 focusableScreen = mNextScreen;
			 } else {
				 focusableScreen = mCurrentScreen;
			 }
			 View child = getChildAt(focusableScreen);
			 if (child != null) {
				 child.requestFocus(direction, previouslyFocusedRect);
			 }
		 }
		 return false;
	 }

	 @Override
	 public boolean dispatchUnhandledMove(View focused, int direction) {
		 if (direction == View.FOCUS_LEFT) {
			 if (getCurrentScreen() > 0) {
				 scrollToScreen(getCurrentScreen() - 1);
				 return true;
			 }
		 } else if (direction == View.FOCUS_RIGHT) {
			 if (getCurrentScreen() < getChildCount() - 1) {
				 scrollToScreen(getCurrentScreen() + 1);
				 return true;
			 }
		 }
		 return super.dispatchUnhandledMove(focused, direction);
	 }

	 @Override
	 public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
		 if (!isLocked()) {
			 View child = getChildAt(mCurrentScreen);
			 if (child != null) {
				 child.addFocusables(views, direction);
			 }
			 if (direction == View.FOCUS_LEFT) {
				 if (mCurrentScreen > 0) {
					 child = getChildAt(mCurrentScreen - 1);
					 if (child != null) {
						 child.addFocusables(views, direction);
					 }
				 }
			 } else if (direction == View.FOCUS_RIGHT) {
				 if (mCurrentScreen < getChildCount() - 1) {
					 child = getChildAt(mCurrentScreen + 1);
					 if (child != null) {
						 child.addFocusables(views, direction);
					 }
				 }
			 }
		 }
	 }

	 @Override
	 protected boolean filterTouchEvent() {
		 return isLocked();
	 }

	 @Override
	 protected boolean isViewAtLocationScrollable(int x, int y) {
		 return false;
	 }

	 @Override
	 public boolean onInterceptTouchEvent(MotionEvent ev) {
		 boolean ret = super.onInterceptTouchEvent(ev);

		 final int action = ev.getAction();

		 switch (action) {
		 case MotionEvent.ACTION_CANCEL:
		 case MotionEvent.ACTION_UP:
			 if (mTouchState == TOUCH_STATE_REST) {
				 final CellLayout currentScreen = (CellLayout) getChildAt(mCurrentScreen);
				 if (!currentScreen.lastDownOnOccupiedCell()) {
				 }
			 }
			 break;
		 case MotionEvent.ACTION_DOWN:
			 EffectInfo info = getCurrentEffectInfo(getChildAt(mCurrentScreen));
			 if (info != null) {
				 info.onTouchDown(isScrolling());
			 }
			 break;
		 }

		 return ret;
	 }

	 @Override
	protected void handleInterceptTouchDown(MotionEvent ev) {
	    super.handleInterceptTouchDown(ev);
	    onActionDown();
	}
	 
	 @Override
	 protected void handleTouchUp() {
		 EffectInfo info = getCurrentEffectInfo(getChildAt(mCurrentScreen));
		 if (info != null) {
			 info.onTouchUpCancel(isScrolling());
		 }
		 mLastScrollX = getScrollX();
		 super.handleTouchUp();
		 onActioUp();
	 }

	 /**
	  * If one of our descendant views decides that it could be focused now, only
	  * pass that along if it's on the current screen. This happens when live
	  * folders requery, and if they're off screen, they end up calling
	  * requestFocus, which pulls it on screen.
	  */
	 @Override
	 public void focusableViewAvailable(View focused) {
		 View current = getChildAt(mCurrentScreen);
		 View v = focused;
		 while (true) {
			 if (v == current) {
				 super.focusableViewAvailable(focused);
				 return;
			 }
			 if (v == this) {
				 return;
			 }
			 ViewParent parent = v.getParent();
			 if (parent instanceof View) {
				 v = (View) v.getParent();
			 } else {
				 return;
			 }
		 }
	 }

	 public void setLauncher(Launcher launcher) {
		 mLauncher = launcher;
	 }
	 
	 public void setOverScrollView(OverScrollView overScrollView) {
	     mOverScrollView = overScrollView;
	 }

	 public Launcher getLauncher() {
		 return mLauncher;
	 }

	 public View getViewForTag(Object tag) {
		 int screenCount = getChildCount();
		 for (int screen = 0; screen < screenCount; screen++) {
			 CellLayout currentScreen = ((CellLayout) getChildAt(screen));
			 int count = currentScreen.getChildCount();
			 for (int i = 0; i < count; i++) {
				 View child = currentScreen.getChildAt(i);
				 if (child.getTag() == tag) {
					 return child;
				 }
			 }
		 }
		 return null;
	 }

	 public void moveToScreen(int screen, boolean animate) {
		 screen = Math.max(0, Math.min(screen, getChildCount() - 1));
		 if (animate) {
			 scrollToScreen(screen);
		 } else {
			 setCurrentScreen(screen);
			 if(screen == mCurrentScreen){
				 mScreenIndicator.forceUpdateIndicator(screen);
			 }
		 }
		 getChildAt(screen).requestFocus();
	 }

	 public void moveToDefaultScreen(boolean animate) {
		 moveToScreen(mDefaultScreen, animate);
	 }

	 public void setScreenIndicator(ScreenIndicator screenIndicator) {
		 mScreenIndicator = screenIndicator;
	 }

	 public void setScreenTransitionType(int screenTransitionType) {
		 this.mScreenTransitionType = screenTransitionType;
		 if (mLauncher == null || !mLauncher.getDragLayer().isHidingCurrentScreen()) {
			 refreshCurrentScreenTransitionType();
		 }
	 }

	 private void refreshCurrentScreenTransitionType() {
		 final int oldCurrentScreenTransitionType = this.mCurrentScreenTransitionType;
		 this.mCurrentScreenTransitionType = this.mScreenTransitionType;

		 if (this.mCurrentScreenTransitionType < 0) {
			 if (this.mCurrentScreenTransitionType == EffectFactory.TYPE_RANDOM) {
				 this.mCurrentScreenTransitionType = EffectFactory.getRandomEffectType();
			 } else {
				 this.mCurrentScreenTransitionType = EffectFactory.TYPE_CLASSIC;
			 }
		 }

		 if (mCurrentEffectInfo == null || mCurrentScreenTransitionType != oldCurrentScreenTransitionType) {

			 EffectInfo oldEffectInfo = mCurrentEffectInfo;

			 mCurrentEffectInfo = EffectFactory.getEffectByType(mCurrentScreenTransitionType);

			 if (oldEffectInfo != null) {
				 oldEffectInfo.onEffectCheckChanged(false, mCurrentEffectInfo);
			 }

			 if (mCurrentEffectInfo != null) {
				 mCurrentEffectInfo.onEffectCheckChanged(true, oldEffectInfo);
			 }

			 mScroller.onEffectChanged(mCurrentEffectInfo);

			 int cacheStrategy;
			 if (mCurrentEffectInfo == null || mCurrentEffectInfo.canEnableWholePageDrawingCache()) {
				 cacheStrategy = CACHE_STRATEGY_SELF;
			 } else {
				 cacheStrategy = CACHE_STRATEGY_CHILDREN;
			 }
			 if (cacheStrategy != this.mCacheStrategy) {
				 this.mCacheStrategy = cacheStrategy;
				 this.enableCurrentScreenCache();
			 }

			 if (mCurrentEffectInfo != null && mCurrentEffectInfo.hasScreenDecor()) {
				 mScreenDecor = getResources().getDrawable(R.drawable.screen_preview);
			 } else {
				 mScreenDecor = null;
			 }

			 int alphaType = mCurrentEffectInfo != null ? mCurrentEffectInfo.getAlphaTransitionType() : 0;
			 setAlphaConcerned((alphaType & 1) != 0);
			 final boolean hasCellAlpha = (alphaType & 2) != 0;
			 for (int i = getChildCount() - 1; i >= 0; i--) {
				 View child = getChildAt(i);
				 if (child instanceof CellLayout) {
					 ((CellLayout) child).setAlphaConcerned(hasCellAlpha);
					 ((CellLayout) child).resetTransitionAlpha();
				 }
			 }
		 }
	 }

	 public int getCurrentScreenTransitionType() {
		 return this.mCurrentScreenTransitionType;
	 }

	 public int getOffset(View childView) {
		 if (!this.isCanLoopScreen()) {
			 return 0;
		 }
		 int childIndex = this.indexOfChild(childView);
		 if (childIndex != 0 && childIndex != getChildCount() - 1) {
			 return 0;
		 }
		 return getOffsetForChildIndex(childIndex);
	 }

	 private int getOffsetForChildIndex(int childIndex) {
		 final int childCount = this.getChildCount();
		 final int measureWidth = this.getMeasuredWidth();

		 if (this.mCurrentScreen == childCount - 1 && childIndex == childCount - 1) {
			 if (mMoveDirection == DIRECTION_RIGHT && this.getScrollX() <= 0) {
				 return -measureWidth * childCount;
			 }
		 }
		 if (this.mCurrentScreen == childCount - 1 && childIndex == 0) {
			 if ((mMoveDirection == DIRECTION_RIGHT || getScrollX() > getChildAt(childIndex).getWidth() + getChildAt(childIndex).getLeft()) && this.getScrollX() > 0) {
				 return measureWidth * childCount;
			 }
		 }
		 if (this.mCurrentScreen == 0 && childIndex == 0) {
			 if (mMoveDirection == DIRECTION_LEFT && this.getScrollX() >= measureWidth * (childCount - 1)) {
				 return measureWidth * childCount;
			 }
		 }
		 if (this.mCurrentScreen == 0 && childIndex == childCount - 1) {
			 if ((mMoveDirection == DIRECTION_LEFT || (getScrollX() < getChildAt(childIndex).getWidth() + getChildAt(childIndex).getLeft() && childIndex != 1))
					 && this.getScrollX() < measureWidth * (childCount - 1)) { // 只有两屏的时候mScrollX判断不对，暂时在这种情况下关闭修改
				 return -measureWidth * childCount;
			 }
		 }
		 return 0;
	 }

	 @Override
	 protected float getCurrentScrollRadio(View childView, int offset) {
		 float childMeasuredWidth = childView.getMeasuredWidth();
		 int childLeft = childView.getLeft() + offset;

		 return (this.getScrollX() - childLeft) * 1.0F / childMeasuredWidth;
	 }

	 @Override
	 protected float getCurrentScrollRadio(int childIndex, int childWidth, int offset) {
		 int childLeft = childWidth * childIndex + offset;

		 return (this.getScrollX() - childLeft) * 1.0F / childWidth;
	 }

	 @Override
	 protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		 if (child == null) {
			 return true;
		 }

		 if (isTransformationDirty) {
			 mChildTransformation.clear();
			 isTransformationDirty = false;
		 }

		 final Transformation childTransformation = mChildTransformation;
		 final EffectInfo effect = getChildTransformation(child, childTransformation);
		 if (effect != null) {
			 if (mLeftDrawable != null) {
				 mLeftDrawable = null;
			 }
			 if (mRightDrawable != null) {
				 mRightDrawable = null;
			 }

			 if (effect.type == EffectFactory.TYPE_CYLINDER || effect.type == EffectFactory.TYPE_SPHERE) {
				 effect.setInterpolator(mScrollInterpolator != null ? mScrollInterpolator : new DecelerateInterpolator());
			 }
			 final int cl = child.getLeft();
			 final int ct = child.getTop();

			 canvas.save();

			 canvas.translate(cl, ct);
			 if (LauncherSettings.getRenderPerformanceMode(getContext()) == LauncherSettings.RENDER_PERFORMANCE_MODE_PREFER_QUALITY) {
				 canvas.setDrawFilter(effect.isWorkspaceNeedAntiAlias() ? UiConstants.ANTI_ALIAS_FILTER : null);
			 }

			 if (mChildTransformation.getTransformationType() == Transformation.TYPE_MATRIX || mChildTransformation.getTransformationType() == Transformation.TYPE_BOTH) {
				 canvas.concat(mChildTransformation.getMatrix());
				 isTransformationDirty = true;
			 }

			 if (mChildTransformation.getTransformationType() == Transformation.TYPE_ALPHA
					 || mChildTransformation.getTransformationType() == Transformation.TYPE_BOTH) {
				 if (mChildTransformation.getAlpha() < 1.0F) {
					 applyAlpha(child, mChildTransformation.getAlpha());
					 isTransformationDirty = true;
				 }
			 }
			 canvas.translate(-cl, -ct);

			 beforeDrawTransformedChild(canvas, child, drawingTime, effect);
			 if (effect.hasScreenDecor()) {
		         //drawScreenDecor(canvas, child, effect.getScreenDecorAlpha(), effect);
			 }

			 final boolean ret = super.drawChild(canvas, child, drawingTime);

			 afterDrawTransformedChild(canvas, child, drawingTime, effect);

			 canvas.restore();

			 return ret;
		 } else {
			 beforeDrawTransformedChild(canvas, child, drawingTime, null);
			 final boolean ret = super.drawChild(canvas, child, drawingTime);
			 afterDrawTransformedChild(canvas, child, drawingTime, null);
			 if (mOverScrollIndex != -1 && getChildAt(mOverScrollIndex) == child) {
				 if (!RuntimeConfig.sLauncherInTouching) {
					 if (Math.abs(mOverScrollX) > Math.abs(mMaxOverScrollX)) {
						 mOverScrollX = mMaxOverScrollX;
					 }
					 if (mLastScrollX < 0) {
						 mOverScrollX = (int) ((getScrollX() * 1.0f / mLastScrollX) * mMaxOverScrollX);
					 } else {
						 mOverScrollX = (int) ((getScrollX() - (getChildCount() - 1) * mWidth) * 1.0f / (mLastScrollX - (getChildCount() - 1) * mWidth) * mMaxOverScrollX);
					 }
				 } else {
					 mMaxOverScrollX = mOverScrollX;
				 }
                 //drawScreenDecor(canvas, child, mOverScrollX * 1.0f / getWidth(), effect);
			 }
			 return ret;
		 }
	 }

	 @Override
	 public boolean isNeedScrollSlowlyOnTheVerge() {
		 EffectInfo effect = getCurrentEffectInfo(null);
		 if (effect == null || effect.type == EffectFactory.TYPE_CLASSIC) {
			 effect = null;
		 }
		 return effect == null;
	 }

	 protected void beforeDrawTransformedChild(Canvas canvas, View child, long drawingTime, EffectInfo info) {}

	 protected void afterDrawTransformedChild(Canvas canvas, View child, long drawingTime, EffectInfo info) {}

/*	 private void drawScreenDecor(Canvas canvas, View child, float alpha, EffectInfo effect) {
		 Drawable decor;
		 if (effect == null) {
			 if (alpha < 0) {
				 if (mRightDrawable == null) {
					 mRightDrawable = mContext.getResources().getDrawable(R.drawable.overscroll_glow_right);
				 }
				 decor = mRightDrawable;
				 alpha = -alpha;
			 } else {
				 if (mLeftDrawable == null) {
					 mLeftDrawable = mContext.getResources().getDrawable(R.drawable.overscroll_glow_left);
				 }
				 decor = mLeftDrawable;
			 }
			 if (alpha > 0.5f) {
				 alpha = 0.5f;
			 }
		 } else {
			 if (mScreenDecor == null) {
				 mScreenDecor = getResources().getDrawable(R.drawable.screen_preview);
			 }
			 decor = mScreenDecor;
			 if (alpha > 1f) {
				 alpha = 1f;
			 }
		 }
		 if (alpha < 0.001f) {
			 return;
		 }

		 //TODO:add by ssy
		 //原高度没有考虑ExtDraggingHeight;
		 decor.setBounds(calcDecorateRct(child));
		 decor.setAlpha((int) (alpha * 255f));
		 decor.draw(canvas);
	 }*/

	 protected Rect calcDecorateRct(View child) {
		 Rect rctRect = new Rect();
		 rctRect.left = child.getLeft();
		 rctRect.top = child.getTop() + ResourceUtils.getStatusBarHeightInResource(mContext) + CellLayout.getSmartTopPadding() /3;
		 rctRect.right = child.getRight();
		 rctRect.bottom = child.getBottom() - ((CellLayout) child).getBottomPadding();
		 return rctRect;
	 }

	 protected EffectInfo getChildTransformation(View childView, Transformation childTransformation) {
		 if (childView == null) {
			 return null;
		 }

		 final DragController dragController = mDragController;
		 if (dragController != null && dragController.isDragging()) {
			 return null;
		 }

		 // TODO: 重构：继承实现，不要直接和Workspace耦合
		 if (isInEditMode()/*Workspace.sInEditMode*/) {
			 return null;
		 }

		 //        if (childView instanceof AddScreen) {
		 //            return null;
		 //        }

		 EffectInfo effect = getCurrentEffectInfo(childView);

		 if (effect == null || effect.type == EffectFactory.TYPE_CLASSIC) {
			 return null;
		 }

		 int offset = getOffset(childView);
		 float radio = getCurrentScrollRadio(childView, offset);

		 if ((radio == 0 && !isScrolling()) || Math.abs(radio) > 1) {
			 return null;
		 }

		 effect = effect.getWorkspaceChildStaticTransformation(this, childView, childTransformation, radio, 0, this.mCurrentScreen, true) ? effect : null;

		 if (effect != null && effect.needInvalidateHardwareAccelerated() && ViewUtils.isHardwareAccelerated(this) && (RuntimeConfig.sLauncherInScrolling || RuntimeConfig.sLauncherInTouching)) {
			 childView.invalidate();
		 }

		 return effect;
	 }

	 @Override
	 protected boolean drawChildrenOrderByMoveDirection(View childView) {
		 final EffectInfo effect = getCurrentEffectInfo(childView);
		 return effect == null ? false : effect.drawChildrenOrderByMoveDirection();
	 }

	 private EffectInfo getCurrentEffectInfo(View childView) {
		 if (mCurrentEffectInfo != null) {
			 return mCurrentEffectInfo;
		 }

		 return EffectFactory.getEffectByType(mCurrentScreenTransitionType);
	 }

	 /**
	  * Return the current {@link CellLayout}, correctly picking the destination
	  * screen while a scroll is in progress.
	  */
	 public CellLayout getCurrentDropLayout() {
		 return (CellLayout) getChildAt(getCurrentDropLayoutIndex());
	 }

	 public int getCurrentDropLayoutIndex() {
		 return mScroller.isFinished() ? mCurrentScreen : mNextScreen;
	 }

	 @Override
	 protected boolean canScroll(int currentScreen, int direction) {
		 if (isLocked()) {
			 return false;
		 }

		 return true;
	 }

	 protected int getScrollScreen() {
		 if (mScroller.isFinished()) {
			 return mCurrentScreen;
		 } else {
			 return mNextScreen;
		 }
	 }

	 @Override
	 public void beforeScrollLeft() {
		 if(ignoreScorll()) return;

		 if (!canScroll(this.getScrollScreen(), DragController.SCROLL_LEFT)) {
			 return;
		 }

		 setMoveRightScreenBarVisiblity(false);
		 setMoveLeftScreenBarVisiblity(true);
	 }

	 @Override
	 public void beforeScrollRight() {
		 if(ignoreScorll()) return;

		 if (!canScroll(this.getScrollScreen(), DragController.SCROLL_RIGHT)) {
			 return;
		 }

		 setMoveLeftScreenBarVisiblity(false);
		 setMoveRightScreenBarVisiblity(true);
	 }

	 @Override
	 public void cancelScroll() {
		 if(ignoreScorll()) return;
		 setMoveLeftScreenBarVisiblity(false);
		 setMoveRightScreenBarVisiblity(false);
	 }

	 @Override
	 public boolean scrollLeft() {
		 if(ignoreScorll()) return false;
		 if (!canScroll(this.getScrollScreen(), DragController.SCROLL_LEFT)) {
			 return false;
		 }

		 if (mScroller.isFinished()) {
			 if (mCurrentScreen > 0) {
				 onScrollLeft(mCurrentScreen - 1, false);
				 return true;
			 } else {
				 onScrollLeft(this.getChildCount() - 1, true);
				 return true;
			 }
		 } else {
			 if (mNextScreen > 0) {
				 onScrollLeft(mNextScreen - 1, false);
				 return true;
			 } else {
				 onScrollLeft(this.getChildCount() - 1, true);
				 return true;
			 }
		 }
	 }

	 protected void onScrollLeft(int screen, boolean isSnapDirectly) {
		 scrollToScreen(screen, isSnapDirectly);
		 if (!canScroll(screen, DragController.SCROLL_LEFT)) {
			 setMoveLeftScreenBarVisiblity(false);
		 }
	 }

	 @Override
	 public boolean scrollRight() {
		 if(ignoreScorll()) return false;
		 if (!canScroll(this.getScrollScreen(), DragController.SCROLL_RIGHT)) {
			 return false;
		 }

		 if (mScroller.isFinished()) {
			 if (mCurrentScreen < getChildCount() - 1) {
				 onScrollRight(mCurrentScreen + 1, false);
				 return true;
			 } else {
				 onScrollRight(0, true);
				 return true;
			 }
		 } else {
			 if (mNextScreen < getChildCount() - 1) {
				 onScrollRight(mNextScreen + 1, false);
				 return true;
			 } else {
				 onScrollRight(0, true);
				 return true;
			 }
		 }
	 }

	 protected void onScrollRight(int screen, boolean isSnapDirectly) {
		 scrollToScreen(screen, isSnapDirectly);
		 if (!canScroll(screen, DragController.SCROLL_RIGHT)) {
			 setMoveRightScreenBarVisiblity(false);
		 }
	 }

	 public void onStart() {
		 loadScreenTransitionType();
	 }

	 public void onResume() {
	 }

	 public void onPause() {
		 handleTouchCancel();
	 }
	 
	 public void onStop() {
	 }

	 private void setMoveLeftScreenBarVisiblity(boolean show) {
		 if (mLauncher == null) {
			 return;
		 }
		 if (show) {
			 mLauncher.getDragLayer().showMoveLeftScreenBar();
		 } else {
			 mLauncher.getDragLayer().hideMoveLeftScreenBar();
		 }
	 }

	 private void setMoveRightScreenBarVisiblity(boolean show) {
		 if (mLauncher == null) {
			 return;
		 }
		 if (show) {
			 mLauncher.getDragLayer().showMoveRightScreenBar();
		 } else {
			 mLauncher.getDragLayer().hideMoveRightScreenBar();
		 }
	 }

	 @Override
	 public void onPageFrom(int currentPage) {
	     final boolean isScrolling = isScrolling();
	     if (isScrolling && !mIsKeepScrolling) {
	         onScrollStart(getScrollX(), getScrollY());
	     } else if (!isScrolling && mIsKeepScrolling) {
	         mIsKeepScrolling = false;
	     }
	 }

	 @Override
	 public void onPageTo(int currentPage) {
         if (!isScrolling() && !mIsKeepScrolling) {
             onScrollStop(getScrollX(), getScrollY());
         }
	 }

	 @Override
	 public void onPageMoving(int orignalPage, int currentPage, int direction) {
		 if (mScreenIndicator != null && !mTouchDownAbortScroll) { // 这里防止快滑未结束时，手再次滑动造成当前屏跳动，不影响慢划
			 mScreenIndicator.snapToScreenWithTouching(currentPage);
		 }
	 }

	 @Override
	 public void onPageSwitched(int previousPage, int currentPage) {
		 if (mCurrentEffectInfo != null) {
			 mCurrentEffectInfo.onEffectEnd(getScreenCount(), previousPage, currentPage);

			 // 4.0 球特效在这里最后一帧需要再次刷新一次
			 if (isEffectEnabled()
					 && mCurrentEffectInfo.needInvalidateHardwareAccelerated()
					 && ViewUtils.isHardwareAccelerated(this)) {
				 for (int i = 0; i < getChildCount(); i++) {
					 View v = getChildAt(i);
					 if (v == null) {
						 continue;
					 }
					 v.invalidate();
				 }
			 }

		 }

		 if (com.shouxinzm.launcher.util.DeviceUtils.isIceCreamSandwich()) {
			 for (int i = 0; i < getChildCount(); i++) {
				 View v = getChildAt(i);
				 if (v instanceof CellLayout) {
					 ((CellLayout) v).resetTransitionAlpha();
				 }
			 }
		 }

		 if (isEffectEnabled()) {
			 refreshCurrentScreenTransitionType();
		 }

		 if (mScreenIndicator != null) {
			 mScreenIndicator.snapToScreen(currentPage);
		 }
	 }
	 
    protected void onActionDown() {
    }
    
    protected void onActioUp() {
        mIsKeepScrolling = false;
    }
	 
	 protected void onScrollStart(int scrollX, int scrollY) {
	     mIsKeepScrolling = true;
	 }
	 
     protected void onScrollStop(int scrollX, int scrollY) {
     }
     
	 protected boolean isEffectEnabled() {
		 return !isInEditMode();
	 }

	 @Override
	 protected void dispatchDraw(Canvas canvas) {
		 if ((getScrollX() < 0 || getScrollX() > (getChildCount() - 1 ) * getWidth()) && !isCanLoopScreen()) {
			 if (getScrollX() < 0) {
				 mOverScrollIndex = 0;
			 } else {
				 mOverScrollIndex = getChildCount() - 1;
			 }
		 } else {
			 mOverScrollIndex = -1;
		 }
		 super.dispatchDraw(canvas);

		 refreshIndicatorPosition();
	 }

	 protected void refreshIndicatorPosition() {
		 if (mScreenIndicator != null) {
			 View cellLayout = getChildAt(mCurrentScreen);

			 if (cellLayout == null) {
				 return;
			 }

			 int offset = getOffset(cellLayout);
			 float radio = getCurrentScrollRadio(cellLayout, offset);

			 if (!mScreenIndicator.canSnapToPosition()) {
				 return;
			 }

			 boolean invalidate = true;
			 if ((radio == 0 && !isScrolling()) || Math.abs(radio) > 1) {
				 invalidate = false;
			 }

			 mScreenIndicator.snapToPosition(radio, mCurrentScreen, invalidate);
		 }
	 }

	 public void resetScreenTransformation() {
		 for (int i = getChildCount() - 1; i >= 0; i--) {
			 ((CellLayout) getChildAt(i)).resetTransitionAlpha();
		 }
	 }

	 public int[] adjustPosition(int[] position) {
		 if (Workspace.sInEditMode) {
			 int centerX = ScreenDimensUtils.getScreenWidth(getContext()) / 2;
			 int centerY = Utilities.dip2px(getContext(), RuntimeConfig.EDITMODE_TOP_OFFSET) + CellLayout.getSmartTopPadding();
			 position[0] = (int) Utils.scaleTo(position[0], centerX, Workspace.sEditModeScaleRatio);
			 position[1] = (int) Utils.scaleTo(position[1], centerY, Workspace.sEditModeScaleRatio);
		 }
		 return position;
	 }

	 public int getCurrentScreen(){
		 return mCurrentScreen;
	 }

	 public OnLongClickListener getLongClickListener(){
		 return mLongClickListener;
	 }

	 private boolean ignoreScorll() {    	
		 boolean fSecondLayerVisiable = false;
		 if (this instanceof Workspace) {
			 return fSecondLayerVisiable;
		 } else {
			 return false;
		 }
	 }
	 
	 public boolean isKeepScrolling() {
	     return mIsKeepScrolling;
	 }
}
