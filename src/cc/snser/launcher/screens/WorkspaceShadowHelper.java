
package cc.snser.launcher.screens;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import cc.snser.launcher.AbstractWorkspace;
import cc.snser.launcher.CellLayout;
import cc.snser.launcher.apps.components.AppIcon;
import cc.snser.launcher.apps.components.IconView;
import cc.snser.launcher.apps.model.ItemInfo;
import cc.snser.launcher.ui.dragdrop.DragView;
import cc.snser.launcher.ui.dragdrop.DropTarget;
import cc.snser.launcher.ui.dragdrop.DragController.CreateOutlineListener;
import cc.snser.launcher.ui.utils.UiConstants;
import cc.snser.launcher.ui.utils.Utilities;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;
import cc.snser.launcher.widget.Widget;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.util.BitmapUtils;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

/**
 * {@link Workspace} 在拖拽时画虚框背景的辅助类
 *
 * @author yangkai
 */
public class WorkspaceShadowHelper implements CreateOutlineListener {

    private static final String TAG = "Launcher.WorkspaceShadowHelper";

    private final AbstractWorkspace mWorkspace;

    private final Context mContext;

    private final Rect mTempRect = new Rect();

    private int[] mTempPosition = new int[2];

    private DragView mShadowDragView;

    private Bitmap mDragOutline;

   public WorkspaceShadowHelper(AbstractWorkspace workspace, Context context) {
        mWorkspace = workspace;
        mContext = context;
    }

    @Override
    public void onCreateOutline(Object dragInfo, View v) {    	
        createShadowView(dragInfo, v, mWorkspace.getCurrentDropLayoutIndex());
    }

    @Override
    public void onCreateOutline(Object dragInfo, Drawable drawable) {
        createDragOutline(drawable, mWorkspace.getCurrentDropLayout());
    }

    private void createShadowView(Object dragInfo, View child, int currentDropLayoutIndex) {
        try {
            final Canvas canvas = new Canvas();
            // The outline is used to visualize where the item will land if
            // dropped
            BitmapUtils.recycleBitmap(mDragOutline);
            if (mShadowDragView != null) {
                mShadowDragView.closeInstance();
            }
            removeShadow();
            CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(currentDropLayoutIndex);
            mDragOutline = createDragOutline(child, 0, canvas, cellLayout, dragInfo);
            if (Workspace.sInEditMode) {
                Bitmap bitmap = BitmapUtils.createScaledBitmap(mDragOutline, Workspace.sEditModeScaleRatio, true);
                if (mDragOutline != bitmap) {
                    BitmapUtils.recycleBitmap(mDragOutline);
                    mDragOutline = bitmap;
                }
            }
            mShadowDragView = new DragView(mContext, mDragOutline, 10, 10, 0, 0, (int) (mDragOutline.getWidth() * 1.0f), (int) (mDragOutline.getHeight() * 1.0f), DragView.TYPE_SHADOW_VIEW, true);
        } catch (Throwable e) {
            XLog.e(TAG, "Create the graphic outline failed");
        }
    }

    private void createDragOutline(Drawable drawable, CellLayout currentDropLayout) {
        try {
            final Canvas canvas = new Canvas();
            // The outline is used to visualize where the item will land if
            // dropped
            BitmapUtils.recycleBitmap(mDragOutline);
            if (mShadowDragView != null) {
                mShadowDragView.closeInstance();
            }
            removeShadow();
            CellLayout cellLayout = currentDropLayout;
            int width = cellLayout.getItemWidthForSpan(1);
            int height = cellLayout.getItemHeightForSpan(1);
            final Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            canvas.setBitmap(b);
            canvas.save();
            int hpadding = (cellLayout.getCellWidth() - drawable.getIntrinsicWidth()) / 2;
            int vpadding = mContext.getResources().getDimensionPixelSize(R.dimen.workspace_icon_padding_top);
            canvas.translate(hpadding, vpadding);
            if (drawable.getBounds().isEmpty()) {
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            }
            drawable.draw(canvas);
            canvas.restore();
            //mOutlineHelper.createOutlineWithBlur(b, canvas, outlineColor, outlineColor);
            mDragOutline = b;
            if (Workspace.sInEditMode) {
                Bitmap bitmap = BitmapUtils.createScaledBitmap(mDragOutline, Workspace.sEditModeScaleRatio, true);
                if (mDragOutline != bitmap) {
                    BitmapUtils.recycleBitmap(mDragOutline);
                    mDragOutline = bitmap;
                }
            }
            mShadowDragView = new DragView(mContext, mDragOutline, 10, 10, 0, 0, (int) (mDragOutline.getWidth() * 1.0f), (int) (mDragOutline.getHeight() * 1.0f), DragView.TYPE_SHADOW_VIEW,true);
        } catch (Throwable e) {
            XLog.e(TAG, "Create the graphic outline failed");
        }
    }

