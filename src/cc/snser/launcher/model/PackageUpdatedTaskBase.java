package cc.snser.launcher.model;

public abstract class PackageUpdatedTaskBase implements Runnable {
    public static final int OP_NONE = 0;
    public static final int OP_ADD = 1;
    public static final int OP_UPDATE = 2;
    public static final int OP_REMOVE = 3; // uninstlled
    public static final int OP_UNAVAILABLE = 4; // external media unmounted
    public static final int OP_AVAILABLE = 5; // external media mounted

    private static final String TAG = "Launcher.Model.PackageUpdatedTask";

    protected final LauncherModel mLauncherModel;
    protected final int mOp;
    protected final String[] mPackages;

    public PackageUpdatedTaskBase(LauncherModel launcherModel, int op, String[] packages) {
        mLauncherModel = launcherModel;

        mOp = op;
        mPackages = packages;
    }

    protected void commonRun() {
    	/* final App context = mLauncherModel.mApp;

        final Callbacks callbacks = mLauncherModel.mCallbacks != null ? mLauncherModel.mCallbacks.get() : null;
        if (callbacks == null) {
            XLog.w(TAG, "Nobody to tell about the new app. Launcher is probably loading.");
            return;
        }

         final String[] packages = mPackages;
        final int n = packages.length;

        if (mOp == OP_ADD || mOp == OP_UPDATE) {
            for (int i = 0; i < n; i++) {
                String packageName = packages[i];

                if (Utils.isThemePackage(packageName)) {
                    FileUtils.deleteQuietly(new File(Theme.ONLINE_THEME_DOWNLOAD, packageName));  // Delete the downloaded theme file
                }
            }
        }

        if (mOp == OP_REMOVE) {
            boolean needToApplyDefaultTheme = false;
            for (int i = 0; i < n; i++) {
                if (Theme.isInUsing(context, packages[i])) {
                    needToApplyDefaultTheme = true;
                }
            }
        }*/
    }
}
