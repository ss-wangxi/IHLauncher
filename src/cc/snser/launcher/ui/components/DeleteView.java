package cc.snser.launcher.ui.components;

import cc.snser.launcher.App;
import cc.snser.launcher.screens.DeleteZone;
import cc.snser.launcher.ui.utils.Utilities;

import com.btime.launcher.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class DeleteView extends View implements OperationView {

    private float mDeleteViewHeight;//整个垃圾箱图片的高度
    private float mViewHeight;
    private float mViewWidth;
    private float mMarginLeft;
    private float mTop;
    private float mLidHeight;
    private float mWidthDifference;
    private Bitmap mTrashBitmap;
    private Bitmap mTrashLidBitmap;
    private Paint mPaint;

    private int mCount = DeleteZone.ANIMATION_COUNT;
    private boolean mIsTurning;//用来判断动画正向反向以及是否刷新
    private int mAnimationStart = 80;

    public DeleteView(Context context) {
        super(context);
        init();
    }

    public void init() {
    	mViewWidth = Utilities.dip2px(getContext(), 26.5f);
    	mMarginLeft = (Utilities.dip2px(getContext(), 2f));
    	mLidHeight = Utilities.dip2px(getContext(), 4f);
    	mWidthDifference  = Utilities.dip2px(getContext(), 2.3f);
    			
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        mViewHeight = getContext().getResources().getDimensionPixelSize(R.dimen.delete_zone_size) - getContext().getResources().getDimensionPixelSize(R.dimen.delete_zone_padding_bottom);
        mTrashBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.delete_zone_trash);
        mTrashLidBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.delete_zone_trash_lid);
        mDeleteViewHeight = mTrashBitmap.getHeight() + mTrashLidBitmap.getHeight();
        mTop = (mViewHeight - mDeleteViewHeight) / 2;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mCount > mAnimationStart) {
            canvas.drawBitmap(mTrashLidBitmap, mMarginLeft, mTop, mPaint);
        } else if (mCount <= mAnimationStart && mCount >= 0) {
            canvas.save();
            canvas.rotate(- 20 * (1 - mCount * 1.25f / DeleteZone.ANIMATION_COUNT), mMarginLeft + mWidthDifference, mTop + mLidHeight);
            canvas.drawBitmap(mTrashLidBitmap, mMarginLeft, mTop, mPaint);
            canvas.restore();
        }
        canvas.drawBitmap(mTrashBitmap, mMarginLeft, mTop + mLidHeight, mPaint);
        if (!(!mIsTurning && mCount >= DeleteZone.ANIMATION_COUNT)) {
            invalidateSelf();
        }
        super.draw(canvas);
    }


    public void setCount(int count) {
        mCount = count;
        invalidate();
    }

    public void invalidateSelf() {
        if ((mIsTurning && mCount > 0)) {
            mCount -= 4;
        } else if (!mIsTurning && mCount < DeleteZone.ANIMATION_COUNT) {
            mCount += 4;
        }
        if (mCount > DeleteZone.ANIMATION_COUNT) {
            mCount = DeleteZone.ANIMATION_COUNT;
        } else if (mCount < 0) {
            mCount = 0;
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