    /**
     * Returns a new bitmap to be used as the object outline, e.g. to visualize the drop location.
     * Responsibility for the bitmap is transferred to the caller.
     */
    private Bitmap createDragOutline(View v, int padding, Canvas canvas, CellLayout cellLayout, Object dragInfo) {
        //final int outlineColor = mContext.getResources().getColor(R.color.outline_color);
        int spanX = 1;
        int spanY = 1;

        if (dragInfo instanceof Widget) {
            Widget info = (Widget) dragInfo;
            spanX = info.getSpanX();
            spanY = info.getSpanY();
        } else if (dragInfo instanceof ItemInfo) {
            ItemInfo info = (ItemInfo) dragInfo;
            spanX = info.spanX;
            spanY = info.spanY;
        }
        spanX = Math.max(1, spanX);
        spanY = Math.max(1, spanY);

        int width = cellLayout.getItemWidthForSpan(spanX);
        int height = cellLayout.getItemHeightForSpan(spanY);
        final Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(b);
        if (dragInfo instanceof Widget) {
            int heightOffset = 25;
            int widthOffset = (width / height) * 10;
            if (widthOffset <= 0) {
                widthOffset = 5;
            }
            final Paint paint = UiConstants.TEMP_PAINT;
            paint.setStrokeWidth(1);
            final int radius = Utilities.dip2px(mContext, 3);
            final RectF rf = new RectF(widthOffset, heightOffset, width - widthOffset, height - heightOffset);
            canvas.save();
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setColor(0x80ffffff);
            canvas.drawRoundRect(rf, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xffffffff);
            canvas.drawRoundRect(rf, radius, radius, paint);
            canvas.restore();
            paint.reset();
        } else {
            drawDragView(v, padding, canvas, true, cellLayout);
        }
        //mOutlineHelper.createOutlineWithBlur(b, canvas, outlineColor, outlineColor);
        return b;
    }

