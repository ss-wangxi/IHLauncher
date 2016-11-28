package cc.snser.launcher.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.View;
import cc.snser.launcher.App;
import cc.snser.launcher.screens.DeleteZone;
import cc.snser.launcher.ui.utils.Utilities;

import com.btime.launcher.R;

public class GrainView extends View implements OperationView {
    private Paint mPaintStroke;
    private Paint mPaintFill;
    private Path mPathPolygon;//箭头
    private RectF mRectF;

    private final static int FIRST = 1;//用以判断四个小方块的动画时机
    private final static int SECOND = 2;
    private final static int THIRD = 3;
    private final static int FOUR = 4;

    private int mAnimationStart = 70;//动画开始的时机
    private int mArrow = 40;//箭头出现的时机
    private int mGapCount = 5;//打开上部缺口需要刷新次数

    private float mStrokeWidth;
    private float mMarginLeft;
    private float mTop;
    private float mInterval;
    private int mMargin;
    private float mWidth;
    private float mYDistance;
    private float mMaxHeight;
    private float mViewWidth;
    private float mRectFWidth;

    private float[] mFirst = {mMargin + mMarginLeft, mMargin + mTop, mMargin + mMarginLeft + mWidth, mMargin + mWidth + mTop};//四个小方块
    private float[] mSecond = {mMargin + mMarginLeft + mInterval + mWidth, mMargin + mTop, mMargin + mMarginLeft + mInterval + mWidth + mWidth, mMargin + mWidth + mTop};
    private float[] mThird = {mMargin + mMarginLeft, mMargin + mWidth + mInterval + mTop, mMargin + mMarginLeft + mWidth, mMargin + mWidth + mInterval + mWidth + mTop};
    private float[] mFour = {mMargin + mMarginLeft + mWidth + mInterval, mMargin + mWidth + mInterval + mTop, mMargin + mMarginLeft + mWidth + mInterval + mWidth, mMargin + mWidth + mInterval + mWidth + mTop};

    private int mCount = DeleteZone.ANIMATION_COUNT;
    private boolean mIsTurning;//用来判断动画正向反向以及是否刷新

    private PorterDuffXfermode mDuffXfermode = new PorterDuffXfermode(Mode.CLEAR);

    public GrainView(Context context) {
        super(context);
        init();
    }

