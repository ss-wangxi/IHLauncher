package cc.snser.launcher.ui.effects;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Transformation;

import com.btime.launcher.util.XLog;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

public abstract class EffectInfoWithWidgetCache extends EffectInfo {

    protected static SparseArray<Bitmap> mCaches = null;
    protected static SparseBooleanArray mRedrawCache = null;
    protected static SparseIntArray mWidgetScreen = null;

    protected boolean mIsUsingWidgetCache = false;
    protected Bitmap cacheBitmap = null;
    protected Paint mCachePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public EffectInfoWithWidgetCache(int type, String key, String title) {
        super(type, key, title);
    }

    public static void destroyAllCaches() {
        if (LOGD_ENABLED) {
            XLog.v("Launcher.EffectInfoWithWidgetCache.onEffectEnd", "destroyAllCaches");
        }

        if (mCaches != null) {
            for (int i = 0; i < mCaches.size(); i++) {
                Bitmap bmp = mCaches.valueAt(i);
                if (bmp != null && !bmp.isRecycled()) {
                    bmp.recycle();
                }
            }
            mCaches.clear();
        }

        if (mRedrawCache != null) {
            mRedrawCache.clear();
        }

        if (mWidgetScreen != null) {
            mWidgetScreen.clear();
        }
    }

    protected Bitmap getWidgetBitmap(View view, int screen, int w, int h, int x, int y) {
        if (w == 0 || h == 0) {
            return null;
        }
        if (mCaches == null) {
            mCaches = new SparseArray<Bitmap>();
        }
        Bitmap bmp = mCaches.get(view.hashCode());
        if (bmp == null) {
            bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
            mCaches.put(view.hashCode(), bmp);

            if (mWidgetScreen == null) {
                mWidgetScreen = new SparseIntArray();
            }
            mWidgetScreen.put(view.hashCode(), screen);
        }

        if (needRedrawCache(view)) {
            bmp.eraseColor(Color.TRANSPARENT);
            view.draw(new Canvas(bmp));
            setRedrawCache(view, false);
        }

        return bmp;
    }

    protected boolean needRedrawCache(View view) {
        if (mRedrawCache == null) {
            mRedrawCache = new SparseBooleanArray();
        }
        return mRedrawCache.get(view.hashCode(), true);
    }

    protected void setRedrawCache(View view, boolean value) {
        if (mRedrawCache == null) {
            mRedrawCache = new SparseBooleanArray();
        }
        mRedrawCache.put(view.hashCode(), value);
    }

    public boolean isUsingWidgetCache() {
        return mIsUsingWidgetCache;
    }

    @Override
    public void onEffectCheckChanged(boolean checked, EffectInfo otherEffectInfo) {
        if (!checked && (otherEffectInfo == null || !(otherEffectInfo instanceof EffectInfoWithWidgetCache && ((EffectInfoWithWidgetCache)otherEffectInfo).isUsingWidgetCache()))) {
            if (mCaches != null) {
                for (int i = 0; i < mCaches.size(); i++) {
                    Bitmap bmp = mCaches.valueAt(i);
                    if (bmp != null && !bmp.isRecycled()) {
                        bmp.recycle();
                    }
                }
                mCaches.clear();
                mCaches = null;
            }
            if (mWidgetScreen != null) {
                mWidgetScreen.clear();
            }
        }
    }

    @Override
    public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
        float childMeasuredWidth = childView.getMeasuredWidth();
        float childMeasuredHeight = childView.getMeasuredHeight();

        Matrix matrix = childTransformation.getMatrix();

        if (isPortrait) {
            matrix.postTranslate(childMeasuredWidth * radio + offset, 0.0F);
        } else {
            matrix.postTranslate(0.0F, childMeasuredHeight * radio + offset);
        }

        childTransformation.setTransformationType(Transformation.TYPE_MATRIX);

        return true;
    }

    @Override
    public void onTouchDown(boolean isScrolling) {
        if (mRedrawCache != null) {
            mRedrawCache.clear();
        }
    }

    @Override
    public void onTouchUpCancel(boolean isScrolling) {

    }

    @Override
    public void onEffectEnd(int screenCount, int previousPage, int currentPage) {
        if (mWidgetScreen == null || mCaches == null || screenCount <= 0) {
            return;
        }

        int nextScreen = (currentPage + 1) % screenCount;
        previousPage = (currentPage - 1 +  screenCount) % screenCount;

        if (LOGD_ENABLED) {
            XLog.v("Launcher.EffectInfoWithWidgetCache.onEffectEnd", "currentScreen: " + currentPage + "   preScreen: " + previousPage + "   nextScreen: " + nextScreen);
        }

        for (int i = 0; i < mWidgetScreen.size(); i++) {
            int screen = mWidgetScreen.valueAt(i);
            if (screen != currentPage && screen != previousPage && screen != nextScreen) {
                Bitmap bmp = mCaches.get(mWidgetScreen.keyAt(i));
                if (bmp != null && !bmp.isRecycled()) {
                    bmp.recycle();
                    mCaches.remove(mWidgetScreen.keyAt(i));

                    if (LOGD_ENABLED) {
                        XLog.v("Launcher.EffectInfoWithWidgetCache.onEffectEnd", "removed: " + screen);
                    }
                }
            }
        }
    }
}