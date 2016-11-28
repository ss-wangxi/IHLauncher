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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewParent;
import cc.snser.launcher.apps.model.ItemInfo;
import cc.snser.launcher.ui.effects.EffectFactory;
import cc.snser.launcher.ui.effects.EffectInfo;
import cc.snser.launcher.ui.effects.SymmetricalLinearTween;
import cc.snser.launcher.ui.utils.Utilities;
import cc.snser.launcher.util.LauncherAnimUtils;
import cc.snser.launcher.util.ResourceUtils;

import com.btime.launcher.adapter.ChannelRecognizeUtils;
import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.support.v4.util.AlphaGroup;
import com.shouxinzm.launcher.support.v4.util.AnimationUtils;
import com.shouxinzm.launcher.support.v4.util.ViewUtils;
import com.shouxinzm.launcher.util.DeviceUtils;
import com.shouxinzm.launcher.util.ScreenDimensUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Stack;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

public abstract class CellLayout extends AlphaGroup implements EffectInfo.Callback {

    private static final String TAG = "Launcher.CellLayout";
    
    protected int mCellWidth;
    protected int mCellHeight;
    
    protected long mFolderId = -1;

    protected int mLongAxisStartPadding;
    protected int mLongAxisEndPadding;

    protected int mShortAxisStartPadding;
    protected int mShortAxisEndPadding;

    protected int mShortAxisCells;
    protected int mLongAxisCells;

    protected int mWidthGap;
    protected int mHeightGap;
    
    protected boolean mBuildDragingCache;
    protected boolean mItemPlacementDirty = false;

    protected int[] mCellXY = new int[2];
    protected boolean[][] mOccupied;
    protected boolean[][] mTmpOccupied;

    private RectF mDragRect = new RectF();
    
    private ArrayList<View> mIntersectingViews = new ArrayList<View>();
    protected Rect mOccupiedRect = new Rect();
    
    protected boolean mIsModifyVPadding;
    protected boolean mIsModifyHPadding;

    private boolean mEffectEabled = true;

    private boolean mReverseRow = false;

    private int[] mAxis;
    
    private static final int REORDER_ANIMATION_DURATION = 150;
    protected HashMap<CellLayout.LayoutParams, SymmetricalLinearTween> mReorderAnimators = new HashMap<CellLayout.LayoutParams, SymmetricalLinearTween>();
    protected HashMap<View, ReorderHintAnimation>
    			mShakeAnimators = new HashMap<View, ReorderHintAnimation>();
    private float mReorderHintAnimationMagnitude;
    
    protected static int mChildWidth = 0;
    protected static int mChildHeight = 0;
    //顶部的高度
    private static int mTopPadding = 0;
    public static int getSmartTopPadding(){
    	return mTopPadding;
    }
    public static int refreshSmartTopPadding(){
    	//现在Launch强制显示Statusbar(默认状态)，根据Android版本（是否有透明的Statusbar）， 以决定偏移
    	mTopPadding = (LauncherSettings.isEnableStatusBarAutoTransparent() || LauncherSettings.isEnableStatusBarAutoTransparentV2()) ? 
    			ResourceUtils.getStatusBarHeightEx(App.getApp().getLauncher()) : 0;
    	
    	//根据是否全屏来判断决定是否再多增加一个额外的信息
    	if(ScreenDimensUtils.isFullScreen(App.getApp().getLauncher())){
    		int statusBarHeight = 0;
            if (!LauncherSettings.isEnableStatusBarAutoTransparent() && !LauncherSettings.isEnableStatusBarAutoTransparentV2()) {
                statusBarHeight = ResourceUtils.getStatusBarHeight(App.getApp().getLauncher());
            }
    		mTopPadding += statusBarHeight;
    	}
    	
        //横屏下不算状态栏高度 added by snsermail@gmail.com
        if (App.getApp().isScreenLandscape()) {
            mTopPadding = 0;
        }
    	
    	return mTopPadding;
    }

    public CellLayout(Context context) {
        this(context, null);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);

        mCellWidth = a.getDimensionPixelSize(R.styleable.CellLayout_cellWidth, 10);
        mCellHeight = a.getDimensionPixelSize(R.styleable.CellLayout_cellHeight, 10);

        mLongAxisStartPadding = a.getDimensionPixelSize(R.styleable.CellLayout_longAxisStartPadding, 10);
        mLongAxisEndPadding = a.getDimensionPixelSize(R.styleable.CellLayout_longAxisEndPadding, 10);
        mShortAxisStartPadding = a.getDimensionPixelSize(R.styleable.CellLayout_shortAxisStartPadding, 10);
        mShortAxisEndPadding = a.getDimensionPixelSize(R.styleable.CellLayout_shortAxisEndPadding, 10);

        int shortAxisCells = a.getInt(R.styleable.CellLayout_shortAxisCells, 0);
        int longAxisCells = a.getInt(R.styleable.CellLayout_longAxisCells, 0);

        a.recycle();

        if (shortAxisCells > 0 && longAxisCells > 0) {
            mAxis = new int[2];
            mAxis[0] = shortAxisCells;
            mAxis[1] = longAxisCells;
        }

        setAlwaysDrawnWithCacheEnabled(false);

