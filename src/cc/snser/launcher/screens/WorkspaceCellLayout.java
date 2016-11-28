
package cc.snser.launcher.screens;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;
import cc.snser.launcher.CellLayout;
import cc.snser.launcher.apps.components.workspace.Shortcut;
import cc.snser.launcher.apps.model.ItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.model.DbManager;
import cc.snser.launcher.screens.DeleteZone.Deletable;
import cc.snser.launcher.style.SettingPreferences;
import cc.snser.launcher.ui.components.ScreenIndicator;
import cc.snser.launcher.ui.effects.SymmetricalLinearTween;
import cc.snser.launcher.ui.effects.TweenCallback;
import cc.snser.launcher.ui.utils.Utilities;
import cc.snser.launcher.util.CellImagePool;
import cc.snser.launcher.util.ResourceUtils;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.support.v4.util.ViewUtils;
import com.shouxinzm.launcher.util.ScreenDimensUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

public class WorkspaceCellLayout extends CellLayout implements Deletable {
    public static final int NORMAL  = 1;
    public static final int EDIT    = 2;
    public static final int DRAGING = 3;

    private int mLayoutStatus = NORMAL;

    private final Rect mRect = new Rect();
    private final CellInfo mCellInfo = new CellInfo();

    private boolean mLastDownOnOccupiedCell = false;
    
    private OverScroller mScroller;
    protected float mCellLayoutHeight;
    private Paint mPaint;
    private Rect mTextRect = new Rect();
    
    private final int OVERLAP_OFFSET = 8;   
    protected int overlapOffset;
    
    public WorkspaceCellLayout(Context context) {
        this(context, null);
    }

