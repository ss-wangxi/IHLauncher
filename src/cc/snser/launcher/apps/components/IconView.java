package cc.snser.launcher.apps.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import cc.snser.launcher.IconCache;
import cc.snser.launcher.RuntimeConfig;
import cc.snser.launcher.Utils;
import cc.snser.launcher.reflect.FieldUtils;
import cc.snser.launcher.ui.utils.IconMetrics;
import cc.snser.launcher.ui.utils.Utilities;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;
import cc.snser.launcher.util.CellImagePool;
import cc.snser.launcher.util.ResourceUtils;
import cc.snser.launcher.util.CellImagePool.Image;

import com.btime.launcher.R;
import com.shouxinzm.launcher.ui.view.FastBitmapDrawable;
import com.shouxinzm.launcher.ui.view.SlipSwitchBitmapDrawable;
import com.shouxinzm.launcher.util.DeviceUtils;

/**
 * 桌面各种icon的基类，负责绘制阴影、绘制缩放、 左上角可点击删除小叉、右上角tip等等.
 *
 */
public class IconView extends BaseIconView implements CellImagePool.Callback {

    /**
     * 为IconView的点击动画设置
     */
    public static boolean isShowIconPressAnimation = true;

    /**
     * 为IconView的显示Tip设置
     */
    public static boolean isShowIconTip = true;

    private int mFlags = 0;

    private static final int DRAW_NOTIFICATION_FLAG = 1;

    private static final int DRAW_TIP_IMAGE_FLAG = 4;

    private static final int EXTRA_DRAW_FLAGS_MASK = 7; // this must equals the sum of all draw flags

    private static final int DRAW_SHOW_IMAGE_ONLY = 8;

    protected static final int SHOW_FOLDER_IMAGE_FLAG = 1 << 16;

    protected static final int FOLDER_IMAGE_FADE_ANIMATION_FLAG = 2 << 16;

    protected static final int EXTRA_FOLDER_FLAGS_MASK = 3 << 16;

    private static final int EXTRA_HIDE_TEXT = EXTRA_FOLDER_FLAGS_MASK | DRAW_SHOW_IMAGE_ONLY;

	private static final int ICON_PRESSED_FLAG = 4 << 8;

    private Object notification;

    protected Rect mRect = new Rect();

    private Drawable notificationBg;

    private Drawable mTipDrawable;

    private IconTip mIconTip;

    private Bitmap maskFolderImage;

    private float folderImageScale = 1.0f;

    private float folderImageAlpha = 1.0f;

    private int mTextWidth = 220;

    protected int drawLeft, drawTop, drawRight, drawBottom;

    private Transformation mTrans;

    private Animation folderBgScaleAnimation;

    private Paint mFolderPaint;

    private Paint mTextPaint;

    private IconCache mIconCache;

    private static Canvas sCanvas = new Canvas();

    private Image mBitmapNormalImage;
    private Bitmap mBitmapNormal;
    private boolean mBitmapNormalExpired;

    private boolean blockUpAnimation = false;
    private boolean isEnableIconPressAnimation = false;
    private boolean mTouchEnabled = true;

    public IconView(Context context) {
        super(context);
        initIconView(context);
    }