    public void init() {
    	mStrokeWidth = Utilities.dip2px(getContext(), 1.5f);
    	mMarginLeft = Utilities.dip2px(getContext(), 1.5f);//为了保证线宽不会被裁减掉，向右边挪动一些
    	mTop = Utilities.dip2px(getContext(), 12f);//保证图片居中
    	mInterval = Utilities.dip2px(getContext(), 2f);//间隔*2，小方块与方形的距离*1，小方块宽度*2，三者之和小于等于方形的宽度
    	mMargin = (Utilities.dip2px(getContext(), 4f));
    	mWidth = Utilities.dip2px(getContext(), 5f);
    	mYDistance = Utilities.dip2px(getContext(), 2);//小方块向上飞出时的Y轴上的单位距离
    	mViewWidth = Utilities.dip2px(getContext(), 26.5f);//grain的宽度
    	mRectFWidth = Utilities.dip2px(getContext(), 20);//方形的宽度

    	    
        mMaxHeight = getContext().getResources().getDimensionPixelSize(R.dimen.delete_zone_size);

        if (mPaintStroke == null) {
            mPaintStroke = new Paint();
        }
        mPaintStroke.setAntiAlias(true);
        mPaintStroke.setStyle(Style.STROKE);
        mPaintStroke.setColor(0xffffffff);
        mPaintStroke.setStrokeWidth(mStrokeWidth);

        if (mPaintFill == null) {
            mPaintFill = new Paint();
        }
        mPaintFill.setAntiAlias(true);
        mPaintFill.setStyle(Style.FILL);
        mPaintFill.setColor(0xffffffff);

        mRectF = new RectF(mMarginLeft, mTop, mMarginLeft + mRectFWidth, mTop + mRectFWidth);

        mPathPolygon = new Path();
        mPathPolygon.moveTo(Utilities.dip2px(getContext(), 5) + mMarginLeft, mTop + Utilities.dip2px(getContext(), 10));
        mPathPolygon.lineTo(Utilities.dip2px(getContext(), 10) + mMarginLeft, mTop + Utilities.dip2px(getContext(), 5));
        mPathPolygon.lineTo(Utilities.dip2px(getContext(), 15) + mMarginLeft, mTop + Utilities.dip2px(getContext(), 10));
        mPathPolygon.lineTo(Utilities.dip2px(getContext(), 12.5f) + mMarginLeft, mTop + Utilities.dip2px(getContext(), 10));
        mPathPolygon.lineTo(Utilities.dip2px(getContext(), 12.5f) + mMarginLeft, mTop + Utilities.dip2px(getContext(), 15));
        mPathPolygon.lineTo(Utilities.dip2px(getContext(), 7.5f) + mMarginLeft, mTop + Utilities.dip2px(getContext(), 15));
        mPathPolygon.lineTo(Utilities.dip2px(getContext(), 7.5f) + mMarginLeft, mTop + Utilities.dip2px(getContext(), 10));
        mPathPolygon.lineTo(Utilities.dip2px(getContext(), 5) + mMarginLeft, mTop + Utilities.dip2px(getContext(), 10));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCount > mAnimationStart) {
            mPaintFill.setAntiAlias(true);
            mPaintFill.setStyle(Style.FILL);
            mPaintFill.setColor(0xffffffff);
            canvas.drawRoundRect(mRectF, 2, 2, mPaintStroke);
            canvas.drawRect(mFirst[0], mFirst[1], mFirst[2], mFirst[3], mPaintFill);
            canvas.drawRect(mSecond[0], mSecond[1], mSecond[2], mSecond[3], mPaintFill);
            canvas.drawRect(mThird[0], mThird[1], mThird[2], mThird[3], mPaintFill);
            canvas.drawRect(mFour[0], mFour[1], mFour[2], mFour[3], mPaintFill);
        } else if (mCount <= mAnimationStart + 4 && mCount >= 0){
            canvas.saveLayer(0, 0, mViewWidth, mMaxHeight, null, Canvas.ALL_SAVE_FLAG);
            canvas.drawRoundRect(mRectF, 2, 2, mPaintStroke);
            mPaintFill.setXfermode(mDuffXfermode);//擦除
            if (mCount >= mAnimationStart - mGapCount) {
                canvas.drawRect(mMarginLeft + Utilities.dip2px(getContext(), 10 - (5 - (mCount - (mAnimationStart - mGapCount)))), 0, mMarginLeft + Utilities.dip2px(getContext(), 10 + (5 - (mCount - (mAnimationStart - mGapCount)))), Utilities.dip2px(getContext(), 25), mPaintFill);
            } else {
                canvas.drawRect(mMarginLeft + Utilities.dip2px(getContext(), 5), 0, mMarginLeft + Utilities.dip2px(getContext(), 15), Utilities.dip2px(getContext(), 25), mPaintFill);
            }
            mPaintFill.setXfermode(null);
            canvas.restore();
            mPaintFill.setAlpha(255);
            canvas.drawRect(mFirst[0] + getPositionX(mAnimationStart - mGapCount - mCount, FIRST), mFirst[1] - getPositionY(mAnimationStart - mGapCount - mCount), mFirst[2] + getPositionX(mAnimationStart - mGapCount - mCount, FIRST), mFirst[3] - getPositionY(mAnimationStart - mGapCount - mCount), mPaintFill);
            canvas.drawRect(mSecond[0] + getPositionX(mAnimationStart - mGapCount - mCount, SECOND), mSecond[1] - getPositionY(mAnimationStart - mGapCount - mCount - 5), mSecond[2] + getPositionX(mAnimationStart - mGapCount - mCount, SECOND), mSecond[3] - getPositionY(mAnimationStart - mGapCount - mCount - 5), mPaintFill);
            canvas.drawRect(mThird[0] + getPositionX(mAnimationStart - mGapCount - mCount, THIRD), mThird[1] - getPositionY(mAnimationStart - mGapCount - mCount - 6), mThird[2] + getPositionX(mAnimationStart - mGapCount - mCount, THIRD), mThird[3] - getPositionY(mAnimationStart - mGapCount - mCount - 6), mPaintFill);
            canvas.drawRect(mFour[0] + getPositionX(mAnimationStart - mGapCount - mCount, FOUR), mFour[1] - getPositionY(mAnimationStart - mGapCount - mCount - 11), mFour[2] + getPositionX(mAnimationStart - mGapCount - mCount, FOUR), mFour[3] - getPositionY(mAnimationStart - mGapCount - mCount - 11), mPaintFill);
            if (mCount <= mArrow) {
                float percentage = (mArrow - mCount) * 1.0f / 10;
                if (percentage > 1) {
                    percentage = 1;
                } else if (percentage < 0) {
                    percentage = 0;
                }
                canvas.translate(0, (1 - percentage) * mWidth);
                int aplha = (int) (percentage * 255);
                if (aplha < 0) {
                    aplha = 0;
                } else if (aplha > 255) {
                    aplha = 255;
                }
                mPaintFill.setAlpha(aplha);
                canvas.drawPath(mPathPolygon, mPaintFill);
                canvas.translate(0, - (1 - percentage) * mWidth);
            }
        }

        if (!(!mIsTurning && mCount >= DeleteZone.ANIMATION_COUNT)) {
            invalidateSelf();
        }
        super.onDraw(canvas);
    }

    public void setCount(int count) {
        mCount = count;
        invalidate();
    }

    public void invalidateSelf() {
        if ((mIsTurning && mCount > 0)) {
            if (mCount < mAnimationStart + 4) {
                mCount --;
            } else {
                mCount -= 4;
            }
        } else if (!mIsTurning && mCount < DeleteZone.ANIMATION_COUNT) {
            if (mCount < mAnimationStart + 4) {
                mCount ++;
            } else {
                mCount += 4;
            }
        }
        this.invalidate();
    }

    public void setAnimationStart(boolean isTurning) {
        mIsTurning = isTurning;
        invalidate();
    }

    private float getPositionX(float position, int which) {
        switch (which) {
            case FIRST:
                if (position >= 1) {
                    return (float) Math.sin((position * 3) / mAnimationStart * 3.0f * Math.PI / 2.0f) * mYDistance;
                }
                break;
            case SECOND:
                if (position >= 4) {
                    return - (float) Math.sin((position * 3 - 5) / mAnimationStart * 3.0f * Math.PI / 2.0f) * mYDistance;
                }
                break;
            case THIRD:
                if (position >= 7) {
                    return (float) Math.sin((position * 3 - 6) / mAnimationStart * 3.0f * Math.PI / 2.0f) * mYDistance;
                }
                break;
            case FOUR:
                if (position >= 10) {
                    return - (float) Math.sin((position * 3 - 11) / mAnimationStart * 3.0f * Math.PI / 2.0f) * mYDistance;
                }
                break;

            default:
                return 0;
        }
        return 0;
    }

    private float getPositionY(int position) {
        if (position >= 0) {
            return 1.8f * Utilities.dip2px(getContext(), position);
        }
        return 0;
    }

    @Override
    public float getViewWidth() {
        return mViewWidth;
    }
}
