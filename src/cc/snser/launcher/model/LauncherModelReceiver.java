package cc.snser.launcher.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import cc.snser.launcher.App;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.util.StringUtils;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

public class LauncherModelReceiver extends BroadcastReceiver {
    private static final String TAG = "Launcher.Model.LauncherModelReceiver";

    private final LauncherModel mLauncherModel;

    LauncherModelReceiver(LauncherModel launcherModel) {
        this.mLauncherModel = launcherModel;
    }

    /**
     * Call from the handler for ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED and
     * ACTION_PACKAGE_CHANGED.
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "onReceive intent=" + intent);
        }

        final LauncherModel.Callbacks callbacks = mLauncherModel.mCallbacks != null ? mLauncherModel.mCallbacks.get() : null;
        if (callbacks == null) {
            XLog.e(TAG, "Launcher context is no longer exists.");
            return;
        }

        final String action = intent.getAction();

        if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                || Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            final String packageName = intent.getData().getSchemeSpecificPart();
            final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

            int op = PackageUpdatedTaskBase.OP_NONE;

            if (StringUtils.isEmpty(packageName)) {
                // they sent us a bad intent
                return;
            }

            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                op = PackageUpdatedTaskBase.OP_UPDATE;
            } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                if (!replacing) {
                    op = PackageUpdatedTaskBase.OP_REMOVE;
                }
                // else, we are replacing the package, so a PACKAGE_ADDED will be sent
                // later, we will update the package at this time

            } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                if (replacing) {
                    op = PackageUpdatedTaskBase.OP_UPDATE;
                } else {
                    op = PackageUpdatedTaskBase.OP_ADD;
                }
            }

            if (op != PackageUpdatedTaskBase.OP_NONE) {
                mLauncherModel.enqueuePackageUpdated(createPackageUpdatedTask(op, new String[] {packageName}));
            }
        } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
            // First, schedule to add these apps back in.
            String[] packages = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);

            mLauncherModel.enqueuePackageUpdated(createPackageUpdatedTask(PackageUpdatedTaskBase.OP_AVAILABLE, packages));
        } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
            // ignore
        }
    }

    protected PackageUpdatedTaskBase createPackageUpdatedTask(int op, String[] packages) {
        return mLauncherModel.createPackageUpdatedTask(op, packages);
    }
}
