package cc.snser.launcher.ui.effects;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PaintFlagsDrawFilter;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

public class EffectInfoProxy extends EffectInfo {

    private EffectInfo host;

    public EffectInfoProxy(int type, String key, String title) {
        super(type, key, title);
    }

    public void setEffectInfo(EffectInfo info) {
        this.host = info;
    }

    @Override
    public boolean canEnableWholePageDrawingCache() {
        return host == null ? true : host.canEnableWholePageDrawingCache();
    }

    @Override
    public boolean isWorkspaceNeedAntiAlias() {
        return host == null ? false : host.isWorkspaceNeedAntiAlias();
    }

    @Override
    public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
        return host == null ? false : host.getWorkspaceChildStaticTransformation(parentView, childView, childTransformation, radio, offset, currentScreen, isPortrait);
    }

    @Override
    public boolean isCellLayoutNeedAntiAlias() {
        return host == null ? false : host.isCellLayoutNeedAntiAlias();
    }

    @Override
    public Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime, Callback callback, float radioX, int offset, float radioY,
            int currentScreen, boolean isPortrait) {
        return host == null ? null : host.applyCellLayoutChildTransformation(parentView, canvas, childView, drawingTime, callback, radioX, offset, radioY, currentScreen, isPortrait);
    }

    @Override
    public boolean needInvalidateHardwareAccelerated() {
        return host == null ? false : host.needInvalidateHardwareAccelerated();
    }

    @Override
    protected boolean apply(Canvas canvas, View child, long drawingTime, PaintFlagsDrawFilter antiAliesFilter, Matrix matrix, float alpha, Callback callback) {
        return host == null ? super.apply(canvas, child, drawingTime, antiAliesFilter, matrix, alpha, callback) : host.apply(canvas, child, drawingTime, antiAliesFilter, matrix, alpha, callback);
    }

    @Override
    public boolean useDefaultScroller() {
        return host == null ? super.useDefaultScroller() : host.useDefaultScroller();
    }

    @Override
    public boolean drawChildrenOrderByMoveDirection() {
        return host == null ? super.drawChildrenOrderByMoveDirection() : host.drawChildrenOrderByMoveDirection();
    }

    @Override
    public void onTouchDown(boolean isScrolling) {
        if (host == null) {
            super.onTouchDown(isScrolling);
        } else {
            host.onTouchDown(isScrolling);
        }
    }

    @Override
    public void onTouchUpCancel(boolean isScrolling) {
        if (host == null) {
            super.onTouchUpCancel(isScrolling);
        } else {
            host.onTouchUpCancel(isScrolling);
        }
    }

    @Override
    public void onEffectCheckChanged(boolean checked, EffectInfo otherEffectInfo) {
        if (!checked) {
            host = null;
        }
    }

    @Override
    public void onEffectEnd(int screenCount, int previousPage, int currentPage) {
        if (host == null) {
            super.onEffectEnd(screenCount, previousPage, currentPage);
        } else {
            host.onEffectEnd(screenCount, previousPage, currentPage);
        }
    }

    @Override
    public void setInterpolator(Interpolator interpolator) {
        if (host == null) {
            super.setInterpolator(interpolator);
        } else {
            host.setInterpolator(interpolator);
        }
    }

    @Override
    public boolean isScreenFused() {
        return host == null ? super.isScreenFused() : host.isScreenFused();
    }

    @Override
    public void onRefresh() {
        if (host == null) {
            super.onRefresh();
        } else {
            host.onRefresh();
        }
    }


}
