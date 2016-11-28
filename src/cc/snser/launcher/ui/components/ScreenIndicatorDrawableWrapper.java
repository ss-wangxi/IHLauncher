package cc.snser.launcher.ui.components;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * 封装drawable，保证选中非选中的Drawable都一样大，避免划屏时候重新measure
 * @author yangkai
 *
 */
public class ScreenIndicatorDrawableWrapper extends Drawable { // TODO 把indicator中stateList逻辑挪到这里来
    
    private Drawable mSelectedDrawable;
    
    private Drawable mUnselectedDrawable;
    
    private float left;
    
    private float top;
    
    private boolean mSelected;
    
    ScreenIndicatorDrawableWrapper(Drawable drawable, Drawable unselectedDrawable) {
    	
    	updateDrawable(drawable, unselectedDrawable);
    }
    
    private void updateDrawable(Drawable drawable, Drawable unselectedDrawable)
    {
    	mSelectedDrawable = drawable;
        mUnselectedDrawable = unselectedDrawable;
        
        int xDiff = drawable.getIntrinsicWidth() - unselectedDrawable.getIntrinsicWidth();
        left = xDiff / 2f;
        
        int yDiff = drawable.getIntrinsicHeight() - unselectedDrawable.getIntrinsicHeight();
        top = yDiff / 2f;
        mSelectedDrawable.setBounds(0, 0, getIntrinsicWidth(), getIntrinsicHeight());
        mUnselectedDrawable.setBounds(0, 0, getIntrinsicWidth(), getIntrinsicHeight());
    }
    
    public Drawable getCurrentSelectedDrawable()
    {
    	return mSelectedDrawable;
    }
    
    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        mSelectedDrawable.setBounds(0, 0, right - left, bottom - top);
    }

    @Override
    public void draw(Canvas canvas) {
        float x = 0;
        float y = 0;
        
        if (mSelected) {
            if (mSelectedDrawable instanceof BitmapDrawable && ((BitmapDrawable) mSelectedDrawable).getBitmap().isRecycled()) {
                return;
            }
           
            if (left >= 0 && top >= 0) {
                mSelectedDrawable.draw(canvas);
            } else {
                x = left < 0 ? left : 0;
                y = top < 0 ? top : 0;

                canvas.translate(-x, -y);
                mSelectedDrawable.draw(canvas);
                canvas.translate(x, y);
            }
        } else {
            if (mUnselectedDrawable instanceof BitmapDrawable && ((BitmapDrawable) mUnselectedDrawable).getBitmap().isRecycled()) {
                return;
            }
            if (left <= 0 && top <= 0) {
                mUnselectedDrawable.draw(canvas);
            } else {
                x = left > 0 ? left : 0;
                y = top > 0 ? top : 0;

                canvas.translate(-x, -y);
                mUnselectedDrawable.draw(canvas);
                canvas.translate(x, y);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        
    }
    
    @Override
    public boolean setState(int[] stateSet) {
        boolean select = false;
        for (int i = 0; i < stateSet.length; i++) {
            if (stateSet[i] == android.R.attr.state_selected) {
                select = true;
                break;
            }
        }

        mSelected = select;
        return true;
    }
    
    @Override
    public boolean isStateful() {
        return true;
    }
    
    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public int getIntrinsicHeight() {
        return Math.max(mSelectedDrawable.getIntrinsicHeight(), mUnselectedDrawable.getIntrinsicHeight());
    }

    @Override
    public int getIntrinsicWidth() {
        return Math.max(mSelectedDrawable.getIntrinsicWidth(), mUnselectedDrawable.getIntrinsicWidth());
    }
}
