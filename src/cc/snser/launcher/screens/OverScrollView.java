package cc.snser.launcher.screens;

import com.btime.launcher.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class OverScrollView extends ImageView {
    
    public static final int DIRECTION_NONE = 0x100;
    public static final int DIRECTION_LEFT = 0x101;
    public static final int DIRECTION_RIGHT = 0x102;
    
    private int mDirection = DIRECTION_NONE;
    
    private int mLeftDrawableResid = R.drawable.outerscroll_left_bg;
    private int mRightDrawableResid = R.drawable.outerscroll_right_bg;
    
    private int mMinScrollX = Integer.MIN_VALUE;
    private int mMaxScrollX = Integer.MAX_VALUE;
    private int mMaxOverScrollX = Integer.MAX_VALUE;

    public OverScrollView(Context context) {
        super(context);
    }

    public OverScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void init(int minScrollX, int maxScrollX, int maxOverScrollX) {
        mMinScrollX = minScrollX;
        mMaxScrollX = maxScrollX;
        mMaxOverScrollX = maxOverScrollX;
    }
    
    public void onWorkspaceScroll(int x, int y) {
        int direction;
        float alpha;
        if (x < mMinScrollX) {
            direction = DIRECTION_LEFT;
            alpha = (mMinScrollX - x) * 1.0f / mMaxOverScrollX;
        } else if (x > mMaxScrollX) {
            direction = DIRECTION_RIGHT;
            alpha = (x - mMaxScrollX) * 1.0f / mMaxOverScrollX;
        } else {
            direction = DIRECTION_NONE;
            alpha = 0.0f;
        }
        if (direction != mDirection) {
            mDirection = direction;
            setImageResource(direction == DIRECTION_LEFT ? mLeftDrawableResid : mRightDrawableResid);
        }
        setAlpha(alpha);
    }

}
