package cc.snser.launcher.util;

import java.util.HashSet;

import com.btime.launcher.util.XLog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.os.Build;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;

public class LauncherAnimUtils {
	static HashSet<Animator> sAnimators = new HashSet<Animator>();
    static Animator.AnimatorListener sEndAnimListener = new Animator.AnimatorListener() {
        public void onAnimationStart(Animator animation) {
        }

        public void onAnimationRepeat(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            sAnimators.remove(animation);
        }

        public void onAnimationCancel(Animator animation) {
            sAnimators.remove(animation);
        }
    };

    public static void cancelOnDestroyActivity(Animator a) {
        sAnimators.add(a);
        a.addListener(sEndAnimListener);
    }

    // Helper method. Assumes a draw is pending, and that if the animation's duration is 0
    // it should be cancelled
    public static void startAnimationAfterNextDraw(final Animator animator, final View view) {
    	if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return;
    	
        view.getViewTreeObserver().addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                private boolean mStarted = false;
                public void onDraw() {
                    if (mStarted) return;
                    mStarted = true;
                    // Use this as a signal that the animation was cancelled
                    if (animator.getDuration() == 0) {
                        return;
                    }
                    animator.start();

                    final ViewTreeObserver.OnDrawListener listener = this;
                    view.post(new Runnable() {
                            public void run() {
                                view.getViewTreeObserver().removeOnDrawListener(listener);
                            }
                        });
                }
            });
    }

    public static void onDestroyActivity() {
        HashSet<Animator> animators = new HashSet<Animator>(sAnimators);
        for (Animator a : animators) {
            if (a.isRunning()) {
                a.cancel();
            } else {
                sAnimators.remove(a);
            }
        }
    }

    public static AnimatorSet createAnimatorSet() {
        AnimatorSet anim = new AnimatorSet();
        cancelOnDestroyActivity(anim);
        return anim;
    }

    public static ValueAnimator ofFloat(View target, float... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setFloatValues(values);
        cancelOnDestroyActivity(anim);
        return anim;
    }

    public static ObjectAnimator ofFloat(View target, String propertyName, float... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setPropertyName(propertyName);
        anim.setFloatValues(values);
        cancelOnDestroyActivity(anim);
        new FirstFrameAnimatorHelper(anim, target);
        return anim;
    }

    public static ObjectAnimator ofPropertyValuesHolder(View target,
            PropertyValuesHolder... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setValues(values);
        cancelOnDestroyActivity(anim);
        new FirstFrameAnimatorHelper(anim, target);
        return anim;
    }

    public static ObjectAnimator ofPropertyValuesHolder(Object target,
            View view, PropertyValuesHolder... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setValues(values);
        cancelOnDestroyActivity(anim);
        new FirstFrameAnimatorHelper(anim, view);
        return anim;
    }
    
    public static class FirstFrameAnimatorHelper extends AnimatorListenerAdapter
    	implements ValueAnimator.AnimatorUpdateListener {
    	    private static final boolean DEBUG = false;
    	    private static final int MAX_DELAY = 1000;
    	    private static final int IDEAL_FRAME_DURATION = 16;
    	    private View mTarget;
    	    private long mStartFrame;
    	    private long mStartTime = -1;
    	    private boolean mHandlingOnAnimationUpdate;
    	    private boolean mAdjustedSecondFrameTime;

    	    private static ViewTreeObserver.OnDrawListener sGlobalDrawListener;
    	    private static long sGlobalFrameCounter;
    	    private static boolean sVisible;

    	    public FirstFrameAnimatorHelper(ValueAnimator animator, View target) {
    	        mTarget = target;
    	        animator.addUpdateListener(this);
    	    }

    	    public FirstFrameAnimatorHelper(ViewPropertyAnimator vpa, View target) {
    	        mTarget = target;
    	        vpa.setListener(this);
    	    }

    	    // only used for ViewPropertyAnimators
    	    public void onAnimationStart(Animator animation) {
    	        final ValueAnimator va = (ValueAnimator) animation;
    	        va.addUpdateListener(FirstFrameAnimatorHelper.this);
    	        onAnimationUpdate(va);
    	    }

    	    public static void setIsVisible(boolean visible) {
    	        sVisible = visible;
    	    }

    	    public static void initializeDrawListener(View view) {
    	        if (sGlobalDrawListener != null) {
    	            view.getViewTreeObserver().removeOnDrawListener(sGlobalDrawListener);
    	        }
    	        sGlobalDrawListener = new ViewTreeObserver.OnDrawListener() {
    	                private long mTime = System.currentTimeMillis();
    	                public void onDraw() {
    	                    sGlobalFrameCounter++;
    	                    if (DEBUG) {
    	                        long newTime = System.currentTimeMillis();
    	                        XLog.d("FirstFrameAnimatorHelper", "TICK " + (newTime - mTime));
    	                        mTime = newTime;
    	                    }
    	                }
    	            };
    	        view.getViewTreeObserver().addOnDrawListener(sGlobalDrawListener);
    	        sVisible = true;
    	    }

    	    public void onAnimationUpdate(final ValueAnimator animation) {
    	        final long currentTime = System.currentTimeMillis();
    	        if (mStartTime == -1) {
    	            mStartFrame = sGlobalFrameCounter;
    	            mStartTime = currentTime;
    	        }

    	        boolean isFinalFrame = Float.compare(1f, animation.getAnimatedFraction()) == 0;

    	        if (!mHandlingOnAnimationUpdate &&
    	            sVisible &&
    	            // If the current play time exceeds the duration, or the animated fraction is 1,
    	            // the animation will get finished, even if we call setCurrentPlayTime -- therefore
    	            // don't adjust the animation in that case
    	            animation.getCurrentPlayTime() < animation.getDuration() && !isFinalFrame) {
    	            mHandlingOnAnimationUpdate = true;
    	            long frameNum = sGlobalFrameCounter - mStartFrame;
    	            // If we haven't drawn our first frame, reset the time to t = 0
    	            // (give up after MAX_DELAY ms of waiting though - might happen, for example, if we
    	            // are no longer in the foreground and no frames are being rendered ever)
    	            if (frameNum == 0 && currentTime < mStartTime + MAX_DELAY) {
    	                // The first frame on animations doesn't always trigger an invalidate...
    	                // force an invalidate here to make sure the animation continues to advance
    	                mTarget.getRootView().invalidate();
    	                animation.setCurrentPlayTime(0);

    	            // For the second frame, if the first frame took more than 16ms,
    	            // adjust the start time and pretend it took only 16ms anyway. This
    	            // prevents a large jump in the animation due to an expensive first frame
    	            } else if (frameNum == 1 && currentTime < mStartTime + MAX_DELAY &&
    	                       !mAdjustedSecondFrameTime &&
    	                       currentTime > mStartTime + IDEAL_FRAME_DURATION) {
    	                animation.setCurrentPlayTime(IDEAL_FRAME_DURATION);
    	                mAdjustedSecondFrameTime = true;
    	            } else {
    	                if (frameNum > 1) {
    	                    mTarget.post(new Runnable() {
    	                            public void run() {
    	                                animation.removeUpdateListener(FirstFrameAnimatorHelper.this);
    	                            }
    	                        });
    	                }
    	                if (DEBUG) print(animation);
    	            }
    	            mHandlingOnAnimationUpdate = false;
    	        } else {
    	            if (DEBUG) print(animation);
    	        }
    	    }

    	    public void print(ValueAnimator animation) {
    	        float flatFraction = animation.getCurrentPlayTime() / (float) animation.getDuration();
    	        XLog.d("FirstFrameAnimatorHelper", sGlobalFrameCounter +
    	              "(" + (sGlobalFrameCounter - mStartFrame) + ") " + mTarget + " dirty? " +
    	              mTarget.isDirty() + " " + flatFraction + " " + this + " " + animation);
    	    }
    }
}
