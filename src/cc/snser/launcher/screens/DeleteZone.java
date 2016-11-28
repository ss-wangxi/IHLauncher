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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.apps.model.workspace.LauncherAppWidgetInfo;
import cc.snser.launcher.apps.model.workspace.LauncherWidgetViewInfo;
import cc.snser.launcher.apps.utils.AppUtils;
import cc.snser.launcher.iphone.model.LauncherModelIphone;
import cc.snser.launcher.model.DbManager;
import cc.snser.launcher.ui.components.DeleteView;
import cc.snser.launcher.ui.components.GrainView;
import cc.snser.launcher.ui.components.HiddenView;
import cc.snser.launcher.ui.components.LineView;
import cc.snser.launcher.ui.components.OperationView;
import cc.snser.launcher.ui.dragdrop.DragController;
import cc.snser.launcher.ui.dragdrop.DragSource;
import cc.snser.launcher.ui.dragdrop.DropTarget;
import cc.snser.launcher.widget.Widget;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

public class DeleteZone extends FrameLayout implements DropTarget, DragController.DragListener {
    private static final String TAG = "Launcher.DeleteZone";

    private static final int ANIMATION_DURATION = 200;
    public static final int ANIMATION_COUNT = 100;
    public static final int ANIMATION_DISSAPPEAR = 400;

    public static final int DELETE_DELETE = 0;
    public static final int DELETE_GRAIN = -1;
    public static final int DELETE_HIDDEN = -2;

    private final int[] mLocation = new int[2];

    private Launcher mLauncher;

    private boolean mTrashMode;

    private AnimationSet mInAnimation;

    private AnimationSet mOutAnimation;

    private AnimationSet mDisappearAnimation;

    private DragController mDragController;

    private final RectF mRegion = new RectF();

    private final Paint mTrashPaint = new Paint();

    private TextView mTrash;
    private OperationView mOperationView;
    private LineView mLineView;
    private LinearLayout mLayout;
    private Context mContext;

    private boolean mIsImmediately = true;
    
    public DeleteZone(Context context, AttributeSet attrs) {
        super(context, attrs);

        try {
            mTrashPaint.setColorFilter(new PorterDuffColorFilter(context.getResources().getColor(
                    R.color.delete_color_filter), PorterDuff.Mode.SRC_ATOP));
        } catch (Throwable e) {
            // ignore
        }
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTrash = (TextView) this.findViewById(R.id.delete_zone_trash);
        mLineView = (LineView) this.findViewById(R.id.delete_zone_lineview);
        mLayout = (LinearLayout) this.findViewById(R.id.delete_zone_layout);
    }

    public boolean acceptDrop(DragObject dragObject) {
        return true;
    }

    public void onDrop(DragObject dragObject) {

        if (dragObject != null && dragObject.dragInfo instanceof Deletable) {
            if (!((Deletable) dragObject.dragInfo).isDeletable(getContext())) {
                return;
            }
        }
        mLineView.startAnimation(mDisappearAnimation);
        mIsImmediately = false;
        completeDrop(dragObject.dragSource, dragObject.dragInfo);
    }

