package cc.snser.launcher.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PointF;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import cc.snser.launcher.ui.components.ScreenIndicator.OnIndicatorChangedListener;

import com.btime.launcher.R;
import com.shouxinzm.launcher.support.v4.util.ViewUtils;

public class ScreenIndicatorWaterView extends View implements OnClickListener {

    private static final int MINIMUM_MOVING_CIRCLE_RADIUS = 0; //修改初始圆的大小，两个圆会内相切，需要使用外公切线来计算。
    private Paint mPaint;

    private float mSelectRadius;
    private float mUnSelectRadius;
    private int mSelectAlpha;
    private int mUnSelectAlpha;
    private float mStartCenterX;
    private float mPaddingHorizontal;
    private float mPaddingVertical;
    private float mCircleCenterGap;

    private int mScreenCount;
    private int mCurrentScreen;
    private float mRatio;

    private Interpolator mRadiusInterpolator;
    private Interpolator mHorizontalExpendInterpolator;

    private PointF mPoint1, mPoint2;
    private Path mPath;

    private OnIndicatorChangedListener mOnClickListener;

    private int mWaterColor;

    public ScreenIndicatorWaterView(Context context, int color) {
        super(context);
        ViewUtils.setLayerType(this, ViewUtils.LAYER_TYPE_SOFTWARE);

        setFocusable(true);

        mWaterColor = color;

        mPoint1 = new PointF();
        mPoint2 = new PointF();
        mPath = new Path();

        mSelectAlpha = 255;
        mUnSelectAlpha = 127;

        int gap = getResources().getDimensionPixelSize(R.dimen.workspace_water_indicator_gap);
        mSelectRadius = getResources().getDimensionPixelSize(R.dimen.workspace_water_indicator_select_radius);
        mUnSelectRadius = getResources().getDimensionPixelSize(R.dimen.workspace_water_indicator_unselect_radius);
        mPaddingHorizontal = gap / 2;
        mPaddingVertical = getResources().getDimensionPixelSize(R.dimen.workspace_water_indicator_vertical_padding);
        mStartCenterX = mUnSelectRadius + mPaddingHorizontal;
        mCircleCenterGap = 2 * mUnSelectRadius + gap;

        mRadiusInterpolator = new LinearInterpolator();//new DecellerateAccerlarateInterpolator();
        mHorizontalExpendInterpolator = new LinearInterpolator();//new DecellerateAccerlarateInterpolator();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mWaterColor);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(0);

        setOnClickListener(this);
    }

    public void setScreen(int screenCount, int currentScreen) {
        boolean requestLayout = mScreenCount != screenCount || currentScreen != mCurrentScreen;
        mScreenCount = screenCount;
        mCurrentScreen = currentScreen;
        if (requestLayout) {
            requestLayout();
            invalidate();
        }
    }

    public void updateRatio(int currentScreen, float ratio) {
        boolean invalidate = mCurrentScreen != currentScreen || mRatio != ratio;
        mRatio = ratio;
        mCurrentScreen = currentScreen;
        if (invalidate) {
            invalidate();
        }
    }

    public int getMaxScreenCount(int maxWidth) {
        return (int) ((maxWidth - mStartCenterX * 2) / mCircleCenterGap + 1);
    }

