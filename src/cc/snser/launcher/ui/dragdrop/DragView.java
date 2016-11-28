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

import android.R.integer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import cc.snser.launcher.IconCache;
import cc.snser.launcher.apps.components.workspace.FloatView;
import cc.snser.launcher.ui.effects.SymmetricalLinearTween;
import cc.snser.launcher.ui.effects.TweenCallback;
import cc.snser.launcher.ui.utils.UiConstants;

public class DragView extends FloatView implements TweenCallback {

    // Number of pixels to add to the dragged item for scaling.  Should be even for pixel alignment.
    private static final int DRAG_SCALE = 18;

    private int mRegistrationX;
    private int mRegistrationY;

    SymmetricalLinearTween mTween;
    private float mScale = 1;
    private float mSnapScale = 1f;
    private float mAnimationScale = 1.0f;
    private int mAnimationAlpha = 255;

    public static final int TYPE_SHADOW_VIEW = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
    public static final int TYPE_DRAG_VIEW = WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL;

    private Rect mDragRegion;
    
    //让当前拖拽的内容以缩放的动画，跳到手指上的参数设置
    private float mDestScale = 1.0f;
    private boolean mSnapToFinger = false;
    private int mRegistrationXShotcut;
    private int mRegistrationYShotcut;
    private int mTopShortCut;
    private int mLeftShortCut;
    private int mMoveDistanceX;
    private int mMoveDistanceY;
    
    //弹回动画设置
    private boolean mHasReboundAnimation;
    private Paint mCurrentPaint;
    private Context mContext;

    /**
     * Construct the drag view.
     * <p>
     * The registration point is the point inside our view that the touch events should
     * be centered upon.
     *
     * @param context A context
     * @param bitmap The view that we're dragging around.  We scale it up when we draw it.
     * @param registrationX The x coordinate of the registration point.
     * @param registrationY The y coordinate of the registration point.
     */
    public DragView(Context context, Bitmap bitmap, int registrationX, int registrationY,
            int left, int top, int width, int height, int type, boolean useDragScale) {
        super(context, bitmap, type);
        mContext = context;
        setDestroyAfterAnimationEnd(true);
        setDismissAfterAnimationEnd(type == TYPE_DRAG_VIEW);

        mTween = new SymmetricalLinearTween(false, 150 /*150 ms duration*/, this);

        mDragRegion = new Rect(0, 0, width, height);

        if (type == TYPE_DRAG_VIEW) {
            mScale = ((float) width + (useDragScale ? DRAG_SCALE : 0)) / width;
            // The point in our scaled bitmap that the touch events are located
            mRegistrationX = registrationX + ((useDragScale ? DRAG_SCALE : 0) / 2);
            mRegistrationY = registrationY + ((useDragScale ? DRAG_SCALE : 0) / 2);
            mCurrentPaint = IconCache.getInstance(context).getTrunclucentPaint();
            setPaint(mCurrentPaint);
            mAnimationAlpha = IconCache.getInstance(context).getTrunclucentPaint().getAlpha();
        } else if (type == TYPE_SHADOW_VIEW) {
            mRegistrationX = 0;
            mRegistrationY = 0;
            Paint p = new Paint();
            p.setAlpha(0x50);
            setPaint(p);
            mAnimationAlpha = 0x50;
        }

        mWidth *= mScale;
        mHeight *= mScale;
        setDrawingFrame(0, 0, mWidth, mHeight);
    }

    @Override
    protected void onCreateLayoutParams(LayoutParams params) {
        params.setTitle("DragView");
    }
    
    public boolean hasReboundAnimation(){
    	return mHasReboundAnimation;
    }
    
    /**
     * 设置是否有回弹动画的标记位，以便稍后再设置动画，便于Hold住DragView
     * @param hasReboundAnimation
     * @return
     */
    public void setReboundAnimation(boolean hasReboundAnimation){
    	mHasReboundAnimation = hasReboundAnimation;
    }

    @Override
    protected int onPredraw(Canvas canvas) {
        if (mType == TYPE_DRAG_VIEW) {
            float scale = mAnimationScale;
            if (scale < 0.999f) { // allow for some float error
                int count = canvas.save();
                canvas.setDrawFilter(UiConstants.ANTI_ALIAS_FILTER);
                canvas.scale(scale, scale, getCenterXOffset(), getCenterYOffset());
                return count;
            }
        }
        return super.onPredraw(canvas);
    }

    @Override
    protected void onPostDraw(Canvas canvas, int count) {
        if (mType == TYPE_DRAG_VIEW && mAnimationScale < 0.999f) {
            canvas.restoreToCount(count);
        }
    }

