package cc.snser.launcher.component.choiceapps.appsview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.ui.components.pagedsv.DefCacheManager;
import cc.snser.launcher.ui.components.pagedsv.PagedScrollView;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;

import java.util.List;
import java.util.Observer;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

/**
 * 分页显示应用列表，主要用于实现应用选择UI
 * @author songzhaochun
 *
 */
public class PagedAppsScrollView extends PagedScrollView {

    protected Observer mDrawObserver;

    protected int mLongAxisStartPadding;
    protected int mLongAxisEndPadding;

    protected int mShortAxisStartPadding;
    protected int mShortAxisEndPadding;

    protected int[] mAxis;

    private int mRowDividerId;

    private int mStretchMode = CellLayout.STRETCH_MODE_SPACING;

    protected int mColumn = 4;

    protected int mRow = 4;

    public PagedAppsScrollView(Context context) {
        this(context, null);
    }

    public PagedAppsScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedAppsScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);
        if (a != null) {
            mLongAxisStartPadding = a.getDimensionPixelSize(R.styleable.CellLayout_longAxisStartPadding, 0);
            mLongAxisEndPadding = a.getDimensionPixelSize(R.styleable.CellLayout_longAxisEndPadding, 0);
            mShortAxisStartPadding = a.getDimensionPixelSize(R.styleable.CellLayout_shortAxisStartPadding, 0);
            mShortAxisEndPadding = a.getDimensionPixelSize(R.styleable.CellLayout_shortAxisEndPadding, 0);
            mAxis = new int[2];
            mAxis[0] = a.getInt(R.styleable.CellLayout_shortAxisCells, 4);
            mAxis[1] = a.getInt(R.styleable.CellLayout_longAxisCells, 1);
            a.recycle();
        }

        initLayoutParams();

        // 开启子页面缓存，实现平滑滚动效果
        this.setCacheHandler(new DefCacheManager(this) {
            @Override
            protected void enableChildViewCache(View childView) {
                boolean enabled = LauncherSettings.canEnableDrawCache(PagedAppsScrollView.this.getContext());
                if (enabled) {
                    ((CellLayout) childView).setChildrenDrawnWithCacheEnabled(true);
                    ((CellLayout) childView).setChildrenDrawingCacheEnabled(true);
                }
            }

            @Override
            protected void disableChildViewCache(View childView, boolean destroyCache) {
                ((CellLayout) childView).setChildrenDrawnWithCacheEnabled(false);
                ((CellLayout) childView).setChildrenDrawingCacheEnabled(false);
            }
        });
    }

    /**
     * 绘制观察者
     * @param observer
     */
    public void setDrawObserver(Observer observer) {
        mDrawObserver = observer;
    }

    public int getCountMax() {
        return mColumn * mRow;
    }

    /**
     * 返回现有cell个数
     * @return
     */
    public int countCells() {
        int total = 0;
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            ViewGroup vg = (ViewGroup) getChildAt(i);
            total += vg.getChildCount();
        }
        return total;
    }

    /**
     * 返回最后一个CellLayout
     * @return
     */
    public CellLayout lastCellLayout() {
        final int count = getChildCount();
        if (count <= 0) {
            return null;
        }
        return (CellLayout) getChildAt(count - 1);
    }

    /**
     * 追加应用的View（一组一组的追加）
     * @param cells
     */
    public void appendCells(List<View> cells) {
        final int currentCount = countCells();
        final int count = currentCount + cells.size();
        CellLayout cellLayout = lastCellLayout();
        for (int i = currentCount; i < count; i++) {
            View view = cells.get(i - currentCount);
            if (i % (mColumn * mRow) == 0) {
                if (LOGD_ENABLED) {
                    XLog.d(TAG, "new CellLayout i=" + i);
                }
                cellLayout = new CellLayout(this.getContext());
                cellLayout.setDimension(mColumn, mRow);
                cellLayout.setRowDivider(mRowDividerId);
                cellLayout.setStretchMode(mStretchMode);
                cellLayout.setStartPadding(mLongAxisStartPadding, mLongAxisEndPadding, mShortAxisStartPadding, mShortAxisEndPadding);
                this.addView(cellLayout);
            }
            cellLayout.addView(view);
        }
    }

    /**
     * 初始化排版信息
     * @return
     */
    protected boolean initLayoutParams() {
        boolean ret = false;

        int[] layout = mAxis;
        if (layout != null) {
            int shortAxisCells = layout[0];
            int longAxisCells = layout[1];
            if (shortAxisCells != mColumn || longAxisCells != mRow) {
                mColumn = shortAxisCells;
                mRow = longAxisCells;
                ret = true;
            }
        }

        return ret;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mDrawObserver != null) {
            mDrawObserver.update(null, canvas);
        }
    }
}