        resetLayout();
    }
    
    public void setBuildDragingCache(boolean setDragingCache){
    	mBuildDragingCache = setDragingCache;
    	if(mBuildDragingCache){
    		setWillNotDraw(false);
    	}else{
    		setWillNotDraw(true);
    	}
    }

    public void setEffectEnabled(boolean enabled) {
        mEffectEabled = enabled;
    }

    public boolean getEffectEnabled() {
        return mEffectEabled;
    }

    public abstract int[] getLayout();

    public boolean resetLayout() {
        boolean ret = false;

        int[] layout = mAxis != null ? mAxis : getLayout();

        int shortAxisCells = layout[1];
        int longAxisCells = layout[0];

        if (shortAxisCells != mShortAxisCells || longAxisCells != mLongAxisCells) {
            mShortAxisCells = shortAxisCells;
            mLongAxisCells = longAxisCells;

            ret = true;
        }

        if (mOccupied == null || mOccupied.length != mShortAxisCells
                || mOccupied[0].length != mLongAxisCells) {
            mOccupied = new boolean[mShortAxisCells][mLongAxisCells];
            mTmpOccupied = new boolean[mShortAxisCells][mLongAxisCells];
        }
        
        int oldCountMax = this.getCountMax();
        if (oldCountMax < this.getCountMax()) {
            if (mTempRectStack != null) {
                this.mTempRectStack.clear();
            }
        }

        return ret;
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child == null) {
            return true;
        }

        final Boolean result = mEffectEabled ? applyChildTransformation(canvas, child, drawingTime) : null;
        
        if (result != null) {
            return result.booleanValue();
        } else {
        	if (mBuildDragingCache) {
        		canvas.save();
				canvas.translate(0,
						Math.max(0, Utilities.dip2px(getContext(), RuntimeConfig.EDITMODE_TOP_OFFSET) - CellLayout.getSmartTopPadding()) 
						);
			}
            boolean drawResult = super.drawChild(canvas, child, drawingTime);
            if (mBuildDragingCache) {
				canvas.restore();
			}
            return drawResult;
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        final int countX = getCountX();
        final int totalLine = (childCount / countX) + (childCount % countX == 0 ? 0 : 1); // 实际总行数
        final int originalLine = i / countX; // 原来的行数
        final int originalRow = i % countX; // 原来的列数
        final int lastLineRowNum = countX - (totalLine * countX - childCount); // 最后一行有几个元素

        int currentLine = getCurrentLineNum(originalLine, totalLine); // 现在所在行数
        int currentRow = 0; // 现在所在行数

        final boolean reverseRow = mReverseRow;
        if (reverseRow) {
            currentRow = Math.min(childCount - countX * currentLine, countX) - 1 - originalRow;
        } else {
            currentRow = originalRow;
        }

        if (currentLine == totalLine - 1 && lastLineRowNum < countX && originalRow >= lastLineRowNum) {
            currentLine = getCurrentLineNum(totalLine - 1, totalLine);

            if (reverseRow) {
                currentRow = Math.min(childCount - countX * currentLine, countX) - 1 - originalRow;
            }
        } else if (originalLine == totalLine - 1) {
            currentLine = getCurrentLineNum(originalLine, totalLine);

            if (reverseRow) {
                currentRow = originalRow + (countX - lastLineRowNum);
                currentRow = Math.min(childCount - countX * currentLine, countX) - 1 - originalRow;
            }
        }

        if (LOGD_ENABLED) {
            XLog.d(TAG, "getChildDrawingOrder i " + i + " childCount " + childCount + " originalLine " + originalLine + " originalRow " + originalRow + " currentLine " + currentLine + " currentRow "
                    + currentRow + " lastLineRowNum " + lastLineRowNum + " totalLine " + totalLine + " reverse " + reverseRow);
        }
        return currentLine * countX + currentRow;
    }

    private int getCurrentLineNum(int originalLine, int totalLineCount) {
        int currentLine = 0;

        if (originalLine % 2 != 0) {
            currentLine = originalLine / 2;
        } else {
            currentLine = totalLineCount - 1 - originalLine / 2;
        }

        return currentLine;
    }

    /**
     * 翻转行顺序
     * @param reverse
     */
    public void reverseChildDrawingRowOrder(boolean reverse) {
        this.mReverseRow = reverse;
    }

    protected Boolean applyChildTransformation(Canvas canvas, View childView, long drawingTime) {
        if (this.getParent() instanceof AbstractWorkspace) {
            AbstractWorkspace workspace = (AbstractWorkspace) this.getParent();

            if (childView == null) {
                return null;
            }

            if (workspace.getDragController() != null && workspace.getDragController().isDragging()) {
                return null;
            }

            if (/*Workspace.sInEditMode*/workspace.isInEditMode()) {
                return null;
            }

            if (childView.getTag() instanceof ItemInfo) {
                final int screenTransitionType = workspace.getCurrentScreenTransitionType();
                final EffectInfo effect = EffectFactory.getEffectByType(screenTransitionType);

                if (effect == null) {
                    return null;
                }

                final int offset = workspace.getOffset(this);
                final float radio = workspace.getCurrentScrollRadio(this, offset);
                final float radioY = workspace.getMotionYRadio();

                if ((radio == 0 && !workspace.isScrolling()) || Math.abs(radio) > 1) {
                    return null;
                }

                if (effect.drawChildrenOrderByMoveDirection()) {
                    setChildrenDrawingOrderEnabled(true);
                } else {
                    setChildrenDrawingOrderEnabled(false);
                }

                return effect.applyCellLayoutChildTransformation(this, canvas, childView, drawingTime, this, radio, offset, radioY, workspace.getCurrentScreen(), true);
            }
        }

        return null;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    public int getCountX() {
        return mShortAxisCells;
    }

    public int getCountY() {
        return mLongAxisCells;
    }

    public int getCountMax() {
        return getCountX() * getCountY();
    }

    public void addView(View child, int index, ViewGroup.LayoutParams params, boolean invalidate) {
        // Generate an id for each view, this assumes we have at most 256x256 cells
        // per workspace screen
        final LayoutParams cellParams = (LayoutParams) params;
        cellParams.regenerateId = true;

        if (invalidate) {
            this.addView(child, index, params);
        } else {
            this.addViewInLayout(child, index, params);
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }

    /**
     * Given a point, return the cell that strictly encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    public void pointToCellExact(int x, int y, int[] result) {
        final int hStartPadding = this.getLeftPadding();
        final int vStartPadding = this.getTopPadding();

        result[0] = (x - hStartPadding) / (mCellWidth + mWidthGap);
        result[1] = (y - vStartPadding) / (mCellHeight + mHeightGap);

        final int xAxis = this.getCountX();
        final int yAxis = this.getCountY();

        if (result[0] < 0) {
            result[0] = 0;
        }
        if (result[0] >= xAxis) {
            result[0] = xAxis - 1;
        }
        if (result[1] < 0) {
            result[1] = 0;
        }
        if (result[1] >= yAxis) {
            result[1] = yAxis - 1;
        }
        if (result.length > 2) {
            int cellX = hStartPadding + result[0] * (mCellWidth + mWidthGap);
            int cellY = vStartPadding + result[1] * (mCellHeight + mHeightGap);

            if (x >= cellX + (mCellWidth) / 3 && x <= cellX + (mCellWidth) * 2 / 3
                    && y >= cellY && y <= cellY + mCellHeight) {
                result[2] = Constant.ITEM_AREA_MID;
            } else if (x <= cellX + (mCellWidth) / 3) {
                result[2] = Constant.ITEM_AREA_LEFT;
            } else if (x >= cellX + (mCellWidth) * 2 / 3) {
                result[2] = Constant.ITEM_AREA_RIGHT;
            } else {
                result[2] = Constant.ITEM_AREA_UNKOWN;
            }
        }
    }

    /**
     * Given a point, return the cell that most closely encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    public void pointToCellRounded(int x, int y, int[] result) {
        pointToCellExact(x + (mCellWidth / 2), y + (mCellHeight / 2), result);
    }

    /**
     * Given a cell coordinate, return the point that represents the upper left corner of that cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    public void cellToPoint(int cellX, int cellY, int spanX, int spanY, int[] result) {
        int hStartPadding = getLeftPadding();
        int vStartPadding = getTopPadding();
        if (cellX == 0 && spanX == this.getCountX()) {
            hStartPadding = 0;
        }
        result[0] = hStartPadding + cellX * (mCellWidth + mWidthGap);
        result[1] = vStartPadding + cellY * (mCellHeight + mHeightGap);
    }

    public int getCellWidth() {
        return mCellWidth;
    }

    public int getCellHeight() {
        return mCellHeight;
    }

    public int getItemWidthForSpan(int spanX) {
        if (spanX <= 0) {
            return 0;
        }

        int hStartPadding = getLeftPadding();
        int hEndPadding = getRightPadding();

        if (spanX == this.getCountX()) {
            hStartPadding = hEndPadding = 0;
            return (this.getMeasuredWidth() == 0 ? ScreenDimensUtils.getScreenShortAxisWidth(this.getContext()) : this.getMeasuredWidth())
                    - hStartPadding - hEndPadding;
        } else {
            return mCellWidth * spanX + mWidthGap * (spanX - 1);
        }
    }

    public int getItemHeightForSpan(int spanY) {
        if (spanY <= 0) {
            return 0;
        }

        int vStartPadding = getTopPadding();
        int vEndPadding = getBottomPadding();

        if (spanY == this.getCountY()) {
            return (this.getMeasuredHeight() == 0 ? ScreenDimensUtils.getScreenLongAxisWidth(this.getContext()) : this.getMeasuredHeight())
                    - vStartPadding - vEndPadding;
        } else {
            return mCellHeight * spanY + mHeightGap * (spanY - 1);
        }
    }

    public int getShortAxisStartPadding() {
        return mShortAxisStartPadding;
    }

    public int getLongAxisStartPadding() {
        return mLongAxisStartPadding;
    }

    public int getLeftPadding() {
    	int leftPadding = getShortAxisStartPadding();
    	return leftPadding;
    }

    public int getTopPadding() {
        return getLongAxisStartPadding();
    }

    public int getShortAxisEndPadding() {
        return mShortAxisEndPadding;
    }

    public int getLongAxisEndPadding() {
        return mLongAxisEndPadding;
    }

    public int getRightPadding() {
    	int rightPadding = getShortAxisEndPadding();
        return rightPadding;
    }

    public int getBottomPadding() {
        return getLongAxisEndPadding();
    }

    public int getCellLongAxisDistance() {
        return mHeightGap + mCellHeight;
    }

    public int getCellShortAxisDistance() {
        return mWidthGap + mCellWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	// TODO: currently ignoring padding
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        
        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }
        
        //碰到竖屏的情况就不处理了 added by snsermail@gmail.com
        if (!App.getApp().isScreenLandscape() || getChildCount() <= 0 || widthSpecSize < heightSpecSize) {
            setMeasuredDimension(widthSpecSize, heightSpecSize);
            return;
        }
        
/*        Log.d("Snser", "CellLayout onMeasure sw=" + widthSpecSize + " sh=" + heightSpecSize);
        Log.d("Snser", "CellLayout onMeasure padding l=" + getLeftPadding() + " t=" + getPaddingTop() + " r=" + getRightPadding() + " b=" + getBottomPadding());
        Log.d("Snser", "CellLayout onMeasure cw=" + mCellWidth + " ch=" + mCellHeight + " gw=" + mWidthGap + " gh=" + mHeightGap);*/
    	
        //计算并修正横向、纵向单元格之间的间距，避免出现单元格区域交错
        calculateGap(widthSpecSize, heightSpecSize);
        
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            
            lp.setup(this.getCountX(), this.getCountY(), widthSpecSize, heightSpecSize, getCellWidth(),
                    getCellHeight(), getWidthGap(), getHeightGap(), getLeftPadding(), getTopPadding(),
                    getRightPadding(), getBottomPadding());

            if (lp.regenerateId) {
                child.setId(((getId() & 0xFF) << 16) | (lp.cellX & 0xFF) << 8 | (lp.cellY & 0xFF));
                lp.regenerateId = false;
            }

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            int childheightMeasureSpec =
                    MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            child.measure(childWidthMeasureSpec, childheightMeasureSpec);
        }

        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }
    
    private int getTotalGap(int size,boolean isVertical){
    	if(isVertical){
    		return size - getLongAxisStartPadding() - getLongAxisEndPadding() - (mCellHeight * mLongAxisCells);
    	}else {
			return size - getShortAxisStartPadding() - getShortAxisEndPadding() - (mCellWidth * mShortAxisCells);
		}
    }
    
    private boolean handleVirtualKeyAction(int heightSpecSize){
    	return false;
    }
    
    private int modifyVerticalGap(int totalGap){
    	final int numLongGaps = mLongAxisCells - 1;
    	int heigtGap = 0;
    	if(numLongGaps <= 0) return heigtGap;

    	heigtGap = totalGap/numLongGaps;
    	if(heigtGap >= 0 || mIsModifyVPadding) return heigtGap;

    	//改为修改单元格高度
    	final int delta = (int)(totalGap * 1.0f /mLongAxisCells + 0.5);
    	mCellHeight += delta;
    	mIsModifyVPadding = true;
    	heigtGap = 0;

    	return heigtGap;
    }

    private int modifyHorizonGap(int totalGap){
    	final int numShortGaps = mShortAxisCells - 1;
    	int widthGap = 0;
    	if(numShortGaps <= 0) return widthGap;

    	widthGap = totalGap/numShortGaps;
    	if(widthGap >= 0 || mIsModifyHPadding) return widthGap;

    	//改为修改单元格宽度
    	final int delta = (int)(totalGap * 1.0f /mShortAxisCells + 0.5);
    	mCellWidth += delta;
    	mIsModifyHPadding = true;
    	widthGap = 0;

    	return widthGap;
    }

    private void calculateGap(int widthSpecSize ,int heightSpecSize){
    	int verticalTotalGap = getTotalGap(heightSpecSize,true);
    	int horizonTotalGap = getTotalGap(widthSpecSize, false);

    	if(!handleVirtualKeyAction(heightSpecSize)){
    		mHeightGap = modifyVerticalGap(verticalTotalGap);	
    	}
    	mWidthGap  = modifyHorizonGap(horizonTotalGap);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

                int childLeft = lp.x;
                int childTop = lp.y;
                child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);

                if (lp.dropped) {
                    lp.dropped = false;

                    final int[] cellXY = mCellXY;
                    getLocationOnScreen(cellXY);

                    /*try {
                        mWallpaperManager.sendWallpaperCommand(getWindowToken(), "android.home.drop",
                                cellXY[0] + childLeft + lp.width / 2,
                                cellXY[1] + childTop + lp.height / 2, 0, null);
                    } catch (Exception e) {
                        // ignore
                    }*/
                }
            }
        }
    }

    /**
     * Drop a child at the specified position
     *
     * @param child The child that is being dropped
     * @param targetXY Destination area to move to
     */
    public void onDropChild(View child, int[] targetXY) {
        onDropChild(child, targetXY, false);
    }

    /**
     * caller is responsible for calling {@link#requestLayout()} and {@link#invalidate()}
     * @see #onDropChild(View, int[])
     * */
    public void onDropChild(View child, int[] targetXY, boolean preventRefresh) {
        if (child != null && targetXY != null) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.cellX = lp.tmpCellX = targetXY[0];
            lp.cellY = lp.tmpCellY = targetXY[1];
            lp.isDragging = false;
            lp.dropped = true;
            lp.isLockedToGrid = true;
            mDragRect.setEmpty();
            if (!preventRefresh) {
                child.requestLayout();
                invalidate();
            }
        }
    }

    public void onDropAborted(View child) {
        if (child != null) {
            ((LayoutParams) child.getLayoutParams()).isDragging = false;
            child.requestLayout();
            invalidate();
        }
        mDragRect.setEmpty();
    }

    /**
     * Start dragging the specified child
     *
     * @param child The child that is being dragged
     */
    public void onDragChild(View child) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        lp.isDragging = true;
        mDragRect.setEmpty();
    }
    
    @Override
    protected void onDraw(Canvas canvas){
    	if(mBuildDragingCache){
    		
    		canvas.save();
    		canvas.translate(0, 
    				Math.max(0, Utilities.dip2px(getContext(), RuntimeConfig.EDITMODE_TOP_OFFSET) - CellLayout.getSmartTopPadding()) );
    		
    		;
    	}
    	
    	super.onDraw(canvas);
    	
    	if(mBuildDragingCache){
    		canvas.restore();
    	}
    }
    
    public void onDragSelf(boolean drag){
    	
    }

    /**
     * Computes the required horizontal and vertical cell spans to always
     * fit the given rectangle.
     *
     * @param width Width in pixels
     * @param height Height in pixels
     */
    public int[] rectToCell(int width, int height) {
        // Always assume we're working with the smallest span to make sure we
        // reserve enough space in both orientations.
        //final Resources resources = getResources();
        int actualWidth = mCellWidth;//resources.getDimensionPixelSize(R.dimen.workspace_cell_width);
        int actualHeight = mCellHeight;//resources.getDimensionPixelSize(R.dimen.workspace_cell_height);
        int smallerSize = Math.min(actualWidth, actualHeight);

        // Always round up to next largest cell
        int spanX = Math.max(1, (width + smallerSize) / smallerSize);
        int spanY = Math.max(1, (height + smallerSize) / smallerSize);

        return new int[] {
            spanX, spanY
        };
    }
    
    public static int[] rectToCell(Resources resources, int width, int height) {
    	
    	int actualWidth = mChildWidth;//resources.getDimensionPixelSize(R.dimen.workspace_cell_width);
        int actualHeight = mChildHeight;//resources.getDimensionPixelSize(R.dimen.workspace_cell_height);
        int smallerSize = Math.min(actualWidth, actualHeight);

        // Always round up to next largest cell
        int spanX = Math.max(1, (width + smallerSize) / smallerSize);
        int spanY = Math.max(1, (height + smallerSize) / smallerSize);

        return new int[] {
            spanX, spanY
        };
    }

    protected void findOccupiedCells(int xCount, int yCount, boolean[][] occupied, View ignoreView) {
        findOccupiedCells(xCount, yCount, occupied, ignoreView, true);
    }

    protected void findOccupiedCells(int xCount, int yCount, boolean[][] occupied, View ignoreView, boolean ignoreDraggingView) {
        for (int y = 0; y < yCount; y++) {
            for (int x = 0; x < xCount; x++) {
                occupied[x][y] = false;
            }
        }

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if ( child.equals(ignoreView)) {
                continue;
            }

            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.isDragging) {
            	continue;
            }

            for (int x = lp.cellX; x < lp.cellX + lp.cellHSpan && x < xCount; x++) {
                for (int y = lp.cellY; y < lp.cellY + lp.cellVSpan && y < yCount; y++) {
                    if (x >= 0 && x < xCount && y >= 0 && y < yCount) {
                        occupied[x][y] = true;
                    }
                }
            }
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CellLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CellLayout.LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new CellLayout.LayoutParams(p);
    }

    public ArrayList<CellInfo> getSingleVacantCells() {
        final int xCount = getCountX();
        final int yCount = getCountY();
        final boolean[][] occupied = mOccupied;

        findOccupiedCells(xCount, yCount, occupied, null);

        ArrayList<CellInfo> ret = new ArrayList<CellInfo>();

        for (int y = 0; y < yCount; y++) {
            for (int x = 0; x < xCount; x++) {
                boolean available = !occupied[x][y];

                if (available) {
                    CellInfo cellInfo = new CellInfo();

                    cellInfo.cellX = x;
                    cellInfo.cellY = y;
                    cellInfo.spanY = 1;
                    cellInfo.spanX = 1;

                    ret.add(cellInfo);
                }
            }
        }

        return ret;
    }

    public View getCellView(int cellX, int cellY) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View childView = getChildAt(i);

            LayoutParams lp = (LayoutParams) childView.getLayoutParams();
            if (lp.isDragging) {
                continue;
            }

            if (cellX >= lp.cellX && cellX < lp.cellX + lp.cellHSpan
                    && cellY >= lp.cellY && cellY < lp.cellY + lp.cellVSpan) {
                return childView;
            }
        }
        return null;
    }

    public void hideAll() {
        int count = this.getChildCount();
        for (int i = 0; i < count; i++) {
            this.getChildAt(i).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Given a cell coordinate, return the point that represents the upper left corner of that cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    public void cellToPoint(int cellX, int cellY, int[] result) {
        final int hStartPadding = getLeftPadding();
        final int vStartPadding = getTopPadding();
        result[0] = hStartPadding + cellX * (mCellWidth + mWidthGap);
        result[1] = vStartPadding + cellY * (mCellHeight + mHeightGap);
    }

    public void showAll() {
        int count = this.getChildCount();
        for (int i = 0; i < count; i++) {
        	final View child = getChildAt(i);
        	if (!((LayoutParams)child.getLayoutParams()).isDragging) {
        		child.setVisibility(View.VISIBLE);
            }
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        /**
         * Horizontal location of the item in the grid.
         */
        @ViewDebug.ExportedProperty
        public int cellX;

        /**
         * Vertical location of the item in the grid.
         */
        @ViewDebug.ExportedProperty
        public int cellY;

        /**
         * Temporary horizontal location of the item in the grid during reorder
         */
        public int tmpCellX;

        /**
         * Temporary vertical location of the item in the grid during reorder
         */
        public int tmpCellY;

        /**
         * Indicates that the temporary coordinates should be used to layout the items
         */
        public boolean useTmpCoords;

        /**
         * Number of cells spanned horizontally by the item.
         */
        @ViewDebug.ExportedProperty
        public int cellHSpan;

        /**
         * Number of cells spanned vertically by the item.
         */
        @ViewDebug.ExportedProperty
        public int cellVSpan;

        /**
         * Indicates whether the item will set its x, y, width and height parameters freely,
         * or whether these will be computed based on cellX, cellY, cellHSpan and cellVSpan.
         */
        public boolean isLockedToGrid = true;

        /**
         * Indicates whether this item can be reordered. Always true except in the case of the
         * the AllApps button.
         */
        public boolean canReorder = true;

        /**
         * Is this item currently being dragged
         */
        public boolean isDragging;

        // X coordinate of the view in the layout.
        @ViewDebug.ExportedProperty
        public
        int x;
        // Y coordinate of the view in the layout.
        @ViewDebug.ExportedProperty
        public
        int y;

        public boolean regenerateId;

        public boolean dropped;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(int cellX, int cellY, int cellHSpan, int cellVSpan) {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            this.cellX = cellX;
            this.cellY = cellY;
            this.cellHSpan = cellHSpan;
            this.cellVSpan = cellVSpan;
        }

        public void setup(int countX, int countY, int widthSpecSize, int heightSpecSize,
                int cellWidth, int cellHeight, int widthGap, int heightGap, int hStartPadding,
                int vStartPadding, int hEndPadding, int vEndPadding) {
            if (isLockedToGrid) {
                final int myCellHSpan = cellHSpan;
                final int myCellVSpan = cellVSpan;
                final int myCellX = useTmpCoords ? tmpCellX : cellX;
                final int myCellY = useTmpCoords ? tmpCellY : cellY;

                if (myCellX == 0 && cellHSpan == countX) {
//                    hStartPadding = hEndPadding = 0;
                    width = widthSpecSize - hStartPadding - hEndPadding - leftMargin - rightMargin;
                } else {
                    width = myCellHSpan * cellWidth + ((myCellHSpan - 1) * widthGap) - leftMargin - rightMargin;
                }
                if (myCellY == 0 && cellVSpan == countY) {
                    height = heightSpecSize - vStartPadding - vEndPadding - topMargin - bottomMargin;
                } else {
                    height = myCellVSpan * cellHeight + ((myCellVSpan - 1) * heightGap) - topMargin - bottomMargin;
                }

                x = hStartPadding + myCellX * (cellWidth + widthGap) + leftMargin;
                y = vStartPadding + myCellY * (cellHeight + heightGap) + topMargin;
            }
        }
    }

    public static final class CellInfo implements ContextMenu.ContextMenuInfo {
        private View cell;
        public int cellX = -1;
        public int cellY = -1;
        public int spanX;
        public int spanY;
        public int screen;
        public boolean valid;

        public View getCell() {
            return cell;
        }

        public void setCell(View view){
        	cell = view;
        }
        
        @Override
        public String toString() {
            return "Cell[view=" + (cell == null ? "null" : cell.getClass()) + ", x=" + cellX +
                    ", y=" + cellY + "]";
        }
    }

    public boolean lastDownOnOccupiedCell() {
        return true;
    }

    // ----- code for shake icons ----
    protected final int[] mTmpXY = new int[2];
    protected final int[] mTmpPoint = new int[2];
    protected int[] mTempLocation = new int[2];

    public void onDragEnter() {
        // do nothing now
    }

    public void onDragExit() {
        // do nothing now
    }

    public void setupLp(CellLayout.LayoutParams lp) {
        lp.setup(this.getCountX(), this.getCountY(), this.getMeasuredWidth(), this.getMeasuredHeight(), mCellWidth,
                mCellHeight, mWidthGap, mHeightGap, getLeftPadding(), getTopPadding(),
                getRightPadding(), getBottomPadding());
    }

    /**
     * Finds the upper-left coordinate of the first rectangle in the grid that can
     * hold a cell of the specified dimensions. If intersectX and intersectY are not -1,
     * then this method will only return coordinates for rectangles that contain the cell
     * (intersectX, intersectY)
     *
     * @param cellXY The array that will contain the position of a vacant cell if such a cell
     *               can be found.
     * @param spanX The horizontal span of the cell we want to find.
     * @param spanY The vertical span of the cell we want to find.
     *
     * @return True if a vacant cell of the specified dimension was found, false otherwise.
     */
    public boolean findCellForSpan(int[] cellXY, int spanX, int spanY) {
        return findCellForSpanThatIntersectsIgnoring(cellXY, spanX, spanY, -1, -1, null, mOccupied);
    }

    /**
     * Like above, but if intersectX and intersectY are not -1, then this method will try to
     * return coordinates for rectangles that contain the cell [intersectX, intersectY]
     *
     * @param spanX The horizontal span of the cell we want to find.
     * @param spanY The vertical span of the cell we want to find.
     * @param ignoreView The home screen item we should treat as not occupying any space
     * @param intersectX The X coordinate of the cell that we should try to overlap
     * @param intersectX The Y coordinate of the cell that we should try to overlap
     *
     * @return True if a vacant cell of the specified dimension was found, false otherwise.
     */
    public boolean findCellForSpanThatIntersects(int[] cellXY, int spanX, int spanY,
            int intersectX, int intersectY) {
        return findCellForSpanThatIntersectsIgnoring(
                cellXY, spanX, spanY, intersectX, intersectY, null, mOccupied);
    }
    
    int[] mTempCellXY = new int[2];
    /**
     * 当前当前屏幕是否还有足够的位置放到指定的Cell
     * @param spanX
     * @param spanY
     * @param intersectX
     * @param intersectY
     * @return
     */
    public boolean hasFreeCell(int spanX, int spanY,int intersectX, int intersectY ){
    	return findCellForSpanThatIntersectsIgnoring(
    				mTempCellXY, spanX, spanY, intersectX, intersectY, null, mOccupied);
    }

    /**
     * The superset of the above two methods
     */
    public boolean findCellForSpanThatIntersectsIgnoring(int[] cellXY, int spanX, int spanY,
            int intersectX, int intersectY, View ignoreView, boolean occupied[][]) {
        findOccupiedCells(this.getCountX(), this.getCountY(), occupied, ignoreView);

        // mark space take by ignoreView as available (method checks if ignoreView is null)
        // markCellsAsUnoccupiedForView(ignoreView, occupied);

        boolean foundCell = false;
        while (true) {
            int startX = 0;
            int endX = this.getCountX() - (spanX - 1);
            if (intersectX >= 0) {
                // startX = Math.max(startX, intersectX - (spanX - 1));
                // endX = Math.min(endX, intersectX + (spanX - 1) + (spanX == 1 ? 1 : 0));
                startX = Math.max(startX, intersectX);
                endX = Math.min(endX, intersectX + 1);
            }
            int startY = 0;
            int endY = this.getCountY() - (spanY - 1);
            if (intersectY >= 0) {
                // startY = Math.max(startY, intersectY - (spanY - 1));
                // endY = Math.min(endY, intersectY + (spanY - 1) + (spanY == 1 ? 1 : 0));
                startY = Math.max(startY, intersectY);
                endY = Math.min(endY, intersectY + 1);
            }

            for (int y = startY; y < endY && !foundCell; y++) {
                inner:
                for (int x = startX; x < endX; x++) {
                    for (int i = 0; i < spanX; i++) {
                        for (int j = 0; j < spanY; j++) {
                            if (occupied[x + i][y + j]) {
                                // small optimization: we can skip to after the column we just found
                                // an occupied cell
                                x += i;
                                continue inner;
                            }
                        }
                    }
                    if (cellXY != null) {
                        cellXY[0] = x;
                        cellXY[1] = y;
                    }
                    foundCell = true;
                    break;
                }
            }
            if (intersectX == -1 && intersectY == -1) {
                break;
            } else {
                // if we failed to find anything, try again but without any requirements of
                // intersecting
                intersectX = -1;
                intersectY = -1;
                continue;
            }
        }

        // re-mark space taken by ignoreView as occupied
        // markCellsAsOccupiedForView(ignoreView, occupied);
        return foundCell;
    }

    protected void markCellsForView(int cellX, int cellY, int spanX, int spanY, boolean[][] occupied,
            boolean value) {
        if (cellX < 0 || cellY < 0) {
            return;
        }
        for (int x = cellX; x < cellX + spanX && x < getCountX(); x++) {
            for (int y = cellY; y < cellY + spanY && y < getCountY(); y++) {
                occupied[x][y] = value;
            }
        }
    }

    protected void cellToCenterPoint(int cellX, int cellY, int[] result) {
        regionToCenterPoint(cellX, cellY, 1, 1, result);
    }

    public void regionToCenterPoint(int cellX, int cellY, int spanX, int spanY, int[] result) {
        final int hStartPadding = getLeftPadding();
        final int vStartPadding = getTopPadding();
        result[0] = hStartPadding + cellX * (mCellWidth + mWidthGap) +
                (spanX * mCellWidth + (spanX - 1) * mWidthGap) / 2;
        result[1] = vStartPadding + cellY * (mCellHeight + mHeightGap) +
                (spanY * mCellHeight + (spanY - 1) * mHeightGap) / 2;
    }

    protected void regionToRect(int cellX, int cellY, int spanX, int spanY, Rect result) {
        final int hStartPadding = getLeftPadding();
        final int vStartPadding = getTopPadding();
        final int left = hStartPadding + cellX * (mCellWidth + mWidthGap);
        final int top = vStartPadding + cellY * (mCellHeight + mHeightGap);
        result.set(left, top, left + (spanX * mCellWidth + (spanX - 1) * mWidthGap),
                top + (spanY * mCellHeight + (spanY - 1) * mHeightGap));
    }

    public float getDistanceFromCell(float x, float y, int[] cell) {
        cellToCenterPoint(cell[0], cell[1], mTmpPoint);
        float distance = (float) Math.sqrt(Math.pow(x - mTmpPoint[0], 2) +
                Math.pow(y - mTmpPoint[1], 2));
        return distance;
    }

    @Override
    public boolean onEffectApplied(Canvas canvas, View child, long drawingTime) {
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public boolean onApplyAlpha(View child, float alpha) {
        applyAlpha(child, alpha);
        return true;
    }

    private Rect dirty = new Rect();

    private Runnable invalidate = new Runnable() {

        @Override
        public void run() {
            if (RuntimeConfig.sLauncherInScrolling) {
                postDelayed(this, 100);
            } else {
                invalidate(dirty);
                dirty.setEmpty();
            }
        }

    };

    @Override
    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
        if (RuntimeConfig.sLauncherInScrolling) {
            this.dirty.union(dirty.left + location[0], dirty.top + location[1], dirty.right + location[0], dirty.bottom + location[1]);;
            removeCallbacks(invalidate);
            postDelayed(invalidate, 100);
            return null;
        }
        return super.invalidateChildInParent(location, dirty);
    }

    public void enableSelfCache() {
        if (ViewUtils.isHardwareAccelerated(this) && DeviceUtils.isJellyBean() && !DeviceUtils.isMiOne()) {
            ViewUtils.enableHardwareLayer(this, true);
            for (int i = getChildCount() - 1; i >= 0; i--) {
                ViewUtils.enableHardwareLayer(getChildAt(i), false);
            }
        } else {
            setDrawingCacheEnabled(true);
            setChildrenDrawnWithCacheEnabled(false);
            setChildrenDrawingCacheEnabled(false);
        }
    }

    public void enableChildrenCache() {
        if (ViewUtils.isHardwareAccelerated(this) && DeviceUtils.isJellyBean() && !DeviceUtils.isMiOne()) {
            ViewUtils.enableHardwareLayer(this, false);
            for (int i = getChildCount() - 1; i >= 0; i--) {
                ViewUtils.enableHardwareLayer(getChildAt(i), true);
            }
        } else {
            setDrawingCacheEnabled(false);
            setChildrenDrawnWithCacheEnabled(true);
            setChildrenDrawingCacheEnabled(true);
        }
    }

    public void disableCache() {
        if (ViewUtils.isHardwareAccelerated(this) && DeviceUtils.isJellyBean() && !DeviceUtils.isMiOne()) {
            ViewUtils.enableHardwareLayer(this, false);
            for (int i = getChildCount() - 1; i >= 0; i--) {
                ViewUtils.enableHardwareLayer(getChildAt(i), false);
            }
        } else {
            setDrawingCacheEnabled(false);
            setChildrenDrawnWithCacheEnabled(false);
            setChildrenDrawingCacheEnabled(false);
        }
    }

    public void destroyCache() {
        if (ViewUtils.isHardwareAccelerated(this) && DeviceUtils.isJellyBean() && !DeviceUtils.isMiOne()) {
            ViewUtils.destroyHardwareLayer(this);
        } else {
            destroyDrawingCache();
        }
    }

    public void resetTransitionAlpha() {
        boolean isPhone = ChannelRecognizeUtils.getCurrentChannel(getContext()) == ChannelRecognizeUtils.CHANNEL_PHONE_DEFAULT;
        if (DeviceUtils.isIceCreamSandwich() && isPhone) {
            AnimationUtils.resetViewAlpha(this);
            for (int i = getChildCount() - 1; i >= 0; i--) {
                AnimationUtils.resetViewAlpha(getChildAt(i));
            }
        }
    }
    
    public long getFolderId(){
        return mFolderId;
    }

    public void setFolderId(long folderId){
        mFolderId = folderId;
    } 
    
    public void markCellsAsUnoccupiedForView(View view) {
        markCellsAsUnoccupiedForView(view, mOccupied);
    }
    
    public void markCellsAsUnoccupiedForView(View view, boolean occupied[][]) {
        if (view == null) return;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, occupied, false);
    }
    
    public int getWidthGap() {
        return mWidthGap;
    }

    public int getHeightGap() {
        return mHeightGap;
    }
    
    private void copyCurrentStateToSolution(ItemConfiguration solution, boolean temp) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            CellAndSpan c;
            if (temp) {
                c = new CellAndSpan(lp.tmpCellX, lp.tmpCellY, lp.cellHSpan, lp.cellVSpan);
            } else {
                c = new CellAndSpan(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan);
            }
            solution.add(child, c);
        }
    }
    
    private void copyOccupiedArray(boolean[][] occupied) {
        for (int i = 0; i < getCountX(); i++) {
            for (int j = 0; j < getCountY(); j++) {
                occupied[i][j] = mOccupied[i][j];
            }
        }
    }
    
    public int[] findNearestArea(
            int pixelX, int pixelY, int spanX, int spanY, int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY, null, false, result);
    }
    
    protected int[] findNearestArea(int pixelX, int pixelY, int spanX, int spanY, View ignoreView,
            boolean ignoreOccupied, int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY,
                spanX, spanY, ignoreView, ignoreOccupied, result, null, mOccupied);
    	//return null;
    }
    
    /*
     * Returns a pair (x, y), where x,y are in {-1, 0, 1} corresponding to vector between
     * the provided point and the provided cell
     */
    private void computeDirectionVector(float deltaX, float deltaY, int[] result) {
        double angle = Math.atan(((float) deltaY) / deltaX);

        result[0] = 0;
        result[1] = 0;
        if (Math.abs(Math.cos(angle)) > 0.5f) {
            result[0] = (int) Math.signum(deltaX);
        }
        if (Math.abs(Math.sin(angle)) > 0.5f) {
            result[1] = (int) Math.signum(deltaY);
        }
    }
    
    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location, and will also weigh in a suggested direction vector of the
     * desired location. This method computers distance based on unit grid distances,
     * not pixel distances.
     *
     * @param cellX The X cell nearest to which you want to search for a vacant area.
     * @param cellY The Y cell nearest which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param direction The favored direction in which the views should move from x, y
     * @param exactDirectionOnly If this parameter is true, then only solutions where the direction
     *        matches exactly. Otherwise we find the best matching direction.
     * @param occoupied The array which represents which cells in the CellLayout are occupied
     * @param blockOccupied The array which represents which cells in the specified block (cellX,
     *        cellY, spanX, spanY) are occupied. This is used when try to move a group of views. 
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    private int[] findNearestArea(int cellX, int cellY, int spanX, int spanY, int[] direction,
            boolean[][] occupied, boolean blockOccupied[][], int[] result) {
        // Keep track of best-scoring drop area
        final int[] bestXY = result != null ? result : new int[2];
        float bestDistance = Float.MAX_VALUE;
        int bestDirectionScore = Integer.MIN_VALUE;

        final int countX = getCountX();
        final int countY = getCountY();

        for (int y = 0; y < countY - (spanY - 1); y++) {
            inner:
            for (int x = 0; x < countX - (spanX - 1); x++) {
                // First, let's see if this thing fits anywhere
                for (int i = 0; i < spanX; i++) {
                    for (int j = 0; j < spanY; j++) {
                        if (occupied[x + i][y + j] && (blockOccupied == null || blockOccupied[i][j])) {
                            continue inner;
                        }
                    }
                }

                float distance = (float)
                        Math.sqrt((x - cellX) * (x - cellX) + (y - cellY) * (y - cellY));
                int[] curDirection = mTmpPoint;
                computeDirectionVector(x - cellX, y - cellY, curDirection);
                // The direction score is just the dot product of the two candidate direction
                // and that passed in.
                int curDirectionScore = direction[0] * curDirection[0] +
                        direction[1] * curDirection[1];
                boolean exactDirectionOnly = false;
                boolean directionMatches = direction[0] == curDirection[0] &&
                        direction[0] == curDirection[0];
                if ((directionMatches || !exactDirectionOnly) &&
                        Float.compare(distance,  bestDistance) < 0 || (Float.compare(distance,
                        bestDistance) == 0 && curDirectionScore > bestDirectionScore)) {
                    bestDistance = distance;
                    bestDirectionScore = curDirectionScore;
                    bestXY[0] = x;
                    bestXY[1] = y;
                }
            }
        }

        // Return -1, -1 if no suitable location found
        if (bestDistance == Float.MAX_VALUE) {
            bestXY[0] = -1;
            bestXY[1] = -1;
        }
        return bestXY;
    }
    
    private final Stack<Rect> mTempRectStack = new Stack<Rect>();
    private void lazyInitTempRectStack() {
        if (mTempRectStack.isEmpty()) {
            for (int i = 0; i < getCountX() * getCountY(); i++) {
                mTempRectStack.push(new Rect());
            }
        }
    }
    
    public void markCellsAsOccupiedForView(View view) {
    	if(view == null) return;
        markCellsAsOccupiedForView(view, mOccupied);
    }
    public void markCellsAsOccupiedForView(View view, boolean[][] occupied) {
        if (view == null) return;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, occupied, true);
    }
    
    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreOccupied If true, the result can be an occupied cell
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    protected int[] findNearestArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY,
            View ignoreView, boolean ignoreOccupied, int[] result, int[] resultSpan,
            boolean[][] occupied) {
        lazyInitTempRectStack();
        // mark space take by ignoreView as available (method checks if ignoreView is null)
        markCellsAsUnoccupiedForView(ignoreView, occupied);

        // For items with a spanX / spanY > 1, the passed in point (pixelX, pixelY) corresponds
        // to the center of the item, but we are searching based on the top-left cell, so
        // we translate the point over to correspond to the top-left.
        pixelX -= (mCellWidth + mWidthGap) * (spanX - 1) / 2f;
        pixelY -= (mCellHeight + mHeightGap) * (spanY - 1) / 2f;

        // Keep track of best-scoring drop area
        final int[] bestXY = result != null ? result : new int[2];
        double bestDistance = Double.MAX_VALUE;
        final Rect bestRect = new Rect(-1, -1, -1, -1);
        final Stack<Rect> validRegions = new Stack<Rect>();

        final int countX = getCountX();
        final int countY = getCountY();

        if (minSpanX <= 0 || minSpanY <= 0 || spanX <= 0 || spanY <= 0 ||
                spanX < minSpanX || spanY < minSpanY) {
            return bestXY;
        }

        for (int y = 0; y < countY - (minSpanY - 1); y++) {
            inner:
            for (int x = 0; x < countX - (minSpanX - 1); x++) {
                int ySize = -1;
                int xSize = -1;
                if (ignoreOccupied) {
                    // First, let's see if this thing fits anywhere
                    for (int i = 0; i < minSpanX; i++) {
                        for (int j = 0; j < minSpanY; j++) {
                            if (occupied[x + i][y + j]) {
                                continue inner;
                            }
                        }
                    }
                    xSize = minSpanX;
                    ySize = minSpanY;

                    // We know that the item will fit at _some_ acceptable size, now let's see
                    // how big we can make it. We'll alternate between incrementing x and y spans
                    // until we hit a limit.
                    boolean incX = true;
                    boolean hitMaxX = xSize >= spanX;
                    boolean hitMaxY = ySize >= spanY;
                    while (!(hitMaxX && hitMaxY)) {
                        if (incX && !hitMaxX) {
                            for (int j = 0; j < ySize; j++) {
                                if (x + xSize > countX -1 || occupied[x + xSize][y + j]) {
                                    // We can't move out horizontally
                                    hitMaxX = true;
                                }
                            }
                            if (!hitMaxX) {
                                xSize++;
                            }
                        } else if (!hitMaxY) {
                            for (int i = 0; i < xSize; i++) {
                                if (y + ySize > countY - 1 || occupied[x + i][y + ySize]) {
                                    // We can't move out vertically
                                    hitMaxY = true;
                                }
                            }
                            if (!hitMaxY) {
                                ySize++;
                            }
                        }
                        hitMaxX |= xSize >= spanX;
                        hitMaxY |= ySize >= spanY;
                        incX = !incX;
                    }
                    incX = true;
                    hitMaxX = xSize >= spanX;
                    hitMaxY = ySize >= spanY;
                }
                final int[] cellXY = mTmpXY;
                cellToCenterPoint(x, y, cellXY);

                // We verify that the current rect is not a sub-rect of any of our previous
                // candidates. In this case, the current rect is disqualified in favour of the
                // containing rect.
                
                Rect currentRect = null;
                if(!mTempRectStack.isEmpty()){
                	currentRect = mTempRectStack.pop();
                }
                
                if(currentRect == null){
                	currentRect = new Rect();
                }
                currentRect.set(x, y, x + xSize, y + ySize);
                boolean contained = false;
                for (Rect r : validRegions) {
                    if (r.contains(currentRect)) {
                        contained = true;
                        break;
                    }
                }
                validRegions.push(currentRect);
                double distance = Math.sqrt(Math.pow(cellXY[0] - pixelX, 2)
                        + Math.pow(cellXY[1] - pixelY, 2));

                if ((distance <= bestDistance && !contained) ||
                        currentRect.contains(bestRect)) {
                    bestDistance = distance;
                    bestXY[0] = x;
                    bestXY[1] = y;
                    if (resultSpan != null) {
                        resultSpan[0] = xSize;
                        resultSpan[1] = ySize;
                    }
                    bestRect.set(currentRect);
                }
            }
        }
        // re-mark space taken by ignoreView as occupied
        markCellsAsOccupiedForView(ignoreView, occupied);

        // Return -1, -1 if no suitable location found
        if (bestDistance == Double.MAX_VALUE) {
            bestXY[0] = -1;
            bestXY[1] = -1;
        }
        recycleTempRects(validRegions);
        return bestXY;
    }
    
    protected void recycleTempRects(Stack<Rect> used) {
        while (!used.isEmpty()) {
            mTempRectStack.push(used.pop());
        }
    }
    
    private boolean pushViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop,
            int[] direction, View dragView, ItemConfiguration currentState) {

        ViewCluster cluster = new ViewCluster(views, currentState);
        Rect clusterRect = cluster.getBoundingRect();
        int whichEdge;
        int pushDistance;
        boolean fail = false;

        // Determine the edge of the cluster that will be leading the push and how far
        // the cluster must be shifted.
        if (direction[0] < 0) {
            whichEdge = ViewCluster.LEFT;
            pushDistance = clusterRect.right - rectOccupiedByPotentialDrop.left;
        } else if (direction[0] > 0) {
            whichEdge = ViewCluster.RIGHT;
            pushDistance = rectOccupiedByPotentialDrop.right - clusterRect.left;
        } else if (direction[1] < 0) {
            whichEdge = ViewCluster.TOP;
            pushDistance = clusterRect.bottom - rectOccupiedByPotentialDrop.top;
        } else {
            whichEdge = ViewCluster.BOTTOM;
            pushDistance = rectOccupiedByPotentialDrop.bottom - clusterRect.top;
        }

        // Break early for invalid push distance.
        if (pushDistance <= 0) {
            return false;
        }

        // Mark the occupied state as false for the group of views we want to move.
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        }

        // We save the current configuration -- if we fail to find a solution we will revert
        // to the initial state. The process of finding a solution modifies the configuration
        // in place, hence the need for revert in the failure case.
        currentState.save();

        // The pushing algorithm is simplified by considering the views in the order in which
        // they would be pushed by the cluster. For example, if the cluster is leading with its
        // left edge, we consider sort the views by their right edge, from right to left.
        cluster.sortConfigurationForEdgePush(whichEdge);

        while (pushDistance > 0 && !fail) {
            for (View v: currentState.sortedViews) {
                // For each view that isn't in the cluster, we see if the leading edge of the
                // cluster is contacting the edge of that view. If so, we add that view to the
                // cluster.
                if (!cluster.views.contains(v) && v != dragView) {
                    if (cluster.isViewTouchingEdge(v, whichEdge)) {
                        LayoutParams lp = (LayoutParams) v.getLayoutParams();
                        if (!lp.canReorder) {
                            // The push solution includes the all apps button, this is not viable.
                            fail = true;
                            break;
                        }
                        cluster.addView(v);
                        CellAndSpan c = currentState.map.get(v);

                        // Adding view to cluster, mark it as not occupied.
                        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
                    }
                }
            }
            pushDistance--;

            // The cluster has been completed, now we move the whole thing over in the appropriate
            // direction.
            cluster.shift(whichEdge, 1);
        }

        boolean foundSolution = false;
        clusterRect = cluster.getBoundingRect();

        // Due to the nature of the algorithm, the only check required to verify a valid solution
        // is to ensure that completed shifted cluster lies completely within the cell layout.
        if (!fail && clusterRect.left >= 0 && clusterRect.right <= getCountX() && clusterRect.top >= 0 &&
                clusterRect.bottom <= getCountY()) {
            foundSolution = true;
        } else {
            currentState.restore();
        }

        // In either case, we set the occupied array as marked for the location of the views
        for (View v: cluster.views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        }

        return foundSolution;
    }
    
 // This method tries to find a reordering solution which satisfies the push mechanic by trying
    // to push items in each of the cardinal directions, in an order based on the direction vector
    // passed.
    protected boolean attemptPushInDirection(ArrayList<View> intersectingViews, Rect occupied,
            int[] direction, View ignoreView, ItemConfiguration solution) {
        if ((Math.abs(direction[0]) + Math.abs(direction[1])) > 1) {
            // If the direction vector has two non-zero components, we try pushing 
            // separately in each of the components.
            int temp = direction[1];
            direction[1] = 0;

            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            direction[1] = temp;
            temp = direction[0];
            direction[0] = 0;

            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Revert the direction
            direction[0] = temp;

            // Now we try pushing in each component of the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            temp = direction[1];
            direction[1] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }

            direction[1] = temp;
            temp = direction[0];
            direction[0] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // revert the direction
            direction[0] = temp;
            direction[0] *= -1;
            direction[1] *= -1;
            
        } else {
            // If the direction vector has a single non-zero component, we push first in the
            // direction of the vector
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Then we try the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Switch the direction back
            direction[0] *= -1;
            direction[1] *= -1;
            
            // If we have failed to find a push solution with the above, then we try 
            // to find a solution by pushing along the perpendicular axis.

            // Swap the components
            int temp = direction[1];
            direction[1] = direction[0];
            direction[0] = temp;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }

            // Then we try the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Switch the direction back
            direction[0] *= -1;
            direction[1] *= -1;

            // Swap the components back
            temp = direction[1];
            direction[1] = direction[0];
            direction[0] = temp;
        }
        return false;
    }
    
    private void markCellsForRect(Rect r, boolean[][] occupied, boolean value) {
        markCellsForView(r.left, r.top, r.width(), r.height(), occupied, value);
    }
    
    protected boolean addViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop,
            int[] direction, View dragView, ItemConfiguration currentState) {
        if (views.size() == 0) return true;

        boolean success = false;
        Rect boundingRect = null;
        // We construct a rect which represents the entire group of views passed in
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            if (boundingRect == null) {
                boundingRect = new Rect(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            } else {
                boundingRect.union(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            }
        }

        // Mark the occupied state as false for the group of views we want to move.
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        }

        boolean[][] blockOccupied = new boolean[boundingRect.width()][boundingRect.height()];
        int top = boundingRect.top;
        int left = boundingRect.left;
        // We mark more precisely which parts of the bounding rect are truly occupied, allowing
        // for interlocking.
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x - left, c.y - top, c.spanX, c.spanY, blockOccupied, true);
        }

        markCellsForRect(rectOccupiedByPotentialDrop, mTmpOccupied, true);

        findNearestArea(boundingRect.left, boundingRect.top, boundingRect.width(),
                boundingRect.height(), direction, mTmpOccupied, blockOccupied, mTempLocation);

        // If we successfuly found a location by pushing the block of views, we commit it
        if (mTempLocation[0] >= 0 && mTempLocation[1] >= 0) {
            int deltaX = mTempLocation[0] - boundingRect.left;
            int deltaY = mTempLocation[1] - boundingRect.top;
            for (View v: views) {
                CellAndSpan c = currentState.map.get(v);
                c.x += deltaX;
                c.y += deltaY;
            }
            success = true;
        }

        // In either case, we set the occupied array as marked for the location of the views
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        }
        return success;
    }
    
    private boolean rearrangementExists(int cellX, int cellY, int spanX, int spanY, int[] direction,
            View ignoreView, ItemConfiguration solution) {
        // Return early if get invalid cell positions
        if (cellX < 0 || cellY < 0) return false;

        mIntersectingViews.clear();
        mOccupiedRect.set(cellX, cellY, cellX + spanX, cellY + spanY);

        // Mark the desired location of the view currently being dragged.
        if (ignoreView != null) {
            CellAndSpan c = solution.map.get(ignoreView);
            if (c != null) {
                c.x = cellX;
                c.y = cellY;
            }
        }
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();
        for (View child: solution.map.keySet()) {
            if (child == ignoreView) continue;
            CellAndSpan c = solution.map.get(child);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            r1.set(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            if (Rect.intersects(r0, r1)) {
                if (!lp.canReorder) {
                    return false;
                }
                mIntersectingViews.add(child);
            }
        }

        // First we try to find a solution which respects the push mechanic. That is, 
        // we try to find a solution such that no displaced item travels through another item
        // without also displacing that item.
        if (attemptPushInDirection(mIntersectingViews, mOccupiedRect, direction, ignoreView,
                solution)) {
            return true;
        }

        // Next we try moving the views as a block, but without requiring the push mechanic.
        if (addViewsToTempLocation(mIntersectingViews, mOccupiedRect, direction, ignoreView,
                solution)) {
            return true;
        }

        // Ok, they couldn't move as a block, let's move them individually
        for (View v : mIntersectingViews) {
            if (!addViewToTempLocation(v, mOccupiedRect, direction, solution)) {
                return false;
            }
        }
        return true;
    }
    
 // This method starts or changes the reorder hint animations
    private void beginOrAdjustHintAnimations(ItemConfiguration solution, View dragView, int delay) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child == dragView) continue;
            CellAndSpan c = solution.map.get(child);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (c != null) {
                ReorderHintAnimation rha = new ReorderHintAnimation(child, lp.cellX, lp.cellY,
                        c.x, c.y, c.spanX, c.spanY);
                rha.animate();
            }
        }
    }
    
    private boolean addViewToTempLocation(View v, Rect rectOccupiedByPotentialDrop,
            int[] direction, ItemConfiguration currentState) {
        CellAndSpan c = currentState.map.get(v);
        boolean success = false;
        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        markCellsForRect(rectOccupiedByPotentialDrop, mTmpOccupied, true);

        findNearestArea(c.x, c.y, c.spanX, c.spanY, direction, mTmpOccupied, null, mTempLocation);

        if (mTempLocation[0] >= 0 && mTempLocation[1] >= 0) {
            c.x = mTempLocation[0];
            c.y = mTempLocation[1];
            success = true;
        }
        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        return success;
    }
    
    protected ItemConfiguration simpleSwap(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX,
            int spanY, int[] direction, View dragView, boolean decX, ItemConfiguration solution) {
        // Copy the current state into the solution. This solution will be manipulated as necessary.
        copyCurrentStateToSolution(solution, false);
        // Copy the current occupied array into the temporary occupied array. This array will be
        // manipulated as necessary to find a solution.
        copyOccupiedArray(mTmpOccupied);

        // We find the nearest cell into which we would place the dragged item, assuming there's
        // nothing in its way.
        int result[] = new int[2];
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);

        boolean success = false;
        // First we try the exact nearest position of the item being dragged,
        // we will then want to try to move this around to other neighbouring positions
        success = rearrangementExists(result[0], result[1], spanX, spanY, direction, dragView,
                solution);

        if (!success) {
            // We try shrinking the widget down to size in an alternating pattern, shrink 1 in
            // x, then 1 in y etc.
            if (spanX > minSpanX && (minSpanY == spanY || decX)) {
                return simpleSwap(pixelX, pixelY, minSpanX, minSpanY, spanX - 1, spanY, direction,
                        dragView, false, solution);
            } else if (spanY > minSpanY) {
                return simpleSwap(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY - 1, direction,
                        dragView, true, solution);
            }
            solution.isSolution = false;
        } else {
            solution.isSolution = true;
            solution.dragViewX = result[0];
            solution.dragViewY = result[1];
            solution.dragViewSpanX = spanX;
            solution.dragViewSpanY = spanY;
        }
        return solution;
    }
    
    public void setUseTempCoords(boolean useTempCoords) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams)getChildAt(i).getLayoutParams();
            lp.useTmpCoords = useTempCoords;
        }
    }
    
    protected void copySolutionToTempState(ItemConfiguration solution, View dragView) {
        for (int i = 0; i < getCountX(); i++) {
            for (int j = 0; j < getCountY(); j++) {
                mTmpOccupied[i][j] = false;
            }
        }

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child == dragView) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                lp.tmpCellX = c.x;
                lp.tmpCellY = c.y;
                lp.cellHSpan = c.spanX;
                lp.cellVSpan = c.spanY;
                markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
            }
        }
        markCellsForView(solution.dragViewX, solution.dragViewY, solution.dragViewSpanX,
                solution.dragViewSpanY, mTmpOccupied, true);
    }
    
    protected void setItemPlacementDirty(boolean dirty) {
        mItemPlacementDirty = dirty;
    }
    protected boolean isItemPlacementDirty() {
        return mItemPlacementDirty;
    }
    
    public boolean createAreaForResize(int cellX, int cellY, int spanX, int spanY,
            View dragView, int[] direction, boolean commit) {
        int[] pixelXY = new int[2];
        regionToCenterPoint(cellX, cellY, spanX, spanY, pixelXY);

        
        // First we determine if things have moved enough to cause a different layout
        ItemConfiguration swapSolution = simpleSwap(pixelXY[0], pixelXY[1], spanX, spanY,
                 spanX,  spanY, direction, dragView,  true,  new ItemConfiguration());

        setUseTempCoords(true);
        if (swapSolution != null && swapSolution.isSolution) {
            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            copySolutionToTempState(swapSolution, dragView);
            setItemPlacementDirty(true);
            animateItemsToSolution(swapSolution, dragView, commit);

            if (commit) {
                commitTempPlacement();
                completeAndClearReorderHintAnimations();
                setItemPlacementDirty(false);
            } else {
                beginOrAdjustHintAnimations(swapSolution, dragView,
                        REORDER_ANIMATION_DURATION);
            }
            requestLayout();
        }
        return swapSolution.isSolution;
        //return false;
    }
    
    protected void completeAndClearReorderHintAnimations() {
        /*for (ReorderHintAnimation a: mShakeAnimators.values()) {
            a.completeAnimationImmediately();
        }
        mShakeAnimators.clear();*/
    }
    
    protected void commitTempPlacement() {
        /*for (int i = 0; i < getCountX(); i++) {
            for (int j = 0; j < getCountY(); j++) {
                mOccupied[i][j] = mTmpOccupied[i][j];
            }
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            ItemInfo info = (ItemInfo) child.getTag();
            // We do a null check here because the item info can be null in the case of the
            // AllApps button in the hotseat.
            if (info != null) {
                if (info.cellX != lp.tmpCellX || info.cellY != lp.tmpCellY ||
                        info.spanX != lp.cellHSpan || info.spanY != lp.cellVSpan) {
                    //info.requiresDbUpdate = true;
                }
                info.cellX = lp.cellX = lp.tmpCellX;
                info.cellY = lp.cellY = lp.tmpCellY;
                info.spanX = lp.cellHSpan;
                info.spanY = lp.cellVSpan;
            }
        }
        //mLauncher.getWorkspace().updateItemLocationsInDatabase(this);*/
    	
    }
    
    protected void animateItemsToSolution(ItemConfiguration solution, View dragView, boolean commitDragView) {

        /*boolean[][] occupied = DESTRUCTIVE_REORDER ? mOccupied : mTmpOccupied;
        for (int i = 0; i < getCountX(); i++) {
            for (int j = 0; j < getCountY(); j++) {
                occupied[i][j] = false;
            }
        }

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child == dragView) continue;
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                animateChildToPosition(child, c.x, c.y, REORDER_ANIMATION_DURATION, 0,
                        DESTRUCTIVE_REORDER, false);
                markCellsForView(c.x, c.y, c.spanX, c.spanY, occupied, true);
            }
        }
        if (commitDragView) {
            markCellsForView(solution.dragViewX, solution.dragViewY, solution.dragViewSpanX,
                    solution.dragViewSpanY, occupied, true);
        }*/
    	return;
    }
    
    public boolean animateChildToPosition(final View child, int cellX, int cellY, int duration,
            int delay, boolean permanent, boolean adjustOccupied) {
        //ShortcutAndWidgetContainer clc = getShortcutsAndWidgets();
        /*boolean[][] occupied = mOccupied;
        if (!permanent) {
            occupied = mTmpOccupied;
        }

        if (indexOfChild(child) != -1) {
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final ItemInfo info = (ItemInfo) child.getTag();

            // We cancel any existing animations
            if (mReorderAnimators.containsKey(lp)) {
                mReorderAnimators.get(lp).cancel();
                mReorderAnimators.remove(lp);
            }

            final int oldX = lp.x;
            final int oldY = lp.y;
            if (adjustOccupied) {
                occupied[lp.cellX][lp.cellY] = false;
                occupied[cellX][cellY] = true;
            }
            lp.isLockedToGrid = true;
            if (permanent) {
                lp.cellX = info.cellX = cellX;
                lp.cellY = info.cellY = cellY;
            } else {
                lp.tmpCellX = cellX;
                lp.tmpCellY = cellY;
            }
            setupLp(lp);
            lp.isLockedToGrid = false;
            final int newX = lp.x;
            final int newY = lp.y;

            lp.x = oldX;
            lp.y = oldY;

            // Exit early if we're not actually moving the view
            if (oldX == newX && oldY == newY) {
                lp.isLockedToGrid = true;
                return true;
            }

            ValueAnimator va = LauncherAnimUtils.ofFloat(child, 0f, 1f);
            va.setDuration(duration);
            mReorderAnimators.put(lp, va);

            va.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float r = ((Float) animation.getAnimatedValue()).floatValue();
                    lp.x = (int) ((1 - r) * oldX + r * newX);
                    lp.y = (int) ((1 - r) * oldY + r * newY);
                    child.requestLayout();
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                boolean cancelled = false;
                public void onAnimationEnd(Animator animation) {
                    // If the animation was cancelled, it means that another animation
                    // has interrupted this one, and we don't want to lock the item into
                    // place just yet.
                    if (!cancelled) {
                        lp.isLockedToGrid = true;
                        child.requestLayout();
                    }
                    if (mReorderAnimators.containsKey(lp)) {
                        mReorderAnimators.remove(lp);
                    }
                }
                public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                }
            });
            va.setStartDelay(delay);
            va.start();
            return true;
        }
        return false;*/
    	return false;
    }
    
    public float getChildrenScale() {
        return 1.0f;
    }
    
    public class ItemConfiguration {
        public HashMap<View, CellAndSpan> map = new HashMap<View, CellAndSpan>();
        private HashMap<View, CellAndSpan> savedMap = new HashMap<View, CellAndSpan>();
        ArrayList<View> sortedViews = new ArrayList<View>();
        public boolean isSolution = false;
        public int dragViewX, dragViewY, dragViewSpanX, dragViewSpanY;

        void save() {
            // Copy current state into savedMap
            for (View v: map.keySet()) {
                map.get(v).copy(savedMap.get(v));
            }
        }

        void restore() {
            // Restore current state from savedMap
            for (View v: savedMap.keySet()) {
                savedMap.get(v).copy(map.get(v));
            }
        }

        void add(View v, CellAndSpan cs) {
            map.put(v, cs);
            savedMap.put(v, new CellAndSpan());
            sortedViews.add(v);
        }

        public int area() {
            return dragViewSpanX * dragViewSpanY;
        }
    }
    
    public class CellAndSpan {
        public int x, y;
        public int spanX, spanY;

        public CellAndSpan() {
        }

        public void copy(CellAndSpan copy) {
            copy.x = x;
            copy.y = y;
            copy.spanX = spanX;
            copy.spanY = spanY;
        }

        public CellAndSpan(int x, int y, int spanX, int spanY) {
            this.x = x;
            this.y = y;
            this.spanX = spanX;
            this.spanY = spanY;
        }

        public String toString() {
            return "(" + x + ", " + y + ": " + spanX + ", " + spanY + ")";
        }

    }
    
    /**
     * This helper class defines a cluster of views. It helps with defining complex edges
     * of the cluster and determining how those edges interact with other views. The edges
     * essentially define a fine-grained boundary around the cluster of views -- like a more
     * precise version of a bounding box.
     */
    private class ViewCluster {
        final static int LEFT = 0;
        final static int TOP = 1;
        final static int RIGHT = 2;
        final static int BOTTOM = 3;

        ArrayList<View> views;
        ItemConfiguration config;
        Rect boundingRect = new Rect();

        int[] leftEdge = new int[getCountY()];
        int[] rightEdge = new int[getCountY()];
        int[] topEdge = new int[getCountX()];
        int[] bottomEdge = new int[getCountX()];
        boolean leftEdgeDirty, rightEdgeDirty, topEdgeDirty, bottomEdgeDirty, boundingRectDirty;

        @SuppressWarnings("unchecked")
        public ViewCluster(ArrayList<View> views, ItemConfiguration config) {
            this.views = (ArrayList<View>) views.clone();
            this.config = config;
            resetEdges();
        }

        void resetEdges() {
            for (int i = 0; i < getCountX(); i++) {
                topEdge[i] = -1;
                bottomEdge[i] = -1;
            }
            for (int i = 0; i < getCountY(); i++) {
                leftEdge[i] = -1;
                rightEdge[i] = -1;
            }
            leftEdgeDirty = true;
            rightEdgeDirty = true;
            bottomEdgeDirty = true;
            topEdgeDirty = true;
            boundingRectDirty = true;
        }

        void computeEdge(int which, int[] edge) {
            int count = views.size();
            for (int i = 0; i < count; i++) {
                CellAndSpan cs = config.map.get(views.get(i));
                switch (which) {
                    case LEFT:
                        int left = cs.x;
                        for (int j = cs.y; j < cs.y + cs.spanY; j++) {
                            if (left < edge[j] || edge[j] < 0) {
                                edge[j] = left;
                            }
                        }
                        break;
                    case RIGHT:
                        int right = cs.x + cs.spanX;
                        for (int j = cs.y; j < cs.y + cs.spanY; j++) {
                            if (right > edge[j]) {
                                edge[j] = right;
                            }
                        }
                        break;
                    case TOP:
                        int top = cs.y;
                        for (int j = cs.x; j < cs.x + cs.spanX; j++) {
                            if (top < edge[j] || edge[j] < 0) {
                                edge[j] = top;
                            }
                        }
                        break;
                    case BOTTOM:
                        int bottom = cs.y + cs.spanY;
                        for (int j = cs.x; j < cs.x + cs.spanX; j++) {
                            if (bottom > edge[j]) {
                                edge[j] = bottom;
                            }
                        }
                        break;
                }
            }
        }

        boolean isViewTouchingEdge(View v, int whichEdge) {
            CellAndSpan cs = config.map.get(v);

            int[] edge = getEdge(whichEdge);

            switch (whichEdge) {
                case LEFT:
                    for (int i = cs.y; i < cs.y + cs.spanY; i++) {
                        if (edge[i] == cs.x + cs.spanX) {
                            return true;
                        }
                    }
                    break;
                case RIGHT:
                    for (int i = cs.y; i < cs.y + cs.spanY; i++) {
                        if (edge[i] == cs.x) {
                            return true;
                        }
                    }
                    break;
                case TOP:
                    for (int i = cs.x; i < cs.x + cs.spanX; i++) {
                        if (edge[i] == cs.y + cs.spanY) {
                            return true;
                        }
                    }
                    break;
                case BOTTOM:
                    for (int i = cs.x; i < cs.x + cs.spanX; i++) {
                        if (edge[i] == cs.y) {
                            return true;
                        }
                    }
                    break;
            }
            return false;
        }

        void shift(int whichEdge, int delta) {
            for (View v: views) {
                CellAndSpan c = config.map.get(v);
                switch (whichEdge) {
                    case LEFT:
                        c.x -= delta;
                        break;
                    case RIGHT:
                        c.x += delta;
                        break;
                    case TOP:
                        c.y -= delta;
                        break;
                    case BOTTOM:
                    default:
                        c.y += delta;
                        break;
                }
            }
            resetEdges();
        }

        public void addView(View v) {
            views.add(v);
            resetEdges();
        }

        public Rect getBoundingRect() {
            if (boundingRectDirty) {
                boolean first = true;
                for (View v: views) {
                    CellAndSpan c = config.map.get(v);
                    if (first) {
                        boundingRect.set(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                        first = false;
                    } else {
                        boundingRect.union(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                    }
                }
            }
            return boundingRect;
        }

        public int[] getEdge(int which) {
            switch (which) {
                case LEFT:
                    return getLeftEdge();
                case RIGHT:
                    return getRightEdge();
                case TOP:
                    return getTopEdge();
                case BOTTOM:
                default:
                    return getBottomEdge();
            }
        }

        public int[] getLeftEdge() {
            if (leftEdgeDirty) {
                computeEdge(LEFT, leftEdge);
            }
            return leftEdge;
        }

        public int[] getRightEdge() {
            if (rightEdgeDirty) {
                computeEdge(RIGHT, rightEdge);
            }
            return rightEdge;
        }

        public int[] getTopEdge() {
            if (topEdgeDirty) {
                computeEdge(TOP, topEdge);
            }
            return topEdge;
        }

        public int[] getBottomEdge() {
            if (bottomEdgeDirty) {
                computeEdge(BOTTOM, bottomEdge);
            }
            return bottomEdge;
        }

        PositionComparator comparator = new PositionComparator();
        class PositionComparator implements Comparator<View> {
            int whichEdge = 0;
            public int compare(View left, View right) {
                CellAndSpan l = config.map.get(left);
                CellAndSpan r = config.map.get(right);
                switch (whichEdge) {
                    case LEFT:
                        return (r.x + r.spanX) - (l.x + l.spanX);
                    case RIGHT:
                        return l.x - r.x;
                    case TOP:
                        return (r.y + r.spanY) - (l.y + l.spanY);
                    case BOTTOM:
                    default:
                        return l.y - r.y;
                }
            }
        }

        public void sortConfigurationForEdgePush(int edge) {
            comparator.whichEdge = edge;
            Collections.sort(config.sortedViews, comparator);
        }
    }
    
 // Class which represents the reorder hint animations. These animations show that an item is
    // in a temporary state, and hint at where the item will return to.
    public class ReorderHintAnimation {
        View child;
        float finalDeltaX;
        float finalDeltaY;
        float initDeltaX;
        float initDeltaY;
        float finalScale;
        float initScale;
        private static final int DURATION = 300;
        Animator a;

        public ReorderHintAnimation(View child, int cellX0, int cellY0, int cellX1, int cellY1,
                int spanX, int spanY) {
            regionToCenterPoint(cellX0, cellY0, spanX, spanY, mTmpPoint);
            final int x0 = mTmpPoint[0];
            final int y0 = mTmpPoint[1];
            regionToCenterPoint(cellX1, cellY1, spanX, spanY, mTmpPoint);
            final int x1 = mTmpPoint[0];
            final int y1 = mTmpPoint[1];
            final int dX = x1 - x0;
            final int dY = y1 - y0;
            finalDeltaX = 0;
            finalDeltaY = 0;
            if (dX == dY && dX == 0) {
            } else {
                if (dY == 0) {
                    finalDeltaX = - Math.signum(dX) * mReorderHintAnimationMagnitude;
                } else if (dX == 0) {
                    finalDeltaY = - Math.signum(dY) * mReorderHintAnimationMagnitude;
                } else {
                    double angle = Math.atan( (float) (dY) / dX);
                    finalDeltaX = (int) (- Math.signum(dX) *
                            Math.abs(Math.cos(angle) * mReorderHintAnimationMagnitude));
                    finalDeltaY = (int) (- Math.signum(dY) *
                            Math.abs(Math.sin(angle) * mReorderHintAnimationMagnitude));
                }
            }
            initDeltaX = child.getTranslationX();
            initDeltaY = child.getTranslationY();
            finalScale = getChildrenScale() - 4.0f / child.getWidth();
            initScale = child.getScaleX();
            this.child = child;
        }

        public void animate() {
            if (mShakeAnimators.containsKey(child)) {
                ReorderHintAnimation oldAnimation = mShakeAnimators.get(child);
                oldAnimation.cancel();
                mShakeAnimators.remove(child);
                if (finalDeltaX == 0 && finalDeltaY == 0) {
                    completeAnimationImmediately();
                    return;
                }
            }
            if (finalDeltaX == 0 && finalDeltaY == 0) {
                return;
            }
            ValueAnimator va = LauncherAnimUtils.ofFloat(child, 0f, 1f);
            a = va;
            va.setRepeatMode(ValueAnimator.REVERSE);
            va.setRepeatCount(ValueAnimator.INFINITE);
            va.setDuration(DURATION);
            va.setStartDelay((int) (Math.random() * 60));
            va.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float r = ((Float) animation.getAnimatedValue()).floatValue();
                    float x = r * finalDeltaX + (1 - r) * initDeltaX;
                    float y = r * finalDeltaY + (1 - r) * initDeltaY;
                    child.setTranslationX(x);
                    child.setTranslationY(y);
                    float s = r * finalScale + (1 - r) * initScale;
                    child.setScaleX(s);
                    child.setScaleY(s);
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                public void onAnimationRepeat(Animator animation) {
                    // We make sure to end only after a full period
                    initDeltaX = 0;
                    initDeltaY = 0;
                    initScale = 1.0F;
                }
            });
            mShakeAnimators.put(child, this);
            va.start();
        }

        private void cancel() {
            if (a != null) {
                a.cancel();
            }
        }

        public void completeAnimationImmediately() {
            if (a != null) {
                a.cancel();
            }

            AnimatorSet s = LauncherAnimUtils.createAnimatorSet();
            a = s;
            s.playTogether(
                LauncherAnimUtils.ofFloat(child, "scaleX", 1.0F),
                LauncherAnimUtils.ofFloat(child, "scaleY", 1.0F),
                LauncherAnimUtils.ofFloat(child, "translationX", 0f),
                LauncherAnimUtils.ofFloat(child, "translationY", 0f)
            );
            s.setDuration(REORDER_ANIMATION_DURATION);
            s.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
            s.start();
        }
    }

    
    
}

