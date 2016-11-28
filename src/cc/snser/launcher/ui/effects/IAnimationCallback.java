package cc.snser.launcher.ui.effects;

import android.view.View;

public interface IAnimationCallback {
    void onTransformation(float interpolatedTime, float dx, float dy);
    void onComplete(View view);
}
