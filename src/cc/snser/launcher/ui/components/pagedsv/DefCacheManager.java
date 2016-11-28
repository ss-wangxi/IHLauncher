package cc.snser.launcher.ui.components.pagedsv;

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import cc.snser.launcher.ui.components.pagedsv.PagedScrollView.CacheHandler;

/**
 * @author songzhaochun
 *
 */
public class DefCacheManager implements CacheHandler {
    private static final long CLEAR_CACHE_DELAY = 5000;

    private static final int MESSAGE_CLEAR_CACHE = 1;

    private ViewGroup mPagedView;

    private boolean mDestroyCache;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MESSAGE_CLEAR_CACHE:
                    doClearCache(false, mDestroyCache);
                    mDestroyCache = false;
                    break;
                default:
                    break;
            }
        }
    };

    public DefCacheManager(ViewGroup pagedView) {
        mPagedView = pagedView;
    }

    @Override
    public void clearCache(boolean immediately, boolean all, boolean destroyCache) {
        this.mHandler.removeMessages(MESSAGE_CLEAR_CACHE);
        mDestroyCache = false;

        if (immediately) {
            doClearCache(all, destroyCache);
        } else {
            this.mHandler.sendEmptyMessageDelayed(MESSAGE_CLEAR_CACHE, CLEAR_CACHE_DELAY);
            mDestroyCache = destroyCache;
        }
    }

    protected void doClearCache(boolean all, boolean destroyCache) {
        final int width = mPagedView.getMeasuredWidth();
        if (width <= 0) {
            return;
        }
        int page = mPagedView.getScrollX() / width;
        final int count = mPagedView.getChildCount();
        for (int i = 0; i < count; i++) {
            View childView = mPagedView.getChildAt(i);

            if (childView != null) {
                if (i == page && !all) {
                    enableChildViewCache(childView);
                } else {
                    disableChildViewCache(childView, destroyCache);
                }
            }
        }
    }

    protected void enableChildViewCache(View childView) {
        if (!childView.isDrawingCacheEnabled()) {
            childView.setDrawingCacheEnabled(true);
        }
    }

    protected void disableChildViewCache(View childView, boolean destroyCache) {
        if (childView.isDrawingCacheEnabled()) {
            childView.setDrawingCacheEnabled(false);
            if (destroyCache) {
                childView.destroyDrawingCache();
            }
        }
    }

    @Override
    public void enableCache(int childIndex) {
        this.mHandler.removeMessages(MESSAGE_CLEAR_CACHE);
        mDestroyCache = false;

        View childView = this.mPagedView.getChildAt(childIndex);

        if (childView != null) {
            enableChildViewCache(childView);
        }
    }

}
