package cc.snser.launcher.util;

import android.content.Intent;

import java.util.HashSet;
import java.util.Set;

/**
 * The interface to handle events just like activity life cycle.
 * <p>
 * Observer/Subject pattern (another name is Listener pattern).</p>
 *
 * @author GuoLin
 *
 */
public class LifecycleSubject {

    /** Observers would be invoked by notifications. */
    private final Set<Observer> observers = new HashSet<Observer>();

    /**
     * Construct a subject.
     */
    public LifecycleSubject() {
        super();
    }

    /**
     * Register a observer.
     * <p>
     * If observer is a instance of SelfRegisterObserver, the method
     * {@link SelfRegisterObserver#onSelfRegister(LifecycleSubject)} would be invoked.</p>
     * @param observers Observer to register
     * @see SelfRegisterObserver
     */
    public void register(Observer... observers) {
        for (Observer observer : observers) {
            if (observer instanceof SelfRegisterObserver) {
                ((SelfRegisterObserver) observer).onSelfRegister(this);
            }
            this.observers.add(observer);
        }
    }

    /**
     * Unregister a observer.
     * <p>
     * Once the observer unregistered, no notify would be sent to it.</p>
     * @param observer Observer to unregister
     */
    public void unregister(Observer observer) {
        observers.remove(observer);
    }

    /**
     * Notify the start event to observers which is {@link StopObserver}.
     * <p>
     * The method {@link StopObserver#onStart()} would be invoked</p>
     */
    public void notifyStart() {
        for (Observer observer : observers) {
            if (observer instanceof StopObserver) {
                ((StopObserver) observer).onStart();
            }
        }
    }

    /**
     * Notify the start event to observers which is {@link PauseObserver}.
     * <p>
     * The method {@link PauseObserver#onResume()} would be invoked</p>
     */
    public void notifyResume() {
        for (Observer observer : observers) {
            if (observer instanceof PauseObserver) {
                ((PauseObserver) observer).onResume();
            }
        }
    }

    /**
     * Notify the new intent event to observers which is {@link NewIntentObserver}.
     * <p>
     * The method {@link NewIntentObserver#onNewIntent()} would be invoked</p>
     */
    public void notifyNewIntent() {
        for (Observer observer : observers) {
            if (observer instanceof NewIntentObserver) {
                ((NewIntentObserver) observer).onNewIntent();
            }
        }
    }

    /**
     * Notify the start event to observers which is {@link PauseObserver}.
     * <p>
     * The method {@link PauseObserver#onPause()} would be invoked</p>
     */
    public void notifyPause() {
        for (Observer observer : observers) {
            if (observer instanceof PauseObserver) {
                ((PauseObserver) observer).onPause();
            }
        }
    }

    /**
     * Notify the start event to observers which is {@link StopObserver}.
     * <p>
     * The method {@link StopObserver#onStop()} would be invoked</p>
     */
    public void notifyStop() {
        for (Observer observer : observers) {
            if (observer instanceof StopObserver) {
                ((StopObserver) observer).onStop();
            }
        }
    }

    /**
     * Notify the start event to observers which is {@link DestroyObserver}.
     * <p>
     * The method {@link DestroyObserver#onDestroy()} would be invoked</p>
     */
    public void notifyDestroy() {
        for (Observer observer : observers) {
            if (observer instanceof DestroyObserver) {
                ((DestroyObserver) observer).onDestroy();
            }
        }
    }

    public void notifyOnActivityResult(int requestCode, int resultCode, Intent data) {
        for (Observer observer : observers) {
            if (observer instanceof OnActivityResultObserver) {
                ((OnActivityResultObserver) observer).onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    /**
     * The top level observer interface.
     * <p>
     * User should NEVER implements this interface.</p>
     *
     * @author GuoLin
     *
     */
    public static interface Observer {

    }

    /**
     * Implement this interface to get the instance of {@link LifecycleSubject} when observer be
     * registered.
     *
     * @author GuoLin
     *
     */
    public static interface SelfRegisterObserver extends Observer {

        /**
         * Fire on the instance being register.
         * @param subject
         * @see LifecycleSubject#register(Observer)
         */
        public void onSelfRegister(LifecycleSubject subject);

    }

    /**
     * Implement this interface to observe pause and resume events in the life
     * cycle of activity.
     *
     * @author GuoLin
     */
    public static interface PauseObserver extends Observer {

        /**
         * Fire on calling {@link LifecycleSubject#notifyResume()}.
         */
        public void onResume();

        /**
         * Fire on calling {@link LifecycleSubject#notifyPause()}.
         */
        public void onPause();

    }

    /**
     * Implement this interface to observe start and stop events in the life
     * cycle of activity.
     *
     * @author GuoLin
     */
    public static interface StopObserver extends Observer {

        /**
         * Fire on calling {@link LifecycleSubject#notifyStart()}.
         */
        public void onStart();

        /**
         * Fire on calling {@link LifecycleSubject#notifyStop()}.
         */
        public void onStop();

    }

    /**
     * Implement this interface to observe new intent events in the life
     * cycle of activity.
     *
     * @author GuoLin
     */
    public static interface NewIntentObserver extends Observer {

        /**
         * Fire on calling {@link LifecycleSubject#notifyNewIntent()}.
         */
        public void onNewIntent();

    }

    /**
     * Implement this interface to observe destroy event in the life cycle of activity.
     *
     * @author GuoLin
     *
     */
    public static interface DestroyObserver extends Observer {

        /**
         * Fire on calling {@link LifecycleSubject#notifyDestroy()}.
         */
        public void onDestroy();

    }

    public static interface OnActivityResultObserver extends Observer {
        public void onActivityResult(int requestCode, int resultCode, Intent data);

    }
}
