package cc.snser.launcher.screens;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;

import android.content.Context;
import android.content.res.Resources;
import android.view.View.MeasureSpec;

public class WorkspaceCellLayoutMeasure {
    private static final String TAG = Workspace.TAG;
    
    public static int cellLayoutWidth;
    public static int cellLayoutHeight;
    
    public static int cellCountHorizontal;
    public static int cellCountVertical;
    public static int cellWidth;
    public static int cellHeight;
    
    public static int cellLayoutMarginLeft;
    public static int cellLayoutMarginTop;
    public static int cellLayoutMarginRight;
    public static int cellLayoutMarginBottom;
    
    public static int cellLayoutPaddingLeft;
    public static int cellLayoutPaddingTop;
    public static int cellLayoutPaddingRight;
    public static int cellLayoutPaddingBottom;
    
    public static int cellGapHorizontal;
    public static int cellGapVertical;
    
    public static int widget2x3MarginLeft;
    public static int widget2x3MarginTop;
    public static int widget2x3MarginRight;
    public static int widget2x3MarginBottom;
    
    public static void onMeasure(Context context, int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthSpecSize != cellLayoutWidth || heightSpecSize != cellLayoutHeight) {
            if (widthSpecMode != MeasureSpec.UNSPECIFIED && heightSpecMode != MeasureSpec.UNSPECIFIED) {
                doMeasure(context, widthSpecSize, heightSpecSize);
            }
        }
    }
    
    private static void doMeasure(Context context, int widthSpecSize, int heightSpecSize) {
        cellLayoutWidth = widthSpecSize;
        cellLayoutHeight = heightSpecSize;
        
        final Resources resources = context.getResources();
        cellCountHorizontal = resources.getInteger(R.integer.workspace_default_cell_num_horizontal);
        cellCountVertical = resources.getInteger(R.integer.workspace_default_cell_num_vertical);
        cellWidth = resources.getDimensionPixelSize(R.dimen.workspace_cell_width);
        cellHeight = resources.getDimensionPixelSize(R.dimen.workspace_cell_height);
        
        cellLayoutMarginLeft = resources.getDimensionPixelSize(R.dimen.workspace_margin_left);
        cellLayoutMarginTop = resources.getDimensionPixelSize(R.dimen.workspace_margin_top);
        cellLayoutMarginRight = resources.getDimensionPixelSize(R.dimen.workspace_margin_right);
        cellLayoutMarginBottom = resources.getDimensionPixelSize(R.dimen.workspace_margin_bottom);
        
        cellLayoutPaddingLeft = 0;
        cellLayoutPaddingTop = 0; //resources.getDimensionPixelSize(R.dimen.virtual_statusbar_height);
        cellLayoutPaddingRight = 0;
        cellLayoutPaddingBottom = 0;
        
        final int totalGapHorizontal = cellLayoutWidth - cellLayoutPaddingLeft - cellLayoutPaddingRight - cellWidth * cellCountHorizontal;
        final int totalGapVertical = cellLayoutHeight - cellLayoutPaddingTop - cellLayoutPaddingBottom - cellHeight * cellCountVertical;
        cellGapHorizontal = totalGapHorizontal / (cellCountHorizontal - 1);
        cellGapVertical = totalGapVertical / (cellCountVertical - 1);
        
        widget2x3MarginLeft = resources.getDimensionPixelSize(R.dimen.widget2x3_margin_left);
        widget2x3MarginTop = resources.getDimensionPixelSize(R.dimen.widget2x3_margin_top);
        widget2x3MarginRight = resources.getDimensionPixelSize(R.dimen.widget2x3_margin_right);
        widget2x3MarginBottom = resources.getDimensionPixelSize(R.dimen.widget2x3_margin_bottom);
        
        XLog.d(TAG, "----------------- doMeasure -----------------");
        XLog.d(TAG, "doMeasure w=" + cellLayoutWidth + " h=" + cellLayoutHeight);
        XLog.d(TAG, "doMeasure ch=" + cellCountHorizontal + " cv=" + cellCountVertical + " cw=" + cellWidth + " ch=" + cellHeight);
        XLog.d(TAG, "doMeasure ml=" + cellLayoutMarginLeft + " mt=" + cellLayoutMarginTop + " mr=" + cellLayoutMarginRight + " mb=" + cellLayoutMarginBottom);
        XLog.d(TAG, "doMeasure pl=" + cellLayoutPaddingLeft + " pt=" + cellLayoutPaddingTop + " pr=" + cellLayoutPaddingRight + " pb=" + cellLayoutPaddingBottom);
        XLog.d(TAG, "doMeasure gh=" + cellGapHorizontal + " gv=" + cellGapVertical);
        XLog.d(TAG, "doMeasure w2x3ml=" + widget2x3MarginLeft + " w2x3mt=" + widget2x3MarginTop + " w2x3mr=" + widget2x3MarginRight + " w2x3mb=" + widget2x3MarginBottom);
    }
    
}
