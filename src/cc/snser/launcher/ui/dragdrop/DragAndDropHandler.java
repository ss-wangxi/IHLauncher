package cc.snser.launcher.ui.dragdrop;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;
import cc.snser.launcher.Utils;

import com.btime.launcher.util.XLog;

import static cc.snser.launcher.Constant.LOGD_ENABLED;
import static cc.snser.launcher.Constant.LOGE_ENABLED;

/**
 * 用于处理拖放的操作类，展示挤压的动画效果
 * 只针对于使用DragSource和DropTarget实现拖放的业务
 * 注意在DragController中注册DropTarget
 * 参数中的x和y都是相对于父容器的坐标
 *
 * <B>
 * 请注意，在refreshLayout的实现中，要执行每个view的clearAnimation方法，来确保刷新的界面能够正常显示
 * </B>
 * @author zhangjing
 *
 */
public abstract class DragAndDropHandler {

    private static final int MSG_MOVE_VIEWS = 1;

    private static final String DRAG_TARGET_LOCATION_KEY = "target_location";

    private int mDragViewSrc = -1;

    private int mDragViewCurrentPositionInParent;

    private int mDragViewTracePosition;

    private Integer mUnDeliveredTarget;

    private long mIntervalWaitToMove = 200;

    private long mMoveAnimationDuration = 200;

    private static final String TAG = "Launcher.dragAndDropHandler";

    private boolean isDraggingFromOther() {
        return mDragViewSrc == getLastDraggableViewIndex() + 1;
    }

    private Handler mHandler =  new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_MOVE_VIEWS:
                    int target = msg.getData().getInt(DRAG_TARGET_LOCATION_KEY);
                    if (mDragViewTracePosition == target || mDragViewTracePosition == -1) {
                        mUnDeliveredTarget = null;

                        handleMoveEvent(mDragViewCurrentPositionInParent,
                                target);

                        mDragViewCurrentPositionInParent = target;
                    }

