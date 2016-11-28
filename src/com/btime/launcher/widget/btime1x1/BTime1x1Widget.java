package com.btime.launcher.widget.btime1x1;

import android.app.Activity;

import com.btime.launcher.R;
import com.btime.launcher.app.AppController;
import com.btime.launcher.widget.entrance.EntranceWidget;

public class BTime1x1Widget extends EntranceWidget {

    public BTime1x1Widget(Activity context) {
        super(context);
        setIcon(R.drawable.widget_btime1x1_icon_default);
        setLabel(R.string.widget_btime1x1_name);
        setLaunchPackage(AppController.PKGNAME_C601_BTIME);
    }

}