    /**
     * Draw the View v into the given Canvas.
     */
    private void drawDragView(View v, int padding, Canvas destCanvas, boolean pruneToDrawable, CellLayout cellLayout) {
        final Rect clipRect = mTempRect;
        v.getDrawingRect(clipRect);
        v.setPressed(false);
        destCanvas.save();
        if (LOGD_ENABLED) {
            XLog.d(TAG, "v  == " + v);
        }
        if (v instanceof AppIcon) {
            final IconView tv = (IconView) ((AppIcon) v).getMainView();
            Drawable d = tv.getIcon();
            clipRect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            int hpadding = (cellLayout.getCellWidth() - d.getIntrinsicWidth()) / 2;
            int vpadding = tv.getPaddingTop();
            destCanvas.translate(hpadding, vpadding);
            destCanvas.clipRect(clipRect, Op.REPLACE);
            d.draw(destCanvas);
        } else {
            destCanvas.translate(-v.getScrollX(), -v.getScrollY());
            if (v.getLayoutParams() instanceof CellLayout.LayoutParams) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
                destCanvas.translate(lp.leftMargin, lp.topMargin);
            }
            v.draw(destCanvas);
        }
        // Restore text visibility of FolderIcon if necessary
        destCanvas.restore();
    }

    public void onDragEnter(CellLayout.CellInfo mDragInfo, CellLayout cellLayout, DropTarget.DragObject dragObject, int[] viewLocation) {
        if (mShadowDragView != null) {
            int spanX = 1;
            int spanY = 1;

            if (mDragInfo != null) {
                spanX = mDragInfo.spanX;
                spanY = mDragInfo.spanY;
            } else if (dragObject.dragInfo instanceof Widget) {
                Widget info = (Widget) dragObject.dragInfo;
                spanX = info.getSpanX();
                spanY = info.getSpanY();
            }
            spanX = Math.max(1, spanX);
            spanY = Math.max(1, spanY);

            final View child = (mDragInfo == null) ? null : mDragInfo.getCell();
            calculateShadowPosition(cellLayout, viewLocation[0], viewLocation[1], spanX, spanY, mTempPosition);
            boolean shadowDragviewVisible = !((WorkspaceCellLayout) cellLayout).isCurrentLocationOccupied(viewLocation[0], viewLocation[1], spanX, spanY, child);
            mShadowDragView.show(mWorkspace.getWindowToken(), mTempPosition[0], mTempPosition[1]);
            //add by ssy:第一次强制不显示好了，这个时候，位置可能不对（指下面的widget往上拖，其它拖拽未试，看QA反馈）
            shadowDragviewVisible = false;
            if (shadowDragviewVisible) {
                mShadowDragView.setVisibility(View.VISIBLE);
            } else {
                mShadowDragView.setVisibility(View.GONE);
            }
            if (mMoveShadowViewRunnable != null) {
                mMoveShadowViewRunnable.tarX = mTempPosition[0];
                mMoveShadowViewRunnable.tarY = mTempPosition[1];
            }
        }
    }

    public void onDragEnd() {
        removeShadow();
    }

    public void setVisibleShadowDragView(int visible) {
        if (mShadowDragView != null) {
            mShadowDragView.setVisibility(visible);
        }
    }

    private void removeShadow() {
        if (mShadowDragView != null) {
            mShadowDragView.setVisibility(View.GONE);
            mShadowDragView.remove();
            mShadowDragView = null;

            BitmapUtils.recycleBitmap(mDragOutline);
        }
    }

    private Interpolator mInterpolator;

    private MoveShadowView mMoveShadowViewRunnable;

    public void moveShadowDragView(CellLayout cellLayout, int cellX, int cellY, int spanX, int spanY) {
        if (mShadowDragView == null) {
            return;
        }

        calculateShadowPosition(cellLayout, cellX, cellY, spanX, spanY, mTempPosition);
        
        if (mShadowDragView.getVisibility() == View.GONE) {
            mShadowDragView.setVisibility(View.VISIBLE);
            mShadowDragView.setLayoutPosition(mTempPosition[0], mTempPosition[1]);
            return;
        }

        //mShadowDragView.move(mTempPosition[0], mTempPosition[1]);
        if (mInterpolator != null) {
            mInterpolator = new AccelerateDecelerateInterpolator();
        }
        mShadowDragView.setInterpolator(mInterpolator);
        mShadowDragView.setDuration(200);

        if (mMoveShadowViewRunnable == null) {
            mMoveShadowViewRunnable = new MoveShadowView();
        }
        mMoveShadowViewRunnable.launch(mTempPosition[0], mTempPosition[1]);
    }

    public DragView getShadowDragView(){
        return mShadowDragView;
    }

    private void calculateShadowPosition(CellLayout cellLayout, int cellX, int cellY, int spanX, int spanY, int[] out) {
        cellLayout.cellToPoint(cellX, cellY, spanX, spanY, out);
        int tarX = out[0];
        int tarY = out[1];
        cellLayout.getLocationOnScreen(out);
        out[0] = tarX;
        out[1] += tarY;
        mWorkspace.adjustPosition(out);
    }

    private class MoveShadowView implements Runnable {

        int tarX, tarY;

        public void launch(int x, int y) {
            if (tarX == x && tarY == y) {
                return;
            }

            tarX = x; tarY = y;

            mShadowDragView.removeCallbacks(this);
            mShadowDragView.postDelayed(this, 70);
        }

        @Override
        public void run() {
            if (mShadowDragView == null) {
                return;
            }
            
            mShadowDragView.animateTo(tarX, tarY, null);
        }

    }
}