    private void completeDrop(final DragSource source, Object dragInfo) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "onDrop from source: " + source + " to: " + this.getClass().getName()
                    + " with dragInfo: " + dragInfo);
        }

        if (dragInfo instanceof Deletable) {
            boolean done = ((Deletable) dragInfo).onDelete(getContext());
            if (done) {
                return;
            }
        }

        if (dragInfo instanceof Widget) {
            return;
        }

        final HomeItemInfo item = (HomeItemInfo) dragInfo;

        handleRemoveHomeItem(source, item);
    }

    private void handleRemoveHomeItem(final DragSource source, final HomeItemInfo item) {
        if (item instanceof HomeDesktopItemInfo) {
            final HomeDesktopItemInfo homeDesktopItemInfo = (HomeDesktopItemInfo) item;
            if (!homeDesktopItemInfo.isShortcut()) {
                if (!homeDesktopItemInfo.isSystem()) {
                    AppUtils.uninstallApplicationInfo(getContext(), homeDesktopItemInfo, Launcher.REQUEST_UNINSTALL_PACKAGE);
                } 
                return;
            }

            mLauncher.removeItem((HomeDesktopItemInfo) item, true);
        } else if (item instanceof LauncherWidgetViewInfo) {
            final LauncherWidgetViewInfo launcherWidgetViewInfo = (LauncherWidgetViewInfo) item;

            mLauncher.removeWidgetView(launcherWidgetViewInfo);
        }else if (item instanceof LauncherAppWidgetInfo) {
            final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) item;

            mLauncher.removeAppWidget(launcherAppWidgetInfo);
        }

        if (item instanceof HomeDesktopItemInfo) {
            ((LauncherModelIphone) mLauncher.getModel()).removeItem((HomeDesktopItemInfo) item, false, false);
        }
        DbManager.deleteItemFromDatabase(mLauncher, item);
    }

    public void onDragEnter(DragObject dragObject) {
        if (dragObject != null && dragObject.dragInfo instanceof Deletable) {
            if (!((Deletable) dragObject.dragInfo).isDeletable(getContext())) {
                return;
            }
        }
        if (mOperationView != null) {
            mOperationView.setAnimationStart(true);
        }
        if (mLineView.getVisibility() == View.VISIBLE) {
            mLineView.setAnimationStart(true);
        }
    }

    public void onDragOver(DragObject dragObject) {
    }

    public void onDragExit(DragObject dragObject, DropTarget dropTarget) {
        if (dragObject != null && dragObject.dragInfo instanceof Deletable) {
            if (!((Deletable) dragObject.dragInfo).isDeletable(getContext())) {
                return;
            }
        }
        if (dropTarget != this) {
            mIsImmediately = true;
            if (mOperationView != null) {
                mOperationView.setAnimationStart(false);
            }
            if (mLineView.getVisibility() == View.VISIBLE) {
                mLineView.setAnimationStart(false);
            }
        }

        if (!dragObject.dragComplete) {
            dragObject.dragView.restorePaint();
        }
    }

    public void onDragStart(DragSource source, Object info, int dragAction) {
        boolean dragViewNotNull = false;
        
        if (info != null) {
        	dragViewNotNull = true;
        }
        if (mLayout.getChildAt(0) != null && mLayout.getChildAt(0) instanceof OperationView) {
            mLayout.removeViewAt(0);
            mOperationView = null;
        }

        if (info instanceof Deletable) {
        	
        	if(((Deletable) info).getLabel(getContext()).equals(getContext().getString(R.string.global_hide)) /*|| deleteable*/)
            {//目前只能隐藏的app, 代表是系统app, 不再显示隐藏, 而是直接操作
              mLineView.setVisibility(View.GONE);
              mTrash.setText(null);
              return;
            }
        	
        	mLineView.startAnimation(new AlphaAnimation(0, 1));
            mLineView.setVisibility(View.VISIBLE);
            //mLineView.setCount(ANIMATION_COUNT);
            
			//双层桌面时，第一层的所有app都当快捷方式处理，直接删除
            int select = DELETE_DELETE;            
            mTrash.setText(((Deletable) info).getLabel(getContext()));
            select = ((Deletable) info).getDeleteZoneIcon(getContext());
            
            if (select == DELETE_HIDDEN) {
                mOperationView = new HiddenView(mContext);
            } else if (select == DELETE_GRAIN) {
                mOperationView = new GrainView(mContext);
            } else if(select == DELETE_DELETE) {
                mOperationView = new DeleteView(mContext);
            }else{
            	mOperationView = null;
            }
            if(mOperationView != null){
            	mLayout.addView((View) mOperationView, 0, new LayoutParams((int) mOperationView.getViewWidth(), android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
                mOperationView.setCount(ANIMATION_COUNT);	
            }
            show(dragViewNotNull);
        }else {
            mLineView.setVisibility(View.GONE);
            mTrash.setText(null);
        }
    }

    public void show(boolean dragViewNotNull) {
        if (dragViewNotNull) {
            if (!mTrashMode) {
                mTrashMode = true;
                createAnimations();
                initRegion();
                startAnimation(mInAnimation);
                if (mLauncher != null) {
                    mLauncher.requestFullScreen(true);
                }
                getParent().bringChildToFront(this);
                setVisibility(VISIBLE);
            } else {
                initRegion();
                if (getVisibility() != VISIBLE) {
                    setVisibility(VISIBLE);
                }
            }
        }
    }

    private void initRegion() {
        final int[] location = mLocation;
        getLocationOnScreen(location);
        mRegion.set(location[0], location[1], location[0] + getRight() - getLeft(), location[1]
                + getBottom() - getTop());
        mDragController.setDeleteRegion(mRegion);
    }

    public void hide() {
        setVisibility(GONE);
        setBackgroundDrawable(null);
        mDragController.setDeleteRegion(null);
    }

    public void onDragEnd(boolean immediately) {
        if (mTrashMode) {
            mTrashMode = false;
            mDragController.setDeleteRegion(null);
            if (immediately) {
                if (mLauncher != null && !(mLauncher.getWorkspace().isInEditMode())) {
                    mLauncher.requestFullScreen(false);
                }
            } else {
                if (!mIsImmediately) {
                    mOutAnimation.setStartOffset(ANIMATION_DISSAPPEAR);
                } else {
                    mOutAnimation.setStartOffset(0);
                }
                startAnimation(mOutAnimation);
            }
            hide();
        }
    }

    private void createAnimations() {
        if (mInAnimation == null) {
            mInAnimation = new FastAnimationSet();
            final AnimationSet animationSet = mInAnimation;
            animationSet.setInterpolator(new AccelerateInterpolator());
            animationSet.addAnimation(new AlphaAnimation(0.0f, 1.0f));
            animationSet.addAnimation(new TranslateAnimation(Animation.ABSOLUTE, 0.0f,
                    Animation.ABSOLUTE, 0.0f, Animation.RELATIVE_TO_SELF,
                    -1.0f, Animation.RELATIVE_TO_SELF, 0.0f));
            animationSet.setDuration(ANIMATION_DURATION);
        }
        if (mOutAnimation == null) {
            mOutAnimation = new FastAnimationSet();
            final AnimationSet animationSet = mOutAnimation;
            animationSet.setInterpolator(new AccelerateInterpolator());
            animationSet.addAnimation(new AlphaAnimation(1.0f, 0.0f));
            animationSet.addAnimation(new FastTranslateAnimation(Animation.ABSOLUTE, 0.0f,
                    Animation.ABSOLUTE, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, -1.0f));
            animationSet.setDuration(ANIMATION_DURATION);
            animationSet.setAnimationListener(new AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    //if (mLauncher != null && !(mLauncher.getWorkspace().isInEditMode())) {
                	//去掉编辑模式的条件，不知道会有什么问题，看看了
                	if (mLauncher != null) {
                        if (mOperationView != null) {
                            mOperationView.setAnimationStart(false);
                            mLayout.removeViewAt(0);
                            mOperationView = null;
                        }
                        if (mLineView.getVisibility() == View.VISIBLE) {
                            mLineView.setAnimationStart(false);
                        }
                        
                        if(!mLauncher.getWorkspace().isInEditMode()){
                        	mLauncher.requestFullScreen(false);
                        }
                        
                        setVisibility(View.GONE);
                    }
                }

                public void onAnimationRepeat(Animation animation) {
                }
            });
        }

        if (mDisappearAnimation == null) {
            AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
            alphaAnimation.setFillAfter(true);
            ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 1.5f, 1.0f, 1.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            mDisappearAnimation = new AnimationSet(true);
            mDisappearAnimation.addAnimation(alphaAnimation);
            mDisappearAnimation.addAnimation(scaleAnimation);
            mDisappearAnimation.setDuration(ANIMATION_DISSAPPEAR);
        }
    }

    public void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

    public void setDragController(DragController dragController) {
        mDragController = dragController;
    }

    private static class FastTranslateAnimation extends TranslateAnimation {
        public FastTranslateAnimation(int fromXType, float fromXValue, int toXType, float toXValue,
                int fromYType, float fromYValue, int toYType, float toYValue) {
            super(fromXType, fromXValue, toXType, toXValue, fromYType, fromYValue, toYType,
                    toYValue);
        }

        @Override
        public boolean willChangeTransformationMatrix() {
            return true;
        }

        @Override
        public boolean willChangeBounds() {
            return false;
        }
    }

    private static class FastAnimationSet extends AnimationSet {
        FastAnimationSet() {
            super(false);
        }

        @Override
        public boolean willChangeTransformationMatrix() {
            return true;
        }

        @Override
        public boolean willChangeBounds() {
            return false;
        }
    }

    public static interface Deletable {
        public boolean isDeletable(Context context);

        public String getLabel(Context context);

        public int getDeleteZoneIcon(Context context);

        public boolean onDelete(Context context);
    }
    
    
}