    public void onTweenValueChanged(float value, float oldValue) {
        if (isAnimating) {
            return;
        }
        
        mPaint.setAlpha((int) (255 - (255 - mAnimationAlpha) * value));
        if(mDestScale == 1.0f){
        	calcuateDrawFrame((mSnapScale + ((mScale - mSnapScale) * value)));
        }else{
        	calcuateDrawFrame(mDestScale + (1 - value) * mDestScale);
        }
        
        if(mSnapToFinger){
            mRegistrationX = mRegistrationXShotcut - (int)(mMoveDistanceX * value);
            mRegistrationY = mRegistrationYShotcut - (int)(mMoveDistanceY * value);
            setLayoutPosition( mLeftShortCut + (int)(mMoveDistanceX * value),
            							mTopShortCut + (int)(mMoveDistanceY * value));
        }
        
        invalidate();
    }
    
    public void restorePaint(){
    	setPaint(mCurrentPaint);
    }

    private void calcuateDrawFrame(float scale) {
        final int left = (int) ((mDragRegion.width() * mScale - mDragRegion.width() * scale) / 2);
        final int right = (int) ((mDragRegion.width() * mScale - mDragRegion.width() * scale) / 2 + mDragRegion.width() * scale);

        final int top = (int) ((mDragRegion.height() * mScale - mDragRegion.height()  * scale) / 2);
        final int bottom = (int) ((mDragRegion.height() * mScale - mDragRegion.height() * scale) / 2 + mDragRegion.height() * scale);
        setDrawingFrame(left, top, right, bottom);
    }

    public void onTweenStarted() {
    }

    public void onTweenCancelled() {
    }

    public void onTweenFinished() {
    }

    public void onDropAnimationStart() {
        setPaint(IconCache.getInstance(mContext).getSolidPaint());
    }

    @Override
    public void show(IBinder windowToken, int x, int y) {
        super.show(windowToken, x - mRegistrationX, y - mRegistrationY);
        if (mType == TYPE_DRAG_VIEW) {
            post(new Runnable() {
		                public void run() {
		                    mTween.start(true);
		                }
		            });
        }
    }
    
    public void show(IBinder windowToken, int x, int y,int animaAlpha, float scale) {
    	final int top = y - mRegistrationY;
    	final int left = x - mRegistrationX;
    	
    	mDestScale = scale;
    	mSnapToFinger = true;
    	mAnimationAlpha = animaAlpha;
    	
    	mTopShortCut = top;
    	mLeftShortCut = left;
    	mRegistrationXShotcut = mRegistrationX;
    	mRegistrationYShotcut = mRegistrationY;

    	mMoveDistanceX = mRegistrationX - mWidth / 2; 
    	mMoveDistanceY = mRegistrationY - mHeight / 2;
    	
        super.show(windowToken, left, top);
        if (mType == TYPE_DRAG_VIEW) {
            post(new Runnable() {
		                public void run() {
		                	mTween.start(true);
		                }
		            });
        }
    }
    
    public boolean startReboundAnimation(int x,int y,int width,int height,FloatView.AnimationCallback callback){
    	mHasReboundAnimation = true;
    	return animateTo(new Rect(x, y, width, height), callback);
    }
    
    /**
     * Move the window containing this view.
     *
     * @param touchX the x coordinate the user touched in screen coordinates
     * @param touchY the y coordinate the user touched in screen coordinates
     */
    public void move(int touchX, int touchY) {
        moveTo(touchX - mRegistrationX, touchY - mRegistrationY);
    }

    public int getDragRegionLeft() {
        return mDragRegion.left;
    }

    public int getDragRegionRight() {
        return mDragRegion.right;
    }

    public int getDragRegionWidth() {
        return mDragRegion.width();
    }

    public int getDragRegionHeight() {
        return mDragRegion.height();
    }

    public void snap() {
        snap(1f, true);
    }

    public void snap(float scale, boolean solid) {
        mSnapScale = scale;
        if (solid) {
            setPaint(IconCache.getInstance(mContext).getSolidPaint());
        }
        mTween.start(false);
    }

    public void unsnap() {
        setPaint(IconCache.getInstance(mContext).getTrunclucentPaint());
        mTween.start(true);
    }

    public void insetToIconFrame(int top, int drawablesize) {
        final float scaledSize = drawablesize * mScale;
        final float centerX = getCenterXOffset();
        final int scaledTop = (int) (top * mScale);

        if (scaledSize > dst.width() || scaledTop >= dst.bottom) {
            return;
        }

        setDrawingFrame((int) (centerX - scaledSize * 0.5f), scaledTop, (int) (centerX + scaledSize * 0.5f), (int) (scaledTop + scaledSize));

        final float originalCenterX = mDragRegion.exactCenterX();
        mDragRegion.left = (int) (originalCenterX - drawablesize * 0.5f);
        mDragRegion.right = (int) (originalCenterX + drawablesize * 0.5f);
        mDragRegion.top = top;
        mDragRegion.bottom = mDragRegion.top + drawablesize;
        clipRect(mDragRegion);
    }

}

