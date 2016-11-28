package cc.snser.launcher.screens;

import com.btime.launcher.R;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class PageCellLayout extends WorkspaceCellLayout {
    private ViewGroup mLeftPageWrapper;

    public PageCellLayout(Context context) {
        this(context, null, 0);
    }

    public PageCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mCellWidth = 1920;
        mCellHeight = 1080;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child.getId() == R.id.left_page_content_wrapper) {
            return super.drawChild(canvas, child, drawingTime);
        }
        return true;
    }
}