    public WorkspaceCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        mScroller = new OverScroller(context, new DecelerateInterpolator());
    }

    public WorkspaceCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        initConfigurationValues();

        mPreviousReorderDirection[0] = INVALID_DIRECTION;
        mPreviousReorderDirection[1] = INVALID_DIRECTION;

        mPaint = new Paint();
        int textSize = getContext().getResources().getDimensionPixelSize(
                R.dimen.workspace_exported_screen_title_text_size);
        mPaint.setTextSize(textSize);
        mPaint.setTextAlign(Paint.Align.LEFT);
        mPaint.setTypeface(Typeface.SANS_SERIF);
        mPaint.setColor(0xB4FFFFFF);
        mPaint.getFontMetrics();
        mPaint.getFontMetrics();
        mPaint.getFontMetrics();
    }

    protected void initConfigurationValues() {
    	overlapOffset = Utilities.dip2px(getContext(), OVERLAP_OFFSET);
        mCellWidth = this.getResources().getDimensionPixelSize(R.dimen.workspace_cell_width);
        mCellHeight = this.getResources().getDimensionPixelSize(R.dimen.workspace_cell_height);

        int[] paddingVertical = ResourceUtils.calculateVerticalPadding(getContext());
        int[] paddingHorizontal = ResourceUtils.calculateHorizontalPadding(getContext());
        mLongAxisStartPadding = paddingVertical[0];
        mLongAxisEndPadding   = paddingVertical[1];
        mShortAxisStartPadding = paddingHorizontal[0];
        mShortAxisEndPadding = paddingHorizontal[1];
       

        //Pair<Float, Float> paddingAndCellWidth = ResourceUtils.calculateWorkspaceFixedShortAxisPaddingAndCellWidth(this.getContext());

        //mShortAxisStartPadding = mShortAxisEndPadding = (int) paddingAndCellWidth.first.floatValue()+overlapOffset;
        
        //这个宽度在横屏下算的不对，暂时去掉这个逻辑 <snsermail@gmail.com>
        //mCellWidth = (int) paddingAndCellWidth.second.floatValue();

        CellImagePool.setDefault(mCellWidth, mCellHeight);
        
        mCellLayoutHeight = ScreenDimensUtils.getScreenHeight(getContext()) 
                - ResourceUtils.getStatusBarHeight(getContext()) 
                - ResourceUtils.getDockbarHeight(getContext());
        
        if(mChildWidth == 0 || mChildHeight == 0){
        	mChildHeight = mCellHeight;
        	mChildWidth = mCellWidth;
        }
       mIsModifyHPadding = mIsModifyVPadding = false;
       XLog.e("testCellLayout","mCellHeight:"+mCellHeight);
    }
    
    public void setLayoutStatus(int status){
        if(mLayoutStatus == status){
            return;
        }

        mLayoutStatus = status;
        invalidate();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        WorkspaceCellLayoutMeasure.onMeasure(getContext(), widthMeasureSpec, heightMeasureSpec);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        final CellInfo cellInfo = mCellInfo;

        if (action == MotionEvent.ACTION_DOWN) {
            final Rect frame = mRect;
            final int x = (int) ev.getX() + getScrollX();
            final int y = (int) ev.getY() + getScrollY();
            if (y >= this.getTop() + this.getTopPadding() && y <= this.getBottom() - this.getBottomPadding()) {
                final int count = getChildCount();

                boolean found = false;
                for (int i = count - 1; i >= 0; i--) {
                    final View child = getChildAt(i);

                    if ((child.getVisibility()) == VISIBLE || child.getAnimation() != null) {
                        child.getHitRect(frame);
                        if (frame.contains(x, y)) {
                            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                            cellInfo.setCell(child);
                            cellInfo.cellX = lp.cellX;
                            cellInfo.cellY = lp.cellY;
                            cellInfo.spanX = lp.cellHSpan;
                            cellInfo.spanY = lp.cellVSpan;
                            cellInfo.valid = true;
                            found = true;
                            break;
                        }
                    }
                }

                mLastDownOnOccupiedCell = found;

                if (!found) {
                    int cellXY[] = mCellXY;
                    pointToCellExact(x, y, cellXY);

                    cellInfo.setCell(null);
                    cellInfo.cellX = cellXY[0];
                    cellInfo.cellY = cellXY[1];
                    cellInfo.spanX = 1;
                    cellInfo.spanY = 1;
                    cellInfo.valid = true;
                }
            } else {
                mLastDownOnOccupiedCell = false;

                cellInfo.setCell(null);
                cellInfo.cellX = -1;
                cellInfo.cellY = -1;
                cellInfo.spanX = 0;
                cellInfo.spanY = 0;
                cellInfo.valid = true;
            }
        } else if (action == MotionEvent.ACTION_UP) {
//            cellInfo.setCell(null);
            cellInfo.cellX = -1;
            cellInfo.cellY = -1;
            cellInfo.spanX = 0;
            cellInfo.spanY = 0;
            cellInfo.valid = false;
        }

        setTag(cellInfo);
        
        if(mLayoutStatus > NORMAL && getChildCount() > 0){
            return true;
        }
        return false;
    }
    
    public void setScreenIndex(int index){
        mCellInfo.screen = index;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mCellInfo.screen = ((ViewGroup) getParent()).indexOfChild(this) - Workspace.getWorkspacePrefixScreenSize();
    }
    
    public void refreshScreenIndex(){
        mCellInfo.screen = ((ViewGroup) getParent()).indexOfChild(this) - Workspace.getWorkspacePrefixScreenSize();
    }

    @Override
    public boolean lastDownOnOccupiedCell() {
        return mLastDownOnOccupiedCell;
    }

    @Override
    public boolean resetLayout() {
        boolean ret = super.resetLayout();

        initConfigurationValues();

        return ret;
    }

    @Override
    public int[] getLayout() {
        return SettingPreferences.getHomeLayout(this.getContext());
    }
    
   
    
    // code for reorder
    @Override
    public void onDragExit() {
        super.onDragExit();

        revertTempState();
    }
    
    public boolean hasEnoughSpace(int spanX,int spanY){
    	//用总个数判断
    	int countX = getCountX();
        int countY = getCountY();
        findOccupiedCells(getCountX(), getCountY(), mOccupied, null);
        int occupiedCount = 0;
        for (int i = 0;i < countX; i++ ){
            for (int j = 0;j < countY; j++ ){
                if(mOccupied[i][j]){
                    occupiedCount++;
                }
            }
        }
        
    	boolean hasEnoughtSpace = occupiedCount + spanX * spanY <= mShortAxisCells * mLongAxisCells;
    	if(!hasEnoughtSpace){
    		return hasEnoughtSpace;
    	}
    	
    	//用FindCellForSpan判断
    	boolean hasCell = findCellForSpan(null, spanX, spanY);
    	if(hasCell){
    		return true;
    	}
    	
    	try {
    		//加入spanx,spany的rect,然后看能不能排下
        	Vector<LayoutParams> lps = new Vector<CellLayout.LayoutParams>(getChildCount());
        	int count = getChildCount();
            for (int i = 0; i < count; i++) {
            	lps.add((LayoutParams)getChildAt(i).getLayoutParams());
            }
            lps.add(new LayoutParams(0, 0, spanX, spanY));
            
            for (int i = 0;i < countX; i++ ){
            	for (int j = 0;j < countY; j++ ){
            		mOccupied[i][j] = false;
            	}
            }
            
            Collections.sort(lps, new Comparator<LayoutParams>() {
                public int compare(LayoutParams left, LayoutParams right) {
                	if(left.cellVSpan == right.cellVSpan){
                		if(left.cellHSpan == right.cellHSpan){
                			return 0;
                		}
                		return left.cellHSpan < right.cellHSpan ? 1 : -1;
                	}
                    return left.cellVSpan < right.cellVSpan ? 1 : -1;
                }
            });
            
            for(int i = 0;i < lps.size();i++){
            	if (!fillOccupied(mOccupied,countX,countY,lps.get(i))){
            		return false;
            	}
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
        return true;
    }
    
    private boolean fillOccupied(boolean[][] occupied,int x,int y, LayoutParams lp){
    	if(lp.cellHSpan == 1 && lp.cellVSpan == 1){
    		for(int i = 0;i< y;i++){
    			for(int j = 0;j< x;j++){
    				if (!occupied[j][i]){
    					occupied[j][i] = true;
    					return true;
    				}
    			}
    		}
    		return false;
    	}
    	
    	boolean found = false;
    	int keepheight = 0;
    	int startX = 0;//找到的放置矩形的区域的cellX
    	int startY = 0;//找到的放置矩形的区域的cellY
    	for(int checkY = 0; checkY < y; checkY++){
    		int thisLength = 0;
    		for(int checkX = 0;checkX < x; checkX++){
    			if(!occupied[checkX][checkY]){
    				thisLength++;
    			}
    			
    			if(thisLength >= lp.cellHSpan){
    				keepheight++;
    				startX = Math.max(startX, checkX + 1 - thisLength);
    				if(keepheight >= lp.cellVSpan){
    					//found
    					startY = checkY + 1 - lp.cellVSpan;
    					found = true;
    				}
    				break;
    			}
    		}
    		
    		if(found){
    			break;
    		}else if(thisLength < lp.cellHSpan){ //在某一行上太小，容不下
    			startX = 0;
    			keepheight = 0;
    		}
    	}
    	
    	//如果startX太靠下，也试着从最左边开始添加,先算出左边第一个未被占用的空格的索引
		int leftHeight = 0;
		for(int i = 0;i < y;i++){
			if(occupied[0][i]){
				leftHeight++;
			}else{
				break;
			}
		}
		
		boolean gotoLeft = false;
    	if(found && startX != 0){
    		if(startY + lp.cellVSpan > leftHeight){
    			//试着从最左边开始
    			if(startY + lp.cellVSpan <= y ){
    				gotoLeft = true;
    			}
    		}
    		
    		if(!gotoLeft){
    			for(int i = startY;i < startY + lp.cellVSpan;i++){
    				for(int j = startX;j < startX + lp.cellHSpan;i++){
    					occupied[j][i] = true;
    				}
    			}
    			return true;
    		}
    	}
    	/*else{
    		//太宽了？从最左边开始添加
    		if(startY + lp.cellVSpan <= y ){
    			gotoLeft = true;
    		}
    	}*/
    	
    	if(gotoLeft || (found && startX == 0)){
    		for(int i = leftHeight;i < leftHeight + lp.cellVSpan;i++){
				for(int j = 0;j < lp.cellHSpan;j++){
					occupied[j][i] = true;
				}
			}
    		return true;
    	}
    	
    	return false;
    	
    	//return true;
    }

    public boolean isDropLocationOccupied(int cellX, int cellY, int spanX, int spanY, View dragView) {
        getViewsIntersectingRegion(cellX, cellY, spanX, spanY, dragView, null, mIntersectingViews, false);
        return !mIntersectingViews.isEmpty();
    }
    
    public ArrayList<View> getDropLocationOccupiedViews(int cellX, int cellY, int spanX, int spanY, View dragView) {
        getViewsIntersectingRegion(cellX, cellY, spanX, spanY, dragView, null, mIntersectingViews, false);
        return mIntersectingViews;
    }
    
    public void getDropLocationOccupiedViews(int cellX, int cellY, int spanX, int spanY, View dragView, ArrayList<View> occupiedViews) {
        getViewsIntersectingRegion(cellX, cellY, spanX, spanY, dragView, null, occupiedViews, false);
    }

    public boolean isCurrentLocationOccupied(int cellX, int cellY, int spanX, int spanY, View dragView) {
        getViewsIntersectingRegion(cellX, cellY, spanX, spanY, dragView, null, mIntersectingViews, true);
        return !mIntersectingViews.isEmpty();
    }
    
    public static final int MODE_DRAG_OVER = 0;
    public static final int MODE_ON_DROP = 1;
    public static final int MODE_ON_DROP_EXTERNAL = 2;
    public static final int MODE_ACCEPT_DROP = 3;
    private static final boolean DESTRUCTIVE_REORDER = false;
    //private static final float REORDER_HINT_MAGNITUDE = 0.12f;
    private static final int REORDER_ANIMATION_DURATION = 150;

    private ArrayList<View> mIntersectingViews = new ArrayList<View>();

    //private boolean mItemPlacementDirty = false;

    //private Rect mOccupiedRect = new Rect();
    private int[] mDirectionVector = new int[2];
    int[] mPreviousReorderDirection = new int[2];
    private static final int INVALID_DIRECTION = -100;

    //private HashMap<CellLayout.LayoutParams, SymmetricalLinearTween> mReorderAnimators = new
      //      HashMap<CellLayout.LayoutParams, SymmetricalLinearTween>();
    //private HashMap<View, ReorderHintAnimation> mShakeAnimators = new HashMap<View, ReorderHintAnimation>();

    public void revertTempState() {
        if (!isItemPlacementDirty() || DESTRUCTIVE_REORDER) {
            return;
        }
        final int count = this.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = this.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY) {
                lp.tmpCellX = lp.cellX;
                lp.tmpCellY = lp.cellY;
                animateChildToPosition(child, lp.cellX, lp.cellY, REORDER_ANIMATION_DURATION,
                        0, false, false);
            }
        }
        completeAndClearReorderHintAnimations();
        setItemPlacementDirty(false);
    }

    public int[] createArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY,
            View dragView, int[] result, int resultSpan[], int mode) {
        findOccupiedCells(this.getCountX(), this.getCountY(), mOccupied, dragView);

        // First we determine if things have moved enough to cause a different layout
        // result = findNearestArea(pixelX, pixelY, spanX, spanY, result);

        // When we are checking drop validity or actually dropping, we don't recompute the
        // direction vector, since we want the solution to match the preview, and it's possible
        // that the exact position of the item has changed to result in a new reordering outcome.
        if ((mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL || mode == MODE_ACCEPT_DROP)
               && mPreviousReorderDirection[0] != INVALID_DIRECTION) {
            mDirectionVector[0] = mPreviousReorderDirection[0];
            mDirectionVector[1] = mPreviousReorderDirection[1];
            // We reset this vector after drop
            if (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                mPreviousReorderDirection[0] = INVALID_DIRECTION;
                mPreviousReorderDirection[1] = INVALID_DIRECTION;
            }
        } else {
            getDirectionVectorForDrop(pixelX, pixelY, spanX, spanY, dragView, mDirectionVector);
            mPreviousReorderDirection[0] = mDirectionVector[0];
            mPreviousReorderDirection[1] = mDirectionVector[1];
        }

        ItemConfiguration swapSolution = simpleSwap(pixelX, pixelY, minSpanX, minSpanY,
                 spanX,  spanY, mDirectionVector, dragView,  true,  new ItemConfiguration());

        // We attempt the approach which doesn't shuffle views at all
        ItemConfiguration noShuffleSolution = findConfigurationNoShuffle(pixelX, pixelY, minSpanX,
                minSpanY, spanX, spanY, dragView, new ItemConfiguration());

        ItemConfiguration finalSolution = null;
        if (swapSolution.isSolution && swapSolution.area() >= noShuffleSolution.area()) {
            finalSolution = swapSolution;
        } else if (noShuffleSolution.isSolution) {
            finalSolution = noShuffleSolution;
        }

        boolean foundSolution = true;
        if (!DESTRUCTIVE_REORDER) {
            setUseTempCoords(true);
        }

        if (finalSolution != null) {
            result[0] = finalSolution.dragViewX;
            result[1] = finalSolution.dragViewY;
            if (resultSpan != null) {
                resultSpan[0] = finalSolution.dragViewSpanX;
                resultSpan[1] = finalSolution.dragViewSpanY;
            }

            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            if (mode == MODE_DRAG_OVER || mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                if (!DESTRUCTIVE_REORDER) {
                    copySolutionToTempState(finalSolution, dragView);
                }
                setItemPlacementDirty(true);
                animateItemsToSolution(finalSolution, dragView, mode == MODE_ON_DROP);

                if (!DESTRUCTIVE_REORDER &&
                        (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL)) {
                    commitTempPlacement();
                    completeAndClearReorderHintAnimations();
                    setItemPlacementDirty(false);
                } else {
                    beginOrAdjustHintAnimations(finalSolution, dragView,
                            REORDER_ANIMATION_DURATION);
                }
            }
        } else {
            foundSolution = false;
            result[0] = result[1] = -1;
            if (resultSpan != null) {
                resultSpan[0] = resultSpan[1] = -1;
            }
        }

        if ((mode == MODE_ON_DROP || !foundSolution) && !DESTRUCTIVE_REORDER) {
            setUseTempCoords(false);
        }

        this.requestLayout();
        return result;
    }

    @SuppressWarnings("unused")
    private void debugArray(String tag, String name, boolean[][] array) {
        XLog.d(tag, name);
        if (array == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        int countX = array.length;
        int countY = array[0].length;
        for (int i = 0; i < countY; i++) {
            builder.delete(0, builder.length());
            for (int j = 0; j < countX; j++) {
                builder.append(array[j][i] ? "1 " : "0 ");
            }
            XLog.d(tag, builder.toString());
        }
    }


    private void copyCurrentStateToSolution(ItemConfiguration solution, boolean temp) {
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            CellAndSpan c;
            if (temp) {
                c = new CellAndSpan(lp.tmpCellX, lp.tmpCellY, lp.cellHSpan, lp.cellVSpan);
            } else {
                c = new CellAndSpan(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan);
            }
            solution.map.put(child, c);
        }
    }

    @Override
    protected void animateItemsToSolution(ItemConfiguration solution, View dragView, boolean
            commitDragView) {
        final int countX = this.getCountX();
        final int countY = this.getCountY();

        boolean[][] occupied = DESTRUCTIVE_REORDER ? mOccupied : mTmpOccupied;
        for (int i = 0; i < countX; i++) {
            for (int j = 0; j < countY; j++) {
                occupied[i][j] = false;
            }
        }

        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);
            if (child == dragView) {
                continue;
            }
            if (((LayoutParams)child.getLayoutParams()).isDragging) {
                continue;
            }
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
        }
    }

    // This method starts or changes the reorder hint animations
    private void beginOrAdjustHintAnimations(ItemConfiguration solution, View dragView, int delay) {
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);
            if (child == dragView) {
                continue;
            }
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.isDragging) {
                continue;
            }
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                ReorderHintAnimation rha = new ReorderHintAnimation(child, lp.cellX, lp.cellY,
                        c.x, c.y, c.spanX, c.spanY);
                rha.animate();
            }
        }
    }

    @Override
    public boolean animateChildToPosition(final View child, int cellX, int cellY, int duration,
            int delay, boolean permanent, boolean adjustOccupied) {
        boolean[][] occupied = mOccupied;
        if (!permanent) {
            occupied = mTmpOccupied;
        }

        if (this.indexOfChild(child) != -1) {
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final ItemInfo info = (ItemInfo) child.getTag();

            // We cancel any existing animations
            if (mReorderAnimators.containsKey(lp)) {
                // Utils.cancelAnimation(mReorderAnimators.get(lp));
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
            this.setupLp(lp);
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

            SymmetricalLinearTween tween = new SymmetricalLinearTween(false, duration, new TweenCallback() {
                boolean cancelled = false;
                boolean invalidateRequired = ViewUtils.isViewUseHardwareLayer(WorkspaceCellLayout.this);
                @Override
                public void onTweenValueChanged(float value, float oldValue) {
                    lp.x = (int) ((1 - value) * oldX + value * newX);
                    lp.y = (int) ((1 - value) * oldY + value * newY);
                    child.requestLayout();
                    if (invalidateRequired) {
                        invalidate();
                    }
                }

                @Override
                public void onTweenStarted() {
                }

                @Override
                public void onTweenCancelled() {
                    cancelled = true;
                }

                @Override
                public void onTweenFinished() {
                    // If the animation was cancelled, it means that another animation
                    // has interrupted this one, and we don't want to lock the item into
                    // place just yet.
                    if (!cancelled) {
                        lp.isLockedToGrid = true;
                        child.requestLayout();
                        if (invalidateRequired) {
                            invalidate();
                        }
                    }
                    if (mReorderAnimators.containsKey(lp)) {
                        mReorderAnimators.remove(lp);
                    }
                }
            }, ScreenDimensUtils.getDefaultDisplay(this.getContext()).getRefreshRate());

            mReorderAnimators.put(lp, tween);

            tween.start(true);
            return true;
        }
        return false;
    }

    @Override
    protected void completeAndClearReorderHintAnimations() {
        for (ReorderHintAnimation a: mShakeAnimators.values()) {
            a.completeAnimationImmediately();
        }
        mShakeAnimators.clear();
    }

    @Override
    protected void commitTempPlacement() {
        final int countX = this.getCountX();
        final int countY = this.getCountY();

        for (int i = 0; i < countX; i++) {
            for (int j = 0; j < countY; j++) {
                mOccupied[i][j] = mTmpOccupied[i][j];
            }
        }
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            HomeItemInfo info = (HomeItemInfo) child.getTag();
            // We do a null check here because the item info can be null in the case of the
            // AllApps button in the hotseat.
            if (info != null && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY)) {
                info.cellX = lp.cellX = lp.tmpCellX;
                info.cellY = lp.cellY = lp.tmpCellY;
                info.spanX = lp.cellHSpan;
                info.spanY = lp.cellVSpan;

                DbManager.moveItemInDatabase(this.getContext(), info);
            }
        }
    }

    public void setUseTempCoords(boolean useTempCoords) {
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams) this.getChildAt(i).getLayoutParams();
            lp.useTmpCoords = useTempCoords;
        }
    }

    private ItemConfiguration findConfigurationNoShuffle(int pixelX, int pixelY, int minSpanX, int minSpanY,
            int spanX, int spanY, View dragView, ItemConfiguration solution) {
        int[] result = new int[2];
        int[] resultSpan = new int[2];
        findNearestVacantArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, null, result,
                resultSpan);
        if (result[0] >= 0 && result[1] >= 0) {
            copyCurrentStateToSolution(solution, false);
            solution.dragViewX = result[0];
            solution.dragViewY = result[1];
            solution.dragViewSpanX = resultSpan[0];
            solution.dragViewSpanY = resultSpan[1];
            solution.isSolution = true;
        } else {
            solution.isSolution = false;
        }
        return solution;
    }

    /* This seems like it should be obvious and straight-forward, but when the direction vector
    needs to match with the notion of the dragView pushing other views, we have to employ
    a slightly more subtle notion of the direction vector. The question is what two points is
    the vector between? The center of the dragView and its desired destination? Not quite, as
    this doesn't necessarily coincide with the interaction of the dragView and items occupying
    those cells. Instead we use some heuristics to often lock the vector to up, down, left
    or right, which helps make pushing feel right.
    */
    private void getDirectionVectorForDrop(int dragViewCenterX, int dragViewCenterY, int spanX,
            int spanY, View dragView, int[] resultDirection) {
        int[] targetDestination = new int[2];

        findNearestArea(dragViewCenterX, dragViewCenterY, spanX, spanY, targetDestination);
        // Rect dragRect = new Rect();
        // regionToRect(targetDestination[0], targetDestination[1], spanX, spanY, dragRect);
        // dragRect.offset(dragViewCenterX - dragRect.centerX(), dragViewCenterY - dragRect.centerY());

        Rect dropRegionRect = new Rect();
        getViewsIntersectingRegion(targetDestination[0], targetDestination[1], spanX, spanY,
                dragView, dropRegionRect, mIntersectingViews, false);

        int dropRegionSpanX = dropRegionRect.width();
        int dropRegionSpanY = dropRegionRect.height();

        regionToRect(dropRegionRect.left, dropRegionRect.top, dropRegionRect.width(),
                dropRegionRect.height(), dropRegionRect);

        int deltaX = (dropRegionRect.centerX() - dragViewCenterX) / spanX;
        int deltaY = (dropRegionRect.centerY() - dragViewCenterY) / spanY;

        if (dropRegionSpanX == this.getCountX() || spanX == this.getCountX()) {
            deltaX = 0;
        }
        if (dropRegionSpanY == this.getCountY() || spanY == this.getCountY()) {
            deltaY = 0;
        }

        if (deltaX == 0 && deltaY == 0) {
            // No idea what to do, give a random direction.
            resultDirection[0] = 1;
            resultDirection[1] = 0;
        } else {
            computeDirectionVector(deltaX, deltaY, resultDirection);
        }
    }

    /*
     * Returns a pair (x, y), where x,y are in {-1, 0, 1} corresponding to vector between
     * the provided point and the provided cell
     */
    private void computeDirectionVector(float deltaX, float deltaY, int[] result) {
        double angle = Math.atan((deltaY) / deltaX);

        result[0] = 0;
        result[1] = 0;
        if (Math.abs(Math.cos(angle)) > 0.5f) {
            result[0] = (int) Math.signum(deltaX);
        }
        if (Math.abs(Math.sin(angle)) > 0.5f) {
            result[1] = (int) Math.signum(deltaY);
        }
    }

    // For a given cell and span, fetch the set of views intersecting the region.
    private void getViewsIntersectingRegion(int cellX, int cellY, int spanX, int spanY,
            View dragView, Rect boundingRect, ArrayList<View> intersectingViews, boolean useTmpCoords) {
        if (boundingRect != null) {
            boundingRect.set(cellX, cellY, cellX + spanX, cellY + spanY);
        }
        intersectingViews.clear();
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();
        final int count = this.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = this.getChildAt(i);
            if (child == dragView) {
                continue;
            }
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.isDragging) {
                continue;
            }
            if (useTmpCoords && lp.useTmpCoords) {
                r1.set(lp.tmpCellX, lp.tmpCellY, lp.tmpCellX + lp.cellHSpan, lp.tmpCellY + lp.cellVSpan);
            } else {
                r1.set(lp.cellX, lp.cellY, lp.cellX + lp.cellHSpan, lp.cellY + lp.cellVSpan);
            }
            if (Rect.intersects(r0, r1)) {
                intersectingViews.add(child);
                if (boundingRect != null) {
                    boundingRect.union(r1);
                }
            }
        }
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
     * @param ignoreView Considers space occupied by this view as unoccupied
     * @param result Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    int[] findNearestVacantArea(int pixelX, int pixelY, int minSpanX, int minSpanY,
            int spanX, int spanY, View ignoreView, int[] result, int[] resultSpan) {
        return findNearestArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, ignoreView, true,
                result, resultSpan, mOccupied);
    }

    /**
     * 为单层模式添加
     * 查找下一个空的cell
     * XXX 能否使用mOccupied？
     * @return int[0]为屏号， int[1]为cellX，int[2]为cellY
     */
    public int[] findFirstEmptyCell(int screenNum) {
        int countX = getCountX();
        int countY = getCountY();
        boolean[][] occupied = new boolean[countX][countY];
        findOccupiedCells(countX, countY, occupied, null);
        for (int j = 0; j < countY; j++) {
            for (int i = 0; i < countX; i++) {
                if (!occupied[i][j]) {
                    return new int[]{screenNum, i, j};
                }
            }
        }
        return null;
    }

    /**
     * 找到当前屏的最后一个有元素的位置的下一个空位。中间的空位不予考虑
     * @param screenNum
     * @return
     */
    public int[] findNextEmptyCell(int screenNum) {
        int countX = getCountX();
        int countY = getCountY();
        boolean[][] occupied = new boolean[countX][countY];
        findOccupiedCells(countX, countY, occupied, null);
        int[] lastOccupied = new int[]{-1, -1};
        for (int j = 0; j < countY; j++) {
            for (int i = 0; i < countX; i++) {
                if (occupied[i][j]) {
                    lastOccupied[0] = i;
                    lastOccupied[1] = j;
                }
            }
        }
        if (lastOccupied[0] == -1 && lastOccupied[1] == -1) {
            return new int[]{screenNum, 0, 0};
        }
        if (lastOccupied[0] == countX - 1 && lastOccupied[1] == countY - 1) {
            return null;
        }

        return new int[]{screenNum, lastOccupied[0] == countX - 1 ? 0 : lastOccupied[0] + 1, lastOccupied[1] + (lastOccupied[0] == countX - 1 ? 1 : 0)};
    }
    
    /*
     * 找到当前屏幕最后一个ShortCut元素的位置
     * */
    public boolean findLastShortCutCell(int [] location){
    	if(location == null || location.length < 2) return false;
    	
    	 int countX = getCountX();
         int countY = getCountY();
         boolean[][] occupied = new boolean[countX][countY];
         findOccupiedCells(countX, countY, occupied, null);
         
         for(int j = countY -1 ; j >= 0;j--){
        	 for(int i = countX - 1;i >= 0;i--){
        		 if(occupied[i][j] ){
        			 View view = getCellView(i, j);
        			 if(view instanceof Shortcut){
        				 location[0] = i;
        				 location[1] = j;
        				 return true;
        			 }
        		 }
        	 }
         }
         
         return false;
    }
    
    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if(mLayoutStatus == WorkspaceCellLayout.DRAGING){
            return true;
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mTextRect.contains((int)event.getX(), (int)event.getY() + getScrollY()) &&
                event.getAction() == MotionEvent.ACTION_UP) {
        }
        
        if(event.getAction() == MotionEvent.ACTION_UP && getScaleY() != 0){
        	flingTo(true);
        }
        
        return super.onTouchEvent(event);
    }

    public boolean isDraging(){
        return mLayoutStatus == WorkspaceCellLayout.DRAGING;
    }

    public void springBack() {
        if(mScroller.springBack(0, getScrollY(), 0, 0, (int) -(getHeight()/3), 0)) {
            invalidate();
        }
    }
    
    public void flingTo (boolean top) {
        mScroller.fling(0, getScrollY(), 0, top ? 8000 : -8000 , 0, 0, -(getHeight()/3), 0);
        invalidate();
    }
    
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int sy = mScroller.getCurrY();
            scrollTo(0, sy);
            awakenScrollBars();
            postInvalidate();
        }
        if(getScrollY() == 0) {
            setChildAlpha(true);
        } else {
            setChildAlpha(false);
        }
    }
    
    public void setChildAlpha(boolean dontalpha) {

        ScreenIndicator indicator = (ScreenIndicator) ((View) getRootView()).findViewById(R.id.indicator);
        int scrolly = getScrollY();
        //int[] newHomeLayout = SettingPreferences.getHomeLayout(getContext());
        int[] newHomeLayout = getLayout();
        //default 4*4
        int nCurLines = 4;
        int nCurColus = 4;
        if(newHomeLayout != null){
        	nCurLines = newHomeLayout[0];
        	nCurColus = newHomeLayout[1];
        }
        
        int nDespearCnts = nCurLines - 1;
        final int nHeightThreshold = indicator.getTop() - 50;
        for(int i = nDespearCnts;i > 0;i--){
        	int nCurLineHeight = (int)(mCellLayoutHeight + Math.abs(scrolly) - Math.abs(i - nDespearCnts) * getCellHeight());
        	
        	if(nCurLineHeight >= nHeightThreshold && !dontalpha){
        		for(int j = 0; j < nCurColus;j++){
        			final View curView = getCellView(j, i);
        			if(curView != null && curView.getVisibility() == VISIBLE){
        				curView.setVisibility(GONE);
        			}
        		}
        	}
        	else {
        		for(int j = 0; j < nCurColus;j++){
        			final View curView = getCellView(j, i);
        			if(curView != null && curView.getVisibility() == GONE){
        				curView.setVisibility(VISIBLE);
        			}
        		}
			}
        }
    }

    @Override
    public boolean isDeletable(Context context) {
        return getChildCount() == 0 ? true : false;
    }

    @Override
    public String getLabel(Context context) {
        if(getChildCount() == 0){
            //return getResources().getString(R.string.screen_manager_screen_deletable);
        }else{
            return getResources().getString(R.string.screen_manager_screen_not_deletable_non_empty);
        }
        return "";
    }

    @Override
    public int getDeleteZoneIcon(Context context) {
        return 0;
    }

    @Override
    public boolean onDelete(Context context) {
        ViewParent vp = getParent();
        if(vp instanceof Workspace){
            ((Workspace)vp).removeScreen(this,true);
        }
        return true;
    }
    
}