    public IconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initIconView(context);
    }

    public IconView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initIconView(context);
    }

    protected boolean enableCellImagePool() {
        return false;
    }

    protected boolean enableIconPressAnimation() {
        return isEnableIconPressAnimation;
    }

    public void setEnableIconPressAnimation(boolean value) {
        isEnableIconPressAnimation = value;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        mBitmapNormalExpired = true;
    }

    @Override
    public void invalidate(Rect rect) {
        super.invalidate(rect);
        mBitmapNormalExpired = true;
    }

    @Override
    public void invalidate(int l, int t, int r, int b) {
        super.invalidate(l, t, r, b);
        mBitmapNormalExpired = true;
    }

    private final void initIconView(Context context) { // some value need to be modified
        mIconCache = IconCache.getInstance(context);
        mIconMetrics = getIconMetrics(context);

        if(Utils.isSumsungGtN8000()){
        	setTextSize(TypedValue.COMPLEX_UNIT_PX, 15);	
        }else {
        	setTextSize(TypedValue.COMPLEX_UNIT_PX, WorkspaceIconUtils.getWorkspaceIconTextSizeInPx(getContext()));	
		}
        
        setTextColor(WorkspaceIconUtils.getWorkspaceIconTextColor(getContext()));

        Typeface t = WorkspaceIconUtils.getWorkspaceIconTypeface(getContext());
        if (t != null) {
            setTypeface(t, t.getStyle());
        }

        // 4.0 mi one lay on software for text shadow
        if (DeviceUtils.isMiOne() && DeviceUtils.isIceCreamSandwich()) {
            Utils.invokeSetLayerTypeMethod(this);
        }

        int line = WorkspaceIconUtils.getIconTextMaxLine(getContext());
        initTextSize(line);
    }

    protected IconMetrics getIconMetrics(Context context) {
        return IconMetrics.getInstance(context);
    }

    // 双层桌面的抽屉层图标文字设置为两行，需要在外部设置
    public void initTextSize(int line) {
//        setMaxLines(line);
//        if (line == 1) {
//            setSingleLine(true);
//        }
        setSingleLine(line == 1);
        mTextWidth = Utilities.dip2px(getContext(), mTextWidth);
        this.setTextWidth(mTextWidth);
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);

        if (WorkspaceIconUtils.needshadowRadius()) {
            setShadowLayer(8, 0, Utils.isLDPI() ? 1 : 2, 0x66000000);
        } else {
            setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        }
    }

    @Override
    public void attach(Image img) {
        // do nothing
    }

    @Override
    public void detach(Image img) {
        if (mBitmapNormalImage == img) {
            mBitmapNormal = null;
            mBitmapNormalImage = null;
        }
    }

    private int getRealWidth() {
        int ret = this.getMeasuredWidth();
        if (this.getLayoutParams() instanceof MarginLayoutParams) {
            ret += ((MarginLayoutParams) this.getLayoutParams()).leftMargin;
            ret += ((MarginLayoutParams) this.getLayoutParams()).rightMargin;
        }
        return ret;
    }

    private int getRealHeight() {
        int ret = this.getMeasuredHeight();
        if (this.getLayoutParams() instanceof MarginLayoutParams) {
            ret += ((MarginLayoutParams) this.getLayoutParams()).topMargin;
            ret += ((MarginLayoutParams) this.getLayoutParams()).bottomMargin;
        }
        return ret;
    }

    @Override
    public void buildDrawingCache(boolean autoScale) {
        if (enableCellImagePool() && CellImagePool.isEnabled(getRealWidth(), getRealHeight())) {
            // do nothing
        } else {
            super.buildDrawingCache(autoScale);
        }
    }

    @Override
    public void destroyDrawingCache() {
        if (mBitmapNormalImage != null) {
            CellImagePool.release(mBitmapNormalImage);

            mBitmapNormal = null;
            mBitmapNormalImage = null;
        }
        super.destroyDrawingCache();
    }

    @SuppressLint("WrongCall")
    @Override
    public Bitmap getDrawingCache(boolean autoScale) {
        if (enableCellImagePool() && CellImagePool.isEnabled(getRealWidth(), getRealHeight())) {
            if (mBitmapNormal == null || mBitmapNormalExpired) {
                mBitmapNormalExpired = false;

                if (mBitmapNormal == null) {
                    mBitmapNormalImage = CellImagePool.obtain(this);
                    if (mBitmapNormalImage == null) {
                        return super.getDrawingCache(autoScale);
                    }
                    mBitmapNormal = mBitmapNormalImage.bitmap;
                }

                mBitmapNormal.eraseColor(0x00000000);

                sCanvas.setBitmap(mBitmapNormal);
                sCanvas.translate(-getScrollX(), -getScrollY());
                onDraw(sCanvas);
                sCanvas.translate(getScrollX(), getScrollY());
            }
            return mBitmapNormal;
        } else {
            return super.getDrawingCache(autoScale);
        }
    }

    public final void setIcon(Bitmap icon) {
        setIcon(new FastBitmapDrawable(icon));
    }

    public final void setIcon(Bitmap icon, int bgResId, int maskResId, boolean slipTransition) {
        Bitmap oldBitmap = null;
        Drawable oldIcon = getIcon();
        if (oldIcon instanceof FastBitmapDrawable) {
            oldBitmap = ((FastBitmapDrawable) oldIcon).getBitmap();
        } else if (oldIcon instanceof SlipSwitchBitmapDrawable ) {
            Bitmap bitmap = ((SlipSwitchBitmapDrawable) oldIcon).getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                oldBitmap = Bitmap.createBitmap(bitmap);
            }
            ((SlipSwitchBitmapDrawable) oldIcon).destroy();
        }

        Bitmap maskBitmap = WorkspaceIconUtils.createIconBitmap(
                new BitmapDrawable(getContext().getResources(),
                        Bitmap.createBitmap(icon.getWidth(), icon.getHeight(),
                                Bitmap.Config.ARGB_8888)),
                getContext(), null, null,
                getContext().getResources().getDrawable(maskResId), true);

        Bitmap bgBitmap = WorkspaceIconUtils.createIconBitmap(
                new BitmapDrawable(getContext().getResources(),
                        Bitmap.createBitmap(icon.getWidth(), icon.getHeight(),
                                Bitmap.Config.ARGB_8888)),
                getContext(), null, null,
                getContext().getResources().getDrawable(bgResId), true);

        Bitmap parsedIcon = WorkspaceIconUtils.createIconBitmap(
                new BitmapDrawable(getContext().getResources(), icon),
                getContext(), null, null,
                null, true);

        SlipSwitchBitmapDrawable drawable = new SlipSwitchBitmapDrawable(parsedIcon,
                slipTransition ? oldBitmap : null, bgBitmap, maskBitmap, 200);

        drawable.setHostView(this);
        setIcon(drawable);
        drawable.startTransition();
    }

    @Override
    public final void setIcon(Drawable icon) {
        super.setIcon(icon);
    }

    public final void refreshAppIconNotification(Object notification) {
        if (Utils.equals(notification, this.notification)) {
            return;
        }
        this.notification = notification;
        if (isValid(notification)) {
            if (notificationBg == null) {
                notificationBg = getResources().getDrawable(R.drawable.notification_text_bg);
            }

            if (mTextPaint == null) {
                mTextPaint = new Paint();
                mTextPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.workspace_notification_text_size));
                mTextPaint.setColor(Color.WHITE);
                mTextPaint.setAntiAlias(true);
                mTextPaint.setFakeBoldText(true);
                mTextPaint.setTextAlign(Align.CENTER);
            }

            mFlags |= DRAW_NOTIFICATION_FLAG;
        } else {
            notificationBg = null;
            mTextPaint = null;
            mFlags &= ~DRAW_NOTIFICATION_FLAG;
        }

        // 桌面滑动时候暂时不做刷新
        if (RuntimeConfig.sLauncherInScrolling || RuntimeConfig.sLauncherInTouching) {
            postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (RuntimeConfig.sLauncherInScrolling || RuntimeConfig.sLauncherInTouching) {
                        IconView.this.postDelayed(this, 200);
                        return;
                    }

                    invalidate();
                }
            }, 200);
            return;
        }

        invalidate();
    }

    private static boolean isValid(Object notification) {
        if (notification == null) {
            return false;
        }

        if (notification instanceof Integer) {
            return ((Integer) notification) > 0;
        } else if (notification instanceof String) {
            return ((String) notification).length() > 0;
        }

        return false;
    }

    public void setShowNotification(boolean show) {
        if (!isValid(notification)) {
            return;
        }
        if (((mFlags & DRAW_NOTIFICATION_FLAG) != 0) ^ show) {
            if (show) {
                mFlags |= DRAW_NOTIFICATION_FLAG;
            } else {
                mFlags &= ~DRAW_NOTIFICATION_FLAG;
            }
            invalidate();
        }
    }

    public void showTipImage(IconTip iconTip, boolean invalidate) {
        if (iconTip != null && mIconTip != null && iconTip.getPriority() < mIconTip.getPriority()) {
            return;
        }

        boolean changed = mIconTip != iconTip;
        mIconTip = iconTip;
        mTipDrawable = mIconTip == null ? null : mIconTip.getDrawable(getContext());

        if ((mFlags & DRAW_TIP_IMAGE_FLAG) == 0) {
            mFlags |= DRAW_TIP_IMAGE_FLAG;
            if (invalidate) {
                invalidate();
            }
            RuntimeConfig.sDirtyDrawingCacheByTipUpdated = true;
        } else if (changed) {
            if (invalidate) {
                invalidate();
            }
            RuntimeConfig.sDirtyDrawingCacheByTipUpdated = true;
        }
    }

    public final void showTipImage(IconTip iconTip) {
        showTipImage(iconTip, true);
    }

    public void hideTipImage(boolean invalidate) {
        mIconTip = null;
        mTipDrawable = null;

        if ((mFlags & DRAW_TIP_IMAGE_FLAG) != 0) {
            mFlags &= ~DRAW_TIP_IMAGE_FLAG;
            if (invalidate) {
                invalidate();
            }
            RuntimeConfig.sDirtyDrawingCacheByTipUpdated = true;
        }
    }

    public final void hideTipImage() {
        hideTipImage(true);
    }

    public void setShowImageOnly(boolean isHideText) {
        if (isHideText) {
            mFlags |= DRAW_SHOW_IMAGE_ONLY;
        } else {
            mFlags &= ~DRAW_SHOW_IMAGE_ONLY;
        }
    }

    public final void setTouchEnabled(boolean enabled) {
        mTouchEnabled = enabled;
    }

    @Override
    public void onDraw(Canvas canvas) {
        calculateDrawPositon();

        boolean folderAnimMore = true;
        if ((mFlags & EXTRA_FOLDER_FLAGS_MASK) != 0) {
            folderAnimMore = drawMaskFolderImage(canvas);
        }

        if ((mFlags & DRAW_SHOW_IMAGE_ONLY) != 0 && !((mFlags & EXTRA_FOLDER_FLAGS_MASK) != 0 && isFolder())) {
            drawIconOnly(canvas);
        }

        if ((mFlags & EXTRA_HIDE_TEXT) != 0) {
            afterDrawMaskFolder(canvas);
        } else {
            /*if (playTextAlpha) {
                playTextAlpha = textAlpha.getTransformation(SystemClock.uptimeMillis(), mTrans);
                if (playTextAlpha) {
                    canvas.saveLayerAlpha(mScrollX, drawBottom + getCompoundDrawablePadding(), mScrollX + getWidth(), mScrollY + getHeight(), (int) (mTrans.getAlpha() * 255), Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
                }
            }*/
            super.onDraw(canvas);
            /*if (playTextAlpha) {
                canvas.restore();
                postInvalidate(mScrollX, drawBottom + getCompoundDrawablePadding(), mScrollX + getWidth(), mScrollY + getHeight());
            }*/
            beforeDrawTips(canvas);
        }

        if (!folderAnimMore) {
            mFlags &= ~FOLDER_IMAGE_FADE_ANIMATION_FLAG;
        }

        if ((mFlags & EXTRA_DRAW_FLAGS_MASK) != 0) {
            if (isShowIconTip && (mFlags & DRAW_TIP_IMAGE_FLAG) == DRAW_TIP_IMAGE_FLAG) {
                drawTipImage(canvas);
            } else if ((mFlags & DRAW_NOTIFICATION_FLAG) == DRAW_NOTIFICATION_FLAG) {
                drawNotification(canvas);
            }
        }

    }

    /** draw something after draw mask folder */
    protected void afterDrawMaskFolder(Canvas canvas) {
    }

    /** draw something after super onDraw finished and before draw tips at corners */
    protected void beforeDrawTips(Canvas canvas) {
    }

    protected final void calculateDrawPositon() {
        final int topWidth = mIconMetrics.iconWidth;
        final int topHeight = mIconMetrics.iconHeight;
        drawTop = getScrollY() + getPaddingTop();
        final int leftOffset = (getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight() - topWidth) / 2 + getCompoundPaddingLeft();
        drawLeft = leftOffset + getScrollX();
        drawBottom = drawTop + topHeight;
        drawRight = drawLeft + topWidth;
    }

    private final void drawTipImage(Canvas canvas) {
        if (mIconTip != null && mTipDrawable != null) {
            int width = mTipDrawable.getIntrinsicWidth();
            int height = mTipDrawable.getIntrinsicHeight();
            int right = drawRight + mIconTip.getOffsetX();
            int top = drawTop + mIconTip.getOffsetY();

            if (right > getScrollX() + getWidth()) {
                right = getScrollX() + getWidth();
            }
            if (top < getScrollY()) {
                top = getScrollY();
            }

            if (mRect == null) {
                mRect = new Rect();
            }

            mRect.set(mTipDrawable.getBounds());
            mTipDrawable.setBounds(right - width, top, right, top + height);
            mTipDrawable.draw(canvas);
            mTipDrawable.setBounds(mRect);
        }
    }

    private final void drawNotification(Canvas canvas) {
        if (!isValid(notification)) {
            return;
        }

        String text = String.valueOf(notification);

        if (mRect == null) {
            mRect = new Rect();
        }

        mTextPaint.getTextBounds(text, 0, text.length(), mRect);

        final int textTop = mRect.top;
        final int bgHeight = Math.max(ResourceUtils.getMinNotificationTextHeight(this.getContext()), mRect.bottom - mRect.top);
        final float textMargin = (bgHeight - mRect.bottom + mRect.top) / 2f;
        final int bgWidth = (int) Math.max(ResourceUtils.getMinNotificationTextWidth(this.getContext()), mRect.right - mRect.left + textMargin * 2);

      //相对于图标顶点来说的缩进比例是一致的
        float offsetRatio = Math.min(
                Math.min(bgWidth / 2f, getScrollX() + getWidth() - drawRight) / bgWidth,
                Math.min(bgHeight / 2f, drawTop - getScrollY()) / bgHeight);
        final int bgLeft = (int) (drawRight - bgWidth * (1 - offsetRatio));
        final int bgTop = (int) (drawTop - bgHeight * offsetRatio);

        mRect.set(notificationBg.getBounds());
        notificationBg.setBounds(bgLeft, bgTop, bgLeft + bgWidth, bgTop + bgHeight);
        notificationBg.draw(canvas);
        notificationBg.setBounds(mRect);

        final float x = bgLeft + bgWidth / 2;
        final float y = bgTop + textMargin - textTop - 1; //XXX: need to optimize
        canvas.drawText(text, x, y, mTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mTouchEnabled) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (isShowIconPressAnimation) {
                    startIconPressAnimation(true);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_UP:
                if (isShowIconPressAnimation) {
                    startIconPressAnimation(false);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                blockUpAnimation = false;
                if (getAnimation() != null) {
                    clearAnimation();
                    // 动画是 fill after，因此需要再刷一次界面
                    invalidateAnimation();
                }
                break;
        }
        return super.onTouchEvent(event);
    }


    private void startIconPressAnimation(boolean pressed) {
        if (!enableIconPressAnimation()) {
            return;
        }
        if (!pressed) {
            if ((mFlags & ICON_PRESSED_FLAG) == 0 || blockUpAnimation) {
                blockUpAnimation = false;
                return;
            }
        }
        if (pressed) {
            mFlags |= ICON_PRESSED_FLAG;
        } else {
            mFlags &= ~ICON_PRESSED_FLAG;
        }
        clearAnimation();
        //startAnimation(IconPressAnimation.obtain(pressed));
        setAnimation(IconPressAnimation.obtain(pressed));
        invalidateAnimation();
    }

    private void invalidateAnimation() {
    	final ViewParent paret = getParent();
    	if (paret != null && paret instanceof View) {
    		((View) paret).invalidate(getLeft(), getTop(), getRight(), getBottom());
    	}
    }

    @Override
    public boolean performLongClick() {
        clearAnimation();
        blockUpAnimation = true;
        return super.performLongClick();
    }

    public void showMaskFolder(boolean show) {
        //XLog.e("TAG", show ? "show" : "hide");

        if (maskFolderImage == null) {
            maskFolderImage = mIconCache.getFolderIconBg();
        }

        if (mFolderPaint == null) {
            mFolderPaint = new Paint();
            mFolderPaint.setAntiAlias(true);
            mFolderPaint.setFilterBitmap(true);
        }

        if (mTrans == null) {
            mTrans = new Transformation();
        }

        folderBgScaleAnimation = FolderScaleAnimation.obtain(getContext(), this, show);

        if (show) {
            mFlags |= SHOW_FOLDER_IMAGE_FLAG;
            mFlags &= ~FOLDER_IMAGE_FADE_ANIMATION_FLAG;
        } else {
            mFlags &= ~SHOW_FOLDER_IMAGE_FLAG;
            mFlags |= FOLDER_IMAGE_FADE_ANIMATION_FLAG;
        }

        invalidate();
    }

    public void clearMaskFolder() {
        mFlags &= ~EXTRA_FOLDER_FLAGS_MASK;
        invalidate();
    }

    @SuppressLint("WrongCall")
    protected void drawIconOnly(Canvas canvas) {
//        Drawable drawable = getCompoundDrawables()[1];
//        if (drawable != null) {
//            canvas.save();
//            canvas.translate(drawLeft, drawTop);
//            drawable.draw(canvas);
//            canvas.restore();
//        }
        setDrawText(false);
        super.onDraw(canvas);
        setDrawText(true);
    }

    protected boolean drawMaskFolderImage(Canvas canvas) {
        //calculateDrawPositon();

        final float cx = (drawLeft + drawRight) / 2.0f;
        final float cy = (drawTop + drawBottom) / 2.0f;

        boolean more = false;

        if (folderBgScaleAnimation != null && (more = folderBgScaleAnimation.getTransformation(SystemClock.uptimeMillis(), mTrans))) {
            folderImageAlpha = mTrans.getAlpha();
        }

        if ((mFlags & SHOW_FOLDER_IMAGE_FLAG) == SHOW_FOLDER_IMAGE_FLAG) {
            folderImageScale = folderImageAlpha; // use alpha value as scale since it is hard to get scale value from transformation directly
        }

        mFolderPaint.setAlpha(0xff);

        if ((mFlags & FOLDER_IMAGE_FADE_ANIMATION_FLAG) == FOLDER_IMAGE_FADE_ANIMATION_FLAG) {
            folderImageScale = 1 + (folderImageScale - 1) * folderImageAlpha;
            if (folderImageScale <= 1.0001f) {
                mFolderPaint.setAlpha(isFolder() ? 255 : 0);
//                mFlags &= ~FOLDER_IMAGE_FADE_ANIMATION_FLAG;
                more = false;
                setShowImageOnly(false);
            } else {
                mFolderPaint.setAlpha(isFolder() ? 255 : (int) (0xff * folderImageAlpha));
            }
        }

        if (maskFolderImage == null || maskFolderImage.isRecycled()) {
            maskFolderImage = mIconCache.getFolderIconBg();
        }

        if (maskFolderImage != null && !maskFolderImage.isRecycled()) {
            canvas.save();
            canvas.scale(folderImageScale, folderImageScale, cx, cy);
            canvas.drawBitmap(maskFolderImage, cx - maskFolderImage.getWidth() / 2f, cy - maskFolderImage.getHeight() / 2f, mFolderPaint);
            canvas.restore();
        }

        if (!more) {
            folderBgScaleAnimation = null;
//            mFlags &= ~FOLDER_IMAGE_FADE_ANIMATION_FLAG;
        }
        invalidate();

        return more;
    }

    protected void clearFolderFlag() {
        if ((mFlags & EXTRA_FOLDER_FLAGS_MASK) != 0) {
            mFlags &= ~EXTRA_FOLDER_FLAGS_MASK;
        }
    }

    /** folder icon must override this method and return true;*/
    protected boolean isFolder() {
        return false;
    }

    // alpha animation can be used as scale animation
    protected static class FolderScaleAnimation extends Animation {

        private static final FolderScaleAnimation FOLDER_SHOW = new FolderScaleAnimation(1.0f, 1.1f);
        private static final FolderScaleAnimation FOLDER_HIDE = new FolderScaleAnimation(1.0f, 0.0f);

        private static final int DURATION = 300;

        static {
            FOLDER_SHOW.setDuration(DURATION);
            FOLDER_HIDE.setDuration(DURATION);
        }

        private float mFromAlpha;
        private float mToAlpha;

        public FolderScaleAnimation(float fromAlpha, float toAlpha) {
            mFromAlpha = fromAlpha;
            mToAlpha = toAlpha;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            final float alpha = mFromAlpha;
            t.setAlpha(alpha + ((mToAlpha - alpha) * interpolatedTime));
        }

        public static FolderScaleAnimation obtain(Context context, View host, boolean show) {
            FolderScaleAnimation ret = null;
            if (!show) {
                ret = FOLDER_HIDE;
            } else {
                float scale = 1.2f;

                float iconSize = WorkspaceIconUtils.getIconSizeWithPadding(context);
                int viewWidth = host.getWidth();
                int iconTop = host.getPaddingTop();

                if (iconSize != 0) {
                    scale = Math.min(scale, Math.min(viewWidth / iconSize , (iconTop + iconSize / 2) / iconSize * 2));
                }

                ret = FOLDER_SHOW;
                ret.mFromAlpha = 1.0f;
                ret.mToAlpha = scale;

            }
            ret.reset();
            ret.setStartTime(Animation.START_ON_FIRST_FRAME);
            return ret;
        }
    }

    protected boolean isDesktopView() {
        return true;
    }
}