                    break;
            }
        }
    };

    /**
     * 处理拖动事件
     * @param dragSourceIndex 拖动屏拖动前所在位置
     * @param dragTargetIndex 拖动屏拖动目标所在的位置
     */
    protected void handleMoveEvent(int dragSourceIndex, int dragTargetIndex) {
        //若dragSourceIndex未初始化，则当作插入操作来移动其他view
        if (dragSourceIndex < dragTargetIndex) {
            // from top to bottom
            for (int i = dragSourceIndex; i < dragTargetIndex; i++) {
                animationMoveView(i + 1, i);
                if (LOGD_ENABLED) {
                    XLog.d(TAG, "move from " + (i + 1) + " to " + i);
                }
            }
        } else if (dragSourceIndex > dragTargetIndex) {
            // from bottom to top
            for (int i = dragSourceIndex; i > dragTargetIndex; i--) {
                animationMoveView(i - 1, i);
                if (LOGD_ENABLED) {
                    XLog.d(TAG, "move from " + (i - 1) + " to " + i);
                }
            }
        }

    }

    protected void animationMoveView(int fromLocation, int toLocation) {

        int screenViewId = fromLocation;
        int[] fromCoordinate = new int[2];
        int[] toCoordinate = new int[2];
        if (fromLocation >= Math.min(mDragViewCurrentPositionInParent, mDragViewSrc) && fromLocation <= Math.max(mDragViewCurrentPositionInParent, mDragViewSrc)) {
            if (mDragViewCurrentPositionInParent > mDragViewSrc) {
                screenViewId = fromLocation + 1;
            } else if (mDragViewCurrentPositionInParent < mDragViewSrc) {
                screenViewId = fromLocation - 1;
            }
        }
        if (LOGD_ENABLED) {
            XLog.d(TAG, "screenViewId  = " + screenViewId);
            XLog.d(TAG, "fromLocation  = " + fromLocation + " toLocation = " + toLocation);
        }
        int[] gridViewScreenPosition = getPositionIgnorScroll(screenViewId);
        int[] fromPosition = getPositionIgnorScroll(fromLocation);
        int[] toPosition = getPositionIgnorScroll(toLocation);

        fromCoordinate[0] = fromPosition[0] - gridViewScreenPosition[0];
        fromCoordinate[1] = fromPosition[1] - gridViewScreenPosition[1];
        toCoordinate[0] = toPosition[0] - gridViewScreenPosition[0];
        toCoordinate[1] = toPosition[1] - gridViewScreenPosition[1];

        View sourceView = getChildViewAt(screenViewId);
        if (LOGD_ENABLED) {
            XLog.d(TAG, "sourceView  == " + sourceView);
        }
        animationMoveView(sourceView, fromCoordinate, toCoordinate, false);
    }

    public void animationMoveView(final int x, final int y, final int xOffset, int yOffset, final int sourceIndex, int targetIndex, final boolean refresh) {
        View view = getChildViewAt(sourceIndex);
        view.setVisibility(View.VISIBLE);
        int[] srcPosition = getPositionIgnorScroll(sourceIndex);
        int[] targetPosition = getPositionIgnorScroll(targetIndex);
        animationMoveView(view, new int[]{x - srcPosition[0] - xOffset, y - srcPosition[1] - yOffset}, new int[]{targetPosition[0] - srcPosition[0], targetPosition[1] - srcPosition[1]}, true);
    }

    protected void animationMoveView(final View view, final int[] fromCoordinate, final int[] toCoordinate, final boolean refresh) {
        if (view == null) {
            return;
        }
        view.requestLayout();
        if (LOGD_ENABLED) {
            XLog.d(TAG, "move view:" + fromCoordinate[0] + "," + toCoordinate[0] + "," +  fromCoordinate[1] + "," + toCoordinate[1]);
        }
        TranslateAnimation animTrans = new TranslateAnimation(
                fromCoordinate[0], toCoordinate[0], fromCoordinate[1],
                toCoordinate[1]);
        //保持动画的位置
        animTrans.setFillAfter(true);
        animTrans.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }
            public void onAnimationRepeat(Animation animation) {
            }
            public void onAnimationEnd(Animation animation) {
                //为了保持动画的位置，取消下列代码.请在刷新界面时执行clearAnimation()
//                view.clearAnimation();
//                invalidate();
                if (refresh) {
                    view.post(new Runnable() {
                        public void run() {
                            refreshLayoutAndClearAnimation();
                        }
                    });
                }
            }
        });
        animTrans.setDuration(mMoveAnimationDuration);
        view.startAnimation(animTrans);
        if (refresh) {
            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                ((View) parent).invalidate();
            }
        }
    }

    protected void sendMoveTargetMessage(int targetId) {
        mHandler.removeMessages(MSG_MOVE_VIEWS);
        Bundle data = new Bundle();
        data.putInt(DRAG_TARGET_LOCATION_KEY, targetId);
        mDragViewTracePosition = targetId;
        Message message = Utils.createMessage(mHandler, MSG_MOVE_VIEWS, data, null);
        mHandler.sendMessageDelayed(message, mIntervalWaitToMove);
    }

    public boolean isDragging() {
        return  mDragViewSrc <= getLastDraggableViewIndex() + 1 && mDragViewSrc >= 0;
    }

    public void updateRefreshedViewPositionInDragging(View view, int index) {
        if (index == mDragViewSrc) {
            view.setVisibility(View.GONE);
            return;
        }
        /*else {
            view.setVisibility(View.VISIBLE);
        }*/
        if (!isDragging()) {
            return;
        }
        view.requestLayout();
        view.clearAnimation();
        int movedTarget = index;
        //正在拖动过程中
        if (mDragViewCurrentPositionInParent >= index && mDragViewSrc < index) {
            movedTarget--;
        } else if (mDragViewCurrentPositionInParent <= index && mDragViewSrc > index) {
            movedTarget++;
        } else {

            return;
        }

        if (LOGD_ENABLED) {
            if (view instanceof TextView) {
                XLog.i(TAG, "updateRefreshedViewPositionInDragging!!!  move " + ((TextView) view).getText() + " from " + index + " to " + movedTarget);
            }
        }

        int[] fromPosition = getPositionIgnorScroll(index);
        int[] toPosition = getPositionIgnorScroll(movedTarget);
        TranslateAnimation animTrans = new TranslateAnimation(
                0, toPosition[0] - fromPosition[0], 0, toPosition[1] - fromPosition[1]);
        //保持动画的位置
        animTrans.setFillAfter(true);
        animTrans.setDuration(0);
        view.startAnimation(animTrans);
    }

    public void handleOnDragEnterFromOtherContainer() {
        setStartDraggingSource(getLastDraggableViewIndex() + 1, false);
    }

    public void setStartDraggingSource(int index, boolean validateIndex) {
        if (validateIndex && (index > getLastDraggableViewIndex() || index < 0)) {
            if (LOGE_ENABLED) {
                XLog.e(TAG, "dragging source is " + index + " out of bound! reset to 0");
            }
            index = 0;
        } else {
            if (LOGD_ENABLED) {
                XLog.d(TAG, "drag source:" + index);
            }
        }
        mDragViewSrc = index;
        mDragViewCurrentPositionInParent = mDragViewSrc;
        mDragViewTracePosition = mDragViewSrc;
    }

    public void handleOnDragOver(int x, int y) {
        Integer targetId = getTargetIndex(x, y);
        sendMoveMsg(targetId);
    }

    private void sendMoveMsg(Integer targetId) {
        if (targetId != null) {
            if (mUnDeliveredTarget == null || !mUnDeliveredTarget.equals(targetId)) {
                if (targetId.equals(mDragViewCurrentPositionInParent)) {
                    return;
                }
                mUnDeliveredTarget = targetId;
                sendMoveTargetMessage(targetId);
            }
        }
    }

    public void resetMovedView() {
        sendMoveTargetMessage(mDragViewSrc);
    }

    public boolean handleOnDrop(int x, int y, int xOffset, int yOffset, View dragSourceView) {
        if (mDragViewSrc < 0) {
            if (LOGD_ENABLED) {
                XLog.d(TAG, "handle on drop but src is invalid; probably because of fast drag and drop make the omit invoke of onDragEnter");
            }
            int index = getIndexOfDragView(dragSourceView);
            setStartDraggingSource(index, true);
        }
        Integer targetScreenId = getTargetIndex(x, y);
        boolean positionChanged = mDragViewSrc != targetScreenId;
        View view = getChildViewAt(mDragViewSrc);
        int[] srcPosition = getPosition(mDragViewSrc);
        int[] targetPosition = getPosition(targetScreenId);
        if (LOGD_ENABLED) {
            XLog.d(TAG, "move " + mDragViewSrc + " to " + targetScreenId);
            XLog.d(TAG, "the moved view = " + view);
            XLog.d(TAG, "srcPosition[0] = " + srcPosition[0] + "srcPosition[1] = " + srcPosition[1]);
            XLog.d(TAG, "targetPosition[0] = " + targetPosition[0] + "targetPosition[1] = " + targetPosition[1]);
            XLog.d(TAG, "x = " + x + " y = " + y);
            XLog.d(TAG, "xOffset= " + xOffset + " yOffset = " + yOffset);
        }
        executeMoveTo(targetScreenId);
        if (mDragViewCurrentPositionInParent != targetScreenId) {
            //移动其余的布局
            handleMoveEvent(mDragViewCurrentPositionInParent, targetScreenId);
        }
        animationMoveView(view, new int[]{x - srcPosition[0] - xOffset, y - srcPosition[1] - yOffset}, new int[]{targetPosition[0] - srcPosition[0], targetPosition[1] - srcPosition[1]}, true);
        mHandler.removeMessages(MSG_MOVE_VIEWS);
        mDragViewSrc = -1;
        mDragViewTracePosition = -1;
        return positionChanged;
    }

    private void executeMoveTo(int target) {
        updateMoveModel(mDragViewSrc, target);
    }

    public void handleOnDropCompleted() {
        mHandler.removeMessages(MSG_MOVE_VIEWS);
        mDragViewSrc = -1;
        mDragViewTracePosition = -1;
    }

    public void stopAnimation() {
        mHandler.removeMessages(MSG_MOVE_VIEWS);
    }

    public Integer getTargetIndex(int x, int y) {
        Integer targetScreenId = getIndexOfPosition(x, y);
        if (targetScreenId == null) {
            targetScreenId = mDragViewCurrentPositionInParent;
        }
        if (!isValidAndDraggableView(targetScreenId)) {
            if (isDraggingFromOther()) {
                targetScreenId = getLastDraggableViewIndex() + 1;
            } else {
                targetScreenId = getLastDraggableViewIndex();
            }
        }
        return targetScreenId;
    }
    /**
     * 返回view对应于父容器的精确坐标地址
     * @param index
     * @return
     */
    public abstract int[] getPosition(int index);

    public abstract int[] getPositionIgnorScroll(int index);
    /**
     * 返回坐标所对应的元素id
     * @param x
     * @param y
     * @return
     */
    public abstract Integer getIndexOfPosition(int x, int y);

    public abstract View getChildViewAt(int index);

    public abstract int getIndexOfDragView(View view);

    public abstract void updateMoveModel(int sourceIndex, int targetIndex);

    public abstract boolean isValidAndDraggableView(int index);

    public abstract int getLastDraggableViewIndex();

    public abstract void refreshLayoutAndClearAnimation();

    public void setIntervalWaitToMove(long intervalWaitToMove) {
        mIntervalWaitToMove = intervalWaitToMove;
    }

    public void setMoveAnimationDuration(long moveAnimationDuration) {
        mMoveAnimationDuration = moveAnimationDuration;
    }

    public int getDragViewCurrentPositionInParent() {
        return mDragViewCurrentPositionInParent;
    }

}
