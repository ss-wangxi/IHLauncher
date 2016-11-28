
package cc.snser.launcher.ui.effects;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PaintFlagsDrawFilter;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.Scroller;
import cc.snser.launcher.LauncherSettings;

/**
 * <p>
 * Represents the infos for the effect of the workspace.
 * </p>
 *
 * @author huangninghai
 * @version 1.0
 */
public abstract class EffectInfo {
    public interface Callback {
        public boolean onEffectApplied(Canvas canvas, View child, long drawingTime);
        public boolean onApplyAlpha(View child, float alpha);
    }

    public final int type;

    public final String key;

    public final String title;

    protected Interpolator interpolator;

    public EffectInfo(int type, String key, String title) {
        this.type = type;
        this.key = key;
        this.title = title;
    }

    @Override
    public String toString() {
        return "key: " + key + "  title: " + title;
    }

    protected boolean apply(Canvas canvas, View child, long drawingTime, PaintFlagsDrawFilter antiAliesFilter, Matrix matrix, float alpha, Callback callback) {

        if (LauncherSettings.getRenderPerformanceMode(child.getContext()) == LauncherSettings.RENDER_PERFORMANCE_MODE_PREFER_QUALITY) {
            canvas.setDrawFilter(antiAliesFilter);
        }

        final boolean concatMatrix = matrix != null && !matrix.isIdentity();
        if (concatMatrix) {
            canvas.save();
            canvas.translate(child.getLeft(), child.getTop());
            canvas.concat(matrix);
            canvas.translate(-child.getLeft(), -child.getTop());
        }

        boolean alphaApplied = false;
        callback.onApplyAlpha(child, 1f);
        if (alpha < 1.0F) {
            if (callback.onApplyAlpha(child, alpha)) {
                alphaApplied = true;
            } else {
                final int cl = child.getLeft();
                final int ct = child.getTop();
                final int cr = child.getRight();
                final int cb = child.getBottom();
                canvas.saveLayerAlpha(cl, ct, cr, cb, (int) (255 * alpha), Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.CLIP_TO_LAYER_SAVE_FLAG);
            }
        }

        boolean ret = callback.onEffectApplied(canvas, child, drawingTime);

        if (alpha < 1.0F && !alphaApplied) {
            canvas.restore();
        }

        if (concatMatrix) {
            canvas.restore();
        }

        return ret;
    }

    public abstract boolean canEnableWholePageDrawingCache();

    public abstract boolean isWorkspaceNeedAntiAlias();

    public abstract boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation,
            float radio, int offset, int currentScreen, boolean isPortrait);

    public abstract boolean isCellLayoutNeedAntiAlias();

    public abstract Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime,
            Callback callback, float radioX, int offset, float radioY, int currentScreen, boolean isPortrait);

    public abstract boolean needInvalidateHardwareAccelerated();

    /**
     * 4.0机器上一些耗时特效暂时不能用{@link FixFPSScroller}}，而是用默认的{@link Scroller}，下一版优化这里
     * @return
     */
    public boolean useDefaultScroller() {
        return false;
    }

    public boolean drawChildrenOrderByMoveDirection() {
        return false;
    }

    public void onTouchDown(boolean isScrolling) {

    }

    public void onTouchUpCancel(boolean isScrolling) {

    }

    public void onEffectCheckChanged(boolean checked, EffectInfo otherEffectInfo) {

    }

    public void onEffectEnd(int screenCount, int previousPage, int currentPage) {

    }

    public void setInterpolator(Interpolator interpolator) {
        this.interpolator = interpolator;
    }

    /**
     * 特效是否会将两个不同的屏叠加
     * */
    public boolean isScreenFused() {
        return false;
    }

    /**
     * 用于随机特效选中之后
     * */
    public void onRefresh() {
    }

    /**
     * 是否需要边框
     * */
    public boolean hasScreenDecor() {
        return false;
    }

    /**
     * 是否需要边框
     * */
    public float getScreenDecorAlpha() {
        return 0;
    }

    public int getAlphaTransitionType() {
        return 0;
    }
}
