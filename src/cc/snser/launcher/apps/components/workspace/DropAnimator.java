package cc.snser.launcher.apps.components.workspace;

import android.graphics.Rect;
import cc.snser.launcher.ui.dragdrop.DragView;

public class DropAnimator implements FloatView.TweenValueCallback, FloatView.TDInterpolator {

    private static final int DURATION = 250;

    private final int[] mTempPosition = new int[2];

    private Bezier bezier = new Bezier();

    private DragView mDragView;

    private Object mDragInfo;

    private final ICallback mCallback;

    public DropAnimator(ICallback cbk) {
        mCallback = cbk;
    }

    public void performDropIntoFolderAnimation(DragView dragView, Object dragInfo, Rect targetRect) {
        reset();
        if (dragInfo != null) {
            mDragView = dragView;
            mDragInfo = dragInfo;

            dragView.setDuration(DURATION);
            dragView.onDropAnimationStart();
            dragView.animateTo(targetRect, this);
        }
    }

    public void performDropAnimation(DragView dragView, Object dragInfo, Rect targetRect, float velocityX, float velocityY) {
        reset();
        mDragView = dragView;
        mDragInfo = dragInfo;
        final int[] pos = mTempPosition;
        dragView.getLocationOnScreen(pos);
        final float fromX = pos[0];
        final float fromY = pos[1];
        final int width = dragView.getMeasuredWidth();
        final int height = dragView.getMeasuredHeight();
        final float deltaX2 = targetRect.exactCenterX() - width / 2.0f - fromX;
        final float deltaY2 = targetRect.exactCenterY() - height / 2.0f - fromY;

        // 运动轨迹采用贝塞尔曲线
        bezier.setControlPoint(fromX + velocityX * 3 / 125, fromY + velocityY * 3 / 125); // 因子可以调整


        final int duration2 = (int) Math.min(Math.sqrt(deltaX2 * deltaX2 + deltaY2 * deltaY2), 250);
        mDragView.setDuration(duration2);
        mDragView.onDropAnimationStart();
        mDragView.animateTo(targetRect, this);
    }

    private void reset() {
    }

    public boolean isAnimating(Object info) {
        return mDragInfo == info;
    }

    @Override
    public void onValueChanged(float ratio) {
        if (mCallback != null) {
            mCallback.onValueChanged(ratio);
        }
    }

    @Override
    public void onAnimationFinished() {
        mDragInfo = null;
        if (mCallback != null) {
            mCallback.onFinished();
        }
    }

    /**
     * @author ?
     *
     */
    public interface ICallback {

        /**
         * @param value
         */
        void onValueChanged(float value);

        /**
         *
         */
        void onFinished();
    }

    @Override
    public void initialize(float fromX, float fromY, float toX, float toY) {
        bezier.setStartPoint(fromX, fromY);
        bezier.setEndPoint(toX, toY);
    }

    @Override
    public void getInterpolation(float[] pos, float ratio) {
        bezier.getCoordinate(ratio, pos);
    }

}