    public void setOnClickCallback(OnIndicatorChangedListener onclickListener) {
        mOnClickListener = onclickListener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int y = getMeasuredHeight() / 2;

        int tempCurrentScreen = mCurrentScreen + (int)(mRatio);
        float tempRatio = mRatio % 1;

        boolean drawWater = tempRatio > 0 && tempCurrentScreen != mScreenCount - 1 || tempRatio < 0 && tempCurrentScreen != 0;
        for (int i = 0; i < mScreenCount; i++) {
            if (tempRatio != 0 && drawWater) {
                if (tempCurrentScreen == i || tempRatio > 0 && i == tempCurrentScreen + 1 || tempRatio < 0 && i == tempCurrentScreen - 1) {
                    continue;
                }
            }
            drawNormalCircle(canvas, i == tempCurrentScreen, mStartCenterX + mCircleCenterGap * i, y);
        }

        if (drawWater && tempRatio != 0) {
            float leftX = 0;
            float rightX = 0;
            if (tempRatio < 0) {
                leftX = mStartCenterX + mCircleCenterGap * (tempCurrentScreen - 1);
            } else {
                leftX = mStartCenterX + mCircleCenterGap * tempCurrentScreen;
            }
            rightX = leftX + mCircleCenterGap;

            drawWater(canvas, leftX, rightX, y, tempRatio);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = (int) (2 * mStartCenterX + (mScreenCount - 1) * mCircleCenterGap);
        int height = (int) (Math.max(mSelectRadius * 2, mUnSelectRadius * 2) + mPaddingVertical * 2);
        setMeasuredDimension(width, height);
    }

    private void drawNormalCircle(Canvas canvas, boolean isSelect, float centerX, float centerY) {
        mPaint.setColor(mWaterColor);
        mPaint.setAlpha(isSelect ? mSelectAlpha : mUnSelectAlpha);
        canvas.drawCircle(centerX, centerY, isSelect ? mSelectRadius : mUnSelectRadius, mPaint);
    }

    private void drawWater(Canvas canvas, float leftX, float rightX, int y, float ratio) {
        float interpolatorInput = Math.abs(ratio);
        if (interpolatorInput > 1) {
            interpolatorInput = 1;
        }

        float bigMovingCirleRadius = 0;
        float smallMovingCirleRadius = 0;
        int unSelectAlpha = 0;
        int selectAlpha = 0;
        float bigCircleRadius = 0;
        float smallCircleRadius = 0;
        if (interpolatorInput <= 0.5f) {
            smallMovingCirleRadius = MINIMUM_MOVING_CIRCLE_RADIUS + (mUnSelectRadius / 2 - MINIMUM_MOVING_CIRCLE_RADIUS) * mRadiusInterpolator.getInterpolation(interpolatorInput * 2);
            bigMovingCirleRadius = MINIMUM_MOVING_CIRCLE_RADIUS + (mSelectRadius / 2 - MINIMUM_MOVING_CIRCLE_RADIUS) * mRadiusInterpolator.getInterpolation(interpolatorInput * 2);
            bigCircleRadius = mSelectRadius - mSelectRadius / 2 * mRadiusInterpolator.getInterpolation(interpolatorInput * 2);
            smallCircleRadius = mUnSelectRadius - mUnSelectRadius / 2 * mRadiusInterpolator.getInterpolation(interpolatorInput * 2);
            unSelectAlpha = mUnSelectAlpha + (int) ((mSelectAlpha - mUnSelectRadius) * mRadiusInterpolator.getInterpolation(interpolatorInput));
            selectAlpha = mSelectAlpha - (int) ((mSelectAlpha - mUnSelectAlpha) * mRadiusInterpolator.getInterpolation(interpolatorInput));
        } else {
            smallMovingCirleRadius = mUnSelectRadius / 2 - (mUnSelectRadius / 2 - MINIMUM_MOVING_CIRCLE_RADIUS) * mRadiusInterpolator.getInterpolation(interpolatorInput * 2 - 1);
            bigMovingCirleRadius = mSelectRadius / 2 - (mSelectRadius / 2 - MINIMUM_MOVING_CIRCLE_RADIUS) * mRadiusInterpolator.getInterpolation(interpolatorInput * 2 - 1);
            bigCircleRadius = mSelectRadius / 2 + mSelectRadius / 2 * mRadiusInterpolator.getInterpolation(interpolatorInput * 2 - 1);
            smallCircleRadius = mUnSelectRadius / 2 + mUnSelectRadius / 2 * mRadiusInterpolator.getInterpolation(interpolatorInput * 2 - 1);
            unSelectAlpha = mUnSelectAlpha + (int) ((mSelectAlpha - mUnSelectRadius) * mRadiusInterpolator.getInterpolation(1 - interpolatorInput));
            selectAlpha = mSelectAlpha - (int) ((mSelectAlpha - mUnSelectAlpha) * mRadiusInterpolator.getInterpolation(1 - interpolatorInput));
        }

        float smallWaterDistance = mCircleCenterGap - mUnSelectRadius + MINIMUM_MOVING_CIRCLE_RADIUS;
        float bigWaterDistance = mCircleCenterGap - mSelectRadius + MINIMUM_MOVING_CIRCLE_RADIUS;
      //先画透明的小圆
        mPaint.setAlpha(unSelectAlpha);
        if (ratio < 0) {//向左移动
            if (interpolatorInput < 0.5) {//向左移动的前半程
                drawWaterInner(canvas, leftX, leftX + mUnSelectRadius - MINIMUM_MOVING_CIRCLE_RADIUS + smallWaterDistance * mHorizontalExpendInterpolator.getInterpolation(interpolatorInput * 2), y, smallCircleRadius, smallMovingCirleRadius, mPaint);
            } else {//向左移动的后半程，半透明区域在向右移动
                drawWaterInner(canvas, leftX + smallWaterDistance * mHorizontalExpendInterpolator.getInterpolation(interpolatorInput * 2 - 1), rightX, y, smallMovingCirleRadius, smallCircleRadius, mPaint);
            }
        } else {
            if (interpolatorInput < 0.5) {//向右移动的前半程
                drawWaterInner(canvas, rightX - mUnSelectRadius + MINIMUM_MOVING_CIRCLE_RADIUS - smallWaterDistance * mHorizontalExpendInterpolator.getInterpolation(interpolatorInput * 2), rightX, y, smallMovingCirleRadius, smallCircleRadius, mPaint);
            } else {//向右移动的后半程，半透明区域在向左移动
                drawWaterInner(canvas, leftX, rightX - smallWaterDistance * mHorizontalExpendInterpolator.getInterpolation(interpolatorInput * 2 - 1), y, smallCircleRadius, smallMovingCirleRadius, mPaint);
            }
        }

        //再画不透明的大圆
        mPaint.setAlpha(selectAlpha);
        if (ratio < 0) {//向左移动
            if (interpolatorInput < 0.5) {//向左移动的前半程
                drawWaterInner(canvas, rightX - mSelectRadius + MINIMUM_MOVING_CIRCLE_RADIUS- bigWaterDistance * mHorizontalExpendInterpolator.getInterpolation(interpolatorInput * 2), rightX, y, bigMovingCirleRadius, bigCircleRadius, mPaint);
            } else {//向左移动的后半程，不透明区域在向左移动
                drawWaterInner(canvas, leftX, rightX - bigWaterDistance * mHorizontalExpendInterpolator.getInterpolation(interpolatorInput * 2 - 1), y, bigCircleRadius, bigMovingCirleRadius, mPaint);
            }
        } else {
            if (interpolatorInput < 0.5) {//向右移动的前半程
                drawWaterInner(canvas, leftX, leftX + mSelectRadius - MINIMUM_MOVING_CIRCLE_RADIUS + bigWaterDistance * mHorizontalExpendInterpolator.getInterpolation(interpolatorInput * 2), y, bigCircleRadius, bigMovingCirleRadius, mPaint);
            } else {//向右移动的后半程，不透明区域在向右移动
                drawWaterInner(canvas, leftX + bigWaterDistance * mHorizontalExpendInterpolator.getInterpolation(interpolatorInput * 2 - 1), rightX, y, bigMovingCirleRadius, bigCircleRadius, mPaint);
            }
        }

    }

    private void drawWaterInner(Canvas canvas, float leftX, float rightX, int y, float radiusLeft, float radiusRight, Paint paint) {
        mPath.reset();
        if (radiusLeft != 0) {
            mPath.addCircle(leftX, y, radiusLeft, Direction.CW);
        }
        if (radiusRight != 0) {
            mPath.addCircle(rightX, y, radiusRight, Direction.CW);
        }
        if (radiusLeft != 0 && radiusRight != 0) {
            fillPathWaterCurve(leftX, rightX, y, radiusLeft, radiusRight);
        }
        canvas.drawPath(mPath, paint);
    }

    /**
     * 按圆的外公切线画直线
     * @param leftX
     * @param rightX
     * @param y
     * @param radiusLeft
     * @param radiusRight
     *//*
    private void fillPathWaterLine(float leftX, float rightX, int y, float radiusLeft, float radiusRight) {
        float gap = rightX - leftX;
        //画公切线梯形
        if (radiusLeft > radiusRight) {
            float x = radiusRight * gap / (radiusLeft - radiusRight);
            float xOffset = radiusLeft * radiusLeft / (x + gap);
            mPoint1.x =  (int) (leftX + xOffset);
            mPoint1.y = y - (int) FloatMath.sqrt(radiusLeft * radiusLeft - xOffset * xOffset);

            xOffset = radiusRight * radiusRight / x;
            mPoint2.x = (int) (rightX + xOffset);
            mPoint2.y = y - (int) FloatMath.sqrt(radiusRight * radiusRight - xOffset * xOffset);
        } else if (radiusLeft < radiusRight) {
            float x = radiusLeft * gap / (radiusRight - radiusLeft);
            float xOffset = radiusLeft * radiusLeft / x;
            mPoint1.x = (int) (leftX - xOffset);
            mPoint1.y = y - (int) FloatMath.sqrt(radiusLeft * radiusLeft - xOffset * xOffset);

            xOffset = radiusRight * radiusRight / (x + gap);
            mPoint2.x = (int) (rightX - xOffset);
            mPoint2.y = y - (int) FloatMath.sqrt(radiusRight * radiusRight - xOffset * xOffset);
        } else {
            mPoint1.x = leftX;
            mPoint1.y = y - radiusLeft;
            mPoint2.x = rightX;
            mPoint2.y = y - radiusRight;
        }

        mPath.moveTo(mPoint1.x, mPoint1.y);
        mPath.lineTo(mPoint2.x, mPoint2.y);
        mPath.lineTo(mPoint2.x, y + (y - mPoint2.y));
        mPath.lineTo(mPoint1.x, y + (y - mPoint1.y));
        mPath.close();
    }*/

    /**
     * 按圆的内公切线画曲线
     * @param leftX
     * @param rightX
     * @param y
     * @param radiusLeft
     * @param radiusRight
     */
    private void fillPathWaterCurve(float leftX, float rightX, int y, float radiusLeft, float radiusRight) {
        float gap = rightX - leftX;
        //画内公切线的贝塞尔曲线
        float x = radiusLeft * gap / (radiusLeft + radiusRight);
        float xOffset = radiusLeft * radiusLeft / x;
        mPoint1.x =  leftX + xOffset;
        mPoint1.y = y - FloatMath.sqrt(radiusLeft * radiusLeft - xOffset * xOffset);

        xOffset = radiusRight * radiusRight / (gap - x);
        mPoint2.x = rightX - xOffset;
        mPoint2.y = y - FloatMath.sqrt(radiusRight * radiusRight - xOffset * xOffset);
        mPath.moveTo(mPoint1.x, mPoint1.y);
        mPath.quadTo(leftX + x, y, mPoint2.x, mPoint2.y);
        mPath.lineTo(mPoint2.x, y + (y - mPoint2.y));
        mPath.quadTo(leftX + x, y, mPoint1.x, y + (y - mPoint1.y));
        mPath.close();
    }


    @Override
    public void onClick(View v) {
        int clickIndex = (int) (mTouchDownX / mCircleCenterGap);
        if (clickIndex > mScreenCount - 1) {
            return;
        }
        if (mOnClickListener != null) {
            mOnClickListener.snapToScreen(clickIndex);
        }
    }

    private float mTouchDownX = 0;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = event.getX();
                break;
        }

        return super.onTouchEvent(event);
    }
}
