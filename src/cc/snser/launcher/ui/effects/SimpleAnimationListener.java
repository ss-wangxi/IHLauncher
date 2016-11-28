package cc.snser.launcher.ui.effects;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

public class SimpleAnimationListener implements AnimationListener {
    private View mView;
    private IAnimationCallback mCallback;
    public SimpleAnimationListener(View view, IAnimationCallback callback) {
        mView = view;
        mCallback = callback;
    }
    @Override
    public void onAnimationStart(Animation arg0) {
    }
    @Override
    public void onAnimationRepeat(Animation arg0) {
    }
    @Override
    public void onAnimationEnd(Animation arg0) {
        if (mCallback != null) {
            mCallback.onComplete(mView);
        }
    }
}
