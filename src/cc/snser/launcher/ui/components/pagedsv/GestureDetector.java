
package cc.snser.launcher.ui.components.pagedsv;

import com.btime.launcher.util.XLog;

import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

/**
 * 分页滚动手势识别
 *
 * @author songzhaochun
 */
public class GestureDetector {

    public static final String TAG = "Launcher.GestureDetector";

    public static final int FLING_NONE = 0;

    public static final int FLING_LEFT = 1;

    public static final int FLING_RIGHT = 2;

    public static final int FLING_UP = 3;

    public static final int FLING_DOWN = 4;

    private static final boolean LOGD_ENABLED = false;

    /**
     * The velocity at which a fling gesture will cause us to snap to the next
     * screen
     */
    private static final int SNAP_VELOCITY = 200;

    private final int mMaximumVelocity;

    private final OnGestureListener mGestureListener;

    private VelocityTracker mVelocityTracker = null;

    private boolean mEnable = true;

    public GestureDetector(ViewConfiguration configuration, OnGestureListener listener) {
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mGestureListener = listener;
    }

    public boolean isEnable() {
        return mEnable;
    }

    public void setEnable(boolean enable) {
        this.mEnable = enable;
    }

    public void forwardTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        // 第二套方案（修正方案）
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            resetSimple();
        }
        addSimpleEvent(ev);

        if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (mGestureListener != null) {
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityX = (int) mVelocityTracker.getXVelocity();
                int velocityY = (int) mVelocityTracker.getYVelocity();

                // 第二套方案（修正方案）
                if (isSimpleHit()) {
                    if (LOGD_ENABLED) {
                        XLog.d(TAG, "hit");
                    }
                    evalSimple(1000, mMaximumVelocity);
                    final int v2x = getSimpleX();
                    final int v2y = getSimpleY();
                    // 如果x方向不一样，则修正
                    if ((velocityX < 0 && v2x > 0) || (velocityX > 0 && v2x < 0)) {
                        // 日志
                        if (LOGD_ENABLED) {
                            XLog.e(TAG, "should change, vx=" + v2x + " vy=" + v2y + " use.x=" + velocityX + " use.y=" + velocityY);
                        }
                        // 变更
                        velocityX = v2x;
                        velocityY = v2y;
                    } else {
                        if (LOGD_ENABLED) {
                            XLog.d(TAG, "ok, vx=" + v2x + " vy=" + v2y + " use.x=" + velocityX + " use.y=" + velocityY);
                        }
                    }
                } else {
                    if (LOGD_ENABLED) {
                        XLog.d(TAG, "hit none");
                    }
                }

                int directionX = FLING_NONE;
                int directionY = FLING_NONE;

                if (velocityX > SNAP_VELOCITY) {
                    directionX = FLING_LEFT;
                } else if (velocityX < -SNAP_VELOCITY) {
                    directionX = FLING_RIGHT;
                }

                if (velocityY > SNAP_VELOCITY) {
                    directionY = FLING_DOWN;
                } else if (velocityY < -SNAP_VELOCITY) {
                    directionY = FLING_UP;
                }

                mGestureListener.onFling(directionX, directionY, velocityX, velocityY);
            }

            // 第二套方案（修正方案）
            resetSimple();
        }
    }

    public void forwardSecondeTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        mVelocityTracker.addMovement(ev);

        if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP) {
            if (mGestureListener != null) {
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int id = -1;
                try {
                    id = ev.getPointerId(ev.getActionIndex());
                } catch (Throwable e) {
                    // fix reported bug, weird bug
                }
                if (id < 0) {
                    return;
                }

                final int velocityX = (int) mVelocityTracker.getXVelocity(id);
                final int velocityY = (int) mVelocityTracker.getYVelocity(id);

                int directionX = FLING_NONE;
                int directionY = FLING_NONE;

                if (velocityX > SNAP_VELOCITY) {
                    directionX = FLING_LEFT;
                } else if (velocityX < -SNAP_VELOCITY) {
                    directionX = FLING_RIGHT;
                }

                if (velocityY > SNAP_VELOCITY) {
                    directionY = FLING_DOWN;
                } else if (velocityY < -SNAP_VELOCITY) {
                    directionY = FLING_UP;
                }

                mGestureListener.onFling(directionX, directionY, velocityX, velocityY);
            }
        }
    }

    public void release() {
        if (mVelocityTracker != null) {
            try {
                mVelocityTracker.recycle();
            } catch (Throwable e) {
                // ignore
            }
            mVelocityTracker = null;
        }

        // 第二套方案（修正方案）
        resetSimple();
    }

    // 如果VelocityTracker给出了明显错误的速度，则采用第二套方案（修正方案），直接根据最近的几次坐标算出速度
    private static final int SIMPLE_VELOCITY_BUFFER_SIZE = 20;
    private static final int SIMPLE_VELOCITY_CHECK_THRESHOLD = 16;
    private static final long SIMPLE_VELOCITY_CHECK_MILLS_SECONDS = 100; // 100 m s;
    float mSimpleVelocityPointX[] = new float[SIMPLE_VELOCITY_BUFFER_SIZE];
    float mSimpleVelocityPointY[] = new float[SIMPLE_VELOCITY_BUFFER_SIZE];
    long mSimpleVelocityTime[] = new long[SIMPLE_VELOCITY_BUFFER_SIZE];
    int mSimpleVelocityNext;
    int mSimpleVelocityCount;
    float mSimpleVelocityX;
    float mSimpleVelocityY;

    void resetSimple() {
        mSimpleVelocityNext = 0;
        mSimpleVelocityCount = 0;
    }

    void addSimpleEvent(MotionEvent ev) {
        int cur;
        mSimpleVelocityCount++;
        cur = mSimpleVelocityNext++;
        cur %= SIMPLE_VELOCITY_BUFFER_SIZE;
        mSimpleVelocityPointX[cur] = ev.getX();
        mSimpleVelocityPointY[cur] = ev.getY();
        mSimpleVelocityTime[cur] = ev.getEventTime();
        // 日志
        if (LOGD_ENABLED) {
            XLog.d(TAG, " cur=" + cur + " next=" + mSimpleVelocityNext + " x=" + ev.getX() + " y=" + ev.getY() + " t=" + ev.getEventTime());
        }
    }

    boolean isSimpleHit() {
       return mSimpleVelocityCount <= SIMPLE_VELOCITY_CHECK_THRESHOLD;
    }

    void evalSimple(int units, float maxVelocity) {
        // 数据不够计算
        if (mSimpleVelocityCount <= 1) {
            mSimpleVelocityX = 0;
            mSimpleVelocityY = 0;
            return;
        }
        // 最后一个坐标
        int idx = (mSimpleVelocityNext + SIMPLE_VELOCITY_BUFFER_SIZE - 1) % SIMPLE_VELOCITY_BUFFER_SIZE;
        final long lastTime = mSimpleVelocityTime[idx];
        final float lastX = mSimpleVelocityPointX[idx];
        final float lastY = mSimpleVelocityPointY[idx];
        // 找到最早的有意义的坐标
        long prevTime = lastTime;
        float prevX = lastX;
        float prevY = lastY;
        for (int i = 1; i < mSimpleVelocityCount && i < SIMPLE_VELOCITY_BUFFER_SIZE; i++) {
            idx = (mSimpleVelocityNext + SIMPLE_VELOCITY_BUFFER_SIZE - 1 - i) % SIMPLE_VELOCITY_BUFFER_SIZE;
            final long t = mSimpleVelocityTime[idx];
            prevTime = t;
            prevX = mSimpleVelocityPointX[idx];
            prevY = mSimpleVelocityPointY[idx];
            if (lastTime - t > SIMPLE_VELOCITY_CHECK_MILLS_SECONDS) {
                break;
            }
        }
        // 时间差无效，或者在时间内找不到有效点
        if (lastTime <= prevTime) {
            mSimpleVelocityX = 0;
            mSimpleVelocityY = 0;
            return;
        }
        // 计算有效起始到结束的速度
        final long deltaT = lastTime - prevTime;
        mSimpleVelocityX = (lastX - prevX) * units / deltaT;
        mSimpleVelocityX = mSimpleVelocityX > maxVelocity ? maxVelocity : mSimpleVelocityX;
        mSimpleVelocityX = mSimpleVelocityX < -maxVelocity ? -maxVelocity : mSimpleVelocityX;
        mSimpleVelocityY = (lastY - prevY) * units / deltaT;
        mSimpleVelocityY = mSimpleVelocityY > maxVelocity ? maxVelocity : mSimpleVelocityY;
        mSimpleVelocityY = mSimpleVelocityY > maxVelocity ? -maxVelocity : mSimpleVelocityY;
        // 日志
        if (LOGD_ENABLED) {
            XLog.d(TAG, "last=" + lastX + " prev=" + prevX + " dt=" + deltaT);
        }
    }

    int getSimpleX() {
        return (int) mSimpleVelocityX;
    }

    int getSimpleY() {
        return (int) mSimpleVelocityY;
    }

    public interface OnGestureListener {
        public void onFling(int directionX, int directionY, int velocityX, int velocityY);
    }
}
