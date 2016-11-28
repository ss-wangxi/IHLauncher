package cc.snser.launcher.model;

import android.os.Handler;
import android.os.HandlerThread;

public class DaemonThread {
    private static final HandlerThread WORKER_THREAD = new HandlerThread("launcher-daemon");
    static {
        WORKER_THREAD.start();
    }
    private static final Handler WORKER_HANDLER = new Handler(WORKER_THREAD.getLooper());

    public static void postThreadTask(Runnable runnable) {
        WORKER_HANDLER.post(runnable);
    }
}
