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

package cc.snser.launcher.ui.dragdrop;

import android.R.bool;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.FloatMath;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import cc.snser.launcher.AbstractWorkspace;
import cc.snser.launcher.App;
import cc.snser.launcher.CellLayout;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.Utils;
import cc.snser.launcher.apps.components.AppIcon;
import cc.snser.launcher.apps.model.ItemInfo;
import cc.snser.launcher.screens.DeleteZone;
import cc.snser.launcher.screens.NotifyZone;
import cc.snser.launcher.screens.Workspace;
import cc.snser.launcher.screens.WorkspaceCellLayout;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;
import cc.snser.launcher.widget.Widget;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.util.BitmapUtils;
import com.shouxinzm.launcher.util.ScreenDimensUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

/**
 * Class for initiating a drag within a view or across multiple views.
 */
public class DragController {
    private static final String TAG = "Launcher.DragController";

    /** Indicates the drag is a move.  */
    public static final int DRAG_ACTION_MOVE = 0;

    /** Indicates the drag is a copy.  */
    public static final int DRAG_ACTION_COPY = 1;

    /** Indicates the drag is a copy.  */
    public static final int DRAG_ACTION_INVISIBLE = 2;
    
    /** Indicates the drag is a copy.  */
    //public static final int DRAG_ACTION_REPLACE = 2;

    public static final int SCROLL_LEFT = 0;
    public static final int SCROLL_RIGHT = 1;

    private static final int SCROLL_DELAY = 600;
    private static final int VIBRATE_DURATION = 35;

    private static final boolean PROFILE_DRAWING_DURING_DRAG = false;

    private static final int SCROLL_OUTSIDE_ZONE = 0;
    private static final int SCROLL_WAITING_IN_ZONE = 1;

    private Context mContext;
    private Handler mHandler;
    private final Vibrator mVibrator;

    // temporaries to avoid gc thrash
    private Rect mRectTemp = new Rect();
    private final int[] mCoordinatesTemp = new int[2];
    private final int[] mCoordinatesTemp2 = new int[2];
    private int[] mLastMoveActionCoordinate = new int[2];

    private final PointF mVelocity = new PointF();

    /** Whether or not we're dragging. */
    private boolean mDragging;

    /** X coordinate of the down event. */
    private float mMotionDownX;

    /** Y coordinate of the down event. */
    private float mMotionDownY;

    /** Original view that is being dragged.  */
    private View mOriginator;

    private View mDropAnimatingView;

    private int mScreenX;
    private int mScreenY;

    /** the area at the edge of the screen that makes the workspace go left
     *   or right while you're dragging.
     */
    private int mScrollZone;

    /** The view that moves around while you drag.  */
    private DropTarget.DragObject mDragObject = new DropTarget.DragObject();

    /** Who can receive drop events */
    private ArrayList<DropTarget> mDropTargets = new ArrayList<DropTarget>();

    /** Who can care drop events (start and end)*/
    private ArrayList<DragListener> mListeners = new ArrayList<DragListener>();

    /** The window token used as the parent for the DragView. */
    private IBinder mWindowToken;

    /** The view that will be scrolled when dragging to the left and right edges of the screen. */
    private View mScrollView;

    private View mMoveTarget;

    private ArrayList<DragScroller> mDragScrollers = new ArrayList<DragScroller>();
    private int mScrollState = SCROLL_OUTSIDE_ZONE;
    private ScrollRunnable mScrollRunnable = new ScrollRunnable();

    private RectF mDeleteRegion;
    private DropTarget mLastDropTarget;

    private InputMethodManager mInputMethodManager;

    private boolean mEnableCoordinateTransform = true;
    private boolean mTransformStateChanging = false;

    /**
     * 当前drag move时，是否发生了目标的变化。用于在dragExit时，判断是drop方法还是拖放出了本身的区域来导致dragTargetChange的变化
     */
    private boolean dropTargetChanging;
    private Vector<CreateOutlineListener> mCreateOutlineListener = new Vector<DragController.CreateOutlineListener>();

    private VelocityTracker mVelocityTracker;
    

    private boolean mIsMoving = false;

    /**
     * Interface to receive notifications when a drag starts or stops
     */
    public interface DragListener {

        /**
         * A drag has begun
         *
         * @param source An object representing where the drag originated
         * @param info The data associated with the object that is being dragged
         * @param dragAction The drag action: either {@link DragController#DRAG_ACTION_MOVE}
         *        or {@link DragController#DRAG_ACTION_COPY}
         */
        void onDragStart(DragSource source, Object info, int dragAction);

