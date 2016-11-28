package cc.snser.launcher.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.View;
import cc.snser.launcher.App;
import cc.snser.launcher.screens.DeleteZone;
import cc.snser.launcher.ui.utils.Utilities;

import com.btime.launcher.R;

public class HiddenView extends View implements OperationView {

    private Paint mPaint;
    private Paint mLinePaint;
    private Paint mTransparentPaint;
    private RectF mArcRectF;//弧形

    private float mTop;
    private float mMarginLeft;
    private float mArcHeight;
    private float mArcWidth;
    private float mLineHeight;
    private float mLineWidth;
    private float mTransparentCircleRadius;
    private float mViewWidth;
    private float mMaxHeight;
    private float mTransparentObliquWidth;
    private float mTransparentWidth;
    private float mTransparentObliquStrokeWidth;

    private int mCount = DeleteZone.ANIMATION_COUNT;
    private int mAnimationStart = DeleteZone.ANIMATION_COUNT - 1;//改动时候不要小于mTransparentAnimationStart， 后面判断用到
    private int mTransparentAnimationStart = DeleteZone.ANIMATION_COUNT - 20;
    private boolean mIsTurning;//用来判断动画正向反向以及是否刷新
    private float mPercentage;//插值器

    private PorterDuffXfermode mDuffXfermode = new PorterDuffXfermode(Mode.DST_IN);//逐步擦除
    private PorterDuffXfermode mXfermode = new PorterDuffXfermode(Mode.CLEAR);//直接擦除
    public HiddenView(Context context) {
        super(context);
        init();
    }

    public void init() {
    	
    	mTop = Utilities.dip2px(getContext(), 15f);//保证图片居中,根据总高度和图形高度来算的
    	mMarginLeft = Utilities.dip2px(getContext(), 4);
    	mArcHeight = Utilities.dip2px(getContext(), 7);
    	mArcWidth = Utilities.dip2px(getContext(),21);
    	mLineHeight = Utilities.dip2px(getContext(), 2);//斜线纵向浮动
    	mLineWidth = Utilities.dip2px(getContext(),3);//斜线横向浮动
    	mTransparentCircleRadius = Utilities.dip2px(getContext(), 5);//透明圆半径
    	mViewWidth = Utilities.dip2px(getContext(), 28.5f);
    	mTransparentObliquWidth = Utilities.dip2px(getContext(), 6f);//透明斜线宽
    	mTransparentWidth = Utilities.dip2px(getContext(), 1.5f);//白色斜线宽
    	mTransparentObliquStrokeWidth = Utilities.dip2px(getContext(), 2f);//透明圆的线宽
    	mMaxHeight = getContext().getResources().getDimensionPixelSize(R.dimen.delete_zone_size);

        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Style.FILL);

