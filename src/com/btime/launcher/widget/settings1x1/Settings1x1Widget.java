package com.btime.launcher.widget.settings1x1;

import android.app.Activity;
import com.btime.launcher.app.AppController;
import com.btime.launcher.widget.entrance.EntranceWidget;

public class Settings1x1Widget extends EntranceWidget {

    public Settings1x1Widget(Activity context) {
        super(context);
        setLaunchPackage(AppController.PKGNAME_C601_SETTINGS);
    }

}