        /**
         * The drag has eneded
         */
        void onDragEnd(boolean immediately);
    }

    public interface CreateOutlineListener {
        void onCreateOutline(Object dragInfo, View view);
        void onCreateOutline(Object dragInfo, Drawable drawable);
    }
    /**
     * Used to create a new DragLayer from XML.
     *
     * @param context The application's context.
     */
    public DragController(Context context) {
        Resources r = context.getResources();

        mContext = context;
        mHandler = new Handler();
        mScrollZone = r.getDimensionPixelSize(R.dimen.scroll_zone);
        mVelocityTracker = VelocityTracker.obtain();
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void setCreateOutline(CreateOutlineListener listener) {
    	if(listener != null){
    		mCreateOutlineListener.add(listener);
    	}
    }

    public void startDrag(View v, DragSource source, Object dragInfo, int dragAction, boolean vibrate, Drawable drawable) {
    	for(CreateOutlineListener createOutlineListener : mCreateOutlineListener){
    		createOutlineListener.onCreateOutline(dragInfo, v);
    	}
    	
        startDrag(v, source, dragInfo, DragController.DRAG_ACTION_COPY, vibrate, false);
    }

    /**
     * Starts a drag.
     *
     * @param v The view that is being dragged
     * @param source An object representing where the drag originated
     * @param dragInfo The data associated with the object that is being dragged
     * @param dragAction The drag action: either {@link #DRAG_ACTION_MOVE} or
     *        {@link #DRAG_ACTION_COPY}
     */
    public boolean startDrag(View v, DragSource source, Object dragInfo, int dragAction, boolean vibrate, boolean haveShadow) {
        mOriginator = v;
        
        Bitmap b = getViewBitmap(v, false);
        if (b == null) {
            // out of memory?
            return false;
        }
        int[] loc = mCoordinatesTemp;
        v.getLocationOnScreen(loc);
        mScreenX = loc[0];
        mScreenY = loc[1];
        
        //设置lastDrop
        /*final int[] coordinates = mCoordinatesTemp;
        DropTarget dropTarget = findDropTarget(mScreenX, mScreenY, coordinates);
        if (dropTarget != null) {
            mDragObject.x = coordinates[0];
            mDragObject.y = coordinates[1];
            mLastDropTarget = dropTarget;
        }*/

        int w = v.getMeasuredWidth();
        int h = v.getMeasuredHeight();

        if (mScreenX > mMotionDownX || mScreenX + w < mMotionDownX
                || mScreenY > mMotionDownY || mScreenY + h < mMotionDownY) {
            mScreenX = (int) (mMotionDownX - w / 2f);
            mScreenY = (int) (mMotionDownY - h / 2f);
        }

        if (haveShadow) {        	
            createOutline(dragInfo);
        }
        boolean result = startDrag(b, 0, 0, b.getWidth(), b.getHeight(), source, dragInfo, dragAction, vibrate);
        if (!result) {
            return result;
        }
        if (dragAction == DRAG_ACTION_MOVE) {
            v.setVisibility(View.GONE);
        } else if (dragAction == DRAG_ACTION_INVISIBLE) {
            v.setVisibility(View.INVISIBLE);
        }
        return result;
    }

    private void createOutline(Object dragInfo) {
    	for(CreateOutlineListener createOutlineListener : mCreateOutlineListener){
    		createOutlineListener.onCreateOutline(dragInfo, mOriginator);
    	}
    }

    /**
     * Starts a drag.
     *
     * @param b The bitmap to display as the drag image.  It will be re-scaled to the
     *          enlarged size.
     * @param screenX The x position on screen of the left-top of the bitmap.
     * @param screenY The y position on screen of the left-top of the bitmap.
     * @param textureLeft The left edge of the region inside b to use.
     * @param textureTop The top edge of the region inside b to use.
     * @param textureWidth The width of the region inside b to use.
     * @param textureHeight The height of the region inside b to use.
     * @param source An object representing where the drag originated
     * @param dragInfo The data associated with the object that is being dragged
     * @param dragAction The drag action: either {@link #DRAG_ACTION_MOVE} or
     *        {@link #DRAG_ACTION_COPY}
     */
    private boolean startDrag(Bitmap b, int textureLeft, int textureTop, int textureWidth, int textureHeight,
            DragSource source, Object dragInfo, int dragAction, boolean vibrate) {
        if (PROFILE_DRAWING_DURING_DRAG) {
            android.os.Debug.startMethodTracing("Launcher");
        }

        // Hide soft keyboard, if visible
        if (mInputMethodManager == null) {
            mInputMethodManager = (InputMethodManager)
                    mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        try {
            mInputMethodManager.hideSoftInputFromWindow(mWindowToken, 0);
        } catch (Exception e) {
            // ignore
        }

        //onDragStart();
        for (DragListener listener : mListeners) {
            listener.onDragStart(source, dragInfo, dragAction);
        }

        int registrationX = ((int) mMotionDownX) - mScreenX;
        int registrationY = ((int) mMotionDownY) - mScreenY;
        float touchOffsetX;
        float touchOffsetY;

        touchOffsetX = mMotionDownX - mScreenX;
        touchOffsetY = mMotionDownY - mScreenY;
        
        if (Workspace.sInEditMode && mContext instanceof Launcher) {
            touchOffsetX /= Workspace.sEditModeScaleRatio;
            touchOffsetY /= Workspace.sEditModeScaleRatio;
        }

        mDragObject.isCancelingDrag = false;
        mDragObject.dragComplete = false;
        mDragObject.xOffset = (int) touchOffsetX;
        mDragObject.yOffset = (int) touchOffsetY;
        mDragObject.dragSource = source;
        mDragObject.dragInfo = dragInfo;
        mDragObject.deferDragViewCleanupPostAnimation = false;

        if (vibrate && isVibrate(mContext)) {
            mVibrator.vibrate(VIBRATE_DURATION);
        }

        try {
            if (mDragObject.dragView != null) {
                mDragObject.dragView.closeInstance();
            }
            
            int offsetwidth = 0; 
            if (dragInfo instanceof WorkspaceCellLayout) {
            	DragView dragView = mDragObject.dragView = new DragView(mContext, b, registrationX, registrationY,
                        textureLeft, textureTop, textureWidth, textureHeight, DragView.TYPE_DRAG_VIEW, false);
            	
            	offsetwidth = textureWidth / 2;
            	
            	dragView.show(mWindowToken, (int) mMotionDownX, (int) mMotionDownY,  255, 0.5f);
            }else{
            	DragView dragView = mDragObject.dragView = new DragView(mContext, b, registrationX, registrationY,
                        textureLeft, textureTop, textureWidth, textureHeight, DragView.TYPE_DRAG_VIEW, true);
            	dragView.show(mWindowToken, (int) mMotionDownX, (int) mMotionDownY);
            }
            mDragging = true;
        } catch (OutOfMemoryError e) {
            this.cancelDrag(false);
            mDragging = false;
        }
        return mDragging;
    }

    public void onDragStart() {
        if (mLastDropTarget == null) {
            final int[] coordinates = mCoordinatesTemp;
            DropTarget dropTarget = findDropTarget(mScreenX, mScreenY, coordinates);
            if (dropTarget != null) {
                mDragObject.x = coordinates[0];
                mDragObject.y = coordinates[1];
                dropTarget.onDragEnter(mDragObject);
                mLastDropTarget = dropTarget;
            }
        }
    }

    private boolean isVibrate(Context context) {
        try {
            if (Settings.System.getInt(context.getApplicationContext().getContentResolver(), "haptic_feedback_enabled", 0) != 0) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Draw the view into a bitmap.
     */
    private Bitmap getViewBitmap(View v, boolean ignoreScale) {
        try {
            if (v instanceof AppIcon) {
                v = ((AppIcon) v).getMainView();
            }

            v.clearFocus();
            v.setPressed(false);

            boolean willNotCache = v.willNotCacheDrawing();
            v.setWillNotCacheDrawing(false);

            // Reset the drawing cache background color to fully transparent
            // for the duration of this operation
            int color = v.getDrawingCacheBackgroundColor();
            v.setDrawingCacheBackgroundColor(0);

            if (color != 0) {
                v.destroyDrawingCache();
            }
            if(v instanceof WorkspaceCellLayout){
            	((WorkspaceCellLayout) v).setBuildDragingCache(true);
            }
            v.buildDrawingCache();
            Bitmap cacheBitmap = v.getDrawingCache();
            if (cacheBitmap == null) {
            	if(v instanceof WorkspaceCellLayout){
            		((WorkspaceCellLayout) v).setBuildDragingCache(false);
                }
            	
                XLog.e(TAG, "failed getViewBitmap(" + v + ")", new RuntimeException());
                return null;
            }
            
            if(v instanceof WorkspaceCellLayout){
            	((WorkspaceCellLayout) v).setBuildDragingCache(false);
            }
            
            Bitmap bitmap = (Workspace.sInEditMode && mContext instanceof Launcher) && !ignoreScale ? 
            			BitmapUtils.createScaledBitmap(cacheBitmap, Workspace.sEditModeScaleRatio, true) : BitmapUtils.createBitmap(cacheBitmap);
            
            // Restore the view
            v.destroyDrawingCache();
            v.setWillNotCacheDrawing(willNotCache);
            v.setDrawingCacheBackgroundColor(color);
            return bitmap;
        } catch (OutOfMemoryError e) {
            XLog.e(TAG, "e  = " + e.toString());
            return null;
        }
    }
    
    /**
     * Call this from a drag source view like this:
     *
     * <pre>
     *  @Override
     *  public boolean dispatchKeyEvent(KeyEvent event) {
     *      return mDragController.dispatchKeyEvent(this, event)
     *              || super.dispatchKeyEvent(event);
     * </pre>
     */
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mDragging;
    }

    /**
     * Stop dragging without dropping.
     */
    public void cancelDrag(boolean immediately) {
        if (mDragging) {
            if (mLastDropTarget != null) {
                dropTargetChanging = true;
                mLastDropTarget.onDragExit(mDragObject, null);
            }
            mDragObject.isCancelingDrag = true;
            mDragObject.dragComplete = true;
            if (mDragObject.dragSource != null) {
                mDragObject.dragSource.onDropCompleted(null, false);
            }
        }
        endDrag(immediately);
    }

    private void endDrag(boolean immediately) {
    	NotifyZone zone = App.getApp().getLauncher().getNotifyZone(); 
        if( zone != null){
        	zone.hide();
        }
        
        if (mDragging) {
            mDragging = false;
            if (mOriginator != null && mOriginator != mDropAnimatingView) {
                mOriginator.setVisibility(View.VISIBLE);
            }
            boolean isDeferred = false;
            if (mDragObject.dragView != null) {
                isDeferred = mDragObject.deferDragViewCleanupPostAnimation;
                if (!isDeferred) {
                	if(!mDragObject.dragView.hasReboundAnimation()){
                		mDragObject.dragView.remove();
                	}
                }
                mDragObject.dragView = null;
            }
            if (mDragObject.dragSource != null) {
                mDragObject.dragSource = null;
            }
            if (!isDeferred) {
                for (DragListener listener : mListeners) {
                    listener.onDragEnd(immediately);
                }
            }
            mOriginator = null;
            mDropAnimatingView = null;
        }

        mScrollRunnable.cancelRun();

        releaseVelocityTracker();

        mEnableCoordinateTransform = true;
    }

    public void onDeferredEndDrag(DragView dragView) {
        if (dragView != null) {
            dragView.remove();
        }

        // If we skipped calling onDragEnd() before, do it now
        for (DragListener listener : mListeners) {
            listener.onDragEnd(false);
        }
    }

    /**
     * Call this from a drag source view.
     */
    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        if (false) {
//            XLog.d(Launcher.TAG, "DragController.onInterceptTouchEvent " + ev + " mDragging="
//                    + mDragging);
//        }

        final int action = ev.getAction();

        // Update the velocity tracker
        acquireVelocityTrackerAndAddMovement(ev);

        final int screenX = clamp((int) ev.getRawX(), 0, ScreenDimensUtils.getScreenWidthPixels(mContext));
        final int screenY = clamp((int) ev.getRawY(), 0, ScreenDimensUtils.getScreenHeightPixels(mContext));
        final int realScreenX;
        final int realScreenY;

        if (Workspace.sInEditMode && mContext instanceof Launcher && mEnableCoordinateTransform) {
            int centerX = ScreenDimensUtils.getScreenWidth(mContext) / 2;
            int centerY = CellLayout.getSmartTopPadding();
            realScreenX = (int) Utils.scaleFrom(screenX, centerX, Workspace.sEditModeScaleRatio);
            realScreenY = (int) Utils.scaleFrom(screenY, centerY, Workspace.sEditModeScaleRatio);
        } else {
            realScreenX = screenX;
            realScreenY = screenY;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE:
            	if(mDragging){
            		mIsMoving = true;	
            	}
                break;

            case MotionEvent.ACTION_DOWN:
                // Remember location of down touch
                mMotionDownX = screenX;
                mMotionDownY = screenY;
                mLastDropTarget = null;
                mIsMoving = false; 
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mDragging) {
                    computeDropVelocity();

                    getCurrentVelocity(mVelocity);
                    drop(realScreenX, realScreenY, screenX, screenY, mVelocity);
                }
                endDrag(false);
                mIsMoving = false; 
                break;
        }

        return mDragging && ev.getPointerCount() == 1;
    }

    private final void getCurrentVelocity(PointF vel) {
        ViewConfiguration config = ViewConfiguration.get(mContext);
        mVelocityTracker.computeCurrentVelocity(1000, config.getScaledMaximumFlingVelocity());
        vel.x = mVelocityTracker.getXVelocity();
        vel.y = mVelocityTracker.getYVelocity();
    }

    private void computeDropVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000, ViewConfiguration.get(mContext).getScaledMaximumFlingVelocity());
        mDragObject.velocityX = Math.max(Math.min(100, mVelocityTracker.getXVelocity()), -100); // restrict to the range from -100 to 100.
        mDragObject.velocityY = Math.max(Math.min(100, mVelocityTracker.getYVelocity()), -100);
    }