        mLinePaint = new Paint();
        mLinePaint.setColor(Color.WHITE);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Style.STROKE);
        mLinePaint.setStrokeWidth(mTransparentWidth);
        mLinePaint.setStrokeCap(Cap.ROUND);//圆角线

        mTransparentPaint = new Paint();
        mTransparentPaint.setColor(Color.WHITE);
        mTransparentPaint.setAntiAlias(true);
        mTransparentPaint.setStyle(Style.STROKE);
        mTransparentPaint.setStrokeWidth(mTransparentObliquWidth);
        mTransparentPaint.setXfermode(mDuffXfermode);

        mArcRectF = new RectF(mMarginLeft, mTop, mMarginLeft + mArcWidth, mTop + mArcHeight + mArcHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setStyle(Style.FILL);
        if (mCount >= mAnimationStart) {
            drawEye(canvas);
        } else if (mCount < mAnimationStart && mCount >= mTransparentAnimationStart) {
            drawHiddenLine(canvas);
            int alpha = (int) ((mAnimationStart - mCount) * 1.0f / (mAnimationStart - (mTransparentAnimationStart)) * 255);
            mLinePaint.setAlpha(alpha);
            canvas.drawLine(mMarginLeft - mLineWidth, mTop + mArcHeight + mArcHeight + mLineHeight, mMarginLeft + mArcWidth + mLineWidth, mTop - mLineHeight, mLinePaint);
        } else if (mCount < mTransparentAnimationStart && mCount >= 0) {
            drawHiddenLine(canvas);
            mLinePaint.setAlpha(255);
            float horizontalDistance = mLineWidth * (1 - mPercentage) * 1.5f;
            float verticalDistance = mLineHeight * (1 - mPercentage) * 1.5f;
            if (mPercentage < 0) {
                horizontalDistance = mLineWidth * (1 + mPercentage) * 1.5f;
                verticalDistance = mLineHeight * (1 + mPercentage) * 1.5f;
            }
            canvas.drawLine(mMarginLeft - mLineWidth + horizontalDistance, mTop + mArcHeight + mArcHeight + mLineHeight - verticalDistance, mMarginLeft + mArcWidth + mLineWidth - horizontalDistance, mTop - mLineHeight + verticalDistance, mLinePaint);
        }

        if (!(!mIsTurning && mCount >= DeleteZone.ANIMATION_COUNT)) {
            invalidateSelf();
        }
        super.onDraw(canvas);
    }

    private void drawEye(Canvas canvas) {
        canvas.saveLayer(0, 0, mViewWidth, mMaxHeight, null, Canvas.ALL_SAVE_FLAG);
        canvas.drawArc(mArcRectF, 0, 360, true, mPaint);
        mPaint.setXfermode(mXfermode);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(mTransparentObliquStrokeWidth);
        canvas.drawCircle(mMarginLeft + mArcWidth / 2, mTop + mArcHeight, mTransparentCircleRadius, mPaint);
        mPaint.setXfermode(null);
        canvas.restore();
    }

    private void drawHiddenLine(Canvas canvas) {
        canvas.saveLayer(0, 0, mViewWidth, mMaxHeight, null, Canvas.ALL_SAVE_FLAG);
        canvas.drawArc(mArcRectF, 0, 360, true, mPaint);
        mPaint.setXfermode(mXfermode);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(mTransparentObliquStrokeWidth);
        canvas.drawCircle(mMarginLeft + mArcWidth / 2, mTop + mArcHeight, mTransparentCircleRadius, mPaint);
        mPaint.setStrokeWidth(mTransparentObliquWidth);
        if (mCount >= mAnimationStart) {
            mTransparentPaint.setAlpha(255);
        } else if (mCount < mAnimationStart && mCount >= mTransparentAnimationStart) {
            mTransparentPaint.setAlpha((int) ((mCount - (mTransparentAnimationStart)) * 1.0f / (mAnimationStart - (mTransparentAnimationStart)) * 255));
        } else if (mCount < mTransparentAnimationStart && mCount >= 0) {
            mTransparentPaint.setAlpha(0);
        }
        canvas.drawLine(mMarginLeft, mTop + mArcHeight + mArcHeight, mMarginLeft + mArcWidth, mTop, mTransparentPaint);
        mPaint.setXfermode(null);
        canvas.restore();
    }



    public void setCount(int count) {
        mCount = count;
        invalidate();
    }

    public void invalidateSelf() {
        if ((mIsTurning && mCount > 0)) {
            if (mCount < mTransparentAnimationStart) {
                mCount -= 2;
            } else {
                mCount -= 3;
            }
        } else if (!mIsTurning && mCount < DeleteZone.ANIMATION_COUNT) {
            if (mCount < mTransparentAnimationStart) {
                mCount += 2;
            } else if (!mIsTurning && mCount < mAnimationStart && mCount > mTransparentAnimationStart) {
                mCount ++;
            } else {
                mCount += 3;
            }
        }
        if (mCount < 0) {
            mCount = 0;
        }
        float count = DeleteZone.ANIMATION_COUNT * 1.0f / mTransparentAnimationStart * mCount;
        mPercentage = (float)((Math.cos((count * 1.0f / DeleteZone.ANIMATION_COUNT + 1) * Math.PI) / 2.0f) + 0.5f) * 1.2f - 0.2f;//从大变小
        if (mPercentage >= 1) {
            mPercentage = 1.0f;
        }
        this.invalidate();
    }

    public void setAnimationStart(boolean isTurning) {
        mIsTurning = isTurning;
        invalidate();
    }

    @Override
    public float getViewWidth() {
        return mViewWidth;
    }

}
