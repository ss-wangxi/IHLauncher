package cc.snser.launcher.ui.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.btime.launcher.R;
import com.shouxinzm.launcher.util.ScreenDimensUtils;

public class LineView extends View{
    private RectF mRectF;

    private int mScreenWidth;

    private float mHeight;
    //private int mMaxHeight;

    private DragEnterAnimation mDragEnterAnimation;
    private Bitmap mDeleteBackground;
    private int mNormalDrawHeight = 0;


    public LineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public void init() {
    	
        mScreenWidth = ScreenDimensUtils.getScreenWidth(getContext());
        //mMaxHeight = getContext().getResources().getDimensionPixelSize(R.dimen.delete_zone_size);
        mHeight = getContext().getResources().getDimensionPixelSize(R.dimen.delete_zone_size) - getContext().getResources().getDimensionPixelSize(R.dimen.delete_zone_padding_bottom);

        mRectF = new RectF(0, 0, mScreenWidth, mHeight);

        mDeleteBackground = BitmapFactory.decodeResource(getResources(), R.drawable.deletezone_bg);
        mNormalDrawHeight = (int)(mDeleteBackground.getHeight() * 0.6);
    }


    @Override
    public void draw(Canvas canvas) {
        canvas.clipRect(mRectF);

        int drawHeight = mNormalDrawHeight;
        if(mDragEnterAnimation == null){
        }else{
            float fPercentage = mDragEnterAnimation.getPercentage();
            int totalDistance = mDeleteBackground.getHeight() - mNormalDrawHeight;
            drawHeight += (int)(totalDistance * fPercentage);
        }

        Rect src = new Rect(0, mDeleteBackground.getHeight() - drawHeight, mDeleteBackground.getWidth(), mDeleteBackground.getHeight());
        Rect dst = new Rect(0, 0, mScreenWidth, drawHeight);
        canvas.drawBitmap(mDeleteBackground, src, dst, null);

        if(mDragEnterAnimation != null && !mDragEnterAnimation.isFinished()){
            mDragEnterAnimation.stepAnimation();
        }
    }

    public void setAnimationStart(boolean isTurning) {
        if(mDragEnterAnimation == null){
            mDragEnterAnimation = new DragEnterAnimation();
        }
        mDragEnterAnimation.setDirection(isTurning ? true : false);
        invalidate();
    }

    private class DragEnterAnimation{
        private float mPercentage;
        private boolean mShow;
        public DragEnterAnimation(){
            mPercentage = 0.0f;
        }

        public float getPercentage(){
            return mPercentage;
        }

        public void setDirection(boolean show){
            mShow = show;
        }

        public void stepAnimation(){
            if(!isFinished()){
                if(mShow){
                    mPercentage = mPercentage + 0.1f;
                    mPercentage = Math.min(mPercentage, 1.0f);
                }else{
                    mPercentage -= mPercentage + 0.1f;
                    mPercentage = Math.max(mPercentage, 0.0f);
                }
                invalidate();
            }
        }

        public boolean isFinished(){
            if(mShow){
                return mPercentage > 1.0f;
            }else{
                return mPercentage <= 0.0f;
            }

        }

    }
}