    /**
     * Sets the view that should handle move events.
     */
    public void setMoveTarget(View view) {
        mMoveTarget = view;
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        return mMoveTarget != null && mMoveTarget.dispatchUnhandledMove(focused, direction);
    }

    /**
     * Call this from a drag source view.
     * 在迅速长按拖动释放的情况下，只会触发ACTION_UP 即 onDrop的事件
     */
    public boolean onTouchEvent(MotionEvent ev) {
        View scrollView = mScrollView;
        if (!mDragging) {
            return false;
        }

        if (mContext instanceof Launcher) {
            final Launcher launcher = ((Launcher) mContext);
            AbstractWorkspace workspace = launcher.getWorkspace();

            if (workspace != null && workspace.isShown()) {
                workspace.handleSecondFingerGusture(ev);
            }
        }

        // Update the velocity tracker
        acquireVelocityTrackerAndAddMovement(ev);

        final int action = ev.getAction();
        final int screenX = clamp((int) ev.getRawX(), 0, ScreenDimensUtils.getScreenWidthPixels(mContext));
        final int screenY = clamp((int) ev.getRawY(), 0, ScreenDimensUtils.getScreenHeightPixels(mContext));
        final int realScreenX;
        final int realScreenY;

        if (Workspace.sInEditMode && mContext instanceof Launcher && mEnableCoordinateTransform) {
            int centerX = ScreenDimensUtils.getScreenWidth(mContext) / 2;
            int centerY = CellLayout.getSmartTopPadding();
            realScreenX = (int) Utils.scaleFrom(screenX, centerX, Workspace.sEditModeScaleRatio);
            realScreenY = (int) Utils.scaleFrom(screenY, centerY, Workspace.sEditModeScaleRatio);
        } else {
            realScreenX = screenX;
            realScreenY = screenY;
        }

        boolean scrollableForScreenY = realScreenY < mScrollView.getHeight();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // Remember where the motion event started
                mMotionDownX = screenX;
                mMotionDownY = screenY;

                if (scrollableForScreenY && ((realScreenX < mScrollZone) || (realScreenX > scrollView.getWidth() - mScrollZone))) {
                    mScrollState = SCROLL_WAITING_IN_ZONE;
                    mScrollRunnable.beforeRun();
                    mHandler.postDelayed(mScrollRunnable, SCROLL_DELAY);
                } else {
                    mScrollState = SCROLL_OUTSIDE_ZONE;
                }

                break;
            case MotionEvent.ACTION_MOVE:
            	onActionMove(ev,realScreenX,realScreenY,scrollableForScreenY);
                break;
            case MotionEvent.ACTION_UP:
                mHandler.removeCallbacks(mScrollRunnable);
                boolean bImmediate = false;
                if (mDragging) {
                    getCurrentVelocity(mVelocity);
                    if (mLastDropTarget instanceof DeleteZone) {
                    	bImmediate = drop((int) (mDragObject.dragView.getX() + mDragObject.dragView.getWidth() / 2), (int) (mDeleteRegion.top + 6), screenX, screenY, mVelocity);
                    } else {
                    	bImmediate = drop(realScreenX, realScreenY, screenX, screenY, mVelocity);
                    }
                }
                endDrag(bImmediate);
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelDrag(false);
        }

