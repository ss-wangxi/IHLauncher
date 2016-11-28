package cc.snser.launcher.component.choiceapps.appsview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author songzhaochun
 *
 */
public class CellLayout extends ViewGroup {

    public static final int STRETCH_MODE_SPACING = 0;

    public static final int STRETCH_MODE_CONTENT = 1;

    private boolean mHorizontalExtend = true;

    private int mColumn = 4;
    private int mRow = 4;

    private int mRowDividerId;

    private int mStretchMode;

    private int mLongAxisStartPadding;
    private int mLongAxisEndPadding;

    private int mShortAxisStartPadding;
    private int mShortAxisEndPadding;

    public CellLayout(Context context) {
        super(context);

        this.setClickable(true);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setClickable(true);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.setClickable(true);
    }

    public void setDimension(int column, int row) {
        if (mColumn != column || mRow != row) {
            mColumn = column;
            mRow = row;
            requestLayout();
        }
    }

    public void setRowDivider(int id) {
        mRowDividerId = id;
    }

    public void setStretchMode(int mode) {
        mStretchMode = mode;
    }

    public void setStartPadding(int longStart, int longEnd, int shortStart, int shortEnd) {
        mLongAxisStartPadding = longStart;
        mLongAxisEndPadding = longEnd;
        mShortAxisStartPadding = shortStart;
        mShortAxisEndPadding = shortEnd;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }

        if (mStretchMode == STRETCH_MODE_SPACING) {
            measureChildren(widthMeasureSpec, heightMeasureSpec);
        } else {
            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                    widthSpecSize / mColumn, widthSpecMode);
            int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSpecSize
                    / mRow, heightSpecMode);
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = getWidth();
        int height = getHeight();

        int verticalSpace = (height - (mLongAxisStartPadding + mLongAxisEndPadding)) / mRow;
        int horizontalSpace = (width - (mShortAxisStartPadding + mShortAxisEndPadding)) / mColumn;

        int screenItemSize = mColumn * mRow;

        final int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int screen = i / screenItemSize;

            int measuredHeightOffset = child.getMeasuredHeight() / 2;
            int measuredWidthOffset = child.getMeasuredWidth() / 2;

            int screenHeightOffset = height * (mHorizontalExtend ? 0 : screen) + mLongAxisStartPadding;
            int screenWidthOffset = width * (!mHorizontalExtend ? 0 : screen) + mShortAxisStartPadding;

            child.layout(screenWidthOffset + (int) (horizontalSpace * (i % mColumn + 0.5) - measuredWidthOffset), screenHeightOffset
                    + (int) (verticalSpace * (i / mColumn % mRow + 0.5) - measuredHeightOffset),
                    screenWidthOffset + (int) (horizontalSpace * (i % mColumn + 0.5) + measuredWidthOffset), screenHeightOffset
                            + (int) (verticalSpace * (i / mColumn % mRow + 0.5) + measuredHeightOffset));
        }
    }

    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        super.setChildrenDrawingCacheEnabled(enabled);
    }

    @Override
    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        super.setChildrenDrawnWithCacheEnabled(enabled);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mRowDividerId != 0) {
            Drawable drawable = getContext().getResources().getDrawable(mRowDividerId);
            int width = getWidth();
            int height = getHeight();
            int screenItemSize = mColumn * mRow;
            int rowDividerHeight = drawable.getIntrinsicHeight();
            int verticalSpace = (height - (mLongAxisStartPadding + mLongAxisEndPadding)) / mRow;

            int index = 0;
            int screenCount = 1;

            int lineNum = (index < screenCount - 1) ? (mRow - 1) : (getChildCount() - index * screenItemSize) / mColumn
                    - ((getChildCount() - index * screenItemSize) % mColumn == 0 ? 1 : 0);
            for (int i = 1; i <= lineNum; i++) {
                int yPosition = (mHorizontalExtend ? 0 : height * index) + mLongAxisStartPadding + verticalSpace * i;
                int xPosition = (mHorizontalExtend ? width * index : 0) + mShortAxisStartPadding;
                drawable.setBounds(xPosition + 2, yPosition - rowDividerHeight / 2, xPosition + width - 2, yPosition + rowDividerHeight / 2);
                drawable.draw(canvas);
            }
        }
    }
}
