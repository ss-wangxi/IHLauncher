package cc.snser.launcher.ui.view;

import android.view.View;

/**
 * View 动画监听器，在{@link View#onAnimationEnd(View)}里调用
 *
 * */
public interface ViewAnimationListener {
    void onAnimationEnd(View v);
}