        return false;
    }
    
    private void onActionMove(MotionEvent ev,int realScreenX,int realScreenY,boolean scrollableForScreenY){
    	dropTargetChanging = false;
        // Update the drag view.  Don't use the clamped pos here so the dragging looks
        // like it goes off screen a little, intead of bumping up against the edge.
        mDragObject.dragView.move((int) ev.getRawX(), (int) ev.getRawY());

        mLastMoveActionCoordinate[0] = (int)ev.getRawX();
        mLastMoveActionCoordinate[1] = (int)ev.getRawY();
        
        //在顶部的最上方，realScreenY可能返回负数导致又退出了DeleteZone区域，此时再放手，会导致无法删除
        realScreenY = realScreenY < 0 ?  0 : realScreenY;

        // Drop on someone?
        final int[] coordinates = mCoordinatesTemp;

        // Scroll, maybe, but not if we're in the delete region.
        int heightdragView = mDragObject.dragView.getHeight();
        boolean inDeleteRegion = false;
        DropTarget dropTarget = null;
        if (mDragObject.dragInfo instanceof ItemInfo) {
            ItemInfo itemInfo = (ItemInfo) mDragObject.dragInfo;
            if (mDeleteRegion != null) {
                if (mDragObject.dragView.getY() < 0) {
                    inDeleteRegion = true;
                } else {
                    float ration = 0.5f;
                    Configuration conf = mContext.getResources().getConfiguration();
                    if (itemInfo.spanY > 1) {
                        ration = 0.5f;
                        if (conf.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            ration = 0.8f;
                        }
                    } else {
                        ration = 0.7f;
                        if (conf.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            ration = 0.9f;
                        }
                    }
                    inDeleteRegion = mDeleteRegion.contains(mDragObject.dragView.getX(), mDragObject.dragView.getY() + heightdragView * ration);
                }
                if (inDeleteRegion) {
                    dropTarget = findDropTarget((int) (mDragObject.dragView.getX() + mDragObject.dragView.getWidth() / 2), (int) (mDeleteRegion.top + 6), coordinates);
                }
            }
        }
        if (!inDeleteRegion) {
            dropTarget = findDropTarget(realScreenX, realScreenY, coordinates);
        }

        mDragObject.x = coordinates[0];
        mDragObject.y = coordinates[1];

        if (dropTarget != null) {
            if (mLastDropTarget == dropTarget) {
                dropTarget.onDragOver(mDragObject);
            } else {
                if (mLastDropTarget != null) {
                    dropTargetChanging = true;
                    mLastDropTarget.onDragExit(mDragObject, dropTarget);
                }
                if (mTransformStateChanging) {
                    mTransformStateChanging = false;
                    retransformCoordinate(dropTarget);
                }
                dropTarget.onDragEnter(mDragObject);
            }
        } else {
            if (mLastDropTarget != null) {
                dropTargetChanging = true;
                mLastDropTarget.onDragExit(mDragObject, null);
            }
        }
        mLastDropTarget = dropTarget;

        if(mDragObject != null &&  mDragObject.dragInfo instanceof CellLayout){
        	//check screen wrap for the screendrag
        }else{
        	boolean needScrollRight = realScreenX > mScrollView.getWidth() - mScrollZone;
            boolean needScrollLeft = realScreenX < mScrollZone;
            scrollableForScreenY = true;
            if (scrollableForScreenY && !inDeleteRegion && needScrollLeft) {
                if (mScrollState == SCROLL_OUTSIDE_ZONE) {
                    mScrollState = SCROLL_WAITING_IN_ZONE;
                    mScrollRunnable.setDirection(SCROLL_LEFT);
                    mScrollRunnable.beforeRun();
                    mHandler.postDelayed(mScrollRunnable, SCROLL_DELAY);
                }
            } else if (scrollableForScreenY && !inDeleteRegion && needScrollRight) {
                if (mScrollState == SCROLL_OUTSIDE_ZONE) {
                	try {
                		mScrollState = SCROLL_WAITING_IN_ZONE;
                        mScrollRunnable.setDirection(SCROLL_RIGHT);
                        mScrollRunnable.beforeRun();
                        mHandler.postDelayed(mScrollRunnable, SCROLL_DELAY);
					} catch (Exception e) {
						e.printStackTrace();
					}
                }
            } else {
                if (mScrollState == SCROLL_WAITING_IN_ZONE) {
                    mScrollState = SCROLL_OUTSIDE_ZONE;
                    mScrollRunnable.setDirection(SCROLL_RIGHT);
                    mScrollRunnable.cancelRun();
                    mHandler.removeCallbacks(mScrollRunnable);
                } else if (mScrollState == SCROLL_OUTSIDE_ZONE) {
                    mScrollRunnable.cancelRun();
                }
            }
        }
    }

    private boolean drop(float x, float y, int screenX, int screenY, PointF vel) {
    	
        final int[] coordinates = mCoordinatesTemp;
        
        DropTarget dropTarget;
        if (dropTargetChanging || mLastDropTarget == null) {
            dropTarget = findDropTarget((int) x, (int) y, coordinates);
        } else {
            dropTarget = mLastDropTarget;
            inDropTarget((int) x, (int) y, coordinates, dropTarget);
        }

        mDragObject.x = coordinates[0];
        mDragObject.y = coordinates[1];

        mDragObject.mDropScreenX = screenX;
        mDragObject.mDropScreenY = screenY;

        //
        mDragObject.mDragViewScreenX = Integer.MIN_VALUE;
        mDragObject.mDragViewScreenY = Integer.MIN_VALUE;

        //
        mDragObject.mDropVelocityX = vel.x;
        mDragObject.mDropVelocityY = vel.y;

        if (mDragObject.dragView != null && mDragObject.dragView.isInitialized()) {
            //
            mDragObject.dragView.getLocationOnScreen(mCoordinatesTemp2);
            mDragObject.mDragViewScreenX = mCoordinatesTemp2[0] + mDragObject.dragView.getWidth() / 2;
            mDragObject.mDragViewScreenY = mCoordinatesTemp2[1] + mDragObject.dragView.getHeight() / 2;
        }

        if (dropTarget != null) {
            mDragObject.dragComplete = true;
            dropTargetChanging = false;
            dropTarget.onDragExit(mDragObject, dropTarget);
            if (dropTarget.acceptDrop(mDragObject)) {
                dropTarget.onDrop(mDragObject);
                mDragObject.dragSource.onDropCompleted((View) dropTarget, true);
                return true;
            } else {
                mDragObject.dragSource.onDropCompleted((View) dropTarget, false);
                return true;
            }
        }

        mDragObject.dragSource.onDropCompleted((View) dropTarget, false);
        return false;
    }

    private DropTarget findDropTarget(int x, int y, int[] dropCoordinates) {

        final ArrayList<DropTarget> dropTargets = mDropTargets;
        final int count = dropTargets.size();
        List<DropTarget> topDropTargets = new ArrayList<DropTarget>();
        for (DropTarget tt : dropTargets) {
            if (isVisible((View) tt) && (tt instanceof DeleteZone)) {
                topDropTargets.add(tt);
            }
        }

        for (DropTarget dropTarget : topDropTargets) {
            if (inDropTarget(x, y, dropCoordinates, dropTarget)) {
                return dropTarget;
            }
        }

        for (int i = count - 1; i >= 0; i--) {
            DropTarget dropTarget = dropTargets.get(i);
            if (isVisible((View) dropTarget) && inDropTarget(x, y, dropCoordinates, dropTarget)) {
                return dropTarget;
            }
        }

        return null;
    }

    private boolean inDropTarget(int x, int y, int[] dropCoordinates, DropTarget dropTarget) {
        final Rect r = mRectTemp;
        if (dropTarget != null && isVisible((View) dropTarget)) {
            dropTarget.getHitRect(r);
            dropTarget.getLocationOnScreen(dropCoordinates);
            r.offset(dropCoordinates[0] - dropTarget.getLeft(), dropCoordinates[1] - dropTarget.getTop());

            //drag screen, 强制在这个workspace区域好了
            if (r.contains(x, y) || (Workspace.sInEditMode && mDragObject.dragInfo instanceof  WorkspaceCellLayout && dropTarget instanceof Workspace)) {
            	dropCoordinates[0] = x - dropCoordinates[0];
            	dropCoordinates[1] = y - dropCoordinates[1];
            	return true;
            }
        }
        return false;
    }

    private boolean isVisible(View view) {
        Object viewParent = view;
        while (viewParent instanceof View) {
            if (((View) viewParent).getVisibility() != View.VISIBLE) {
                return false;
            }
            viewParent = ((View) viewParent).getParent();
        }
        return true;
    }

    /**
     * Clamp val to be &gt;= min and &lt; max.
     */
    private static int clamp(int val, int min, int max) {
        if (val < min) {
            return min;
        } else if (val >= max) {
            return max - 1;
        } else {
            return val;
        }
    }

    public void removeDragScollers() {
        mDragScrollers.clear();
    }

    public void addDragScoller(DragScroller scroller) {
        mDragScrollers.add(scroller);
    }

    public void setWindowToken(IBinder token) {
        mWindowToken = token;
    }

    /**
     * Remove a previously installed drag listener.
     */
    public void removeDragListeners() {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "All drag listeners are removed");
        }
        mListeners.clear();
    }

    public void removeDragListener(DragListener l) {
        if (l == null) {
            return;
        }
        mListeners.remove(l);
    }

    /**
     * Sets the drag listner which will be notified when a drag starts or ends.
     */
    public void addDragListener(DragListener l) {
        mListeners.add(l);
    }

    public void removeDropTargets() {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "All drop targets are removed");
        }
        mDropTargets.clear();
    }

    /**
     * Add a DropTarget to the list of potential places to receive drop events.
     */
    public void addDropTarget(DropTarget target) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Drop target " + target + " is added");
        }
        mDropTargets.add(target);
    }

    /**
     * Don't send drop events to <em>target</em> any more.
     */
    public void removeDropTarget(DropTarget target) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Drop target " + target + " is removed");
        }
        mDropTargets.remove(target);
    }

    private void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
    }

    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            try {
                mVelocityTracker.recycle();
            } catch (Throwable e) {
                // ignore
            }
            mVelocityTracker = null;
        }
    }

    /**
     * Set which view scrolls for touch events near the edge of the screen.
     */
    public void setScrollView(View v) {
        mScrollView = v;
    }

    /**
     * Specifies the delete region.  We won't scroll on touch events over the delete region.
     *
     * @param region The rectangle in screen coordinates of the delete region.
     */
    public void setDeleteRegion(RectF region) {
        mDeleteRegion = region;
    }

    private class ScrollRunnable implements Runnable {
        private int mDirection;

        ScrollRunnable() {
        }

        public void beforeRun() {
            for (DragScroller dragScroller : mDragScrollers) {
                if (mDirection == SCROLL_LEFT) {
                    dragScroller.beforeScrollLeft();
                } else {
                	XLog.d(TAG, "dragScroller.beforeScrollRight()");
                    dragScroller.beforeScrollRight();
                }
            }
        }

        public void cancelRun() {
        	XLog.d(TAG, "dragScroller.cancelScroll()");
            for (DragScroller dragScroller : mDragScrollers) {
                dragScroller.cancelScroll();
            }
        }

        public void run() {
        	XLog.d(TAG, "dragScroller.scrollRight()");
            for (DragScroller dragScroller : mDragScrollers) {
                if (mDirection == SCROLL_LEFT) {
                    dragScroller.scrollLeft();
                } else {
                    dragScroller.scrollRight();
                }
            }
            mScrollState = SCROLL_OUTSIDE_ZONE;
        }

        void setDirection(int direction) {
            mDirection = direction;
        }
    }

    public boolean isDropTargetChanging() {
        return dropTargetChanging;
    }

    public boolean isDragging() {
        return mDragging;
    }

    public int[] getDraggingStartLocation() {
        return mCoordinatesTemp;
    }

    public View getDraggingSourceView() {
        return mOriginator;
    }

    public DropTarget.DragObject getDragObject() {
        return mDragObject;
    }

    public DropTarget getLastDropTarget() {
        return mLastDropTarget;
    }

    public void setCoordinateTransformEnabled(boolean enable) {
        mTransformStateChanging = !mEnableCoordinateTransform && enable;
        mEnableCoordinateTransform = enable;
    }

    public void setDropAnimatingView(View view) {
        mDropAnimatingView = view;
    }

    private void retransformCoordinate(DropTarget dropTarget) {
        final int[] loc = mCoordinatesTemp;
        dropTarget.getLocationOnScreen(loc);
        final Rect r = mRectTemp;
        dropTarget.getHitRect(r);
        dropTarget.getLocationOnScreen(loc);
        r.offset(loc[0] - dropTarget.getLeft(), loc[1] - dropTarget.getTop());
        int realScreenX = mDragObject.x + loc[0];
        int realScreenY = mDragObject.y + loc[1];
        if (Workspace.sInEditMode && mContext instanceof Launcher && mEnableCoordinateTransform) {
            int centerX = ScreenDimensUtils.getScreenWidth(mContext) / 2;
            int centerY = CellLayout.getSmartTopPadding();
            realScreenX = (int) Utils.scaleFrom(realScreenX, centerX, Workspace.sEditModeScaleRatio);
            realScreenY = (int) Utils.scaleFrom(realScreenY, centerY, Workspace.sEditModeScaleRatio);
        }
        mDragObject.x = realScreenX - loc[0];
        mDragObject.y = realScreenY - loc[1];
    }

    public int[] getLastActionMoveCoordinate(){
        return mLastMoveActionCoordinate;
    }
    
    /**
     * 处理异常拖拽问题：
     * 场景：在滚动内部有滚动条的widget时，手法好的话会触发拖拽逻辑，这个时候消息已经被widget拦截了
     * 所以就无法终止拖拽了
     * mIsMoving标记在拖拽时是否有ACTION_MOVE消息
     */
    public boolean isExceptionDrag(){
    	return mDragging && !mIsMoving;
    }
    
    public void onExceptionDrag(){
    	cancelDrag(true);
    }
}
